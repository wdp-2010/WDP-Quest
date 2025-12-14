package com.wdp.quest.quest;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.integrations.ProgressIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages daily quest rotation and selection
 * Each day, players get a fresh set of quests based on their progress level
 */
public class DailyQuestManager {
    
    private final WDPQuestPlugin plugin;
    private final QuestManager questManager;
    
    // Cache of daily quests per player (refreshes on day change)
    private final Map<UUID, DailyQuestData> playerDailyQuests = new ConcurrentHashMap<>();
    
    // Global daily seed for consistent selection
    private long currentDaySeed;
    private LocalDate currentDate;
    
    // Configuration
    private int questsPerDay = 5;
    private int resetHour = 0;
    
    public DailyQuestManager(WDPQuestPlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.currentDate = LocalDate.now();
        this.currentDaySeed = generateDaySeed(currentDate);
        
        loadConfig();
        startDailyResetTask();
    }
    
    private void loadConfig() {
        questsPerDay = plugin.getConfig().getInt("daily-quests.quests-per-day", 5);
        resetHour = plugin.getConfig().getInt("daily-quests.reset-hour", 0);
    }
    
    /**
     * Generate a consistent seed based on date
     */
    private long generateDaySeed(LocalDate date) {
        return date.toEpochDay() * 31 + 17;
    }
    
    /**
     * Start the task that checks for daily reset
     */
    private void startDailyResetTask() {
        // Check every minute for day change
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            LocalDate now = LocalDate.now();
            LocalTime time = LocalTime.now();
            
            // Check if we've passed the reset hour and day has changed
            if (!now.equals(currentDate) && time.getHour() >= resetHour) {
                currentDate = now;
                currentDaySeed = generateDaySeed(currentDate);
                
                // Clear all cached daily quests
                playerDailyQuests.clear();
                
                // Notify online players
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String message = plugin.getConfigManager().getMessage("daily-reset");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                    }
                });
                
                plugin.getLogger().info("Daily quests have been reset!");
            }
        }, 20L * 60, 20L * 60); // Check every minute
    }
    
    /**
     * Get the daily quests for a player
     */
    public List<Quest> getDailyQuests(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check if we have cached quests for today
        DailyQuestData data = playerDailyQuests.get(uuid);
        if (data != null && data.date.equals(currentDate)) {
            return data.quests;
        }
        
        // Generate new daily quests
        List<Quest> dailyQuests = generateDailyQuests(player);
        playerDailyQuests.put(uuid, new DailyQuestData(currentDate, dailyQuests));
        
        return dailyQuests;
    }
    
    /**
     * Generate daily quests for a player based on their progress
     */
    private List<Quest> generateDailyQuests(Player player) {
        double playerProgress = getPlayerProgress(player);
        
        // Get available quests for player's progress level
        List<Quest> availableNormal = questManager.getQuestsForProgress(playerProgress, false);
        List<Quest> availableHard = questManager.getHardQuests().stream()
                .filter(q -> q.getRequiredProgress() <= playerProgress)
                .collect(Collectors.toList());
        
        List<Quest> selectedQuests = new ArrayList<>();
        Random random = new Random(currentDaySeed + player.getUniqueId().hashCode());
        
        // Shuffle available quests using the seeded random
        List<Quest> shuffledNormal = new ArrayList<>(availableNormal);
        Collections.shuffle(shuffledNormal, random);
        
        List<Quest> shuffledHard = new ArrayList<>(availableHard);
        Collections.shuffle(shuffledHard, random);
        
        // First, add any multi-day hard quests that are still active
        List<Quest> activeHardQuests = getActiveMultiDayQuests(player, shuffledHard);
        selectedQuests.addAll(activeHardQuests);
        
        // Determine how many more quests we need
        int remaining = questsPerDay - selectedQuests.size();
        
        // Try to add 1 hard quest if we don't have one yet
        if (selectedQuests.stream().noneMatch(Quest::isHardQuest) && !shuffledHard.isEmpty()) {
            for (Quest hard : shuffledHard) {
                if (!selectedQuests.contains(hard)) {
                    selectedQuests.add(hard);
                    remaining--;
                    break;
                }
            }
        }
        
        // Fill with normal quests, trying to get variety across categories
        Map<QuestCategory, List<Quest>> byCategory = shuffledNormal.stream()
                .filter(q -> !selectedQuests.contains(q))
                .collect(Collectors.groupingBy(Quest::getCategory));
        
        // Round-robin across categories for variety
        List<QuestCategory> categories = new ArrayList<>(byCategory.keySet());
        Collections.shuffle(categories, random);
        
        int catIndex = 0;
        while (remaining > 0 && !categories.isEmpty()) {
            QuestCategory cat = categories.get(catIndex % categories.size());
            List<Quest> catQuests = byCategory.get(cat);
            
            if (catQuests != null && !catQuests.isEmpty()) {
                selectedQuests.add(catQuests.remove(0));
                remaining--;
                
                if (catQuests.isEmpty()) {
                    categories.remove(cat);
                    byCategory.remove(cat);
                }
            } else {
                categories.remove(cat);
            }
            
            if (!categories.isEmpty()) {
                catIndex = (catIndex + 1) % categories.size();
            }
        }
        
        // Sort by category then sort order
        selectedQuests.sort((a, b) -> {
            int catCompare = a.getCategory().ordinal() - b.getCategory().ordinal();
            return catCompare != 0 ? catCompare : a.getSortOrder() - b.getSortOrder();
        });
        
        return selectedQuests;
    }
    
    /**
     * Get hard quests that span multiple days and are still active
     */
    private List<Quest> getActiveMultiDayQuests(Player player, List<Quest> hardQuests) {
        List<Quest> active = new ArrayList<>();
        
        for (Quest quest : hardQuests) {
            if (quest.getDaysAvailable() > 1) {
                // Check if this quest started on a previous day and is still within its window
                int daysAgo = getDaysSinceQuestStart(quest);
                if (daysAgo > 0 && daysAgo < quest.getDaysAvailable()) {
                    // Quest is still active from a previous day
                    active.add(quest);
                }
            }
        }
        
        return active;
    }
    
    /**
     * Calculate days since a quest would have started (based on seed cycling)
     */
    private int getDaysSinceQuestStart(Quest quest) {
        // Use quest ID hash to determine which day it started
        int questHash = quest.getId().hashCode();
        int cycleLength = quest.getDaysAvailable();
        
        // Calculate which day in the cycle we're on
        long dayNumber = currentDate.toEpochDay();
        int dayInCycle = (int) ((dayNumber + questHash) % cycleLength);
        
        return dayInCycle;
    }
    
    /**
     * Get player progress from WDP-Progress
     */
    private double getPlayerProgress(Player player) {
        ProgressIntegration progress = plugin.getProgressIntegration();
        if (progress != null && progress.isEnabled()) {
            return progress.getPlayerProgress(player);
        }
        return 0;
    }
    
    /**
     * Get time until next daily reset
     */
    public Duration getTimeUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = LocalDate.now().atTime(resetHour, 0);
        
        if (now.isAfter(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }
        
        return Duration.between(now, nextReset);
    }
    
    /**
     * Format time until reset as a string
     */
    public String getTimeUntilResetFormatted() {
        Duration duration = getTimeUntilReset();
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    /**
     * Check if a quest is a daily quest for a player today
     */
    public boolean isDailyQuest(Player player, Quest quest) {
        return getDailyQuests(player).contains(quest);
    }
    
    /**
     * Get the remaining days for a hard quest
     */
    public int getRemainingDays(Quest quest) {
        if (!quest.isHardQuest() || quest.getDaysAvailable() <= 1) {
            return 1;
        }
        
        int daysAgo = getDaysSinceQuestStart(quest);
        return Math.max(1, quest.getDaysAvailable() - daysAgo);
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        loadConfig();
        playerDailyQuests.clear();
    }
    
    /**
     * Data class for cached daily quests
     */
    private static class DailyQuestData {
        final LocalDate date;
        final List<Quest> quests;
        
        DailyQuestData(LocalDate date, List<Quest> quests) {
            this.date = date;
            this.quests = quests;
        }
    }
}

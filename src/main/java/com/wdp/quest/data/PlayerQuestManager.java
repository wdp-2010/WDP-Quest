package com.wdp.quest.data;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.quest.QuestObjective;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player quest data in memory with database persistence
 */
public class PlayerQuestManager {
    
    private final WDPQuestPlugin plugin;
    private final Map<UUID, PlayerQuestData> playerData = new ConcurrentHashMap<>();
    
    public PlayerQuestManager(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get or load player data
     */
    public PlayerQuestData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, id -> 
            plugin.getDatabaseManager().loadPlayerData(uuid));
    }
    
    public PlayerQuestData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Load player data (called on join)
     */
    public void loadPlayer(UUID uuid) {
        PlayerQuestData data = plugin.getDatabaseManager().loadPlayerData(uuid);
        playerData.put(uuid, data);
    }
    
    /**
     * Unload player data (called on quit)
     */
    public void unloadPlayer(UUID uuid) {
        PlayerQuestData data = playerData.remove(uuid);
        if (data != null) {
            savePlayerData(data);
        }
    }
    
    /**
     * Save player data to database
     */
    public void savePlayerData(PlayerQuestData data) {
        // Run async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PlayerQuestData.QuestProgress progress : data.getActiveQuests()) {
                plugin.getDatabaseManager().savePlayerQuest(data.getUuid(), progress);
                
                for (Map.Entry<String, PlayerQuestData.ObjectiveProgress> entry : 
                        progress.getAllObjectiveProgress().entrySet()) {
                    plugin.getDatabaseManager().saveObjectiveProgress(
                        data.getUuid(),
                        progress.getQuestId(),
                        entry.getKey(),
                        entry.getValue().getCurrentAmount(),
                        entry.getValue().isCompleted()
                    );
                }
            }
            
            for (PlayerQuestData.QuestProgress progress : data.getCompletedQuests()) {
                plugin.getDatabaseManager().savePlayerQuest(data.getUuid(), progress);
            }
        });
    }
    
    /**
     * Save all loaded players
     */
    public void saveAllPlayers() {
        for (PlayerQuestData data : playerData.values()) {
            savePlayerData(data);
        }
    }
    
    /**
     * Start a quest for a player
     */
    public boolean startQuest(Player player, Quest quest) {
        PlayerQuestData data = getPlayerData(player);
        
        // Check if already has this quest
        if (data.hasQuest(quest.getId())) {
            if (data.isQuestCompleted(quest.getId()) && !quest.isRepeatable()) {
                player.sendMessage(plugin.getMessages().get("quests.already-completed"));
                return false;
            }
            if (data.isQuestActive(quest.getId())) {
                player.sendMessage(plugin.getMessages().get("quests.already-active"));
                return false;
            }
        }
        
        // Check cooldown
        if (data.isOnCooldown(quest.getId())) {
            long remaining = data.getCooldownRemaining(quest.getId()) / 1000;
            player.sendMessage(plugin.getMessages().get("quests.cooldown", "seconds", String.valueOf(remaining)));
            return false;
        }
        
        // Check max active quests
        int maxQuests = plugin.getConfigManager().getMaxActiveQuests();
        if (data.getActiveQuestCount() >= maxQuests) {
            player.sendMessage(plugin.getMessages().get("quests.max-reached", "max", String.valueOf(maxQuests)));
            return false;
        }
        
        // Check progress requirement
        double playerProgress = plugin.getProgressIntegration().getPlayerProgress(player);
        if (playerProgress < quest.getRequiredProgress() && 
                !player.hasPermission("wdp.quest.bypass")) {
            player.sendMessage(plugin.getMessages().get("quests.locked", "progress", String.format("%.1f", quest.getRequiredProgress())));
            return false;
        }
        
        // Create quest progress
        PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(quest.getId());
        data.addQuestProgress(progress);
        
        // Auto-track if enabled
        if (plugin.getConfigManager().isAutoTrack()) {
            data.setTrackedQuestId(quest.getId());
        }
        
        // Save
        plugin.getDatabaseManager().savePlayerQuest(player.getUniqueId(), progress);
        
        // Play sound and send message
        player.playSound(player.getLocation(), 
            plugin.getConfigManager().getSound("start-quest"), 1.0f, 1.0f);
        player.sendMessage(plugin.getMessages().get("quests.started", "quest", quest.getDisplayName()));
        
        return true;
    }
    
    /**
     * Abandon a quest
     */
    public boolean abandonQuest(Player player, String questId) {
        PlayerQuestData data = getPlayerData(player);
        
        if (!data.isQuestActive(questId)) {
            player.sendMessage(plugin.getMessages().get("quests.not-active"));
            return false;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(questId);
        data.removeQuest(questId);
        plugin.getDatabaseManager().deletePlayerQuest(player.getUniqueId(), questId);
        
        // Clear tracking if this was tracked
        if (data.isTracking(questId)) {
            data.setTrackedQuestId(null);
        }
        
        String questName = quest != null ? quest.getDisplayName() : questId;
        player.sendMessage(plugin.getMessages().get("quests.abandoned", "quest", questName));
        
        return true;
    }
    
    /**
     * Complete a quest
     */
    public void completeQuest(Player player, Quest quest) {
        PlayerQuestData data = getPlayerData(player);
        PlayerQuestData.QuestProgress progress = data.getQuestProgress(quest.getId());
        
        if (progress == null) return;
        
        // Mark completed
        progress.setStatus(PlayerQuestData.QuestStatus.COMPLETED);
        progress.setCompletedAt(System.currentTimeMillis());
        
        // Clear tracking if this was tracked
        if (data.isTracking(quest.getId())) {
            data.setTrackedQuestId(null);
        }
        
        // Set cooldown for repeatable quests
        if (quest.isRepeatable() && quest.getCooldownSeconds() > 0) {
            long cooldownUntil = System.currentTimeMillis() + (quest.getCooldownSeconds() * 1000);
            data.setCooldown(quest.getId(), cooldownUntil);
            plugin.getDatabaseManager().saveCooldown(player.getUniqueId(), quest.getId(), cooldownUntil);
        }
        
        // Give rewards
        giveRewards(player, quest);
        
        // Register achievement if enabled
        if (plugin.getConfigManager().isRegisterAchievements()) {
            plugin.getProgressIntegration().grantAchievement(player, quest.getId());
        }
        
        // Recalculate progress if enabled
        if (plugin.getConfigManager().isRecalculateOnComplete()) {
            plugin.getProgressIntegration().recalculateProgress(player);
        }
        
        // Save
        plugin.getDatabaseManager().savePlayerQuest(player.getUniqueId(), progress);
        
        // Sound and message
        player.playSound(player.getLocation(),
            plugin.getConfigManager().getSound("complete-quest"), 1.0f, 1.0f);
        player.sendMessage(plugin.getMessages().get("quests.completed", "quest", quest.getDisplayName()));
        
        // Broadcast if enabled
        if (plugin.getConfigManager().isBroadcastCompletion()) {
            String broadcast = plugin.getMessages().get("broadcast.quest-completed",
                "player", player.getName(), "quest", quest.getDisplayName());
            Bukkit.broadcastMessage(broadcast);
        }
    }
    
    /**
     * Give quest rewards to player
     */
    private void giveRewards(Player player, Quest quest) {
        var rewards = quest.getRewards();
        var economy = plugin.getEconomyIntegration();
        var config = plugin.getConfigManager();
        
        // Calculate scaled rewards
        double coinReward = rewards.getCoins();
        if (config.isScaleByProgress()) {
            double progress = plugin.getProgressIntegration().getPlayerProgress(player);
            double multiplier = 1 + (progress / 100.0) * config.getProgressMultiplier();
            coinReward *= multiplier;
        }
        
        // Give coins
        if (coinReward > 0 && economy.isEnabled()) {
            economy.giveCoins(player, coinReward);
            player.sendMessage(plugin.getMessages().get("rewards.coins", "amount", String.format("%.0f", coinReward)));
        }
        
        // Give tokens
        if (rewards.getTokens() > 0 && economy.hasTokenSupport()) {
            economy.giveTokens(player, rewards.getTokens());
            player.sendMessage(plugin.getMessages().get("rewards.tokens", "amount", String.format("%.0f", rewards.getTokens())));
        }
        
        // Give experience
        int xpReward = rewards.getExperience();
        if (config.isBonusXp()) {
            xpReward += config.getXpPerQuest();
        }
        if (xpReward > 0) {
            player.giveExp(xpReward);
            player.sendMessage(plugin.getMessages().get("rewards.xp", "amount", String.valueOf(xpReward)));
        }
        
        // Give items
        for (var item : rewards.getItems()) {
            var leftover = player.getInventory().addItem(item.clone());
            // Drop any items that don't fit
            for (var drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        
        // Execute commands
        for (String command : rewards.getCommands()) {
            String parsedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }
    
    /**
     * Update objective progress
     */
    public void updateObjective(Player player, Quest quest, QuestObjective objective, int amount) {
        PlayerQuestData data = getPlayerData(player);
        PlayerQuestData.QuestProgress progress = data.getQuestProgress(quest.getId());
        
        if (progress == null || progress.getStatus() != PlayerQuestData.QuestStatus.ACTIVE) {
            return;
        }
        
        // Increment progress
        progress.incrementObjective(objective.getId(), amount, objective.getTargetAmount());
        
        // Save objective progress
        var objProgress = progress.getObjectiveProgress(objective.getId());
        plugin.getDatabaseManager().saveObjectiveProgress(
            player.getUniqueId(),
            quest.getId(),
            objective.getId(),
            objProgress.getCurrentAmount(),
            objProgress.isCompleted()
        );
        
        // Send progress message if tracking
        if (data.isTracking(quest.getId())) {
            player.sendMessage(plugin.getMessages().get("objectives.progress",
                "objective", objective.getFormattedDescription(),
                "current", String.valueOf(objProgress.getCurrentAmount()),
                "target", String.valueOf(objective.getTargetAmount())));
        }
        
        // Check if quest is complete
        if (progress.areAllObjectivesComplete(quest.getTotalObjectives())) {
            completeQuest(player, quest);
        }
    }
}

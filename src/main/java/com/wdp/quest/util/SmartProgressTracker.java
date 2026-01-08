package com.wdp.quest.util;

import com.wdp.quest.WDPQuestPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart progress update system that prevents spam and shows meaningful updates
 * 
 * Features:
 * - Shows at most 8 milestones for any objective
 * - Minimum time between messages (configurable)
 * - Time-based updates (shows update after inactivity)
 * - Spam detection with helpful message
 * - Per-player toggle for updates
 */
public class SmartProgressTracker {
    
    private final WDPQuestPlugin plugin;
    
    // Player preferences (UUID -> enabled)
    private final Map<UUID, Boolean> playerPreferences = new ConcurrentHashMap<>();
    
    // Track last progress time per player per quest+objective
    private final Map<String, Long> lastProgressTime = new ConcurrentHashMap<>();
    
    // Track last message time per player per quest
    private final Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    
    // Track message count in time window per player
    private final Map<UUID, List<Long>> messageHistory = new ConcurrentHashMap<>();
    
    // Configuration
    private int minTimeBetweenMessages = 2000; // 2 seconds
    private int inactivityThreshold = 30000; // 30 seconds
    private int spamWindowSeconds = 60; // 1 minute
    private int maxMessagesInWindow = 15; // Max messages per minute before warning
    
    public SmartProgressTracker(WDPQuestPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        var config = plugin.getConfigManager();
        // Load from config if available
        minTimeBetweenMessages = plugin.getConfig().getInt("progress-updates.min-time-between-ms", 2000);
        inactivityThreshold = plugin.getConfig().getInt("progress-updates.inactivity-threshold-ms", 30000);
        spamWindowSeconds = plugin.getConfig().getInt("progress-updates.spam-window-seconds", 60);
        maxMessagesInWindow = plugin.getConfig().getInt("progress-updates.max-messages-in-window", 15);
    }
    
    /**
     * Check if a progress update should be shown
     * 
     * @param player The player
     * @param questId The quest ID
     * @param objectiveId The objective ID
     * @param currentAmount Current progress
     * @param targetAmount Target progress
     * @return true if update should be shown
     */
    public boolean shouldShowUpdate(Player player, String questId, String objectiveId, 
                                   int currentAmount, int targetAmount) {
        UUID uuid = player.getUniqueId();
        
        // Check if player has disabled updates
        if (!isEnabled(uuid)) {
            return false;
        }
        
        String progressKey = uuid + ":" + questId + ":" + objectiveId;
        String messageKey = uuid + ":" + questId;
        long now = System.currentTimeMillis();
        
        // Always show completion
        if (currentAmount >= targetAmount) {
            recordMessage(uuid, now);
            return true;
        }
        
        // Check if this is a milestone
        if (!isMilestone(currentAmount, targetAmount)) {
            // Not a milestone, but check for inactivity update
            Long lastProgress = lastProgressTime.get(progressKey);
            if (lastProgress != null && (now - lastProgress) > inactivityThreshold) {
                // Been inactive, show update on return
                lastProgressTime.put(progressKey, now);
                if (canSendMessage(messageKey, now)) {
                    recordMessage(uuid, now);
                    return true;
                }
            } else {
                lastProgressTime.put(progressKey, now);
            }
            return false;
        }
        
        // This is a milestone, check timing
        lastProgressTime.put(progressKey, now);
        
        if (!canSendMessage(messageKey, now)) {
            // Too soon since last message, skip this milestone
            return false;
        }
        
        // Check for spam
        recordMessage(uuid, now);
        if (isSpamming(uuid, now)) {
            // Show spam warning
            player.sendMessage(plugin.getMessages().get("quest.progress-spam-warning"));
            return true; // Still show this update
        }
        
        return true;
    }
    
    /**
     * Calculate if current amount is a milestone
     * Smart milestones: at most 8 updates for the entire objective
     * 
     * Examples:
     * - 4 target: milestones at 1, 2, 3, 4
     * - 64 target: milestones at 8, 16, 24, 32, 40, 48, 56, 64
     * - 128 target: milestones at 16, 32, 48, 64, 80, 96, 112, 128
     */
    private boolean isMilestone(int current, int target) {
        if (current >= target) return true; // Completion is always a milestone
        
        // Calculate milestone interval (at most 8 milestones)
        int interval = Math.max(1, target / 8);
        
        // Special case: for very small targets (1-8), show each one
        if (target <= 8) {
            return true;
        }
        
        // Check if current is a multiple of interval
        return (current % interval) == 0;
    }
    
    /**
     * Check if enough time has passed since last message
     */
    private boolean canSendMessage(String messageKey, long now) {
        Long lastTime = lastMessageTime.get(messageKey);
        if (lastTime == null) {
            return true;
        }
        return (now - lastTime) >= minTimeBetweenMessages;
    }
    
    /**
     * Record that a message was sent
     */
    private void recordMessage(UUID uuid, long now) {
        String messageKey = uuid + ":*"; // Track all messages for this player
        lastMessageTime.put(messageKey, now);
        
        // Add to history for spam detection
        messageHistory.computeIfAbsent(uuid, k -> new ArrayList<>()).add(now);
        
        // Clean old history
        cleanOldHistory(uuid, now);
    }
    
    /**
     * Check if player is spamming (too many messages in time window)
     */
    private boolean isSpamming(UUID uuid, long now) {
        List<Long> history = messageHistory.get(uuid);
        if (history == null) return false;
        
        cleanOldHistory(uuid, now);
        return history.size() >= maxMessagesInWindow;
    }
    
    /**
     * Remove old message history outside the spam window
     */
    private void cleanOldHistory(UUID uuid, long now) {
        List<Long> history = messageHistory.get(uuid);
        if (history == null) return;
        
        long windowStart = now - (spamWindowSeconds * 1000L);
        history.removeIf(time -> time < windowStart);
    }
    
    /**
     * Check if updates are enabled for this player
     */
    public boolean isEnabled(UUID uuid) {
        return playerPreferences.getOrDefault(uuid, true);
    }
    
    /**
     * Toggle updates for a player
     */
    public void toggleUpdates(UUID uuid) {
        boolean current = isEnabled(uuid);
        playerPreferences.put(uuid, !current);
    }
    
    /**
     * Set update preference for a player
     */
    public void setEnabled(UUID uuid, boolean enabled) {
        playerPreferences.put(uuid, enabled);
    }
    
    /**
     * Clear tracking data for a player (on logout)
     */
    public void clearPlayer(UUID uuid) {
        String prefix = uuid.toString() + ":";
        lastProgressTime.keySet().removeIf(key -> key.startsWith(prefix));
        lastMessageTime.keySet().removeIf(key -> key.startsWith(prefix));
        messageHistory.remove(uuid);
    }
}

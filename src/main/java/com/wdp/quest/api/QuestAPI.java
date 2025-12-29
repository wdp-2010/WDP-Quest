package com.wdp.quest.api;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.data.PlayerQuestData;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.quest.QuestCategory;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Public API for other plugins to interact with the quest system
 * 
 * Usage:
 * <pre>
 * QuestAPI api = ((WDPQuestPlugin) Bukkit.getPluginManager().getPlugin("WDPQuest")).getQuestAPI();
 * boolean started = api.startQuest(player, "mining_basics");
 * </pre>
 */
public class QuestAPI {
    
    private final WDPQuestPlugin plugin;
    
    public QuestAPI(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get a quest by its ID
     * @param questId The quest identifier
     * @return The quest, or null if not found
     */
    public Quest getQuest(String questId) {
        return plugin.getQuestManager().getQuest(questId);
    }
    
    /**
     * Get all quests in a category
     * @param category The quest category
     * @return List of quests in that category
     */
    public List<Quest> getQuestsByCategory(QuestCategory category) {
        return plugin.getQuestManager().getQuestsByCategory(category);
    }
    
    /**
     * Get quests available to a player based on their progress
     * @param playerProgress The player's progress percentage
     * @return List of available quests
     */
    public List<Quest> getAvailableQuests(double playerProgress) {
        return plugin.getQuestManager().getAvailableQuests(playerProgress);
    }
    
    /**
     * Start a quest for a player
     * @param player The player
     * @param questId The quest to start
     * @return true if successfully started
     */
    public boolean startQuest(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return false;
        return plugin.getPlayerQuestManager().startQuest(player, quest);
    }
    
    /**
     * Abandon a quest for a player
     * @param player The player
     * @param questId The quest to abandon
     * @return true if successfully abandoned
     */
    public boolean abandonQuest(Player player, String questId) {
        return plugin.getPlayerQuestManager().abandonQuest(player, questId);
    }
    
    /**
     * Check if a player has an active quest
     * @param player The player
     * @param questId The quest ID
     * @return true if quest is active
     */
    public boolean isQuestActive(Player player, String questId) {
        return plugin.getPlayerQuestManager().getPlayerData(player).isQuestActive(questId);
    }
    
    /**
     * Check if a player has completed a quest
     * @param player The player
     * @param questId The quest ID
     * @return true if quest is completed
     */
    public boolean isQuestCompleted(Player player, String questId) {
        return plugin.getPlayerQuestManager().getPlayerData(player).isQuestCompleted(questId);
    }
    
    /**
     * Get a player's active quest count
     * @param player The player
     * @return Number of active quests
     */
    public int getActiveQuestCount(Player player) {
        return plugin.getPlayerQuestManager().getPlayerData(player).getActiveQuestCount();
    }
    
    /**
     * Get a player's completed quest count
     * @param player The player
     * @return Number of completed quests
     */
    public int getCompletedQuestCount(Player player) {
        return plugin.getPlayerQuestManager().getPlayerData(player).getCompletedQuestCount();
    }
    
    /**
     * Get quest completion percentage for a player
     * @param player The player
     * @param questId The quest ID
     * @return Completion percentage (0-100), or -1 if quest not active
     */
    public double getQuestProgress(Player player, String questId) {
        PlayerQuestData data = plugin.getPlayerQuestManager().getPlayerData(player);
        PlayerQuestData.QuestProgress progress = data.getQuestProgress(questId);
        Quest quest = plugin.getQuestManager().getQuest(questId);
        
        if (progress == null || quest == null) return -1;
        var targets = new java.util.LinkedHashMap<String,Integer>();
        for (var obj : quest.getObjectives()) targets.put(obj.getId(), obj.getTargetAmount());
        if (targets.isEmpty()) return 100.0;
        return progress.getActualCompletionPercentage(targets);
    }
    
    /**
     * Trigger a custom objective for a player
     * This can be used by other plugins to progress custom objectives
     * @param player The player
     * @param customData The custom objective data identifier
     * @param amount Amount to progress
     */
    public void triggerCustomObjective(Player player, String customData, int amount) {
        var listener = new com.wdp.quest.listeners.QuestObjectiveListener(plugin);
        listener.triggerCustomObjective(player, customData, amount);
    }
    
    /**
     * Trigger a visit objective for a player
     * @param player The player
     * @param location The location identifier
     */
    public void triggerVisitObjective(Player player, String location) {
        var listener = new com.wdp.quest.listeners.QuestObjectiveListener(plugin);
        listener.triggerVisitObjective(player, location);
    }
    
    /**
     * Get total number of loaded quests
     * @return Quest count
     */
    public int getTotalQuestCount() {
        return plugin.getQuestManager().getQuestCount();
    }
    
    /**
     * Force complete a quest for a player (admin use)
     * @param player The player
     * @param questId The quest to complete
     * @return true if successfully completed
     */
    public boolean forceCompleteQuest(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return false;
        
        PlayerQuestData data = plugin.getPlayerQuestManager().getPlayerData(player);
        
        // Start if not active
        if (!data.isQuestActive(questId)) {
            PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(questId);
            data.addQuestProgress(progress);
        }
        
        // Complete all objectives
        PlayerQuestData.QuestProgress progress = data.getQuestProgress(questId);
        for (var objective : quest.getObjectives()) {
            progress.setObjectiveProgress(objective.getId(), objective.getTargetAmount(), true);
        }
        
        // Complete quest
        plugin.getPlayerQuestManager().completeQuest(player, quest);
        return true;
    }
}

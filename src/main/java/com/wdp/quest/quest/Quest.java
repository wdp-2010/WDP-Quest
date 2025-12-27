package com.wdp.quest.quest;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a quest definition
 */
public class Quest {
    
    private final String id;
    private String displayName;
    private String description;
    private List<String> lore;
    private Material icon;
    private QuestCategory category;
    private double requiredProgress;
    private List<QuestObjective> objectives;
    private QuestRewards rewards;
    private boolean repeatable;
    private long cooldownSeconds;
    private int sortOrder;
    
    // Daily quest system fields
    private boolean hardQuest;      // Is this a hard quest?
    private int daysAvailable;      // How many days this quest is available (default 1, hard quests can be 2-3+)
    
    public Quest(String id) {
        this.id = id;
        this.displayName = id;
        this.description = "";
        this.lore = new ArrayList<>();
        this.icon = Material.BOOK;
        this.category = QuestCategory.BEGINNER;
        this.requiredProgress = 0;
        this.objectives = new ArrayList<>();
        this.rewards = new QuestRewards();
        this.repeatable = false;
        this.cooldownSeconds = 0;
        this.sortOrder = 0;
        this.hardQuest = false;
        this.daysAvailable = 1;
    }
    
    // Builder pattern methods
    public Quest displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
    
    public Quest description(String description) {
        this.description = description;
        return this;
    }
    
    public Quest lore(List<String> lore) {
        this.lore = lore;
        return this;
    }
    
    public Quest icon(Material icon) {
        this.icon = icon;
        return this;
    }
    
    public Quest category(QuestCategory category) {
        this.category = category;
        return this;
    }
    
    public Quest requiredProgress(double requiredProgress) {
        this.requiredProgress = requiredProgress;
        return this;
    }
    
    public Quest addObjective(QuestObjective objective) {
        this.objectives.add(objective);
        return this;
    }
    
    public Quest objectives(List<QuestObjective> objectives) {
        this.objectives = objectives;
        return this;
    }
    
    public Quest rewards(QuestRewards rewards) {
        this.rewards = rewards;
        return this;
    }
    
    public Quest repeatable(boolean repeatable) {
        this.repeatable = repeatable;
        return this;
    }
    
    public Quest cooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
        return this;
    }
    
    public Quest sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }
    
    public Quest hardQuest(boolean hardQuest) {
        this.hardQuest = hardQuest;
        return this;
    }
    
    public Quest daysAvailable(int daysAvailable) {
        this.daysAvailable = Math.max(1, daysAvailable);
        return this;
    }
    
    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getLore() { return lore; }
    public Material getIcon() { return icon; }
    public QuestCategory getCategory() { return category; }
    public double getRequiredProgress() { return requiredProgress; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public QuestRewards getRewards() { return rewards; }
    public boolean isRepeatable() { return repeatable; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public int getSortOrder() { return sortOrder; }
    public boolean isHardQuest() { return hardQuest; }
    public int getDaysAvailable() { return daysAvailable; }
    
    /**
     * Get total objectives count
     */
    public int getTotalObjectives() {
        return objectives.size();
    }
    
    /**
     * Get the progress range this quest belongs to (e.g., "0-20")
     */
    public String getProgressRange() {
        int lower = (int) (Math.floor(requiredProgress / 20) * 20);
        int upper = lower + 20;
        return lower + "-" + Math.min(upper, 100);
    }
}

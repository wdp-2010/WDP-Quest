package com.wdp.quest.quest;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * Quest categories based on progress ranges
 */
public enum QuestCategory {
    
    BEGINNER("Beginner", "§a", Material.WOODEN_SWORD, 0, 20, 
            "Perfect for new adventurers starting their journey."),
    
    EARLY("Early Game", "§e", Material.STONE_SWORD, 20, 40,
            "For players getting established in the world."),
    
    INTERMEDIATE("Intermediate", "§6", Material.IRON_SWORD, 40, 60,
            "Challenging quests for experienced players."),
    
    ADVANCED("Advanced", "§c", Material.DIAMOND_SWORD, 60, 80,
            "Difficult quests requiring significant preparation."),
    
    EXPERT("Expert", "§5", Material.NETHERITE_SWORD, 80, 100,
            "The ultimate challenges for master adventurers.");
    
    private final String displayName;
    private final String color;
    private final Material icon;
    private final int minProgress;
    private final int maxProgress;
    private final String description;
    
    QuestCategory(String displayName, String color, Material icon, int minProgress, int maxProgress, String description) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.minProgress = minProgress;
        this.maxProgress = maxProgress;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getColoredName() { return color + displayName; }
    public Material getIcon() { return icon; }
    public int getMinProgress() { return minProgress; }
    public int getMaxProgress() { return maxProgress; }
    public String getDescription() { return description; }
    
    /**
     * Get the progress range string (e.g., "0-20%")
     */
    public String getProgressRange() {
        return minProgress + "-" + maxProgress + "%";
    }
    
    /**
     * Check if a progress value falls within this category
     */
    public boolean isInRange(double progress) {
        return progress >= minProgress && progress < maxProgress;
    }
    
    /**
     * Get category by progress value
     */
    public static QuestCategory fromProgress(double progress) {
        for (QuestCategory category : values()) {
            if (category.isInRange(progress)) {
                return category;
            }
        }
        return EXPERT; // Default to expert for 100%
    }
    
    /**
     * Get category by name (case-insensitive)
     */
    public static QuestCategory fromName(String name) {
        for (QuestCategory category : values()) {
            if (category.name().equalsIgnoreCase(name) || 
                category.displayName.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return BEGINNER;
    }
}

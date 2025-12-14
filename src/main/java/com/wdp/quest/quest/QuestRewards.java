package com.wdp.quest.quest;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the rewards for completing a quest
 */
public class QuestRewards {
    
    private double coins;
    private double tokens;
    private int experience;
    private List<ItemStack> items;
    private List<String> commands;
    
    public QuestRewards() {
        this.coins = 0;
        this.tokens = 0;
        this.experience = 0;
        this.items = new ArrayList<>();
        this.commands = new ArrayList<>();
    }
    
    // Builder methods
    public QuestRewards coins(double coins) {
        this.coins = coins;
        return this;
    }
    
    public QuestRewards tokens(double tokens) {
        this.tokens = tokens;
        return this;
    }
    
    public QuestRewards experience(int experience) {
        this.experience = experience;
        return this;
    }
    
    public QuestRewards addItem(ItemStack item) {
        this.items.add(item);
        return this;
    }
    
    public QuestRewards items(List<ItemStack> items) {
        this.items = items;
        return this;
    }
    
    public QuestRewards addCommand(String command) {
        this.commands.add(command);
        return this;
    }
    
    public QuestRewards commands(List<String> commands) {
        this.commands = commands;
        return this;
    }
    
    /**
     * Check if there are any rewards
     */
    public boolean hasRewards() {
        return coins > 0 || tokens > 0 || experience > 0 || !items.isEmpty() || !commands.isEmpty();
    }
    
    /**
     * Get a summary of rewards for display
     */
    public List<String> getRewardSummary() {
        List<String> summary = new ArrayList<>();
        
        if (coins > 0) {
            summary.add("§e" + formatNumber(coins) + " §6SkillCoins");
        }
        if (tokens > 0) {
            summary.add("§d" + formatNumber(tokens) + " §5SkillTokens");
        }
        if (experience > 0) {
            summary.add("§a" + experience + " §2Experience");
        }
        if (!items.isEmpty()) {
            summary.add("§b" + items.size() + " §3Item(s)");
        }
        
        return summary;
    }
    
    private String formatNumber(double num) {
        if (num >= 1000) {
            return String.format("%.1fk", num / 1000);
        }
        return String.format("%.0f", num);
    }
    
    // Getters
    public double getCoins() { return coins; }
    public double getTokens() { return tokens; }
    public int getExperience() { return experience; }
    public List<ItemStack> getItems() { return items; }
    public List<String> getCommands() { return commands; }
    
    // Setters
    public void setCoins(double coins) { this.coins = coins; }
    public void setTokens(double tokens) { this.tokens = tokens; }
    public void setExperience(int experience) { this.experience = experience; }
}

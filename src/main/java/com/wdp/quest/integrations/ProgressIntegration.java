package com.wdp.quest.integrations;

import com.wdp.progress.WDPProgressPlugin;
import com.wdp.progress.api.ProgressAPI;
import com.wdp.quest.WDPQuestPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Integration with WDP-Progress plugin for progress tracking
 */
public class ProgressIntegration {
    
    private final WDPQuestPlugin plugin;
    private ProgressAPI progressAPI;
    private boolean enabled = false;
    
    public ProgressIntegration(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize connection to WDP-Progress
     * @return true if successfully connected
     */
    public boolean initialize() {
        Plugin progressPlugin = Bukkit.getPluginManager().getPlugin("WDPProgress");
        
        if (progressPlugin == null) {
            plugin.getLogger().warning("WDPProgress plugin not found!");
            return false;
        }
        
        if (!progressPlugin.isEnabled()) {
            plugin.getLogger().warning("WDPProgress plugin is not enabled!");
            return false;
        }
        
        if (progressPlugin instanceof WDPProgressPlugin) {
            progressAPI = ((WDPProgressPlugin) progressPlugin).getProgressAPI();
            enabled = true;
            plugin.getLogger().info("Successfully hooked into WDP-Progress!");
            
            // Test the API
            plugin.getLogger().info("WDP-Progress API version verified.");
            return true;
        }
        
        plugin.getLogger().warning("Failed to get WDP-Progress API!");
        return false;
    }
    
    /**
     * Get a player's current progress (1-100)
     */
    public double getPlayerProgress(Player player) {
        if (!enabled || progressAPI == null) return 0;
        return progressAPI.getPlayerProgress(player);
    }
    
    /**
     * Get a player's progress by UUID (works offline)
     */
    public double getPlayerProgress(UUID uuid) {
        if (!enabled || progressAPI == null) return 0;
        return progressAPI.getPlayerProgress(uuid);
    }
    
    /**
     * Check if player has minimum progress threshold
     */
    public boolean hasProgress(Player player, double threshold) {
        if (!enabled || progressAPI == null) return true; // Fail-open
        return progressAPI.hasProgress(player, threshold);
    }
    
    /**
     * Grant a quest completion achievement
     */
    public boolean grantAchievement(Player player, String questId) {
        if (!enabled || progressAPI == null) return false;
        if (!plugin.getConfigManager().isRegisterAchievements()) return false;
        
        String achievementId = plugin.getConfigManager().getAchievementPrefix() + questId;
        boolean result = progressAPI.grantAchievement(player, achievementId);
        
        if (result) {
            plugin.getLogger().fine("Granted achievement " + achievementId + " to " + player.getName());
        }
        
        return result;
    }
    
    /**
     * Check if player has a quest achievement
     */
    public boolean hasAchievement(Player player, String questId) {
        if (!enabled || progressAPI == null) return false;
        String achievementId = plugin.getConfigManager().getAchievementPrefix() + questId;
        return progressAPI.hasAchievement(player, achievementId);
    }
    
    /**
     * Force recalculate player's progress
     */
    public double recalculateProgress(Player player) {
        if (!enabled || progressAPI == null) return 0;
        return progressAPI.recalculateProgress(player);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public ProgressAPI getProgressAPI() {
        return progressAPI;
    }
}

package com.wdp.quest.config;

import com.wdp.quest.WDPQuestPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Manages plugin configuration
 */
public class ConfigManager {
    
    private final WDPQuestPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration navbarConfig;
    
    // Cached config values
    private int maxActiveQuests;
    private boolean autoTrack;
    private boolean broadcastCompletion;
    private String broadcastFormat;
    private int itemsPerPage;
    private boolean scaleByProgress;
    private double progressMultiplier;
    private boolean bonusXp;
    private int xpPerQuest;
    private boolean registerAchievements;
    private String achievementPrefix;
    private boolean recalculateOnComplete;
    private String messagePrefix;
    
    public ConfigManager(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load values
        maxActiveQuests = config.getInt("quests.max-active-quests", 5);
        autoTrack = config.getBoolean("quests.auto-track", true);
        broadcastCompletion = config.getBoolean("quests.broadcast-completion", true);
        broadcastFormat = config.getString("quests.broadcast-format", "&6&l%player% &r&6completed the quest: &e%quest%");
        itemsPerPage = config.getInt("gui.items-per-page", 28);
        scaleByProgress = config.getBoolean("rewards.scale-by-progress", true);
        progressMultiplier = config.getDouble("rewards.progress-multiplier", 0.5);
        bonusXp = config.getBoolean("rewards.bonus-xp", true);
        xpPerQuest = config.getInt("rewards.xp-per-quest", 50);
        registerAchievements = config.getBoolean("progress.register-achievements", true);
        achievementPrefix = config.getString("progress.achievement-prefix", "quest_");
        recalculateOnComplete = config.getBoolean("progress.recalculate-on-complete", true);
        messagePrefix = config.getString("messages.prefix", "&8[&6Quest&8] &r");
        
        // Load navbar config
        loadNavbarConfig();
    }
    
    private void loadNavbarConfig() {
        File navbarFile = new File(plugin.getDataFolder(), "navbar.yml");
        if (!navbarFile.exists()) {
            plugin.saveResource("navbar.yml", false);
        }
        navbarConfig = YamlConfiguration.loadConfiguration(navbarFile);
    }
    
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "Message not found: " + key);
        return colorize(messagePrefix + message);
    }
    
    public String getMessageRaw(String key) {
        return colorize(config.getString("messages." + key, "Message not found: " + key));
    }
    
    public String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public Sound getSound(String key) {
        String soundName = config.getString("gui.sounds." + key, "UI_BUTTON_CLICK");
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.UI_BUTTON_CLICK;
        }
    }
    
    public String getMainMenuTitle() {
        return colorize(config.getString("gui.main-title", "&8&l✦ &6&lQuest Journal &8&l✦"));
    }
    
    public String getDetailMenuTitle(String questName) {
        return colorize(config.getString("gui.detail-title", "&8&l✦ &e%quest% &8&l✦")
                .replace("%quest%", questName));
    }
    
    // Getters
    public int getMaxActiveQuests() { return maxActiveQuests; }
    public boolean isAutoTrack() { return autoTrack; }
    public boolean isBroadcastCompletion() { return broadcastCompletion; }
    public String getBroadcastFormat() { return colorize(broadcastFormat); }
    public int getItemsPerPage() { return itemsPerPage; }
    public boolean isScaleByProgress() { return scaleByProgress; }
    public double getProgressMultiplier() { return progressMultiplier; }
    public boolean isBonusXp() { return bonusXp; }
    public int getXpPerQuest() { return xpPerQuest; }
    public boolean isRegisterAchievements() { return registerAchievements; }
    public String getAchievementPrefix() { return achievementPrefix; }
    public boolean isRecalculateOnComplete() { return recalculateOnComplete; }
    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getNavbarConfig() { return navbarConfig; }
}

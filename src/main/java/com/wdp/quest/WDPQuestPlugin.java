package com.wdp.quest;

import com.wdp.quest.commands.QuestAdminCommand;
import com.wdp.quest.commands.QuestCommand;
import com.wdp.quest.config.ConfigManager;
import com.wdp.quest.config.MessageManager;
import com.wdp.quest.data.DatabaseManager;
import com.wdp.quest.data.PlayerQuestManager;
import com.wdp.quest.integrations.EconomyIntegration;
import com.wdp.quest.integrations.ProgressIntegration;
import com.wdp.quest.listeners.PlayerEventListener;
import com.wdp.quest.listeners.QuestObjectiveListener;
import com.wdp.quest.quest.DailyQuestManager;
import com.wdp.quest.quest.QuestManager;
import com.wdp.quest.ui.QuestMenuListener;
import com.wdp.quest.api.QuestAPI;
import com.wdp.quest.util.SmartProgressTracker;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * WDP Quest Plugin - Complete quest system with progress integration
 */
public class WDPQuestPlugin extends JavaPlugin {
    
    private static WDPQuestPlugin instance;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private QuestManager questManager;
    private DailyQuestManager dailyQuestManager;
    private PlayerQuestManager playerQuestManager;
    private ProgressIntegration progressIntegration;
    private EconomyIntegration economyIntegration;
    private QuestAPI questAPI;
    private SmartProgressTracker smartProgressTracker;
    private int autoSaveTaskId = -1;
    
    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        
        getLogger().info("========================================");
        getLogger().info("  WDP Quest System v" + getDescription().getVersion());
        getLogger().info("  Loading quest system...");
        getLogger().info("========================================");
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize integrations
        progressIntegration = new ProgressIntegration(this);
        if (!progressIntegration.initialize()) {
            getLogger().severe("WDPProgress is required but not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        economyIntegration = new EconomyIntegration(this);
        economyIntegration.initialize();
        
        // Initialize quest system
        questManager = new QuestManager(this);
        questManager.loadQuests();
        
        // Initialize daily quest manager
        dailyQuestManager = new DailyQuestManager(this, questManager);
        
        playerQuestManager = new PlayerQuestManager(this);
        
        // Initialize smart progress tracker
        smartProgressTracker = new SmartProgressTracker(this);
        
        // Initialize API
        questAPI = new QuestAPI(this);
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start auto-save task (every 5 minutes = 6000 ticks)
        startAutoSave();
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("========================================");
        getLogger().info("  WDP Quest System enabled!");
        getLogger().info("  Loaded " + questManager.getQuestCount() + " quests");
        getLogger().info("  Auto-save enabled (5 minute intervals)");
        getLogger().info("  Load time: " + loadTime + "ms");
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling WDP Quest System...");
        
        // Cancel auto-save task
        if (autoSaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autoSaveTaskId);
        }
        
        // Save all player data
        if (playerQuestManager != null) {
            getLogger().info("Saving all player quest data...");
            playerQuestManager.saveAllPlayers();
        }
        
        // Close database
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("WDP Quest System disabled.");
    }
    
    private void registerCommands() {
        QuestCommand questCommand = new QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);
        
        QuestAdminCommand adminCommand = new QuestAdminCommand(this);
        getCommand("questadmin").setExecutor(adminCommand);
        getCommand("questadmin").setTabCompleter(adminCommand);
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestObjectiveListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestMenuListener(this), this);
    }
    
    /**
     * Start periodic auto-save task to prevent data loss
     */
    private void startAutoSave() {
        // Auto-save every 5 minutes (6000 ticks)
        autoSaveTaskId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerQuestManager != null) {
                int playerCount = getServer().getOnlinePlayers().size();
                if (playerCount > 0) {
                    getLogger().info("Auto-saving quest data for " + playerCount + " online players...");
                    playerQuestManager.saveAllPlayers();
                }
            }
        }, 6000L, 6000L).getTaskId();
    }
    
    public void reload() {
        configManager.loadConfig();
        messageManager.reload();
        questManager.loadQuests();
        dailyQuestManager.reload();
        getLogger().info("Configuration and quests reloaded.");
    }
    
    // Getters
    public static WDPQuestPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessageManager getMessages() {
        return messageManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public PlayerQuestManager getPlayerQuestManager() {
        return playerQuestManager;
    }
    
    public ProgressIntegration getProgressIntegration() {
        return progressIntegration;
    }
    
    public EconomyIntegration getEconomyIntegration() {
        return economyIntegration;
    }
    
    public DailyQuestManager getDailyQuestManager() {
        return dailyQuestManager;
    }
    
    public QuestAPI getQuestAPI() {
        return questAPI;
    }
    
    public SmartProgressTracker getSmartProgressTracker() {
        return smartProgressTracker;
    }
}

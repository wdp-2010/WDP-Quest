package com.wdp.quest.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Universal configuration migration system for WDP-Quest
 */
public class ConfigMigration {
    
    private final JavaPlugin plugin;
    
    public ConfigMigration(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean migrateConfig(File configFile, String resourcePath, int currentVersion) {
        if (!configFile.exists()) return false;
        
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
        int userVersion = userConfig.getInt("config-version", 1);
        
        if (userVersion >= currentVersion) return false;
        
        plugin.getLogger().info("========================================");
        plugin.getLogger().info("Config migration: " + configFile.getName());
        plugin.getLogger().info("User version: " + userVersion + " → Current: " + currentVersion);
        plugin.getLogger().info("========================================");
        
        try {
            backupConfig(configFile);
            
            InputStream defaultStream = plugin.getResource(resourcePath);
            if (defaultStream == null) {
                plugin.getLogger().severe("Could not load default config from JAR: " + resourcePath);
                return false;
            }
            
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream)
            );
            
            FileConfiguration mergedConfig = mergeConfigs(userConfig, defaultConfig);
            mergedConfig.set("config-version", currentVersion);
            mergedConfig.save(configFile);
            
            plugin.getLogger().info("✓ Migration complete! Backup: " + configFile.getName() + ".backup");
            return true;
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to migrate config: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void backupConfig(File configFile) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        File backupFile = new File(configFile.getParent(), 
            configFile.getName() + ".backup." + timestamp);
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        File simpleBackup = new File(configFile.getParent(), configFile.getName() + ".backup");
        Files.copy(configFile.toPath(), simpleBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    private FileConfiguration mergeConfigs(FileConfiguration userConfig, FileConfiguration defaultConfig) {
        FileConfiguration merged = new YamlConfiguration();
        
        for (String key : defaultConfig.getKeys(true)) {
            if (!defaultConfig.isConfigurationSection(key)) {
                merged.set(key, defaultConfig.get(key));
            }
        }
        
        for (String key : userConfig.getKeys(true)) {
            if (!userConfig.isConfigurationSection(key)) {
                merged.set(key, userConfig.get(key));
            }
        }
        
        return merged;
    }
    
    public void checkLanguageVersion(File languageFile, String resourcePath) {
        if (!languageFile.exists()) return;
        
        FileConfiguration userLang = YamlConfiguration.loadConfiguration(languageFile);
        
        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream == null) return;
        
        FileConfiguration defaultLang = YamlConfiguration.loadConfiguration(
            new InputStreamReader(defaultStream)
        );
        
        String userVersion = userLang.getString("language-version", "unknown");
        String defaultVersion = defaultLang.getString("language-version", "unknown");
        
        if (!userVersion.equals(defaultVersion)) {
            plugin.getLogger().warning("========================================");
            plugin.getLogger().warning("LANGUAGE FILE VERSION MISMATCH");
            plugin.getLogger().warning("File: " + languageFile.getName());
            plugin.getLogger().warning("Your version: " + userVersion + " | Current: " + defaultVersion);
            plugin.getLogger().warning("");
            plugin.getLogger().warning("Options:");
            plugin.getLogger().warning("  1. Delete " + languageFile.getName() + " to regenerate");
            plugin.getLogger().warning("  2. Manually update your translations");
            plugin.getLogger().warning("  3. Ignore this (not recommended)");
            plugin.getLogger().warning("========================================");
        }
    }
}

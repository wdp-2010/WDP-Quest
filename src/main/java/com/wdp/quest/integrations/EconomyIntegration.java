package com.wdp.quest.integrations;

import com.wdp.quest.WDPQuestPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Integration with Vault economy (SkillCoins via AuraSkills)
 */
public class EconomyIntegration {
    
    private final WDPQuestPlugin plugin;
    private Economy economy;
    private boolean enabled = false;
    
    // Direct AuraSkills integration for tokens
    private Object auraSkillsPlugin;
    private java.lang.reflect.Method getEconomyMethod;
    private java.lang.reflect.Method addBalanceMethod;
    private java.lang.reflect.Method getBalanceMethod;
    private Object tokensEnum;
    
    public EconomyIntegration(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize Vault economy connection
     */
    public boolean initialize() {
        // Try Vault first
        if (setupVault()) {
            plugin.getLogger().info("Successfully hooked into Vault economy!");
            
            // Also try to setup direct AuraSkills for tokens
            setupAuraSkillsDirect();
            
            enabled = true;
            return true;
        }
        
        plugin.getLogger().warning("Vault not found - economy rewards disabled!");
        return false;
    }
    
    private boolean setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = 
                Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Setup direct AuraSkills integration for token rewards
     */
    private void setupAuraSkillsDirect() {
        try {
            auraSkillsPlugin = Bukkit.getPluginManager().getPlugin("AuraSkills");
            if (auraSkillsPlugin == null) {
                plugin.getLogger().info("AuraSkills not found - token rewards disabled.");
                return;
            }
            
            // Get the economy provider via reflection
            getEconomyMethod = auraSkillsPlugin.getClass().getMethod("getSkillCoinsEconomy");
            Object economyProvider = getEconomyMethod.invoke(auraSkillsPlugin);
            
            if (economyProvider != null) {
                // Get CurrencyType.TOKENS
                Class<?> currencyTypeClass = Class.forName("dev.aurelium.auraskills.common.skillcoins.CurrencyType");
                tokensEnum = Enum.valueOf((Class<Enum>) currencyTypeClass, "TOKENS");
                
                // Get methods
                addBalanceMethod = economyProvider.getClass().getMethod("addBalance", UUID.class, currencyTypeClass, double.class);
                getBalanceMethod = economyProvider.getClass().getMethod("getBalance", UUID.class, currencyTypeClass);
                
                plugin.getLogger().info("Direct AuraSkills integration enabled for token rewards!");
            }
        } catch (Exception e) {
            plugin.getLogger().info("Could not setup direct AuraSkills integration: " + e.getMessage());
            auraSkillsPlugin = null;
        }
    }
    
    /**
     * Give coins to a player (via Vault/SkillCoins)
     */
    public boolean giveCoins(Player player, double amount) {
        if (!enabled || economy == null) return false;
        
        economy.depositPlayer(player, amount);
        return true;
    }
    
    /**
     * Give tokens to a player (direct AuraSkills)
     */
    public boolean giveTokens(Player player, double amount) {
        if (auraSkillsPlugin == null || addBalanceMethod == null) return false;
        
        try {
            Object economyProvider = getEconomyMethod.invoke(auraSkillsPlugin);
            if (economyProvider != null) {
                addBalanceMethod.invoke(economyProvider, player.getUniqueId(), tokensEnum, amount);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give tokens: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if player has enough coins
     */
    public boolean hasCoins(Player player, double amount) {
        if (!enabled || economy == null) return true;
        return economy.has(player, amount);
    }
    
    /**
     * Take coins from player
     */
    public boolean takeCoins(Player player, double amount) {
        if (!enabled || economy == null) return false;
        if (!hasCoins(player, amount)) return false;
        
        economy.withdrawPlayer(player, amount);
        return true;
    }
    
    /**
     * Get player's coin balance
     */
    public double getCoins(Player player) {
        if (!enabled || economy == null) return 0;
        return economy.getBalance(player);
    }
    
    /**
     * Get player's token balance
     */
    public double getTokens(Player player) {
        if (auraSkillsPlugin == null || getBalanceMethod == null) return 0;
        
        try {
            Object economyProvider = getEconomyMethod.invoke(auraSkillsPlugin);
            if (economyProvider != null) {
                return (double) getBalanceMethod.invoke(economyProvider, player.getUniqueId(), tokensEnum);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get tokens: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Format currency amount
     */
    public String formatCoins(double amount) {
        if (!enabled || economy == null) return String.format("%.0f", amount);
        return economy.format(amount);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean hasTokenSupport() {
        return auraSkillsPlugin != null && addBalanceMethod != null;
    }
    
    public Economy getEconomy() {
        return economy;
    }
}

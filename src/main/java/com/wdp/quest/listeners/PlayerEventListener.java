package com.wdp.quest.listeners;

import com.wdp.quest.WDPQuestPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit for data loading/saving
 */
public class PlayerEventListener implements Listener {
    
    private final WDPQuestPlugin plugin;
    
    public PlayerEventListener(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getPlayerQuestManager().loadPlayer(event.getPlayer().getUniqueId());
        });
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Unload and save player data
        plugin.getPlayerQuestManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}

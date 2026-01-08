package com.wdp.quest.commands;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.ui.QuestMenuHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main quest command handler
 */
public class QuestCommand implements CommandExecutor, TabCompleter {
    
    private final WDPQuestPlugin plugin;
    private final QuestMenuHandler menuHandler;
    
    public QuestCommand(WDPQuestPlugin plugin) {
        this.plugin = plugin;
        this.menuHandler = new QuestMenuHandler(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("commands.players-only"));
            return true;
        }
        
        if (!player.hasPermission("wdp.quest.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        // No args - open main menu
        if (args.length == 0) {
            // Check if there are completed active quests waiting for claim
            var playerData = plugin.getPlayerQuestManager().getPlayerData(player);
            List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
            boolean hasReadyToClaim = false;
            
            for (Quest quest : dailyQuests) {
                if (playerData.isQuestActive(quest.getId())) {
                    var progress = playerData.getQuestProgress(quest.getId());
                    if (progress != null) {
                        var targets = new java.util.LinkedHashMap<String, Integer>();
                        for (var obj : quest.getObjectives()) {
                            targets.put(obj.getId(), obj.getTargetAmount());
                        }
                        double completion = progress.getActualCompletionPercentage(targets);
                        if (completion >= 100.0) {
                            hasReadyToClaim = true;
                            break;
                        }
                    }
                }
            }
            
            if (hasReadyToClaim) {
                player.sendMessage(plugin.getMessages().get("quests.ready-to-claim"));
            }
            
            menuHandler.openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list" -> handleList(player, args);
            case "track" -> handleTrack(player, args);
            case "abandon" -> handleAbandon(player, args);
            case "info" -> handleInfo(player, args);
            case "active" -> handleActive(player);
            case "completed" -> handleCompleted(player);
            case "updates" -> handleUpdates(player);
            default -> {
                // Try to treat as quest ID
                Quest quest = plugin.getQuestManager().getQuest(args[0]);
                if (quest != null) {
                    menuHandler.openQuestDetail(player, quest);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("quest-not-found"));
                }
            }
        }
        
        return true;
    }
    
    private void handleList(Player player, String[] args) {
        // Just open the main menu (shows 5 daily quests)
        menuHandler.openMainMenu(player);
    }
    
    private void handleTrack(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().get("commands.usage.track"));
            return;
        }
        
        String questId = args[1];
        var data = plugin.getPlayerQuestManager().getPlayerData(player);
        
        if (!data.isQuestActive(questId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("quest-not-active"));
            return;
        }
        
        data.setTrackedQuestId(questId);
        Quest quest = plugin.getQuestManager().getQuest(questId);
        String questName = quest != null ? quest.getDisplayName() : questId;
        player.sendMessage(plugin.getMessages().get("commands.now-tracking", "quest", questName));
    }
    
    private void handleAbandon(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().get("commands.usage.abandon"));
            return;
        }
        
        plugin.getPlayerQuestManager().abandonQuest(player, args[1]);
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessages().get("commands.usage.info"));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(args[1]);
        if (quest == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("quest-not-found"));
            return;
        }
        
        menuHandler.openQuestDetail(player, quest);
    }
    
    private void handleActive(Player player) {
        // Active quests are now shown in the main menu
        menuHandler.openMainMenu(player);
    }
    
    private void handleCompleted(Player player) {
        // Completed quests are marked with checkmarks in the main menu
        menuHandler.openMainMenu(player);
    }
    
    private void handleUpdates(Player player) {
        plugin.getSmartProgressTracker().toggleUpdates(player.getUniqueId());
        boolean enabled = plugin.getSmartProgressTracker().isEnabled(player.getUniqueId());
        
        if (enabled) {
            player.sendMessage(plugin.getMessages().get("progress-updates-enabled"));
        } else {
            player.sendMessage(plugin.getMessages().get("progress-updates-disabled"));
        }
    }
    
    public QuestMenuHandler getMenuHandler() {
        return menuHandler;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("track", "abandon", "info", "updates"));
            // Add quest IDs
            completions.addAll(plugin.getQuestManager().getQuestIds());
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "track", "abandon" -> {
                    var data = plugin.getPlayerQuestManager().getPlayerData(((Player) sender).getUniqueId());
                    for (var progress : data.getActiveQuests()) {
                        completions.add(progress.getQuestId());
                    }
                }
                case "info" -> completions.addAll(plugin.getQuestManager().getQuestIds());
            }
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}

package com.wdp.quest.commands;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.data.PlayerQuestData;
import com.wdp.quest.quest.Quest;
import org.bukkit.Bukkit;
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
 * Admin quest command handler
 */
public class QuestAdminCommand implements CommandExecutor, TabCompleter {
    
    private final WDPQuestPlugin plugin;
    
    public QuestAdminCommand(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wdp.quest.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "give" -> handleGive(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "reset" -> handleReset(sender, args);
            case "progress" -> handleProgress(sender, args);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m                    &r &6Quest Admin &8&m                    "));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin reload &7- Reload configuration"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin give <player> <quest> &7- Give quest to player"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin complete <player> <quest> &7- Complete quest for player"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin reset <player> [quest] &7- Reset quest(s) for player"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin progress <player> &7- View player's quest progress"));
        sender.sendMessage(plugin.getConfigManager().colorize("&e/questadmin list &7- List all loaded quests"));
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m                                                          "));
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /questadmin give <player> <quest>"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(args[2]);
        if (quest == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("quest-not-found"));
            return;
        }
        
        // Force start quest (bypass requirements)
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(quest.getId());
        data.addQuestProgress(progress);
        plugin.getDatabaseManager().savePlayerQuest(target.getUniqueId(), progress);
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&aGave quest &e" + quest.getDisplayName() + " &ato &e" + target.getName()));
        target.sendMessage(plugin.getConfigManager().getMessage("quest-started")
            .replace("%quest%", quest.getDisplayName()));
    }
    
    private void handleComplete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /questadmin complete <player> <quest>"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(args[2]);
        if (quest == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("quest-not-found"));
            return;
        }
        
        // Check if player has quest active
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        if (!data.isQuestActive(quest.getId())) {
            // Start quest first
            PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(quest.getId());
            data.addQuestProgress(progress);
        }
        
        // Complete all objectives
        var progress = data.getQuestProgress(quest.getId());
        for (var objective : quest.getObjectives()) {
            progress.setObjectiveProgress(objective.getId(), objective.getTargetAmount(), true);
        }
        
        // Complete quest
        plugin.getPlayerQuestManager().completeQuest(target, quest);
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&aCompleted quest &e" + quest.getDisplayName() + " &afor &e" + target.getName()));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /questadmin reset <player> [quest]"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        
        if (args.length >= 3) {
            // Reset specific quest
            String questId = args[2];
            data.removeQuest(questId);
            plugin.getDatabaseManager().deletePlayerQuest(target.getUniqueId(), questId);
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&aReset quest &e" + questId + " &afor &e" + target.getName()));
        } else {
            // Reset all quests
            for (var progress : new ArrayList<>(data.getActiveQuests())) {
                data.removeQuest(progress.getQuestId());
                plugin.getDatabaseManager().deletePlayerQuest(target.getUniqueId(), progress.getQuestId());
            }
            for (var progress : new ArrayList<>(data.getCompletedQuests())) {
                data.removeQuest(progress.getQuestId());
                plugin.getDatabaseManager().deletePlayerQuest(target.getUniqueId(), progress.getQuestId());
            }
            sender.sendMessage(plugin.getConfigManager().colorize(
                "&aReset all quests for &e" + target.getName()));
        }
    }
    
    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().colorize("&cUsage: /questadmin progress <player>"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        double progress = plugin.getProgressIntegration().getPlayerProgress(target);
        
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m              &r &6" + target.getName() + "'s Quest Progress &8&m              "));
        sender.sendMessage(plugin.getConfigManager().colorize("&7Player Progress: &e" + String.format("%.1f", progress) + "%"));
        sender.sendMessage(plugin.getConfigManager().colorize("&7Active Quests: &e" + data.getActiveQuestCount()));
        sender.sendMessage(plugin.getConfigManager().colorize("&7Completed Quests: &e" + data.getCompletedQuestCount()));
        
        if (!data.getActiveQuests().isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().colorize("&7Active:"));
            for (var questProgress : data.getActiveQuests()) {
                Quest quest = plugin.getQuestManager().getQuest(questProgress.getQuestId());
                String name = quest != null ? quest.getDisplayName() : questProgress.getQuestId();
                double completion = 0;
                if (quest != null) {
                    var targets = new java.util.LinkedHashMap<String,Integer>();
                    for (var obj : quest.getObjectives()) targets.put(obj.getId(), obj.getTargetAmount());
                    completion = targets.isEmpty() ? 100.0 : questProgress.getActualCompletionPercentage(targets);
                }
                sender.sendMessage(plugin.getConfigManager().colorize(
                    "  &8- &e" + name + " &7(" + String.format("%.0f", completion) + "%)"));
            }
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m                                                              "));
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m                    &r &6Loaded Quests &8&m                    "));
        
        for (var category : com.wdp.quest.quest.QuestCategory.values()) {
            var quests = plugin.getQuestManager().getQuestsByCategory(category);
            sender.sendMessage(plugin.getConfigManager().colorize(
                category.getColor() + category.getDisplayName() + " &7(" + quests.size() + " quests)"));
        }
        
        sender.sendMessage(plugin.getConfigManager().colorize(
            "&7Total: &e" + plugin.getQuestManager().getQuestCount() + " quests"));
        sender.sendMessage(plugin.getConfigManager().colorize("&8&m                                                          "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "give", "complete", "reset", "progress", "list"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give", "complete", "reset", "progress" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give", "complete", "reset" -> completions.addAll(plugin.getQuestManager().getQuestIds());
            }
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}

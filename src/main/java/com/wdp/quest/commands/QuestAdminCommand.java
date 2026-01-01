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
        sender.sendMessage(plugin.getMessages().get("commands.admin.help-header"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.reload"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.give"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.complete"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.reset"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.progress"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help.list"));
        sender.sendMessage(plugin.getMessages().get("commands.admin.help-footer"));
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().get("commands.admin.usage.give"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get("errors.player-not-found"));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(args[2]);
        if (quest == null) {
            sender.sendMessage(plugin.getMessages().get("quests.not-found"));
            return;
        }
        
        // Force start quest (bypass requirements)
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(quest.getId());
        data.addQuestProgress(progress);
        plugin.getDatabaseManager().savePlayerQuest(target.getUniqueId(), progress);
        
        sender.sendMessage(plugin.getMessages().get("commands.admin.gave-quest",
            "quest", quest.getDisplayName(), "player", target.getName()));
        target.sendMessage(plugin.getMessages().get("quests.started", "quest", quest.getDisplayName()));
    }
    
    private void handleComplete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().get("commands.admin.usage.complete"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get("errors.player-not-found"));
            return;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(args[2]);
        if (quest == null) {
            sender.sendMessage(plugin.getMessages().get("quests.not-found"));
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
        
        sender.sendMessage(plugin.getMessages().get("commands.admin.completed-quest",
            "quest", quest.getDisplayName(), "player", target.getName()));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessages().get("commands.admin.usage.reset"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get("errors.player-not-found"));
            return;
        }
        
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        
        if (args.length >= 3) {
            // Reset specific quest
            String questId = args[2];
            data.removeQuest(questId);
            plugin.getDatabaseManager().deletePlayerQuest(target.getUniqueId(), questId);
            sender.sendMessage(plugin.getMessages().get("commands.admin.reset-quest",
                "quest", questId, "player", target.getName()));
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
            sender.sendMessage(plugin.getMessages().get("commands.admin.reset-all-quests",
                "player", target.getName()));
        }
    }
    
    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessages().get("commands.admin.usage.progress"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get("errors.player-not-found"));
            return;
        }
        
        var data = plugin.getPlayerQuestManager().getPlayerData(target);
        double progress = plugin.getProgressIntegration().getPlayerProgress(target);
        
        sender.sendMessage(plugin.getMessages().get("commands.admin.progress-header", "player", target.getName()));
        sender.sendMessage(plugin.getMessages().get("commands.admin.progress-level", "progress", String.format("%.1f", progress)));
        sender.sendMessage(plugin.getMessages().get("commands.admin.active-quests", "count", String.valueOf(data.getActiveQuestCount())));
        sender.sendMessage(plugin.getMessages().get("commands.admin.completed-quests", "count", String.valueOf(data.getCompletedQuestCount())));
        
        if (!data.getActiveQuests().isEmpty()) {
            sender.sendMessage(plugin.getMessages().get("commands.admin.active-label"));
            for (var questProgress : data.getActiveQuests()) {
                Quest quest = plugin.getQuestManager().getQuest(questProgress.getQuestId());
                String name = quest != null ? quest.getDisplayName() : questProgress.getQuestId();
                double completion = 0;
                if (quest != null) {
                    var targets = new java.util.LinkedHashMap<String,Integer>();
                    for (var obj : quest.getObjectives()) targets.put(obj.getId(), obj.getTargetAmount());
                    completion = targets.isEmpty() ? 100.0 : questProgress.getActualCompletionPercentage(targets);
                }
                sender.sendMessage(plugin.getMessages().get("commands.admin.active-quest-entry",
                    "quest", name, "completion", String.format("%.0f", completion)));
            }
        }
        
        sender.sendMessage(plugin.getMessages().get("commands.admin.progress-footer"));
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().get("commands.admin.list-header"));
        
        for (var category : com.wdp.quest.quest.QuestCategory.values()) {
            var quests = plugin.getQuestManager().getQuestsByCategory(category);
            sender.sendMessage(plugin.getMessages().get("commands.admin.list-category",
                "color", category.getColor(), "category", category.getDisplayName(), "count", String.valueOf(quests.size())));
        }
        
        sender.sendMessage(plugin.getMessages().get("commands.admin.list-total",
            "count", String.valueOf(plugin.getQuestManager().getQuestCount())));
        sender.sendMessage(plugin.getMessages().get("commands.admin.list-footer"));
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

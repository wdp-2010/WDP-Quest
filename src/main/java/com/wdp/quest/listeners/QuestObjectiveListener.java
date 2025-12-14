package com.wdp.quest.listeners;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.data.PlayerQuestData;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.quest.QuestObjective;
import com.wdp.quest.quest.QuestObjective.ObjectiveType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Listens for events that can progress quest objectives
 */
public class QuestObjectiveListener implements Listener {
    
    private final WDPQuestPlugin plugin;
    
    public QuestObjectiveListener(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        processObjective(event.getPlayer(), ObjectiveType.MINE, event.getBlock().getType(), 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        processObjective(event.getPlayer(), ObjectiveType.PLACE, event.getBlock().getType(), 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        processObjective(killer, ObjectiveType.KILL, event.getEntityType(), 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack result = event.getRecipe().getResult();
        int amount = result.getAmount();
        
        // Handle shift-click crafting
        if (event.isShiftClick()) {
            int maxCraft = getMaxCraftAmount(event);
            amount *= maxCraft;
        }
        
        processObjective(player, ObjectiveType.CRAFT, result.getType(), amount);
    }
    
    private int getMaxCraftAmount(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        int maxAmount = result.getMaxStackSize();
        
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && ingredient.getType() != Material.AIR) {
                maxAmount = Math.min(maxAmount, ingredient.getAmount());
            }
        }
        
        return maxAmount;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        processObjective(event.getPlayer(), ObjectiveType.SMELT, 
            event.getBlock().getType(), event.getItemAmount());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            processObjective(event.getPlayer(), ObjectiveType.FISH, null, 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            processObjective(player, ObjectiveType.BREED, null, 1);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        processObjective(event.getEnchanter(), ObjectiveType.ENCHANT, null, 1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelChange(PlayerLevelChangeEvent event) {
        int gained = event.getNewLevel() - event.getOldLevel();
        if (gained > 0) {
            processObjective(event.getPlayer(), ObjectiveType.LEVEL_UP, null, gained);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String advancementKey = event.getAdvancement().getKey().getKey();
        // Skip recipe advancements
        if (advancementKey.startsWith("recipes/")) return;
        
        processObjective(event.getPlayer(), ObjectiveType.ADVANCEMENT, advancementKey, 1);
    }
    
    /**
     * Process an objective action for all active quests.
     * Also auto-starts daily quests if the action matches their objectives.
     */
    private void processObjective(Player player, ObjectiveType type, Object data, int amount) {
        var playerData = plugin.getPlayerQuestManager().getPlayerData(player);
        
        // First, check if any daily quests can be auto-started
        autoStartMatchingDailyQuests(player, playerData, type, data);
        
        // Then process active quests as normal
        for (PlayerQuestData.QuestProgress progress : playerData.getActiveQuests()) {
            Quest quest = plugin.getQuestManager().getQuest(progress.getQuestId());
            if (quest == null) continue;
            
            for (QuestObjective objective : quest.getObjectives()) {
                // Skip completed objectives
                if (progress.isObjectiveComplete(objective.getId())) continue;
                
                // Check if this action matches the objective
                if (objective.matches(type, data)) {
                    plugin.getPlayerQuestManager().updateObjective(player, quest, objective, amount);
                }
            }
        }
    }
    
    /**
     * Auto-start daily quests that match the current action.
     * This allows quests to start automatically when a player performs a relevant action.
     */
    private void autoStartMatchingDailyQuests(Player player, PlayerQuestData playerData, ObjectiveType type, Object data) {
        // Get today's daily quests for this player
        List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
        
        for (Quest quest : dailyQuests) {
            // Skip if already active
            if (playerData.isQuestActive(quest.getId())) continue;
            
            // Skip if already completed
            if (playerData.isQuestCompleted(quest.getId())) continue;
            
            // Check if any objective matches this action
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.matches(type, data)) {
                    // Auto-start this quest!
                    plugin.getPlayerQuestManager().startQuest(player, quest);
                    plugin.getLogger().info("Auto-started quest '" + quest.getDisplayName() + "' for " + player.getName());
                    break; // Only start once per quest
                }
            }
        }
    }
    
    /**
     * Manually trigger a custom objective (for integrations)
     */
    public void triggerCustomObjective(Player player, String customData, int amount) {
        processObjective(player, ObjectiveType.CUSTOM, customData, amount);
    }
    
    /**
     * Manually trigger a visit objective
     */
    public void triggerVisitObjective(Player player, String location) {
        processObjective(player, ObjectiveType.VISIT, location, 1);
    }
}

package com.wdp.quest.ui;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.ui.QuestMenuHandler.MenuState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles inventory click events for quest menus.
 * Updated for SkillCoins-style bottom navigation layout.
 * Layout:
 * - Row 0: Quest 1 (slot 0 = icon, slots 1-8 = progress bar)
 * - Row 1: Separator
 * - Row 2: Quest 2 (slot 18 = icon, slots 19-26 = progress bar)
 * - Row 3: Separator
 * - Row 4: Quest 3 (slot 36 = icon, slots 37-44 = progress bar)
 * - Row 5: Bottom navigation (slots 45-53)
 */
public class QuestMenuListener implements Listener {
    
    private final WDPQuestPlugin plugin;
    private final QuestMenuHandler menuHandler;
    
    // Quest icon slots in main menu (slot 0, 18, 36 for the 3 quests per page)
    private static final int QUEST_SLOT_1 = 0;
    private static final int QUEST_SLOT_2 = 18;
    private static final int QUEST_SLOT_3 = 36;
    
    // Bottom navigation slots (row 5)
    private static final int PREV_PAGE_SLOT = 48;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int CLOSE_SLOT = 53;
    
    // Quests per page
    private static final int QUESTS_PER_PAGE = 3;
    
    public QuestMenuListener(WDPQuestPlugin plugin) {
        this.plugin = plugin;
        
        // Get the menu handler from the command
        var questCmd = plugin.getCommand("quest");
        if (questCmd != null && questCmd.getExecutor() instanceof com.wdp.quest.commands.QuestCommand cmd) {
            this.menuHandler = cmd.getMenuHandler();
        } else {
            this.menuHandler = new QuestMenuHandler(plugin);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        MenuState state = menuHandler.getMenuState(player.getUniqueId());
        if (state == null) return;
        
        // Always cancel the event for our menus to prevent item movement
        event.setCancelled(true);
        
        // Ignore clicks outside the top inventory
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (isFillerItem(clicked)) return;
        
        int slot = event.getRawSlot();
        
        // Play click sound
        player.playSound(player.getLocation(), plugin.getConfigManager().getSound("click"), 0.5f, 1.0f);
        
        switch (state.type) {
            case MAIN -> handleMainMenuClick(player, slot, clicked, state);
            case DETAIL -> handleDetailMenuClick(player, slot, clicked, state);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        MenuState state = menuHandler.getMenuState(player.getUniqueId());
        if (state == null) return;
        
        // Cancel all drag events in our menus
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            menuHandler.clearMenuState(player.getUniqueId());
        }
    }
    
    private void handleMainMenuClick(Player player, int slot, ItemStack clicked, MenuState state) {
        List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
        int page = state.page;
        int startIndex = page * QUESTS_PER_PAGE;
        int totalPages = (int) Math.ceil((double) dailyQuests.size() / QUESTS_PER_PAGE);
        
        // Quest slot 1 (slot 9)
        if (slot == QUEST_SLOT_1) {
            int questIndex = startIndex;
            if (questIndex < dailyQuests.size()) {
                Quest quest = dailyQuests.get(questIndex);
                menuHandler.openQuestDetail(player, quest, page, true);
            }
            return;
        }
        
        // Quest slot 2 (slot 27)
        if (slot == QUEST_SLOT_2) {
            int questIndex = startIndex + 1;
            if (questIndex < dailyQuests.size()) {
                Quest quest = dailyQuests.get(questIndex);
                menuHandler.openQuestDetail(player, quest, page, true);
            }
            return;
        }
        
        // Quest slot 3 (slot 45)
        if (slot == QUEST_SLOT_3) {
            int questIndex = startIndex + 2;
            if (questIndex < dailyQuests.size()) {
                Quest quest = dailyQuests.get(questIndex);
                menuHandler.openQuestDetail(player, quest, page, true);
            }
            return;
        }
        
        // Previous page button
        if (slot == PREV_PAGE_SLOT && page > 0) {
            menuHandler.openMainMenu(player, page - 1, true);
            return;
        }
        
        // Close button
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        
        // Next page button
        if (slot == NEXT_PAGE_SLOT && page < totalPages - 1) {
            menuHandler.openMainMenu(player, page + 1, true);
            return;
        }
    }
    
    private void handleDetailMenuClick(Player player, int slot, ItemStack clicked, MenuState state) {
        String questId = state.data;
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return;
        
        var playerData = plugin.getPlayerQuestManager().getPlayerData(player);
        boolean isActive = playerData.isQuestActive(questId);
        boolean isCompleted = playerData.isQuestCompleted(questId);
                
        switch (slot) {
            // Back button (slot 53)
            case 53 -> menuHandler.openMainMenu(player, state.page, true);
            
            // Main action button (slot 49) - context-dependent (tracking/repeat only)
            case 49 -> {
                Material type = clicked.getType();
                
                // Track/Untrack (Ender Eye/Pearl) - when active
                if ((type == Material.ENDER_EYE || type == Material.ENDER_PEARL) && isActive) {
                    if (playerData.isTracking(questId)) {
                        playerData.setTrackedQuestId(null);
                        player.sendMessage(plugin.getMessages().get("commands.stopped-tracking"));
                    } else {
                        playerData.setTrackedQuestId(questId);
                        player.sendMessage(plugin.getMessages().get("commands.now-tracking", "quest", quest.getDisplayName()));
                    }
                    menuHandler.openQuestDetail(player, quest, state.page, true);
                }
                // Repeat quest (Experience Bottle)
                else if (type == Material.EXPERIENCE_BOTTLE && isCompleted && quest.isRepeatable() && 
                         !playerData.isOnCooldown(questId)) {
                    playerData.removeQuest(questId);
                    if (plugin.getPlayerQuestManager().startQuest(player, quest)) {
                        menuHandler.openQuestDetail(player, quest, state.page, true);
                    }
                }
            }
            
            // Abandon button (slot 50) - only when active
            case 50 -> {
                if (isActive) {
                    plugin.getPlayerQuestManager().abandonQuest(player, questId);
                    menuHandler.openQuestDetail(player, quest, state.page, true);
                }
            }
        }
    }
    
    private boolean isFillerItem(ItemStack item) {
        if (item == null) return true;
        Material type = item.getType();
        return type == Material.BLACK_STAINED_GLASS_PANE ||
               type == Material.GRAY_STAINED_GLASS_PANE ||
               type == Material.LIGHT_GRAY_STAINED_GLASS_PANE ||
               type == Material.WHITE_STAINED_GLASS_PANE ||
               type == Material.RED_STAINED_GLASS_PANE ||
               type == Material.ORANGE_STAINED_GLASS_PANE ||
               type == Material.YELLOW_STAINED_GLASS_PANE ||
               type == Material.LIME_STAINED_GLASS_PANE;
    }
}

package com.wdp.quest.ui;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.data.PlayerQuestData;
import com.wdp.quest.quest.Quest;
import com.wdp.quest.quest.QuestObjective;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles quest menu creation and display.
 * Layout: Double chest (54 slots) with 3 quests per page.
 * Each quest has 1 icon slot + 8 progress bar segments.
 * Progress bar uses Custom Model Data for resource pack textures.
 */
public class QuestMenuHandler {
    
    private final WDPQuestPlugin plugin;
    private final Map<UUID, MenuState> openMenus = new HashMap<>();
    
    // Progress bar Custom Model Data values
    // Simple format: 100X for normal (1000-1005), 101X for hard (1010-1015)
    // All segments use the same base, fill level is 0-5
    private static final int CMD_NORMAL = 1000;
    private static final int CMD_HARD = 1010;
    
    // Total progress units: 8 segments × 5 fills each = 40 units = 100%
    private static final int SEGMENTS = 8;
    private static final int FILLS_PER_SEGMENT = 5;
    
    public QuestMenuHandler(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Opens the main quest menu - double chest with 3 quests per page.
     * Layout:
     * Row 0: Header (player head, title, currency)
     * Row 1: Quest 1 (icon + 8 progress segments)
     * Row 2: Separator
     * Row 3: Quest 2 (icon + 8 progress segments)
     * Row 4: Separator  
     * Row 5: Quest 3 (icon + 8 progress segments) OR navigation
     */
    public void openMainMenu(Player player) {
        openMainMenu(player, 0, false);
    }
    
    public void openMainMenu(Player player, int page) {
        openMainMenu(player, page, false);
    }
    
    public void openMainMenu(Player player, int page, boolean updateOnly) {
        Inventory inv = Bukkit.createInventory(player, 54,
            plugin.getConfigManager().getMainMenuTitle());
        
        double playerProgress = plugin.getProgressIntegration().getPlayerProgress(player);
        PlayerQuestData playerData = plugin.getPlayerQuestManager().getPlayerData(player);
        List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
        
        fillBackground(inv, Material.BLACK_STAINED_GLASS_PANE);
        
        double coins = plugin.getEconomyIntegration().getCoins(player);
        int tokens = (int) plugin.getEconomyIntegration().getTokens(player);
        
        // === QUESTS (3 per page) ===
        int questsPerPage = 3;
        int startIndex = page * questsPerPage;
        int totalPages = (int) Math.ceil((double) dailyQuests.size() / questsPerPage);
        
        // Quest row positions: row 0 (slots 0-8), row 2 (slots 18-26), row 4 (slots 36-44)
        int[] questRowStarts = {0, 18, 36};
        
        for (int i = 0; i < questsPerPage && (startIndex + i) < dailyQuests.size(); i++) {
            Quest quest = dailyQuests.get(startIndex + i);
            int rowStart = questRowStarts[i];
            
            // Quest icon (first slot of row)
            inv.setItem(rowStart, createQuestIcon(quest, playerProgress, playerData));
            
            // Progress bar (8 segments)
            double completion = getQuestCompletion(quest, playerData);
            boolean isHard = quest.isHardQuest();
            
            for (int seg = 0; seg < SEGMENTS; seg++) {
                int slot = rowStart + 1 + seg;
                inv.setItem(slot, createProgressSegment(seg, completion, isHard, SEGMENTS));
            }
        }
        
        // === SEPARATORS (rows 1 and 3) ===
        fillRow(inv, 1, Material.GRAY_STAINED_GLASS_PANE);
        fillRow(inv, 3, Material.GRAY_STAINED_GLASS_PANE);
        
        // === BOTTOM NAVIGATION BAR (row 5, slots 45-53) ===
        applyNavbar(inv, player, "main_menu", page, totalPages, startIndex, questsPerPage, coins, tokens);
        
        openMenus.put(player.getUniqueId(), new MenuState(MenuType.MAIN, null, page));
        
        if (updateOnly) {
            // Update existing inventory to prevent close event
            Inventory existingInv = player.getOpenInventory().getTopInventory();
            if (existingInv != null && existingInv.getSize() == 54) {
                existingInv.clear();
                for (int i = 0; i < inv.getSize(); i++) {
                    existingInv.setItem(i, inv.getItem(i));
                }
                player.updateInventory();
                return;
            }
        }
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), plugin.getConfigManager().getSound("open-menu"), 0.5f, 1.0f);
    }
    
    /**
     * Opens the quest detail view.
     */
    public void openQuestDetail(Player player, Quest quest) {
        openQuestDetail(player, quest, 0, false);
    }
    
    /**
     * Opens the quest detail view, remembering the page we came from.
     */
    public void openQuestDetail(Player player, Quest quest, int fromPage) {
        openQuestDetail(player, quest, fromPage, false);
    }
    
    /**
     * Opens the quest detail view, remembering the page we came from.
     * @param updateOnly If true, updates existing inventory instead of creating new one
     */
    public void openQuestDetail(Player player, Quest quest, int fromPage, boolean updateOnly) {
        Inventory inv = Bukkit.createInventory(null, 54,
            plugin.getConfigManager().getDetailMenuTitle(quest.getDisplayName()));
        
        PlayerQuestData playerData = plugin.getPlayerQuestManager().getPlayerData(player);
        PlayerQuestData.QuestProgress questProgress = playerData.getQuestProgress(quest.getId());
        boolean isActive = playerData.isQuestActive(quest.getId());
        boolean isCompleted = playerData.isQuestCompleted(quest.getId());
        
        fillBackground(inv, Material.GRAY_STAINED_GLASS_PANE);
        
        // === ROW 0: Header ===
        List<String> iconLore = new ArrayList<>();
        iconLore.add("§7" + quest.getDescription());
        iconLore.add(" ");
        if (quest.isHardQuest()) {
            iconLore.add("§c§l⚔ HARD QUEST ⚔");
            iconLore.add(" ");
        }
        iconLore.add("§7Required Progress: §e" + quest.getRequiredProgress() + "%");
        if (quest.isRepeatable()) {
            iconLore.add("§7Repeatable: §aYes");
        }
        
        ItemStack questIcon = createItem(quest.getIcon(),
            quest.getCategory().getColor() + "§l" + quest.getDisplayName(),
            iconLore.toArray(new String[0]));
        if (quest.isHardQuest()) {
            setGlowing(questIcon);
        }
        inv.setItem(4, questIcon);
        
        // Status
        inv.setItem(8, createStatusItem(isCompleted, isActive, quest, questProgress));
        
        // === ROW 1: Full-width progress bar ===
        double completion = getQuestCompletion(quest, playerData);
        for (int seg = 0; seg < 9; seg++) {
            int slot = 9 + seg; // slots 9-17
            inv.setItem(slot, createProgressSegment(seg, completion, quest.isHardQuest(), 9));
        }
        
        // === ROW 2: Objectives ===
        inv.setItem(18, createItem(Material.PAPER, "§6§lObjectives", "§7Complete all to finish"));
        
        int objSlot = 19;
        for (QuestObjective objective : quest.getObjectives()) {
            if (objSlot > 25) break;
            
            boolean objComplete = questProgress != null && questProgress.isObjectiveComplete(objective.getId());
            int current = questProgress != null ? questProgress.getObjectiveAmount(objective.getId()) : 0;
            int target = objective.getTargetAmount();
            
            Material objMat = objComplete ? Material.LIME_DYE : Material.GRAY_DYE;
            String objStatus = objComplete ? "§a✓ " : "§7○ ";
            String progress = objComplete ? "§aComplete!" : "§7" + current + "§8/§7" + target;
            
            inv.setItem(objSlot++, createItem(objMat,
                objStatus + "§f" + objective.getFormattedDescription(), progress));
        }
        
        // === ROW 3: Rewards ===
        inv.setItem(27, createItem(Material.CHEST, "§6§lRewards", "§7What you'll receive"));
        
        int rewardSlot = 28;
        for (String reward : quest.getRewards().getRewardSummary()) {
            if (rewardSlot > 34) break;
            inv.setItem(rewardSlot++, createItem(Material.GOLD_NUGGET, reward));
        }
        
        // === ROW 5: Bottom navigation bar ===
        applyNavbar(inv, player, "detail_menu", quest, playerData, isActive, isCompleted);
        
        openMenus.put(player.getUniqueId(), new MenuState(MenuType.DETAIL, quest.getId(), fromPage));
        
        if (updateOnly) {
            // Update existing inventory to prevent close event
            Inventory existingInv = player.getOpenInventory().getTopInventory();
            if (existingInv != null && existingInv.getSize() == 54) {
                existingInv.clear();
                for (int i = 0; i < inv.getSize(); i++) {
                    existingInv.setItem(i, inv.getItem(i));
                }
                player.updateInventory();
                return;
            }
        }
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), plugin.getConfigManager().getSound("click"), 0.5f, 1.0f);
    }
    
    // === PROGRESS BAR CREATION ===
    
    /**
     * Calculate quest completion percentage.
     */
    private double getQuestCompletion(Quest quest, PlayerQuestData playerData) {
        if (playerData.isQuestCompleted(quest.getId())) {
            return 100.0;
        }

        PlayerQuestData.QuestProgress progress = playerData.getQuestProgress(quest.getId());
        if (progress == null) {
            return 0.0;
        }

        // Use actual completion percentage based on each objective's target amount
        var targets = getObjectiveTargets(quest);
        if (targets.isEmpty()) return 100.0;
        return progress.getActualCompletionPercentage(targets);
    }
    
    /**
     * Creates a progress bar segment item with proper Custom Model Data.
     * 
     * @param segmentIndex Which segment (0-7)
     * @param completion Overall completion percentage (0-100)
     * @param isHard Whether this is a hard quest (red vs green)
     * @param length Unused parameter for future use
     */
    private ItemStack createProgressSegment(int segmentIndex, double completion, boolean isHard, int length) {
        // Convert percentage to units (0-40)

        int TOTAL_UNITS = length * FILLS_PER_SEGMENT; // 40
        int totalFilledUnits = (int) Math.round(completion / 100.0 * TOTAL_UNITS);
        
        // Calculate this segment's fill level (0-5)
        int unitsBeforeThis = segmentIndex * FILLS_PER_SEGMENT;
        int unitsInThisSegment = Math.max(0, Math.min(FILLS_PER_SEGMENT, totalFilledUnits - unitsBeforeThis));
        
        // Simple CMD: 1000+fill for normal, 1010+fill for hard
        int cmdBase = isHard ? CMD_HARD : CMD_NORMAL;
        
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setCustomModelData(cmdBase + unitsInThisSegment);
            
            // Visual feedback in name and lore (fallback without resource pack)
            String color = isHard ? "§c" : "§a";
            String segmentBar = createSegmentVisual(unitsInThisSegment, isHard);
            
            meta.setDisplayName(segmentBar);
            
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Overall: " + color + String.format("%.0f", completion) + "%");
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates a visual representation of a single segment (fallback).
     */
    private String createSegmentVisual(int fillLevel, boolean isHard) {
        String filled = isHard ? "§c█" : "§a█";
        String empty = "§7░";

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < FILLS_PER_SEGMENT; i++) {
            bar.append(i < fillLevel ? filled : empty);
        }
        return bar.toString();
    }

    /**
     * Build a map of objective ID -> target amount for a quest
     */
    private java.util.Map<String, Integer> getObjectiveTargets(Quest quest) {
        var map = new java.util.LinkedHashMap<String, Integer>();
        for (QuestObjective obj : quest.getObjectives()) {
            map.put(obj.getId(), obj.getTargetAmount());
        }
        return map;
    }
    
    // === ITEM CREATION HELPERS ===
    
    private ItemStack createQuestIcon(Quest quest, double playerProgress, PlayerQuestData playerData) {
        boolean canStart = playerProgress >= quest.getRequiredProgress();
        boolean isActive = playerData.isQuestActive(quest.getId());
        boolean isCompleted = playerData.isQuestCompleted(quest.getId());
        
        Material icon = quest.getIcon();
        String prefix = " ";
        
        if (isCompleted) {
            prefix = "§a✓ ";
        } else if (isActive) {
            prefix = "§e● ";
        } else if (!canStart) {
            icon = Material.BARRIER;
            prefix = "§c✖ ";
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription());
        lore.add(" ");
        
        if (quest.isHardQuest()) {
            lore.add("§c§l⚔ HARD QUEST ⚔");
            lore.add(" ");
        }
        
        // Show completion status
        double completion = getQuestCompletion(quest, playerData);
        if (isCompleted) {
            lore.add("§aCompleted!");
        } else if (isActive) {
            lore.add("§7Progress: §e" + String.format("%.0f", completion) + "%");
        } else if (!canStart) {
            lore.add("§cRequires " + quest.getRequiredProgress() + "% progress");
        } else {
            lore.add("§7Ready to start!");
            lore.add("§8Just start doing objectives");
        }
        
        // Show objectives count
        lore.add(" ");
        lore.add("§6§lObjectives: §e" + quest.getObjectives().size());
        PlayerQuestData.QuestProgress questProgress = playerData.getQuestProgress(quest.getId());
        if (questProgress != null && isActive) {
            int completed = 0;
            for (var obj : quest.getObjectives()) {
                if (questProgress.isObjectiveComplete(obj.getId())) {
                    completed++;
                }
            }
            lore.add("§7Completed: §a" + completed + "§7/§e" + quest.getObjectives().size());
        }
        
        // Show rewards preview (first 2)
        lore.add(" ");
        lore.add("§6§lRewards:");
        List<String> rewardSummary = quest.getRewards().getRewardSummary();
        int rewardCount = 0;
        for (String reward : rewardSummary) {
            if (rewardCount >= 2) {
                int remaining = rewardSummary.size() - 2;
                if (remaining > 0) {
                    lore.add("§7  ... and " + remaining + " more");
                }
                break;
            }
            lore.add("§7  " + reward);
            rewardCount++;
        }
        
        lore.add(" ");
        lore.add("§a▶ Click for details");
        
        ItemStack item = createItem(icon, 
            prefix + quest.getCategory().getColor() + quest.getDisplayName(),
            lore.toArray(new String[0]));
        
        if (isCompleted || quest.isHardQuest()) {
            setGlowing(item);
        }
        
        return item;
    }
    
    private ItemStack createStatusItem(boolean isCompleted, boolean isActive, 
                                       Quest quest, PlayerQuestData.QuestProgress questProgress) {
        Material mat;
        String name;
        String[] lore;
        
        if (isCompleted) {
            mat = Material.LIME_CONCRETE;
            name = "§a§l✓ Completed";
            lore = new String[]{"§7You have completed this quest!"};
        } else if (isActive) {
            mat = Material.YELLOW_CONCRETE;
            name = "§e§l● In Progress";
            double completion = 0;
            if (questProgress != null) {
                var targets = getObjectiveTargets(quest);
                completion = targets.isEmpty() ? 100.0 : questProgress.getActualCompletionPercentage(targets);
            }
            lore = new String[]{
                "§7Progress: §e" + String.format("%.0f", completion) + "%"
            };
        } else {
            mat = Material.GREEN_CONCRETE;
            name = "§a§l◇ Available";
            lore = new String[]{"§7Ready to start!"};
        }
        
        return createItem(mat, name, lore);
    }
    
    private ItemStack createPlayerHead(Player player, double playerProgress, PlayerQuestData playerData) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('§', "§6§l" + player.getName()));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            
            // Get daily quests info
            List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
            int completedDaily = 0;
            Quest activeHardQuest = null;
            
            for (Quest quest : dailyQuests) {
                if (playerData.isQuestCompleted(quest.getId())) {
                    completedDaily++;
                }
                if (quest.isHardQuest() && playerData.isQuestActive(quest.getId())) {
                    activeHardQuest = quest;
                }
            }
            
            // Time until reset
            String timeLeft = plugin.getDailyQuestManager().getTimeUntilResetFormatted();
            lore.add(ChatColor.translateAlternateColorCodes('§', "§7Time Left: §e" + timeLeft));
            
            // Daily quests completed out of 5
            lore.add(ChatColor.translateAlternateColorCodes('§', "§7Daily Quests: §a" + completedDaily + "§7/§a5"));
            
            // Hard quest info if present
            if (activeHardQuest != null) {
                int daysLeft = plugin.getDailyQuestManager().getRemainingDays(activeHardQuest);
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('§', "§c§l⚔ HARD QUEST ⚔"));
                lore.add(ChatColor.translateAlternateColorCodes('§', "§7Sticks for: §c" + daysLeft + "§7 more day" + (daysLeft != 1 ? "s" : "")));
            }
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            head.setItemMeta(meta);
        }
        return head;
    }
    
    private void fillBackground(Inventory inv, Material material) {
        ItemStack filler = createItem(material, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }
    
    private void fillRow(Inventory inv, int row, Material material) {
        ItemStack filler = createItem(material, " ");
        int start = row * 9;
        for (int i = 0; i < 9; i++) {
            inv.setItem(start + i, filler);
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                if (line != null && !line.isEmpty()) {
                    loreList.add(ChatColor.translateAlternateColorCodes('§', line));
                }
            }
            if (!loreList.isEmpty()) {
                meta.setLore(loreList);
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private void setGlowing(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
    }
    
    // === MENU STATE ===
    
    public MenuState getMenuState(UUID uuid) {
        return openMenus.get(uuid);
    }
    
    public void clearMenuState(UUID uuid) {
        openMenus.remove(uuid);
    }
    
    public enum MenuType {
        MAIN,
        DETAIL
    }
    
    public static class MenuState {
        public final MenuType type;
        public final String data;
        public final int page;
        
        public MenuState(MenuType type, String data, int page) {
            this.type = type;
            this.data = data;
            this.page = page;
        }
    }
    
    /**
     * Applies the navbar from config to the inventory.
     * Main menu version with page/currency context.
     */
    private void applyNavbar(Inventory inv, Player player, String menuType, 
                            int page, int totalPages, int startIndex, int questsPerPage,
                            double coins, int tokens) {
        // Fill row 5 with black glass
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        
        // Load navbar config
        var config = plugin.getConfigManager().getNavbarConfig();
        if (config == null) {
            // Fallback to hardcoded if config fails
            applyHardcodedNavbar(inv, player, page, totalPages, startIndex, questsPerPage, coins, tokens);
            return;
        }
        
        var menuConfig = config.getConfigurationSection(menuType);
        if (menuConfig == null) {
            applyHardcodedNavbar(inv, player, page, totalPages, startIndex, questsPerPage, coins, tokens);
            return;
        }
        
        // Prepare player info for player_head
        PlayerQuestData pqd = plugin.getPlayerQuestManager().getPlayerData(player);
        double playerProgress = plugin.getProgressIntegration().getPlayerProgress(player);
        
        // Apply each navbar item
        for (String key : menuConfig.getKeys(false)) {
            var itemSection = menuConfig.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = itemSection.getInt("slot");
            if (slot < 0 || slot >= 54) continue;
            
            // Check condition
            String condition = itemSection.getString("condition");
            if (condition != null && !evaluateCondition(condition, page, totalPages, false, false)) {
                continue;
            }
            
            // Get material
            String materialName = itemSection.getString("material", "black_stained_glass_pane");
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
            
            // Get display name with placeholders
            String displayName = itemSection.getString("display_name", " ");
            displayName = replacePlaceholders(displayName, player, page, totalPages, startIndex, questsPerPage, coins, tokens);
            
            // Get lore with placeholders
            List<String> lore = itemSection.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(replacePlaceholders(line, player, page, totalPages, startIndex, questsPerPage, coins, tokens));
            }
            
            // Create and set item
            // Special handling for player_head to set skull owner
            if (material == Material.PLAYER_HEAD) {
                ItemStack head = createPlayerHead(player, playerProgress, pqd);
                // Override display name and lore
                ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('§', displayName));
                    if (!processedLore.isEmpty()) {
                        meta.setLore(processedLore.stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('§', line))
                            .collect(java.util.stream.Collectors.toList()));
                    }
                    head.setItemMeta(meta);
                }
                inv.setItem(slot, head);
            } else if (processedLore.isEmpty()) {
                inv.setItem(slot, createItem(material, displayName));
            } else {
                inv.setItem(slot, createItem(material, displayName, processedLore.toArray(new String[0])));
            }
        }
    }
    
    /**
     * Applies the navbar from config to the inventory.
     * Detail menu version with quest context.
     */
    private void applyNavbar(Inventory inv, Player player, String menuType,
                            Quest quest, PlayerQuestData playerData, boolean isActive, boolean isCompleted) {
        // Fill row 5 with black glass
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        
        // Load navbar config
        var config = plugin.getConfigManager().getNavbarConfig();
        if (config == null) {
            // Fallback to hardcoded if config fails
            applyHardcodedDetailNavbar(inv, quest, playerData, isActive, isCompleted);
            return;
        }
        
        var menuConfig = config.getConfigurationSection(menuType);
        if (menuConfig == null) {
            applyHardcodedDetailNavbar(inv, quest, playerData, isActive, isCompleted);
            return;
        }
        
        // Prepare player info for player_head
        double playerProgress = plugin.getProgressIntegration().getPlayerProgress(player);
        
        // Apply each navbar item
        for (String key : menuConfig.getKeys(false)) {
            var itemSection = menuConfig.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            int slot = itemSection.getInt("slot");
            if (slot < 0 || slot >= 54) continue;
            
            // Check condition
            String condition = itemSection.getString("condition");
            if (condition != null && !evaluateCondition(condition, 0, 0, isActive, isCompleted)) {
                continue;
            }
            
            // Get material
            String materialName = itemSection.getString("material", "black_stained_glass_pane");
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;
            
            // Get display name with placeholders
            String displayName = itemSection.getString("display_name", " ");
            displayName = replacePlaceholders(displayName, player, quest, isActive, isCompleted);
            
            // Get lore with placeholders
            List<String> lore = itemSection.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(replacePlaceholders(line, player, quest, isActive, isCompleted));
            }
            
            // Special handling for main_action slot (slot 49) - context dependent
            if (slot == 49) {
                // This will be overridden by the specific action buttons below
                // Skip the config version for this slot
                continue;
            }
            
            // Create and set item - special handling for PLAYER_HEAD
            if (material == Material.PLAYER_HEAD) {
                ItemStack head = createPlayerHead(player, playerProgress, playerData);
                ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('§', displayName));
                    if (!processedLore.isEmpty()) {
                        meta.setLore(processedLore.stream()
                            .map(line -> ChatColor.translateAlternateColorCodes('§', line))
                            .collect(java.util.stream.Collectors.toList()));
                    }
                    head.setItemMeta(meta);
                }
                inv.setItem(slot, head);
            } else if (processedLore.isEmpty()) {
                inv.setItem(slot, createItem(material, displayName));
            } else {
                inv.setItem(slot, createItem(material, displayName, processedLore.toArray(new String[0])));
            }
        }
        
        // Add the context-dependent main action button (slot 49)
        if (isActive) {
            boolean tracking = playerData.isTracking(quest.getId());
            inv.setItem(49, createItem(
                tracking ? Material.ENDER_EYE : Material.ENDER_PEARL,
                tracking ? "§d§lTracking ✓" : "§e§lTrack Quest",
                "§7Toggle quest tracking"
            ));
        } else if (!isCompleted) {
            inv.setItem(49, createItem(Material.EMERALD, "§a§lStart Quest", 
                "§7Click to begin!",
                " ",
                "§8Or just start doing objectives",
                "§8and it will auto-start!"));
        } else if (quest.isRepeatable() && !playerData.isOnCooldown(quest.getId())) {
            inv.setItem(49, createItem(Material.EXPERIENCE_BOTTLE, "§b§lRepeat Quest", "§7Click to restart!"));
        }
    }
    
    /**
     * Fallback hardcoded navbar for main menu.
     */
    private void applyHardcodedNavbar(Inventory inv, Player player, 
                                     int page, int totalPages, int startIndex, int questsPerPage,
                                     double coins, int tokens) {
        List<Quest> dailyQuests = plugin.getDailyQuestManager().getDailyQuests(player);
    
        // Player head (slot 46) - no balance here (balance shown in nugget at slot 45)
        PlayerQuestData pqd = plugin.getPlayerQuestManager().getPlayerData(player);
        double playerProgress = plugin.getProgressIntegration().getPlayerProgress(player);
        ItemStack head = createPlayerHead(player, playerProgress, pqd);
        inv.setItem(46, head);
        

        // Page info moved to center (slot 49)
        inv.setItem(49, createItem(Material.PAPER,
            "§e§lPage: §f§l" + (page + 1) + " §8§l/ " + totalPages, " ", 
            "§7Viewing quests " + (startIndex + 1) + "-" + Math.min(startIndex + questsPerPage, dailyQuests.size())));
        
        // Balance nugget (slot 45)
        inv.setItem(45, createItem(Material.GOLD_NUGGET, "Balance: ",
            " ",
            "§eSkillCoins: §6" + String.format("%.0f", coins) + " ⛃",
            "§aTokens: §2" + String.format("%,d", tokens) + " 🎟"));
        
        // Previous page (slot 48)
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "§e§l← Previous", "§7Go to page " + page));
        }
        
        // Next page (slot 50)
        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "§e§lNext →", "§7Go to page " + (page + 2)));
        }
        
        // Close button (slot 53)
        inv.setItem(53, createItem(Material.BARRIER, "§c§lClose", "§7Click to close menu"));
    }
    
    /**
     * Fallback hardcoded navbar for detail menu.
     */
    private void applyHardcodedDetailNavbar(Inventory inv, Quest quest, 
                                           PlayerQuestData playerData, boolean isActive, boolean isCompleted) {
        // Main action button (slot 49)
        if (isActive) {
            boolean tracking = playerData.isTracking(quest.getId());
            inv.setItem(49, createItem(
                tracking ? Material.ENDER_EYE : Material.ENDER_PEARL,
                tracking ? "§d§lTracking ✓" : "§e§lTrack Quest",
                "§7Toggle quest tracking"
            ));
        } else if (quest.isRepeatable() && !playerData.isOnCooldown(quest.getId())) {
            inv.setItem(49, createItem(Material.EXPERIENCE_BOTTLE, "§b§lRepeat Quest", "§7Click to restart!"));
        }
        
        // Abandon button (slot 50) - only when active
        if (isActive) {
            inv.setItem(50, createItem(Material.TNT, "§c§lAbandon", 
                "§7Remove quest", "§c⚠ Progress will be lost!"));
        }
        
        // Back button moved to slot 53 (replaces close)
        inv.setItem(53, createItem(Material.SPYGLASS, "§c§l← Back", "§7Return to quest menu"));
    }
    
    /**
     * Replace placeholders in strings.
     */
    private String replacePlaceholders(String text, Player player, 
                                      int page, int totalPages, int startIndex, int questsPerPage,
                                      double coins, int tokens) {
        if (text == null) return " ";
        
        String result = text
            .replace("{page}", String.valueOf(page + 1))
            .replace("{total_pages}", String.valueOf(totalPages))
            .replace("{prev_page}", String.valueOf(page))
            .replace("{next_page}", String.valueOf(page + 2))
            .replace("{start}", String.valueOf(startIndex + 1))
            .replace("{end}", String.valueOf(Math.min(startIndex + questsPerPage, 
                plugin.getDailyQuestManager().getDailyQuests(player).size())))
            .replace("{player_name}", player.getName())
            .replace("{coins}", String.format("%.0f", coins))
            .replace("{tokens}", String.format("%,d", tokens));
        
        return plugin.getConfigManager().colorize(result);
    }
    
    /**
     * Replace placeholders for detail menu.
     */
    private String replacePlaceholders(String text, Player player, Quest quest, 
                                      boolean isActive, boolean isCompleted) {
        if (text == null) return " ";
        
        String result = text
            .replace("{quest_name}", quest != null ? quest.getDisplayName() : " ")
            .replace("{is_active}", String.valueOf(isActive))
            .replace("{is_completed}", String.valueOf(isCompleted));
        
        return plugin.getConfigManager().colorize(result);
    }
    
    /**
     * Evaluate condition strings like "page > 0" or "is_active".
     */
    private boolean evaluateCondition(String condition, int page, int totalPages, 
                                     boolean isActive, boolean isCompleted) {
        if (condition == null || condition.trim().isEmpty()) return true;
        
        try {
            // Simple condition parser
            if (condition.contains(">")) {
                String[] parts = condition.split(">");
                if (parts[0].trim().equals("page") && parts[1].trim().equals("0")) {
                    return page > 0;
                }
                if (parts[0].trim().equals("page") && parts[1].trim().contains("total_pages")) {
                    return page < totalPages - 1;
                }
            } else if (condition.contains("<")) {
                String[] parts = condition.split("<");
                if (parts[0].trim().equals("page") && parts[1].trim().contains("total_pages")) {
                    return page < totalPages - 1;
                }
            } else if (condition.equals("is_active")) {
                return isActive;
            } else if (condition.equals("!is_active")) {
                return !isActive;
            }
        } catch (Exception e) {
            // If parsing fails, return true (show the item)
            return true;
        }
        
        return true;
    }
}

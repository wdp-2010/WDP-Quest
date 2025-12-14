package com.wdp.quest.quest;

import com.wdp.quest.WDPQuestPlugin;
import com.wdp.quest.quest.QuestObjective.ObjectiveType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all quest definitions and loading
 */
public class QuestManager {
    
    private final WDPQuestPlugin plugin;
    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final Map<QuestCategory, List<Quest>> questsByCategory = new EnumMap<>(QuestCategory.class);
    
    public QuestManager(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all quests from configuration
     */
    public void loadQuests() {
        quests.clear();
        questsByCategory.clear();
        
        // Initialize category lists
        for (QuestCategory category : QuestCategory.values()) {
            questsByCategory.put(category, new ArrayList<>());
        }
        
        // Save default quest files
        saveDefaultQuests();
        
        // Load quests from files
        File questsDir = new File(plugin.getDataFolder(), "quests");
        if (!questsDir.exists()) {
            questsDir.mkdirs();
        }
        
        File[] questFiles = questsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (questFiles != null) {
            for (File file : questFiles) {
                loadQuestsFromFile(file);
            }
        }
        
        // Sort quests by category and sort order
        for (QuestCategory category : QuestCategory.values()) {
            questsByCategory.get(category).sort(Comparator.comparingInt(Quest::getSortOrder));
        }
        
        plugin.getLogger().info("Loaded " + quests.size() + " quests across " + QuestCategory.values().length + " categories");
    }
    
    private void saveDefaultQuests() {
        String[] defaultFiles = {
            "quests/beginner_quests.yml",
            "quests/early_quests.yml",
            "quests/intermediate_quests.yml",
            "quests/advanced_quests.yml",
            "quests/expert_quests.yml"
        };
        
        for (String fileName : defaultFiles) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                plugin.saveResource(fileName, false);
            }
        }
    }
    
    private void loadQuestsFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        ConfigurationSection questsSection = config.getConfigurationSection("quests");
        if (questsSection == null) return;
        
        for (String questId : questsSection.getKeys(false)) {
            ConfigurationSection questConfig = questsSection.getConfigurationSection(questId);
            if (questConfig == null) continue;
            
            try {
                Quest quest = loadQuest(questId, questConfig);
                if (quest != null) {
                    quests.put(questId, quest);
                    questsByCategory.get(quest.getCategory()).add(quest);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load quest " + questId + ": " + e.getMessage());
            }
        }
    }
    
    private Quest loadQuest(String id, ConfigurationSection config) {
        Quest quest = new Quest(id);
        
        quest.displayName(config.getString("name", id));
        quest.description(config.getString("description", ""));
        quest.lore(config.getStringList("lore"));
        
        // Parse icon
        String iconName = config.getString("icon", "BOOK");
        try {
            quest.icon(Material.valueOf(iconName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            quest.icon(Material.BOOK);
        }
        
        // Parse category
        String categoryName = config.getString("category", "BEGINNER");
        quest.category(QuestCategory.fromName(categoryName));
        
        quest.requiredProgress(config.getDouble("required-progress", 0));
        quest.repeatable(config.getBoolean("repeatable", false));
        quest.cooldownSeconds(config.getLong("cooldown-seconds", 0));
        quest.sortOrder(config.getInt("sort-order", 0));
        
        // Load hard quest settings
        quest.hardQuest(config.getBoolean("hard", false));
        quest.daysAvailable(config.getInt("days-available", quest.isHardQuest() ? 3 : 1));
        
        // Load objectives
        ConfigurationSection objectivesSection = config.getConfigurationSection("objectives");
        if (objectivesSection != null) {
            for (String objId : objectivesSection.getKeys(false)) {
                ConfigurationSection objConfig = objectivesSection.getConfigurationSection(objId);
                if (objConfig != null) {
                    QuestObjective objective = loadObjective(objId, objConfig);
                    if (objective != null) {
                        quest.addObjective(objective);
                    }
                }
            }
        }
        
        // Load rewards
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            quest.rewards(loadRewards(rewardsSection));
        }
        
        return quest;
    }
    
    private QuestObjective loadObjective(String id, ConfigurationSection config) {
        String typeName = config.getString("type", "CUSTOM");
        ObjectiveType type;
        try {
            type = ObjectiveType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ObjectiveType.CUSTOM;
        }
        
        int amount = config.getInt("amount", 1);
        QuestObjective objective = new QuestObjective(id, type, amount);
        
        objective.description(config.getString("description", type.getDefaultDescription()));
        
        // Parse type-specific data
        String materialName = config.getString("material");
        if (materialName != null) {
            try {
                objective.material(Material.valueOf(materialName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        
        String entityName = config.getString("entity");
        if (entityName != null) {
            try {
                objective.entityType(EntityType.valueOf(entityName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        
        objective.customData(config.getString("data"));
        
        return objective;
    }
    
    private QuestRewards loadRewards(ConfigurationSection config) {
        QuestRewards rewards = new QuestRewards();
        
        rewards.coins(config.getDouble("coins", 0));
        rewards.tokens(config.getDouble("tokens", 0));
        rewards.experience(config.getInt("experience", 0));
        
        // Load item rewards
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null) {
                    String materialName = itemConfig.getString("material", "STONE");
                    int amount = itemConfig.getInt("amount", 1);
                    try {
                        Material material = Material.valueOf(materialName.toUpperCase());
                        rewards.addItem(new ItemStack(material, amount));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        
        // Load command rewards
        rewards.commands(config.getStringList("commands"));
        
        return rewards;
    }
    
    // Quest lookup methods
    public Quest getQuest(String id) {
        return quests.get(id);
    }
    
    public Collection<Quest> getAllQuests() {
        return quests.values();
    }
    
    public List<Quest> getQuestsByCategory(QuestCategory category) {
        return questsByCategory.getOrDefault(category, Collections.emptyList());
    }
    
    public List<Quest> getAvailableQuests(double playerProgress) {
        return quests.values().stream()
                .filter(q -> q.getRequiredProgress() <= playerProgress)
                .collect(Collectors.toList());
    }
    
    public int getQuestCount() {
        return quests.size();
    }
    
    public int getQuestCount(QuestCategory category) {
        return questsByCategory.getOrDefault(category, Collections.emptyList()).size();
    }
    
    public Set<String> getQuestIds() {
        return quests.keySet();
    }
    
    /**
     * Get all hard quests
     */
    public List<Quest> getHardQuests() {
        return quests.values().stream()
                .filter(Quest::isHardQuest)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all normal (non-hard) quests
     */
    public List<Quest> getNormalQuests() {
        return quests.values().stream()
                .filter(q -> !q.isHardQuest())
                .collect(Collectors.toList());
    }
    
    /**
     * Get quests filtered by progress and difficulty
     */
    public List<Quest> getQuestsForProgress(double playerProgress, boolean includeHard) {
        return quests.values().stream()
                .filter(q -> q.getRequiredProgress() <= playerProgress)
                .filter(q -> includeHard || !q.isHardQuest())
                .collect(Collectors.toList());
    }
}

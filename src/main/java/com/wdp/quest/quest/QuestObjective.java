package com.wdp.quest.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Represents a quest objective that must be completed
 */
public class QuestObjective {
    
    private final String id;
    private final ObjectiveType type;
    private String description;
    private int targetAmount;
    
    // Type-specific data
    private Material material;      // For MINE, CRAFT, COLLECT, PLACE
    private EntityType entityType;  // For KILL
    private String customData;      // For VISIT, CUSTOM
    
    public QuestObjective(String id, ObjectiveType type, int targetAmount) {
        this.id = id;
        this.type = type;
        this.targetAmount = targetAmount;
        this.description = type.getDefaultDescription();
    }
    
    // Builder methods
    public QuestObjective description(String description) {
        this.description = description;
        return this;
    }
    
    public QuestObjective material(Material material) {
        this.material = material;
        return this;
    }
    
    public QuestObjective entityType(EntityType entityType) {
        this.entityType = entityType;
        return this;
    }
    
    public QuestObjective customData(String customData) {
        this.customData = customData;
        return this;
    }
    
    /**
     * Check if an action matches this objective
     */
    public boolean matches(ObjectiveType actionType, Object data) {
        if (this.type != actionType) return false;
        
        switch (type) {
            case KILL:
                return entityType != null && entityType.equals(data);
            case MINE:
            case CRAFT:
            case COLLECT:
            case PLACE:
            case SMELT:
                if (material != null && data instanceof Material) {
                    Material actionMat = (Material) data;
                    // Direct match
                    if (material.equals(actionMat)) return true;
                    
                    // Group matches (for wood types, etc.)
                    return isInMaterialGroup(material, actionMat);
                }
                return false;
            case VISIT:
            case CUSTOM:
                return customData != null && customData.equals(data);
            case FISH:
            case BREED:
            case ENCHANT:
            case TRADE:
            case LEVEL_UP:
            case ADVANCEMENT:
                return true; // Generic match, amount tracked
            default:
                return false;
        }
    }
    
    /**
     * Check if actionMat is in the same group as the objective material.
     * Used for objectives like "craft planks" to match all wood plank types.
     */
    private boolean isInMaterialGroup(Material objective, Material action) {
        String objName = objective.name();
        String actName = action.name();
        
        // Wood planks: OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, etc.
        if (objName.endsWith("_PLANKS") && actName.endsWith("_PLANKS")) {
            return true;
        }
        
        // Logs: OAK_LOG, SPRUCE_LOG, etc. and stripped variants
        if ((objName.endsWith("_LOG") || objName.contains("_WOOD")) && 
            (actName.endsWith("_LOG") || actName.contains("_WOOD"))) {
            return true;
        }
        
        // Wool colors
        if (objName.endsWith("_WOOL") && actName.endsWith("_WOOL")) {
            return true;
        }
        
        // Concrete
        if (objName.endsWith("_CONCRETE") && actName.endsWith("_CONCRETE")) {
            return true;
        }
        
        // Terracotta
        if (objName.endsWith("_TERRACOTTA") && actName.endsWith("_TERRACOTTA")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get display description with placeholders filled
     */
    public String getFormattedDescription() {
        String desc = description;
        
        if (material != null) {
            desc = desc.replace("%material%", formatMaterialName(material));
        }
        if (entityType != null) {
            desc = desc.replace("%entity%", formatEntityName(entityType));
        }
        if (customData != null) {
            desc = desc.replace("%data%", customData);
        }
        desc = desc.replace("%amount%", String.valueOf(targetAmount));
        
        return desc;
    }
    
    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    private String formatEntityName(EntityType entity) {
        String name = entity.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    // Getters
    public String getId() { return id; }
    public ObjectiveType getType() { return type; }
    public String getDescription() { return description; }
    public int getTargetAmount() { return targetAmount; }
    public Material getMaterial() { return material; }
    public EntityType getEntityType() { return entityType; }
    public String getCustomData() { return customData; }
    
    /**
     * Objective types that can be tracked
     */
    public enum ObjectiveType {
        KILL("Kill %amount% %entity%"),
        MINE("Mine %amount% %material%"),
        CRAFT("Craft %amount% %material%"),
        COLLECT("Collect %amount% %material%"),
        PLACE("Place %amount% %material%"),
        SMELT("Smelt %amount% %material%"),
        FISH("Catch %amount% fish"),
        BREED("Breed %amount% animals"),
        ENCHANT("Enchant %amount% items"),
        TRADE("Complete %amount% villager trades"),
        LEVEL_UP("Gain %amount% experience levels"),
        VISIT("Visit %data%"),
        ADVANCEMENT("Earn advancement: %data%"),
        CUSTOM("Complete: %data%");
        
        private final String defaultDescription;
        
        ObjectiveType(String defaultDescription) {
            this.defaultDescription = defaultDescription;
        }
        
        public String getDefaultDescription() {
            return defaultDescription;
        }
    }
}

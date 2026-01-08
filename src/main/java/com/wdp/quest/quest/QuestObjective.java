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
     * 
     * ONLY matches if the objective uses OAK as the prefix, which indicates "any type".
     * For example: OAK_PLANKS matches any planks, but JUNGLE_LOG only matches JUNGLE_LOG.
     */
    private boolean isInMaterialGroup(Material objective, Material action) {
        String objName = objective.name();
        String actName = action.name();
        
        // Only allow group matching if the objective uses OAK (the "default" wood type)
        // This means OAK_LOG will match any log, but JUNGLE_LOG only matches JUNGLE_LOG
        if (!objName.startsWith("OAK_") && !objName.startsWith("WHITE_")) {
            return false; // Specific material requested, no group matching
        }
        
        // Wood planks: OAK_PLANKS matches any planks
        if (objName.equals("OAK_PLANKS") && actName.endsWith("_PLANKS")) {
            return true;
        }
        
        // Logs: OAK_LOG matches any log (including stripped variants and wood blocks)
        if (objName.equals("OAK_LOG")) {
            if (actName.endsWith("_LOG") || actName.endsWith("_WOOD") || 
                actName.equals("CRIMSON_STEM") || actName.equals("WARPED_STEM") ||
                actName.equals("CRIMSON_HYPHAE") || actName.equals("WARPED_HYPHAE") ||
                actName.startsWith("STRIPPED_")) {
                return true;
            }
        }
        
        // Fences: OAK_FENCE matches any fence
        if (objName.equals("OAK_FENCE") && actName.endsWith("_FENCE") && !actName.endsWith("_FENCE_GATE")) {
            return true;
        }
        
        // Fence Gates: OAK_FENCE_GATE matches any fence gate
        if (objName.equals("OAK_FENCE_GATE") && actName.endsWith("_FENCE_GATE")) {
            return true;
        }
        
        // Doors: OAK_DOOR matches any door
        if (objName.equals("OAK_DOOR") && actName.endsWith("_DOOR")) {
            return true;
        }
        
        // Trapdoors: OAK_TRAPDOOR matches any trapdoor
        if (objName.equals("OAK_TRAPDOOR") && actName.endsWith("_TRAPDOOR")) {
            return true;
        }
        
        // Boats: OAK_BOAT matches any boat (including chest boats)
        if (objName.equals("OAK_BOAT") && (actName.endsWith("_BOAT") || actName.endsWith("_RAFT"))) {
            return true;
        }
        
        // Stairs: OAK_STAIRS matches any wooden stairs
        if (objName.equals("OAK_STAIRS") && actName.endsWith("_STAIRS") && 
            !actName.contains("STONE") && !actName.contains("BRICK") && !actName.contains("QUARTZ")) {
            return true;
        }
        
        // Slabs: OAK_SLAB matches any wooden slab
        if (objName.equals("OAK_SLAB") && actName.endsWith("_SLAB") && 
            !actName.contains("STONE") && !actName.contains("BRICK") && !actName.contains("QUARTZ")) {
            return true;
        }
        
        // Buttons: OAK_BUTTON matches any wooden button
        if (objName.equals("OAK_BUTTON") && actName.endsWith("_BUTTON") && 
            !actName.equals("STONE_BUTTON")) {
            return true;
        }
        
        // Pressure Plates: OAK_PRESSURE_PLATE matches any wooden pressure plate
        if (objName.equals("OAK_PRESSURE_PLATE") && actName.endsWith("_PRESSURE_PLATE") && 
            !actName.contains("STONE") && !actName.contains("WEIGHTED")) {
            return true;
        }
        
        // Wool colors: WHITE_WOOL matches any wool color
        if (objName.equals("WHITE_WOOL") && actName.endsWith("_WOOL")) {
            return true;
        }
        
        // Concrete: WHITE_CONCRETE matches any concrete color
        if (objName.equals("WHITE_CONCRETE") && actName.endsWith("_CONCRETE")) {
            return true;
        }
        
        // Terracotta: WHITE_TERRACOTTA or TERRACOTTA matches any terracotta
        if ((objName.equals("WHITE_TERRACOTTA") || objName.equals("TERRACOTTA")) && 
            (actName.endsWith("_TERRACOTTA") || actName.equals("TERRACOTTA"))) {
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

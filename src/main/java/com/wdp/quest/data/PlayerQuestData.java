package com.wdp.quest.data;

import java.util.*;

/**
 * Stores a player's quest progress data
 */
public class PlayerQuestData {
    
    private final UUID uuid;
    private final Map<String, QuestProgress> questProgress = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private String trackedQuestId;
    
    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    // Quest progress methods
    
    public void addQuestProgress(QuestProgress progress) {
        questProgress.put(progress.getQuestId(), progress);
    }
    
    public QuestProgress getQuestProgress(String questId) {
        return questProgress.get(questId);
    }
    
    public boolean hasQuest(String questId) {
        return questProgress.containsKey(questId);
    }
    
    public boolean isQuestActive(String questId) {
        QuestProgress progress = questProgress.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.ACTIVE;
    }
    
    public boolean isQuestCompleted(String questId) {
        QuestProgress progress = questProgress.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.COMPLETED;
    }
    
    public List<QuestProgress> getActiveQuests() {
        List<QuestProgress> active = new ArrayList<>();
        for (QuestProgress progress : questProgress.values()) {
            if (progress.getStatus() == QuestStatus.ACTIVE) {
                active.add(progress);
            }
        }
        return active;
    }
    
    public List<QuestProgress> getCompletedQuests() {
        List<QuestProgress> completed = new ArrayList<>();
        for (QuestProgress progress : questProgress.values()) {
            if (progress.getStatus() == QuestStatus.COMPLETED) {
                completed.add(progress);
            }
        }
        return completed;
    }
    
    public int getActiveQuestCount() {
        int count = 0;
        for (QuestProgress progress : questProgress.values()) {
            if (progress.getStatus() == QuestStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }
    
    public int getCompletedQuestCount() {
        int count = 0;
        for (QuestProgress progress : questProgress.values()) {
            if (progress.getStatus() == QuestStatus.COMPLETED) {
                count++;
            }
        }
        return count;
    }
    
    public void removeQuest(String questId) {
        questProgress.remove(questId);
    }
    
    // Cooldown methods
    
    public void setCooldown(String questId, long cooldownUntil) {
        cooldowns.put(questId, cooldownUntil);
    }
    
    public boolean isOnCooldown(String questId) {
        Long cooldownUntil = cooldowns.get(questId);
        return cooldownUntil != null && cooldownUntil > System.currentTimeMillis();
    }
    
    public long getCooldownRemaining(String questId) {
        Long cooldownUntil = cooldowns.get(questId);
        if (cooldownUntil == null) return 0;
        return Math.max(0, cooldownUntil - System.currentTimeMillis());
    }
    
    // Tracked quest
    
    public String getTrackedQuestId() {
        return trackedQuestId;
    }
    
    public void setTrackedQuestId(String trackedQuestId) {
        this.trackedQuestId = trackedQuestId;
    }
    
    public boolean isTracking(String questId) {
        return questId.equals(trackedQuestId);
    }
    
    /**
     * Quest status enum
     */
    public enum QuestStatus {
        ACTIVE,
        COMPLETED,
        ABANDONED
    }
    
    /**
     * Progress for a single quest
     */
    public static class QuestProgress {
        
        private final String questId;
        private QuestStatus status = QuestStatus.ACTIVE;
        private long startedAt;
        private Long completedAt;
        private final Map<String, ObjectiveProgress> objectiveProgress = new HashMap<>();
        
        public QuestProgress(String questId) {
            this.questId = questId;
            this.startedAt = System.currentTimeMillis();
        }
        
        public String getQuestId() { return questId; }
        public QuestStatus getStatus() { return status; }
        public long getStartedAt() { return startedAt; }
        public Long getCompletedAt() { return completedAt; }
        
        public void setStatus(QuestStatus status) { this.status = status; }
        public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
        public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
        
        // Objective progress
        
        public void setObjectiveProgress(String objectiveId, int amount, boolean completed) {
            objectiveProgress.put(objectiveId, new ObjectiveProgress(objectiveId, amount, completed));
        }
        
        public ObjectiveProgress getObjectiveProgress(String objectiveId) {
            return objectiveProgress.computeIfAbsent(objectiveId, id -> new ObjectiveProgress(id, 0, false));
        }
        
        public int getObjectiveAmount(String objectiveId) {
            ObjectiveProgress progress = objectiveProgress.get(objectiveId);
            return progress != null ? progress.currentAmount : 0;
        }
        
        public boolean isObjectiveComplete(String objectiveId) {
            ObjectiveProgress progress = objectiveProgress.get(objectiveId);
            return progress != null && progress.completed;
        }
        
        public void incrementObjective(String objectiveId, int amount, int target) {
            ObjectiveProgress progress = getObjectiveProgress(objectiveId);
            progress.currentAmount = Math.min(progress.currentAmount + amount, target);
            progress.completed = progress.currentAmount >= target;
        }
        
        public Map<String, ObjectiveProgress> getAllObjectiveProgress() {
            return objectiveProgress;
        }
        
        /**
         * Check if all objectives are complete
         */
        public boolean areAllObjectivesComplete(int totalObjectives) {
            if (objectiveProgress.size() < totalObjectives) return false;
            for (ObjectiveProgress progress : objectiveProgress.values()) {
                if (!progress.completed) return false;
            }
            return true;
        }
        
        /**
         * Get completion percentage based on completed objective COUNT (legacy)
         */
        public double getCompletionPercentage(int totalObjectives) {
            if (totalObjectives == 0) return 100;
            int completed = 0;
            for (ObjectiveProgress progress : objectiveProgress.values()) {
                if (progress.completed) completed++;
            }
            return (completed * 100.0) / totalObjectives;
        }
        
        /**
         * Get actual completion percentage based on progress towards each objective's target.
         * This gives a more accurate progress reading (e.g., 5/10 items = 50%)
         * @param targets Map of objective ID to target amount
         */
        public double getActualCompletionPercentage(java.util.Map<String, Integer> targets) {
            if (targets == null || targets.isEmpty()) return 0;
            
            double totalProgress = 0;
            for (var entry : targets.entrySet()) {
                String objId = entry.getKey();
                int target = entry.getValue();
                if (target <= 0) continue;
                
                ObjectiveProgress progress = objectiveProgress.get(objId);
                int current = (progress != null) ? progress.currentAmount : 0;
                
                // Cap at 100% per objective
                double objPercent = Math.min(100.0, (current * 100.0) / target);
                totalProgress += objPercent;
            }
            
            return totalProgress / targets.size();
        }
    }
    
    /**
     * Progress for a single objective
     */
    public static class ObjectiveProgress {
        
        private final String objectiveId;
        private int currentAmount;
        private boolean completed;
        
        public ObjectiveProgress(String objectiveId, int currentAmount, boolean completed) {
            this.objectiveId = objectiveId;
            this.currentAmount = currentAmount;
            this.completed = completed;
        }
        
        public String getObjectiveId() { return objectiveId; }
        public int getCurrentAmount() { return currentAmount; }
        public boolean isCompleted() { return completed; }
        
        public void setCurrentAmount(int amount) { this.currentAmount = amount; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}

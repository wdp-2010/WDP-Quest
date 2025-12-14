package com.wdp.quest.data;

import com.wdp.quest.WDPQuestPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages SQLite database for quest progress storage
 */
public class DatabaseManager {
    
    private final WDPQuestPlugin plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(WDPQuestPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            // Create data directory
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // Setup HikariCP
            String dbPath = new File(dataDir, "quests.db").getAbsolutePath();
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            // SQLite specific settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            
            dataSource = new HikariDataSource(config);
            
            // Create tables
            createTables();
            
            plugin.getLogger().info("Database initialized successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Player quest progress table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS player_quests (
                    uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                    started_at BIGINT NOT NULL,
                    completed_at BIGINT DEFAULT NULL,
                    PRIMARY KEY (uuid, quest_id)
                )
            """);
            
            // Objective progress table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS objective_progress (
                    uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    objective_id VARCHAR(64) NOT NULL,
                    current_amount INT NOT NULL DEFAULT 0,
                    completed BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (uuid, quest_id, objective_id),
                    FOREIGN KEY (uuid, quest_id) REFERENCES player_quests(uuid, quest_id) ON DELETE CASCADE
                )
            """);
            
            // Quest cooldowns table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS quest_cooldowns (
                    uuid VARCHAR(36) NOT NULL,
                    quest_id VARCHAR(64) NOT NULL,
                    cooldown_until BIGINT NOT NULL,
                    PRIMARY KEY (uuid, quest_id)
                )
            """);
            
            // Create indexes
            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_player_quests_uuid ON player_quests(uuid)"
            );
            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_player_quests_status ON player_quests(status)"
            );
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    // Player quest operations
    
    public void savePlayerQuest(UUID uuid, PlayerQuestData.QuestProgress progress) {
        String sql = """
            INSERT OR REPLACE INTO player_quests (uuid, quest_id, status, started_at, completed_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, progress.getQuestId());
            stmt.setString(3, progress.getStatus().name());
            stmt.setLong(4, progress.getStartedAt());
            stmt.setObject(5, progress.getCompletedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save quest progress", e);
        }
    }
    
    public void saveObjectiveProgress(UUID uuid, String questId, String objectiveId, int amount, boolean completed) {
        String sql = """
            INSERT OR REPLACE INTO objective_progress (uuid, quest_id, objective_id, current_amount, completed)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, questId);
            stmt.setString(3, objectiveId);
            stmt.setInt(4, amount);
            stmt.setBoolean(5, completed);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save objective progress", e);
        }
    }
    
    public PlayerQuestData loadPlayerData(UUID uuid) {
        PlayerQuestData data = new PlayerQuestData(uuid);
        
        // Load quest progress
        String questSql = "SELECT quest_id, status, started_at, completed_at FROM player_quests WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(questSql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String questId = rs.getString("quest_id");
                PlayerQuestData.QuestStatus status = PlayerQuestData.QuestStatus.valueOf(rs.getString("status"));
                long startedAt = rs.getLong("started_at");
                // Handle null completed_at - SQLite doesn't support getObject with Long.class well
                long completedAtRaw = rs.getLong("completed_at");
                Long completedAt = rs.wasNull() ? null : completedAtRaw;
                
                PlayerQuestData.QuestProgress progress = new PlayerQuestData.QuestProgress(questId);
                progress.setStatus(status);
                progress.setStartedAt(startedAt);
                progress.setCompletedAt(completedAt);
                
                // Load objective progress for this quest
                loadObjectiveProgress(conn, uuid, questId, progress);
                
                data.addQuestProgress(progress);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + uuid, e);
        }
        
        // Load cooldowns
        String cooldownSql = "SELECT quest_id, cooldown_until FROM quest_cooldowns WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(cooldownSql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String questId = rs.getString("quest_id");
                long cooldownUntil = rs.getLong("cooldown_until");
                data.setCooldown(questId, cooldownUntil);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load cooldowns for " + uuid, e);
        }
        
        return data;
    }
    
    private void loadObjectiveProgress(Connection conn, UUID uuid, String questId, PlayerQuestData.QuestProgress progress) throws SQLException {
        String sql = "SELECT objective_id, current_amount, completed FROM objective_progress WHERE uuid = ? AND quest_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, questId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String objectiveId = rs.getString("objective_id");
                int amount = rs.getInt("current_amount");
                boolean completed = rs.getBoolean("completed");
                progress.setObjectiveProgress(objectiveId, amount, completed);
            }
        }
    }
    
    public void deletePlayerQuest(UUID uuid, String questId) {
        String sql = "DELETE FROM player_quests WHERE uuid = ? AND quest_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, questId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete quest progress", e);
        }
    }
    
    public void saveCooldown(UUID uuid, String questId, long cooldownUntil) {
        String sql = "INSERT OR REPLACE INTO quest_cooldowns (uuid, quest_id, cooldown_until) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, questId);
            stmt.setLong(3, cooldownUntil);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save cooldown", e);
        }
    }
    
    public void clearExpiredCooldowns() {
        String sql = "DELETE FROM quest_cooldowns WHERE cooldown_until < ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear expired cooldowns", e);
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

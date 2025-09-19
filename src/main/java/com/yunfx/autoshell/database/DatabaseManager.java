package com.yunfx.autoshell.database;

import com.yunfx.autoshell.model.Script;
import com.yunfx.autoshell.model.ScriptGroup;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:autoshell.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        String createGroupsTable = """
            CREATE TABLE IF NOT EXISTS script_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                created_at TEXT NOT NULL
            )
        """;

        String createScriptsTable = """
            CREATE TABLE IF NOT EXISTS scripts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                file_path TEXT NOT NULL UNIQUE,
                last_modified TEXT,
                executable BOOLEAN DEFAULT 1,
                content TEXT
            )
        """;

        String createGroupScriptsTable = """
            CREATE TABLE IF NOT EXISTS group_scripts (
                group_id INTEGER,
                script_id INTEGER,
                PRIMARY KEY (group_id, script_id),
                FOREIGN KEY (group_id) REFERENCES script_groups(id) ON DELETE CASCADE,
                FOREIGN KEY (script_id) REFERENCES scripts(id) ON DELETE CASCADE
            )
        """;

        String createScriptTagsTable = """
            CREATE TABLE IF NOT EXISTS script_tags (
                script_id INTEGER,
                tag TEXT,
                PRIMARY KEY (script_id, tag),
                FOREIGN KEY (script_id) REFERENCES scripts(id) ON DELETE CASCADE
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createGroupsTable);
            stmt.execute(createScriptsTable);
            stmt.execute(createGroupScriptsTable);
            stmt.execute(createScriptTagsTable);
        }
    }

    // Script Group Operations
    public void saveGroup(ScriptGroup group) throws SQLException {
        String sql = "INSERT OR REPLACE INTO script_groups (id, name, description, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, group.getId());
            stmt.setString(2, group.getName());
            stmt.setString(3, group.getDescription());
            stmt.setString(4, group.getCreatedAt().toString());
            
            stmt.executeUpdate();
            
            if (group.getId() == null) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        group.setId(rs.getLong(1));
                    }
                }
            }
        }
    }

    public List<ScriptGroup> getAllGroups() throws SQLException {
        List<ScriptGroup> groups = new ArrayList<>();
        String sql = "SELECT * FROM script_groups ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ScriptGroup group = new ScriptGroup();
                group.setId(rs.getLong("id"));
                group.setName(rs.getString("name"));
                group.setDescription(rs.getString("description"));
                group.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
                
                // Load associated scripts
                loadGroupScripts(group);
                groups.add(group);
            }
        }
        return groups;
    }

    public void deleteGroup(Long groupId) throws SQLException {
        String sql = "DELETE FROM script_groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.executeUpdate();
        }
    }

    // Script Operations
    public void saveScript(Script script) throws SQLException {
        String sql = "INSERT OR REPLACE INTO scripts (id, name, description, file_path, last_modified, executable, content) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, null); // Let auto-increment handle ID
            stmt.setString(2, script.getName());
            stmt.setString(3, script.getDescription());
            stmt.setString(4, script.getFilePath().toString());
            stmt.setString(5, script.getLastModified() != null ? script.getLastModified().toString() : null);
            stmt.setBoolean(6, script.isExecutable());
            stmt.setString(7, script.getContent());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    // Update script with generated ID if needed
                }
            }
        }
    }

    public List<Script> getAllScripts() throws SQLException {
        List<Script> scripts = new ArrayList<>();
        String sql = "SELECT * FROM scripts ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Script script = new Script();
                script.setName(rs.getString("name"));
                script.setDescription(rs.getString("description"));
                script.setFilePath(java.nio.file.Paths.get(rs.getString("file_path")));
                if (rs.getString("last_modified") != null) {
                    script.setLastModified(LocalDateTime.parse(rs.getString("last_modified")));
                }
                script.setExecutable(rs.getBoolean("executable"));
                script.setContent(rs.getString("content"));
                
                // Load tags
                loadScriptTags(script);
                scripts.add(script);
            }
        }
        return scripts;
    }

    // Group-Script Association
    public void addScriptToGroup(Long groupId, String scriptPath) throws SQLException {
        // First ensure script exists in database
        Long scriptId = getScriptIdByPath(scriptPath);
        if (scriptId == null) {
            throw new SQLException("Script not found: " + scriptPath);
        }

        String sql = "INSERT OR IGNORE INTO group_scripts (group_id, script_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setLong(2, scriptId);
            stmt.executeUpdate();
        }
    }

    public void removeScriptFromGroup(Long groupId, String scriptPath) throws SQLException {
        Long scriptId = getScriptIdByPath(scriptPath);
        if (scriptId == null) return;

        String sql = "DELETE FROM group_scripts WHERE group_id = ? AND script_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
            stmt.setLong(2, scriptId);
            stmt.executeUpdate();
        }
    }

    private void loadGroupScripts(ScriptGroup group) throws SQLException {
        String sql = """
            SELECT s.* FROM scripts s
            JOIN group_scripts gs ON s.id = gs.script_id
            WHERE gs.group_id = ?
            ORDER BY s.name
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, group.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Script script = new Script();
                    script.setName(rs.getString("name"));
                    script.setDescription(rs.getString("description"));
                    script.setFilePath(java.nio.file.Paths.get(rs.getString("file_path")));
                    if (rs.getString("last_modified") != null) {
                        script.setLastModified(LocalDateTime.parse(rs.getString("last_modified")));
                    }
                    script.setExecutable(rs.getBoolean("executable"));
                    script.setContent(rs.getString("content"));
                    
                    loadScriptTags(script);
                    group.addScript(script);
                }
            }
        }
    }

    private void loadScriptTags(Script script) throws SQLException {
        String sql = "SELECT tag FROM script_tags WHERE script_id = (SELECT id FROM scripts WHERE file_path = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, script.getFilePath().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    script.addTag(rs.getString("tag"));
                }
            }
        }
    }

    private Long getScriptIdByPath(String scriptPath) throws SQLException {
        String sql = "SELECT id FROM scripts WHERE file_path = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, scriptPath);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

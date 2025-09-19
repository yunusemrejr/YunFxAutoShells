package com.yunfx.autoshell.database;

import com.yunfx.autoshell.model.Script;
import com.yunfx.autoshell.model.ScriptGroup;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/.local/share/yunfx-autoshell/autoshell.db";
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
            // Ensure the database directory exists
            String dbPath = System.getProperty("user.home") + "/.local/share/yunfx-autoshell/autoshell.db";
            java.io.File dbFile = new java.io.File(dbPath);
            java.io.File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                boolean created = dbDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create database directory: " + dbDir.getAbsolutePath());
                }
            }
            
            // Load SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("SQLite JDBC driver not found. Please ensure sqlite-jdbc is in the classpath.", e);
            }
            
            // Create connection with proper URL
            String dbUrl = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(dbUrl);
            
            // Enable foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            
            createTables();
            
            System.out.println("Database initialized successfully at: " + dbPath);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
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
            System.out.println("Creating script_groups table...");
            stmt.execute(createGroupsTable);
            System.out.println("Creating scripts table...");
            stmt.execute(createScriptsTable);
            System.out.println("Creating group_scripts table...");
            stmt.execute(createGroupScriptsTable);
            System.out.println("Creating script_tags table...");
            stmt.execute(createScriptTagsTable);
            System.out.println("All tables created successfully!");
        }
    }

    // Script Group Operations
    public void saveGroup(ScriptGroup group) throws SQLException {
        if (group.getId() == null) {
            // Insert new group
            String sql = "INSERT INTO script_groups (name, description, created_at) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, group.getName());
                stmt.setString(2, group.getDescription());
                stmt.setString(3, group.getCreatedAt().toString());
                
                stmt.executeUpdate();
                
                // Get the generated ID using last_insert_rowid()
                try (Statement idStmt = connection.createStatement();
                     ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        group.setId(rs.getLong(1));
                    }
                }
            }
        } else {
            // Update existing group
            String sql = "UPDATE script_groups SET name = ?, description = ?, created_at = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, group.getName());
                stmt.setString(2, group.getDescription());
                stmt.setString(3, group.getCreatedAt().toString());
                stmt.setLong(4, group.getId());
                
                stmt.executeUpdate();
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
        // Check if script already exists
        String checkSql = "SELECT id FROM scripts WHERE file_path = ?";
        Long existingId = null;
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, script.getFilePath().toString());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    existingId = rs.getLong("id");
                }
            }
        }
        
        if (existingId != null) {
            // Update existing script
            String sql = "UPDATE scripts SET name = ?, description = ?, last_modified = ?, executable = ?, content = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, script.getName());
                stmt.setString(2, script.getDescription());
                stmt.setString(3, script.getLastModified() != null ? script.getLastModified().toString() : null);
                stmt.setBoolean(4, script.isExecutable());
                stmt.setString(5, script.getContent());
                stmt.setLong(6, existingId);
                
                stmt.executeUpdate();
            }
        } else {
            // Insert new script
            String sql = "INSERT INTO scripts (name, description, file_path, last_modified, executable, content) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, script.getName());
                stmt.setString(2, script.getDescription());
                stmt.setString(3, script.getFilePath().toString());
                stmt.setString(4, script.getLastModified() != null ? script.getLastModified().toString() : null);
                stmt.setBoolean(5, script.isExecutable());
                stmt.setString(6, script.getContent());
                
                stmt.executeUpdate();
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
            // Try to find script by path in the scripts table
            String findSql = "SELECT id FROM scripts WHERE file_path = ?";
            try (PreparedStatement findStmt = connection.prepareStatement(findSql)) {
                findStmt.setString(1, scriptPath);
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        scriptId = rs.getLong("id");
                    }
                }
            }
            
            if (scriptId == null) {
                throw new SQLException("Script not found in database: " + scriptPath);
            }
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

    public List<Script> getScriptsByGroup(Long groupId) throws SQLException {
        List<Script> scripts = new ArrayList<>();
        String sql = """
            SELECT s.* FROM scripts s
            JOIN group_scripts gs ON s.id = gs.script_id
            WHERE gs.group_id = ?
            ORDER BY s.name
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, groupId);
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
                    scripts.add(script);
                }
            }
        }
        return scripts;
    }

    public void removeGroup(Long groupId) throws SQLException {
        // First remove all script associations for this group
        String deleteGroupScriptsSql = "DELETE FROM group_scripts WHERE group_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteGroupScriptsSql)) {
            stmt.setLong(1, groupId);
            stmt.executeUpdate();
        }
        
        // Then remove the group itself
        String deleteGroupSql = "DELETE FROM script_groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteGroupSql)) {
            stmt.setLong(1, groupId);
            stmt.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

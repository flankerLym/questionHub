package com.questionhub.desktop.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final Path dbFile;

    public Database(Path dataDir) {
        this.dbFile = dataDir.resolve("questionhub.db");
    }

    public Path dbFile() { return dbFile; }

    public Connection open() throws SQLException {
        try { Files.createDirectories(dbFile.getParent()); }
        catch (Exception e) { throw new SQLException("无法创建数据目录", e); }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }

    public void init() throws SQLException {
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS folders (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    sort INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS qa_items (
                    id TEXT PRIMARY KEY,
                    folder_id TEXT NOT NULL,
                    question TEXT NOT NULL,
                    answer TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY(folder_id) REFERENCES folders(id) ON DELETE CASCADE
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qa_folder ON qa_items(folder_id)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qa_updated ON qa_items(updated_at DESC)");
        }
    }
}

package com.questionhub.desktop.db;

import com.questionhub.desktop.model.Folder;
import com.questionhub.desktop.model.QaItem;
import com.questionhub.desktop.util.Validation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class ArchiveRepository {
    private final Database db;

    public ArchiveRepository(Database db) { this.db = db; }

    public List<Folder> folders() throws SQLException {
        List<Folder> out = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT id,name,sort,created_at,updated_at FROM folders ORDER BY sort, created_at");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Folder(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getLong(4), rs.getLong(5)));
        }
        return out;
    }

    public List<QaItem> questions(String folderId) throws SQLException {
        List<QaItem> out = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT id,folder_id,question,answer,created_at,updated_at FROM qa_items WHERE folder_id=? ORDER BY updated_at DESC")) {
            ps.setString(1, folderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(readQa(rs));
            }
        }
        return out;
    }

    public List<QaItem> allQuestions() throws SQLException {
        List<QaItem> out = new ArrayList<>();
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT id,folder_id,question,answer,created_at,updated_at FROM qa_items ORDER BY updated_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(readQa(rs));
        }
        return out;
    }

    public void insertFolder(Folder f) throws SQLException {
        ensureFolderNameUnique(f.name(), null);
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("INSERT INTO folders(id,name,sort,created_at,updated_at) VALUES(?,?,?,?,?)")) {
            ps.setString(1, f.id()); ps.setString(2, Validation.folderName(f.name())); ps.setInt(3, f.sort()); ps.setLong(4, f.createdAt()); ps.setLong(5, f.updatedAt());
            ps.executeUpdate();
        }
    }

    public void renameFolder(String id, String name) throws SQLException {
        name = Validation.folderName(name);
        ensureFolderNameUnique(name, id);
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("UPDATE folders SET name=?,updated_at=? WHERE id=?")) {
            ps.setString(1, name); ps.setLong(2, System.currentTimeMillis()); ps.setString(3, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("文件夹不存在");
        }
    }

    public void deleteFolder(String id) throws SQLException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("DELETE FROM folders WHERE id=?")) {
            ps.setString(1, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("文件夹不存在");
        }
    }

    public void upsertQuestion(QaItem item) throws SQLException {
        String q = Validation.question(item.question());
        String a = Validation.answer(item.answer());
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("""
            INSERT INTO qa_items(id,folder_id,question,answer,created_at,updated_at) VALUES(?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET folder_id=excluded.folder_id,question=excluded.question,answer=excluded.answer,updated_at=excluded.updated_at
            """)) {
            ps.setString(1, item.id()); ps.setString(2, item.folderId()); ps.setString(3, q); ps.setString(4, a);
            ps.setLong(5, item.createdAt()); ps.setLong(6, item.updatedAt());
            ps.executeUpdate();
        }
    }

    public void deleteQuestion(String id) throws SQLException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("DELETE FROM qa_items WHERE id=?")) {
            ps.setString(1, id);
            if (ps.executeUpdate() == 0) throw new IllegalArgumentException("问题不存在");
        }
    }

    public void replaceAll(List<Folder> folders, List<QaItem> items) throws SQLException {
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("DELETE FROM qa_items");
                    st.executeUpdate("DELETE FROM folders");
                }
                try (PreparedStatement pf = c.prepareStatement("INSERT INTO folders(id,name,sort,created_at,updated_at) VALUES(?,?,?,?,?)")) {
                    for (Folder f : folders) {
                        pf.setString(1, f.id()); pf.setString(2, f.name()); pf.setInt(3, f.sort()); pf.setLong(4, f.createdAt()); pf.setLong(5, f.updatedAt()); pf.addBatch();
                    }
                    pf.executeBatch();
                }
                try (PreparedStatement pq = c.prepareStatement("INSERT INTO qa_items(id,folder_id,question,answer,created_at,updated_at) VALUES(?,?,?,?,?,?)")) {
                    for (QaItem q : items) {
                        pq.setString(1, q.id()); pq.setString(2, q.folderId()); pq.setString(3, q.question()); pq.setString(4, q.answer()); pq.setLong(5, q.createdAt()); pq.setLong(6, q.updatedAt()); pq.addBatch();
                    }
                    pq.executeBatch();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void ensureFolderNameUnique(String name, String currentId) throws SQLException {
        String sql = currentId == null
                ? "SELECT 1 FROM folders WHERE lower(name)=lower(?) LIMIT 1"
                : "SELECT 1 FROM folders WHERE lower(name)=lower(?) AND id<>? LIMIT 1";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            if (currentId != null) ps.setString(2, currentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("已存在同名文件夹");
            }
        }
    }

    private QaItem readQa(ResultSet rs) throws SQLException {
        return new QaItem(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5), rs.getLong(6));
    }
}

package com.yegnachat.server.chat;

import com.yegnachat.server.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    private final DatabaseService db;

    public ChatService(DatabaseService db) {
        this.db = db;
    }

    public void savePrivateMessage(int senderId, int receiverId, String content) throws SQLException {
        String sql = """
            INSERT INTO messages (sender_id, receiver_id, content)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public List<String> fetchPrivateHistory(int userA, int userB) throws SQLException {
        String sql = """
            SELECT sender_id, content, created_at
            FROM messages
            WHERE (sender_id=? AND receiver_id=?)
               OR (sender_id=? AND receiver_id=?)
            ORDER BY created_at
        """;

        List<String> messages = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(rs.getInt("sender_id") + ":" + rs.getString("content"));
            }
        }
        return messages;
    }

    public void saveGroupMessage(int senderId, int groupId, String content) throws SQLException {
        String sql = """
            INSERT INTO group_messages (group_id, sender_id, content)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ps.setInt(2, senderId);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public List<String> fetchGroupHistory(int groupId) throws SQLException {
        String sql = """
            SELECT sender_id, content, created_at
            FROM group_messages
            WHERE group_id=?
            ORDER BY created_at
        """;

        List<String> messages = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messages.add(rs.getInt("sender_id") + ":" + rs.getString("content"));
            }
        }
        return messages;
    }

    public List<Integer> getGroupMembers(int groupId) throws SQLException {
        String sql = """
            SELECT user_id FROM group_members WHERE group_id=?
        """;

        List<Integer> members = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(rs.getInt("user_id"));
            }
        }
        return members;
    }
}

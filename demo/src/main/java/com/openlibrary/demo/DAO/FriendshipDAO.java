package com.openlibrary.demo.DAO;

import org.springframework.stereotype.Component;
import com.openlibrary.demo.controller.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) responsible for handling database operations related to
 * friendships between members in the Libbie application.
 * This class provides low-level JDBC access to the friendship table,
 * which stores mutual friendship relations. Each friendship is stored as a single row
 * where member_1_id & member_2_id to ensure uniqueness and simplify lookups.
 * Friendships are treated as bidirectional: if A is friends with B, then B is also friends with A.
 */
@Component
public class FriendshipDAO {

    private final DatabaseConnection connection;

    public FriendshipDAO(DatabaseConnection sqlHandler) {
        this.connection = sqlHandler;
    }

    /**
     * Checks whether two members are friends.
     *
     * Since the friendship table enforces member_1_id & member_2_id,
     * the method automatically orders the IDs to match the table constraint.
     *
     * @param id1 The ID of the first member.
     * @param id2 The ID of the second member.
     * @return {@code true} if the members are friends; {@code false} otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean areFriends(Long id1, Long id2) throws SQLException {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);

        String sql = "SELECT 1 FROM friendship WHERE member_1_id = ? AND member_2_id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, min);
            stmt.setLong(2, max);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves the list of all friend IDs for a given member.
     *
     * Since friendships can be stored with the member as either member_1_id
     * or member_2_id, this method handles both cases.
     *
     * @param memberId The ID of the member whose friends should be retrieved.
     * @return A list of member IDs representing the member's friends.
     * @throws SQLException if a database error occurs.
     */
    public List<Long> findFriendIds(Long memberId) throws SQLException {
        String sql = """
            SELECT CASE
                     WHEN member_1_id = ? THEN member_2_id
                     ELSE member_1_id
                   END AS friend_id
            FROM friendship
            WHERE member_1_id = ? OR member_2_id = ?
            """;

        List<Long> friendIds = new ArrayList<>();
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            stmt.setLong(2, memberId);
            stmt.setLong(3, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friendIds.add(rs.getLong("friend_id"));
                }
            }
        }

        return friendIds;
    }

    /**
     * Sends a friend request from one member to another by inserting a new record
     * into the pending_friendship table with status set to 'pending'.
     *
     * @param requesterId the ID of the member sending the request
     * @param receiverId the ID of the member receiving the request
     * @throws SQLException if a database access error occurs or the insert fails
     */
    public void sendFriendRequest(int requesterId, int receiverId) throws SQLException {
        String sql = """
        INSERT INTO pending_friendship (requester_id, receiver_id, status, requested_at)
        VALUES (?, ?, 'pending', NOW())
    """;

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requesterId);
            stmt.setInt(2, receiverId);
            stmt.executeUpdate();
        }
    }

}


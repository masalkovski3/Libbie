package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.DatabaseConnection;
import com.openlibrary.demo.model.Member;
import com.openlibrary.demo.util.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.*;
import java.util.*;

/**
 * Data Access Object for handling member-related operations in the database.
 */

@Component
public class MemberDAO {

    private DatabaseConnection connection;
    private JdbcTemplate jdbcTemplate;

    public MemberDAO(DatabaseConnection sqlHandler, JdbcTemplate jdbcTemplate) {
        this.connection = sqlHandler;
        this.jdbcTemplate = jdbcTemplate;
    }

    private String[] bios = {
            "Welcome to my Libbie 🦉",
            "Books. Tea. Silence. Repeat.",
            "Page-turner in progress...",
            "Librarian of my own world.",
            "Warning: May cause book envy.",
            "I read. Therefore I am",
            "Level 42 Bookmage - Class: Librarian",
            "Discovering one book at a time"
    };

    /**
     * Saves a new member to the database if the email is unique and password is strong.
     *
     * @param email        Member's email address.
     * @param displayName  Member's display name.
     * @param password     Member's plain-text password.
     * @return The generated member ID.
     * @throws SQLException If a database error occurs.
     * @throws IllegalArgumentException If the email already exists or the password is too weak.
     */
    public Long saveMember(String email, String displayName, String password) throws SQLException {
        String sql = "INSERT INTO member (email, display_name, password_hash, bio) VALUES (?, ?, ?, ?) RETURNING member_id";

        if (existsByEmail(email)) {
            throw new IllegalArgumentException("ERROR: A member with this email already exists.");
        }
        if (!passwordIsStrong(password)) {
            throw new IllegalArgumentException("ERROR: The password is too weak");

        }

        String hashedPassword = PasswordUtils.hashPassword(password);

        String defaultBio = this.bios[(int) (Math.random() * bios.length)];

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());
            preparedStatement.setString(2, displayName);
            preparedStatement.setString(3, hashedPassword);
            preparedStatement.setString(4, defaultBio);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                System.out.println("Member created " + resultSet.getString("member_id") );
                return resultSet.getLong(1);
            } else {
                throw new SQLException("Could not create a member, not ID was returned.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Updates the display name and password hash for an existing member.
     *
     * @param memberId     The ID of the member to update.
     * @param displayName  New display name.
     * @param passwordHash New hashed password.
     * @return True if the update was successful.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateMember(Long memberId, String displayName, String passwordHash) throws SQLException {
        String sql = "UPDATE member SET display_name = ?, password_hash = ? WHERE member_id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, displayName);
            preparedStatement.setString(2, passwordHash);
            preparedStatement.setLong(3, memberId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Finds a member by their ID.
     *
     * @param memberId The ID of the member.
     * @return Optional containing member data as a map if found.
     * @throws SQLException If a database error occurs.
     */
    public Optional<Map<String, Object>> findById(Long memberId) throws SQLException {
        String sql = "SELECT member_id, email, display_name, created_at, bio, profile_image FROM member WHERE member_id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", resultSet.getLong("member_id"));
                member.put("email", resultSet.getString("email"));
                member.put("displayName", resultSet.getString("display_name"));
                member.put("createdAt", resultSet.getTimestamp("created_at"));
                member.put("bio", resultSet.getString("bio"));
                member.put("profileImage", resultSet.getString("profile_image"));
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a member by their email.
     *
     * @param email Email address.
     * @return Optional containing member data as a map if found.
     * @throws SQLException If a database error occurs.
     */
    public Optional<Map<String, Object>> findByEmail(String email) throws SQLException {
        String sql = "SELECT member_id, email, display_name, password_hash, created_at, bio, profile_image FROM member WHERE email = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", resultSet.getLong("member_id"));
                member.put("email", resultSet.getString("email"));
                member.put("displayName", resultSet.getString("display_name"));
                member.put("passwordHash", resultSet.getString("password_hash"));
                member.put("createdAt", resultSet.getTimestamp("created_at"));
                member.put("bio", resultSet.getString("bio"));
                member.put("profileImage", resultSet.getString("profile_image"));
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a member exists with the given email.
     *
     * @param email Email address.
     * @return True if a member exists.
     * @throws SQLException If a database error occurs.
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member WHERE email = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }

        return false;
    }

    /**
     * Authenticates a member using their email and plain-text password.
     *
     * @param email         Email address.
     * @param plainPassword Plain-text password.
     * @return Optional containing authenticated Member if credentials are valid.
     * @throws SQLException If a database error occurs.
     */

    public Optional<Member> authenticate(String email, String plainPassword) throws SQLException {
        Optional<Map<String, Object>> optionalData = findByEmail(email);

        if (optionalData.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> data = optionalData.get();
        String storedHash = (String) data.get("passwordHash");

        if (PasswordUtils.verifyPassword(plainPassword, storedHash)) { // Antag att du har en sådan klass
            Member member = new Member();
            member.setId(((Number) data.get("id")).longValue());
            member.setUsername((String) data.get("email"));
            member.setName((String) data.get("displayName"));
            member.setBio((String) data.get("bio"));

            return Optional.of(member);
        }

        return Optional.empty(); // Lösenordet stämde inte
    }

    /**
     * Checks if a member exists by their display name (username).
     *
     * @param username Display name.
     * @return True if the username exists.
     * @throws SQLException If a database error occurs.
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member WHERE display_name = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, username.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }

        return false;
    }

    /**
     * Verifies a password by comparing it directly to the stored hash.
     * (NOTE: This method assumes the password is already hashed, which may be insecure.)
     *
     * @param email    Email address.
     * @param password Password to verify.
     * @return True if the password matches the stored hash.
     * @throws SQLException If a database error occurs.
     */
    public boolean verifyPasswordByEmail(String email, String password) throws SQLException {
        String sql = "SELECT password_hash FROM member WHERE email = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("RAW password: " + password);
                String passwordHashFromDB = resultSet.getString("password_hash");
                if (password.equals(passwordHashFromDB)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if a password meets strength requirements.
     *
     * @param password The password to evaluate.
     * @return True if the password is strong.
     */
    private boolean passwordIsStrong(String password) {
        if (password.length() < 12) {
            return false;
        }

        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        if (!password.matches(".*[a-z].*")) {
            return false;
        }

        if (!password.matches(".*\\d.*")) {
            return false;
        }

        return true;
    }
    /**
     * Updates a member's display name and bio.
     * If bio is empty, a random default bio is assigned.
     *
     * @param member The member object containing updated info.
     */
    public void updateProfileInfo(Member member) {
        String sql = "UPDATE member SET display_name = ?, bio = ?, updated_at = now() WHERE email = ?";
        String bio = member.getBio();

        if(bio == null || bio.trim().isEmpty()){
            member.setBio(this.bios[(int) (Math.random() * bios.length)]);
        }

        jdbcTemplate.update(sql, member.getName(), member.getBio(), member.getUsername());
    }

    /**
     * Counts how many friends a member has.
     *
     * @param memberId ID of the member.
     * @return Number of friends.
     * @throws SQLException If a database error occurs.
     */
    public int countFriends(Long memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM friendship WHERE member_1_id = ? OR member_2_id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, memberId);
            stmt.setLong(2, memberId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Updates the profile image path for a member.
     *
     * @param memberId The member's ID.
     * @param filePath The new file path to the profile image.
     */
    public void updateProfilePicture(Long memberId, String filePath) {
        String sql = "UPDATE member SET profile_image = ? WHERE member_id = ?";
        jdbcTemplate.update(sql, filePath, memberId);
    }

    /**
     * Searches for members whose display name contains the given query string,
     * excluding the current user and users who are already friends with them.
     *
     * @param query the search string to match against display names
     * @param currentMemberId the ID of the currently logged-in user
     * @return a list of matching Member objects
     * @throws SQLException if a database access error occurs
     */
    public List<Member> searchMembersByName(String query, Long currentMemberId) throws SQLException {
        List<Member> results = new ArrayList<>();
        String sql = """
        SELECT * FROM member 
        WHERE LOWER(display_name) LIKE LOWER(?) 
        AND member_id != ? 
        AND member_id NOT IN (
            SELECT CASE 
                WHEN member_1_id = ? THEN member_2_id 
                ELSE member_1_id 
            END
            FROM friendship
            WHERE member_1_id = ? OR member_2_id = ?
        )
    """;

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            stmt.setLong(2, currentMemberId);
            stmt.setLong(3, currentMemberId);
            stmt.setLong(4, currentMemberId);
            stmt.setLong(5, currentMemberId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapToMember(rs)); // Du har antagligen redan en sådan metod
            }
        }
        return results;
    }

    /**
     * Maps a single row from the given ResultSet to a Member object.
     *
     * @param rs the ResultSet positioned at the current row
     * @return a Member object populated with data from the current ResultSet row
     * @throws SQLException if a database access error occurs
     */
    private Member mapToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setId(rs.getLong("member_id"));
        member.setUsername(rs.getString("email"));
        member.setName(rs.getString("display_name"));
        member.setPassword(rs.getString("password_hash"));
        member.setBio(rs.getString("bio"));
        member.setProfileImage(rs.getString("profile_image"));
        return member;
    }


}
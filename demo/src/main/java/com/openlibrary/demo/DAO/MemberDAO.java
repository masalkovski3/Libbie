package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.DatabaseController;
import com.openlibrary.demo.controller.SqlHandler;
import com.openlibrary.demo.model.Member;
import com.openlibrary.demo.util.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

//import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MemberDAO {

    @Autowired
    private SqlHandler sqlHandler;

    /**
     * Sparar en ny medlem i databasen
     */
    public Long saveMember(String email, String displayName, String password) throws SQLException {
        String sql = "INSERT INTO member (email, display_name, password_hash) VALUES (?, ?, ?) RETURNING member_id";

        if (existsByEmail(email)) {
            throw new IllegalArgumentException("ERROR: A member with this email already exists.");
        }
        if (!passwordIsStrong(password)) {
            throw new IllegalArgumentException("ERROR: The password is too weak");

        }

        String hashedPassword = PasswordUtils.hashPassword(password);

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());
            preparedStatement.setString(2, displayName);
            preparedStatement.setString(3, hashedPassword);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                System.out.println("Member created " + resultSet.getString("member_id") );
                return resultSet.getLong(1);
            } else {
                throw new SQLException("Kunde inte skapa medlem, ingen ID returnerades");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Uppdaterar en existerande medlem
     */
    public boolean updateMember(Long memberId, String displayName, String passwordHash) throws SQLException {
        String sql = "UPDATE member SET display_name = ?, password_hash = ? WHERE member_id = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, displayName);
            preparedStatement.setString(2, passwordHash);
            preparedStatement.setLong(3, memberId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Hittar en medlem baserat på ID
     */
    public Optional<Map<String, Object>> findById(Long memberId) throws SQLException {
        String sql = "SELECT member_id, email, display_name, created_at FROM member WHERE member_id = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", resultSet.getLong("member_id"));
                member.put("email", resultSet.getString("email"));
                member.put("displayName", resultSet.getString("display_name"));
                member.put("createdAt", resultSet.getTimestamp("created_at"));
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    /**
     * Hittar en medlem baserat på e-post
     */
    public Optional<Map<String, Object>> findByEmail(String email) throws SQLException {
        String sql = "SELECT member_id, email, display_name, password_hash, created_at FROM member WHERE email = ?";

        try (Connection conn = sqlHandler.getConnection();
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
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    /**
     * Kontrollerar om en medlem med en viss e-post redan finns
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member WHERE email = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, email.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }

        return false;
    }


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

            return Optional.of(member);
        }

        return Optional.empty(); // Lösenordet stämde inte
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM member WHERE display_name = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, username.toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }

        return false;
    }


    public boolean verifyPasswordByEmail(String email, String password) throws SQLException {
        String sql = "SELECT password_hash FROM member WHERE email = ?";

        try (Connection conn = sqlHandler.getConnection();
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

}
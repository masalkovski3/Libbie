package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.DatabaseConnection;
import com.openlibrary.demo.model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for handling database operations related to book reviews.
 * This class provides methods to create, read, update, and delete reviews.
 */
@Component
public class ReviewDAO {

    private DatabaseConnection sqlHandler;

    @Autowired
    public ReviewDAO(DatabaseConnection sqlHandler, JdbcTemplate jdbcTemplate) {
        this.sqlHandler = sqlHandler;
    }

    /**
     * Saves a new review for a book or updates an existing one.
     * If the user has already reviewed the book, the existing review is updated.
     *
     * @param memberId The ID of the member submitting the review
     * @param openLibraryId The Open Library ID of the book being reviewed
     * @param score The rating score (1-5)
     * @param reviewText The text content of the review
     * @throws SQLException If a database error occurs
     */
    public void saveReview(Long memberId, String openLibraryId, int score, String reviewText) throws SQLException {
        // Clean any '/works/' prefix (for compatibility with BookshelfDAO)
        String cleanId = openLibraryId.replace("/works/", "");

        // SQL to save/update review (INSERT or UPDATE)
        String sql = "INSERT INTO book_review (member_id, open_library_id, review_score, review_text) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (member_id, open_library_id) DO UPDATE " +
                "SET review_score = EXCLUDED.review_score, review_text = EXCLUDED.review_text";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);
            preparedStatement.setString(2, cleanId);
            preparedStatement.setInt(3, score);
            preparedStatement.setString(4, reviewText);

            preparedStatement.executeUpdate();
        }
    }

    /**
     * Retrieves all reviews for a specific book.
     *
     * @param openLibraryId The Open Library ID of the book
     * @return List of Review objects containing review data
     * @throws SQLException If a database error occurs
     */
    public List<Review> getReviewsByBookId(String openLibraryId) throws SQLException {
        // Clean any '/works/' prefix
        String cleanId = openLibraryId.replace("/works/", "");

        List<Review> reviews = new ArrayList<>();

        String sql = "SELECT br.review_score, br.review_text, br.review_date, m.display_name, m.profile_image " +
                "FROM book_review br " +
                "JOIN member m ON br.member_id = m.member_id " +
                "WHERE br.open_library_id = ? " +
                "ORDER BY br.review_date DESC";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, cleanId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Review review = new Review();
                review.setScore(resultSet.getInt("review_score"));
                review.setText(resultSet.getString("review_text"));
                review.setReviewDate(resultSet.getTimestamp("review_date"));
                review.setMemberName(resultSet.getString("display_name"));
                review.setProfileImage(resultSet.getString("profile_image"));

                reviews.add(review);
            }
        }

        return reviews;
    }

    /**
     * Gets the average review score for a book.
     *
     * @param openLibraryId The Open Library ID of the book
     * @return The average review score as a double, or 0.0 if no reviews exist
     * @throws SQLException If a database error occurs
     */
    public double getAverageReviewScore(String openLibraryId) throws SQLException {
        // Clean any '/works/' prefix
        String cleanId = openLibraryId.replace("/works/", "");

        String sql = "SELECT review_score FROM book WHERE open_library_id = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, cleanId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Double score = resultSet.getDouble("review_score");
                return score != null ? score : 0.0;
            }
        }

        return 0.0;
    }

    /**
     * Retrieves a member's review for a specific book if it exists.
     *
     * @param memberId The ID of the member
     * @param openLibraryId The Open Library ID of the book
     * @return The Review object if found, or null if the member hasn't reviewed the book
     * @throws SQLException If a database error occurs
     */
    public Review getMemberReview(Long memberId, String openLibraryId) throws SQLException {
        // Clean any '/works/' prefix
        String cleanId = openLibraryId.replace("/works/", "");

        String sql = "SELECT review_score, review_text, review_date " +
                "FROM book_review " +
                "WHERE member_id = ? AND open_library_id = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);
            preparedStatement.setString(2, cleanId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Review review = new Review();
                review.setScore(resultSet.getInt("review_score"));
                review.setText(resultSet.getString("review_text"));
                review.setReviewDate(resultSet.getTimestamp("review_date"));
                return review;
            }
        }

        return null;
    }

    /**
     * Deletes a member's review for a specific book.
     *
     * @param memberId The ID of the member
     * @param openLibraryId The Open Library ID of the book
     * @return true if the review was successfully deleted, false if no review was found
     * @throws SQLException If a database error occurs
     */
    public boolean deleteReview(Long memberId, String openLibraryId) throws SQLException {
        // Clean any '/works/' prefix
        String cleanId = openLibraryId.replace("/works/", "");

        String sql = "DELETE FROM book_review WHERE member_id = ? AND open_library_id = ?";

        try (Connection conn = sqlHandler.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);
            preparedStatement.setString(2, cleanId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
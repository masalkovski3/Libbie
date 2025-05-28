package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.DatabaseConnection;
import com.openlibrary.demo.model.Book;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for retrieving book recommendations from the database.
 * This class provides methods to fetch top-rated books based on their review scores.
 */
@Repository
public class RecommendationDAO {
    private DatabaseConnection connection;

    /**
     * Constructs a RecommendationDAO with the specified database connection.
     *
     * @param connection the DatabaseConnection to be used for database connections
     */
    public RecommendationDAO(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Retrieves a list of the top-rated books from the database, ordered by review score.
     *
     * @param limit the maximum number of books to return
     * @return a list of Book objects, ordered by review score in descending order
     * @throws SQLException if a database access error occurs
     */
    public List<Book> getTopRatedBooks(int limit) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM book WHERE review_score IS NOT NULL ORDER BY review_score DESC LIMIT ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book book = new Book(
                            rs.getString("open_library_id"),
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("cover_url"),
                            rs.getDouble("review_score")
                    );
                    books.add(book);
                }
            }
        }
        return books;
    }

    public List<Book> getRecommendedForYou(int limit) throws SQLException {
        List<Book> books = new ArrayList<>();
        return books;
    }
}

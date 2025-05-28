package com.openlibrary.demo.DAO;

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

@Repository
public class RecommendationDAO {
    private DataSource dataSource;

    public RecommendationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Book> getTopRatedBooks(int limit) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM book WHERE review_score IS NOT NULL ORDER BY review_score DESC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
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

    public List<Book> getRecommendedBooksForUser(long memberId, int limit) throws SQLException {
        List<Book> books = new ArrayList<>();

        String sql =
                "WITH high_rated_by_user AS ( " +
                        "    SELECT br1.open_library_id " +
                        "    FROM book_review br1 " +
                        "    WHERE br1.member_id = ? " +
                        "      AND br1.review_score > 3 " +
                        "), " +
                        "similar_users AS ( " +
                        "    SELECT DISTINCT br2.member_id " +
                        "    FROM book_review br2 " +
                        "    JOIN high_rated_by_user hru ON br2.open_library_id = hru.open_library_id " +
                        "    WHERE br2.review_score > 3 " +
                        "      AND br2.member_id != ? " +
                        "), " +
                        "recommended_books AS ( " +
                        "    SELECT br3.open_library_id, AVG(br3.review_score) AS avg_score " +
                        "    FROM book_review br3 " +
                        "    WHERE br3.member_id IN (SELECT member_id FROM similar_users) " +
                        "      AND br3.review_score > 3 " +
                        "      AND br3.open_library_id NOT IN (SELECT open_library_id FROM high_rated_by_user) " +
                        "    GROUP BY br3.open_library_id " +
                        ") " +
                        "SELECT b.open_library_id, b.title, array_to_string(b.authors, ', ') AS authors, b.cover_url, rb.avg_score AS review_score " +
                        "FROM recommended_books rb " +
                        "JOIN book b ON b.open_library_id = rb.open_library_id " +
                        "ORDER BY rb.avg_score DESC " +
                        "LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set parameters (memberId used twice)
            stmt.setLong(1, memberId);
            stmt.setLong(2, memberId);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book();
                book.setWorkId(rs.getString("open_library_id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("authors"));
                book.setCoverUrl(rs.getString("cover_url"));
                book.setReviewScore(rs.getDouble("review_score"));

                books.add(book);
            }


            System.out.println("1 Lista:" + books);
        }
        System.out.println("2 Lista:" + books);
        return books;
    }

}

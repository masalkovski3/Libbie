package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.BookController;
import com.openlibrary.demo.controller.DatabaseConnection;
import com.openlibrary.demo.model.Bookshelf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BookshelfDAO {

    private DatabaseConnection databaseConnection;
    private JdbcTemplate jdbcTemplate;
    private BookController bookController;

    // TODO: Refactor to return List<Bookshelf> once model and DAO are aligned
    public BookshelfDAO(DatabaseConnection sqlHandler, BookController bookController) {
        this.databaseConnection = sqlHandler;
        this.bookController = bookController;
    }

    /**
     * Saves a new bookshelf to the database and returns its generated ID.
     *
     * @param memberId the ID of the member who owns the bookshelf
     * @param name the name of the bookshelf
     * @param description the description of the bookshelf
     * @param isPublic whether the bookshelf is public or private
     * @return the generated ID of the newly created bookshelf
     * @throws SQLException if a database access error occurs
     */
    public Long saveBookshelf(Long memberId, String name, String description, boolean isPublic) throws SQLException {
        String sql = "INSERT INTO bookshelf (name, member_id, description, is_public) VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setLong(2, memberId);
            preparedStatement.setString(3, description != null ? description : "");
            preparedStatement.setBoolean(4, isPublic);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                throw new SQLException("Couldn't create bookshelf, no ID returned.");
            }
        }
    }

    /**
     * Checks if a bookshelf with the given name already exists for the specified member.
     *
     * @param name the name of the bookshelf
     * @param memberId the ID of the member
     * @return true if a bookshelf with the same name exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean existsByNameAndMemberId(String name, Long memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookshelf WHERE name = ? AND member_id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setLong(2, memberId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        }

        return false;
    }

    /**
     * Retrieves all bookshelves for a specific member, ordered by creation time descending.
     *
     * @param memberId the ID of the member
     * @return a list of maps, each representing a bookshelf's properties
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> findByMemberId(Long memberId) throws SQLException {
        List<Map<String, Object>> bookshelves = new ArrayList<>();
        String sql = "SELECT id, name, description, is_public, position FROM bookshelf WHERE member_id = ? ORDER BY created_at DESC";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, memberId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> bookshelf = new HashMap<>();
                bookshelf.put("id", resultSet.getLong("id"));
                bookshelf.put("name", resultSet.getString("name"));
                bookshelf.put("description", resultSet.getString("description"));
                bookshelf.put("isPublic", resultSet.getBoolean("is_public"));
                bookshelf.put("position", resultSet.getInt("position"));
                bookshelves.add(bookshelf);
            }
        }

        return bookshelves;
    }





    /**
     * Retrieves a bookshelf by its ID.
     *
     * @param id the ID of the bookshelf
     * @return an Optional containing the bookshelf map if found, or empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<Map<String, Object>> findById(Long id) throws SQLException {
        String sql = "SELECT id, name, member_id, description, is_public, position FROM bookshelf WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Map<String, Object> bookshelf = new HashMap<>();
                bookshelf.put("id", resultSet.getLong("id"));
                bookshelf.put("name", resultSet.getString("name"));
                bookshelf.put("memberId", resultSet.getLong("member_id"));
                bookshelf.put("description", resultSet.getString("description"));
                bookshelf.put("isPublic", resultSet.getBoolean("is_public"));
                bookshelf.put("position", resultSet.getInt("position"));
                return Optional.of(bookshelf);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves a bookshelf by its ID.
     *
     * @param id the ID of the bookshelf
     * @return an Optional containing the bookshelf map if found, or empty otherwise
     * @throws SQLException if a database access error occurs
     */
    public Bookshelf findByIdReturnBookshelf(Long id) throws SQLException {
        String sql = "SELECT id, name, member_id, description, is_public, position FROM bookshelf WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Bookshelf bookshelf = new Bookshelf();
                bookshelf.setId(resultSet.getLong("id"));
                bookshelf.setName(resultSet.getString("name"));
                bookshelf.setMemberId(resultSet.getLong("member_id"));
                bookshelf.setDescription(resultSet.getString("description"));
                bookshelf.setVisibility(resultSet.getBoolean("is_public"));
                bookshelf.setPosition(resultSet.getInt("position"));
                return bookshelf;
            }
        }
        return null;
    }

    /**
     * Adds a book to a bookshelf. If the book is not in the database, it is first fetched from Open Library.
     *
     * @param bookshelfId the ID of the bookshelf
     * @param openLibraryId the Open Library ID of the book
     * @throws SQLException if a database access error occurs
     */
    public void addBookToShelf(Long bookshelfId, String openLibraryId) throws SQLException {
        // Först kontrollera om boken redan finns i databasen, annars måste vi lägga till den
        ensureBookExists(openLibraryId);

        // Räkna antal böcker på hyllan för att bestämma positionen
        int position = 0;
        String countSql = "SELECT COUNT(*) FROM bookshelf_book WHERE bookshelf_id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(countSql)) {
            preparedStatement.setLong(1, bookshelfId);
            ResultSet countResult = preparedStatement.executeQuery();
            if (countResult.next()) {
                position = countResult.getInt(1);
            }
        }

        // Lägg till boken på hyllan
        String sql = "INSERT INTO bookshelf_book (bookshelf_id, book_id, position) VALUES (?, ?, ?) " +
                "ON CONFLICT (bookshelf_id, book_id) DO NOTHING";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setString(2, openLibraryId);
            preparedStatement.setInt(3, position);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Checks if a book already exists in the local database. If not, fetches book data from the Open Library API.
     *
     * @param openLibraryId the Open Library ID of the book
     * @throws SQLException if a database access error occurs
     */
    private void ensureBookExists(String openLibraryId) throws SQLException {
        String cleanId = openLibraryId.replace("/works/", "");
        String checkSql = "SELECT COUNT(*) FROM book WHERE open_library_id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(checkSql)) {
            preparedStatement.setString(1, cleanId);
            ResultSet checkResult = preparedStatement.executeQuery();

            if (checkResult.next() && checkResult.getInt(1) == 0) {
                // Boken finns inte, hämta information från Open Library API
                try {
                    // Rensa bort eventuella '/works/' prefix
                    //String cleanId = openLibraryId.replace("/works/", "");

                    RestTemplate restTemplate = new RestTemplate();
                    ObjectMapper mapper = new ObjectMapper();

                    String workUrl = bookController.getWorkUrl(cleanId);
                    String response = restTemplate.getForObject(workUrl, String.class);
                    JsonNode root = mapper.readTree(response);

                    String title = bookController.getTitle(root);
                    String description = bookController.getDescription(root);
                    List<String> author = bookController.getAuthorWithKey(root, restTemplate, mapper);

                    Integer coverId = bookController.getCoverId(root);
                    String coverUrl = bookController.getCoverUrl(coverId, cleanId, restTemplate, mapper);
                    int publishedYear = bookController.getPublishYear(cleanId, restTemplate, mapper);

                    addToDatabase(cleanId, title, author, publishedYear, description, coverUrl, conn);

                } catch (Exception e) {
                    System.err.println("Error occurred while fetching data from API: " + e.getMessage());
                    addMinimalBookEntry(cleanId);
                }
            }
        }
    }

    /**
     * Adds a minimal book entry to the database in case API fetching fails in ensureBookExists(String openLibraryId).
     *
     * @param id the Open Library ID of the book
     * @throws SQLException if a database access error occurs
     */
    private void addMinimalBookEntry(String id) throws SQLException {
        // Om API-anropet misslyckas, lägg till en minimal bokpost så att vi kan fortsätta
        String insertSql = "INSERT INTO book (open_library_id, title, authors, published_year, description) " +
                "VALUES (?, ?, ARRAY['Unknown author'], 2000, 'No description available')";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement insertStatement = conn.prepareStatement(insertSql)) {
            insertStatement.setString(1, id);
            insertStatement.setString(2, "Book " + id);
            insertStatement.executeUpdate();
        }
    }

    /**
     * Adds a fully populated book entry to the database.
     *
     * @param cleanId the sanitized Open Library ID
     * @param title the title of the book
     * @param authors the authors of the book
     * @param publishedYear the year the book was published
     * @param description the description of the book
     * @param coverUrl the URL to the book's cover image
     * @throws SQLException if a database access error occurs
     */
    private void addToDatabase(String cleanId, String title, List<String> authors, int publishedYear, String description, String coverUrl, Connection connection) throws SQLException {
        String insertSql = "INSERT INTO book (open_library_id, title, authors, published_year, description, cover_url) VALUES (?, ?, ?, ?, ?, ?)";
        java.sql.Array sqlAuthors = connection.createArrayOf("text", authors.toArray());

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement insertStatement = conn.prepareStatement(insertSql)) {
            insertStatement.setString(1, cleanId);
            insertStatement.setString(2, title);
            insertStatement.setArray(3, sqlAuthors);
            insertStatement.setInt(4, publishedYear);
            insertStatement.setString(5, description);
            insertStatement.setString(6, coverUrl);

            insertStatement.executeUpdate();
            System.out.println("Book added to database: " + title);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a list of authors to an array of strings.
     *
     * <p>If the input list is {@code null}, the method returns an empty array.</p>
     *
     * @param authors the list of author names to convert
     * @return an array containing the same author names, or an empty array if the input is {@code null}
     */
    private static Object[] convertAuthorsListToArray(List<String> authors) {
        if (authors == null) {
            return new Object[0];
        }
        return authors.toArray(new Object[0]);
    }

    /**
     * Removes a book from a bookshelf.
     *
     * @param bookshelfId the ID of the bookshelf
     * @param openLibraryId the Open Library ID of the book
     * @return true if the book was successfully removed, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean removeBookFromShelf(Long bookshelfId, String openLibraryId) throws SQLException {
        // SQL-sats för att ta bort boken från bookshelf_book-tabellen
        String sql = "DELETE FROM bookshelf_book WHERE bookshelf_id = ? AND (book_id = ? OR book_id = '/works/' || ?)";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            // Sätt parametrar för bookshelfId och openLibraryId
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setString(2, openLibraryId);
            preparedStatement.setString(3, openLibraryId);

            // Utför uppdateringen
            int rowsAffected = preparedStatement.executeUpdate();

            // Om en bok tas bort, uppdatera positionerna för de återstående böckerna
            if (rowsAffected > 0) {
                reorderBooks(bookshelfId); // Uppdatera böckernas positioner
            }

            // Returnera true om boken togs bort (dvs. minst en rad påverkades)
            return rowsAffected > 0;
        }
    }


    /**
     * Reorders the positions of books on a bookshelf after one is removed.
     *
     * @param bookshelfId the ID of the bookshelf
     * @throws SQLException if a database access error occurs
     */
    private void reorderBooks(Long bookshelfId) throws SQLException {
        String sql = "WITH numbered AS (" +
                "  SELECT book_id, ROW_NUMBER() OVER (ORDER BY position) - 1 AS new_pos " +
                "  FROM bookshelf_book " +
                "  WHERE bookshelf_id = ? " +
                ") " +
                "UPDATE bookshelf_book SET position = numbered.new_pos " +
                "FROM numbered " +
                "WHERE bookshelf_book.bookshelf_id = ? " +
                "  AND bookshelf_book.book_id = numbered.book_id";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setLong(2, bookshelfId);
            preparedStatement.executeUpdate();
        }
    }


    /**
     * Retrieves all books on a specific bookshelf.
     *
     * @param bookshelfId the ID of the bookshelf
     * @return a list of maps, each representing a book and its properties
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> findBooksByBookshelfId(Long bookshelfId) throws SQLException {
        List<Map<String, Object>> books = new ArrayList<>();
        String sql = "SELECT b.open_library_id, b.title, b.authors, b.published_year, b.cover_url, bb.position " +
                "FROM bookshelf_book bb " +
                "JOIN book b ON bb.book_id = b.open_library_id " +
                "WHERE bb.bookshelf_id = ? " +
                "ORDER BY bb.position";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Map<String, Object> book = new HashMap<>();
                book.put("openLibraryId", resultSet.getString("open_library_id"));
                book.put("title", resultSet.getString("title"));
                book.put("authors", resultSet.getArray("authors"));
                book.put("publishedYear", resultSet.getInt("published_year"));
                book.put("coverUrl", resultSet.getString("cover_url"));
                book.put("position", resultSet.getInt("position"));
                books.add(book);
            }
        }

        return books;
    }

    /**
     * Deletes a bookshelf from the database.
     *
     * @param bookshelfId the ID of the bookshelf to delete
     * @return true if the bookshelf was deleted, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteBookshelf(Long bookshelfId) throws SQLException {
        String sql = "DELETE FROM bookshelf WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Updates the name of a bookshelf. Ensures the new name is unique for the user.
     *
     * @param bookshelfId the ID of the bookshelf
     * @param newName the new name to set
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs or if the new name already exists
     */
    public boolean updateBookshelfName(Long bookshelfId, String newName) throws SQLException {
        // Först hämta bokhyllan för att få medlems-ID
        Optional<Map<String, Object>> bookshelfOpt = findById(bookshelfId);
        if (bookshelfOpt.isEmpty()) {
            return false;
        }

        Long memberId = (Long) bookshelfOpt.get().get("memberId");

        // Kontrollera att det nya namnet är unikt för denna medlem
        if (existsByNameAndMemberId(newName, memberId)) {
            throw new SQLException("A bookshelf named '" + newName + "' already exists in your profile.");
        }

        String sql = "UPDATE bookshelf SET name = ? WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, newName);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Updates the description of a bookshelf.
     *
     * @param bookshelfId the ID of the bookshelf
     * @param description the new description
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateBookshelfDescription(Long bookshelfId, String description) throws SQLException {
        String sql = "UPDATE bookshelf SET description = ? WHERE id = ?";
        System.out.println("In BookshelfDAO");

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            System.out.println("Uppdaterar beskrivning för bookshelfId: " + bookshelfId + " till: " + description);

            preparedStatement.setString(1, description);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Antal ändrade rader: " + rowsAffected);

            return rowsAffected > 0;
        }
    }

    /**
     * Updates the visibility of a bookshelf (public/private).
     *
     * @param bookshelfId the ID of the bookshelf
     * @param isPublic the new visibility setting
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateBookshelfVisibility(Long bookshelfId, boolean isPublic) throws SQLException {
        String sql = "UPDATE bookshelf SET is_public = ? WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, isPublic);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Retrieves all public bookshelves belonging to the specified member.
     *
     * Each bookshelf is returned as a Map<String, Object>, where the keys correspond
     * to column names in the bookshelf table (e.g., {id}, {name}, {is_public}).
     *
     * @param memberId the ID of the member whose public bookshelves should be retrieved
     * @return a list of maps, each representing a public bookshelf and its database fields
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> findPublicByMemberId(Long memberId) throws SQLException {
        String sql = "SELECT * FROM bookshelf WHERE member_id = ? AND is_public = true ORDER BY created_at DESC";
        return jdbcTemplate.queryForList(sql, memberId);
    }

    /**
     * Retrieves all public bookshelves for a given member.
     *
     * @param memberId The ID of the member whose public bookshelves are requested.
     * @return A list of {@link Bookshelf} objects that are marked as public.
     * @throws SQLException if a database access error occurs.
     */
    public List<Bookshelf> getPublicBookshelvesByMemberId(Long memberId) throws SQLException {
        List<Bookshelf> bookshelves = new ArrayList<>();
        String sql = "SELECT * FROM bookshelf WHERE member_id = ? AND is_public = TRUE";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, memberId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Bookshelf shelf = new Bookshelf();
                shelf.setId(rs.getLong("id"));
                shelf.setMemberId(rs.getLong("member_id"));
                shelf.setName(rs.getString("name"));
                shelf.setDescription(rs.getString("description"));
                shelf.setVisibility(rs.getBoolean("is_public"));
                // Lägg till fler fält om Bookshelf har fler attribut
                bookshelves.add(shelf);
            }

        }

        return bookshelves;
    }

    public void setVisibility(Long bookshelfId, boolean isPublic) throws SQLException {
        String sql = "UPDATE bookshelf SET is_public = ? WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isPublic);
            stmt.setLong(2, bookshelfId);
            stmt.executeUpdate();
        }
    }


}
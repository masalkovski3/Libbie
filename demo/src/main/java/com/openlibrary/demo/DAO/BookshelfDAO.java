package com.openlibrary.demo.DAO;

import com.openlibrary.demo.controller.DatabaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BookshelfDAO {

    @Autowired
    private DatabaseController databaseController;

    /**
     * Sparar en ny bokhylla och returnerar dess ID
     */
    public Long saveBookshelf(Long memberId, String name, String description, boolean isPublic) throws SQLException {
        String sql = "INSERT INTO bookshelf (name, member_id, description, is_public) VALUES (?, ?, ?, ?) RETURNING id";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setLong(2, memberId);
            preparedStatement.setString(3, description != null ? description : "");
            preparedStatement.setBoolean(4, isPublic);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                throw new SQLException("Kunde inte skapa bokhyllan, ingen ID returnerades");
            }
        }
    }

    /**
     * Kontrollerar om en bokhylla med samma namn redan finns för en medlem
     */
    public boolean existsByNameAndMemberId(String name, Long memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookshelf WHERE name = ? AND member_id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
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
     * Hämtar alla bokhyllor för en viss medlem
     */
    public List<Map<String, Object>> findByMemberId(Long memberId) throws SQLException {
        List<Map<String, Object>> bookshelves = new ArrayList<>();
        String sql = "SELECT id, name, description, is_public, position FROM bookshelf WHERE member_id = ? ORDER BY position";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
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
     * Hämtar en bokhylla baserat på ID
     */
    public Optional<Map<String, Object>> findById(Long id) throws SQLException {
        String sql = "SELECT id, name, member_id, description, is_public, position FROM bookshelf WHERE id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
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
     * Lägger till en bok på en bokhylla
     */
    public void addBookToShelf(Long bookshelfId, String openLibraryId) throws SQLException {
        // Först kontrollera om boken redan finns i databasen, annars måste vi lägga till den
        ensureBookExists(openLibraryId);

        // Räkna antal böcker på hyllan för att bestämma positionen
        int position = 0;
        String countSql = "SELECT COUNT(*) FROM bookshelf_book WHERE bookshelf_id = ?";
        try (PreparedStatement countStatement = databaseController.connection.prepareStatement(countSql)) {
            countStatement.setLong(1, bookshelfId);
            ResultSet countResult = countStatement.executeQuery();
            if (countResult.next()) {
                position = countResult.getInt(1);
            }
        }

        // Lägg till boken på hyllan
        String sql = "INSERT INTO bookshelf_book (bookshelf_id, book_id, position) VALUES (?, ?, ?) " +
                "ON CONFLICT (bookshelf_id, book_id) DO NOTHING";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setString(2, openLibraryId);
            preparedStatement.setInt(3, position);
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Säkerställer att boken finns i databasen
     * Om boken inte finns, hämta information från Open Library API och lägg till den
     */
    private void ensureBookExists(String openLibraryId) throws SQLException {
        // Kontrollera om boken redan finns i databasen
        String checkSql = "SELECT COUNT(*) FROM book WHERE open_library_id = ?";
        try (PreparedStatement checkStatement = databaseController.connection.prepareStatement(checkSql)) {
            checkStatement.setString(1, openLibraryId);
            ResultSet checkResult = checkStatement.executeQuery();

            if (checkResult.next() && checkResult.getInt(1) == 0) {
                // Boken finns inte, hämta information från Open Library API
                try {
                    // Rensa bort eventuella '/works/' prefix
                    String cleanId = openLibraryId.replace("/works/", "");

                    // Skapa RestTemplate för API-anrop
                    RestTemplate restTemplate = new RestTemplate();
                    ObjectMapper mapper = new ObjectMapper();

                    // Hämta information om verket
                    String workUrl = "https://openlibrary.org/works/" + cleanId + ".json";
                    String response = restTemplate.getForObject(workUrl, String.class);
                    JsonNode root = mapper.readTree(response);

                    // Extrahera boktitel
                    String title = root.path("title").asText("Okänd titel");

                    // Extrahera beskrivning
                    String description = "";
                    JsonNode descNode = root.path("description");
                    if (descNode.isTextual()) {
                        description = descNode.asText();
                    } else if (descNode.has("value")) {
                        description = descNode.path("value").asText();
                    }

                    if (description.isEmpty()) {
                        description = "Ingen beskrivning tillgänglig";
                    }

                    // Hantera författare
                    List<String> authors = new ArrayList<>();
                    JsonNode authorsNode = root.path("authors");

                    if (authorsNode.isArray()) {
                        for (JsonNode authorNode : authorsNode) {
                            if (authorNode.has("author") && authorNode.path("author").has("key")) {
                                String authorKey = authorNode.path("author").path("key").asText();

                                // Hämta författarnamn via separat API-anrop
                                String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                                String authorResponse = restTemplate.getForObject(authorUrl, String.class);
                                JsonNode authorRoot = mapper.readTree(authorResponse);

                                String authorName = authorRoot.path("name").asText("Okänd författare");
                                authors.add(authorName);
                            }
                        }
                    }

                    if (authors.isEmpty()) {
                        authors.add("Okänd författare");
                    }

                    // Extrahera publiceringsår
                    int publishedYear = 0;
                    JsonNode firstPublishYear = root.path("first_publish_year");
                    if (!firstPublishYear.isMissingNode() && firstPublishYear.isInt()) {
                        publishedYear = firstPublishYear.asInt();
                    } else {
                        // Använd nuvarande år som standard om publiceringsår saknas
                        publishedYear = java.time.Year.now().getValue();
                    }

                    // Extrahera omslagsbild
                    String coverUrl = "";
                    JsonNode covers = root.path("covers");
                    if (covers.isArray() && covers.size() > 0) {
                        int coverId = covers.get(0).asInt();
                        coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
                    }

                    // Skapa SQL för att lägga till boken i databasen
                    String insertSql = "INSERT INTO book (open_library_id, title, authors, published_year, description, cover_url) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStatement = databaseController.connection.prepareStatement(insertSql)) {
                        insertStatement.setString(1, openLibraryId);
                        insertStatement.setString(2, title);

                        // Konvertera författarlistan till PostgreSQL array
                        java.sql.Array sqlAuthors = databaseController.connection.createArrayOf("text", authors.toArray());
                        insertStatement.setArray(3, sqlAuthors);

                        insertStatement.setInt(4, publishedYear);
                        insertStatement.setString(5, description);
                        insertStatement.setString(6, coverUrl);

                        insertStatement.executeUpdate();
                        System.out.println("Lade till ny bok i databasen: " + title);
                    }

                } catch (Exception e) {
                    System.err.println("Fel vid hämtning av bokdetaljer från API: " + e.getMessage());

                    // Om API-anropet misslyckas, lägg till en minimal bokpost så att vi kan fortsätta
                    String insertSql = "INSERT INTO book (open_library_id, title, authors, published_year, description) " +
                            "VALUES (?, ?, ARRAY['Okänd författare'], 2000, 'Beskrivning saknas')";
                    try (PreparedStatement insertStatement = databaseController.connection.prepareStatement(insertSql)) {
                        insertStatement.setString(1, openLibraryId);
                        insertStatement.setString(2, "Bok " + openLibraryId);
                        insertStatement.executeUpdate();
                    }
                }
            }
        }
    }

    //Tar bort en bok från en bokhylla
    public boolean removeBookFromShelf(Long bookshelfId, String openLibraryId) throws SQLException {
        String sql = "DELETE FROM bookshelf_book WHERE bookshelf_id = ? AND book_id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setString(2, openLibraryId);

            int rowsAffected = preparedStatement.executeUpdate();

            // Uppdatera positioner för återstående böcker
            if (rowsAffected > 0) {
                reorderBooks(bookshelfId);
            }

            return rowsAffected > 0;
        }
    }

    //Uppdaterar positioner för böcker efter borttagning
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

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);
            preparedStatement.setLong(2, bookshelfId);
            preparedStatement.executeUpdate();
        }
    }

    //Hämtar alla böcker på en bokhylla
    public List<Map<String, Object>> findBooksByBookshelfId(Long bookshelfId) throws SQLException {
        List<Map<String, Object>> books = new ArrayList<>();
        String sql = "SELECT b.open_library_id, b.title, b.authors, b.published_year, b.cover_url, bb.position " +
                "FROM bookshelf_book bb " +
                "JOIN book b ON bb.book_id = b.open_library_id " +
                "WHERE bb.bookshelf_id = ? " +
                "ORDER BY bb.position";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
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

    //Tar bort en bokhylla
    public boolean deleteBookshelf(Long bookshelfId) throws SQLException {
        String sql = "DELETE FROM bookshelf WHERE id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    //Uppdaterar namnet på en bokhylla
    public boolean updateBookshelfName(Long bookshelfId, String newName) throws SQLException {
        // Först hämta bokhyllan för att få medlems-ID
        Optional<Map<String, Object>> bookshelfOpt = findById(bookshelfId);
        if (bookshelfOpt.isEmpty()) {
            return false;
        }

        Long memberId = (Long) bookshelfOpt.get().get("memberId");

        // Kontrollera att det nya namnet är unikt för denna medlem
        if (existsByNameAndMemberId(newName, memberId)) {
            throw new SQLException("En bokhylla med namnet '" + newName + "' finns redan för denna användare");
        }

        String sql = "UPDATE bookshelf SET name = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setString(1, newName);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Uppdaterar en bokhyllas beskrivning
     */
    public boolean updateBookshelfDescription(Long bookshelfId, String description) throws SQLException {
        String sql = "UPDATE bookshelf SET description = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setString(1, description);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Uppdaterar en bokhyllas synlighet (publik/privat)
     */
    public boolean updateBookshelfVisibility(Long bookshelfId, boolean isPublic) throws SQLException {
        String sql = "UPDATE bookshelf SET is_public = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = databaseController.connection.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, isPublic);
            preparedStatement.setLong(2, bookshelfId);

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
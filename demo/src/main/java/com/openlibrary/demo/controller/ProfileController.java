package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.DAO.BookshelfDAO;
import com.openlibrary.demo.DAO.MemberDAO;
import com.openlibrary.demo.model.Book;
import com.openlibrary.demo.model.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller som hanterar användarprofilsidan och tillhörande API-funktioner
 * såsom hantering av bokhyllor och sökning av böcker via OpenLibrary API.
 *
 * @author Linn Otendal, Emmi Masalkovski
 */
//TODO: Ändra så att demo-användare inte används vid inlogg
@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private MemberDAO memberDAO;

    @Autowired
    private BookshelfDAO bookshelfDAO;

    // Temporär inloggad användare för demo (ersätt med riktig sessionshantering senare)
    private static final long DEMO_USER_ID = 1;

    /**
     * Visar profilsidan med användarens information, bokhyllor och tillhörande böcker.
     *
     * @param model Model-objekt som används för att skicka data till vyn.
     * @return Namnet på HTML-vyn ("profile").
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @GetMapping
    public String profile(Model model, HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");

        if (currentMember == null) {
            return "redirect:/logIn";
        }

        Long memberId = currentMember.getId();

        try {
            // Se till att demo-användare finns
            //ensureDemoUserExists();

            // Hämta medlemsinformation
            Optional<Map<String, Object>> memberOpt = memberDAO.findById(memberId);
            if (memberOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            model.addAttribute("member", memberOpt.get());

            // Hämta användarens bokhyllor
            List<Map<String, Object>> bookshelves = bookshelfDAO.findByMemberId(memberId);
            model.addAttribute("bookshelves", bookshelves);

            // Hämta böcker för varje bokhylla
            Map<Long, List<Book>> booksByShelf = new HashMap<>();

            for (Map<String, Object> shelf : bookshelves) {
                Long shelfId = (Long) shelf.get("id");
                List<Map<String, Object>> bookData = bookshelfDAO.findBooksByBookshelfId(shelfId);
                List<Book> books = new ArrayList<>();

                for (Map<String, Object> bookMap : bookData) {
                    String workId = (String) bookMap.get("openLibraryId");
                    String title = (String) bookMap.get("title");
                    String coverUrl = (String) bookMap.get("coverUrl");

                    // Hämta författare från authors-array, om tillgängligt
                    Object authorsObj = bookMap.get("authors");
                    String author = "Unknown";
                    if (authorsObj != null && authorsObj instanceof java.sql.Array) {
                        String[] authors = (String[]) ((java.sql.Array) authorsObj).getArray();
                        if (authors.length > 0) {
                            author = authors[0];
                        }
                    }

                    books.add(new Book(title, author, workId, coverUrl));
                }

                booksByShelf.put(shelfId, books);
            }

            model.addAttribute("booksByShelf", booksByShelf);
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            model.addAttribute("error", "A database error occurred: " + e.getMessage());
        } catch (ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
        }

        return "profile";
    }

    /**
     * Säkerställer att demo-användaren existerar i databasen.
     *
     * @throws SQLException om databasåtkomst misslyckas.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    private void ensureDemoUserExists() throws SQLException {
        if (!memberDAO.findById(DEMO_USER_ID).isPresent()) {
            // Skapa en stark demo-lösenordshash som uppfyller kraven
            String demoPasswordHash = "Password123456"; // Senare ska detta vara krypterat

            try {
                // Försöka spara en ny användare
                memberDAO.saveMember("libbie@example.com", "Libbie Demo", demoPasswordHash);
                System.out.println("Demo-user created");
            } catch (Exception e) {
                System.err.println("Coudn't create demo-user: " + e.getMessage());
                // Vi kan inte göra mycket mer här, förhoppningsvis finns användaren redan
            }
        }
    }

    /**
     * Skapar en ny bokhylla för den inloggade användaren.
     *
     * @param name Namn på bokhyllan.
     * @param description (Valfritt) Beskrivning av bokhyllan.
     * @param isPublic Anger om bokhyllan ska vara publik.
     * @return ResponseEntity med ny bokhyllas information eller felmeddelande.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @PostMapping("/bookshelves")
    @ResponseBody
    public ResponseEntity<?> createBookshelf(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") boolean isPublic,
            HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera om namn redan används
            if (bookshelfDAO.existsByNameAndMemberId(name, memberId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "A bookshelf with this name already exists"));
            }

            Long bookshelfId = bookshelfDAO.saveBookshelf(memberId, name, description, isPublic);

            Map<String, Object> response = new HashMap<>();
            response.put("id", bookshelfId);
            response.put("name", name);
            response.put("description", description);
            response.put("isPublic", isPublic);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lägger till en bok i en specifik bokhylla.
     *
     * @param bookshelfId ID för bokhyllan.
     * @param workId OpenLibrary work ID för boken.
     * @return ResponseEntity som indikerar om operationen lyckades.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @PostMapping("/bookshelves/{bookshelfId}/books")
    @ResponseBody
    public ResponseEntity<?> addBookToShelf(
            @PathVariable Long bookshelfId,
            @RequestParam String workId,
            HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "This bookshelf belongs to another user"));
            }

            bookshelfDAO.addBookToShelf(bookshelfId, workId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tar bort en bok från en bokhylla.
     *
     * @param bookshelfId ID för bokhyllan.
     * @param workId OpenLibrary work ID för boken.
     * @return ResponseEntity som indikerar om borttagningen lyckades.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @DeleteMapping("/bookshelves/{bookshelfId}/books/{workId}")
    @ResponseBody
    public ResponseEntity<?> removeBookFromShelf(
            @PathVariable Long bookshelfId,
            @PathVariable String workId,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bookshelf belongs to another user"));
            }

            boolean removed = bookshelfDAO.removeBookFromShelf(bookshelfId, workId);
            if (removed) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Coudn't find this book in the bookshelf"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Tar bort en bokhylla.
     *
     * @param bookshelfId ID för bokhyllan som ska tas bort.
     * @return ResponseEntity som indikerar resultatet av borttagningen.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @DeleteMapping("/bookshelves/{bookshelfId}")
    @ResponseBody
    public ResponseEntity<?> deleteBookshelf(
            @PathVariable Long bookshelfId,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bookshelf belongs to another user"));
            }

            boolean deleted = bookshelfDAO.deleteBookshelf(bookshelfId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Byter namn på en befintlig bokhylla.
     *
     * @param bookshelfId ID för bokhyllan.
     * @param name Det nya namnet.
     * @return ResponseEntity med resultatet av uppdateringen.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @PutMapping("/bookshelves/{bookshelfId}")
    @ResponseBody
    public ResponseEntity<?> renameBookshelf(
            @PathVariable Long bookshelfId,
            @RequestParam String name,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfName(bookshelfId, name);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "name", name));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Uppdaterar beskrivningen för en bokhylla.
     *
     * @param bookshelfId ID för bokhyllan.
     * @param description Ny beskrivning.
     * @return ResponseEntity med resultatet av uppdateringen.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @PutMapping("/bookshelves/{bookshelfId}/description")
    @ResponseBody
    public ResponseEntity<?> updateBookshelfDescription(
            @PathVariable Long bookshelfId,
            @RequestParam String description,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfDescription(bookshelfId, description);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "description", description));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Uppdaterar synligheten (publik/privat) för en bokhylla.
     *
     * @param bookshelfId ID för bokhyllan.
     * @param isPublic Ny synlighetsstatus.
     * @return ResponseEntity med resultatet av uppdateringen.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    @PutMapping("/bookshelves/{bookshelfId}/visibility")
    @ResponseBody
    public ResponseEntity<?> updateBookshelfVisibility(
            @PathVariable Long bookshelfId,
            @RequestParam boolean isPublic,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long memberId = currentMember.getId();

        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfVisibility(bookshelfId, isPublic);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "isPublic", isPublic));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Söker efter böcker via OpenLibrary API och returnerar resultat i JSON-format.
     *
     * @param query Sökterm för boktitlar/författare.
     * @return ResponseEntity med lista över matchande böcker.
     *
     * @author Linn Otendal, Emmi Masalkovski
     */
    /*
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchBooks(@RequestParam String query) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://openlibrary.org/search.json?q=" + query + "&limit=10";

            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode docs = root.path("docs");

            List<Map<String, Object>> results = new ArrayList<>();

            for (JsonNode doc : docs) {
                Map<String, Object> book = new HashMap<>();
                book.put("title", doc.path("title").asText("Unknown title"));

                // Hämta författare
                JsonNode authors = doc.path("author_name");
                String author = (authors.isArray() && authors.size() > 0) ?
                        authors.get(0).asText() : "Unknown author";
                book.put("author", author);

                // Hämta Work ID
                String key = doc.path("key").asText();
                book.put("workId", key);

                // Hämta omslag om tillgängligt
                String coverUrl = "";
                if (doc.has("cover_i")) {
                    int coverId = doc.path("cover_i").asInt();
                    coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
                }
                book.put("coverUrl", coverUrl);

                results.add(book);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
     */

    /**
     * Handles GET requests to "/search" and queries the OpenLibrary API with the given search string.
     *
     * @param query the search query string
     * @return ResponseEntity with list of books or error message
     *
     * @author Linn Otendal, Emmi Masalkovski, Amelie Music
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<?> searchBooks(@RequestParam String query) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://openlibrary.org/search.json?q=" + query + "&limit=10";
            String response = restTemplate.getForObject(url, String.class);

            List<Map<String, Object>> books = parseBookResults(response);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Parses the full JSON response string into a list of book maps.
     *
     * @param response JSON string from the OpenLibrary API
     * @return list of book maps
     * @throws IOException if JSON parsing fails
     *
     * @author Linn Otendal, Emmi Masalkovski, Amelie Music
     */
    private List<Map<String, Object>> parseBookResults(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(response);
        } catch (IOException e) {
            throw new IOException("Failed to parse response", e);
        }

        JsonNode docs = root.path("docs");
        List<Map<String, Object>> results = new ArrayList<>();

        for (JsonNode doc : docs) {
            results.add(extractBookInfo(doc));
        }

        return results;
    }

    /**
     * Extracts relevant book information from a single JSON node.
     *
     * @param doc a JSON node representing one book
     * @return a map with title, author, workId, and coverUrl
     *
     * @author Linn Otendal, Emmi Masalkovski, Amelie Music
     */
    private Map<String, Object> extractBookInfo(JsonNode doc) {
        Map<String, Object> book = new HashMap<>();
        book.put("title", doc.path("title").asText("Unknown title"));

        JsonNode authors = doc.path("author_name");
        String author = (authors.isArray() && authors.size() > 0) ?
                authors.get(0).asText() : "Unknown author";
        book.put("author", author);

        book.put("workId", doc.path("key").asText());

        String coverUrl = "";
        if (doc.has("cover_i")) {
            int coverId = doc.path("cover_i").asInt();
            coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
        }
        book.put("coverUrl", coverUrl);

        return book;
    }


}
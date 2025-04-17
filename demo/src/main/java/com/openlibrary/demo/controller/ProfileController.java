package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.DAO.BookshelfDAO;
import com.openlibrary.demo.DAO.MemberDAO;
import com.openlibrary.demo.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private MemberDAO memberDAO;

    @Autowired
    private BookshelfDAO bookshelfDAO;

    // Temporär inloggad användare för demo (ersätt med riktig sessionshantering senare)
    private static final long DEMO_USER_ID = 1;

    // Visa profilsidan
    @GetMapping
    public String profile(Model model) {
        try {
            // Se till att demo-användare finns
            ensureDemoUserExists();

            // Hämta medlemsinformation
            Optional<Map<String, Object>> memberOpt = memberDAO.findById(DEMO_USER_ID);
            if (memberOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Användare hittades inte");
            }
            model.addAttribute("member", memberOpt.get());

            // Hämta användarens bokhyllor
            List<Map<String, Object>> bookshelves = bookshelfDAO.findByMemberId(DEMO_USER_ID);
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
                    String author = "Okänd";
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
            System.err.println("Databasfel: " + e.getMessage());
            model.addAttribute("error", "Ett databasfel inträffade: " + e.getMessage());
        } catch (ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
        }

        return "profile";
    }

    // Säkerställer att demo-användaren finns för testning
    private void ensureDemoUserExists() throws SQLException {
        if (!memberDAO.findById(DEMO_USER_ID).isPresent()) {
            // Skapa en stark demo-lösenordshash som uppfyller kraven
            String demoPasswordHash = "Password123456"; // Senare ska detta vara krypterat

            try {
                // Försöka spara en ny användare
                memberDAO.saveMember("libbie@example.com", "Libbie Demo", demoPasswordHash);
                System.out.println("Demo-användare skapad");
            } catch (Exception e) {
                System.err.println("Kunde inte skapa demo-användare: " + e.getMessage());
                // Vi kan inte göra mycket mer här, förhoppningsvis finns användaren redan
            }
        }
    }

    // API för att skapa en ny bokhylla
    @PostMapping("/bookshelves")
    @ResponseBody
    public ResponseEntity<?> createBookshelf(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") boolean isPublic) {
        try {
            // Kontrollera om namn redan används
            if (bookshelfDAO.existsByNameAndMemberId(name, DEMO_USER_ID)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "En bokhylla med detta namn finns redan"));
            }

            Long bookshelfId = bookshelfDAO.saveBookshelf(DEMO_USER_ID, name, description, isPublic);

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

    // API för att lägga till en bok i en bokhylla
    @PostMapping("/bookshelves/{bookshelfId}/books")
    @ResponseBody
    public ResponseEntity<?> addBookToShelf(
            @PathVariable Long bookshelfId,
            @RequestParam String workId) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            bookshelfDAO.addBookToShelf(bookshelfId, workId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // API för att ta bort en bok från en bokhylla
    @DeleteMapping("/bookshelves/{bookshelfId}/books/{workId}")
    @ResponseBody
    public ResponseEntity<?> removeBookFromShelf(
            @PathVariable Long bookshelfId,
            @PathVariable String workId) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            boolean removed = bookshelfDAO.removeBookFromShelf(bookshelfId, workId);
            if (removed) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Boken hittades inte i bokhyllan"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // API för att ta bort en bokhylla
    @DeleteMapping("/bookshelves/{bookshelfId}")
    @ResponseBody
    public ResponseEntity<?> deleteBookshelf(@PathVariable Long bookshelfId) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            boolean deleted = bookshelfDAO.deleteBookshelf(bookshelfId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // API för att byta namn på en bokhylla
    @PutMapping("/bookshelves/{bookshelfId}")
    @ResponseBody
    public ResponseEntity<?> renameBookshelf(
            @PathVariable Long bookshelfId,
            @RequestParam String name) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            boolean updated = bookshelfDAO.updateBookshelfName(bookshelfId, name);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "name", name));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // API för att uppdatera en bokhyllas beskrivning
    @PutMapping("/bookshelves/{bookshelfId}/description")
    @ResponseBody
    public ResponseEntity<?> updateBookshelfDescription(
            @PathVariable Long bookshelfId,
            @RequestParam String description) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            boolean updated = bookshelfDAO.updateBookshelfDescription(bookshelfId, description);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "description", description));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // API för att uppdatera en bokhyllas synlighet (publik/privat)
    @PutMapping("/bookshelves/{bookshelfId}/visibility")
    @ResponseBody
    public ResponseEntity<?> updateBookshelfVisibility(
            @PathVariable Long bookshelfId,
            @RequestParam boolean isPublic) {
        try {
            // Kontrollera att bokhyllan tillhör användaren
            Optional<Map<String, Object>> bookshelfOpt = bookshelfDAO.findById(bookshelfId);
            if (bookshelfOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }

            // Jämför primitiva värden med ==
            Long memberId = (Long) bookshelfOpt.get().get("memberId");
            if (DEMO_USER_ID != memberId) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan tillhör inte denna användare"));
            }

            boolean updated = bookshelfDAO.updateBookshelfVisibility(bookshelfId, isPublic);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "isPublic", isPublic));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bokhyllan hittades inte"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint för att hämta JSON-format av sökresultat för JS-användning i klienten
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
                book.put("title", doc.path("title").asText("Okänd titel"));

                // Hämta författare
                JsonNode authors = doc.path("author_name");
                String author = (authors.isArray() && authors.size() > 0) ?
                        authors.get(0).asText() : "Okänd författare";
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
}
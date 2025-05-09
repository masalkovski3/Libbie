package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.DAO.BookshelfDAO;
import com.openlibrary.demo.DAO.MemberDAO;
import com.openlibrary.demo.DAO.FriendshipDAO;
import com.openlibrary.demo.model.Book;
import com.openlibrary.demo.model.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    private MemberDAO memberDAO;
    private BookshelfDAO bookshelfDAO;
    private FriendshipDAO friendshipDAO;

    // Temporär inloggad användare för demo (ersätt med riktig sessionshantering senare)
    private static final long DEMO_USER_ID = 1;

    public ProfileController(MemberDAO memberDAO, BookshelfDAO bookshelfDAO, FriendshipDAO friendshipDAO) {
        this.memberDAO = memberDAO;
        this.bookshelfDAO = bookshelfDAO;
        this.friendshipDAO = friendshipDAO;
    }

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
        System.out.println("Current member: " + currentMember);

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

            Map<String, Object> memberData = memberOpt.get();
            int friendCount = memberDAO.countFriends(memberId);
            memberData.put("friendCount", friendCount);
            List<Long> friendIds = friendshipDAO.findFriendIds(memberId);
            List<Map<String, Object>> friendProfiles = new ArrayList<>();

            for (Long friendId : friendIds) {
                memberDAO.findById(friendId).ifPresent(friendProfiles::add);
            }

            model.addAttribute("friends", friendProfiles);

            model.addAttribute("member", memberOpt.get());
            model.addAttribute("member", memberData);

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
                    String openLibraryId = (String) bookMap.get("openLibraryId");
                    String cleanId = openLibraryId != null ? openLibraryId.replace("/works/", "") : "";

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

                    books.add(new Book(title, author, cleanId, coverUrl));
                }

                booksByShelf.put(shelfId, books);
            }

            model.addAttribute("booksByShelf", booksByShelf);
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
            model.addAttribute("errorMessage", "A database error occurred: " + e.getMessage());
            model.addAttribute("showError", true);
        } catch (ResponseStatusException e) {
            model.addAttribute("errorMessage", e.getReason());
            model.addAttribute("showError", true);
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
                        .body(Map.of("errorMessage", "A bookshelf with this name already exists"));
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
                    .body(Map.of("errorMessage", e.getMessage()));
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "This bookshelf belongs to another user"));
            }

            String cleanId = workId.replace("/works/", "");
            bookshelfDAO.addBookToShelf(bookshelfId, cleanId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errorMessage", e.getMessage()));
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

        System.out.println("Bookshelf ID: " +bookshelfId);
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "Bookshelf belongs to another user"));
            }

            boolean removed = bookshelfDAO.removeBookFromShelf(bookshelfId, workId);
            if (removed) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMessage", "Couldn't find this book in the bookshelf"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", e.getMessage()));
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "Bookshelf belongs to another user"));
            }

            boolean deleted = bookshelfDAO.deleteBookshelf(bookshelfId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", e.getMessage()));
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfName(bookshelfId, name);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "name", name));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errorMessage", e.getMessage()));
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfDescription(bookshelfId, description);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "description", description));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", e.getMessage()));
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
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }

            // Jämför primitiva värden med ==
            Long ownerId = (Long) bookshelfOpt.get().get("memberId");
            if (!ownerId.equals(memberId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("errorMessage", "Bookshelf belongs to another user"));
            }

            boolean updated = bookshelfDAO.updateBookshelfVisibility(bookshelfId, isPublic);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "isPublic", isPublic));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("errorMessage", "Bookshelf not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", e.getMessage()));
        }
    }

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
                    .body(Map.of("errorMessage", e.getMessage()));
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

    @PostMapping("/update")
    public String updateProfile(@RequestParam String displayName,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) MultipartFile profileImage,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) throws IOException {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not currently logged in");
            redirectAttributes.addFlashAttribute("showError", true);
            return "redirect:/logIn";
        }

        if(displayName == null || displayName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Display name is required");
            redirectAttributes.addFlashAttribute("showError", true);
            return "redirect:/profile";
        }

        currentMember.setName(displayName.trim());
        currentMember.setBio(bio != null ? bio.trim() : "");

        if(profileImage != null && !profileImage.isEmpty()) {
            try{
                String imageUrl = saveProfileImage(profileImage, currentMember.getId());
                currentMember.setProfileImage(imageUrl);
                memberDAO.updateProfilePicture(currentMember.getId(), imageUrl);
            } catch (IOException e){
                redirectAttributes.addFlashAttribute("errorMessage", "Could not save profile image");
                redirectAttributes.addFlashAttribute("showError", true);
                return "redirect:/profile";
            }
        }

        System.out.println("PROFILE IMAGE PATH TO SAVE: " + currentMember.getProfileImage());
        memberDAO.updateProfileInfo(currentMember); // Skapa denna metod i DAO

        redirectAttributes.addFlashAttribute("updateSuccess", "Your profile has been updated.");
        return "redirect:/profile";
    }

    @PostMapping("/upload-picture")
    public String uploadProfileImage(@RequestParam("image") MultipartFile image,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not currently logged in");
            redirectAttributes.addFlashAttribute("showError", true);
            return "redirect:/logIn";
        }

        if(image.isEmpty()){
            redirectAttributes.addFlashAttribute("errorMessage", "Please select an image to upload");
            redirectAttributes.addFlashAttribute("showError", true);
            return "redirect:/profile";
        }

        try{
            String uploadDirectory = "uploads/";
            Files.createDirectories(Paths.get(uploadDirectory));

            String fileName = currentMember.getId() + "_" + image.getOriginalFilename();
            Path path = Paths.get(uploadDirectory + fileName);
            image.transferTo(path);

            memberDAO.updateProfilePicture(currentMember.getId(), "/" + uploadDirectory + fileName );

            redirectAttributes.addFlashAttribute("updateSuccess", "Your profile has been updated.");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
            redirectAttributes.addFlashAttribute("showError", true);
        }
        
        return "redirect:/profile";
    }

     private String saveProfileImage(MultipartFile image, Long memberId) throws IOException {
         Path uploadPath = Paths.get("profileImages");

         if(!Files.exists(uploadPath)) {
             Files.createDirectory(uploadPath);
         }

         String filename = memberId + "_" + image.getOriginalFilename();
         Path filePath = uploadPath.resolve(filename);

         Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

         return filename;
     }

    /**
     * Displays the profile of another member, if they are a confirmed friend.
     * Only public bookshelves are shown.
     *
     * @param id The member ID of the profile to view.
     * @param session HTTP session for identifying the current user.
     * @param model Spring MVC model for passing data to the view.
     * @return The "profile" view if allowed, or a redirect/error otherwise.
     */
    @GetMapping("/{id}")
    public String viewFriendProfile(@PathVariable Long id, HttpSession session, Model model) {
        Member currentUser = (Member) session.getAttribute("currentMember");

        if (currentUser == null) {
            return "redirect:/logIn";
        }

        if (currentUser.getId().equals(id)) {
            return "redirect:/profile"; // viewing own profile
        }

        try {
            boolean areFriends = friendshipDAO.areFriends(currentUser.getId(), id);
            if (!areFriends) {
                model.addAttribute("errorMessage", "You are not friends with this user.");
                model.addAttribute("showError", true);
                return "profile"; // Or redirect to /search or home if more appropriate
            }

            Optional<Map<String, Object>> friendOpt = memberDAO.findById(id);
            if (friendOpt.isEmpty()) {
                model.addAttribute("errorMessage", "User not found.");
                model.addAttribute("showError", true);
                return "profile";
            }

            Map<String, Object> friendData = friendOpt.get();
            int friendCount = memberDAO.countFriends(id);
            friendData.put("friendCount", friendCount);
            List<Long> friendIds = friendshipDAO.findFriendIds(id);
            List<Map<String, Object>> friendProfiles = new ArrayList<>();

            for (Long friendId : friendIds) {
                memberDAO.findById(friendId).ifPresent(friendProfiles::add);
            }

            model.addAttribute("friends", friendProfiles);

            model.addAttribute("member", friendData);

            // Only public bookshelves
            List<Map<String, Object>> bookshelves = bookshelfDAO.findPublicByMemberId(id);
            model.addAttribute("bookshelves", bookshelves);

            Map<Long, List<Book>> booksByShelf = new HashMap<>();
            for (Map<String, Object> shelf : bookshelves) {
                Long shelfId = (Long) shelf.get("id");
                List<Map<String, Object>> bookData = bookshelfDAO.findBooksByBookshelfId(shelfId);
                List<Book> books = new ArrayList<>();
                for (Map<String, Object> bookMap : bookData) {
                    books.add(new Book(
                            (String) bookMap.get("title"),
                            (String) bookMap.getOrDefault("author", "Unknown"),
                            ((String) bookMap.get("openLibraryId")).replace("/works/", ""),
                            (String) bookMap.get("coverUrl")
                    ));
                }
                booksByShelf.put(shelfId, books);
            }

            model.addAttribute("booksByShelf", booksByShelf);
            return "profile";

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
            model.addAttribute("showError", true);
            return "profile";
        }
    }

    /**
     * Returns the current user's bookshelves as a JSON response for AJAX requests.
     * This method is typically used to display bookshelves when adding a book from a book page.
     *
     * @param session the current HTTP session, used to identify the logged-in user
     * @return a ResponseEntity containing the list of bookshelves as JSON if the user is logged in,
     *         or an error message if the user is not authenticated or a database error occurs
     */
    @GetMapping("/bookshelves")
    @ResponseBody
    public ResponseEntity<?> getUserBookshelves(HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("errorMessage", "You must be logged in to view your bookshelves"));
        }

        Long memberId = currentMember.getId();

        try {
            List<Map<String, Object>> bookshelves = bookshelfDAO.findByMemberId(memberId);
            return ResponseEntity.ok(bookshelves);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", e.getMessage()));
        }
    }

}
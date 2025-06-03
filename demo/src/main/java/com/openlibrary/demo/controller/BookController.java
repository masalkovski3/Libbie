package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.DAO.ReviewDAO;
import com.openlibrary.demo.external.OpenLibraryBook;
import com.openlibrary.demo.model.Member;
import com.openlibrary.demo.model.Review;
import com.openlibrary.demo.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller class responsible for handling book detail views based on Open Library data.
 * This controller fetches data from Open Library's public APIs using a work ID, and optionally a cover ID.
 * It processes JSON responses to extract book metadata such as title, description, author, and cover image.
 * It also fetches and displays user reviews for the book.
 * The data is then added to the model and passed to the "book" view for rendering.
 */
@Tag(name = "Books", description = "Endpoints for retrieving book data and reviews")
@Controller
public class BookController {

    private final ReviewDAO reviewDAO;
    private final BookService bookService;

    @Autowired
    public BookController(ReviewDAO reviewDAO, BookService bookService) {
        this.bookService = bookService;
        this.reviewDAO = reviewDAO;
    }

    /**
     * Handles HTTP GET requests to the `/books/{workId}` endpoint.
     * Fetches book metadata (title, description, author, and cover) from Open Library's public API,
     * using the given work ID. If a cover ID is provided, it is used directly; otherwise,
     * the method attempts to extract a cover ID from the book's edition data.
     * Also retrieves and displays user reviews for the book.
     *
     * @param workId  The Open Library work ID (e.g., OL12345W), typically in the format `/works/{id}`
     * @param coverId (Optional) A specific cover ID to use for the book's cover image
     * @param model   The Spring MVC model used to pass attributes to the Thymeleaf view
     * @param session The HTTP session containing user information
     * @return The name of the view to render ("book") or "error" if the work was not found
     * @throws JsonProcessingException If parsing the JSON responses fails
     */
    @Operation(
            summary = "Get book details by work ID",
            description = "Fetches book metadata from Open Library and retrieves reviews from the local database",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book details and reviews retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Book not found in Open Library"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/books/{workId}")
    public String books(@PathVariable String workId,
                        @RequestParam(required = false) Integer coverId,
                        Model model,
                        HttpSession session) throws JsonProcessingException {

        OpenLibraryBook book = bookService.getBookByWorkId(workId);

        if(book == null){
            model.addAttribute("errorMessage", "Book not found in Open Library");
            model.addAttribute("showError", true);
            return "book";
        }

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        String url = "https://openlibrary.org/works/" + workId + ".json";
        String jsonResponse = restTemplate.getForObject(url, String.class);
        JsonNode root = mapper.readTree(jsonResponse);

        model.addAttribute("title", book.getTitle());
        model.addAttribute("author", book.getAuthorName() != null && !book.getAuthorName().isEmpty()
                ? String.join(", ", book.getAuthorName())
                : "Unknown author");
        model.addAttribute("authorKey", getAuthorKey(root));
        model.addAttribute("description",
                book.getDescription() != null && !book.getDescription().isEmpty()
                        ? book.getDescription()
                        : "No description available.");
        model.addAttribute("coverUrl", book.getCoverUrl());
        model.addAttribute("workId", workId);

        List<Review> reviews = Collections.emptyList();
        double averageScore = 0.0;
        Review userReview = null;

        try {
            reviews = reviewDAO.getReviewsByBookId(workId);
            averageScore = reviewDAO.getAverageReviewScore(workId);

            Member currentMember = (Member) session.getAttribute("currentMember");
            if (currentMember != null) {
                userReview = reviewDAO.getMemberReview(currentMember.getId(), workId);
                model.addAttribute("canReview", true);
            } else {
                model.addAttribute("canReview", false);
            }

        } catch (SQLException e) {
            System.err.println("Failed to load reviews: " + e.getMessage());
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("averageScore", averageScore);
        model.addAttribute("userReview", userReview);

        return "book";

        /*
        String cleanId = workId.replace("/works/", "");
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        String workUrl = getWorkUrl(cleanId);
        // === 1. Fetch basic book data from "works"
        JsonNode root;
        try {
            String response = restTemplate.getForObject(workUrl, String.class);
            root = mapper.readTree(response);
        } catch (HttpClientErrorException.NotFound e){
            model.addAttribute("errorMessage", "The book was not found");
            model.addAttribute("showError", true);
            return "book"; // Return the book page with error message instead of error page
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred when retrieving the book: " + e.getMessage());
            model.addAttribute("showError", true);
            return "book";
        }

        // === 2. Title
        String title = getTitle(root);

        // === 3. Description (can be text or object)
        String description = getDescription(root);

        // === 4. Author (fetch via separate /authors/{id} call)
        String author = "";
        String authorKey = getAuthorKey(root);
        if(!authorKey.isEmpty()) {
            author = getAuthor(root, restTemplate, mapper, authorKey);
        }

        // === 5. Cover image (fetched via editions if any exist)
        String coverUrl = getCoverUrl(coverId, cleanId, restTemplate, mapper);

        // === 6. Fetch reviews for the book
        List<Review> reviews = Collections.emptyList();
        double averageScore = 0.0;
        Review userReview = null;

        try {
            reviews = reviewDAO.getReviewsByBookId(workId);
            averageScore = reviewDAO.getAverageReviewScore(workId);

            // If user is logged in, fetch their review (if it exists)
            Member currentMember = (Member) session.getAttribute("currentMember");
            if (currentMember != null) {
                userReview = reviewDAO.getMemberReview(currentMember.getId(), workId);
                model.addAttribute("canReview", true);
            } else {
                model.addAttribute("canReview", false);
            }
        } catch (SQLException e) {
            // Log the error but continue loading the page
            System.err.println("Failed to load reviews: " + e.getMessage());
        }

        // === 7. Send data to view
        model.addAttribute("title", title);
        model.addAttribute("description", description.isEmpty() ? "No description available." : description);
        model.addAttribute("coverUrl", coverUrl);
        model.addAttribute("author", author);
        model.addAttribute("authorKey", authorKey);
        model.addAttribute("workId", workId);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageScore", averageScore);
        model.addAttribute("userReview", userReview);

        return "book";

         */
    }

    /**
     * Constructs the full API URL for the given work ID.
     *
     * @param cleanId The cleaned work ID without `/works/` prefix
     * @return A full URL string for accessing work data via Open Library API
     */
    @Operation(summary = "Get full Open Library API URL for a given work ID")
    public String getWorkUrl(String cleanId) {
        return "https://openlibrary.org/works/" + cleanId + ".json";
    }

    /**
     * Extracts the book title from the given JSON root node.
     *
     * @param root The root JSON node from the work API response
     * @return The book title, or "Unknown" if not found
     */
    @Operation(summary = "Extract book title from Open Library API response")
    public String getTitle(JsonNode root) {
        String title = root.path("title").asText();
        return title.isEmpty() ? "Unknown" : title;
    }

    /**
     * Extracts the book description from the given JSON root node.
     * Handles both textual and object-based description fields.
     *
     * @param root The root JSON node from the work API response
     * @return The description text, or an empty string if unavailable
     */
    @Operation(summary = "Extract book description from Open Library API response")
    public String getDescription(JsonNode root) {
        JsonNode descNode = root.path("description");
        if (descNode.isTextual()) {
            return descNode.asText();
        } else if (descNode.has("value")) {
            return descNode.path("value").asText();
        }
        return "No description available.";
    }

    /**
     * Fetches the author's display name using the given author key.
     *
     * @param root         The root JSON node from the work API response
     * @param restTemplate Spring's HTTP client used to perform the request
     * @param mapper       Jackson ObjectMapper for parsing JSON
     * @param authorKey    The Open Library author key (e.g., /authors/OL1234A)
     * @return The author's name, or "Unknown" if not found
     * @throws JsonProcessingException If parsing the JSON response fails
     */
    @Operation(summary = "Get author name using author key")
    public String getAuthor(JsonNode root, RestTemplate restTemplate, ObjectMapper mapper, String authorKey) throws JsonProcessingException {
        String author;
        String authorUrl = "https://openlibrary.org" + authorKey + ".json";
        String authorResponse = restTemplate.getForObject(authorUrl, String.class);
        JsonNode authorRoot = mapper.readTree(authorResponse);
        author = authorRoot.path("name").asText("Unknown");
        return author;
    }

    @Operation(summary = "Get list of all authors for a work")
    public List<String> getAuthorWithKey(JsonNode root, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
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

        return authors;
    }

    @Operation(summary = "Extract first available cover ID from work JSON")
    public Integer getCoverId(JsonNode root) {
        Integer coverId;
        JsonNode covers = root.path("covers");
        if (covers.isArray() && !covers.isEmpty()) {
            coverId = covers.get(0).asInt();
        } else {
            coverId = null;
        }
        return coverId;
    }

    /**
     * Extracts the author key (identifier) from the work JSON root.
     *
     * @param root The root JSON node from the work API response
     * @return The author's key string (e.g., /authors/OL1234A), or empty if not found
     */
    @Operation(summary = "Extract first author key from work JSON")
    public String getAuthorKey(JsonNode root) throws JsonProcessingException {
        String authorKey = "";
        JsonNode authorsNode = root.path("authors");
        if (authorsNode.isArray() && authorsNode.size() > 0) {
            authorKey = authorsNode.get(0).path("author").path("key").asText(); // e.g. /authors/OL1234A
        }
        return authorKey;
    }

    /**
     * Determines the appropriate cover URL for a book. If a cover ID is provided,
     * it is used directly; otherwise, the method attempts to extract one from
     * the book's edition data.
     *
     * @param coverId      The optional cover ID provided as a request parameter
     * @param cleanId      The cleaned work ID (without `/works/`)
     * @param restTemplate Spring's HTTP client used to perform requests
     * @param mapper       Jackson ObjectMapper for parsing JSON
     * @return A URL string pointing to a large cover image, or empty string if none found
     * @throws JsonProcessingException If parsing the edition response fails
     */
    @Operation(summary = "Determine best cover URL based on coverId or edition data")
    public String getCoverUrl(Integer coverId, String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
        String coverUrl;
        if (coverId != null) {
            coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        } else {
            try {
                coverUrl = bookService.getCoverUrl(null, cleanId, restTemplate, mapper);
            } catch (Exception e) {
                e.printStackTrace();
                coverUrl = "/images/blue-logo.jpeg";
            }
        }
        return coverUrl;
        /*
        String coverUrl = "";
        if (coverId !=null){
            coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        }

        String editionsUrl = "https://openlibrary.org/works/" + cleanId + "/editions.json?limit=50";
        String editionResponse = restTemplate.getForObject(editionsUrl, String.class);
        JsonNode editionRoot = mapper.readTree(editionResponse);
        JsonNode editionDocs = editionRoot.path("entries");

        if (editionDocs.isArray()) {
            for (JsonNode edition : editionDocs) {
                JsonNode covers = edition.path("covers");
                if (covers.isArray() && covers.size() > 0) {
                    coverId = covers.get(0).asInt();
                    coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                    System.out.println("Cover found: " + coverUrl);
                    break;
                }
            }
        }
        return coverUrl;

         */
    }

    /**
     * Extracts the earliest publication year from the book's editions.
     *
     * @param cleanId      The cleaned work ID (without `/works/`)
     * @param restTemplate Spring's HTTP client used to perform requests
     * @param mapper       Jackson ObjectMapper for parsing JSON
     * @return The earliest publication year found, or -1 if not available
     * @throws JsonProcessingException If parsing the edition response fails
     */
    @Operation(summary = "Get earliest known publication year from edition data")
    public int getPublishYear(String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
        String editionsUrl = "https://openlibrary.org/works/" + cleanId + "/editions.json?limit=50";
        String editionResponse = restTemplate.getForObject(editionsUrl, String.class);
        JsonNode editionRoot = mapper.readTree(editionResponse);
        JsonNode editionDocs = editionRoot.path("entries");

        int earliestYear = Integer.MAX_VALUE;

        if (editionDocs.isArray()) {
            for (JsonNode edition : editionDocs) {
                JsonNode publishDateNode = edition.path("publish_date");

                if (publishDateNode.isTextual()) {
                    String publishDate = publishDateNode.asText();
                    // Try to extract year (e.g., from "May 1984" or "1984")
                    String yearStr = publishDate.replaceAll(".*?(\\d{4}).*", "$1");
                    try {
                        int year = Integer.parseInt(yearStr);
                        if (year < earliestYear) {
                            earliestYear = year;
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid dates
                    }
                }
            }
        }

        return earliestYear == Integer.MAX_VALUE ? LocalDate.now().getYear() : earliestYear;
    }

}
package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Controller class responsible for handling book detail views based on Open Library data.
 * <p>
 * This controller fetches data from Open Library's public APIs using a work ID, and optionally a cover ID.
 * It processes JSON responses to extract book metadata such as title, description, author, and cover image.
 * The data is then added to the model and passed to the "book" view for rendering.
 * </p>
 */
@Controller
public class BookController {

    //private BookApiController bookApiController; eventuell lösning

    /**
     * Handles HTTP GET requests to the `/books/{workId}` endpoint.
     * <p>
     * Fetches book metadata (title, description, author, and cover) from Open Library's public API,
     * using the given work ID. If a cover ID is provided, it is used directly; otherwise,
     * the method attempts to extract a cover ID from the book's edition data.
     * </p>
     *
     * @param workId  The Open Library work ID (e.g., OL12345W), typically in the format `/works/{id}`
     * @param coverId (Optional) A specific cover ID to use for the book's cover image
     * @param model   The Spring MVC model used to pass attributes to the Thymeleaf view
     * @return The name of the view to render ("book") or "error" if the work was not found
     * @throws JsonProcessingException If parsing the JSON responses fails
     */
    @GetMapping("/books/{workId}")
    public String books(@PathVariable String workId,
                        @RequestParam(required = false) Integer coverId,
                        Model model) throws JsonProcessingException {

        String cleanId = workId.replace("/works/", "");
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        String workUrl = getWorkUrl(cleanId);
        // === 1. Hämta grundläggande bokdata från "works"
        JsonNode root;
        try {
            String response = restTemplate.getForObject(workUrl, String.class);
            root = mapper.readTree(response);
        } catch (HttpClientErrorException.NotFound e){
            model.addAttribute("errorMessage", "The book was not found");
            model.addAttribute("showError", true);
            return "book"; // Returera boksidan med felmeddelande istället för error-sidan
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred when retrieving the book: " + e.getMessage());
            model.addAttribute("showError", true);
            return "book";
        }

        // === 2. Titel
        String title = getTitle(root);

        // === 3. Beskrivning (kan vara text eller objekt)
        String description = getDescription(root);

        // === 4. Författare (hämta via separat /authors/{id}-anrop)
        String author = "";
        String authorKey = getAuthorKey(root);
        if(!authorKey.isEmpty()) {
            author = getAuthor(root, restTemplate, mapper, authorKey);
        }

        // === 5. Omslagsbild (hämtas via editioner – om någon finns)
        String coverUrl = getCoverUrl(coverId,cleanId, restTemplate, mapper);

        // === 6. Skicka data till vy
        model.addAttribute("title", title);
        model.addAttribute("description", description.isEmpty() ? "No description available." : description);
        model.addAttribute("coverUrl", coverUrl);
        model.addAttribute("author", author);
        model.addAttribute("authorKey", authorKey);

        return "book";
    }

    /**
     * Constructs the full API URL for the given work ID.
     *
     * @param cleanId The cleaned work ID without `/works/` prefix
     * @return A full URL string for accessing work data via Open Library API
     */
    private String getWorkUrl(String cleanId) {
        return "https://openlibrary.org/works/" + cleanId + ".json";
    }

    /**
     * Extracts the book title from the given JSON root node.
     *
     * @param root The root JSON node from the work API response
     * @return The book title, or "Unknown" if not found
     */
    private String getTitle(JsonNode root) {
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
    private String getDescription(JsonNode root) {
        String description = "";
        JsonNode descNode = root.path("description");
        if (descNode.isTextual()) {
            description = descNode.asText();
        } else if (descNode.has("value")) {
            description = descNode.path("value").asText();
        }
        return description;
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
    private String getAuthor(JsonNode root, RestTemplate restTemplate, ObjectMapper mapper, String authorKey) throws JsonProcessingException {
        String author = "Unknown";
        String authorUrl = "https://openlibrary.org" + authorKey + ".json";
        String authorResponse = restTemplate.getForObject(authorUrl, String.class);
        JsonNode authorRoot = mapper.readTree(authorResponse);
        author = authorRoot.path("name").asText("Unknown");
        return author;
    }

    /**
     * Extracts the author key (identifier) from the work JSON root.
     *
     * @param root The root JSON node from the work API response
     * @return The author's key string (e.g., /authors/OL1234A), or empty if not found
     */
    private String getAuthorKey(JsonNode root) throws JsonProcessingException {
        String authorKey = "";
        JsonNode authorsNode = root.path("authors");
        if (authorsNode.isArray() && authorsNode.size() > 0) {
            authorKey = authorsNode.get(0).path("author").path("key").asText(); // t.ex. /authors/OL1234A
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
    private String getCoverUrl(Integer coverId, String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
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
    }
}

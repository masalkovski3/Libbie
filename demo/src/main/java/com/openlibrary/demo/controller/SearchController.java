package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.model.Book;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller responsible for handling book search functionality via the OpenLibrary API.
 * Provides a search page and displays results based on user input, sorting, and limit.
 */
@Controller
@Tag(name = "Book Search", description = "Book search functionality")
public class SearchController {

    /**
     * Handles GET requests to the /search endpoint. If no query is provided, attempts to reuse
     * the previous search stored in the session. Otherwise, performs a new book search using
     * the OpenLibrary API and populates the model with the results.
     *
     * @param query   The search query string entered by the user (optional)
     * @param limit   The maximum number of results to return (default: 30)
     * @param sort    The sorting method for results (e.g., "relevance", "new", "editions")
     * @param model   Spring model to pass attributes to the view
     * @param session HTTP session to store and retrieve the user's last search
     * @return The name of the view to render (search.html)
     */
    @Operation(summary = "Search books page",
            description = "Display search page with results from OpenLibrary API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search page displayed with results"),
            @ApiResponse(responseCode = "200", description = "Search page displayed with error message")
    })
    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, defaultValue = "30") int limit,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            Model model,
            HttpSession session) {

        RestTemplate restTemplate = new RestTemplate();
        String url;

        // Om genren finns, hämta topplistan via OpenLibrary Subjects API
        if (genre != null && !genre.trim().isEmpty()) {
            url = "https://openlibrary.org/subjects/" + genre.toLowerCase().replace(" ", "_") + ".json?limit=" + limit;
        }
        // Annars, använd vanlig sökning via query
        else if (query != null && !query.trim().isEmpty()) {
            url = "https://openlibrary.org/search.json?q=" + query + "&limit=" + limit;
        }
        // Om varken query eller genre anges, returnera tomma resultat
        else {
            model.addAttribute("books", new ArrayList<>());
            return "search";
        }

        try {
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode docs = root.has("works") ? root.path("works") : root.path("docs");

            List<Book> books = createBookObject(limit, docs, model, restTemplate, mapper);
            model.addAttribute("books", books);
            model.addAttribute("query", query);
            model.addAttribute("genre", genre);
            model.addAttribute("selectedSort", sort);

            return "search";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred during search.");
            model.addAttribute("books", new ArrayList<>());
            return "search";
        }
}

    /**
     * Converts JSON response data from OpenLibrary into a list of Book objects.
     * Also checks if no results were found and updates the model with an error message.
     *
     * @param limit        Maximum number of results to include
     * @param docs         JSON array of book documents from OpenLibrary's response
     * @param model        Spring model to populate with error messages if necessary
     * @param restTemplate RestTemplate for performing additional API calls
     * @param mapper       ObjectMapper to parse JSON responses
     * @return A list of Book objects created from the API response
     * @throws JsonProcessingException If an error occurs while parsing JSON
     */
    @Operation(summary = "Create book objects from API response",
            description = "Convert JSON response from OpenLibrary to Book objects")
    private List <Book> createBookObject(int limit, JsonNode docs, Model model, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, docs.size()); i++) {
            JsonNode doc = docs.get(i);
            String title = doc.path("title").asText();
            JsonNode authors = doc.path("author_name");
            String author = (authors.isArray() && authors.size() > 0) ? authors.get(0).asText() : "Unknown";
            String workID = doc.path("key").asText();
            String cleanId = workID.replace("/works/", "");

            // Hämta omslag
            int coverId;
            JsonNode covers = doc.path("cover_i");

            if (!covers.isMissingNode() && !covers.isNull()) {
                coverId = covers.asInt();
            } else {
                coverId = 0;
            }

            String coverUrl = getCoverUrl(doc.path("cover_i").asInt(), cleanId, restTemplate, mapper);
            books.add(new Book(title, author, workID, coverUrl, coverId));
        }

        // Visa felmeddelande om inga böcker hittades
        if (books.isEmpty()) {
            model.addAttribute("errorMessage", "No books found matching your search criteria. Please try a different search term.");
            model.addAttribute("showError", true);
        }
        return books;
    }

    /**
     * Builds a URL for querying the OpenLibrary API based on the user's search input.
     *
     * @param query The search query
     * @param sort  Sorting option (e.g., "new", "editions"); only included if valid
     * @param limit The maximum number of results to return
     * @return A StringBuilder representing the full API request URL
     */
    @Operation(summary = "Create search URL",
            description = "Build OpenLibrary API URL with search parameters")
    private StringBuilder createUrl(String query, String sort, int limit) {
        StringBuilder urlBuilder = new StringBuilder("https://openlibrary.org/search.json?q=");
        urlBuilder.append(query.trim().replace(" ", "+"));

        if (sort != null && !sort.isEmpty() && ("new".equals(sort) || "editions".equals(sort))) {
            urlBuilder.append("&sort=").append(sort);
        }

        urlBuilder.append("&limit=").append(limit);

        return urlBuilder;
    }

    /**
     * Determines the most appropriate cover image URL for a book.
     * If a cover ID is provided, it uses that; otherwise, it searches through the
     * book's editions to find a cover image.
     *
     * @param coverId      The primary cover ID (may be null or zero)
     * @param cleanId      The book's work ID (without "/works/")
     * @param restTemplate RestTemplate for fetching edition data from the API
     * @param mapper       ObjectMapper for parsing JSON responses
     * @return A URL string pointing to the best available cover image
     * @throws JsonProcessingException If JSON processing fails when reading editions
     */
    @Operation(summary = "Get cover URL for book",
            description = "Determine best cover image URL for a book")
    private String getCoverUrl(Integer coverId, String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
        String coverUrl = "";
        if (coverId != null){
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
                    //System.out.println("Cover found: " + coverUrl);
                    break;
                }
            }
        }
        return coverUrl;
    }
}

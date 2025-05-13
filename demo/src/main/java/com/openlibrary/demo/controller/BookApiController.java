package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.model.Book;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that handles book search requests using the OpenLibrary API.
 * This controller exposes a REST endpoint that takes a search query,
 * fetches matching books from the OpenLibrary public API, parses the result,
 * and returns a list of {@link Book} objects.
 */
@RestController
@RequestMapping("/api/books")
public class BookApiController {

    /**
     * Searches for books based on a query string.
     * The method sends a GET request to the OpenLibrary API with the specified search query.
     * It parses the JSON response and maps up to 10 results to {@link Book} objects.
     *  * Each book includes a title, author name, OpenLibrary work Id, and optionally a cover image URL and Id.
     *
     * @param query the search string to look for books; must not be null or blank
     * @return a list of up to 10 {@link Book} objects matching the query;
     *         an empty list if the query is blank or yields no results
     * @throws JsonProcessingException if there is an error while parsing the OpenLibrary API response
     */
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String query,
                                  @RequestParam(required = false) String genre) throws JsonProcessingException {
        if(query == null || query.trim().isEmpty()){
            return new ArrayList<>();
        }

        RestTemplate restTemplate = new RestTemplate();

        if (genre != null && !genre.trim().isEmpty()){
            query+= " " + genre;
        }

        String url = "https://openlibrary.org/search.json?q=" + query;
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode docs = root.path("docs");

        List<Book> books = new ArrayList<>();
        for (int i = 0; i < Math.min(10, docs.size()); i++) {
            JsonNode doc = docs.get(i);
            String title = getTitle(doc);
            String author = getAuthor(doc);
            String workId = getWorkId(doc);

            // Försök hämta ett omslag
            int coverId = getCoverId(doc);
            String coverUrl = getCoverUrl(doc, coverId);
            books.add(new Book(title, author, workId, coverUrl, coverId));
        }
        return books;
    }

    /**
     * Gets the Cover URL from the API
     * @param doc
     * @param coverId, the coverId for the actual book
     * @return coverUrl
     */
    public String getCoverUrl(JsonNode doc, int coverId){
        String coverUrl = "";
        JsonNode covers = doc.path("cover_i");

        if (!covers.isMissingNode() && !covers.isNull()) {
            coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
        }

        // Om ingen coverUrl hittades ska vår logga dyka upp
        if (coverUrl.isEmpty()) {
            coverUrl = "/images/blue-logo.jpeg"; 
        }

        return coverUrl;
    }

    /**
     * Extracts the cover Id from the given book JSON node.
     *
     * @param doc the JSON node representing a single book from OpenLibrary's API response
     * @return the cover Id if present; otherwise, 0
     */
    public int getCoverId(JsonNode doc){
        int coverId = 0;
        JsonNode covers = doc.path("cover_i");
        if (!covers.isMissingNode() && !covers.isNull()){
            coverId = covers.asInt();
            return coverId;
        }
        return coverId;

    }

    /**
     * Extracts the book title from the given JSON node.
     *
     * @param doc the JSON node representing a single book from OpenLibrary's API response
     * @return the title of the book as a string
     */
    public String getTitle(JsonNode doc){
        String title = doc.path("title").asText();

        return title;
    }

    /**
     * Extracts the author's name from the given JSON node.
     * <p>
     * If multiple authors are listed, only the first one is returned.
     * If no author is available, "Unknown" is returned.
     * </p>
     *
     * @param doc the JSON node representing a single book from OpenLibrary's API response
     * @return the author's name, or "Unknown" if not available
     */
    public String getAuthor(JsonNode doc){
        JsonNode authors = doc.path("author_name");
        String author = (authors.isArray() && authors.size() > 0) ? authors.get(0).asText() : "Unknown";

        return author;

    }

    /**
     * Extracts the work Id from the given JSON node.
     * <p>
     * The work Id is a unique identifier for the book in the OpenLibrary system.
     * </p>
     *
     * @param doc the JSON node representing a single book from OpenLibrary's API response
     * @return the work Id as a string
     */
    public String getWorkId(JsonNode doc){
        String workId = doc.path("key").asText();

        return workId;
    }
}
package com.openlibrary.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.dto.SearchResult;
import com.openlibrary.demo.external.*;
import com.openlibrary.demo.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for querying the Open Library Search API
 * and mapping the result to the application's Book model.
 */
@Service
public class BookService {

    private static final String BASE_URL = "https://openlibrary.org/search.json";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Searches Open Library with pagination.
     *
     * @param query  the search string (title, author, etc.)
     * @param limit  max number of books to return
     * @param offset number of books to skip (used for pagination)
     * @return a SearchResult containing list of books and total result count
     */
    public SearchResult searchBooks(String query, int limit, int offset) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = BASE_URL + "?q=" + encodedQuery + "&limit=" + limit + "&offset=" + offset;

        OpenLibraryResponse response = restTemplate.getForObject(url, OpenLibraryResponse.class);

        if (response == null || response.getDocs() == null) {
            return new SearchResult(List.of(), 0);
        }

        List<Book> books = response.getDocs().stream()
                .map(this::mapToBook)
                .collect(Collectors.toList());

        return new SearchResult(books, response.getNumFound());
    }

    /**
     * Maps Open Library's book format to the internal Book model.
     *
     * @param ol the OpenLibraryBook object from API response
     * @return a Book object for internal use
     */
    private Book mapToBook(OpenLibraryBook ol) {
        String title = ol.getTitle() != null ? ol.getTitle() : "Untitled";
        String author = (ol.getAuthorName() != null && !ol.getAuthorName().isEmpty())
                ? ol.getAuthorName().get(0)
                : "Unknown";
        String workId = ol.getKey() != null ? ol.getKey() : "/works/undefined";
        String coverUrl = (ol.getCoverId() != null)
                ? "https://covers.openlibrary.org/b/id/" + ol.getCoverId() + "-M.jpg"
                : "/images/blue-logo.jpeg";

        return new Book(title, author, workId, coverUrl);
    }

    public SearchResult searchByQuery(String query, int limit, int offset) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/search.json?q=" + encodedQuery + "&limit=" + limit + "&offset=" + offset;

        return fetchSearchResults(url, true);
    }

    public SearchResult searchByGenre(String genre, int limit, int offset) {
        String encodedGenre = URLEncoder.encode(genre.toLowerCase().replace(" ", "_"), StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/subjects/" + encodedGenre + ".json?limit=" + limit + "&offset=" + offset;

        return fetchSearchResults(url, false);
    }

    private SearchResult fetchSearchResults(String url, boolean isQueryBased) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);

            JsonNode items = isQueryBased ? root.path("docs") : root.path("works");
            int totalCount = isQueryBased ? root.path("numFound").asInt() : root.path("work_count").asInt();

            List<Book> books = new ArrayList<>();
            for (JsonNode node : items) {
                String title = node.path("title").asText("Untitled");

                // Författare
                String author = "Unknown";
                JsonNode authorNode = isQueryBased ? node.path("author_name") : node.path("authors");
                if (authorNode.isArray() && authorNode.size() > 0) {
                    author = isQueryBased
                            ? authorNode.get(0).asText()
                            : authorNode.get(0).path("name").asText("Unknown");
                }

                String workId = node.path("key").asText("/works/undefined");
                Integer coverId = null;

                if (node.has("cover_id") && node.get("cover_id").isInt()) {
                    coverId = node.get("cover_id").asInt();
                } else if (node.has("cover_i") && node.get("cover_i").isInt()) {
                    coverId = node.get("cover_i").asInt();
                }

                String coverUrl = (coverId != null)
                        ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg"
                        : "/images/blue-logo.jpeg";

                books.add(new Book(title, author, workId, coverUrl));
            }

            return new SearchResult(books, totalCount);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResult(List.of(), 0);
        }
    }

}


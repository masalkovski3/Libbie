package com.openlibrary.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.dto.SearchResult;
import com.openlibrary.demo.external.*;
import com.openlibrary.demo.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service responsible for querying the Open Library Search API
 * and mapping the result to the application's Book model.
 */
@Service
public class BookService {

    private static final String BASE_URL = "https://openlibrary.org/search.json";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

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

        return fetchSearchResults(url, true, query, limit);
    }

    public SearchResult searchByGenre(String genre, int limit, int offset) {
        String encodedGenre = URLEncoder.encode(genre.toLowerCase().replace(" ", "_"), StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/subjects/" + encodedGenre + ".json?limit=" + limit + "&offset=" + offset;

        return fetchSearchResults(url, false, genre, limit);
    }

    private SearchResult fetchSearchResults(String url, boolean isQueryBased, String rawQuery, int limit) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);

            JsonNode items = isQueryBased ? root.path("docs") : root.path("works");
            String lowerCaseQuery = rawQuery.toLowerCase();

            // Filtrera på titel/författare och begränsa till `limit`
            List<JsonNode> filteredItems = StreamSupport.stream(items.spliterator(), false)
                    .filter(node -> {
                        String title = node.path("title").asText("").toLowerCase();
                        String author = isQueryBased
                                ? node.path("author_name").isArray() && node.path("author_name").size() > 0
                                ? node.path("author_name").get(0).asText("").toLowerCase()
                                : ""
                                : node.path("authors").isArray() && node.path("authors").size() > 0
                                ? node.path("authors").get(0).path("name").asText("").toLowerCase()
                                : "";

                        return title.contains(lowerCaseQuery) || author.contains(lowerCaseQuery);
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            // Parallell bearbetning av filtrerade böcker
            List<CompletableFuture<Book>> futures = filteredItems.stream()
                    .map(node -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String title = node.path("title").asText("Untitled");

                            String author = "Unknown";
                            JsonNode authorNode = isQueryBased ? node.path("author_name") : node.path("authors");
                            if (authorNode.isArray() && authorNode.size() > 0) {
                                author = isQueryBased
                                        ? authorNode.get(0).asText()
                                        : authorNode.get(0).path("name").asText("Unknown");
                            }

                            String workId = node.path("key").asText("/works/undefined");
                            String cleanId = workId.replace("/works/", "");

                            // Försök hämta coverId direkt från sökresultatet
                            Integer coverId = null;
                            if (node.has("cover_id") && node.get("cover_id").isInt()) {
                                coverId = node.get("cover_id").asInt();
                            } else if (node.has("cover_i") && node.get("cover_i").isInt()) {
                                coverId = node.get("cover_i").asInt();
                            }

                            // Försök använda coverId direkt, annars hämta från editions.json
                            String coverUrl;
                            if (coverId != null) {
                                coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                            } else {
                                try {
                                    coverUrl = getCoverUrl(null, cleanId, restTemplate, mapper);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    coverUrl = "/images/blue-logo.jpeg";
                                }
                            }

                            return new Book(title, author, workId, coverUrl);

                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Vänta in alla böcker och samla upp
            List<Book> books = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Du kan välja att returnera originalantal (för paginering) eller bara books.size()
            int totalCount = isQueryBased ? root.path("numFound").asInt() : root.path("work_count").asInt();

            return new SearchResult(books, totalCount);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResult(List.of(), 0);
        }
    }

    private String getCoverUrl(Integer coverId, String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
        String coverUrl = "";
        if (coverId != null) {
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


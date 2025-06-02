package com.openlibrary.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.dto.SearchResult;
import com.openlibrary.demo.external.*;
import com.openlibrary.demo.model.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service responsible for querying the Open Library Search API
 * and mapping the result to the application's Book model.
 */
@Service
public class BookService {

    private static final String BASE_URL = "https://openlibrary.org/search.json";

    private ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Searches Open Library with pagination.
     *
     * @param query  the search string (title, author, etc.)
     * @return a SearchResult containing list of books and total result count
     */
    public List<OpenLibraryBook> searchBooks(String query) {
        String url = "https://openlibrary.org/search.json?q=" + UriUtils.encodeQuery(query, StandardCharsets.UTF_8);
        String response = restTemplate.getForObject(url, String.class);

        try {
            OpenLibraryResponse olResponse = objectMapper.readValue(response, OpenLibraryResponse.class);
            return olResponse.getDocs();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
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
        int fetchLimit = 500;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/search.json?q=" + encodedQuery + "&limit=" + fetchLimit;

        return fetchSearchResults(url, true, query, limit, offset);
    }

    public SearchResult searchByGenre(String genre, int limit, int offset) {
        int fetchLimit = 500;
        String encodedGenre = URLEncoder.encode(genre.toLowerCase().replace(" ", "_"), StandardCharsets.UTF_8);
        String url = "https://openlibrary.org/subjects/" + encodedGenre + ".json?limit=" + fetchLimit;

        return fetchSearchResults(url, false, genre, limit, offset);
    }

    private SearchResult fetchSearchResults(String url, boolean isQueryBased, String rawQuery, int limit, int offset) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);

            JsonNode items = isQueryBased ? root.path("docs") : root.path("works");
            String lowerCaseQuery = rawQuery.toLowerCase();

            List<JsonNode> filteredItems = StreamSupport.stream(items.spliterator(), false)
                    .filter(node -> {
                        String title = node.path("title").asText("").toLowerCase();

                        String author = "";
                        if (isQueryBased && node.has("author_name") && node.path("author_name").isArray() && node.path("author_name").size() > 0) {
                            author = node.path("author_name").get(0).asText("").toLowerCase();
                        } else if (!isQueryBased && node.has("authors") && node.path("authors").isArray() && node.path("authors").size() > 0) {
                            author = node.path("authors").get(0).path("name").asText("").toLowerCase();
                        }

                        return title.contains(lowerCaseQuery) || author.contains(lowerCaseQuery);
                    })
                    .collect(Collectors.toList());

            int totalCount = filteredItems.size();

            List<JsonNode> pagedItems = filteredItems.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            List<CompletableFuture<Book>> futures = pagedItems.stream()
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

                            Integer coverId = null;
                            if (node.has("cover_id") && node.get("cover_id").isInt()) {
                                coverId = node.get("cover_id").asInt();
                            } else if (node.has("cover_i") && node.get("cover_i").isInt()) {
                                coverId = node.get("cover_i").asInt();
                            }

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

            List<Book> books = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SearchResult(books, totalCount);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResult(List.of(), 0);
        }
    }

    public String getCoverUrl(Integer coverId, String cleanId, RestTemplate restTemplate, ObjectMapper mapper) throws JsonProcessingException {
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

    public OpenLibraryBook getBookByWorkId(String workId) {
        List<OpenLibraryBook> books = searchBooks(workId);

        for (OpenLibraryBook book : books) {
            if (book.getKey() != null && book.getKey().contains(workId)) {
                try {
                    String url = "https://openlibrary.org/works/" + workId + ".json";
                    String json = restTemplate.getForObject(url, String.class);
                    JsonNode root = objectMapper.readTree(json);
                    JsonNode descNode = root.path("description");

                    if (descNode.isTextual()) {
                        book.setDescription(cleanDescription(descNode.asText()));
                    } else if (descNode.has("value")) {
                        book.setDescription(cleanDescription(descNode.path("value").asText()));
                    } else {
                        book.setDescription("No description available.");
                    }

                } catch (Exception e) {
                    book.setDescription("No description available.");
                    e.printStackTrace();
                }

                return book;
            }
        }

        return null;
    }

    public static String cleanDescription(String raw) {
        if (raw == null) return "";

        // Lista över meta-start-ord där vi vill klippa bort resten
        String[] markers = {
                "\\(source\\)", "See also:", "Preceded by:", "Followed by:",
                "Contains:", "\\[\\d+\\]:", "Source"
        };

        // Skapa ett regex som matchar första förekomst av något av dessa
        String patternString = String.join("|", markers);
        Pattern pattern = Pattern.compile("(?i)(" + patternString + ")");
        Matcher matcher = pattern.matcher(raw);

        String trimmed = raw;
        if (matcher.find()) {
            trimmed = raw.substring(0, matcher.start());
        }

        // Rensa kvarvarande markdown
        return trimmed
                .replaceAll("\\[(.*?)\\]\\[\\d+\\]", "$1")            // [text][1] → text
                .replaceAll("\\[(.*?)\\]\\((https?://.*?)\\)", "$1")  // [text](url) → text
                .replaceAll("-{3,}", "")                              // ---- → bort
                .replaceAll("\\s{2,}", " ")                           // dubbla mellanrum → ett
                .trim()
                .replaceAll("\\([\\[]?$", "")     // rensa ( eller ([ i slutet av strängen
                .replaceAll("\\[$", "");          // ensamt [ på slutet

    }


}


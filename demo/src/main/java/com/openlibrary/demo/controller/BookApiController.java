package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.model.Book;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookApiController {

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String query) throws JsonProcessingException {
        if(query == null || query.trim().isEmpty()){
            return new ArrayList<>();
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://openlibrary.org/search.json?q=" + query;
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode docs = root.path("docs");

        List<Book> books = new ArrayList<>();
        for (int i = 0; i < Math.min(10, docs.size()); i++) {
            JsonNode doc = docs.get(i);
            String title = doc.path("title").asText();
            JsonNode authors = doc.path("author_name");
            String author = (authors.isArray() && authors.size() > 0) ? authors.get(0).asText() : "Unknown";
            String workID = doc.path("key").asText();

            // Försök hämta ett omslag
            String coverUrl = "";
            JsonNode covers = doc.path("cover_i");
            int coverId = 0;
            if (!covers.isMissingNode() && !covers.isNull()) {
                coverId = covers.asInt();
                coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
            }

            books.add(new Book(title, author, workID, coverUrl, coverId));
        }

        return books;
    }
}
package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.model.Book;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SearchController {
    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1900") Integer year,
            @RequestParam(required = false) String language,
            Model model) throws JsonProcessingException {

        // Om sökning är tom, visa tom sida
        if(query == null || query.trim().isEmpty()){
            model.addAttribute("books", new ArrayList<>());
            model.addAttribute("query", "");
            return "search";
        }

        RestTemplate restTemplate = new RestTemplate();

        // Bygg URL med filter
        StringBuilder urlBuilder = new StringBuilder("https://openlibrary.org/search.json?q=");
        urlBuilder.append(query);

        // Lägg till årtalsfilter om det är över 1900
        if (year > 1900) {
            urlBuilder.append("&publish_year=>");
            urlBuilder.append(year);
        }

        // Lägg till språkfilter om specificerat
        if (language != null && !language.isEmpty()) {
            urlBuilder.append("&language=");
            urlBuilder.append(language);
        }

        String url = urlBuilder.toString();
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
            if (!covers.isMissingNode() && !covers.isNull()) {
                int coverId = covers.asInt();
                coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
            }

            books.add(new Book(title, author, workID, coverUrl));
        }

        model.addAttribute("books", books);
        model.addAttribute("query", query);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedLanguage", language);

        return "search";
    }
}

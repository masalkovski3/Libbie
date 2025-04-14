package com.openlibrary.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.openlibrary.demo.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class BookController {

    @GetMapping("/")
    public String home(){
        return "home";
    }

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

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/profile")
    public String profile() { return "profile"; }

    @GetMapping("/books/{workId}")
    public String books(@PathVariable String workId, Model model) throws JsonProcessingException {

        String cleanId = workId.replace("/works/", "");
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // === 1. Hämta grundläggande bokdata från "works"
        String workUrl = "https://openlibrary.org/works/" + cleanId + ".json";
        String response = restTemplate.getForObject(workUrl, String.class);
        JsonNode root = mapper.readTree(response);

        // === 2. Titel
        String title = root.path("title").asText();

        // === 3. Beskrivning (kan vara text eller objekt)
        String description = "";
        JsonNode descNode = root.path("description");
        if (descNode.isTextual()) {
            description = descNode.asText();
        } else if (descNode.has("value")) {
            description = descNode.path("value").asText();
        }

        // === 4. Författare (hämta via separat /authors/{id}-anrop)
        String author = "Unknown";
        String authorKey = "";
        JsonNode authorsNode = root.path("authors");

        if (authorsNode.isArray() && authorsNode.size() > 0) {
            authorKey = authorsNode.get(0).path("author").path("key").asText(); // t.ex. /authors/OL1234A
            if (!authorKey.isEmpty()) {
                String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                String authorResponse = restTemplate.getForObject(authorUrl, String.class);
                JsonNode authorRoot = mapper.readTree(authorResponse);
                author = authorRoot.path("name").asText("Unknown");
            }
        }

        // === 5. Omslagsbild (hämtas via editioner – om någon finns)
        String coverUrl = "";
        String editionsUrl = "https://openlibrary.org/works/" + cleanId + "/editions.json?limit=50";
        String editionResponse = restTemplate.getForObject(editionsUrl, String.class);
        JsonNode editionRoot = mapper.readTree(editionResponse);
        JsonNode editionDocs = editionRoot.path("entries");

        if (editionDocs.isArray()) {
            for (JsonNode edition : editionDocs) {
                JsonNode covers = edition.path("covers");
                if (covers.isArray() && covers.size() > 0) {
                    int coverId = covers.get(0).asInt();
                    coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                    System.out.println("✅ Cover found: " + coverUrl);
                    break;
                }
            }
        }

        // === 6. Skicka data till vy
        model.addAttribute("title", title);
        model.addAttribute("description", description.isEmpty() ? "No description available." : description);
        model.addAttribute("coverUrl", coverUrl);
        model.addAttribute("author", author);
        model.addAttribute("authorKey", authorKey);

        return "book";
    }

    @GetMapping("/logIn-signUp")
    public String logIn_signUp(){
        return "logIn-signUp";
    }
}

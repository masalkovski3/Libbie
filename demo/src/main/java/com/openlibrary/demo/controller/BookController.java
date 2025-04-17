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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class BookController {

    @GetMapping("/books/{workId}")
    public String books(@PathVariable String workId, Model model) throws JsonProcessingException {

        String cleanId = workId.replace("/works/", "");
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        // === 1. Hämta grundläggande bokdata från "works"
        String workUrl = "https://openlibrary.org/works/" + cleanId + ".json";
        //String response = restTemplate.getForObject(workUrl, String.class);
        //JsonNode root = mapper.readTree(response);
        JsonNode root;
        try {
            String response = restTemplate.getForObject(workUrl, String.class);
            root = mapper.readTree(response);
        }catch (HttpClientErrorException.NotFound e){
            model.addAttribute("error", "The book was not found");
            return "error";
        }

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

}

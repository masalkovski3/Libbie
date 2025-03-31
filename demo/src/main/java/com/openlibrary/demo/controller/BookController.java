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
    public String search(@RequestParam(required = false) String query, Model model) throws JsonProcessingException {
        if(query == null || query.trim().isEmpty()){
            model.addAttribute("books", new ArrayList<>());
            model.addAttribute("query", "");
            return "search";
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
            books.add(new Book(title, author, workID));
        }

        model.addAttribute("books", books);
        model.addAttribute("query", query);
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
        String url = "https://openlibrary.org/works/" + cleanId + ".json";

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        String title = root.path("title").asText();
        String description = "";
        JsonNode descNode = root.path("description");
        if (descNode != null) {
            description = descNode.asText();
        } else if (descNode.has("value")) {
            description = descNode.get("value").asText();
        }

        model.addAttribute("title", title);
        model.addAttribute("description", description);
        return "book";
    }
}

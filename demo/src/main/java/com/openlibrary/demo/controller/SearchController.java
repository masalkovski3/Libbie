package com.openlibrary.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlibrary.demo.model.Book;
import jakarta.servlet.http.HttpSession;
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
            @RequestParam(required = false, defaultValue = "30") int limit,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            Model model,
            HttpSession session) {

        // Om sökning är tom, kolla om vi har sparat en sökning i sessionen
        if(query == null || query.trim().isEmpty()){
            // Kolla om vi har en sparad sökning och använd den
            String savedQuery = (String) session.getAttribute("lastSearchQuery");
            String savedSort = (String) session.getAttribute("lastSearchSort");

            if(savedQuery != null && !savedQuery.isEmpty()) {
                return "redirect:/search?query=" + savedQuery +
                        (savedSort != null ? "&sort=" + savedSort : "");
            }

            model.addAttribute("books", new ArrayList<>());
            model.addAttribute("query", "");
            return "search";
        }

        // Spara denna sökning i session för senare användning
        session.setAttribute("lastSearchQuery", query);
        session.setAttribute("lastSearchSort", sort);

        try {
            RestTemplate restTemplate = new RestTemplate();

            // Bygg enklare URL utan filter
            StringBuilder urlBuilder = new StringBuilder("https://openlibrary.org/search.json?q=");
            urlBuilder.append(query.trim().replace(" ", "+"));

<<<<<<< HEAD
            // Lägg till sorteringsalternativ
            if (sort != null && !sort.isEmpty() && ("new".equals(sort) || "editions".equals(sort))) {
                urlBuilder.append("&sort=").append(sort);
=======
        // Lägg till språkfilter om specificerat
        if (language != null && !language.isEmpty()) {
            urlBuilder.append("&language=");
            urlBuilder.append(language);
        }

        if((sort != null && !sort.isEmpty()) && "new".equals(sort) || "editions".equals(sort)) {
            urlBuilder.append("&sort=").append(sort);
        }

        urlBuilder.append("&limit=").append(limit);

        String url = urlBuilder.toString();
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        JsonNode docs = root.path("docs");


        List<Book> books = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, docs.size()); i++) {
            JsonNode doc = docs.get(i);
            String title = doc.path("title").asText();
            JsonNode authors = doc.path("author_name");
            String author = (authors.isArray() && authors.size() > 0) ? authors.get(0).asText() : "Unknown";
            String workID = doc.path("key").asText();
            String cleanId = workID.replace("/works/", "");

            // Försök hämta ett omslag

            String coverUrl;
            int coverId;
            JsonNode covers = doc.path("cover_i");

            if (!covers.isMissingNode() && !covers.isNull()) {
                coverId = covers.asInt();
                coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
            } else {
                coverId = 0;
                coverUrl = "/images/blue-logo.jpg";
>>>>>>> fix-slow-search
            }
            books.add(new Book(title, author, workID, coverUrl, coverId));

//            String editionsUrl = "https://openlibrary.org/works/" + cleanId + "/editions.json?limit=50";
//            String editionResponse = restTemplate.getForObject(editionsUrl, String.class);
//            JsonNode editionRoot = mapper.readTree(editionResponse);
//            JsonNode editionDocs = editionRoot.path("entries");
//            Integer coverId;
//            String coverUrl;
//
//            if (editionDocs.isArray()) {
//                for (JsonNode edition : editionDocs) {
//                    JsonNode covers = edition.path("covers");
//                    if (covers.isArray() && covers.size() > 0) {
//                        coverId = covers.get(0).asInt();
//                        coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
//                        System.out.println("✅ Cover found: " + coverUrl);
//                        books.add(new Book(title, author, workID, coverUrl));
//                        break;
//
//                    }
//                }
//            }

            urlBuilder.append("&limit=").append(limit);

            String url = urlBuilder.toString();
            System.out.println("Search URL: " + url);

            String response;
            try {
                response = restTemplate.getForObject(url, String.class);
            } catch (Exception e) {
                // Specifik hantering för API-fel
                model.addAttribute("errorMessage", "The book database is currently unavailable. Please try your search again later.");
                model.addAttribute("showError", true);
                model.addAttribute("books", new ArrayList<>());
                model.addAttribute("query", query);
                model.addAttribute("selectedSort", sort);
                return "search";
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode docs = root.path("docs");

            List<Book> books = new ArrayList<>();

            // Konvertera API-resultat till böcker
            for (int i = 0; i < Math.min(limit, docs.size()); i++) {
                JsonNode doc = docs.get(i);

                String title = doc.path("title").asText("Unknown title");

                // Hämta författarnamn
                JsonNode authors = doc.path("author_name");
                String author = (authors.isArray() && authors.size() > 0) ?
                        authors.get(0).asText() : "Unknown";

                String workID = doc.path("key").asText();

                // Hämta bokomslag
                String coverUrl = "";
                JsonNode coverIdNode = doc.path("cover_i");
                if (!coverIdNode.isMissingNode() && !coverIdNode.isNull()) {
                    int coverId = coverIdNode.asInt();
                    coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                }

                books.add(new Book(title, author, workID, coverUrl));
            }

            // Visa felmeddelande om inga böcker hittades
            if (books.isEmpty()) {
                model.addAttribute("errorMessage", "No books found matching your search criteria. Please try a different search term.");
                model.addAttribute("showError", true);
            }

            model.addAttribute("books", books);
            model.addAttribute("query", query);
            model.addAttribute("selectedSort", sort);

            return "search";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred during search. Please try again later.");
            model.addAttribute("showError", true);
            model.addAttribute("books", new ArrayList<>());
            model.addAttribute("query", query);
            model.addAttribute("selectedSort", sort);
            return "search";
        }
    }
}

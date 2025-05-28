package com.openlibrary.demo.controller;

import com.openlibrary.demo.DAO.RecommendationDAO;
import com.openlibrary.demo.model.Book;
import com.openlibrary.demo.model.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.SQLException;
import java.util.List;

@Controller
public class HomeController {
    private final RecommendationDAO recommendationDAO;

    public HomeController(RecommendationDAO recommendationDAO) {
        this.recommendationDAO = recommendationDAO;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        try {
            List<Book> books = recommendationDAO.getTopRatedBooks(20);
            model.addAttribute("books", books);

            Member currentMember = (Member) session.getAttribute("currentMember");
            if (currentMember != null) {
                List<Book> recommendedBooks = recommendationDAO.getRecommendedBooksForUser(currentMember.getId(), 20);
                model.addAttribute("recommendedBooks", recommendedBooks);
            }
        } catch (SQLException e) {
            model.addAttribute("error", "An error occurred while fetching top rated books.");
        }
        return "home";
    }
}

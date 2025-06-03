package com.openlibrary.demo.controller;

import com.openlibrary.demo.DAO.ReviewDAO;
import com.openlibrary.demo.model.Member;
import com.openlibrary.demo.model.Review;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling book reviews.
 * This controller provides endpoints for creating, retrieving, updating, and deleting book reviews.
 * All endpoints return JSON responses and are designed to be used with AJAX requests from the front-end.
 */
@Controller
@RequestMapping("/reviews")
@Tag(name = "Book Reviews", description = "Book review management API")
public class ReviewController {

    private final ReviewDAO reviewDAO;

    @Autowired
    public ReviewController(ReviewDAO reviewDAO) {
        this.reviewDAO = reviewDAO;
    }

    /**
     * Retrieves all reviews for a specific book via AJAX.
     * This endpoint is called when loading the book page to display all reviews.
     *
     * @param workId The Open Library work ID of the book
     * @return JSON response containing reviews and average score
     */
    @Operation(summary = "Get book reviews",
            description = "Retrieve all reviews for a specific book")
    @GetMapping("/book/{workId}")
    @ResponseBody
    public ResponseEntity<?> getBookReviews(@PathVariable String workId) {
        try {
            List<Review> reviews = reviewDAO.getReviewsByBookId(workId);
            double averageScore = reviewDAO.getAverageReviewScore(workId);

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviews);
            response.put("averageScore", averageScore);

            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load reviews: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Retrieves the current user's review for a specific book (if exists).
     * This endpoint is called when a logged-in user views a book to check if they've already reviewed it.
     *
     * @param workId The Open Library work ID of the book
     * @param session The HTTP session containing the logged-in user information
     * @return JSON response containing the user's review data or an error message
     */
    @Operation(summary = "Get user's review",
            description = "Get the current user's review for a book")
    @GetMapping("/user/{workId}")
    @ResponseBody
    public ResponseEntity<?> getUserReview(@PathVariable String workId,
                                           HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You must be logged in to view your reviews");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            Review review = reviewDAO.getMemberReview(currentMember.getId(), workId);
            if (review == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("hasReview", false);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("hasReview", true);
                response.put("review", review);
                return ResponseEntity.ok(response);
            }
        } catch (SQLException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load your review: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Creates or updates a review for a book.
     * This endpoint is called when a user submits the review form.
     * If the user has already reviewed the book, their existing review is updated.
     *
     * @param workId The Open Library work ID of the book
     * @param score The rating score (1-5)
     * @param reviewText The text content of the review
     * @param session The HTTP session containing the logged-in user information
     * @return JSON response containing the updated reviews and average score, or an error message
     */
    @Operation(summary = "Save review",
            description = "Create or update a review for a book")
    @PostMapping("/book/{workId}")
    @ResponseBody
    public ResponseEntity<?> saveReview(
            @PathVariable String workId,
            @RequestParam int score,
            @RequestParam String reviewText,
            HttpSession session) {

        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You must be logged in to submit a review");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Validate the score (1-5 stars)
        if (score < 1 || score > 5) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Rating must be between 1 and 5 stars");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        try {
            List<String> userBookIds = reviewDAO.getAllWorkIdsInUserBookshelves(currentMember.getId());

            if (!userBookIds.contains(workId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You need to add this book to one of your bookshelves to write a review.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            reviewDAO.saveReview(currentMember.getId(), workId, score, reviewText);

            // Get updated information
            List<Review> reviews = reviewDAO.getReviewsByBookId(workId);
            double averageScore = reviewDAO.getAverageReviewScore(workId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("averageScore", averageScore);

            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You need to add this book to a bookshelf to be able to write a review.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Deletes a user's review for a book.
     * This endpoint is called when a user wants to remove their review.
     *
     * @param workId The Open Library work ID of the book
     * @param session The HTTP session containing the logged-in user information
     * @return JSON response confirming deletion and containing updated review data, or an error message
     */
    @Operation(summary = "Delete review",
            description = "Delete the user's review for a book")
    @DeleteMapping("/book/{workId}")
    @ResponseBody
    public ResponseEntity<?> deleteReview(@PathVariable String workId,
                                          HttpSession session) {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "You must be logged in to delete a review");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            boolean deleted = reviewDAO.deleteReview(currentMember.getId(), workId);
            if (!deleted) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No review found to delete");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // Get updated information
            List<Review> reviews = reviewDAO.getReviewsByBookId(workId);
            double averageScore = reviewDAO.getAverageReviewScore(workId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", reviews);
            response.put("averageScore", averageScore);

            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete review: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
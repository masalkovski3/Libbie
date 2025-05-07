package com.openlibrary.demo.model;

import java.sql.Timestamp;

/**
 * Represents a book review with rating, text content, and metadata about the reviewer.
 * This class is used to store and display reviews for books in the Libbie application.
 */
public class Review {
    private int score;
    private String text;
    private Timestamp reviewDate;
    private String memberName;
    private String profileImage;

    /**
     * Default constructor
     */
    public Review() {}

    /**
     * Constructs a Review with all required fields
     *
     * @param score The review rating (1-5 stars)
     * @param text The text content of the review
     * @param reviewDate The timestamp when the review was submitted
     * @param memberName The display name of the member who submitted the review
     * @param profileImage The profile image path of the reviewer
     */
    public Review(int score, String text, Timestamp reviewDate, String memberName, String profileImage) {
        this.score = score;
        this.text = text;
        this.reviewDate = reviewDate;
        this.memberName = memberName;
        this.profileImage = profileImage;
    }

    /**
     * Gets the review score
     *
     * @return The review score (1-5)
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the review score
     *
     * @param score The review score (1-5)
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Gets the review text content
     *
     * @return The text content of the review
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the review text content
     *
     * @param text The text content of the review
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the review submission date
     *
     * @return The timestamp when the review was submitted
     */
    public Timestamp getReviewDate() {
        return reviewDate;
    }

    /**
     * Sets the review submission date
     *
     * @param reviewDate The timestamp when the review was submitted
     */
    public void setReviewDate(Timestamp reviewDate) {
        this.reviewDate = reviewDate;
    }

    /**
     * Gets the reviewer's display name
     *
     * @return The name of the member who submitted the review
     */
    public String getMemberName() {
        return memberName;
    }

    /**
     * Sets the reviewer's display name
     *
     * @param memberName The name of the member who submitted the review
     */
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    /**
     * Gets the reviewer's profile image path
     *
     * @return The profile image path of the reviewer
     */
    public String getProfileImage() {
        return profileImage;
    }

    /**
     * Sets the reviewer's profile image path
     *
     * @param profileImage The profile image path of the reviewer
     */
    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /**
     * Generates an HTML string of stars based on the review score
     *
     * @return A string of filled and empty stars representing the score
     */
    public String getStarsHtml() {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= score) {
                stars.append("★"); // Filled star
            } else {
                stars.append("☆"); // Empty star
            }
        }
        return stars.toString();
    }

    /**
     * Formats the review date in a readable way
     *
     * @return A formatted date string in the format "yyyy-MM-dd HH:mm"
     */
    public String getFormattedDate() {
        if (reviewDate == null) return "";
        java.time.LocalDateTime localDateTime = reviewDate.toLocalDateTime();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return localDateTime.format(formatter);
    }
}
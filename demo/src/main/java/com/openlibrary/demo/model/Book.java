package com.openlibrary.demo.model;

/**
 * Represents a book retrieved from the OpenLibrary API.
 * Contains metadata such as title, author, work ID, cover image URL, and cover ID.
 */

public class Book {

    private String title;
    private String author;
    private String workId;
    private String coverUrl;
    private int coverId;
    private double reviewScore;

    /**
     * Default constructor.
     */
    public Book() {}

    /**
     * Constructs a Book with all available attributes.
     *
     * @param title    The title of the book
     * @param author   The author of the book
     * @param workId   The unique work ID from OpenLibrary
     * @param coverUrl The URL to the book's cover image
     * @param coverId  The ID of the book's cover image
     */

    public Book(String title, String author, String workId, String coverUrl, int coverId) {
        this.title = title;
        this.author = author;
        this.workId = workId;
        this.coverUrl = coverUrl;
        this.coverId = coverId;
    }

    /**
     * Constructs a Book without specifying a coverId.
     * Sets coverId to 0 by default.
     *
     * @param title    The title of the book
     * @param author   The author of the book
     * @param workId   The unique work ID from OpenLibrary
     * @param coverUrl The URL to the book's cover image
     */

    public Book(String title, String author, String workId, String coverUrl) {
        this(title, author, workId, coverUrl, 0); // 0 som default för coverId tills det är löst
    }

    public Book(String openLibraryId, String title, String author, String coverUrl, double reviewScore) {
        this.coverUrl = coverUrl;
        this.workId = openLibraryId;
        this.title = title;
        this.author = author;
        this.reviewScore = reviewScore;
    }

    /**
     * Returns the cover image URL of the book.
     *
     * @return The book's cover URL
     */
    public String getCoverUrl() {
        return coverUrl;
    }


    /**
     * Sets the cover image URL of the book.
     *
     * @param coverUrl The book's cover URL
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * Returns the title of the book.
     *
     * @return The book's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the book.
     * @param title The book's title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the author of the book.
     * @return author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the book.
     * @param author
     */
    public void setAuthor(String author) {
        this.author = author;
    }


    /**
     * Returns the work Id of the book.
     * A work ID uniquely identifies the core work (e.g., a novel or nonfiction title)
     * across all its editions and formats in the OpenLibrary database.
     *
     * @return The book's work Id
     */
    public String getWorkId() {
        return workId;
    }

    /**
     * Sets the work ID of the book.
     * This ID should refer to the unique work entry in OpenLibrary, which encompasses all editions.
     * @param workId The book's work ID
     */
    public void setWorkId(String workId) {
        this.workId = workId;
    }

    /**
     * Returns the review score of the book.
     * @return The book's score based on reviews.
     */
    public double getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(double reviewScore) {
        this.reviewScore = reviewScore;
    }
}

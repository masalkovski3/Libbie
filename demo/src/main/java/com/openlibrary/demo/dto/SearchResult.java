package com.openlibrary.demo.dto;

import com.openlibrary.demo.model.Book;

import java.util.List;

/**
 * DTO for encapsulating paginated search results from the Open Library API.
 * Contains both the list of Book objects and the total number of matching results.
 */
public class SearchResult {

    private final List<Book> books;
    private final int totalCount;

    /**
     * Constructs a SearchResult with a list of books and total result count.
     *
     * @param books      the list of books on the current page
     * @param totalCount the total number of results found by the search
     */
    public SearchResult(List<Book> books, int totalCount) {
        this.books = books;
        this.totalCount = totalCount;
    }

    /**
     * Returns the list of books for the current page.
     *
     * @return list of Book objects
     */
    public List<Book> getBooks() {
        return books;
    }

    /**
     * Returns the total number of search results found.
     *
     * @return total result count
     */
    public int getTotalCount() {
        return totalCount;
    }
}

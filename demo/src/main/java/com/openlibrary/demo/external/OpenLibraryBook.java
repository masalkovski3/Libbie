package com.openlibrary.demo.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a single book result from Open Library's search API response.
 * This is a DTO that mirrors the structure of JSON in the "docs" array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryBook {

    private String title;

    @JsonProperty("author_name")
    private List<String> authorName;

    /**
     * The unique identifier of the work, usually starts with "/works/OL...W"
     */
    private String key;

    /**
     * The internal Open Library cover ID, used to build cover image URLs.
     */
    @JsonProperty("cover_i")
    private Integer coverId;

    private String description;

    public String getTitle() {
        return title;
    }

    public List<String> getAuthorName() {
        return authorName;
    }

    public String getKey() {
        return key;
    }

    public Integer getCoverId() {
        return coverId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthorName(List<String> authorName) {
        this.authorName = authorName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setCoverId(Integer coverId) {
        this.coverId = coverId;
    }

    public String getCoverUrl() {
        return (coverId != null)
                ? "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg"
                : "/images/blue-logo.jpeg";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

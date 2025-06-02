package com.openlibrary.demo.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Represents the JSON response from the Open Library search API.
 * Used for parsing the full search result structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryResponse {

    private int numFound;
    private List<OpenLibraryBook> docs;

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public List<OpenLibraryBook> getDocs() {
        return docs;
    }

    public void setDocs(List<OpenLibraryBook> docs) {
        this.docs = docs;
    }
}

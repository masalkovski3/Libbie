package com.openlibrary.demo.model;

public class Book {

    private String title;
    private String author;
    private String workId;

    public Book() {}

    public Book(String title, String author, String workId) {
        this.title = title;
        this.author = author;
        this.workId = workId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

}

package com.openlibrary.demo.model;

public class Book {

    private String title;
    private String author;
    private String workId;
    private String coverUrl;
    private int coverId;

    public Book() {}

    public Book(String title, String author, String workId, String coverUrl, int coverId) {
        this.title = title;
        this.author = author;
        this.workId = workId;
        this.coverUrl = coverUrl;
        this.coverId = coverId;
    }

    public Book(String title, String author, String workId, String coverUrl) {
        this(title, author, workId, coverUrl, 0); // 0 som default för coverId tills det är löst
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

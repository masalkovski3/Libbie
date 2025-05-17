package com.openlibrary.demo.model;

import java.util.Objects;

public class Bookshelf {
    private Long id;
    private String name;
    private Long memberId;
    private String description;

    public Bookshelf() {
    }

    public Bookshelf(String name, Long memberId) {
        this.name = name;
        this.memberId = memberId;
    }

    public Bookshelf(Long id, String name, Long memberId) {
        this.id = id;
        this.name = name;
        this.memberId = memberId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookshelf bookshelf = (Bookshelf) o;
        return Objects.equals(id, bookshelf.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
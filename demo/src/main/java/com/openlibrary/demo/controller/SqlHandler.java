package com.openlibrary.demo.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SqlHandler {
    @Autowired
    private DatabaseController databaseController;

    public SqlHandler(){
        databaseController = new DatabaseController();
    }

    @PostConstruct
    public void startConnection() {
        databaseController.databaseConnection();
    }

    @PreDestroy
    public void kill(){
        databaseController.killConnection();
    }

}

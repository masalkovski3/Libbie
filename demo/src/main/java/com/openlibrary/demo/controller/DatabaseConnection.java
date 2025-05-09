package com.openlibrary.demo.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConnection {

    private DataSource dataSource;

    public DatabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        // Gör initialiseringslogik här som inte behöver direkta DAO-referenser
        // Till exempel, kontrollera databasanslutningen
        try (Connection conn = getConnection()) {
            System.out.println("Database connection pool initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database connection: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

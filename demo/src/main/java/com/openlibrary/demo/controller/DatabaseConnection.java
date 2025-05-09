package com.openlibrary.demo.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a database connection manager that uses a connection pool to provide connections to the database.
 * This component is managed by Spring and is initialized after dependency injection is complete.
 */
@Component
public class DatabaseConnection {

    /**
     * The data source providing database connections.
     */
    private DataSource dataSource;

    /**
     * Constructs a new DatabaseConnection with the provided DataSource.
     *
     * @param dataSource the DataSource used to provide database connections
     */
    public DatabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Initializes the database connection pool.
     * Called automatically after the constructor when the component is created.
     * Verifies that the database connection is available and logs the result.
     */
    @PostConstruct
    public void init() {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection pool initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database connection: " + e.getMessage());
        }
    }

    /**
     * Retrieves a database connection from the connection pool.
     *
     * @return a Connection object representing a connection to the database
     * @throws SQLException if a database access error occurs or the connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

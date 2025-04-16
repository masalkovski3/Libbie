package com.openlibrary.demo.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.postgresql.Driver;


import java.sql.*;
import java.sql.DriverManager;
import java.sql.Statement;

@Controller
public class DatabaseController {
    public String dbURL = "jdbc:postgresql://pgserver.mau.se/";
    @Value("${spring.datasource.username}")
    public String user;
    @Value("${spring.datasource.password}")
    public String password;

    public Connection connection;
    public Statement statement;

    public void databaseConnection(){
        try{
            //Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(dbURL,user,password);
            statement = connection.createStatement();
            System.out.println("Connected to database ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void killConnection(){
        try{
            this.connection.close();
            System.out.println("Database killed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

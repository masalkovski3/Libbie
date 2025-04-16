package com.openlibrary.demo;

import com.openlibrary.demo.controller.DatabaseController;
import com.openlibrary.demo.controller.SqlHandler;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OpenLibraryDemoApplication {



	public static void main(String[] args) {
		SpringApplication.run(OpenLibraryDemoApplication.class, args);

	}

}

package com.openlibrary.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OpenLibraryDemoApplication {

//TODO: Fixa miljövariabler till databasuppkopplingen i application.properties
//TODO: Lösa dynamisk IP så vi kan köra servern i molnet och slipper lokala IP-adresser

	public static void main(String[] args) {
		SpringApplication.run(OpenLibraryDemoApplication.class, args);

	}

}

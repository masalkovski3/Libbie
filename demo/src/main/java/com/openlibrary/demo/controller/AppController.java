package com.openlibrary.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller för att hantera de statiska sidorna i applikationen.
 * Denna controller hanterar HTTP GET-förfrågningar för hemsidan och om-sidan.
 * @author Delaram Azad
 */
@Controller
public class AppController {

    /**
     * Hanterar GET-förfrågningar till "/about".
     *
     * @return namnet på HTML-vyn för om-sidan.
     * @author Delaram Azad
     */
    @GetMapping("/about")
    public String about() {
        return "about";
    }

}

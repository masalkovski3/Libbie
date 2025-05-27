package com.openlibrary.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller för att hantera de statiska sidorna i applikationen.
 * Denna controller hanterar HTTP GET-förfrågningar för hemsidan och om-sidan.
 */
@Controller
@Tag(name = "Static Pages", description = "Static page endpoints")
public class AppController {

    /**
     * Hanterar GET-förfrågningar till "/about".
     *
     * @return namnet på HTML-vyn för om-sidan.
     * @author Delaram Azad
     */
    @Operation(summary = "Display about page",
            description = "Shows information about the Libbie application")
    @GetMapping("/about")
    public String about() {
        return "about";
    }

}

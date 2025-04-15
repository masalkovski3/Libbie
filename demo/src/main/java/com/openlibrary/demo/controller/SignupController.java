package com.openlibrary.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SignupController {

    @GetMapping("/signUp")
    public String signUp() {
        return "signUp";
    }
}

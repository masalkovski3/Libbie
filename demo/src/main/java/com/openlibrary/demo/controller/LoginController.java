package com.openlibrary.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/logIn-signUp")
    public String logIn_signUp() {
        return "logIn-signUp";
    }
}

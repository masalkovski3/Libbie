package com.openlibrary.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SignupController {

    @GetMapping("/signUp")
    public String signUp() {
        return "signUp";
    }

    @PostMapping("/signUp")
    @ResponseBody
    public String handleSignUp(@RequestParam String firstname,
                              @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam("repeat-password") String repeatPassword) {
        System.out.println("Sign-up: " + firstname + "/" + username + "/" + password + "/" + repeatPassword);
        return "OK";
    }
}

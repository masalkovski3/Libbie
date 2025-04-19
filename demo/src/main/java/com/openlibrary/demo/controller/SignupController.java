package com.openlibrary.demo.controller;

import org.springframework.ui.Model;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class SignupController {

    @GetMapping("/signUp")
    public String signUp() {
        return "signUp";
    }

    @Autowired
    private MemberDAO memberDAO;

    @PostMapping("/signUp")
    public String handleSignUp(@RequestParam String firstname,
                              @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam("repeat-password") String repeatPassword, Model model) {
        System.out.println("Sign-up: " + firstname + "/" + username + "/" + password + "/" + repeatPassword);

        try {
            memberDAO.saveMember(username, firstname, password);
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("felmeddelande");
            return "signUp";
        }
    }
}

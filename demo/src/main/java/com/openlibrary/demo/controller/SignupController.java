package com.openlibrary.demo.controller;

import jakarta.servlet.http.HttpSession;
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
                              @RequestParam("repeat-password") String repeatPassword,
                               Model model,
                               HttpSession session) {
        System.out.println("Sign-up: " + firstname + "/" + username + "/" + password + "/" + repeatPassword);

        try {
            Long memberId = memberDAO.saveMember(username, firstname, password);
            var memberOpt = memberDAO.authenticate(username, password);
            if (memberOpt.isPresent()) {
                session.setAttribute("currentMember", memberOpt.get());
            }
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("felmeddelande", "Couldn't create member");
            return "signUp";
        }
    }
}

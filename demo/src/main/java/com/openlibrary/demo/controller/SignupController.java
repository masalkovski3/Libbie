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

    private MemberDAO memberDAO;

    public SignupController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    @PostMapping("/signUp")
    public String handleSignUp(@RequestParam String firstname,
                              @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam("repeat-password") String repeatPassword,
                               Model model,
                               HttpSession session) {
        System.out.println("Sign-up: " + firstname + "/" + username + "/" + password + "/" + repeatPassword);

        try {
          // Validera email
            if (!isValidEmail(username)) {
                model.addAttribute("errorMessage", "Please enter a valid email address");
                model.addAttribute("showError", true);
                model.addAttribute("firstname", firstname);
                model.addAttribute("username", username);
                return "signUp";
            }

            // Validera att lösenorden matchar
            if (!password.equals(repeatPassword)) {
                model.addAttribute("errorMessage", "Passwords do not match");
                model.addAttribute("showError", true);
                model.addAttribute("firstname", firstname);
                model.addAttribute("username", username);
                return "signUp";
            }

            // Kontrollera om email redan existerar
            if (memberDAO.existsByEmail(username)) {
                model.addAttribute("errorMessage", "An account with this email address already exists");
                model.addAttribute("showError", true);
                model.addAttribute("firstname", firstname);
                return "signUp";
            }
          
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

    //Likadan valideringsmetod som i LoginController, kanske kan göras till en gemensam utility-metod
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

}

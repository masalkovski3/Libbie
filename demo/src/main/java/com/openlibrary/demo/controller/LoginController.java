package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@Controller
public class LoginController {

    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    @Autowired
    private MemberDAO memberDAO;

    @PostMapping("/logIn")
    public String handleLogin(@RequestParam String email, @RequestParam String password, Model model) throws SQLException {
        System.out.println("E-post: " + email);
        System.out.println("Lösenord: " + password);

        try {
            if (memberDAO.existsByEmail(email)) {
                System.out.println("Email exists");

                if (memberDAO.verifyPasswordByEmail(email, password)) {
                    System.out.println("Password verified");
                    return "redirect:/profile";

                } else {
                    System.out.println("Password doesn't match");
                    model.addAttribute("felmeddelande");
                }
            } else {
                System.out.println("Email doesn't exist");
                model.addAttribute("felmeddelande");
            }
        } catch (SQLException e) {
            System.out.println("Databasfel: " + e.getMessage());
            model.addAttribute("felmeddelande");
        }

        return "logIn";
    }
}

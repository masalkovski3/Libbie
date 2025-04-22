package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.openlibrary.demo.model.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.Optional;

@Controller
public class LoginController {

    private MemberDAO memberDAO;

    @Autowired
    public LoginController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    @PostMapping("/logIn")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) throws SQLException {
        try{
            Optional<Member> optionalMember = memberDAO.authenticate(email, password);
            if(optionalMember.isPresent()){
                session.setAttribute("currentMember", optionalMember.get());
                redirectAttributes.addFlashAttribute("loginSuccess", "You have successfully logged in!");
                return "redirect:/profile"; //går till profilsidan
            } else {
                //redirectAttributes.addFlashAttribute("loginError", "Invalid email or password!");
                return "redirect:/logIn"; //felaktigt lösenord/email
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "redirect:/logIn"; //Eller annan sida
        }
    }
}

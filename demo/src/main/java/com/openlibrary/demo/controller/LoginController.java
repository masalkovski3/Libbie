package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.openlibrary.demo.model.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
                              Model model) throws SQLException {
        try{
            Optional<Member> optionalMember = memberDAO.authenticate(email, password);
            if(optionalMember.isPresent()){
                session.setAttribute("currentMember", optionalMember.get());
                return "redirect:/profile"; //går till profilsidan
            } else {
                return "redirect:/logIn"; //felaktigt lösenord/email
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "redirect:/logIn"; //Eller annan sida
        }
    }
}

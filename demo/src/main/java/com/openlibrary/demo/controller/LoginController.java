package com.openlibrary.demo.controller;

import com.openlibrary.demo.model.Member;
import com.openlibrary.demo.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    private final MemberRepository memberRepository;

    public LoginController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/logIn-signUp")
    public String logIn_signUp() {
        return "logIn-signUp";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String firstname,
                         @RequestParam String email,
                         @RequestParam String password,
                         RedirectAttributes redirectAttributes) {

        if (memberRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already in use.");
            return "redirect:/logIn-signUp";
        }

        Member newMember = new Member(firstname, email, password); // Obs: lösenord i klartext, fixas senare
        memberRepository.save(newMember);

        redirectAttributes.addFlashAttribute("message", "Signup successful! You can now log in.");
        return "redirect:/logIn-signUp";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isPresent() && memberOpt.get().getPassword().equals(password)) {
            session.setAttribute("loggedInMember", memberOpt.get());
            return "redirect:/profile";
        }

        redirectAttributes.addFlashAttribute("error", "Invalid email or password.");
        return "redirect:/logIn-signUp";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}

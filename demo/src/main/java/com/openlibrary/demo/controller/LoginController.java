package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

   // private final MemberRepository memberRepository;

   /* public LoginController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    */



    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    /*
    @PostMapping("/signup")
    public String signup(@RequestParam String firstname,
                         @RequestParam String email,
                         @RequestParam String password,
                         RedirectAttributes redirectAttributes) {

        if (memberRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email already in use.");
            return "/signup";
        }

        Member newMember = new Member(firstname, email, password); // Obs: lösenord i klartext, fixas senare
        memberRepository.save(newMember);

        redirectAttributes.addFlashAttribute("message", "Signup successful! You can now log in.");
        return "/signup";
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
        return "redirect:/logIn";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

     */

    @PostMapping("/logIn")
    @ResponseBody
    public String handleLogin(@RequestParam String username, @RequestParam String password) {
        System.out.println("username = " + username);
        System.out.println("password = " + password);
        return "OK";
    }

}

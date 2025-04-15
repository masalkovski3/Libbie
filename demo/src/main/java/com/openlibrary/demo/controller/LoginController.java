package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    @PostMapping("/logIn")
    @ResponseBody
    public String handleLogin(@RequestParam String username, @RequestParam String password) {
        System.out.println("username = " + username);
        System.out.println("password = " + password);
        return "OK";
    }
}

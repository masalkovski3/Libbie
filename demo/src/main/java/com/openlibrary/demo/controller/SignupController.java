package com.openlibrary.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for handling user sign-up related requests.
 */
@Controller
public class SignupController {

    private MemberDAO memberDAO;

    /**
     * Constructs a new {@code SignupController} with the specified {@code MemberDAO}.
     *
     * @param memberDAO the DAO used for member data access and authentication
     */
    public SignupController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Handles GET requests to the sign-up page.
     *
     * @return the name of the sign-up view template
     */
    @GetMapping("/signUp")
    public String signUp() {
        return "signUp";
    }

    /**
     * Handles POST requests for user sign-up.
     * Validates the input data, checks for existing email, and performs authentication.
     * If successful, stores the authenticated member in the session and redirects to the profile page.
     * Otherwise, returns the sign-up view with an error message.
     *
     * @param firstname      the first name of the user
     * @param username       the email address (used as username)
     * @param password       the user's chosen password
     * @param repeatPassword the repeated password for confirmation
     * @param model          the model used to pass attributes to the view
     * @param session        the HTTP session used to store user data
     * @return a redirect to the profile page on success, or the sign-up view on failure
     */
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

            memberDAO.saveMember(username,firstname, password);
            var memberOpt = memberDAO.authenticate(username, password);
            if (memberOpt.isPresent()) {
                session.setAttribute("currentMember", memberOpt.get());
            }
          
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Couldn't create member");
            model.addAttribute("showError", true);
            return "signUp";
        }
    }

    /**
     * Validates whether the given email address is in a correct format.
     * Note: This method is duplicated in {@code LoginController} and could be refactored into a shared utility class.
     *
     * @param email the email address to validate
     * @return true if the email format is valid; false otherwise
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

}

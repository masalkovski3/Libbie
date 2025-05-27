package com.openlibrary.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import com.openlibrary.demo.DAO.MemberDAO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller responsible for handling user sign-up related requests.
 */
@Controller
@Tag(name = "Authentication", description = "User registration")
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
    @Operation(summary = "Display signup page")
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
    @Operation(summary = "Process user registration",
            description = "Create new user account and authenticate")
    @PostMapping("/signUp")
    public String handleSignUp(@RequestParam String firstname,
                              @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam("repeat-password") String repeatPassword,
                               Model model,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
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

                redirectAttributes.addFlashAttribute("errorMessage", "Welcome to Libbie! Your account has been successfully created.");
                redirectAttributes.addFlashAttribute("showError", true);
            }
          
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Couldn't create member");
            model.addAttribute("showError", true);
            return "signUp";
        }
    }

    /**
     * Validates whether the provided email address is in a valid format.
     *
     * An email is considered valid if it matches the regular expression pattern,
     * which checks for the general structure of a typical email address.
     *
     * @param email the email address to validate
     * @return {@code true} if the email is valid, {@code false} otherwise
     */
    @Operation(summary = "Validate email format",
            description = "Check if email address has valid format")
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }

}

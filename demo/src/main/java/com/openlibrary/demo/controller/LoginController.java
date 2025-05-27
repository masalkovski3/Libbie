package com.openlibrary.demo.controller;

//import com.openlibrary.demo.repository.MemberRepository;
import com.openlibrary.demo.DAO.MemberDAO;
import com.openlibrary.demo.model.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller responsible for handling login-related requests.
 * This class provides endpoints to render the login page and process user authentication.
 * If the login is successful, the authenticated member is stored in the session and redirected to the profile page.
 * If authentication fails, the user is redirected back to the login page.
 *
 */
@Controller
@Tag(name = "Authentication", description = "User authentication endpoints")
public class LoginController {

    private MemberDAO memberDAO;

    /**
     * Constructs a LoginController with a MemberDAO dependency.
     * @param memberDAO the DAO used to authenticate members
     */
    public LoginController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Displays the login page.
     * @return the name of the login view
     */
    @Operation(summary = "Display login page")
    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    /**
     * Handles login form submission.
     * Authenticates the user based on the provided email and password.
     * If authentication is successful, the member is stored in the session and redirected to the profile page.
     * Otherwise, the user is redirected back to the login page.
     *
     * @param email               the user's email address
     * @param password            the user's password
     * @param session             the HTTP session to store the authenticated member
     * @param redirectAttributes  attributes for flash messages during redirection
     * @return a redirect to the profile page if successful, or back to the login page if not
     * @throws SQLException if a database access error occurs during authentication
     */
    @Operation(summary = "Authenticate user",
            description = "Authenticate user with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Login successful, redirect to profile"),
            @ApiResponse(responseCode = "200", description = "Login failed, return to login page with error")
    })
    @PostMapping("/logIn")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes,
                              Model model) throws SQLException {
        try{
            // Validera email först
            if (!isValidEmail(email)) {
                model.addAttribute("errorMessage", "Please enter a valid email address");
                model.addAttribute("showError", true);
                model.addAttribute("email", email);
                return "logIn";
            }

            //Försök autentisera
            Optional<Member> optionalMember = memberDAO.authenticate(email, password);

            if(optionalMember.isPresent()){
                //Om det lyckas
                session.setAttribute("currentMember", optionalMember.get());

                //ha kvar bekräftelsemeddelande för login?
                redirectAttributes.addFlashAttribute("errorMessage", "You have successfully logged in!"); //ej error men använder samma metod
                redirectAttributes.addFlashAttribute("showError", true);

                return "redirect:/profile";
            } else {
                //Om det inte lyckas
                //Kolla först om email existerar
                if (!memberDAO.existsByEmail(email)) {
                    model.addAttribute("errorMessage", "No account found with this email address");
                    model.addAttribute("showError", true);
                    model.addAttribute("email", email);
                    return "logIn";
                } else {
                    // Email existerar men lösenord är fel
                    model.addAttribute("errorMessage", "Incorrect password. Please try again");
                    model.addAttribute("showError", true);
                    model.addAttribute("email", email);
                    return "logIn";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "A database error occurred. Please try again later");
            model.addAttribute("showError", true);
            model.addAttribute("email", email);
            return "logIn";
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

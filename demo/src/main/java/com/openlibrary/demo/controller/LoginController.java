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

/**
 * Controller responsible for handling login-related requests.
 *
 * This class provides endpoints to render the login page and process user authentication.
 * If the login is successful, the authenticated member is stored in the session and redirected to the profile page.
 * If authentication fails, the user is redirected back to the login page.
 *
 * @author Emmi Masalkovski, Delaram Azad, Amelie Music
 */
@Controller
public class LoginController {

    private MemberDAO memberDAO;

    /**
     * Constructs a LoginController with a MemberDAO dependency.
     *
     * @param memberDAO the DAO used to authenticate members
     *
     * @author Emmi Masalkovski
     */
    @Autowired
    public LoginController(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Displays the login page.
     *
     * @return the name of the login view
     *
     * @author Delaram Azad
     */
    @GetMapping("/logIn")
    public String logIn() {
        return "logIn";
    }

    /**
     * Handles login form submission.
     *
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
     *
     * @author Amelie Music, Emmi Masalkovski
     */
    @PostMapping("/logIn")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) throws SQLException {
        try{
            Optional<Member> optionalMember = memberDAO.authenticate(email, password);
            if(optionalMember.isPresent()){
                session.setAttribute("currentMember", optionalMember.get());
                redirectAttributes.addFlashAttribute("loginSuccess", "You have successfully logged in!");
                return "redirect:/profile"; //går till profilsidan
            } else {
                // TODO: Lägg till felmeddelande.
                return "redirect:/logIn"; //felaktigt lösenord/email
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "redirect:/logIn"; //Eller annan sida
        }
    }
}

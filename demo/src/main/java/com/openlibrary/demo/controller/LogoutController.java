package com.openlibrary.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsible for handling user logout.
 *
 * <p>When the user accesses the /logout endpoint, the session is invalidated
 * and the user is redirected to the login page with a logout flag. This flag
 * can be used to display a confirmation message to the user.</p>
 *
 * <p>This controller assumes that session-based authentication is used and
 * that the user's identity is stored under the session attribute "currentMember".</p>
 */
@Controller
public class LogoutController {

    /**
     * Logs out the user by invalidating the current session and redirects
     * them to the login page with a logout success parameter.
     *
     * @param session the current HTTP session
     * @return a redirect to the login page with logout indicator
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/logIn?logout=true";
    }

}

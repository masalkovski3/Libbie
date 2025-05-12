package com.openlibrary.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller responsible for handling user logout.
 *
 * When the user accesses the /logout endpoint, the session is invalidated
 * and the user is redirected to the login page with a logout flag. This flag
 * can be used to display a confirmation message to the user.
 *
 * This controller assumes that session-based authentication is used and
 * that the user's identity is stored under the session attribute "currentMember".
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
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();

        redirectAttributes.addFlashAttribute("errorMessage", "You have been successfully logged out.");
        redirectAttributes.addFlashAttribute("showError", true);

        return "redirect:/logIn";
    }

}

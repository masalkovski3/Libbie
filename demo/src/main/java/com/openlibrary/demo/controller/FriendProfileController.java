package com.openlibrary.demo.controller;

import com.openlibrary.demo.DAO.BookshelfDAO;
import com.openlibrary.demo.DAO.FriendshipDAO;
import com.openlibrary.demo.DAO.MemberDAO;
import com.openlibrary.demo.DAO.ReviewDAO;
import com.openlibrary.demo.model.Book;
import com.openlibrary.demo.model.Bookshelf;
import com.openlibrary.demo.model.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling the viewing of a friend's profile.
 * Only accessible if the logged-in user is friends with the target member.
 */
@Controller
@RequestMapping("/friend")
public class FriendProfileController {

    private final MemberDAO memberDAO;
    private final FriendshipDAO friendshipDAO;
    private final BookshelfDAO bookshelfDAO;

    public FriendProfileController(MemberDAO memberDAO, FriendshipDAO friendshipDAO,BookshelfDAO bookshelfDAO) {
        this.memberDAO = memberDAO;
        this.friendshipDAO = friendshipDAO;
        this.bookshelfDAO = bookshelfDAO;
    }

    /**
     * Displays a read-only view of a friend's profile, including public bookshelves and books in each shelf.
     *
     * @param friendId The ID of the friend whose profile is being viewed.
     * @param session  The current HTTP session to get the logged-in user.
     * @param model    Spring's model to pass data to the view.
     * @return The friendProfile view, or an error/redirect if access is invalid.
     * @throws SQLException if a database access error occurs.
     */
    @GetMapping("/{friendId}")
    public String showFriendProfile(@PathVariable Long friendId,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) throws SQLException {
        Member currentMember = (Member) session.getAttribute("currentMember");
        if (currentMember == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not currently logged in\nPlease login again.");
            return "redirect:/logIn";
        }
        Long currentUserId = currentMember.getId();

        if (!friendshipDAO.areFriends(currentUserId, friendId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not friends with this user");
            return "profile";
        }

        Optional<Map<String, Object>> friendOptional = memberDAO.findById(friendId);
        if (friendOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This user doesn't exist");
            return "profile";
        }

        Map<String, Object> friend = friendOptional.get();
        List<Bookshelf> publicBookshelves = bookshelfDAO.getPublicBookshelvesByMemberId(friendId);

        Map<Long, List<Map<String, Object>>> booksByShelf = new HashMap<>();
        for (Bookshelf shelf : publicBookshelves) {
            List<Map<String, Object>> books = bookshelfDAO.findBooksByBookshelfId(shelf.getId());
            booksByShelf.put(shelf.getId(), books);
        }

        model.addAttribute("friend", friend);
        model.addAttribute("bookshelves", publicBookshelves);
        model.addAttribute("booksByShelf", booksByShelf);

        return "friendProfile";
    }

}

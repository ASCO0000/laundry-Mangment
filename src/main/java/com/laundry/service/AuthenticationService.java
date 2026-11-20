package com.laundry.service;

import com.laundry.dao.UserDAO;
import com.laundry.exception.AuthenticationException;
import com.laundry.exception.ValidationException;
import com.laundry.model.User;
import com.laundry.util.PasswordUtil;
import com.laundry.util.ValidationUtil;

/**
 * Service managing user login sessions and role-based access control.
 */
public class AuthenticationService {

    private final UserDAO userDAO;
    private static User currentUser = null;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user against stored credentials.
     */
    public User login(String username, String password) {
        ValidationUtil.requireNonEmpty(username, "Username");
        ValidationUtil.requireNonEmpty(password, "Password");

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("This account is inactive. Please contact the administrator.");
        }

        boolean matched = PasswordUtil.checkPassword(password, user.getPasswordHash());
        if (!matched) {
            throw new AuthenticationException("Invalid username or password.");
        }

        currentUser = user;
        return user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static void logout() {
        currentUser = null;
    }

    public void changePassword(int userId, String oldPassword, String newPassword) {
        ValidationUtil.requireNonEmpty(oldPassword, "Current password");
        ValidationUtil.validatePasswordStrength(newPassword);

        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ValidationException("User not found.");
        }

        if (!PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password does not match.");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        userDAO.updatePassword(userId, newHash);
    }
}

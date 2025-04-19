package com.openlibrary.demo.util;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordUtils {

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}



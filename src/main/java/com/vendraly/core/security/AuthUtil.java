package com.vendraly.core.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidades para manejo de contraseñas usando BCrypt.
 */
public final class AuthUtil {

    private AuthUtil() {
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String password, String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(password, hash);
    }
}

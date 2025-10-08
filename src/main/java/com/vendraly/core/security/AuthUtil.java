package com.vendraly.core.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Clase de utilidad para manejar operaciones de seguridad, principalmente el hashing de contraseñas.
 * Usa BCrypt para asegurar que las contraseñas nunca se almacenen en texto plano.
 */
public class AuthUtil {

    // Cost (número de rondas). 10–12 es recomendado para servidores normales.
    private static final int COST = 12;

    /**
     * Hashea una contraseña usando BCrypt con salt aleatorio.
     * @param password La contraseña en texto plano.
     * @return El hash de la contraseña, incluyendo el salt.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    /**
     * Verifica si una contraseña coincide con un hash dado.
     * @param password La contraseña en texto plano a verificar.
     * @param hashedPassword El hash almacenado en la base de datos.
     * @return true si la contraseña es válida, false en caso contrario.
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) return false;
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Por si el hash no es válido o está corrupto
            return false;
        }
    }

    /**
     * Permite detectar si el hash debería actualizarse (ejemplo: cost más alto).
     * @param hashedPassword Hash almacenado.
     * @return true si necesita rehash.
     */
    public static boolean needsRehash(String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2")) return true;
        try {
            int rounds = Integer.parseInt(hashedPassword.split("\\$")[2]);
            return rounds < COST;
        } catch (Exception e) {
            return true;
        }
    }
}

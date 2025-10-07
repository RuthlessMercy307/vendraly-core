package com.vendraly.core.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Clase de utilidad para manejar operaciones de seguridad, principalmente el hashing de contraseñas.
 * Usa SHA-256 para asegurar que las contraseñas nunca se almacenen en texto plano.
 */
public class AuthUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Hashea una contraseña usando el algoritmo SHA-256.
     * @param password La contraseña en texto plano.
     * @return El hash de la contraseña como una cadena hexadecimal, o null si el algoritmo falla.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(password.getBytes());

            // Convertir el array de bytes a una representación hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                // Asegura que cada byte se represente con 2 caracteres
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Este error nunca debería ocurrir con SHA-256
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifica si una contraseña coincide con un hash dado.
     * @param password La contraseña en texto plano a verificar.
     * @param hashedPassword El hash almacenado en la base de datos.
     * @return true si el hash de la contraseña coincide con el hash almacenado, false en caso contrario.
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        String submittedHash = hashPassword(password);
        return submittedHash != null && submittedHash.equals(hashedPassword);
    }
}
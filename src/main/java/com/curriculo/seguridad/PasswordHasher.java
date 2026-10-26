package com.curriculo.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad criptográfica para generar y verificar hashes de contraseñas mediante SHA-256.
 * Previene el almacenamiento de contraseñas en texto plano en la base de datos PostgreSQL.
 */
public class PasswordHasher {

    /**
     * Calcula el hash SHA-256 en formato hexadecimal de 64 caracteres.
     *
     * @param password Contraseña en texto plano.
     * @return Hash SHA-256 en minúsculas, o null si la contraseña es nula.
     */
    public static String hash(String password) {
        if (password == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error crítico: Algoritmo SHA-256 no disponible en la JVM: " + e.getMessage(), e);
        }
    }

    /**
     * Compara una contraseña plana contra un hash almacenado en la base de datos.
     *
     * @param passwordPlana  Contraseña en texto plano enviada por el usuario.
     * @param hashAlmacenado Hash SHA-256 registrado en la base de datos.
     * @return true si coinciden exactamente, false en caso contrario.
     */
    public static boolean verificar(String passwordPlana, String hashAlmacenado) {
        if (passwordPlana == null || hashAlmacenado == null) {
            return false;
        }
        String hashCalculado = hash(passwordPlana);
        return hashCalculado.equalsIgnoreCase(hashAlmacenado.trim());
    }
}

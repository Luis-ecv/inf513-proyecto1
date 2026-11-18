package com.curriculo.seguridad;

import java.util.regex.Pattern;

/**
 * Validador estricto de políticas de contraseñas para el sistema.
 * Reglas exigidas:
 * 1. Longitud obligatoria: Mínimo 8 y Máximo 25 caracteres.
 * 2. Caracteres permitidos: Letras mayúsculas (A-Z, Ñ), minúsculas (a-z, ñ), dígitos (0-9) y símbolos especiales permitidos.
 * 3. Símbolos especiales permitidos: @, #, $, %, *, !, ?, ., -, _
 * 4. Requisitos obligatorios de complejidad:
 *    - Al menos 1 letra mayúscula (A-Z o Ñ).
 *    - Al menos 1 letra minúscula (a-z o ñ).
 *    - Al menos 1 dígito numérico (0-9).
 *    - Al menos 1 carácter especial permitido (@, #, $, %, *, !, ?, ., -, _).
 * 5. Prohibidos: Espacios en blanco, comillas dobles, comas, corchetes y tildes.
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 25;

    // Caracteres permitidos globales (sin espacios ni comillas ni corchetes)
    private static final Pattern PATTERN_CARACTERES_PERMITIDOS = Pattern.compile("^[a-zA-Z0-9ñÑ@#$%*!?._\\-]+$");

    // Reglas de complejidad individuales
    private static final Pattern PATTERN_MAYUSCULA = Pattern.compile("[A-ZÑ]");
    private static final Pattern PATTERN_MINUSCULA = Pattern.compile("[a-zñ]");
    private static final Pattern PATTERN_DIGITO = Pattern.compile("[0-9]");
    private static final Pattern PATTERN_ESPECIAL = Pattern.compile("[@#$%*!?._\\-]");

    public static final String EJEMPLO_VALIDO = "Curriculo2026ñ*";
    public static final String SIMBOLOS_PERMITIDOS = "@, #, $, %, *, !, ?, ., -, _";

    public static class Resultado {
        private final boolean valido;
        private final String mensaje;

        private Resultado(boolean valido, String mensaje) {
            this.valido = valido;
            this.mensaje = mensaje;
        }

        public static Resultado exito() {
            return new Resultado(true, null);
        }

        public static Resultado error(String motivo) {
            String detalle = "Error en la contraseña: " + motivo + "\n" +
                    "• Reglas requeridas:\n" +
                    "  - Longitud permitida: Entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres.\n" +
                    "  - Complejidad obligatoria: Al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.\n" +
                    "  - Símbolos especiales permitidos: " + SIMBOLOS_PERMITIDOS + "\n" +
                    "  - Restricción: No se permiten espacios en blanco, comillas ni caracteres no autorizados.\n" +
                    "• Ejemplo de contraseña válida: " + EJEMPLO_VALIDO;
            return new Resultado(false, detalle);
        }

        public boolean isValido() {
            return valido;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    public static Resultado validar(String password) {
        if (password == null || password.trim().isEmpty()) {
            return Resultado.error("La contraseña es un campo obligatorio y no puede estar vacía.");
        }

        int longitud = password.length();

        if (longitud < MIN_LENGTH) {
            return Resultado.error("La contraseña tiene " + longitud + " caracteres, incumpliendo la longitud mínima requerida (mínimo " + MIN_LENGTH + " caracteres).");
        }

        if (longitud > MAX_LENGTH) {
            return Resultado.error("La contraseña supera el límite máximo permitido de " + MAX_LENGTH + " caracteres (longitud actual: " + longitud + ").");
        }

        if (!PATTERN_CARACTERES_PERMITIDOS.matcher(password).matches()) {
            return Resultado.error("Se detectaron caracteres no permitidos (espacios, comillas, corchetes o símbolos no autorizados).");
        }

        if (!PATTERN_MAYUSCULA.matcher(password).find()) {
            return Resultado.error("Debe contener al menos una letra mayúscula (A-Z o Ñ).");
        }

        if (!PATTERN_MINUSCULA.matcher(password).find()) {
            return Resultado.error("Debe contener al menos una letra minúscula (a-z o ñ).");
        }

        if (!PATTERN_DIGITO.matcher(password).find()) {
            return Resultado.error("Debe contener al menos un dígito numérico (0-9).");
        }

        if (!PATTERN_ESPECIAL.matcher(password).find()) {
            return Resultado.error("Debe contener al menos un carácter especial permitido (" + SIMBOLOS_PERMITIDOS + ").");
        }

        return Resultado.exito();
    }
}

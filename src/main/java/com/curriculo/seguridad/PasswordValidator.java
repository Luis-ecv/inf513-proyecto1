package com.curriculo.seguridad;

import java.util.regex.Pattern;

/**
 * Validador estricto de políticas de contraseñas para el sistema.
 * Reglas exigidas:
 * 1. Longitud obligatoria: Mínimo 8 y Máximo 25 caracteres.
 * 2. Caracteres permitidos: Letras mayúsculas (A-Z), minúsculas (a-z), dígitos (0-9) y letras 'ñ' / 'Ñ'.
 * 3. Prohibidos: Caracteres especiales (@, #, $, %, &, *, !, ?, etc.), espacios en blanco y tildes.
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 25;
    private static final Pattern PATTERN_CARACTERES = Pattern.compile("^[a-zA-Z0-9ñÑ]+$");
    public static final String EJEMPLO_VALIDO = "Curriculo2026ñ";

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
                    "  - Caracteres admitidos: Solo letras (A-Z, a-z), la letra 'ñ' / 'Ñ' y números (0-9).\n" +
                    "  - Restricción: No se permiten caracteres especiales (@, #, $, etc.), tildes ni espacios.\n" +
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

        if (!PATTERN_CARACTERES.matcher(password).matches()) {
            return Resultado.error("Se detectaron caracteres especiales, espacios o acentos no permitidos.");
        }

        return Resultado.exito();
    }
}

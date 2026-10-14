package com.curriculo.config;

/**
 * Gestor centralizado de configuración del sistema.
 * Permite sobreescribir valores mediante variables de entorno o propiedades del sistema,
 * manteniendo por defecto las credenciales oficiales de la plataforma TecnoWeb.
 */
public class Config {

    // Servidor de correo electrónico (POP3 y SMTP)
    public static final String MAIL_HOST = getEnv("MAIL_HOST", "www.tecnoweb.org.bo");
    public static final int POP3_PORT = getEnvInt("POP3_PORT", 110);
    public static final int SMTP_PORT = getEnvInt("SMTP_PORT", 25);
    public static final String MAIL_USER = getEnv("MAIL_USER", "grupo02sa");
    public static final String MAIL_PASSWORD = getEnv("MAIL_PASSWORD", "grup002grup002*");
    public static final String MAIL_EMAIL = getEnv("MAIL_EMAIL", "grupo02sa@tecnoweb.org.bo");

    // Servidor de Base de Datos PostgreSQL
    public static final String DB_HOST = getEnv("DB_HOST", "db.tecnoweb.org.bo");
    public static final String DB_PORT = getEnv("DB_PORT", "5432");
    public static final String DB_NAME = getEnv("DB_NAME", "db_grupo02sa");
    public static final String DB_USER = getEnv("DB_USER", "grupo02sa");
    public static final String DB_PASSWORD = getEnv("DB_PASSWORD", "grup002grup002*");

    // Intervalo de sondeo POP3 en milisegundos
    public static final int POP3_POLL_INTERVAL_MS = getEnvInt("POP3_POLL_INTERVAL_MS", 10000);

    private Config() {
        // Evitar instanciación directa
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String value = getEnv(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}

package com.curriculo.db;

import com.curriculo.config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Proveedor de conexiones JDBC hacia la base de datos PostgreSQL.
 */
public class Conexion {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver de PostgreSQL no encontrado en el classpath: " + e.getMessage());
        }
    }

    private Conexion() {
        // Evitar instanciación directa
    }

    public static Connection getConnection() {
        String url = String.format("jdbc:postgresql://%s:%s/%s?characterEncoding=UTF-8",
                Config.DB_HOST, Config.DB_PORT, Config.DB_NAME);
        try {
            return DriverManager.getConnection(url, Config.DB_USER, Config.DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos PostgreSQL: " + e.getMessage());
            return null;
        }
    }
}

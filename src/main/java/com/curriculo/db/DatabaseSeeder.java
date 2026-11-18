package com.curriculo.db;

import com.curriculo.dao.UsuarioDAO;
import com.curriculo.modelo.Usuario;
import com.curriculo.seguridad.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Seeder de base de datos para sembrar usuarios administradores de prueba con
 * contraseñas
 * encriptadas en SHA-256 según la política de seguridad institucional.
 */
public class DatabaseSeeder {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO SIEMBRA DE BASE DE DATOS (SEEDS) ===");
        seederAdministradores();
        migrarUsuarioEvans();
        System.out.println("=== SIEMBRA COMPLETADA CON ÉXITO ===");
    }

    public static void seederAdministradores() {
        UsuarioDAO usuarioDAO = new UsuarioDAO();

        // Lista de los 4 administradores solicitados
        Usuario[] administradores = new Usuario[] {
                new Usuario(
                        "Luis Fernando",
                        "luis.ecv1111@gmail.com",
                        "luisecv",
                        PasswordHasher.hash("Admin2026ñ"),
                        "fotos/luis.png",
                        1 // Rol 1: Administrador / Director
                ),
                new Usuario(
                        "Carlos Marca",
                        "marcacarlosestudio@gmail.com",
                        "cmarca",
                        PasswordHasher.hash("Admin2026ñ"),
                        "fotos/carlos.png",
                        1),
                new Usuario(
                        "Tania Valda",
                        "taniavaldadonozo@gmail.com",
                        "tvalda",
                        PasswordHasher.hash("Admin2026ñ"),
                        "fotos/tania.png",
                        1),
                new Usuario(
                        "The Criptale",
                        "thecriptale@gmail.com",
                        "criptale",
                        PasswordHasher.hash("Admin2026ñ"),
                        "fotos/criptale.png",
                        1)
        };

        for (Usuario admin : administradores) {
            Usuario existenteEmail = usuarioDAO.obtenerPorEmail(admin.getEmail());
            Usuario existenteUser = usuarioDAO.obtenerPorUsername(admin.getUsername());

            if (existenteEmail != null) {
                System.out.println("[OMITIDO] El usuario con correo '" + admin.getEmail() + "' ya existe con ID: "
                        + existenteEmail.getId());
            } else if (existenteUser != null) {
                System.out.println("[OMITIDO] El username '" + admin.getUsername() + "' ya existe con ID: "
                        + existenteUser.getId());
            } else {
                int id = usuarioDAO.insertar(admin);
                if (id > 0) {
                    System.out.println("[CREADO] Administrador: " + admin.getNombreCompleto() + " | Email: "
                            + admin.getEmail() + " | ID asignado: " + id + " (Password hasheado SHA-256)");
                } else {
                    System.err.println("[ERROR] No se pudo insertar administrador: " + admin.getEmail());
                }
            }
        }
    }

    public static void migrarUsuarioEvans() {
        // Migrar al usuario ID 1 para que su contraseña plana 'admin123' quede
        // encriptada en SHA-256
        String sql = "UPDATE usuario SET password = ? WHERE id = 1 AND password = 'admin123'";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, PasswordHasher.hash("admin123"));
                int filas = ps.executeUpdate();
                if (filas > 0) {
                    System.out.println(
                            "[MIGRADO] Contraseña plana de Ing. Evans Balcazar (ID 1) actualizada a hash SHA-256.");
                } else {
                    System.out
                            .println("[INFO] Usuario ID 1 ya contaba con contraseña hasheada o no requirió migración.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al migrar contraseña del usuario ID 1: " + e.getMessage());
        }
    }
}

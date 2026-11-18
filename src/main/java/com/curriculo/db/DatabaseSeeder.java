package com.curriculo.db;

import com.curriculo.dao.UsuarioDAO;
import com.curriculo.modelo.Usuario;
import com.curriculo.seguridad.PasswordHasher;

/**
 * Seeder de base de datos para sembrar y sincronizar los usuarios del sistema
 * con contraseñas seguras encriptadas en SHA-256 (política estricta con carácter especial).
 */
public class DatabaseSeeder {

    public static final String PASSWORD_DEFAULT = "Admin2026ñ*";

    public static void main(String[] args) {
        System.out.println("=== INICIANDO SIEMBRA Y SINCRONIZACIÓN DE USUARIOS (SEEDS) ===");
        seederUsuarios();
        System.out.println("=== SIEMBRA Y ACTUALIZACIÓN COMPLETADA CON ÉXITO ===");
    }

    public static void seederUsuarios() {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        String passHash = PasswordHasher.hash(PASSWORD_DEFAULT);

        // Lista de los 7 usuarios del sistema (Directores, Delegados e Invitados)
        Usuario[] usuarios = new Usuario[] {
                new Usuario(
                        "Ing. Evans Balcazar",
                        "grupo02sa@tecnoweb.org.bo",
                        "director",
                        passHash,
                        "fotos/director.png",
                        1 // Rol 1: Director
                ),
                new Usuario(
                        "Luis Fernando",
                        "luis.ecv1111@gmail.com",
                        "luisecv",
                        passHash,
                        "fotos/luis.png",
                        1 // Rol 1: Director
                ),
                new Usuario(
                        "Carlos Marca",
                        "marcacarlosestudio@gmail.com",
                        "cmarca",
                        passHash,
                        "fotos/carlos.png",
                        1 // Rol 1: Director
                ),
                new Usuario(
                        "Tania Valda",
                        "taniavaldadonozo@gmail.com",
                        "tvalda",
                        passHash,
                        "fotos/tania.png",
                        1 // Rol 1: Director
                ),
                new Usuario(
                        "The Criptale",
                        "thecriptale@gmail.com",
                        "criptale",
                        passHash,
                        "fotos/criptale.png",
                        1 // Rol 1: Director
                ),
                new Usuario(
                        "Lic. Carlos Roca (Delegado Curricular)",
                        "croca@uagrm.edu.bo",
                        "croca",
                        passHash,
                        "fotos/croca.png",
                        2 // Rol 2: Delegado
                ),
                new Usuario(
                        "Dr. Mario Mendoza (Docente Invitado)",
                        "mmendoza@uagrm.edu.bo",
                        "mmendoza",
                        passHash,
                        "fotos/mmendoza.png",
                        3 // Rol 3: Invitado
                )
        };

        for (Usuario u : usuarios) {
            Usuario existenteEmail = usuarioDAO.obtenerPorEmail(u.getEmail());
            Usuario existenteUser = usuarioDAO.obtenerPorUsername(u.getUsername());

            if (existenteEmail != null) {
                existenteEmail.setPasswordHash(u.getPasswordHash());
                existenteEmail.setIdRol(u.getIdRol());
                existenteEmail.setActivo(true);
                // Si tiene foto previa personalizada, se conserva; si no, se asigna la del seeder
                if (existenteEmail.getFotoRuta() == null || existenteEmail.getFotoRuta().trim().isEmpty()) {
                    existenteEmail.setFotoRuta(u.getFotoRuta());
                }
                boolean actualizado = usuarioDAO.actualizar(existenteEmail);
                if (actualizado) {
                    System.out.println("[ACTUALIZADO] Usuario: " + existenteEmail.getNombreCompleto()
                            + " | Email: " + existenteEmail.getEmail()
                            + " | Password SHA-256 actualizado con éxito.");
                } else {
                    System.err.println("[ERROR] No se pudo actualizar contraseña para: " + existenteEmail.getEmail());
                }
            } else if (existenteUser != null) {
                existenteUser.setPasswordHash(u.getPasswordHash());
                existenteUser.setIdRol(u.getIdRol());
                existenteUser.setActivo(true);
                boolean actualizado = usuarioDAO.actualizar(existenteUser);
                if (actualizado) {
                    System.out.println("[ACTUALIZADO] Usuario (por username): " + existenteUser.getUsername()
                            + " | Password SHA-256 actualizado con éxito.");
                } else {
                    System.err.println("[ERROR] No se pudo actualizar usuario por username: " + existenteUser.getUsername());
                }
            } else {
                int id = usuarioDAO.insertar(u);
                if (id > 0) {
                    System.out.println("[CREADO] Usuario: " + u.getNombreCompleto()
                            + " | Email: " + u.getEmail()
                            + " | ID asignado: " + id
                            + " (Password hasheado SHA-256)");
                } else {
                    System.err.println("[ERROR] No se pudo insertar usuario: " + u.getEmail());
                }
            }
        }
    }
}

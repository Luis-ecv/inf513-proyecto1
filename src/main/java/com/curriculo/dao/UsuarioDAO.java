package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Usuario;
import com.curriculo.seguridad.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de usuarios y roles (CU1).
 * Implementado con try-with-resources para prevenir fugas de conexión.
 */
public class UsuarioDAO {

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nombre_completo, u.email, u.username, u.password, " +
                "u.foto_ruta, u.activo, u.fecha_registro, u.id_rol, r.nombre AS rol_nombre " +
                "FROM usuario u " +
                "JOIN rol r ON u.id_rol = r.id " +
                "ORDER BY u.id ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre_completo"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("foto_ruta"),
                            rs.getBoolean("activo"),
                            rs.getTimestamp("fecha_registro"),
                            rs.getInt("id_rol"),
                            rs.getString("rol_nombre")
                    );
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    public int insertar(Usuario u) {
        String sql = "INSERT INTO usuario(nombre_completo, email, username, password, foto_ruta, id_rol) " +
                "VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getNombreCompleto());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getUsername());
                ps.setString(4, u.getPasswordHash());
                ps.setString(5, u.getFotoRuta());
                ps.setInt(6, u.getIdRol());

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar usuario: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Usuario u) {
        String sql = "UPDATE usuario SET nombre_completo = ?, email = ?, username = ?, " +
                "password = ?, foto_ruta = ?, id_rol = ?, activo = ? WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getNombreCompleto());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getUsername());
                ps.setString(4, u.getPasswordHash());
                ps.setString(5, u.getFotoRuta());
                ps.setInt(6, u.getIdRol());
                ps.setBoolean(7, u.isActivo());
                ps.setInt(8, u.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuario WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }

    public Usuario obtenerPorEmail(String email) {
        String sql = "SELECT u.id, u.nombre_completo, u.email, u.username, u.password, " +
                "u.foto_ruta, u.activo, u.fecha_registro, u.id_rol, r.nombre AS rol_nombre " +
                "FROM usuario u " +
                "JOIN rol r ON u.id_rol = r.id " +
                "WHERE u.email = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapearUsuario(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario por email: " + e.getMessage());
        }
        return null;
    }

    public Usuario obtenerPorUsername(String username) {
        String sql = "SELECT u.id, u.nombre_completo, u.email, u.username, u.password, " +
                "u.foto_ruta, u.activo, u.fecha_registro, u.id_rol, r.nombre AS rol_nombre " +
                "FROM usuario u " +
                "JOIN rol r ON u.id_rol = r.id " +
                "WHERE u.username = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapearUsuario(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario por username: " + e.getMessage());
        }
        return null;
    }

    public Usuario autenticar(String email, String password) {
        String sql = "SELECT u.id, u.nombre_completo, u.email, u.username, u.password, " +
                "u.foto_ruta, u.activo, u.fecha_registro, u.id_rol, r.nombre AS rol_nombre " +
                "FROM usuario u " +
                "JOIN rol r ON u.id_rol = r.id " +
                "WHERE u.email = ? AND (u.password = ? OR u.password = ?) AND u.activo = true";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, PasswordHasher.hash(password));
                ps.setString(3, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapearUsuario(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al autenticar usuario: " + e.getMessage());
        }
        return null;
    }

    public boolean asociarEmail(int idUsuario, String email) {
        String sql = "UPDATE usuario SET email = ? WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setInt(2, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al asociar email al usuario: " + e.getMessage());
            return false;
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nombre_completo"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("foto_ruta"),
                rs.getBoolean("activo"),
                rs.getTimestamp("fecha_registro"),
                rs.getInt("id_rol"),
                rs.getString("rol_nombre"));
    }
}

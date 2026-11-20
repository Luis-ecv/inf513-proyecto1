package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Capitulo;
import com.curriculo.modelo.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de Capítulos y Asignación de Responsables de Redacción (CU4,
 * RF-05, RF-06).
 */
public class CapituloDAO {

    public List<Capitulo> listarPorComponente(int idComponente) {
        List<Capitulo> lista = new ArrayList<>();
        String sql = "SELECT c.id, c.numero, c.titulo, c.contenido, c.id_componente, c.id_estado, c.created_at AS fecha_creacion, " +
                "comp.nombre AS componente_nombre, e.nombre AS estado_nombre, " +
                "COALESCE(STRING_AGG(u.nombre_completo, ', '), 'Sin asignar') AS responsables " +
                "FROM capitulo c " +
                "JOIN componente comp ON c.id_componente = comp.id " +
                "JOIN estado_capitulo e ON c.id_estado = e.id " +
                "LEFT JOIN asignacion_capitulo ac ON c.id = ac.id_capitulo " +
                "LEFT JOIN usuario u ON ac.id_usuario = u.id " +
                "WHERE c.id_componente = ? " +
                "GROUP BY c.id, comp.nombre, e.nombre " +
                "ORDER BY c.numero ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idComponente);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Capitulo cap = new Capitulo(
                                rs.getInt("id"),
                                rs.getInt("numero"),
                                rs.getString("titulo"),
                                rs.getString("contenido"),
                                rs.getInt("id_componente"),
                                rs.getInt("id_estado"),
                                rs.getTimestamp("fecha_creacion"));
                        cap.setNombreComponente(rs.getString("componente_nombre"));
                        cap.setNombreEstado(rs.getString("estado_nombre"));
                        cap.setResponsables(rs.getString("responsables"));
                        lista.add(cap);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar capítulos por componente: " + e.getMessage());
        }
        return lista;
    }

    public List<Capitulo> listarTodos() {
        List<Capitulo> lista = new ArrayList<>();
        String sql = "SELECT c.id, c.numero, c.titulo, c.contenido, c.id_componente, c.id_estado, c.created_at AS fecha_creacion, " +
                "comp.nombre AS componente_nombre, e.nombre AS estado_nombre, " +
                "COALESCE(STRING_AGG(u.nombre_completo, ', '), 'Sin asignar') AS responsables " +
                "FROM capitulo c " +
                "JOIN componente comp ON c.id_componente = comp.id " +
                "JOIN estado_capitulo e ON c.id_estado = e.id " +
                "LEFT JOIN asignacion_capitulo ac ON c.id = ac.id_capitulo " +
                "LEFT JOIN usuario u ON ac.id_usuario = u.id " +
                "GROUP BY c.id, comp.nombre, e.nombre " +
                "ORDER BY c.id_componente ASC, c.numero ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Capitulo cap = new Capitulo(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getString("titulo"),
                            rs.getString("contenido"),
                            rs.getInt("id_componente"),
                            rs.getInt("id_estado"),
                            rs.getTimestamp("fecha_creacion"));
                    cap.setNombreComponente(rs.getString("componente_nombre"));
                    cap.setNombreEstado(rs.getString("estado_nombre"));
                    cap.setResponsables(rs.getString("responsables"));
                    lista.add(cap);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar todos los capítulos: " + e.getMessage());
        }
        return lista;
    }

    public int insertar(Capitulo c) {
        String sql = "INSERT INTO capitulo(numero, titulo, contenido, id_componente, id_estado, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.getNumero());
                ps.setString(2, c.getTitulo());
                ps.setString(3, c.getContenido());
                ps.setInt(4, c.getIdComponente());
                ps.setInt(5, c.getIdEstado() > 0 ? c.getIdEstado() : 1);

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
            System.err.println("Error al insertar capítulo: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Capitulo c) {
        String sql = "UPDATE capitulo SET numero = ?, titulo = ?, contenido = ?, id_componente = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, c.getNumero());
                ps.setString(2, c.getTitulo());
                ps.setString(3, c.getContenido());
                ps.setInt(4, c.getIdComponente());
                ps.setInt(5, c.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar capítulo: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM capitulo WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar capítulo: " + e.getMessage());
            return false;
        }
    }

    public Capitulo obtenerPorId(int id) {
        String sql = "SELECT c.id, c.numero, c.titulo, c.contenido, c.id_componente, c.id_estado, c.created_at AS fecha_creacion, " +
                "comp.nombre AS componente_nombre, e.nombre AS estado_nombre, " +
                "COALESCE(STRING_AGG(u.nombre_completo, ', '), 'Sin asignar') AS responsables " +
                "FROM capitulo c " +
                "JOIN componente comp ON c.id_componente = comp.id " +
                "JOIN estado_capitulo e ON c.id_estado = e.id " +
                "LEFT JOIN asignacion_capitulo ac ON c.id = ac.id_capitulo " +
                "LEFT JOIN usuario u ON ac.id_usuario = u.id " +
                "WHERE c.id = ? " +
                "GROUP BY c.id, comp.nombre, e.nombre";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Capitulo cap = new Capitulo(
                                rs.getInt("id"),
                                rs.getInt("numero"),
                                rs.getString("titulo"),
                                rs.getString("contenido"),
                                rs.getInt("id_componente"),
                                rs.getInt("id_estado"),
                                rs.getTimestamp("fecha_creacion"));
                        cap.setNombreComponente(rs.getString("componente_nombre"));
                        cap.setNombreEstado(rs.getString("estado_nombre"));
                        cap.setResponsables(rs.getString("responsables"));
                        return cap;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener capítulo por id: " + e.getMessage());
        }
        return null;
    }

    public boolean asignarRedactor(int idCapitulo, int idUsuario) {
        String sql = "INSERT INTO asignacion_capitulo(id_capitulo, id_usuario) VALUES(?, ?) " +
                "ON CONFLICT (id_capitulo, id_usuario) DO NOTHING";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                ps.setInt(2, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al asignar redactor a capítulo: " + e.getMessage());
            return false;
        }
    }

    public boolean removerRedactor(int idCapitulo, int idUsuario) {
        String sql = "DELETE FROM asignacion_capitulo WHERE id_capitulo = ? AND id_usuario = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                ps.setInt(2, idUsuario);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al remover asignación de redactor: " + e.getMessage());
            return false;
        }
    }

    public List<Usuario> listarRedactores(int idCapitulo) {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nombre_completo, u.email, u.username, u.password, " +
                "u.foto_ruta, u.activo, u.fecha_registro, u.id_rol, r.nombre AS rol_nombre " +
                "FROM asignacion_capitulo ac " +
                "JOIN usuario u ON ac.id_usuario = u.id " +
                "JOIN rol r ON u.id_rol = r.id " +
                "WHERE ac.id_capitulo = ? ORDER BY u.nombre_completo ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            System.err.println("Error al listar redactores del capítulo: " + e.getMessage());
        }
        return lista;
    }

    public boolean cambiarEstado(int idCapitulo, int nuevoEstadoId) {
        String sql = "UPDATE capitulo SET id_estado = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, nuevoEstadoId);
                ps.setInt(2, idCapitulo);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al cambiar estado del capítulo: " + e.getMessage());
            return false;
        }
    }

    public int contarEvidencias(int idCapitulo) {
        String sql = "SELECT COUNT(*) FROM evidencia_capitulo WHERE id_capitulo = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return 0;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar evidencias del capítulo: " + e.getMessage());
        }
        return 0;
    }
}

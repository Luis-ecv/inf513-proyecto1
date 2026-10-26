package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Observacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de observaciones y subsanaciones técnicas sobre capítulos
 * (CU5, RF-08).
 */
public class ObservacionDAO {

    public int insertar(Observacion o) {
        String sql = "INSERT INTO observacion(comentario, id_capitulo, id_usuario_observador) VALUES(?, ?, ?)";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, o.getComentario());
                ps.setInt(2, o.getIdCapitulo());
                ps.setInt(3, o.getIdUsuarioObservador());

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
            System.err.println("Error al insertar observación: " + e.getMessage());
        }
        return -1;
    }

    public boolean subsanar(int idObs, String respuesta, int idDelegado) {
        String sql = "UPDATE observacion SET respuesta_delegado = ?, fecha_respuesta = CURRENT_TIMESTAMP, " +
                "resuelta = TRUE, fecha_cierre = CURRENT_TIMESTAMP, id_usuario_respuesta = ? WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, respuesta);
                ps.setInt(2, idDelegado);
                ps.setInt(3, idObs);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al subsanar observación: " + e.getMessage());
            return false;
        }
    }

    public List<Observacion> listarPorCapitulo(int idCapitulo) {
        List<Observacion> lista = new ArrayList<>();
        String sql = "SELECT o.id, o.comentario, o.fecha_creacion, o.respuesta_delegado, o.fecha_respuesta, " +
                "o.resuelta, o.fecha_cierre, o.id_capitulo, o.id_usuario_observador, o.id_usuario_respuesta, " +
                "u1.nombre_completo AS observador_nombre, u2.nombre_completo AS delegado_nombre, " +
                "c.titulo AS capitulo_titulo " +
                "FROM observacion o " +
                "JOIN capitulo c ON o.id_capitulo = c.id " +
                "JOIN usuario u1 ON o.id_usuario_observador = u1.id " +
                "LEFT JOIN usuario u2 ON o.id_usuario_respuesta = u2.id " +
                "WHERE o.id_capitulo = ? " +
                "ORDER BY o.fecha_creacion DESC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Observacion o = new Observacion(
                                rs.getInt("id"),
                                rs.getString("comentario"),
                                rs.getTimestamp("fecha_creacion"),
                                rs.getString("respuesta_delegado"),
                                rs.getTimestamp("fecha_respuesta"),
                                rs.getBoolean("resuelta"),
                                rs.getTimestamp("fecha_cierre"),
                                rs.getInt("id_capitulo"),
                                rs.getInt("id_usuario_observador"),
                                (Integer) rs.getObject("id_usuario_respuesta"));
                        o.setNombreObservador(rs.getString("observador_nombre"));
                        o.setNombreRespuesta(rs.getString("delegado_nombre"));
                        o.setTituloCapitulo(rs.getString("capitulo_titulo"));
                        lista.add(o);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar observaciones: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Cuenta cuántas observaciones tiene un capítulo que NO hayan sido resueltas
     * aún (RF-07).
     */
    public int contarObservacionesPendientes(int idCapitulo) {
        String sql = "SELECT COUNT(*) FROM observacion WHERE id_capitulo = ? AND resuelta = FALSE";
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
            System.err.println("Error al contar observaciones pendientes: " + e.getMessage());
        }
        return 0;
    }

    public Observacion obtenerPorId(int id) {
        String sql = "SELECT o.id, o.comentario, o.fecha_creacion, o.respuesta_delegado, o.fecha_respuesta, " +
                "o.resuelta, o.fecha_cierre, o.id_capitulo, o.id_usuario_observador, o.id_usuario_respuesta, " +
                "u1.nombre_completo AS observador_nombre, u2.nombre_completo AS delegado_nombre, " +
                "c.titulo AS capitulo_titulo " +
                "FROM observacion o " +
                "JOIN capitulo c ON o.id_capitulo = c.id " +
                "JOIN usuario u1 ON o.id_usuario_observador = u1.id " +
                "LEFT JOIN usuario u2 ON o.id_usuario_respuesta = u2.id " +
                "WHERE o.id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Observacion o = new Observacion(
                                rs.getInt("id"),
                                rs.getString("comentario"),
                                rs.getTimestamp("fecha_creacion"),
                                rs.getString("respuesta_delegado"),
                                rs.getTimestamp("fecha_respuesta"),
                                rs.getBoolean("resuelta"),
                                rs.getTimestamp("fecha_cierre"),
                                rs.getInt("id_capitulo"),
                                rs.getInt("id_usuario_observador"),
                                (Integer) rs.getObject("id_usuario_respuesta"));
                        o.setNombreObservador(rs.getString("observador_nombre"));
                        o.setNombreRespuesta(rs.getString("delegado_nombre"));
                        o.setTituloCapitulo(rs.getString("capitulo_titulo"));
                        return o;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener observación por id: " + e.getMessage());
        }
        return null;
    }
}

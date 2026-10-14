package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.HistorialModificacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la pista de auditoría inmutable de cambios de estado (CU5, RF-10).
 */
public class HistorialDAO {

    public int registrar(HistorialModificacion h) {
        String sql = "INSERT INTO historial_modificacion(estado_anterior, estado_nuevo, descripcion_accion, " +
                "justificacion_tecnica, id_capitulo, id_usuario) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, h.getEstadoAnterior());
                ps.setString(2, h.getEstadoNuevo());
                ps.setString(3, h.getDescripcionAccion());
                ps.setString(4, h.getJustificacionTecnica());
                ps.setInt(5, h.getIdCapitulo());
                ps.setInt(6, h.getIdUsuario());

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
            System.err.println("Error al registrar auditoría en historial: " + e.getMessage());
        }
        return -1;
    }

    public List<HistorialModificacion> listarPorCapitulo(int idCapitulo) {
        List<HistorialModificacion> lista = new ArrayList<>();
        String sql = "SELECT h.id, h.estado_anterior, h.estado_nuevo, h.descripcion_accion, " +
                "h.justificacion_tecnica, h.fecha_hora, h.id_capitulo, h.id_usuario, " +
                "u.nombre_completo AS usuario_nombre, c.titulo AS capitulo_titulo " +
                "FROM historial_modificacion h " +
                "JOIN usuario u ON h.id_usuario = u.id " +
                "JOIN capitulo c ON h.id_capitulo = c.id " +
                "WHERE h.id_capitulo = ? " +
                "ORDER BY h.fecha_hora DESC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        HistorialModificacion h = new HistorialModificacion(
                                rs.getInt("id"),
                                rs.getString("estado_anterior"),
                                rs.getString("estado_nuevo"),
                                rs.getString("descripcion_accion"),
                                rs.getString("justificacion_tecnica"),
                                rs.getTimestamp("fecha_hora"),
                                rs.getInt("id_capitulo"),
                                rs.getInt("id_usuario")
                        );
                        h.setNombreUsuario(rs.getString("usuario_nombre"));
                        h.setTituloCapitulo(rs.getString("capitulo_titulo"));
                        lista.add(h);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar historial de modificaciones: " + e.getMessage());
        }
        return lista;
    }
}

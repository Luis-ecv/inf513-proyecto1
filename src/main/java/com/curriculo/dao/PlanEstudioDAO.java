package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.PlanEstudio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión del Plan de Estudios (CU2, RF-01).
 */
public class PlanEstudioDAO {

    public List<PlanEstudio> listar() {
        List<PlanEstudio> lista = new ArrayList<>();
        String sql = "SELECT id, codigo, carrera, anio_vigencia, descripcion, objetivos, " +
                "perfil_egreso, limite_creditos_min, limite_creditos_max, estado, created_at AS fecha_creacion " +
                "FROM plan_estudio " +
                "ORDER BY anio_vigencia DESC, id DESC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    PlanEstudio p = new PlanEstudio(
                            rs.getInt("id"),
                            rs.getString("codigo"),
                            rs.getString("carrera"),
                            rs.getInt("anio_vigencia"),
                            rs.getString("descripcion"),
                            rs.getString("objetivos"),
                            rs.getString("perfil_egreso"),
                            rs.getInt("limite_creditos_min"),
                            rs.getInt("limite_creditos_max"),
                            rs.getString("estado"),
                            rs.getTimestamp("fecha_creacion")
                    );
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar planes de estudio: " + e.getMessage());
        }
        return lista;
    }

    public int insertar(PlanEstudio p) {
        String sql = "INSERT INTO plan_estudio(codigo, carrera, anio_vigencia, descripcion, " +
                "objetivos, perfil_egreso, limite_creditos_min, limite_creditos_max, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getCodigo());
                ps.setString(2, p.getCarrera());
                ps.setInt(3, p.getAnioVigencia());
                ps.setString(4, p.getDescripcion());
                ps.setString(5, p.getObjetivos());
                ps.setString(6, p.getPerfilEgreso());
                ps.setInt(7, p.getLimiteCreditosMin());
                ps.setInt(8, p.getLimiteCreditosMax());

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
            System.err.println("Error al insertar plan de estudio: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(PlanEstudio p) {
        String sql = "UPDATE plan_estudio SET codigo = ?, carrera = ?, anio_vigencia = ?, " +
                "descripcion = ?, objetivos = ?, perfil_egreso = ?, limite_creditos_min = ?, " +
                "limite_creditos_max = ?, estado = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getCodigo());
                ps.setString(2, p.getCarrera());
                ps.setInt(3, p.getAnioVigencia());
                ps.setString(4, p.getDescripcion());
                ps.setString(5, p.getObjetivos());
                ps.setString(6, p.getPerfilEgreso());
                ps.setInt(7, p.getLimiteCreditosMin());
                ps.setInt(8, p.getLimiteCreditosMax());
                ps.setString(9, p.getEstado());
                ps.setInt(10, p.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar plan de estudio: " + e.getMessage());
            return false;
        }
    }

    public PlanEstudio obtenerPorId(int id) {
        String sql = "SELECT id, codigo, carrera, anio_vigencia, descripcion, objetivos, " +
                "perfil_egreso, limite_creditos_min, limite_creditos_max, estado, created_at AS fecha_creacion " +
                "FROM plan_estudio WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new PlanEstudio(
                                rs.getInt("id"),
                                rs.getString("codigo"),
                                rs.getString("carrera"),
                                rs.getInt("anio_vigencia"),
                                rs.getString("descripcion"),
                                rs.getString("objetivos"),
                                rs.getString("perfil_egreso"),
                                rs.getInt("limite_creditos_min"),
                                rs.getInt("limite_creditos_max"),
                                rs.getString("estado"),
                                rs.getTimestamp("fecha_creacion")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener plan por id: " + e.getMessage());
        }
        return null;
    }
}

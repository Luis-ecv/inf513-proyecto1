package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Asignatura;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de Asignaturas (CU2, RF-02).
 */
public class AsignaturaDAO {

    public List<Asignatura> listarPorPlan(int idPlan) {
        List<Asignatura> lista = new ArrayList<>();
        String sql = "SELECT id, sigla, nombre, semestre, horas_teoricas, horas_practicas, " +
                "creditos, area_formativa, id_plan_estudio " +
                "FROM asignatura " +
                "WHERE id_plan_estudio = ? " +
                "ORDER BY semestre ASC, sigla ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPlan);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Asignatura a = new Asignatura(
                                rs.getInt("id"),
                                rs.getString("sigla"),
                                rs.getString("nombre"),
                                rs.getInt("semestre"),
                                rs.getInt("horas_teoricas"),
                                rs.getInt("horas_practicas"),
                                rs.getInt("creditos"),
                                rs.getString("area_formativa"),
                                rs.getInt("id_plan_estudio")
                        );
                        lista.add(a);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar asignaturas del plan: " + e.getMessage());
        }
        return lista;
    }

    public List<Asignatura> listarTodas() {
        List<Asignatura> lista = new ArrayList<>();
        String sql = "SELECT id, sigla, nombre, semestre, horas_teoricas, horas_practicas, " +
                "creditos, area_formativa, id_plan_estudio " +
                "FROM asignatura ORDER BY id_plan_estudio ASC, semestre ASC, sigla ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asignatura a = new Asignatura(
                            rs.getInt("id"),
                            rs.getString("sigla"),
                            rs.getString("nombre"),
                            rs.getInt("semestre"),
                            rs.getInt("horas_teoricas"),
                            rs.getInt("horas_practicas"),
                            rs.getInt("creditos"),
                            rs.getString("area_formativa"),
                            rs.getInt("id_plan_estudio")
                    );
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar todas las asignaturas: " + e.getMessage());
        }
        return lista;
    }

    public int insertar(Asignatura a) {
        String sql = "INSERT INTO asignatura(sigla, nombre, semestre, horas_teoricas, " +
                "horas_practicas, creditos, area_formativa, id_plan_estudio) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, a.getSigla());
                ps.setString(2, a.getNombre());
                ps.setInt(3, a.getSemestre());
                ps.setInt(4, a.getHorasTeoricas());
                ps.setInt(5, a.getHorasPracticas());
                ps.setInt(6, a.getCreditos());
                ps.setString(7, a.getAreaFormativa());
                ps.setInt(8, a.getIdPlanEstudio());

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
            System.err.println("Error al insertar asignatura: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Asignatura a) {
        String sql = "UPDATE asignatura SET sigla = ?, nombre = ?, semestre = ?, " +
                "horas_teoricas = ?, horas_practicas = ?, creditos = ?, area_formativa = ? " +
                "WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.getSigla());
                ps.setString(2, a.getNombre());
                ps.setInt(3, a.getSemestre());
                ps.setInt(4, a.getHorasTeoricas());
                ps.setInt(5, a.getHorasPracticas());
                ps.setInt(6, a.getCreditos());
                ps.setString(7, a.getAreaFormativa());
                ps.setInt(8, a.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar asignatura: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM asignatura WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar asignatura: " + e.getMessage());
            return false;
        }
    }

    public Asignatura obtenerPorId(int id) {
        String sql = "SELECT id, sigla, nombre, semestre, horas_teoricas, horas_practicas, " +
                "creditos, area_formativa, id_plan_estudio FROM asignatura WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Asignatura(
                                rs.getInt("id"),
                                rs.getString("sigla"),
                                rs.getString("nombre"),
                                rs.getInt("semestre"),
                                rs.getInt("horas_teoricas"),
                                rs.getInt("horas_practicas"),
                                rs.getInt("creditos"),
                                rs.getString("area_formativa"),
                                rs.getInt("id_plan_estudio")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener asignatura por id: " + e.getMessage());
        }
        return null;
    }
}

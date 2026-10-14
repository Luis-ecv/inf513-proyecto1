package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Componente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de Componentes temáticos del rediseño curricular (CU4, RF-05).
 */
public class ComponenteDAO {

    public List<Componente> listarPorPlan(int idPlan) {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT id, numero, nombre, descripcion, id_plan_estudio " +
                "FROM componente WHERE id_plan_estudio = ? ORDER BY numero ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPlan);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Componente c = new Componente(
                                rs.getInt("id"),
                                rs.getInt("numero"),
                                rs.getString("nombre"),
                                rs.getString("descripcion"),
                                rs.getInt("id_plan_estudio")
                        );
                        lista.add(c);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar componentes por plan: " + e.getMessage());
        }
        return lista;
    }

    public List<Componente> listarTodos() {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT id, numero, nombre, descripcion, id_plan_estudio " +
                "FROM componente ORDER BY id_plan_estudio ASC, numero ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Componente c = new Componente(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getInt("id_plan_estudio")
                    );
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar todos los componentes: " + e.getMessage());
        }
        return lista;
    }

    public int insertar(Componente c) {
        String sql = "INSERT INTO componente(numero, nombre, descripcion, id_plan_estudio) VALUES(?, ?, ?, ?)";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.getNumero());
                ps.setString(2, c.getNombre());
                ps.setString(3, c.getDescripcion());
                ps.setInt(4, c.getIdPlanEstudio());

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
            System.err.println("Error al insertar componente: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(Componente c) {
        String sql = "UPDATE componente SET numero = ?, nombre = ?, descripcion = ? WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, c.getNumero());
                ps.setString(2, c.getNombre());
                ps.setString(3, c.getDescripcion());
                ps.setInt(4, c.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar componente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM componente WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar componente: " + e.getMessage());
            return false;
        }
    }

    public Componente obtenerPorId(int id) {
        String sql = "SELECT id, numero, nombre, descripcion, id_plan_estudio FROM componente WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Componente(
                                rs.getInt("id"),
                                rs.getInt("numero"),
                                rs.getString("nombre"),
                                rs.getString("descripcion"),
                                rs.getInt("id_plan_estudio")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener componente por id: " + e.getMessage());
        }
        return null;
    }
}

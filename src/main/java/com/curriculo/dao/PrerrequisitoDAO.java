package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Asignatura;
import com.curriculo.modelo.Prerrequisito;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * DAO para gestión de Prerrequisitos y validación de Grafo Acíclico Dirigido (DAG) (CU3, RF-03, RF-04).
 */
public class PrerrequisitoDAO {

    /**
     * Inserta un prerrequisito validando previamente que no genere ciclos directos ni indirectos.
     * Retorna 1 si tuvo éxito, -1 si generaría un ciclo, 0 si ocurrió un error SQL o ya existía.
     */
    public int insertarConValidacion(int idAsignatura, int idRequisito) {
        if (idAsignatura == idRequisito) {
            System.err.println("Rechazado: Una asignatura no puede ser prerrequisito de sí misma.");
            return -1; // Ciclo directo
        }

        if (existe(idAsignatura, idRequisito)) {
            System.err.println("El prerrequisito ya está registrado.");
            return 0;
        }

        if (generariaCiclo(idAsignatura, idRequisito)) {
            System.err.println("Rechazado por validación de grafo: Se detectó dependencia circular indirecta.");
            return -1; // Ciclo indirecto detectado
        }

        String sql = "INSERT INTO prerrequisito(id_asignatura, id_asignatura_requisito) VALUES(?, ?)";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idAsignatura);
                ps.setInt(2, idRequisito);
                return ps.executeUpdate() > 0 ? 1 : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar prerrequisito: " + e.getMessage());
            return 0;
        }
    }

    public boolean eliminar(int idAsignatura, int idRequisito) {
        String sql = "DELETE FROM prerrequisito WHERE id_asignatura = ? AND id_asignatura_requisito = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idAsignatura);
                ps.setInt(2, idRequisito);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar prerrequisito: " + e.getMessage());
            return false;
        }
    }

    public boolean existe(int idAsignatura, int idRequisito) {
        String sql = "SELECT 1 FROM prerrequisito WHERE id_asignatura = ? AND id_asignatura_requisito = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idAsignatura);
                ps.setInt(2, idRequisito);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar prerrequisito: " + e.getMessage());
            return false;
        }
    }

    /**
     * Algoritmo de Detección de Ciclos en Grafo Dirigido (RF-03).
     * Comprueba si agregar la arista (idAsignatura -> idRequisito) genera un ciclo.
     * Esto ocurre si ya existe un camino navegando desde idRequisito hacia idAsignatura.
     */
    public boolean generariaCiclo(int idAsignatura, int idRequisito) {
        Set<Integer> visitados = new HashSet<>();
        Queue<Integer> cola = new LinkedList<>();

        cola.add(idRequisito);
        visitados.add(idRequisito);

        while (!cola.isEmpty()) {
            int actual = cola.poll();

            if (actual == idAsignatura) {
                return true; // Se encontró un camino de regreso: ¡Ciclo detectado!
            }

            // Obtener todos los prerrequisitos que 'actual' necesita
            List<Integer> prerequisitosDeActual = obtenerIdsRequisitosDirectos(actual);
            for (int vecino : prerequisitosDeActual) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    cola.add(vecino);
                }
            }
        }

        return false;
    }

    public List<Integer> obtenerIdsRequisitosDirectos(int idAsignatura) {
        List<Integer> lista = new ArrayList<>();
        String sql = "SELECT id_asignatura_requisito FROM prerrequisito WHERE id_asignatura = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idAsignatura);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener IDs de requisitos directos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista los prerrequisitos directos con datos descriptivos de la materia requisito (RF-03).
     */
    public List<Prerrequisito> listarRequisitosDirectos(int idAsignatura) {
        List<Prerrequisito> lista = new ArrayList<>();
        String sql = "SELECT p.id_asignatura, p.id_asignatura_requisito, " +
                "a1.sigla AS sigla_asig, a1.nombre AS nombre_asig, a1.semestre AS sem_asig, " +
                "a2.sigla AS sigla_req, a2.nombre AS nombre_req, a2.semestre AS sem_req " +
                "FROM prerrequisito p " +
                "JOIN asignatura a1 ON p.id_asignatura = a1.id " +
                "JOIN asignatura a2 ON p.id_asignatura_requisito = a2.id " +
                "WHERE p.id_asignatura = ? " +
                "ORDER BY a2.semestre ASC, a2.sigla ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idAsignatura);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Prerrequisito p = new Prerrequisito(
                                rs.getInt("id_asignatura"),
                                rs.getInt("id_asignatura_requisito"),
                                rs.getString("sigla_asig"),
                                rs.getString("nombre_asig"),
                                rs.getInt("sem_asig"),
                                rs.getString("sigla_req"),
                                rs.getString("nombre_req"),
                                rs.getInt("sem_req")
                        );
                        lista.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar prerrequisitos directos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene la cadena completa de prerrequisitos hacia atrás (todas las materias previas requeridas) (RF-04).
     */
    public List<Asignatura> obtenerCadenaHaciaAtras(int idAsignatura) {
        List<Asignatura> resultado = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();
        Queue<Integer> cola = new LinkedList<>();

        cola.add(idAsignatura);
        visitados.add(idAsignatura);

        AsignaturaDAO asignaturaDAO = new AsignaturaDAO();

        while (!cola.isEmpty()) {
            int actual = cola.poll();
            List<Integer> requisitos = obtenerIdsRequisitosDirectos(actual);

            for (int reqId : requisitos) {
                if (!visitados.contains(reqId)) {
                    visitados.add(reqId);
                    cola.add(reqId);
                    Asignatura asig = asignaturaDAO.obtenerPorId(reqId);
                    if (asig != null) {
                        resultado.add(asig);
                    }
                }
            }
        }

        // Ordenar por semestre ascendente
        resultado.sort((a, b) -> Integer.compare(a.getSemestre(), b.getSemestre()));
        return resultado;
    }

    /**
     * Obtiene la cadena de asignaturas dependientes hacia adelante (materias que requieren de esta) (RF-04).
     */
    public List<Asignatura> obtenerCadenaHaciaAdelante(int idAsignatura) {
        List<Asignatura> resultado = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();
        Queue<Integer> cola = new LinkedList<>();

        cola.add(idAsignatura);
        visitados.add(idAsignatura);

        AsignaturaDAO asignaturaDAO = new AsignaturaDAO();

        while (!cola.isEmpty()) {
            int actual = cola.poll();
            List<Integer> dependientes = obtenerIdsDependientesDirectos(actual);

            for (int depId : dependientes) {
                if (!visitados.contains(depId)) {
                    visitados.add(depId);
                    cola.add(depId);
                    Asignatura asig = asignaturaDAO.obtenerPorId(depId);
                    if (asig != null) {
                        resultado.add(asig);
                    }
                }
            }
        }

        // Ordenar por semestre ascendente
        resultado.sort((a, b) -> Integer.compare(a.getSemestre(), b.getSemestre()));
        return resultado;
    }

    private List<Integer> obtenerIdsDependientesDirectos(int idRequisito) {
        List<Integer> lista = new ArrayList<>();
        String sql = "SELECT id_asignatura FROM prerrequisito WHERE id_asignatura_requisito = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null) return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idRequisito);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener IDs de materias dependientes: " + e.getMessage());
        }
        return lista;
    }
}

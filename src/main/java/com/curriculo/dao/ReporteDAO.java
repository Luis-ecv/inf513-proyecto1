package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.Asignatura;
import com.curriculo.modelo.Capitulo;
import com.curriculo.modelo.DocumentoEvidencia;
import com.curriculo.modelo.PlanEstudio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO analítico para la generación de métricas de avance, tablero de control
 * (CU7, RF-09),
 * compilación del informe final consolidado (CU7, RF-15) y estadísticas de
 * productividad (CU8).
 */
public class ReporteDAO {

    public static class MetricasDashboard {
        public int totalCapitulos;
        public int borradorCount;
        public int enRevisionCount;
        public int observadoCount;
        public int aprobadoCount;
        public double porcentajeAvance;
        public List<Capitulo> capitulosObservados = new ArrayList<>();
        public int totalAsignaturas;
        public int totalCreditos;
    }

    public static class EstadisticaComponente {
        public int idComponente;
        public int numero;
        public String nombre;
        public int totalCapitulos;
        public int aprobados;
        public double porcentaje;
    }

    public static class EstadisticaUsuario {
        public int idUsuario;
        public String nombreCompleto;
        public String rolNombre;
        public int capitulosAsignados;
        public int observacionesEmitidas;
        public int observacionesSubsanadas;
    }

    public static class ReporteFinalConsolidado {
        public PlanEstudio plan;
        public List<Asignatura> asignaturas = new ArrayList<>();
        public List<Capitulo> capitulosAprobados = new ArrayList<>();
        public List<DocumentoEvidencia> evidenciasVigentes = new ArrayList<>();
        public int totalCreditos;
    }

    /**
     * Obtiene las métricas consolidadas del Tablero de Control (RF-09).
     */
    public MetricasDashboard obtenerMetricasDashboard() {
        MetricasDashboard m = new MetricasDashboard();

        String sqlEstados = "SELECT e.nombre, COUNT(c.id) AS cantidad " +
                "FROM estado_capitulo e " +
                "LEFT JOIN capitulo c ON e.id = c.id_estado " +
                "GROUP BY e.id, e.nombre";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return m;

            // 1. Conteo por estados de la máquina de estados
            try (PreparedStatement ps = conn.prepareStatement(sqlEstados);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("nombre");
                    int cant = rs.getInt("cantidad");
                    m.totalCapitulos += cant;

                    if ("Borrador".equalsIgnoreCase(estado))
                        m.borradorCount = cant;
                    else if ("En Revisión".equalsIgnoreCase(estado))
                        m.enRevisionCount = cant;
                    else if ("Observado".equalsIgnoreCase(estado))
                        m.observadoCount = cant;
                    else if ("Aprobado".equalsIgnoreCase(estado))
                        m.aprobadoCount = cant;
                }
            }

            if (m.totalCapitulos > 0) {
                m.porcentajeAvance = ((double) m.aprobadoCount / m.totalCapitulos) * 100.0;
            }

            // 2. Alertas de capítulos en estado 'Observado' con detalle de observaciones
            // pendientes
            String sqlObs = "SELECT c.id, c.numero, c.titulo, comp.nombre AS comp_nombre, e.nombre AS est_nombre " +
                    "FROM capitulo c " +
                    "JOIN componente comp ON c.id_componente = comp.id " +
                    "JOIN estado_capitulo e ON c.id_estado = e.id " +
                    "WHERE c.id_estado = 3 " +
                    "ORDER BY c.numero ASC";

            try (PreparedStatement ps = conn.prepareStatement(sqlObs);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Capitulo cap = new Capitulo(
                            rs.getInt("id"),
                            rs.getInt("numero"),
                            rs.getString("titulo"),
                            null,
                            0,
                            3,
                            null);
                    cap.setNombreComponente(rs.getString("comp_nombre"));
                    cap.setNombreEstado(rs.getString("est_nombre"));
                    m.capitulosObservados.add(cap);
                }
            }

            // 3. Totales de la malla curricular
            String sqlMalla = "SELECT COUNT(id) AS total_asig, COALESCE(SUM(creditos), 0) AS total_cred FROM asignatura";
            try (PreparedStatement ps = conn.prepareStatement(sqlMalla);
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.totalAsignaturas = rs.getInt("total_asig");
                    m.totalCreditos = rs.getInt("total_cred");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener métricas del dashboard: " + e.getMessage());
        }

        return m;
    }

    /**
     * Compila todos los elementos oficiales para el Reporte Final Consolidado
     * (RF-15).
     */
    public ReporteFinalConsolidado compilarReporteFinal(int idPlan) {
        ReporteFinalConsolidado rep = new ReporteFinalConsolidado();
        PlanEstudioDAO planDAO = new PlanEstudioDAO();
        rep.plan = planDAO.obtenerPorId(idPlan);

        if (rep.plan == null) {
            return null;
        }

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return rep;

            // 1. Asignaturas de la malla
            String sqlAsig = "SELECT id, sigla, nombre, semestre, horas_teoricas, horas_practicas, creditos, area_formativa "
                    +
                    "FROM asignatura WHERE id_plan_estudio = ? ORDER BY semestre ASC, sigla ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlAsig)) {
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
                                idPlan);
                        rep.totalCreditos += a.getCreditos();
                        rep.asignaturas.add(a);
                    }
                }
            }

            // 2. Capítulos Aprobados con contenido formal
            String sqlCaps = "SELECT c.id, c.numero, c.titulo, c.contenido, comp.nombre AS comp_nombre, " +
                    "COALESCE(STRING_AGG(u.nombre_completo, ', '), 'Comisión Curricular') AS redactores " +
                    "FROM capitulo c " +
                    "JOIN componente comp ON c.id_componente = comp.id " +
                    "LEFT JOIN asignacion_capitulo ac ON c.id = ac.id_capitulo " +
                    "LEFT JOIN usuario u ON ac.id_usuario = u.id " +
                    "WHERE comp.id_plan_estudio = ? AND c.id_estado = 4 " + // Estado 4 = Aprobado
                    "GROUP BY c.id, c.numero, c.titulo, c.contenido, comp.nombre, comp.numero " +
                    "ORDER BY comp.numero ASC, c.numero ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlCaps)) {
                ps.setInt(1, idPlan);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Capitulo cap = new Capitulo(
                                rs.getInt("id"),
                                rs.getInt("numero"),
                                rs.getString("titulo"),
                                rs.getString("contenido"),
                                0,
                                4,
                                null);
                        cap.setNombreComponente(rs.getString("comp_nombre"));
                        cap.setResponsables(rs.getString("redactores"));
                        rep.capitulosAprobados.add(cap);
                    }
                }
            }

            // 3. Documentos de respaldo y resoluciones vigentes
            String sqlDocs = "SELECT d.id, d.codigo_norma, d.titulo, d.tipo_documento, d.organo_emisor, " +
                    "d.fecha_emision, d.archivo_ruta, d.vigente, d.id_documento_padre, d.created_at AS fecha_registro " +
                    "FROM documento_evidencia d " +
                    "WHERE d.vigente = TRUE " +
                    "ORDER BY d.fecha_emision DESC";
            try (PreparedStatement ps = conn.prepareStatement(sqlDocs);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentoEvidencia doc = new DocumentoEvidencia(
                            rs.getInt("id"),
                            rs.getString("codigo_norma"),
                            rs.getString("titulo"),
                            rs.getString("tipo_documento"),
                            rs.getString("organo_emisor"),
                            rs.getDate("fecha_emision"),
                            rs.getString("archivo_ruta"),
                            rs.getBoolean("vigente"),
                            (Integer) rs.getObject("id_documento_padre"),
                            rs.getTimestamp("fecha_registro"));
                    rep.evidenciasVigentes.add(doc);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al compilar reporte final: " + e.getMessage());
        }

        return rep;
    }

    /**
     * Obtiene estadísticas de avance por componente curricular (CU8).
     */
    public List<EstadisticaComponente> obtenerEstadisticasPorComponente() {
        List<EstadisticaComponente> lista = new ArrayList<>();
        String sql = "SELECT comp.id, comp.numero, comp.nombre, " +
                "COUNT(c.id) AS total_capitulos, " +
                "COUNT(CASE WHEN c.id_estado = 4 THEN 1 END) AS aprobados " +
                "FROM componente comp " +
                "LEFT JOIN capitulo c ON comp.id = c.id_componente " +
                "GROUP BY comp.id, comp.numero, comp.nombre " +
                "ORDER BY comp.numero ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EstadisticaComponente e = new EstadisticaComponente();
                    e.idComponente = rs.getInt("id");
                    e.numero = rs.getInt("numero");
                    e.nombre = rs.getString("nombre");
                    e.totalCapitulos = rs.getInt("total_capitulos");
                    e.aprobados = rs.getInt("aprobados");
                    e.porcentaje = e.totalCapitulos > 0 ? ((double) e.aprobados / e.totalCapitulos) * 100.0 : 0.0;
                    lista.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estadísticas por componente: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene estadísticas de productividad de usuarios y redactores (CU8).
     */
    public List<EstadisticaUsuario> obtenerEstadisticasUsuarios() {
        List<EstadisticaUsuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nombre_completo, r.nombre AS rol_nombre, " +
                "COUNT(DISTINCT ac.id_capitulo) AS caps_asignados, " +
                "COUNT(DISTINCT obs_emitidas.id) AS obs_emitidas, " +
                "COUNT(DISTINCT obs_subsanadas.id) AS obs_subsanadas " +
                "FROM usuario u " +
                "JOIN rol r ON u.id_rol = r.id " +
                "LEFT JOIN asignacion_capitulo ac ON u.id = ac.id_usuario " +
                "LEFT JOIN observacion obs_emitidas ON u.id = obs_emitidas.id_usuario_observador " +
                "LEFT JOIN observacion obs_subsanadas ON u.id = obs_subsanadas.id_usuario_respuesta AND obs_subsanadas.resuelta = TRUE "
                +
                "WHERE u.activo = TRUE " +
                "GROUP BY u.id, u.nombre_completo, r.nombre " +
                "ORDER BY caps_asignados DESC, u.nombre_completo ASC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EstadisticaUsuario u = new EstadisticaUsuario();
                    u.idUsuario = rs.getInt("id");
                    u.nombreCompleto = rs.getString("nombre_completo");
                    u.rolNombre = rs.getString("rol_nombre");
                    u.capitulosAsignados = rs.getInt("caps_asignados");
                    u.observacionesEmitidas = rs.getInt("obs_emitidas");
                    u.observacionesSubsanadas = rs.getInt("obs_subsanadas");
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener estadísticas de usuarios: " + e.getMessage());
        }
        return lista;
    }
}

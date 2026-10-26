package com.curriculo.dao;

import com.curriculo.db.Conexion;
import com.curriculo.modelo.DocumentoEvidencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión del Repositorio Digital de Evidencias, Jerarquía Normativa y
 * Búsqueda Facetada (CU6, RF-11, RF-12, RF-13, RF-14).
 */
public class DocumentoEvidenciaDAO {

    public int insertar(DocumentoEvidencia d) {
        String sql = "INSERT INTO documento_evidencia(codigo_norma, titulo, tipo_documento, organo_emisor, " +
                "fecha_emision, archivo_ruta, vigente, id_documento_padre) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, d.getCodigoNorma());
                ps.setString(2, d.getTitulo());
                ps.setString(3, d.getTipoDocumento());
                ps.setString(4, d.getOrganoEmisor());
                ps.setDate(5, d.getFechaEmision());
                ps.setString(6, d.getArchivoRuta());
                ps.setBoolean(7, d.isVigente());
                if (d.getIdDocumentoPadre() != null && d.getIdDocumentoPadre() > 0) {
                    ps.setInt(8, d.getIdDocumentoPadre());
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }

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
            System.err.println("Error al insertar documento de evidencia: " + e.getMessage());
        }
        return -1;
    }

    public boolean actualizar(DocumentoEvidencia d) {
        String sql = "UPDATE documento_evidencia SET codigo_norma = ?, titulo = ?, tipo_documento = ?, " +
                "organo_emisor = ?, fecha_emision = ?, archivo_ruta = ?, vigente = ?, id_documento_padre = ? WHERE id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, d.getCodigoNorma());
                ps.setString(2, d.getTitulo());
                ps.setString(3, d.getTipoDocumento());
                ps.setString(4, d.getOrganoEmisor());
                ps.setDate(5, d.getFechaEmision());
                ps.setString(6, d.getArchivoRuta());
                ps.setBoolean(7, d.isVigente());
                if (d.getIdDocumentoPadre() != null && d.getIdDocumentoPadre() > 0) {
                    ps.setInt(8, d.getIdDocumentoPadre());
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }
                ps.setInt(9, d.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al actualizar documento de evidencia: " + e.getMessage());
            return false;
        }
    }

    public boolean cambiarVigencia(int id, boolean vigente) {
        String sql = "UPDATE documento_evidencia SET vigente = ? WHERE id = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, vigente);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al cambiar vigencia de documento: " + e.getMessage());
            return false;
        }
    }

    public List<DocumentoEvidencia> listarTodos() {
        List<DocumentoEvidencia> lista = new ArrayList<>();
        String sql = "SELECT d.id, d.codigo_norma, d.titulo, d.tipo_documento, d.organo_emisor, " +
                "d.fecha_emision, d.archivo_ruta, d.vigente, d.id_documento_padre, d.fecha_registro, " +
                "padre.codigo_norma AS padre_codigo, padre.titulo AS padre_titulo " +
                "FROM documento_evidencia d " +
                "LEFT JOIN documento_evidencia padre ON d.id_documento_padre = padre.id " +
                "ORDER BY d.fecha_emision DESC, d.id DESC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DocumentoEvidencia d = mapearDocumento(rs);
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar documentos de evidencia: " + e.getMessage());
        }
        return lista;
    }

    public DocumentoEvidencia obtenerPorId(int id) {
        String sql = "SELECT d.id, d.codigo_norma, d.titulo, d.tipo_documento, d.organo_emisor, " +
                "d.fecha_emision, d.archivo_ruta, d.vigente, d.id_documento_padre, d.fecha_registro, " +
                "padre.codigo_norma AS padre_codigo, padre.titulo AS padre_titulo " +
                "FROM documento_evidencia d " +
                "LEFT JOIN documento_evidencia padre ON d.id_documento_padre = padre.id " +
                "WHERE d.id = ?";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return null;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapearDocumento(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener documento por id: " + e.getMessage());
        }
        return null;
    }

    public boolean vincularACapitulo(int idCapitulo, int idDocumento) {
        String sql = "INSERT INTO evidencia_capitulo(id_capitulo, id_documento_evidencia) VALUES(?, ?) " +
                "ON CONFLICT (id_capitulo, id_documento_evidencia) DO NOTHING";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                ps.setInt(2, idDocumento);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al vincular evidencia a capítulo: " + e.getMessage());
            return false;
        }
    }

    public boolean desvincularDeCapitulo(int idCapitulo, int idDocumento) {
        String sql = "DELETE FROM evidencia_capitulo WHERE id_capitulo = ? AND id_documento_evidencia = ?";
        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                ps.setInt(2, idDocumento);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al desvincular evidencia de capítulo: " + e.getMessage());
            return false;
        }
    }

    public List<DocumentoEvidencia> listarEvidenciasDeCapitulo(int idCapitulo) {
        List<DocumentoEvidencia> lista = new ArrayList<>();
        String sql = "SELECT d.id, d.codigo_norma, d.titulo, d.tipo_documento, d.organo_emisor, " +
                "d.fecha_emision, d.archivo_ruta, d.vigente, d.id_documento_padre, d.fecha_registro, " +
                "padre.codigo_norma AS padre_codigo, padre.titulo AS padre_titulo " +
                "FROM evidencia_capitulo ec " +
                "JOIN documento_evidencia d ON ec.id_documento_evidencia = d.id " +
                "LEFT JOIN documento_evidencia padre ON d.id_documento_padre = padre.id " +
                "WHERE ec.id_capitulo = ? " +
                "ORDER BY d.fecha_emision DESC";

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCapitulo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearDocumento(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar evidencias del capítulo: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Motor de Búsqueda Facetada (RF-14).
     * Permite filtrar por tipo de documento, emisor institucional, código de norma,
     * rango de fechas o capítulo asignado.
     */
    public List<DocumentoEvidencia> busquedaFacetada(String criterio, String valor) {
        List<DocumentoEvidencia> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT d.id, d.codigo_norma, d.titulo, d.tipo_documento, d.organo_emisor, " +
                        "d.fecha_emision, d.archivo_ruta, d.vigente, d.id_documento_padre, d.fecha_registro, " +
                        "padre.codigo_norma AS padre_codigo, padre.titulo AS padre_titulo " +
                        "FROM documento_evidencia d " +
                        "LEFT JOIN documento_evidencia padre ON d.id_documento_padre = padre.id " +
                        "LEFT JOIN evidencia_capitulo ec ON d.id = ec.id_documento_evidencia " +
                        "WHERE 1=1 ");

        String crit = criterio != null ? criterio.trim().toUpperCase() : "";

        if ("TIPO".equals(crit)) {
            sql.append("AND LOWER(d.tipo_documento) LIKE LOWER(?) ");
        } else if ("EMISOR".equals(crit)) {
            sql.append("AND LOWER(d.organo_emisor) LIKE LOWER(?) ");
        } else if ("CODIGO".equals(crit)) {
            sql.append("AND LOWER(d.codigo_norma) LIKE LOWER(?) ");
        } else if ("CAPITULO".equals(crit)) {
            sql.append("AND ec.id_capitulo = ? ");
        } else if ("VIGENTE".equals(crit)) {
            sql.append("AND d.vigente = ? ");
        } else {
            // Búsqueda libre en código, título u emisor
            sql.append(
                    "AND (LOWER(d.codigo_norma) LIKE LOWER(?) OR LOWER(d.titulo) LIKE LOWER(?) OR LOWER(d.organo_emisor) LIKE LOWER(?)) ");
        }

        sql.append("ORDER BY d.fecha_emision DESC");

        try (Connection conn = Conexion.getConnection()) {
            if (conn == null)
                return lista;
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                String likeVal = "%" + valor.trim() + "%";

                if ("TIPO".equals(crit) || "EMISOR".equals(crit) || "CODIGO".equals(crit)) {
                    ps.setString(1, likeVal);
                } else if ("CAPITULO".equals(crit)) {
                    ps.setInt(1, Integer.parseInt(valor.trim()));
                } else if ("VIGENTE".equals(crit)) {
                    ps.setBoolean(1, Boolean.parseBoolean(valor.trim()));
                } else {
                    ps.setString(1, likeVal);
                    ps.setString(2, likeVal);
                    ps.setString(3, likeVal);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearDocumento(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda facetada: " + e.getMessage());
        }
        return lista;
    }

    private DocumentoEvidencia mapearDocumento(ResultSet rs) throws SQLException {
        DocumentoEvidencia d = new DocumentoEvidencia(
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
        d.setCodigoNormaPadre(rs.getString("padre_codigo"));
        d.setTituloNormaPadre(rs.getString("padre_titulo"));
        return d;
    }
}

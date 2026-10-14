package com.curriculo.modelo;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Entidad que representa un Documento Digital de Respaldo o Resolución Normativa (CU6, RF-11, RF-12).
 * Soporta jerarquía normativa autorreferencial mediante idDocumentoPadre.
 */
public class DocumentoEvidencia {
    private int id;
    private String codigoNorma;
    private String titulo;
    private String tipoDocumento;
    private String organoEmisor;
    private Date fechaEmision;
    private String archivoRuta;
    private boolean vigente;
    private Integer idDocumentoPadre;
    private Timestamp fechaRegistro;

    // Campos auxiliares para visualización
    private String codigoNormaPadre;
    private String tituloNormaPadre;

    public DocumentoEvidencia() {
        this.vigente = true;
    }

    public DocumentoEvidencia(String codigoNorma, String titulo, String tipoDocumento, String organoEmisor, Date fechaEmision, String archivoRuta, Integer idDocumentoPadre) {
        this.codigoNorma = codigoNorma;
        this.titulo = titulo;
        this.tipoDocumento = tipoDocumento;
        this.organoEmisor = organoEmisor;
        this.fechaEmision = fechaEmision;
        this.archivoRuta = archivoRuta;
        this.vigente = true;
        this.idDocumentoPadre = idDocumentoPadre;
    }

    public DocumentoEvidencia(int id, String codigoNorma, String titulo, String tipoDocumento, String organoEmisor, Date fechaEmision, String archivoRuta, boolean vigente, Integer idDocumentoPadre, Timestamp fechaRegistro) {
        this.id = id;
        this.codigoNorma = codigoNorma;
        this.titulo = titulo;
        this.tipoDocumento = tipoDocumento;
        this.organoEmisor = organoEmisor;
        this.fechaEmision = fechaEmision;
        this.archivoRuta = archivoRuta;
        this.vigente = vigente;
        this.idDocumentoPadre = idDocumentoPadre;
        this.fechaRegistro = fechaRegistro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigoNorma() {
        return codigoNorma;
    }

    public void setCodigoNorma(String codigoNorma) {
        this.codigoNorma = codigoNorma;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getOrganoEmisor() {
        return organoEmisor;
    }

    public void setOrganoEmisor(String organoEmisor) {
        this.organoEmisor = organoEmisor;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getArchivoRuta() {
        return archivoRuta;
    }

    public void setArchivoRuta(String archivoRuta) {
        this.archivoRuta = archivoRuta;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }

    public Integer getIdDocumentoPadre() {
        return idDocumentoPadre;
    }

    public void setIdDocumentoPadre(Integer idDocumentoPadre) {
        this.idDocumentoPadre = idDocumentoPadre;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getCodigoNormaPadre() {
        return codigoNormaPadre;
    }

    public void setCodigoNormaPadre(String codigoNormaPadre) {
        this.codigoNormaPadre = codigoNormaPadre;
    }

    public String getTituloNormaPadre() {
        return tituloNormaPadre;
    }

    public void setTituloNormaPadre(String tituloNormaPadre) {
        this.tituloNormaPadre = tituloNormaPadre;
    }

    @Override
    public String toString() {
        return "[" + codigoNorma + "] " + titulo + " (" + organoEmisor + ", " + fechaEmision + ")";
    }
}

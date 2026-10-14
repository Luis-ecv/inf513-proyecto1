package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que vincula formalmente un documento de evidencia a un capítulo (CU6, RF-13).
 */
public class EvidenciaCapitulo {
    private int idCapitulo;
    private int idDocumentoEvidencia;
    private Timestamp fechaVinculacion;

    // Campos auxiliares para visualización
    private String tituloCapitulo;
    private String codigoNorma;
    private String tituloDocumento;
    private String archivoRuta;

    public EvidenciaCapitulo() {
    }

    public EvidenciaCapitulo(int idCapitulo, int idDocumentoEvidencia) {
        this.idCapitulo = idCapitulo;
        this.idDocumentoEvidencia = idDocumentoEvidencia;
    }

    public EvidenciaCapitulo(int idCapitulo, int idDocumentoEvidencia, Timestamp fechaVinculacion, String tituloCapitulo, String codigoNorma, String tituloDocumento, String archivoRuta) {
        this.idCapitulo = idCapitulo;
        this.idDocumentoEvidencia = idDocumentoEvidencia;
        this.fechaVinculacion = fechaVinculacion;
        this.tituloCapitulo = tituloCapitulo;
        this.codigoNorma = codigoNorma;
        this.tituloDocumento = tituloDocumento;
        this.archivoRuta = archivoRuta;
    }

    public int getIdCapitulo() {
        return idCapitulo;
    }

    public void setIdCapitulo(int idCapitulo) {
        this.idCapitulo = idCapitulo;
    }

    public int getIdDocumentoEvidencia() {
        return idDocumentoEvidencia;
    }

    public void setIdDocumentoEvidencia(int idDocumentoEvidencia) {
        this.idDocumentoEvidencia = idDocumentoEvidencia;
    }

    public Timestamp getFechaVinculacion() {
        return fechaVinculacion;
    }

    public void setFechaVinculacion(Timestamp fechaVinculacion) {
        this.fechaVinculacion = fechaVinculacion;
    }

    public String getTituloCapitulo() {
        return tituloCapitulo;
    }

    public void setTituloCapitulo(String tituloCapitulo) {
        this.tituloCapitulo = tituloCapitulo;
    }

    public String getCodigoNorma() {
        return codigoNorma;
    }

    public void setCodigoNorma(String codigoNorma) {
        this.codigoNorma = codigoNorma;
    }

    public String getTituloDocumento() {
        return tituloDocumento;
    }

    public void setTituloDocumento(String tituloDocumento) {
        this.tituloDocumento = tituloDocumento;
    }

    public String getArchivoRuta() {
        return archivoRuta;
    }

    public void setArchivoRuta(String archivoRuta) {
        this.archivoRuta = archivoRuta;
    }

    @Override
    public String toString() {
        return "Capítulo #" + idCapitulo + " <-> Evidencia [" + codigoNorma + "] " + tituloDocumento;
    }
}

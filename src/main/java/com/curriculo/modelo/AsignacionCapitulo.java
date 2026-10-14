package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que vincula a un Usuario responsable con la redacción de un Capítulo (CU4, RF-05).
 */
public class AsignacionCapitulo {
    private int idCapitulo;
    private int idUsuario;
    private Timestamp fechaAsignacion;

    // Campos auxiliares para visualización
    private String tituloCapitulo;
    private String nombreUsuario;
    private String emailUsuario;

    public AsignacionCapitulo() {
    }

    public AsignacionCapitulo(int idCapitulo, int idUsuario) {
        this.idCapitulo = idCapitulo;
        this.idUsuario = idUsuario;
    }

    public AsignacionCapitulo(int idCapitulo, int idUsuario, Timestamp fechaAsignacion, String tituloCapitulo, String nombreUsuario, String emailUsuario) {
        this.idCapitulo = idCapitulo;
        this.idUsuario = idUsuario;
        this.fechaAsignacion = fechaAsignacion;
        this.tituloCapitulo = tituloCapitulo;
        this.nombreUsuario = nombreUsuario;
        this.emailUsuario = emailUsuario;
    }

    public int getIdCapitulo() {
        return idCapitulo;
    }

    public void setIdCapitulo(int idCapitulo) {
        this.idCapitulo = idCapitulo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Timestamp getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(Timestamp fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public String getTituloCapitulo() {
        return tituloCapitulo;
    }

    public void setTituloCapitulo(String tituloCapitulo) {
        this.tituloCapitulo = tituloCapitulo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    @Override
    public String toString() {
        return "Capítulo ID: " + idCapitulo + " -> " + nombreUsuario + " (" + emailUsuario + ")";
    }
}

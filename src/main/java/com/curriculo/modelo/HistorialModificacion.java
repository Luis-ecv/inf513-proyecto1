package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad para la pista de auditoría inmutable de transiciones de estado (CU5, RF-10).
 * Registra fecha, hora, estado anterior, estado nuevo, usuario responsable y justificación técnica.
 */
public class HistorialModificacion {
    private int id;
    private String estadoAnterior;
    private String estadoNuevo;
    private String descripcionAccion;
    private String justificacionTecnica;
    private Timestamp fechaHora;
    private int idCapitulo;
    private int idUsuario;

    // Campos auxiliares para visualización
    private String nombreUsuario;
    private String tituloCapitulo;

    public HistorialModificacion() {
    }

    public HistorialModificacion(String estadoAnterior, String estadoNuevo, String descripcionAccion, String justificacionTecnica, int idCapitulo, int idUsuario) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.descripcionAccion = descripcionAccion;
        this.justificacionTecnica = justificacionTecnica;
        this.idCapitulo = idCapitulo;
        this.idUsuario = idUsuario;
    }

    public HistorialModificacion(int id, String estadoAnterior, String estadoNuevo, String descripcionAccion, String justificacionTecnica, Timestamp fechaHora, int idCapitulo, int idUsuario) {
        this.id = id;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.descripcionAccion = descripcionAccion;
        this.justificacionTecnica = justificacionTecnica;
        this.fechaHora = fechaHora;
        this.idCapitulo = idCapitulo;
        this.idUsuario = idUsuario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getDescripcionAccion() {
        return descripcionAccion;
    }

    public void setDescripcionAccion(String descripcionAccion) {
        this.descripcionAccion = descripcionAccion;
    }

    public String getJustificacionTecnica() {
        return justificacionTecnica;
    }

    public void setJustificacionTecnica(String justificacionTecnica) {
        this.justificacionTecnica = justificacionTecnica;
    }

    public Timestamp getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Timestamp fechaHora) {
        this.fechaHora = fechaHora;
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

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getTituloCapitulo() {
        return tituloCapitulo;
    }

    public void setTituloCapitulo(String tituloCapitulo) {
        this.tituloCapitulo = tituloCapitulo;
    }

    @Override
    public String toString() {
        return "[" + fechaHora + "] " + estadoAnterior + " -> " + estadoNuevo + " (" + justificacionTecnica + ")";
    }
}

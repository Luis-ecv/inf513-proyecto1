package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que representa un señalamiento u observación técnica sobre un capítulo (CU5, RF-08).
 * Contempla la doble autoría: la autoridad que observa y el delegado que responde la subsanación.
 */
public class Observacion {
    private int id;
    private String comentario;
    private Timestamp fechaCreacion;
    private String respuestaDelegado;
    private Timestamp fechaRespuesta;
    private boolean resuelta;
    private Timestamp fechaCierre;
    private int idCapitulo;
    private int idUsuarioObservador;
    private Integer idUsuarioRespuesta;

    // Campos auxiliares para visualización
    private String tituloCapitulo;
    private String nombreObservador;
    private String nombreRespuesta;

    public Observacion() {
        this.resuelta = false;
    }

    public Observacion(String comentario, int idCapitulo, int idUsuarioObservador) {
        this.comentario = comentario;
        this.idCapitulo = idCapitulo;
        this.idUsuarioObservador = idUsuarioObservador;
        this.resuelta = false;
    }

    public Observacion(int id, String comentario, Timestamp fechaCreacion, String respuestaDelegado, Timestamp fechaRespuesta, boolean resuelta, Timestamp fechaCierre, int idCapitulo, int idUsuarioObservador, Integer idUsuarioRespuesta) {
        this.id = id;
        this.comentario = comentario;
        this.fechaCreacion = fechaCreacion;
        this.respuestaDelegado = respuestaDelegado;
        this.fechaRespuesta = fechaRespuesta;
        this.resuelta = resuelta;
        this.fechaCierre = fechaCierre;
        this.idCapitulo = idCapitulo;
        this.idUsuarioObservador = idUsuarioObservador;
        this.idUsuarioRespuesta = idUsuarioRespuesta;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getRespuestaDelegado() {
        return respuestaDelegado;
    }

    public void setRespuestaDelegado(String respuestaDelegado) {
        this.respuestaDelegado = respuestaDelegado;
    }

    public Timestamp getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(Timestamp fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public boolean isResuelta() {
        return resuelta;
    }

    public void setResuelta(boolean resuelta) {
        this.resuelta = resuelta;
    }

    public Timestamp getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(Timestamp fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public int getIdCapitulo() {
        return idCapitulo;
    }

    public void setIdCapitulo(int idCapitulo) {
        this.idCapitulo = idCapitulo;
    }

    public int getIdUsuarioObservador() {
        return idUsuarioObservador;
    }

    public void setIdUsuarioObservador(int idUsuarioObservador) {
        this.idUsuarioObservador = idUsuarioObservador;
    }

    public Integer getIdUsuarioRespuesta() {
        return idUsuarioRespuesta;
    }

    public void setIdUsuarioRespuesta(Integer idUsuarioRespuesta) {
        this.idUsuarioRespuesta = idUsuarioRespuesta;
    }

    public String getTituloCapitulo() {
        return tituloCapitulo;
    }

    public void setTituloCapitulo(String tituloCapitulo) {
        this.tituloCapitulo = tituloCapitulo;
    }

    public String getNombreObservador() {
        return nombreObservador;
    }

    public void setNombreObservador(String nombreObservador) {
        this.nombreObservador = nombreObservador;
    }

    public String getNombreRespuesta() {
        return nombreRespuesta;
    }

    public void setNombreRespuesta(String nombreRespuesta) {
        this.nombreRespuesta = nombreRespuesta;
    }

    @Override
    public String toString() {
        return "Obs #" + id + " [" + (resuelta ? "Resuelta" : "Pendiente") + "]: " + comentario;
    }
}

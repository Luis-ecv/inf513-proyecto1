package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que representa un Capítulo del rediseño curricular (CU4, RF-05).
 * Vinculado a un componente y sujeto a la máquina de estados.
 */
public class Capitulo {
    private int id;
    private int numero;
    private String titulo;
    private String contenido;
    private int idComponente;
    private int idEstado;
    private Timestamp fechaCreacion;

    // Campos auxiliares para visualización
    private String nombreComponente;
    private String nombreEstado;
    private String responsables; // Nombres de los redactores asignados separados por coma

    public Capitulo() {
        this.idEstado = 1; // Por defecto: 'Borrador'
    }

    public Capitulo(int numero, String titulo, String contenido, int idComponente) {
        this.numero = numero;
        this.titulo = titulo;
        this.contenido = contenido;
        this.idComponente = idComponente;
        this.idEstado = 1; // Estado inicial 'Borrador'
    }

    public Capitulo(int id, int numero, String titulo, String contenido, int idComponente, int idEstado, Timestamp fechaCreacion) {
        this.id = id;
        this.numero = numero;
        this.titulo = titulo;
        this.contenido = contenido;
        this.idComponente = idComponente;
        this.idEstado = idEstado;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public int getIdComponente() {
        return idComponente;
    }

    public void setIdComponente(int idComponente) {
        this.idComponente = idComponente;
    }

    public int getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(int idEstado) {
        this.idEstado = idEstado;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getNombreComponente() {
        return nombreComponente;
    }

    public void setNombreComponente(String nombreComponente) {
        this.nombreComponente = nombreComponente;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }

    public String getResponsables() {
        return responsables;
    }

    public void setResponsables(String responsables) {
        this.responsables = responsables;
    }

    @Override
    public String toString() {
        return "Capítulo " + numero + ": " + titulo + " [" + (nombreEstado != null ? nombreEstado : idEstado) + "]";
    }
}

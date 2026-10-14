package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que representa un Plan de Estudios (Malla Curricular).
 * Incorpora los campos exigidos por RF-01: objetivos, perfil de egreso y límites de créditos.
 */
public class PlanEstudio {
    private int id;
    private String codigo;
    private String carrera;
    private int anioVigencia;
    private String descripcion;
    private String objetivos;
    private String perfilEgreso;
    private int limiteCreditosMin;
    private int limiteCreditosMax;
    private String estado;
    private Timestamp fechaCreacion;

    public PlanEstudio() {
        this.estado = "En Elaboración";
        this.limiteCreditosMin = 200;
        this.limiteCreditosMax = 280;
    }

    public PlanEstudio(String codigo, String carrera, int anioVigencia, String descripcion, String objetivos, String perfilEgreso, int limiteCreditosMin, int limiteCreditosMax) {
        this.codigo = codigo;
        this.carrera = carrera;
        this.anioVigencia = anioVigencia;
        this.descripcion = descripcion;
        this.objetivos = objetivos;
        this.perfilEgreso = perfilEgreso;
        this.limiteCreditosMin = limiteCreditosMin;
        this.limiteCreditosMax = limiteCreditosMax;
        this.estado = "En Elaboración";
    }

    public PlanEstudio(int id, String codigo, String carrera, int anioVigencia, String descripcion, String objetivos, String perfilEgreso, int limiteCreditosMin, int limiteCreditosMax, String estado, Timestamp fechaCreacion) {
        this.id = id;
        this.codigo = codigo;
        this.carrera = carrera;
        this.anioVigencia = anioVigencia;
        this.descripcion = descripcion;
        this.objetivos = objetivos;
        this.perfilEgreso = perfilEgreso;
        this.limiteCreditosMin = limiteCreditosMin;
        this.limiteCreditosMax = limiteCreditosMax;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public int getAnioVigencia() {
        return anioVigencia;
    }

    public void setAnioVigencia(int anioVigencia) {
        this.anioVigencia = anioVigencia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getObjetivos() {
        return objetivos;
    }

    public void setObjetivos(String objetivos) {
        this.objetivos = objetivos;
    }

    public String getPerfilEgreso() {
        return perfilEgreso;
    }

    public void setPerfilEgreso(String perfilEgreso) {
        this.perfilEgreso = perfilEgreso;
    }

    public int getLimiteCreditosMin() {
        return limiteCreditosMin;
    }

    public void setLimiteCreditosMin(int limiteCreditosMin) {
        this.limiteCreditosMin = limiteCreditosMin;
    }

    public int getLimiteCreditosMax() {
        return limiteCreditosMax;
    }

    public void setLimiteCreditosMax(int limiteCreditosMax) {
        this.limiteCreditosMax = limiteCreditosMax;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "Plan [" + codigo + "] " + carrera + " (" + anioVigencia + ") - Estado: " + estado;
    }
}

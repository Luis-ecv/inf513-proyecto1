package com.curriculo.modelo;

/**
 * Entidad que representa un Componente del marco de rediseño curricular (CU4, RF-05).
 * Agrupa capítulos correlativos temáticos dentro de un Plan de Estudios.
 */
public class Componente {
    private int id;
    private int numero;
    private String nombre;
    private String descripcion;
    private int idPlanEstudio;

    public Componente() {
    }

    public Componente(int numero, String nombre, String descripcion, int idPlanEstudio) {
        this.numero = numero;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.idPlanEstudio = idPlanEstudio;
    }

    public Componente(int id, int numero, String nombre, String descripcion, int idPlanEstudio) {
        this.id = id;
        this.numero = numero;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.idPlanEstudio = idPlanEstudio;
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getIdPlanEstudio() {
        return idPlanEstudio;
    }

    public void setIdPlanEstudio(int idPlanEstudio) {
        this.idPlanEstudio = idPlanEstudio;
    }

    @Override
    public String toString() {
        return "Componente " + numero + ": " + nombre;
    }
}

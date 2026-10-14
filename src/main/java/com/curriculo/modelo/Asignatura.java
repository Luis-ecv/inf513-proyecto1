package com.curriculo.modelo;

/**
 * Entidad que representa una Asignatura/Materia perteneciente a un Plan de Estudios.
 * Cumple con RF-02 (sigla, nombre, semestre, horas teóricas, horas prácticas, créditos y área).
 */
public class Asignatura {
    private int id;
    private String sigla;
    private String nombre;
    private int semestre;
    private int horasTeoricas;
    private int horasPracticas;
    private int creditos;
    private String areaFormativa;
    private int idPlanEstudio;

    public Asignatura() {
    }

    public Asignatura(String sigla, String nombre, int semestre, int horasTeoricas, int horasPracticas, int creditos, String areaFormativa, int idPlanEstudio) {
        this.sigla = sigla;
        this.nombre = nombre;
        this.semestre = semestre;
        this.horasTeoricas = horasTeoricas;
        this.horasPracticas = horasPracticas;
        this.creditos = creditos;
        this.areaFormativa = areaFormativa;
        this.idPlanEstudio = idPlanEstudio;
    }

    public Asignatura(int id, String sigla, String nombre, int semestre, int horasTeoricas, int horasPracticas, int creditos, String areaFormativa, int idPlanEstudio) {
        this.id = id;
        this.sigla = sigla;
        this.nombre = nombre;
        this.semestre = semestre;
        this.horasTeoricas = horasTeoricas;
        this.horasPracticas = horasPracticas;
        this.creditos = creditos;
        this.areaFormativa = areaFormativa;
        this.idPlanEstudio = idPlanEstudio;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getSemestre() {
        return semestre;
    }

    public void setSemestre(int semestre) {
        this.semestre = semestre;
    }

    public int getHorasTeoricas() {
        return horasTeoricas;
    }

    public void setHorasTeoricas(int horasTeoricas) {
        this.horasTeoricas = horasTeoricas;
    }

    public int getHorasPracticas() {
        return horasPracticas;
    }

    public void setHorasPracticas(int horasPracticas) {
        this.horasPracticas = horasPracticas;
    }

    public int getCreditos() {
        return creditos;
    }

    public void setCreditos(int creditos) {
        this.creditos = creditos;
    }

    public String getAreaFormativa() {
        return areaFormativa;
    }

    public void setAreaFormativa(String areaFormativa) {
        this.areaFormativa = areaFormativa;
    }

    public int getIdPlanEstudio() {
        return idPlanEstudio;
    }

    public void setIdPlanEstudio(int idPlanEstudio) {
        this.idPlanEstudio = idPlanEstudio;
    }

    @Override
    public String toString() {
        return "[" + sigla + "] " + nombre + " (Sem: " + semestre + ", Cr: " + creditos + ", HT: " + horasTeoricas + ", HP: " + horasPracticas + ")";
    }
}

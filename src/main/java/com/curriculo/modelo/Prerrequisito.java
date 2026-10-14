package com.curriculo.modelo;

/**
 * Entidad que representa la relación de prelación entre asignaturas (CU3, RF-03).
 */
public class Prerrequisito {
    private int idAsignatura;
    private int idAsignaturaRequisito;

    // Campos auxiliares para visualización
    private String siglaAsignatura;
    private String nombreAsignatura;
    private int semestreAsignatura;

    private String siglaRequisito;
    private String nombreRequisito;
    private int semestreRequisito;

    public Prerrequisito() {
    }

    public Prerrequisito(int idAsignatura, int idAsignaturaRequisito) {
        this.idAsignatura = idAsignatura;
        this.idAsignaturaRequisito = idAsignaturaRequisito;
    }

    public Prerrequisito(int idAsignatura, int idAsignaturaRequisito, String siglaAsignatura, String nombreAsignatura, int semestreAsignatura, String siglaRequisito, String nombreRequisito, int semestreRequisito) {
        this.idAsignatura = idAsignatura;
        this.idAsignaturaRequisito = idAsignaturaRequisito;
        this.siglaAsignatura = siglaAsignatura;
        this.nombreAsignatura = nombreAsignatura;
        this.semestreAsignatura = semestreAsignatura;
        this.siglaRequisito = siglaRequisito;
        this.nombreRequisito = nombreRequisito;
        this.semestreRequisito = semestreRequisito;
    }

    public int getIdAsignatura() {
        return idAsignatura;
    }

    public void setIdAsignatura(int idAsignatura) {
        this.idAsignatura = idAsignatura;
    }

    public int getIdAsignaturaRequisito() {
        return idAsignaturaRequisito;
    }

    public void setIdAsignaturaRequisito(int idAsignaturaRequisito) {
        this.idAsignaturaRequisito = idAsignaturaRequisito;
    }

    public String getSiglaAsignatura() {
        return siglaAsignatura;
    }

    public void setSiglaAsignatura(String siglaAsignatura) {
        this.siglaAsignatura = siglaAsignatura;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public void setNombreAsignatura(String nombreAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
    }

    public int getSemestreAsignatura() {
        return semestreAsignatura;
    }

    public void setSemestreAsignatura(int semestreAsignatura) {
        this.semestreAsignatura = semestreAsignatura;
    }

    public String getSiglaRequisito() {
        return siglaRequisito;
    }

    public void setSiglaRequisito(String siglaRequisito) {
        this.siglaRequisito = siglaRequisito;
    }

    public String getNombreRequisito() {
        return nombreRequisito;
    }

    public void setNombreRequisito(String nombreRequisito) {
        this.nombreRequisito = nombreRequisito;
    }

    public int getSemestreRequisito() {
        return semestreRequisito;
    }

    public void setSemestreRequisito(int semestreRequisito) {
        this.semestreRequisito = semestreRequisito;
    }

    @Override
    public String toString() {
        return "[" + siglaAsignatura + "] requiere [" + siglaRequisito + "]";
    }
}

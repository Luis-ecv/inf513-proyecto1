package com.curriculo.modelo;

import java.sql.Timestamp;

/**
 * Entidad que representa un Usuario (Director, Delegado, Invitado) con soporte para foto y rol.
 */
public class Usuario {
    private int id;
    private String nombreCompleto;
    private String email;
    private String username;
    private String passwordHash;
    private String fotoRuta;
    private boolean activo;
    private Timestamp fechaRegistro;
    private int idRol;
    private String nombreRol;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(String nombreCompleto, String email, String username, String passwordHash, String fotoRuta, int idRol) {
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fotoRuta = fotoRuta;
        this.activo = true;
        this.idRol = idRol;
    }

    public Usuario(int id, String nombreCompleto, String email, String username, String passwordHash, String fotoRuta, boolean activo, Timestamp fechaRegistro, int idRol, String nombreRol) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fotoRuta = fotoRuta;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
        this.idRol = idRol;
        this.nombreRol = nombreRol;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFotoRuta() {
        return fotoRuta;
    }

    public void setFotoRuta(String fotoRuta) {
        this.fotoRuta = fotoRuta;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    @Override
    public String toString() {
        return "ID: " + id + " | " + nombreCompleto + " (" + email + ") | Rol: " + (nombreRol != null ? nombreRol : idRol) + " | Estado: " + (activo ? "Activo" : "Inactivo");
    }
}

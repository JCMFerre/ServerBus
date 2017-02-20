package com.reskitow.serverbus.Model;

public class Autobus {

    private String matricula;
    private String contrasena;
    private boolean activo;

    public Autobus(String matricula, String contrasena, boolean activo) {
        this.matricula = matricula;
        this.contrasena = contrasena;
        this.activo = activo;
    }

    public Autobus() {
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Autobus{" + "matricula=" + matricula + ", contrasena=" + contrasena + ", activo=" + activo + '}';
    }
}

package com.reskitow.serverbus.Model;

public class Ruta {

    private String idSesion;
    private String matricula;
    private String fecha;
    private double latitud;
    private double longitud;

    public Ruta(String idSesion, String matricula, String fecha, double latitud, double longitud) {
        this.idSesion = idSesion;
        this.matricula = matricula;
        this.fecha = fecha;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public Ruta() {
    }

    public String getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(String idSesion) {
        this.idSesion = idSesion;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    @Override
    public String toString() {
        return "Ruta{" + "idSesion=" + idSesion + ", matricula=" + matricula + ", fecha=" + fecha + ", latitud=" + latitud + ", longitud=" + longitud + '}';
    }
}

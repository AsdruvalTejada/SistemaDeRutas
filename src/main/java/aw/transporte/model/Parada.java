package aw.transporte.model;
import java.util.Objects;

public class Parada {
    private String id;
    private String nombre;
    private double coorx;
    private double coory;

    public Parada() {
    }

    public Parada(String id, String nombre, double x, double y) {
        this.id = id;
        this.nombre = nombre;
        this.coorx = x;
        this.coory = y;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getCoorx() {
        return coorx;
    }

    public double getCoory() {
        return coory;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCoorx(double coorx) {
        this.coorx = coorx;
    }

    public void setCoory(double coory) {
        this.coory = coory;
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parada parada = (Parada) o;
        return Objects.equals(id, parada.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

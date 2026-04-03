package aw.transporte.model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parada {
    private String id;
    private String nombre;
    private double coorx;
    private double coory;

    /**
     * Función: (Constructor) Parada
     * Objetivo: Inicializar una nueva parada con sus datos básicos.
     * @param id     (String) Identificador único (ej. P1).
     * @param nombre (String) Nombre legible de la parada.
     * @param x      (double) Posición horizontal en el plano.
     * @param y      (double) Posición vertical en el plano.
     */
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

    //Para hacer pruebas imprimiendo en la consola la parada
    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }

    /**
     * Función: equals
     * Objetivo: Comparar si dos objetos Parada son iguales basándose en su ID único.
     */
    // Para comparar dos paradas a ver si son las mismas usando su ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parada parada = (Parada) o;
        return Objects.equals(id, parada.id);
    }

    // Genera una clave numérica única basada en el 'id' esto servirá para que el Grafo y los algoritmos de búsqueda
    // puedan buscar y acceder a esta parada en las distintas colecciones (como HashMaps o HashSets) y
    // llevar un registro de las paradas que ya visité
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

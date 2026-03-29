package aw.transporte.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Ruta {
    private String idOrigen;
    private String idDestino;
    private String nombreLinea; // Para los Transbordos
    private Map<CriterioPesos, Double> pesos;

    public Ruta(String idOrigen, String idDestino, String nombreLinea, double tiempo, double distancia, double costo) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.nombreLinea = nombreLinea;
        this.pesos = new HashMap<>();
        this.pesos.put(CriterioPesos.TIEMPO, tiempo);
        this.pesos.put(CriterioPesos.DISTANCIA, distancia);
        this.pesos.put(CriterioPesos.COSTO, costo);
        this.pesos.put(CriterioPesos.TRANSBORDOS, 1.0);
    }

    public String getIdOrigen() { return idOrigen; }
    public String getIdDestino() { return idDestino; }
    public String getNombreLinea() { return nombreLinea; }
    public Map<CriterioPesos, Double> getPesos() { return pesos; }

    public double getValorPeso(CriterioPesos criterio) {
        return pesos.getOrDefault(criterio, 0.0);
    }

    // --- IMPLEMENTACIONES PARA EL HASHSET ---
    // Dos rutas son iguales si van al mismo destino en la misma línea
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ruta ruta = (Ruta) o;
        return Objects.equals(idDestino, ruta.idDestino) && Objects.equals(nombreLinea, ruta.nombreLinea);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDestino, nombreLinea);
    }
}
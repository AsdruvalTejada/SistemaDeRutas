package aw.transporte.model;

import java.util.HashMap;
import java.util.Map;

public class Ruta {
    private String idOrigen;
    private String idDestino;
    private Map<CriterioPesos, Double> pesos;


    public Ruta(String idOrigen, String idDestino, double tiempo, double distancia, double costo) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.pesos = new HashMap<>();
        this.pesos.put(CriterioPesos.TIEMPO, tiempo);
        this.pesos.put(CriterioPesos.DISTANCIA, distancia);
        this.pesos.put(CriterioPesos.COSTO, costo);
        this.pesos.put(CriterioPesos.TRANSBORDOS, 0.0);
    }

    // Getters necesarios para el Grafo y el AppController
    public String getIdOrigen() { return idOrigen; }
    public String getIdDestino() { return idDestino; }

    public Map<CriterioPesos, Double> getPesos() {
        return pesos;
    }

    public double getValorPeso(CriterioPesos criterio) {
        return pesos.getOrDefault(criterio, 0.0);
    }

}
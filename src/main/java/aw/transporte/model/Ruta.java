package aw.transporte.model;

import java.util.HashMap;
import java.util.Map;

public class Ruta {
    private String idOrigen;
    private String idDestino;

    private Map<CriterioPesos, Double> pesos;

    public Ruta(String idOrigen, String idDestino, double tiempo, double distancia, double costo, int transbordos) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.pesos = new HashMap<>();
        this.pesos.put(CriterioPesos.TIEMPO, tiempo);
        this.pesos.put(CriterioPesos.DISTANCIA, distancia);
        this.pesos.put(CriterioPesos.COSTO, costo);
        this.pesos.put(CriterioPesos.TRANSBORDOS, (double) transbordos);
    }

    public String getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(String idOrigen) {
        this.idOrigen = idOrigen;
    }

    public String getIdDestino() {
        return idDestino;
    }

    public void setIdDestino(String idDestino) {
        this.idDestino = idDestino;
    }

    public Map<CriterioPesos, Double> getPesos() {
        return pesos;
    }

    public void setPesos(Map<CriterioPesos, Double> pesos) {
        this.pesos = pesos;
    }

    public double getValorPeso(CriterioPesos criterio) {
        // Esté método retorna el peso, y si por algún error no existe, retorna infinito para que Dijkstra lo ignore
        return pesos.getOrDefault(criterio, Double.MAX_VALUE);
    }

    // Método para modificar un peso específico si lo necesitan después
    public void setValorPeso(CriterioPesos criterio, double valor) {
        this.pesos.put(criterio, valor);
    }

    //Para hacer pruebas cuando queramos ver los datos de la ruta.
    @Override
    public String toString() {
        return "Ruta de " + idOrigen + " a " + idDestino;
    }
}
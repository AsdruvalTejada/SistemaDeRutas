package aw.transporte.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Ruta {
    private String idOrigen;
    private String idDestino;
    private String nombreLinea; // Para los Transbordos
    private Map<CriterioPesos, Double> pesos;

    /**
     * Función: (Constructor) Ruta -
     * Objetivo: Crear una conexión entre paradas e inicializar el mapa de pesos.
     * @param idOrigen    (String) ID del nodo de salida.
     * @param idDestino   (String) ID del nodo de llegada.
     * @param nombreLinea (String) Nombre de la línea de transporte (ej. Línea A).
     * @param tiempo      (double) Tiempo estimado en minutos.
     * @param distancia   (double) Distancia en kilómetros.
     * @param costo       (double) Costo del pasaje.
     */
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

    /**
     * Función: getValorPeso -
     * Objetivo: Obtener el valor numérico de un peso específico según el criterio.
     * @param criterio (CriterioPesos) El criterio a consultar.
     * @return         (double) El valor del peso solicitado.
     */
    public double getValorPeso(CriterioPesos criterio) {
        return pesos.getOrDefault(criterio, 0.0);
    }

    /**
     * Función: equals -
     * Objetivo: Determinar si dos rutas son idénticas (mismo destino y misma línea).
     */
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

    public void setNombreLinea(String nombreLinea) {
        this.nombreLinea = nombreLinea;
    }
}
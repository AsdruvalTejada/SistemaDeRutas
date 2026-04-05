package aw.transporte.model;

/**
 * Enum: CriterioPesos
 * Objetivo: Definir las constantes inmutables que representan las métricas de evaluación
 * o "pesos" utilizables por los algoritmos de búsqueda de rutas óptimas
 * (Dijkstra, Bellman-Ford y Yen).
 */
public enum CriterioPesos {
    TIEMPO,
    DISTANCIA,
    COSTO,
    TRANSBORDOS
}
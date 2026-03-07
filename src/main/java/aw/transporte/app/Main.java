package aw.transporte.app;

import aw.transporte.data.JsonGestor;
import aw.transporte.logic.CalculadoraRutas;
import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== STARTING SISTEMA DE RUTAS ===");

        // 1. Instanciamos nuestro gestor de base de datos
        JsonGestor dbGestor = new JsonGestor();
        Grafo sistemaInfo = dbGestor.fetchGrafoData();

        // --- INYECTANDO NUEVA RED DE PRUEBAS ---
        System.out.println("-> Limpiando base de datos e inyectando red de prueba (Triángulo)...");

        // Limpiamos la memoria para que el test sea exacto
        sistemaInfo.getParadas().clear();
        sistemaInfo.getAdyacencia().clear();

        // Creamos las 3 Paradas
        sistemaInfo.agregarParada(new Parada("P1", "Sede PUCMM", 10.0, 20.0));
        sistemaInfo.agregarParada(new Parada("P2", "Monumento", 50.0, 60.0));
        sistemaInfo.agregarParada(new Parada("P3", "Estadio Cibao", 30.0, 80.0));

        // Creamos las Rutas (Origen, Destino, TIEMPO, Distancia, Costo, Transbordos)
        // Opción A: Ruta DIRECTA de P1 a P3 (Lenta: toma 35 minutos)
        sistemaInfo.agregarRuta(new Ruta("P1", "P3", 35.0, 10.0, 50.0, 0));

        // Opción B: Ruta CON ESCALA P1 -> P2 -> P3 (Vía rápida: toma 10 + 5 = 15 minutos)
        sistemaInfo.agregarRuta(new Ruta("P1", "P2", 10.0, 5.0, 25.0, 0));
        sistemaInfo.agregarRuta(new Ruta("P2", "P3", 5.0, 3.0, 15.0, 0));

        // --- PRUEBA DEL ALGORITMO DIJKSTRA (Estudiante B) ---

        // --- PRUEBA DE LOS 3 ALGORITMOS MAESTROS ---
        System.out.println("\n--- INICIANDO CALCULADORA DE RUTAS ---");
        CalculadoraRutas calculadora = new CalculadoraRutas();

        // 1. DIJKSTRA (Optimizar por TIEMPO)
        System.out.println("\n>> Buscando ruta más RÁPIDA (Tiempo) [Usa Dijkstra]:");
        CalculadoraRutas.ResultadoCamino resTiempo = calculadora.calcularRutaIdeal(sistemaInfo, "P1", "P3", CriterioPesos.TIEMPO);
        System.out.println("Camino: " + String.join(" -> ", resTiempo.paradas) + " | Minutos: " + resTiempo.costoTotal);

        // 2. BFS (Optimizar por TRANSBORDOS)
        System.out.println("\n>> Buscando ruta con MENOS SALTOS (Transbordos) [Usa BFS]:");
        CalculadoraRutas.ResultadoCamino resSaltos = calculadora.calcularRutaIdeal(sistemaInfo, "P1", "P3", CriterioPesos.TRANSBORDOS);
        System.out.println("Camino: " + String.join(" -> ", resSaltos.paradas) + " | Transbordos: " + resSaltos.costoTotal);

        // 3. BELLMAN-FORD (Optimizar por COSTO)
        System.out.println("\n>> Buscando ruta más BARATA (Costo) [Usa Bellman-Ford]:");
        CalculadoraRutas.ResultadoCamino resCosto = calculadora.calcularRutaIdeal(sistemaInfo, "P1", "P3", CriterioPesos.COSTO);
        System.out.println("Camino: " + String.join(" -> ", resCosto.paradas) + " | $: " + resCosto.costoTotal);
        // Al terminar todo, guardamos el estado actual en el JSON
        System.out.println("\n--- CERRANDO SISTEMA ---");
        dbGestor.saveGrafo(sistemaInfo);
    }
}
package aw.transporte.logic;

import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

import java.util.*;

/**
 * Clase: CalculadoraRutas
 * Objetivo: Motor de búsqueda y optimización que implementa los algoritmos clásicos
 * de grafos (Dijkstra, Bellman-Ford, Floyd-Warshall y Yen) para la gestión de transporte.
 */
public class CalculadoraRutas {

    public static class ResultadoCamino {
        public List<String> paradas;
        public double costoTotal;

        public ResultadoCamino(List<String> paradas, double costoTotal) {
            this.paradas = paradas;
            this.costoTotal = costoTotal;
        }
    }

    // El record guarda la "lineaLlegada" para saber en qué bus/tren vamos montados
    private record NodoDistancia(String idParada, double distanciaAcumulada, String lineaLlegada) {}

    /**
     * Función: calcularRutaIdeal
     * Objetivo: Punto de entrada principal que decide qué algoritmo utilizar (Dijkstra o Bellman-Ford) según el criterio seleccionado para encontrar la ruta óptima.
     * @param grafo    (Grafo) El grafo que contiene la red de paradas y rutas.
     * @param origen   (String) ID del nodo de partida.
     * @param destino  (String) ID del nodo de llegada.
     * @param criterio (CriterioPesos) El criterio de evaluación (TIEMPO, DISTANCIA, COSTO, TRANSBORDOS).
     * @return         (ResultadoCamino) Un objeto con la lista de paradas de la mejor ruta y su costo total.
     */
    public ResultadoCamino calcularRutaIdeal(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        return switch (criterio) {
            // Pasamos 'null' porque en la ruta ideal no queremos bloquear ninguna calle
            case TIEMPO, DISTANCIA, TRANSBORDOS -> dijkstra(grafo, origen, destino, criterio, null);
            case COSTO -> bellmanFord(grafo, origen, destino, criterio);
        };
    }

    /**
     * Función: dijkstra
     * Objetivo: Hallar la ruta más corta o económica desde un origen a un destino. Permite bloquear una ruta específica para forzar la búsqueda de alternativas. Su complejidad es O(E log V).
     * @param grafo         (Grafo) El sistema de rutas a evaluar.
     * @param idOrigen      (String) ID de la parada inicial.
     * @param idDestino     (String) ID de la parada final.
     * @param criterio      (CriterioPesos) El criterio de peso a minimizar.
     * @param rutaBloqueada (Ruta) Una ruta específica a ignorar durante el cálculo (útil para el Algoritmo de Yen), o null si es una búsqueda normal.
     * @return              (ResultadoCamino) La ruta óptima encontrada o null si no hay conexión.
     */
    private ResultadoCamino dijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio, Ruta rutaBloqueada) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        Map<String, String> lineasLlegada = new HashMap<>();

        PriorityQueue<NodoDistancia> colaPrioridad = new PriorityQueue<>(Comparator.comparingDouble(NodoDistancia::distanciaAcumulada));

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);
        colaPrioridad.add(new NodoDistancia(idOrigen, 0.0, null));

        while (!colaPrioridad.isEmpty()) {
            NodoDistancia actual = colaPrioridad.poll();
            String u = actual.idParada();
            String lineaActual = actual.lineaLlegada();

            if (u.equals(idDestino)) break;
            if (actual.distanciaAcumulada() > distancias.get(u)) continue;

            for (Ruta ruta : grafo.getAdyacencia().getOrDefault(u, new HashSet<>())) {

                // ¡MAGIA PARA LAS ALTERNATIVAS! Si esta calle está bloqueada, la ignoramos.
                if (rutaBloqueada != null && ruta.equals(rutaBloqueada)) continue;

                String v = ruta.getIdDestino();
                String lineaSiguiente = ruta.getNombreLinea();
                double pesoArista = (criterio == CriterioPesos.TRANSBORDOS) ? 0.0 : ruta.getValorPeso(criterio);

                double penalizacion = 0.0;
                if (lineaActual != null && !lineaActual.equals(lineaSiguiente)) {
                    if (criterio == CriterioPesos.TIEMPO) penalizacion = 5.0;
                    else if (criterio == CriterioPesos.TRANSBORDOS) penalizacion = 1.0;
                }

                double nuevaDistancia = distancias.get(u) + pesoArista + penalizacion;

                if (nuevaDistancia < distancias.getOrDefault(v, Double.MAX_VALUE)) {
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    lineasLlegada.put(v, lineaSiguiente);
                    colaPrioridad.add(new NodoDistancia(v, nuevaDistancia, lineaSiguiente));
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    /**
     * Función: obtenerAlternativas
     * Objetivo: Generar una lista ordenada con múltiples rutas alternativas entre dos puntos utilizando el Algoritmo de Yen y bloqueando aristas secuencialmente.
     * @param grafo    (Grafo) El grafo sobre el cual calcular las alternativas.
     * @param origen   (String) ID de la parada de inicio.
     * @param destino  (String) ID de la parada de destino.
     * @param criterio (CriterioPesos) El criterio para evaluar el peso de las rutas.
     * @return         (List<ResultadoCamino>) Lista de rutas alternativas ordenadas de mejor a peor según su costo total.
     */
    public List<ResultadoCamino> obtenerAlternativas(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        List<ResultadoCamino> alternativas = new ArrayList<>();
        ResultadoCamino principal = dijkstra(grafo, origen, destino, criterio, null);

        if (principal == null || principal.paradas.size() < 2) return alternativas;

        Set<List<String>> rutasVistas = new HashSet<>();
        rutasVistas.add(principal.paradas);

        // Bloqueamos un tramo a la vez para encontrar nuevos caminos
        for (int i = 0; i < principal.paradas.size() - 1; i++) {
            String u = principal.paradas.get(i);
            String v = principal.paradas.get(i + 1);

            Ruta tramoABloquear = null;
            for(Ruta r : grafo.getAdyacencia().get(u)) {
                if(r.getIdDestino().equals(v)) tramoABloquear = r;
            }

            ResultadoCamino candidato = dijkstra(grafo, origen, destino, criterio, tramoABloquear);

            // Si encontró una ruta y no es repetida, la guarda
            if (candidato != null && !rutasVistas.contains(candidato.paradas)) {
                alternativas.add(candidato);
                rutasVistas.add(candidato.paradas);
            }
        }
        // Ordenamos la lista de la más barata/rápida a la más costosa
        alternativas.sort(Comparator.comparingDouble(r -> r.costoTotal));
        return alternativas;
    }

    /**
     * Función: bellmanFord
     * Objetivo: Encontrar la ruta óptima permitiendo el procesamiento de pesos negativos (ej. descuentos en costos). Su complejidad es O(V * E).
     * @param grafo     (Grafo) La La red de paradas y conexiones.
     * @param idOrigen  (String) ID del nodo de salida.
     * @param idDestino (String) ID del nodo de llegada.
     * @param criterio  (CriterioPesos) Criterio de evaluación de la ruta.
     * @return          (ResultadoCamino) La ruta calculada con su costo, o null si detecta ciclos negativos o no hay camino.
     */
    private ResultadoCamino bellmanFord(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        Map<String, String> lineasLlegada = new HashMap<>();

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);

        int V = grafo.getParadas().size();

        for (int i = 0; i < V - 1; i++) {
            for (String u : grafo.getAdyacencia().keySet()) {
                for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                    String v = ruta.getIdDestino();
                    String lineaActual = lineasLlegada.get(u);
                    String lineaSiguiente = ruta.getNombreLinea();
                    double pesoArista = ruta.getValorPeso(criterio);

                    double penalizacion = 0.0;
                    if (lineaActual != null && !lineaActual.equals(lineaSiguiente)) penalizacion = 2.0;

                    if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista + penalizacion < distancias.get(v)) {
                        distancias.put(v, distancias.get(u) + pesoArista + penalizacion);
                        predecesores.put(v, u);
                        lineasLlegada.put(v, lineaSiguiente);
                    }
                }
            }
        }

        for (String u : grafo.getAdyacencia().keySet()) {
            for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                String v = ruta.getIdDestino();
                String lineaActual = lineasLlegada.get(u);
                String lineaSiguiente = ruta.getNombreLinea();
                double pesoArista = ruta.getValorPeso(criterio);

                double penalizacion = 0.0;
                if (lineaActual != null && !lineaActual.equals(lineaSiguiente)) penalizacion = 2.0;

                if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista + penalizacion < distancias.get(v)) {
                    return null; // Si de verdad entra aquí, hay un ciclo negativo real
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    /**
     * Función: reconstruirCamino
     * Objetivo: Método auxiliar que traza el camino de vuelta desde el destino hasta el origen usando el mapa de predecesores.
     * @param predecesores (Map<String, String>) Diccionario que vincula cada nodo con el nodo del que provino.
     * @param distancias   (Map<String, Double>) Diccionario con los costos acumulados hacia cada parada.
     * @param idOrigen     (String) ID del nodo donde inició la búsqueda.
     * @param idDestino    (String) ID del nodo donde finalizó la búsqueda.
     * @return             (ResultadoCamino) Objeto con la lista de paradas en el orden correcto y el costo total final.
     */
    private ResultadoCamino reconstruirCamino(Map<String, String> predecesores, Map<String, Double> distancias, String idOrigen, String idDestino) {
        if (!predecesores.containsKey(idDestino) && !idOrigen.equals(idDestino)) return null;
        List<String> caminoFinal = new ArrayList<>();
        String pasoActual = idDestino;
        while (pasoActual != null) {
            caminoFinal.add(pasoActual);
            pasoActual = predecesores.get(pasoActual);
        }
        Collections.reverse(caminoFinal);
        double costoFinal = distancias.isEmpty() ? 0 : distancias.get(idDestino);
        return new ResultadoCamino(caminoFinal, costoFinal);
    }

    public static class ResultadoMatrizGlobal {
        public double[][] matrizDistancias;
        public int[][] matrizSiguientes;
        public List<String> indiceAParadaId;
        public Map<String, Integer> paradaIdAIndice;

        public ResultadoMatrizGlobal(double[][] distancias, int[][] siguientes, List<String> indexToId, Map<String, Integer> idToIndex) {
            this.matrizDistancias = distancias;
            this.matrizSiguientes = siguientes;
            this.indiceAParadaId = indexToId;
            this.paradaIdAIndice = idToIndex;
        }
    }

    /**
     * Función: calcularRutasGlobales
     * Objetivo: Generar una matriz global con las distancias y costos mínimos de todos los nodos contra todos los nodos.
     * En lugar de usar Floyd-Warshall estándar, implementa un enfoque APSP (All-Pairs Shortest Path) ejecutando
     * el motor principal (Dijkstra/Bellman-Ford) iterativamente. Esto garantiza que el historial de rutas se mantenga
     * y las penalizaciones por transbordos de líneas se calculen con exactitud matemática. Su complejidad es O(V^2 * E log V).
     * @param grafoActivo   (Grafo) El grafo completo a procesar.
     * @param criterioViaje (CriterioPesos) El criterio para calcular los pesos entre todos los nodos (ej. Costo, Tiempo).
     * @return              (ResultadoMatrizGlobal) Objeto contenedor con la matriz de distancias y los índices mapeados.
     */
    public ResultadoMatrizGlobal calcularRutasGlobales(Grafo grafoActivo, CriterioPesos criterioViaje) {
        int totalParadas = grafoActivo.getParadas().size();
        double[][] matrizDistancias = new double[totalParadas][totalParadas];
        int[][] matrizSiguientes = new int[totalParadas][totalParadas];

        List<String> indiceAParadaId = new ArrayList<>(grafoActivo.getParadas().keySet());
        Map<String, Integer> paradaIdAIndice = new HashMap<>();
        for (int i = 0; i < totalParadas; i++) paradaIdAIndice.put(indiceAParadaId.get(i), i);

        for (int fila = 0; fila < totalParadas; fila++) {
            Arrays.fill(matrizDistancias[fila], Double.MAX_VALUE);
            matrizDistancias[fila][fila] = 0.0;
        }

        // Ejecutamos tu motor perfecto (Dijkstra/Bellman) de todos contra todos
        for (int i = 0; i < totalParadas; i++) {
            String origen = indiceAParadaId.get(i);
            for (int j = 0; j < totalParadas; j++) {
                if (i == j) continue;
                String destino = indiceAParadaId.get(j);

                // Aprovechamos que tu método ya calcula transbordos y líneas perfecto
                ResultadoCamino res = calcularRutaIdeal(grafoActivo, origen, destino, criterioViaje);
                if (res != null) {
                    matrizDistancias[i][j] = res.costoTotal;
                }
            }
        }
        return new ResultadoMatrizGlobal(matrizDistancias, matrizSiguientes, indiceAParadaId, paradaIdAIndice);
    }

    /**
     * Función: auditoriaConectividad
     * Objetivo: Verificar qué paradas son inalcanzables desde un punto de origen usando BFS.
     * @param grafo        (Grafo) El grafo que será escaneado.
     * @param paradaInicio (String) ID del nodo donde iniciará la propagación de búsqueda.
     * @return             (Set<String>) Conjunto de IDs correspondientes a paradas desconectadas.
     */
    public Set<String> auditoriaConectividad(Grafo grafo, String paradaInicio) {
        Set<String> visitados = new HashSet<>();
        Queue<String> cola = new LinkedList<>();

        cola.add(paradaInicio);
        visitados.add(paradaInicio);

        while (!cola.isEmpty()) {
            String u = cola.poll();
            for (Ruta r : grafo.getAdyacencia().getOrDefault(u, new HashSet<>())) {
                if (!visitados.contains(r.getIdDestino())) {
                    visitados.add(r.getIdDestino());
                    cola.add(r.getIdDestino());
                }
            }
        }

        // Al total de paradas le restamos las que pudimos visitar. Las que sobran, son huérfanas.
        Set<String> huerfanas = new HashSet<>(grafo.getParadas().keySet());
        huerfanas.removeAll(visitados);
        return huerfanas;
    }
}
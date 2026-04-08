package aw.transporte.logic;

import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

import java.util.*;

/**
 * Clase: CalculadoraRutas
 * Objetivo: Motor de búsqueda y optimización que implementa los algoritmos clásicos
 * de grafos (Dijkstra, Bellman-Ford, Floyd-Warshall y Yen Completo) para la gestión de transporte.
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
        return buscarRutaConBloqueos(grafo, origen, destino, criterio, new HashSet<>(), new HashSet<>());
    }

    /**
     * Función: buscarRutaConBloqueos
     * Objetivo: Controlador central que enruta la búsqueda hacia Dijkstra o Bellman-Ford,
     * aplicando listas de bloqueos dinámicos. Es el núcleo de la generación de alternativas.
     * @param grafo             (Grafo) El grafo activo.
     * @param origen            (String) ID de la parada inicial.
     * @param destino           (String) ID de la parada final.
     * @param criterio          (CriterioPesos) Criterio a minimizar.
     * @param aristasBloqueadas (Set<Ruta>) Conjunto de rutas a ignorar en este cálculo.
     * @param nodosBloqueados   (Set<String>) Conjunto de paradas a ignorar en este cálculo.
     * @return                  (ResultadoCamino) La ruta calculada evadiendo los bloqueos.
     */
    private ResultadoCamino buscarRutaConBloqueos(Grafo grafo, String origen, String destino, CriterioPesos criterio, Set<Ruta> aristasBloqueadas, Set<String> nodosBloqueados) {
        if (criterio == CriterioPesos.COSTO) {
            return bellmanFord(grafo, origen, destino, criterio, aristasBloqueadas, nodosBloqueados);
        } else {
            return dijkstra(grafo, origen, destino, criterio, aristasBloqueadas, nodosBloqueados);
        }
    }

    /**
     * Función: obtenerAlternativas
     * Objetivo: Generar una lista de rutas alternativas utilizando el verdadero Algoritmo de Yen
     * (K-Shortest Paths con Spur Paths). Bloquea sub-rutas y nodos para forzar desvíos reales.
     * @param grafo    (Grafo) El grafo sobre el cual calcular las alternativas.
     * @param origen   (String) ID de la parada de inicio.
     * @param destino  (String) ID de la parada de destino.
     * @param criterio (CriterioPesos) El criterio para evaluar el peso de las rutas.
     * @return         (List<ResultadoCamino>) Lista de rutas alternativas ordenadas de mejor a peor.
     */
    public List<ResultadoCamino> obtenerAlternativas(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        List<ResultadoCamino> alternativasA = new ArrayList<>();
        PriorityQueue<ResultadoCamino> posiblesB = new PriorityQueue<>(Comparator.comparingDouble(r -> r.costoTotal));

        // 1. Encontrar la ruta principal
        ResultadoCamino principal = buscarRutaConBloqueos(grafo, origen, destino, criterio, new HashSet<>(), new HashSet<>());
        if (principal == null || principal.paradas.size() < 2) return alternativasA;
        alternativasA.add(principal);

        int maxAlternativas = 4; // Queremos 4 alternativas extra (Total 5 opciones)

        // 2. Generación de Spur Paths
        for (int k = 1; k <= maxAlternativas; k++) {
            ResultadoCamino rutaAnterior = alternativasA.get(k - 1);

            for (int i = 0; i < rutaAnterior.paradas.size() - 1; i++) {
                String nodoDesvio = rutaAnterior.paradas.get(i);
                List<String> rutaRaiz = rutaAnterior.paradas.subList(0, i + 1);

                Set<Ruta> aristasBloqueadas = new HashSet<>();
                Set<String> nodosBloqueados = new HashSet<>();

                // Bloquear rutas que ya conocemos para forzar a buscar caminos nuevos
                for (ResultadoCamino alt : alternativasA) {
                    if (alt.paradas.size() > i && alt.paradas.subList(0, i + 1).equals(rutaRaiz)) {
                        String u = alt.paradas.get(i);
                        String v = alt.paradas.get(i + 1);
                        for (Ruta r : grafo.getAdyacencia().getOrDefault(u, new HashSet<>())) {
                            if (r.getIdDestino().equals(v)) aristasBloqueadas.add(r);
                        }
                    }
                }

                // Bloquear nodos de la ruta raíz para evitar bucles hacia atrás
                for (int j = 0; j < rutaRaiz.size() - 1; j++) {
                    nodosBloqueados.add(rutaRaiz.get(j));
                }

                // Buscar el desvío desde el nodo actual
                ResultadoCamino caminoDesvio = buscarRutaConBloqueos(grafo, nodoDesvio, destino, criterio, aristasBloqueadas, nodosBloqueados);

                if (caminoDesvio != null) {
                    List<String> caminoTotal = new ArrayList<>(rutaRaiz);
                    caminoTotal.remove(caminoTotal.size() - 1); // Quitar duplicado
                    caminoTotal.addAll(caminoDesvio.paradas);

                    double costoTotal = recalcularCostoExacto(grafo, caminoTotal, criterio);
                    ResultadoCamino nuevoCandidato = new ResultadoCamino(caminoTotal, costoTotal);

                    // Evitar duplicados
                    boolean duplicado = false;
                    for (ResultadoCamino p : alternativasA) if (p.paradas.equals(caminoTotal)) duplicado = true;
                    for (ResultadoCamino p : posiblesB) if (p.paradas.equals(caminoTotal)) duplicado = true;

                    if (!duplicado) posiblesB.add(nuevoCandidato);
                }
            }

            if (posiblesB.isEmpty()) break;
            alternativasA.add(posiblesB.poll());
        }

        alternativasA.remove(0); // Separamos la principal de la lista de alternativas
        return alternativasA;
    }

    /**
     * Función: recalcularCostoExacto
     * Objetivo: Suma los pesos reales de una ruta fusionada (Raíz + Desvío),
     * aplicando correctamente las penalizaciones de transbordo en el proceso.
     */
    private double recalcularCostoExacto(Grafo grafo, List<String> camino, CriterioPesos criterio) {
        double costo = 0.0;
        String lineaAnterior = null;

        for (int i = 0; i < camino.size() - 1; i++) {
            String u = camino.get(i);
            String v = camino.get(i + 1);

            double mejorPeso = Double.MAX_VALUE;
            String mejorLinea = null;

            for (Ruta r : grafo.getAdyacencia().getOrDefault(u, new HashSet<>())) {
                if (r.getIdDestino().equals(v)) {
                    double peso = (criterio == CriterioPesos.TRANSBORDOS) ? 0.0 : r.getValorPeso(criterio);
                    double penalizacion = 0.0;

                    if (lineaAnterior != null && !lineaAnterior.equals(r.getNombreLinea())) {
                        if (criterio == CriterioPesos.TIEMPO) penalizacion = 5.0;
                        else if (criterio == CriterioPesos.TRANSBORDOS) penalizacion = 1.0;
                        else if (criterio == CriterioPesos.COSTO) penalizacion = 2.0;
                    }

                    if (peso + penalizacion < mejorPeso) {
                        mejorPeso = peso + penalizacion;
                        mejorLinea = r.getNombreLinea();
                    }
                }
            }
            costo += mejorPeso;
            lineaAnterior = mejorLinea;
        }
        return costo;
    }

    /**
     * Función: dijkstra
     * Objetivo: Hallar la ruta más rápida/corta desde un origen a un destino.
     * Su complejidad es O((V+E) log V). Soporta el bloqueo de nodos y aristas para Yen.
     * @param grafo             (Grafo) El sistema de rutas a evaluar.
     * @param idOrigen          (String) ID de la parada inicial.
     * @param idDestino         (String) ID de la parada final.
     * @param criterio          (CriterioPesos) El criterio de peso a minimizar.
     * @param aristasBloqueadas (Set<Ruta>) Aristas prohibidas temporalmente.
     * @param nodosBloqueados   (Set<String>) Nodos prohibidos temporalmente.
     * @return                  (ResultadoCamino) La ruta óptima encontrada o null si no hay conexión.
     */
    private ResultadoCamino dijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio, Set<Ruta> aristasBloqueadas, Set<String> nodosBloqueados) {
        if (nodosBloqueados.contains(idOrigen) || nodosBloqueados.contains(idDestino)) return null;

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
                String v = ruta.getIdDestino();

                // Aplicar los bloqueos de Yen
                if (aristasBloqueadas.contains(ruta)) continue;
                if (nodosBloqueados.contains(v)) continue;

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
     * Función: bellmanFord
     * Objetivo: Encontrar la ruta óptima permitiendo el procesamiento de pesos negativos (ej. descuentos).
     * Su complejidad es O(V * E). Soporta el bloqueo de nodos y aristas para Yen.
     * @param grafo             (Grafo) La red de paradas y conexiones.
     * @param idOrigen          (String) ID del nodo de salida.
     * @param idDestino         (String) ID del nodo de llegada.
     * @param criterio          (CriterioPesos) Criterio de evaluación de la ruta.
     * @param aristasBloqueadas (Set<Ruta>) Aristas prohibidas temporalmente.
     * @param nodosBloqueados   (Set<String>) Nodos prohibidos temporalmente.
     * @return                  (ResultadoCamino) La ruta calculada o null.
     */
    private ResultadoCamino bellmanFord(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio, Set<Ruta> aristasBloqueadas, Set<String> nodosBloqueados) {
        if (nodosBloqueados.contains(idOrigen) || nodosBloqueados.contains(idDestino)) return null;

        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        Map<String, String> lineasLlegada = new HashMap<>();

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);

        int V = grafo.getParadas().size();

        for (int i = 0; i < V - 1; i++) {
            for (String u : grafo.getAdyacencia().keySet()) {
                if (nodosBloqueados.contains(u)) continue;

                for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                    String v = ruta.getIdDestino();

                    // Aplicar bloqueos de Yen
                    if (aristasBloqueadas.contains(ruta)) continue;
                    if (nodosBloqueados.contains(v)) continue;

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
     * el motor principal iterativamente. Esto garantiza que el historial de rutas se mantenga.
     * @param grafoActivo   (Grafo) El grafo completo a procesar.
     * @param criterioViaje (CriterioPesos) El criterio para calcular los pesos.
     * @return              (ResultadoMatrizGlobal) Contenedor con la matriz de distancias.
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

        for (int i = 0; i < totalParadas; i++) {
            String origen = indiceAParadaId.get(i);
            for (int j = 0; j < totalParadas; j++) {
                if (i == j) continue;
                String destino = indiceAParadaId.get(j);

                ResultadoCamino res = calcularRutaIdeal(grafoActivo, origen, destino, criterioViaje);
                if (res != null) {
                    matrizDistancias[i][j] = res.costoTotal;
                }
            }
        }
        return new ResultadoMatrizGlobal(matrizDistancias, matrizSiguientes, indiceAParadaId, paradaIdAIndice);
    }

    /**
     * Función: floydWarshall
     * Objetivo: Calcular la ruta mínima entre TODOS los pares de paradas
     * usando el algoritmo clásico de Floyd-Warshall con tres ciclos anidados.
     * Nota: Por su incapacidad de calcular penalizaciones de transbordos de forma clásica,
     * se mantiene solo para análisis académico/diagnóstico.
     * @param grafoActivo   (Grafo) Red de paradas y conexiones.
     * @param criterioViaje (CriterioPesos) Criterio de peso de las aristas.
     * @return              (ResultadoMatrizGlobal) Matriz de distancias.
     */
    public ResultadoMatrizGlobal floydWarshall(Grafo grafoActivo, CriterioPesos criterioViaje) {
        List<String> indices = new ArrayList<>(grafoActivo.getParadas().keySet());
        Map<String, Integer> paradaAIndice = new HashMap<>();
        int V = indices.size();

        for (int i = 0; i < V; i++) paradaAIndice.put(indices.get(i), i);

        double[][] dist = new double[V][V];
        int[][] siguiente = new int[V][V];

        for (int i = 0; i < V; i++) {
            Arrays.fill(dist[i], Double.MAX_VALUE / 2);
            Arrays.fill(siguiente[i], -1);
            dist[i][i] = 0.0;
        }

        for (Map.Entry<String, Set<Ruta>> entry : grafoActivo.getAdyacencia().entrySet()) {
            int u = paradaAIndice.get(entry.getKey());
            for (Ruta ruta : entry.getValue()) {
                Integer vIdx = paradaAIndice.get(ruta.getIdDestino());
                if (vIdx != null) {
                    double peso = ruta.getValorPeso(criterioViaje);
                    if (peso < dist[u][vIdx]) {
                        dist[u][vIdx] = peso;
                        siguiente[u][vIdx] = vIdx;
                    }
                }
            }
        }

        for (int k = 0; k < V; k++) {
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        siguiente[i][j] = siguiente[i][k];
                    }
                }
            }
        }

        return new ResultadoMatrizGlobal(dist, siguiente, indices, paradaAIndice);
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

        Set<String> huerfanas = new HashSet<>(grafo.getParadas().keySet());
        huerfanas.removeAll(visitados);
        return huerfanas;
    }
}
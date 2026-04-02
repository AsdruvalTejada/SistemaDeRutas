package aw.transporte.logic;

import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

import java.util.*;

/**
 * Clase: CalculadoraRutas
 * Objetivo: Motor de búsqueda y optimización que implementa los algoritmos clásicos
 * de grafos (Dijkstra, Bellman-Ford, Floyd-Warshall, BFS y Yen) para la gestión de transporte.
 */
public class CalculadoraRutas {

    /**
     * Clase Interna: ResultadoCamino
     * Objetivo: Empaquetar la lista de paradas y el costo acumulado de una ruta específica.
     */
    public static class ResultadoCamino {
        public List<String> paradas;
        public double costoTotal;

        public ResultadoCamino(List<String> paradas, double costoTotal) {
            this.paradas = paradas;
            this.costoTotal = costoTotal;
        }
    }

    /**
     * Clase Interna: ResultadoMatrizGlobal
     * Objetivo: Almacenar los resultados del algoritmo Floyd-Warshall para consultas masivas.
     */
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

    // Registro auxiliar para Dijkstra que incluye la memoria de la línea de llegada
    private record NodoDistancia(String idParada, double distanciaAcumulada, String lineaLlegada) {}

    /**
     * Función: calcularRutaIdeal
     * Objetivo: Punto de entrada que decide qué algoritmo utilizar según el criterio seleccionado.
     * @param grafo    (Grafo) El grafo que contiene la red de transporte actual.
     * @param origen   (String) ID de la parada donde inicia el viaje.
     * @param destino  (String) ID de la parada donde finaliza el viaje.
     * @param criterio (CriterioPesos) Criterio de evaluación (TIEMPO, DISTANCIA, TRANSBORDOS, COSTO).
     * @return         (ResultadoCamino) Objeto con la lista de paradas y el costo total óptimo.
     */
    public ResultadoCamino calcularRutaIdeal(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        return switch (criterio) {
            case TIEMPO, DISTANCIA, TRANSBORDOS -> ejecutarDijkstra(grafo, origen, destino, criterio, null);
            case COSTO -> ejecutarBellmanFord(grafo, origen, destino, criterio);
        };
    }

    /**
     * Función: ejecutarDijkstra
     * Objetivo: Hallar la ruta más corta entre dos puntos. Permite omitir una ruta para cálculos alternativos.
     * @param grafo         (Grafo) El grafo sobre el cual se calculará la ruta.
     * @param idOrigen      (String) ID del nodo de partida.
     * @param idDestino     (String) ID del nodo de llegada.
     * @param criterio      (CriterioPesos) Criterio de peso de las aristas.
     * @param rutaBloqueada (Ruta) Arista específica a ignorar (puede ser null).
     * @return              (ResultadoCamino) Objeto con el camino más corto encontrado.
     */
    private ResultadoCamino ejecutarDijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio, Ruta rutaBloqueada) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        PriorityQueue<NodoDistancia> colaPrioridad = new PriorityQueue<>(Comparator.comparingDouble(NodoDistancia::distanciaAcumulada));

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);
        colaPrioridad.add(new NodoDistancia(idOrigen, 0.0, null));

        while (!colaPrioridad.isEmpty()) {
            NodoDistancia actual = colaPrioridad.poll();
            String u = actual.idParada();

            if (u.equals(idDestino)) break;
            if (actual.distanciaAcumulada() > distancias.get(u)) continue;

            for (Ruta ruta : grafo.getAdyacencia().getOrDefault(u, new HashSet<>())) {
                // Lógica de Ruta Alternativa: Ignora el tramo bloqueado
                if (rutaBloqueada != null && ruta.equals(rutaBloqueada)) continue;

                String v = ruta.getIdDestino();
                double pesoBase = (criterio == CriterioPesos.TRANSBORDOS) ? 0.0 : ruta.getValorPeso(criterio);

                // Penalización por cambio de línea (Transbordos)
                double penalizacion = 0.0;
                if (actual.lineaLlegada() != null && !actual.lineaLlegada().equals(ruta.getNombreLinea())) {
                    if (criterio == CriterioPesos.TIEMPO) penalizacion = 5.0;
                    else if (criterio == CriterioPesos.TRANSBORDOS) penalizacion = 1.0;
                }

                double nuevaDistancia = distancias.get(u) + pesoBase + penalizacion;

                if (nuevaDistancia < distancias.getOrDefault(v, Double.MAX_VALUE)) {
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    colaPrioridad.add(new NodoDistancia(v, nuevaDistancia, ruta.getNombreLinea()));
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    /**
     * Función: obtenerPlanB
     * Objetivo: Calcular la segunda mejor ruta posible (Plan B) utilizando una versión del Algoritmo de Yen.
     * @param grafo    (Grafo) El grafo de la red de transporte.
     * @param origen   (String) ID del nodo de partida.
     * @param destino  (String) ID del nodo de llegada.
     * @param criterio (CriterioPesos) Criterio de peso a evaluar.
     * @return         (ResultadoCamino) La segunda mejor ruta disponible o null si no existe.
     */
    public ResultadoCamino obtenerPlanB(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        ResultadoCamino principal = ejecutarDijkstra(grafo, origen, destino, criterio, null);
        if (principal == null || principal.paradas.size() < 2) return null;

        ResultadoCamino mejorAlternativa = null;
        double minCostoAlt = Double.MAX_VALUE;

        // Se prueba bloqueando cada tramo de la ruta principal para encontrar el mejor desvío
        for (int i = 0; i < principal.paradas.size() - 1; i++) {
            String u = principal.paradas.get(i);
            String v = principal.paradas.get(i + 1);

            Ruta tramoABloquear = null;
            for(Ruta r : grafo.getAdyacencia().get(u)) if(r.getIdDestino().equals(v)) tramoABloquear = r;

            ResultadoCamino candidato = ejecutarDijkstra(grafo, origen, destino, criterio, tramoABloquear);

            if (candidato != null && candidato.costoTotal < minCostoAlt) {
                if (!candidato.paradas.equals(principal.paradas)) {
                    mejorAlternativa = candidato;
                    minCostoAlt = candidato.costoTotal;
                }
            }
        }
        return mejorAlternativa;
    }

    /**
     * Función: ejecutarBellmanFord
     * Objetivo: Calcular la ruta óptima considerando costos, permitiendo aristas con pesos negativos.
     * @param grafo     (Grafo) El grafo sobre el cual se operará.
     * @param idOrigen  (String) ID del nodo de partida.
     * @param idDestino (String) ID del nodo de llegada.
     * @param criterio  (CriterioPesos) Criterio de peso (usualmente COSTO).
     * @return          (ResultadoCamino) Objeto con la ruta óptima resolviendo pesos negativos.
     */
    private ResultadoCamino ejecutarBellmanFord(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);

        int V = grafo.getParadas().size();
        for (int i = 0; i < V - 1; i++) {
            for (String u : grafo.getAdyacencia().keySet()) {
                for (Ruta r : grafo.getAdyacencia().get(u)) {
                    double peso = r.getValorPeso(criterio);
                    if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + peso < distancias.get(r.getIdDestino())) {
                        distancias.put(r.getIdDestino(), distancias.get(u) + peso);
                        predecesores.put(r.getIdDestino(), u);
                    }
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    /**
     * Función: calcularFloydWarshall
     * Objetivo: Calcula la ruta más corta de todos los nodos contra todos los nodos
     * utilizando el algoritmo de Floyd-Warshall.
     * @param grafoActivo   (Grafo) El grafo que contiene los nodos (paradas) y aristas (rutas).
     * @param criterioViaje (CriterioPesos) El criterio por el cual se evaluará el peso de las rutas.
     * @return              (ResultadoMatrizGlobal) Objeto que contiene las matrices de distancias,
     * siguientes pasos y diccionarios de traducción de IDs.
     */
    public ResultadoMatrizGlobal calcularFloydWarshall(Grafo grafoActivo, CriterioPesos criterioViaje) {
        int totalParadas = grafoActivo.getParadas().size();
        double[][] matrizDistancias = new double[totalParadas][totalParadas];
        int[][] matrizSiguientes = new int[totalParadas][totalParadas];

        List<String> indiceAParadaId = new ArrayList<>(grafoActivo.getParadas().keySet());
        Map<String, Integer> paradaIdAIndice = new HashMap<>();

        for (int i = 0; i < totalParadas; i++) paradaIdAIndice.put(indiceAParadaId.get(i), i);

        for (int fila = 0; fila < totalParadas; fila++) {
            Arrays.fill(matrizDistancias[fila], Double.MAX_VALUE);
            Arrays.fill(matrizSiguientes[fila], -1);
            matrizDistancias[fila][fila] = 0.0;
        }

        for (String uId : grafoActivo.getAdyacencia().keySet()) {
            int u = paradaIdAIndice.get(uId);
            for (Ruta r : grafoActivo.getAdyacencia().get(uId)) {
                int v = paradaIdAIndice.get(r.getIdDestino());
                double peso = (criterioViaje == CriterioPesos.TRANSBORDOS) ? 1.0 : r.getValorPeso(criterioViaje);
                if (peso < matrizDistancias[u][v]) {
                    matrizDistancias[u][v] = peso;
                    matrizSiguientes[u][v] = v;
                }
            }
        }

        for (int k = 0; k < totalParadas; k++) {
            for (int i = 0; i < totalParadas; i++) {
                for (int j = 0; j < totalParadas; j++) {
                    if (matrizDistancias[i][k] != Double.MAX_VALUE && matrizDistancias[k][j] != Double.MAX_VALUE
                            && matrizDistancias[i][k] + matrizDistancias[k][j] < matrizDistancias[i][j]) {
                        matrizDistancias[i][j] = matrizDistancias[i][k] + matrizDistancias[k][j];
                        matrizSiguientes[i][j] = matrizSiguientes[i][k];
                    }
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

        Set<String> huerfanas = new HashSet<>(grafo.getParadas().keySet());
        huerfanas.removeAll(visitados);
        return huerfanas;
    }

    /**
     * Función: reconstruirCamino
     * Objetivo: Auxiliar para armar la lista de paradas a partir del mapa de predecesores.
     * @param predecesores (Map<String, String>) Diccionario con los enlaces origen-destino trazados.
     * @param distancias   (Map<String, Double>) Diccionario con los pesos acumulados hacia cada nodo.
     * @param idOrigen     (String) Nodo donde inicia la ruta reconstruida.
     * @param idDestino    (String) Nodo donde finaliza la ruta reconstruida.
     * @return             (ResultadoCamino) Objeto limpio con la ruta en orden correcto y costo total.
     */
    private ResultadoCamino reconstruirCamino(Map<String, String> predecesores, Map<String, Double> distancias, String idOrigen, String idDestino) {
        if (!predecesores.containsKey(idDestino) && !idOrigen.equals(idDestino)) return null;
        List<String> camino = new ArrayList<>();
        String actual = idDestino;
        while (actual != null) {
            camino.add(actual);
            actual = predecesores.get(actual);
        }
        Collections.reverse(camino);
        return new ResultadoCamino(camino, distancias.get(idDestino));
    }
}
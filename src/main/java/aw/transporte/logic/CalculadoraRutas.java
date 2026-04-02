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
     * Objetivo: Punto de entrada que decide qué algoritmo utilizar según el criterio seleccionado.
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
     * Objetivo: Hallar la ruta más corta. Permite omitir una ruta (rutaBloqueada) para hallar alternativas.
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
     * Objetivo: Retorna una lista ordenada con múltiples rutas alternativas usando Algoritmo de Yen.
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
     * Objetivo: Resolver rutas considerando pesos negativos (descuentos).
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
                double pesoArista = ruta.getValorPeso(criterio);
                if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista < distancias.get(v)) return null;
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

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
     * Objetivo: Calcula la ruta más corta de todos los nodos contra todos los nodos (Floyd-Warshall).
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
            Arrays.fill(matrizSiguientes[fila], -1);
            matrizDistancias[fila][fila] = 0.0;
        }

        for (String idOrigen : grafoActivo.getAdyacencia().keySet()) {
            int indiceOrigen = paradaIdAIndice.get(idOrigen);
            for (Ruta rutaActual : grafoActivo.getAdyacencia().get(idOrigen)) {
                int indiceDestino = paradaIdAIndice.get(rutaActual.getIdDestino());
                double pesoRuta = (criterioViaje == CriterioPesos.TRANSBORDOS) ? 1.0 : rutaActual.getValorPeso(criterioViaje);
                if (pesoRuta < matrizDistancias[indiceOrigen][indiceDestino]) {
                    matrizDistancias[indiceOrigen][indiceDestino] = pesoRuta;
                    matrizSiguientes[indiceOrigen][indiceDestino] = indiceDestino;
                }
            }
        }

        for (int nodoPuente = 0; nodoPuente < totalParadas; nodoPuente++) {
            for (int nodoOrigen = 0; nodoOrigen < totalParadas; nodoOrigen++) {
                for (int nodoDestino = 0; nodoDestino < totalParadas; nodoDestino++) {
                    boolean rutaValida = matrizDistancias[nodoOrigen][nodoPuente] != Double.MAX_VALUE &&
                            matrizDistancias[nodoPuente][nodoDestino] != Double.MAX_VALUE;
                    if (rutaValida) {
                        double nuevaDistanciaCalculada = matrizDistancias[nodoOrigen][nodoPuente] + matrizDistancias[nodoPuente][nodoDestino];
                        if (nuevaDistanciaCalculada < matrizDistancias[nodoOrigen][nodoDestino]) {
                            matrizDistancias[nodoOrigen][nodoDestino] = nuevaDistanciaCalculada;
                            matrizSiguientes[nodoOrigen][nodoDestino] = matrizSiguientes[nodoOrigen][nodoPuente];
                        }
                    }
                }
            }
        }
        return new ResultadoMatrizGlobal(matrizDistancias, matrizSiguientes, indiceAParadaId, paradaIdAIndice);
    }
}
package aw.transporte.logic;

import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

import java.util.*;

public class CalculadoraRutas {

    public static class ResultadoCamino {
        public List<String> paradas;
        public double costoTotal;

        public ResultadoCamino(List<String> paradas, double costoTotal) {
            this.paradas = paradas;
            this.costoTotal = costoTotal;
        }
    }

    // AÑADIDO: Ahora el record guarda la "lineaLlegada" para saber en qué bus/tren vamos montados
    private record NodoDistancia(String idParada, double distanciaAcumulada, String lineaLlegada) {}

    public ResultadoCamino calcularRutaIdeal(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        // AHORA TRANSBORDOS LO RESUELVE DIJKSTRA
        return switch (criterio) {
            case TIEMPO, DISTANCIA, TRANSBORDOS -> dijkstra(grafo, origen, destino, criterio);
            case COSTO -> bellmanFord(grafo, origen, destino, criterio);
        };
    }

    // 1. DIJKSTRA SUPER-CARGADO (Tiempo, Distancia y Transbordos)
    private ResultadoCamino dijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();

        // La memoria de la calculadora: Guarda por qué línea llegamos a una parada
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
                String lineaSiguiente = ruta.getNombreLinea();

                // Si buscamos menos transbordos, la distancia normal no importa (es 0).
                double pesoArista = (criterio == CriterioPesos.TRANSBORDOS) ? 0.0 : ruta.getValorPeso(criterio);

                // LÓGICA DE PENALIZACIÓN POR CAMBIO DE LÍNEA
                double penalizacion = 0.0;
                if (lineaActual != null && !lineaActual.equals(lineaSiguiente)) {
                    if (criterio == CriterioPesos.TIEMPO) penalizacion = 5.0;       // 5 mins esperando otro bus
                    else if (criterio == CriterioPesos.TRANSBORDOS) penalizacion = 1.0; // Sumamos 1 transbordo
                }

                double nuevaDistancia = distancias.get(u) + pesoArista + penalizacion;

                if (nuevaDistancia < distancias.getOrDefault(v, Double.MAX_VALUE)) {
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    lineasLlegada.put(v, lineaSiguiente); // Memorizamos la nueva línea
                    colaPrioridad.add(new NodoDistancia(v, nuevaDistancia, lineaSiguiente));
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    // 2. BELLMAN-FORD (Para Costo, porque soporta posibles subsidios o costos negativos)
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

                    // LÓGICA DE PENALIZACIÓN DE COSTO
                    double penalizacion = 0.0;
                    if (lineaActual != null && !lineaActual.equals(lineaSiguiente)) {
                        penalizacion = 2.0; // $2 extra por el ticket de transbordo
                    }

                    if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista + penalizacion < distancias.get(v)) {
                        distancias.put(v, distancias.get(u) + pesoArista + penalizacion);
                        predecesores.put(v, u);
                        lineasLlegada.put(v, lineaSiguiente);
                    }
                }
            }
        }

        // Verificación de Ciclos Negativos
        for (String u : grafo.getAdyacencia().keySet()) {
            for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                String v = ruta.getIdDestino();
                double pesoArista = ruta.getValorPeso(criterio);
                if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista < distancias.get(v)) {
                    return null;
                }
            }
        }

        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    private ResultadoCamino reconstruirCamino(Map<String, String> predecesores, Map<String, Double> distancias, String idOrigen, String idDestino) {
        if (!predecesores.containsKey(idDestino) && !idOrigen.equals(idDestino)) {
            return null; // Inalcanzable
        }

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

    // Usamos una clase interna para empaquetar la respuesta.
    public static class ResultadoMatrizGlobal {
        public double[][] matrizDistancias;
        public int[][] matrizSiguientes;
        public List<String> indiceAParadaId;     // Para saber qué ID (P1) es el índice [0]
        public Map<String, Integer> paradaIdAIndice; // Para saber qué índice [0] es el ID (P1)

        public ResultadoMatrizGlobal(double[][] distancias, int[][] siguientes, List<String> indexToId, Map<String, Integer> idToIndex) {
            this.matrizDistancias = distancias;
            this.matrizSiguientes = siguientes;
            this.indiceAParadaId = indexToId;
            this.paradaIdAIndice = idToIndex;
        }
    }

    /**
     * Función: calcularRutasGlobales
     * Objetivo: Calcula la ruta más corta de todos los nodos contra todos los nodos
     * utilizando el algoritmo de Floyd-Warshall (O(V^3)).
     * @param grafoActivo   (Grafo) El grafo que contiene los nodos (paradas) y aristas (rutas).
     * @param criterioViaje (CriterioPesos) El criterio por el cual se evaluará el peso de las rutas.
     * @return              (ResultadoMatrizGlobal) Objeto que contiene las matrices de distancias,
     * siguientes pasos y diccionarios de traducción de IDs.
     */
    public ResultadoMatrizGlobal calcularRutasGlobales(Grafo grafoActivo, CriterioPesos criterioViaje) {
        int totalParadas = grafoActivo.getParadas().size();
        double[][] matrizDistancias = new double[totalParadas][totalParadas];
        int[][] matrizSiguientes = new int[totalParadas][totalParadas];

        // 1. Crear diccionarios de traducción (ID -> Índice y viceversa)
        List<String> indiceAParadaId = new ArrayList<>(grafoActivo.getParadas().keySet());
        Map<String, Integer> paradaIdAIndice = new HashMap<>();

        for (int i = 0; i < totalParadas; i++) {
            paradaIdAIndice.put(indiceAParadaId.get(i), i);
        }

        // 2. Inicializar las matrices (Llenamos de "infinito" por defecto)
        for (int fila = 0; fila < totalParadas; fila++) {
            Arrays.fill(matrizDistancias[fila], Double.MAX_VALUE);
            Arrays.fill(matrizSiguientes[fila], -1);
            matrizDistancias[fila][fila] = 0.0; // La distancia a uno mismo es cero
        }

        // 3. Cargar las rutas existentes en la matriz inicial
        for (String idOrigen : grafoActivo.getAdyacencia().keySet()) {
            int indiceOrigen = paradaIdAIndice.get(idOrigen);

            for (Ruta rutaActual : grafoActivo.getAdyacencia().get(idOrigen)) {
                int indiceDestino = paradaIdAIndice.get(rutaActual.getIdDestino());

                // Si el criterio es TRANSBORDOS, cada ruta pesa 1. Si no, tomamos su valor real.
                double pesoRuta = (criterioViaje == CriterioPesos.TRANSBORDOS) ? 1.0 : rutaActual.getValorPeso(criterioViaje);

                // Prevenimos que una ruta más cara sobreescriba una más barata si hay múltiples líneas
                if (pesoRuta < matrizDistancias[indiceOrigen][indiceDestino]) {
                    matrizDistancias[indiceOrigen][indiceDestino] = pesoRuta;
                    matrizSiguientes[indiceOrigen][indiceDestino] = indiceDestino;
                }
            }
        }

        // 4. EL NÚCLEO DEL ALGORITMO (Los famosos 3 ciclos for anidados de Floyd)
        for (int nodoPuente = 0; nodoPuente < totalParadas; nodoPuente++) {
            for (int nodoOrigen = 0; nodoOrigen < totalParadas; nodoOrigen++) {
                for (int nodoDestino = 0; nodoDestino < totalParadas; nodoDestino++) {

                    boolean rutaValida = matrizDistancias[nodoOrigen][nodoPuente] != Double.MAX_VALUE &&
                            matrizDistancias[nodoPuente][nodoDestino] != Double.MAX_VALUE;

                    if (rutaValida) {
                        double nuevaDistanciaCalculada = matrizDistancias[nodoOrigen][nodoPuente] + matrizDistancias[nodoPuente][nodoDestino];

                        if (nuevaDistanciaCalculada < matrizDistancias[nodoOrigen][nodoDestino]) {
                            matrizDistancias[nodoOrigen][nodoDestino] = nuevaDistanciaCalculada;
                            // El siguiente paso para ir al Destino, es el mismo que para ir al nodo Puente
                            matrizSiguientes[nodoOrigen][nodoDestino] = matrizSiguientes[nodoOrigen][nodoPuente];
                        }
                    }
                }
            }
        }

        return new ResultadoMatrizGlobal(matrizDistancias, matrizSiguientes, indiceAParadaId, paradaIdAIndice);
    }
}
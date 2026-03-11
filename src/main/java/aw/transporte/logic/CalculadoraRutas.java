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

    private record NodoDistancia(String idParada, double distanciaAcumulada) {}

    // Este Método es el que llamará la Interfaz Gráfica)
    public ResultadoCamino calcularRutaIdeal(Grafo grafo, String origen, String destino, CriterioPesos criterio) {
        // Dependiendo de lo que pida el usuario, usamos el algoritmo matemáticamente correcto
        return switch (criterio) {
            case TIEMPO, DISTANCIA -> dijkstra(grafo, origen, destino, criterio);
            case TRANSBORDOS -> bfsTransbordos(grafo, origen, destino);
            case COSTO -> bellmanFord(grafo, origen, destino, criterio);
        };
    }

    // 1. DIJKSTRA (Para Tiempo y Distancia - No para descuentos o pesos negativos)
    private ResultadoCamino dijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        PriorityQueue<NodoDistancia> colaPrioridad = new PriorityQueue<>(Comparator.comparingDouble(NodoDistancia::distanciaAcumulada));

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);
        colaPrioridad.add(new NodoDistancia(idOrigen, 0.0));

        while (!colaPrioridad.isEmpty()) {
            NodoDistancia actual = colaPrioridad.poll();
            String u = actual.idParada();

            if (u.equals(idDestino)) break;
            if (actual.distanciaAcumulada() > distancias.get(u)) continue;

            for (Ruta ruta : grafo.getAdyacencia().getOrDefault(u, new ArrayList<>())) {
                String v = ruta.getIdDestino();
                double pesoArista = ruta.getValorPeso(criterio);
                double nuevaDistancia = distancias.get(u) + pesoArista;

                if (nuevaDistancia < distancias.getOrDefault(v, Double.MAX_VALUE)) {
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    colaPrioridad.add(new NodoDistancia(v, nuevaDistancia));
                }
            }
        }
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    // 2. BELLMAN-FORD (Para Costo - Si Soporta descuentos o pesos negativos)
    private ResultadoCamino bellmanFord(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {
        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();

        for (String p : grafo.getParadas().keySet()) distancias.put(p, Double.MAX_VALUE);
        distancias.put(idOrigen, 0.0);

        int V = grafo.getParadas().size();

        // 1. Relajar todas las aristas V - 1 veces
        for (int i = 0; i < V - 1; i++) {
            for (String u : grafo.getAdyacencia().keySet()) {
                for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                    String v = ruta.getIdDestino();
                    double pesoArista = ruta.getValorPeso(criterio);

                    if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista < distancias.get(v)) {
                        distancias.put(v, distancias.get(u) + pesoArista);
                        predecesores.put(v, u);
                    }
                }
            }
        }

        // 2. 🔥 NUEVO: Verificación de Ciclos Negativos (Blindaje contra bucles infinitos) 🔥
        for (String u : grafo.getAdyacencia().keySet()) {
            for (Ruta ruta : grafo.getAdyacencia().get(u)) {
                String v = ruta.getIdDestino();
                double pesoArista = ruta.getValorPeso(criterio);

                // Si después de V-1 iteraciones sigo encontrando rutas más baratas, hay un ciclo infinito
                if (distancias.get(u) != Double.MAX_VALUE && distancias.get(u) + pesoArista < distancias.get(v)) {
                    System.out.println("🚨 ALERTA: ¡Ciclo negativo detectado entre " + u + " y " + v + "! Abortando cálculo.");
                    return null; // Retornamos null para que la interfaz sepa que falló y NO haga el while infinito
                }
            }
        }

        // 3. Si todo está seguro, reconstruimos el camino
        return reconstruirCamino(predecesores, distancias, idOrigen, idDestino);
    }

    // 3. BFS (Para Transbordos - Busca la menor cantidad de saltos)
    private ResultadoCamino bfsTransbordos(Grafo grafo, String idOrigen, String idDestino) {
        Queue<String> cola = new LinkedList<>();
        Set<String> visitados = new HashSet<>();
        Map<String, String> predecesores = new HashMap<>();

        cola.add(idOrigen);
        visitados.add(idOrigen);

        while (!cola.isEmpty()) {
            String u = cola.poll();
            if (u.equals(idDestino)) break;

            for (Ruta ruta : grafo.getAdyacencia().getOrDefault(u, new ArrayList<>())) {
                String v = ruta.getIdDestino();
                if (!visitados.contains(v)) {
                    visitados.add(v);
                    predecesores.put(v, u);
                    cola.add(v);
                }
            }
        }

        // Para BFS, el "costo" es la cantidad de saltos (paradas - 1)
        ResultadoCamino res = reconstruirCamino(predecesores, new HashMap<>(), idOrigen, idDestino);
        if (res != null) {
            res.costoTotal = res.paradas.size() - 1; // 3 paradas = 2 transbordos
        }
        return res;
    }

    // MÉTODO PARA ARMAR LA LISTA DE LA RUTA FINAL
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
}
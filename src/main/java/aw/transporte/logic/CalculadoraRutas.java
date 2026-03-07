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

    public ResultadoCamino dijkstra(Grafo grafo, String idOrigen, String idDestino, CriterioPesos criterio) {

        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();

        PriorityQueue<NodoDistancia> colaPrioridad = new PriorityQueue<>(
                Comparator.comparingDouble(NodoDistancia::distanciaAcumulada)
        );

        for (String idParada : grafo.getParadas().keySet()) {
            distancias.put(idParada, Double.MAX_VALUE);
        }
        distancias.put(idOrigen, 0.0);
        colaPrioridad.add(new NodoDistancia(idOrigen, 0.0));

        while (!colaPrioridad.isEmpty()) {
            NodoDistancia actual = colaPrioridad.poll();
            String u = actual.idParada();

            // Si llegamos al destino, podemos detener la búsqueda para ahorrar tiempo
            if (u.equals(idDestino)) break;

            // Si encontramos una distancia mayor a la que ya tenemos registrada, la ignoramos
            if (actual.distanciaAcumulada() > distancias.get(u)) continue;

            // Revisamos los vecinos (las rutas que salen del nodo 'u')
            List<Ruta> rutasSalientes = grafo.getAdyacencia().getOrDefault(u, new ArrayList<>());

            for (Ruta ruta : rutasSalientes) {
                String v = ruta.getIdDestino();
                // Aquí usamos el Enum:
                double pesoArista = ruta.getValorPeso(criterio);

                double nuevaDistancia = distancias.get(u) + pesoArista;

                // Si encontramos un camino más corto hacia 'v', actualizamos todo
                if (nuevaDistancia < distancias.getOrDefault(v, Double.MAX_VALUE)) {
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    colaPrioridad.add(new NodoDistancia(v, nuevaDistancia));
                }
            }
        }

        // Si la distancia al destino sigue siendo infinito, es porque no hay forma de llegar.
        if (distancias.get(idDestino) == Double.MAX_VALUE) {
            return null; // Ruta inalcanzable
        }

        List<String> caminoFinal = new ArrayList<>();
        String pasoActual = idDestino;

        // Vamos retrocediendo desde el destino hasta el origen usando los predecesores
        while (pasoActual != null) {
            caminoFinal.add(pasoActual);
            pasoActual = predecesores.get(pasoActual);
        }

        // Invertimos la lista porque la armamos de atrás para adelante
        Collections.reverse(caminoFinal);

        return new ResultadoCamino(caminoFinal, distancias.get(idDestino));
    }
}

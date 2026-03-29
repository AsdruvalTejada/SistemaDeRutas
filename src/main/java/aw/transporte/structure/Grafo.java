package aw.transporte.structure;

import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import java.util.*;

public class Grafo {
    private Map<String, Parada> paradas;
    private Map<String, Set<Ruta>> adyacencia; // Ahora usamos un set para evitar duplicados desde acá

    public Grafo() {
        this.paradas = new HashMap<>();
        this.adyacencia = new HashMap<>();
    }

    public String generarId() {
        // Para buscar el ID máximo
        int maxId = paradas.keySet().stream()
                .filter(id -> id.startsWith("P"))
                .mapToInt(id -> {
                    try { return Integer.parseInt(id.substring(1)); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max()
                .orElse(0);
        return "P" + (maxId + 1);
    }

    public Map<String, Parada> getParadas() { return paradas; }
    public void setParadas(Map<String, Parada> paradas) { this.paradas = paradas; }
    public Map<String, Set<Ruta>> getAdyacencia() { return adyacencia; }
    public void setAdyacencia(Map<String, Set<Ruta>> adyacencia) { this.adyacencia = adyacencia; }

    public void agregarParada(Parada p) {
        paradas.putIfAbsent(p.getId(), p);
        adyacencia.computeIfAbsent(p.getId(), k -> new HashSet<>());
    }

    public boolean eliminarParada(String id) {
        if (paradas.remove(id) == null) return false;
        adyacencia.remove(id);
        // Para limpiar rutas fantasmas
        adyacencia.values().forEach(rutas -> rutas.removeIf(ruta -> ruta.getIdDestino().equals(id)));
        return true;
    }

    // Recibimos los objetos directos, el HashSet rechaza duplicados desde antes.
    public boolean agregarRuta(Parada origen, Parada destino, String linea, double tiempo, double costo, double distancia) {
        if (origen != null && destino != null && paradas.containsKey(origen.getId())) {
            Ruta nuevaRuta = new Ruta(origen.getId(), destino.getId(), linea, tiempo, distancia, costo);
            return adyacencia.get(origen.getId()).add(nuevaRuta);
        }
        return false;
    }

    public boolean eliminarRuta(Parada origen, Parada destino) {
        if (origen != null && adyacencia.containsKey(origen.getId())) {
            return adyacencia.get(origen.getId()).removeIf(ruta -> ruta.getIdDestino().equals(destino.getId()));
        }
        return false;
    }
}
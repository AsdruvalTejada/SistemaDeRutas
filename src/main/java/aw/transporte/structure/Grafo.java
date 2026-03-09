package aw.transporte.structure;

import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import java.util.*;

public class Grafo {
    private Map<String, Parada> paradas;

    private Map<String, List<Ruta>> adyacencia;

    public Grafo() {
        this.paradas = new HashMap<>();
        this.adyacencia = new HashMap<>();
    }

    public Map<String, Parada> getParadas() {
        return paradas;
    }

    public void setParadas(Map<String, Parada> paradas) {
        this.paradas = paradas;
    }

    public Map<String, List<Ruta>> getAdyacencia() {
        return adyacencia;
    }

    public void setAdyacencia(Map<String, List<Ruta>> adyacencia) {
        this.adyacencia = adyacencia;
    }

    public void agregarParada(Parada p) {
        if (!paradas.containsKey(p.getId())) {
            paradas.put(p.getId(), p);
            adyacencia.put(p.getId(), new ArrayList<>());
        }
    }

    public boolean modificarParada(String id, String nuevoNombre, double nuevaX, double nuevaY) {
        if (paradas.containsKey(id)) {
            Parada p = paradas.get(id);
            p.setNombre(nuevoNombre);
            p.setCoorx(nuevaX);
            p.setCoory(nuevaY);
            return true;
        }
        return false;
    }

    public boolean eliminarParada(String id) {
        if (!paradas.containsKey(id)) return false;

        // 1. Eliminar la parada y su lista de rutas salientes
        paradas.remove(id);
        adyacencia.remove(id);

        // 2. Limpiar las rutas que entraban a esta parada desde CUALQUIER otra
        for (Parada p : paradas.values()) {
            if (p.getRutas() != null) {
                p.getRutas().removeIf(ruta -> ruta.getIdDestino().equals(id));
            }
        }
        return true;
    }



    public boolean eliminarRuta(String idOrigen, String idDestino) {
        if (adyacencia.containsKey(idOrigen)) {
            List<Ruta> rutasSalientes = adyacencia.get(idOrigen);
            // El removeIf para buscar y eliminar la ruta que coincida con el destino
            return rutasSalientes.removeIf(ruta -> ruta.getIdDestino().equals(idDestino));
        }
        return false;
    }

    public void agregarRuta(String origenId, String destinoId, double tiempo, double costo, double distancia) {
        if (paradas.containsKey(origenId) && paradas.containsKey(destinoId)) {

            boolean existe = adyacencia.get(origenId).stream().anyMatch(r -> r.getIdDestino().equals(destinoId));

            if (!existe) {
                Ruta nuevaRuta = new Ruta(origenId, destinoId, tiempo, distancia, costo);
                adyacencia.get(origenId).add(nuevaRuta);
                paradas.get(origenId).getRutas().add(nuevaRuta);
                System.out.println("Conexión establecida: " + origenId + " -> " + destinoId);
            } else {
                System.out.println("La ruta " + origenId + " -> " + destinoId + " ya existe.");
            }
        }
    }

    public void reconstruirAdyacenciaDesdeParadas() {
        for (Parada p : paradas.values()) {
            // Aseguramos que la lista exista en el mapa de adyacencia
            adyacencia.put(p.getId(), new ArrayList<>(p.getRutas()));
        }
        System.out.println("Adyacencia sincronizada.");
    }
}
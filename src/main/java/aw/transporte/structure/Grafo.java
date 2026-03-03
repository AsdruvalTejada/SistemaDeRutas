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
        if (!paradas.containsKey(id)) {
            return false; // No existe
        }
        paradas.remove(id);
        adyacencia.remove(id);
        // Aquí se eliminan todas las rutas que entraban a esta parada desde otras
        for (List<Ruta> rutas : adyacencia.values()) {
            rutas.removeIf(ruta -> ruta.getIdDestino().equals(id));
        }
        return true;
    }

    public void agregarRuta(Ruta r) {
        if (adyacencia.containsKey(r.getIdOrigen()) && paradas.containsKey(r.getIdDestino())) {
            adyacencia.get(r.getIdOrigen()).add(r);
        } else {
            System.out.println("Origen o destino inválido.");
        }
    }

    public boolean eliminarRuta(String idOrigen, String idDestino) {
        if (adyacencia.containsKey(idOrigen)) {
            List<Ruta> rutasSalientes = adyacencia.get(idOrigen);
            // El removeIf para buscar y eliminar la ruta que coincida con el destino
            return rutasSalientes.removeIf(ruta -> ruta.getIdDestino().equals(idDestino));
        }
        return false;
    }
}
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

    public void agregarRuta(Ruta r) {
        if (adyacencia.containsKey(r.getIdOrigen())) {
            adyacencia.get(r.getIdOrigen()).add(r);
        }
    }
}
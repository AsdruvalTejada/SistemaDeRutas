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

    // Generador automático de IDs (Ej: Si existe P1, P2 y P5, el siguiente será P6)
    public String generarId() {
        int maxId = 0;

        for (String id : paradas.keySet()) {
            // Verificamos que el ID empiece con "P"
            if (id.startsWith("P")) {
                try {
                    // Extraemos el número después de la "P" y buscamos el más grande
                    int numero = Integer.parseInt(id.substring(1));
                    if (numero > maxId) {
                        maxId = numero;
                    }
                } catch (NumberFormatException e) {
                    // Si por alguna razón hay un ID raro, lo ignoramos
                }
            }
        }

        // Retornamos la letra "P" más el siguiente número disponible
        return "P" + (maxId + 1);
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

        // 2. Limpiar las rutas que entraban a esta parada buscando directamente en la adyacencia
        for (List<Ruta> rutasSalientes : adyacencia.values()) {
            rutasSalientes.removeIf(ruta -> ruta.getIdDestino().equals(id));
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
                adyacencia.get(origenId).add(nuevaRuta); // Solo guardamos en la adyacencia
                System.out.println("Conexión establecida: " + origenId + " -> " + destinoId);
            } else {
                System.out.println("La ruta " + origenId + " -> " + destinoId + " ya existe.");
            }
        }
    }
}
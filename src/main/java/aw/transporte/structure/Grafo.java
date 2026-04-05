package aw.transporte.structure;

import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import java.util.*;

/**
 * Clase: Grafo
 * Objetivo: Estructura de datos principal que modela la red de transporte.
 * Utiliza un diccionario (HashMap) para los vértices (Paradas) y listas de adyacencia
 * basadas en conjuntos (HashSet) para las aristas (Rutas), garantizando un acceso en O(1) amortizado
 * y previniendo conexiones duplicadas.
 */
public class Grafo {
    private Map<String, Parada> paradas;
    private Map<String, Set<Ruta>> adyacencia; // Ahora usamos un set para evitar duplicados desde acá

    public Grafo() {
        this.paradas = new HashMap<>();
        this.adyacencia = new HashMap<>();
    }

    /**
     * Función: generarId
     * Objetivo: Escanear las paradas existentes y generar un nuevo identificador secuencial único
     * (ej. "P15") para asignar a una nueva parada que se está creando en la interfaz.
     * @return (String) El nuevo ID autogenerado.
     */
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

    public Map<String, Parada> getParadas() {
        return paradas;
    }

    public void setParadas(Map<String, Parada> paradas) {
        this.paradas = paradas;
    }

    public Map<String, Set<Ruta>> getAdyacencia() {
        return adyacencia;
    }

    public void setAdyacencia(Map<String, Set<Ruta>> adyacencia) {
        this.adyacencia = adyacencia;
    }

    /**
     * Función: agregarParada
     * Objetivo: Insertar un nuevo nodo (Parada) en el grafo y prepararle una lista de adyacencia vacía.
     * @param p (Parada) El objeto de la parada a registrar.
     */
    public void agregarParada(Parada p) {
        paradas.putIfAbsent(p.getId(), p);
        adyacencia.computeIfAbsent(p.getId(), k -> new HashSet<>());
    }

    /**
     * Función: eliminarParada
     * Objetivo: Borrar un nodo del grafo y barrer todas las listas de adyacencia restantes para
     * destruir cualquier ruta "fantasma" que apuntara hacia la parada eliminada. Costo: O(|V| + |E|).
     * @param id (String) Identificador único de la parada a eliminar.
     * @return   (boolean) True si se eliminó con éxito, False si la parada no existía.
     */
    public boolean eliminarParada(String id) {
        if (paradas.remove(id) == null) return false;
        adyacencia.remove(id);
        // Para limpiar rutas fantasmas
        adyacencia.values().forEach(rutas -> rutas.removeIf(ruta -> ruta.getIdDestino().equals(id)));
        return true;
    }

    /**
     * Función: agregarRuta
     * Objetivo: Crear una arista dirigida entre dos paradas existentes con todos sus pesos calculados.
     * @param origen    (Parada) Nodo de salida.
     * @param destino   (Parada) Nodo de llegada.
     * @param linea     (String) Nombre de la línea de transporte.
     * @param tiempo    (double) Costo en minutos.
     * @param costo     (double) Costo en moneda (pasaje).
     * @param distancia (double) Costo en kilómetros.
     * @return          (boolean) True si se insertó la ruta, False si ya existía (rechazado por el Set) o nodos nulos.
     */
    // Recibimos los objetos directos, el HashSet rechaza duplicados desde antes.
    public boolean agregarRuta(Parada origen, Parada destino, String linea, double tiempo, double costo, double distancia) {
        if (origen != null && destino != null && paradas.containsKey(origen.getId())) {
            Ruta nuevaRuta = new Ruta(origen.getId(), destino.getId(), linea, tiempo, distancia, costo);
            return adyacencia.get(origen.getId()).add(nuevaRuta);
        }
        return false;
    }

    /**
     * Función: eliminarRuta
     * Objetivo: Destruir la conexión directa entre un origen y un destino específicos.
     * @param origen  (Parada) Nodo de partida.
     * @param destino (Parada) Nodo de llegada de la arista a borrar.
     * @return        (boolean) True si se encontró y eliminó la ruta, False en caso contrario.
     */
    public boolean eliminarRuta(Parada origen, Parada destino) {
        if (origen != null && adyacencia.containsKey(origen.getId())) {
            return adyacencia.get(origen.getId()).removeIf(ruta -> ruta.getIdDestino().equals(destino.getId()));
        }
        return false;
    }
}
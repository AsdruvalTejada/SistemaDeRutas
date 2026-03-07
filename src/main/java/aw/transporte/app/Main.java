package aw.transporte.app;

import aw.transporte.data.JsonGestor;
import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== STARTING SISTEMA DE RUTAS ===");

        // 1. Instanciamos nuestro gestor con flow
        JsonGestor dbGestor = new JsonGestor();

        // 2. Intentamos hacer fetch de la data
        Grafo sistemaInfo = dbGestor.fetchGrafoData();

        // 3. Si el grafo está vacío (porque es la primera vez que lo corres), le metemos data dummy
        if (sistemaInfo.getParadas().isEmpty()) {
            System.out.println("-> Inyectando data de prueba al sistema...");

            sistemaInfo.agregarParada(new Parada("P1", "Sede PUCMM", 10.0, 20.0));
            sistemaInfo.agregarParada(new Parada("P2", "Monumento", 50.0, 60.0));

            // Origen, Destino, Tiempo, Distancia, Costo, Transbordos
            sistemaInfo.agregarRuta(new Ruta("P1", "P2", 15.0, 5.5, 35.0, 0));
        } else {
            System.out.println("-> Data ya existía. Saltando la inyección de prueba.");
        }

        // --- AQUÍ PUEDES HACER TUS PRUEBAS ---
        // Por ejemplo, imprimir cuántas paradas hay:
        System.out.println("Total de paradas actuales: " + sistemaInfo.getParadas().size());

        // 4. Al terminar todo, guardamos el estado actual
        System.out.println("\n--- CERRANDO SISTEMA ---");
        dbGestor.saveGrafo(sistemaInfo);
    }

    // Comentario de prueba para verificar Git
}
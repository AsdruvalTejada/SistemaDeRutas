package aw.transporte.app;

import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.model.CriterioPesos;
import aw.transporte.structure.Grafo;

public class Main {
    public static void main(String[] args) {
        System.out.println("PRUEBAS DEL SISTEMA DE TRANSPORTE");

        Grafo sistema = new Grafo();

        // 1. Agregar Paradas
        sistema.agregarParada(new Parada("P1", "Sede PUCMM", 10.0, 20.0));
        sistema.agregarParada(new Parada("P2", "Monumento", 50.0, 60.0));
        sistema.agregarParada(new Parada("P3", "Estadio Cibao", 30.0, 80.0));
        System.out.println("Paradas registradas: " + sistema.getParadas().size()); // Debe ser 3

        // 2. Agregar Rutas (Origen, Destino, Tiempo, Distancia, Costo, Transbordos)
        sistema.agregarRuta(new Ruta("P1", "P2", 15.0, 5.5, 35.0, 0));
        sistema.agregarRuta(new Ruta("P2", "P3", 10.0, 3.2, 35.0, 0));
        sistema.agregarRuta(new Ruta("P1", "P3", 30.0, 8.0, 50.0, 1)); // Ruta directa más larga

        System.out.println("Rutas desde Sede PUCMM (P1): " + sistema.getAdyacencia().get("P1").size()); // Debe ser 2

        // 3. Probar los Pesos con tu Enum
        Ruta rutaP1P2 = sistema.getAdyacencia().get("P1").get(0);
        System.out.println("\nDetalles de la " + rutaP1P2 + ":");
        System.out.println("- Tiempo: " + rutaP1P2.getValorPeso(CriterioPesos.TIEMPO) + " min");
        System.out.println("- Costo: $" + rutaP1P2.getValorPeso(CriterioPesos.COSTO));

        // 4. Probar Eliminación
        System.out.println("\n--- Eliminando Parada P2 (Monumento) ---");
        sistema.eliminarParada("P2");

        System.out.println("Paradas restantes: " + sistema.getParadas().size()); // Debe ser 2

        // Como P2 ya no existe, la ruta P1->P2 también debió borrarse automáticamente
        System.out.println("Rutas restantes desde Sede PUCMM (P1): " + sistema.getAdyacencia().get("P1").size()); // Debe ser 1 (solo la que va a P3)
    }
}
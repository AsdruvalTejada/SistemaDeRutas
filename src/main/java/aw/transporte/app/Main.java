package aw.transporte.app;

import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;

public class Main {
    public static void main(String[] args) {
        System.out.println("---PRUEBAS---");

        Grafo sistemaTransporte = new Grafo();

        Parada p1 = new Parada("P01", "Sede Central", 100.0, 200.0);
        Parada p2 = new Parada("P02", "Parada Las Carreras", 150.0, 300.0);

        sistemaTransporte.agregarParada(p1);
        sistemaTransporte.agregarParada(p2);
        System.out.println("Paradas agregadas con éxito.");

        Ruta rutaIda = new Ruta("P01", "P02", 12.0, 3.5, 35.0, 0);

        sistemaTransporte.agregarRuta(rutaIda);
        System.out.println("Ruta agregada con éxito.");

        System.out.println("\n--- RESULTADOS ---");
        System.out.println("Total de paradas en el sistema: " + sistemaTransporte.getParadas().size());

        int rutasDesdeP01 = sistemaTransporte.getAdyacencia().get("P01").size();
        System.out.println("Rutas que salen: " + rutasDesdeP01);

        System.out.println(sistemaTransporte.getAdyacencia().get("P01").get(0));
    }
}
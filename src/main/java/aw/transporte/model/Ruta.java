package aw.transporte.model;

public class Ruta {
    private String idOrigen;
    private String idDestino;
    private double tiempoEnMin;
    private double distanciaKm;
    private double costo;
    private int transbordos;

    public Ruta(String idOrigen, String idDestino, double tiempoEnMin, double distanciaKm, double costo, int transbordos) {
        this.idOrigen = idOrigen;
        this.idDestino = idDestino;
        this.tiempoEnMin = tiempoEnMin;
        this.distanciaKm = distanciaKm;
        this.costo = costo;
        this.transbordos = transbordos;
    }

    public String getIdOrigen() { return idOrigen; }
    public String getIdDestino() { return idDestino; }
    public double getTiempo() { return tiempoEnMin; }
    public double getDistanciaKm() { return distanciaKm; }
    public double getCosto() { return costo; }
    public int getTransbordos() { return transbordos; }

    //Para hacer pruebas cuando queramos ver los datos de la ruta.
    @Override
    public String toString() {
        return "Ruta de " + idOrigen + " a " + idDestino + " (" + tiempoEnMin + " min)";
    }
}
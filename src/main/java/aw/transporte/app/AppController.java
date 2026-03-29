package aw.transporte.app;

import aw.transporte.data.JsonGestor;
import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;
import aw.transporte.logic.CalculadoraRutas;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.*;

public class AppController {

    @FXML private TextField txtParadaNombre, txtRutaTiempo, txtRutaCosto;
    @FXML private Button btnAgregarParada, btnAgregarRuta, btnEliminarRuta, btnCalcular;
    @FXML private ComboBox<CriterioPesos> comboCriterio;

    // LAS NUEVAS VARIABLES DE COMBOBOX
    @FXML private ComboBox<Parada> comboRutaOrigen, comboRutaDestino, comboCalcOrigen, comboCalcDestino;

    @FXML private Label lblEstado;
    @FXML private Pane graphPane;
    @FXML private AnchorPane anchorMapa;

    private Grafo sistemaInfo;
    private JsonGestor dbGestor;
    private static final double ZOOM = 7.0;
    private double clickX, clickY;

    @FXML
    public void initialize() {
        dbGestor = new JsonGestor();
        sistemaInfo = dbGestor.fetchGrafoData();

        try {
            String imagePath = getClass().getResource("/mapa_fondo.png").toExternalForm();
            anchorMapa.setStyle(
                    "-fx-background-image: url('" + imagePath + "'); " + "-fx-background-size: cover; " + "-fx-background-position: center; " + "-fx-opacity: 0.9; " + "-fx-background-color: #ffffff;"
            );
        } catch (Exception e) { System.out.println("No se encontró la imagen."); }

        if (comboCriterio != null) comboCriterio.setItems(FXCollections.observableArrayList(CriterioPesos.values()));

        graphPane.setOnMouseClicked(event -> {
            clickX = (event.getX() - 100) / ZOOM;
            clickY = (event.getY() - 100) / ZOOM;
            dibujarPuntoTemporal(event.getX(), event.getY());
            updateStatus("Ubicación marcada.");
        });

        btnAgregarParada.setOnAction(e -> handleAgregarParada());
        btnAgregarRuta.setOnAction(e -> handleAgregarRuta());
        btnEliminarRuta.setOnAction(e -> handleEliminarRuta());
        btnCalcular.setOnAction(e -> handleCalcularRuta());

        dibujarGrafoVisual();
        actualizarComboBoxesParadas(); // Cargamos las listas al iniciar
        aplicarFijadorDeTexto(comboRutaOrigen, "Seleccione Origen");
        aplicarFijadorDeTexto(comboRutaDestino, "Seleccione Destino");
        aplicarFijadorDeTexto(comboCalcOrigen, "Punto de Partida");
        aplicarFijadorDeTexto(comboCalcDestino, "Punto de Llegada");
        aplicarFijadorDeTextoCriterio(comboCriterio, "Criterio de Viaje");
    }

    // EL MOTOR QUE ACTUALIZA LAS LISTAS
    private void actualizarComboBoxesParadas() {
        List<Parada> listaParadas = new ArrayList<>(sistemaInfo.getParadas().values());
        listaParadas.sort(Comparator.comparing(Parada::getNombre));
        ObservableList<Parada> opciones = FXCollections.observableArrayList(listaParadas);

        if (comboRutaOrigen != null) comboRutaOrigen.setItems(opciones);
        if (comboRutaDestino != null) comboRutaDestino.setItems(opciones);
        if (comboCalcOrigen != null) comboCalcOrigen.setItems(opciones);
        if (comboCalcDestino != null) comboCalcDestino.setItems(opciones);
    }

    private void limpiarCamposParadas() {
        txtParadaNombre.clear();
    }

    private void limpiarCamposRutas() {
        comboRutaOrigen.setValue(null);
        comboRutaDestino.setValue(null);
        txtRutaTiempo.clear();
        txtRutaCosto.clear();
    }

    private void aplicarFijadorDeTexto(ComboBox<Parada> combo, String textoFantasma) {
        combo.setButtonCell(new ListCell<Parada>() {
            @Override
            protected void updateItem(Parada item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(textoFantasma); // Si está vacío, fuerza el texto
                } else {
                    setText(item.toString()); // Si tiene una parada, muestra su nombre
                }
            }
        });
    }

    private void aplicarFijadorDeTextoCriterio(ComboBox<CriterioPesos> combo, String textoFantasma) {
        combo.setButtonCell(new ListCell<CriterioPesos>() {
            @Override
            protected void updateItem(CriterioPesos item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(textoFantasma);
                } else {
                    setText(item.toString());
                }
            }
        });
    }

    private void limpiarCamposViaje() {
        comboCalcOrigen.setValue(null);
        comboCalcDestino.setValue(null);
        comboCriterio.setValue(null);
    }

    private void handleAgregarParada() {
        String nombreParada = txtParadaNombre.getText().trim();

        if (nombreParada.isEmpty()) {
            updateStatus("Olvidaste ponerle nombre a la parada.");
            return;
        }

        try {
            String nuevoId = sistemaInfo.generarId(); // Usamos el generador automático

            sistemaInfo.agregarParada(new Parada(nuevoId, nombreParada, clickX, clickY));
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            actualizarComboBoxesParadas(); // Refrescamos las listas

            updateStatus("Parada guardada automáticamente como: " + nuevoId);
            limpiarCamposParadas();

        } catch (Exception e) {
            updateStatus("Error crítico al guardar la parada.");
        }
    }

    private void handleAgregarRuta() {
        try {
            Parada origen = comboRutaOrigen.getValue();
            Parada destino = comboRutaDestino.getValue();

            if (origen == null || destino == null) {
                updateStatus("⚠️ Seleccione origen y destino.");
                return;
            }
            if (origen.getId().equals(destino.getId())) {
                updateStatus("⚠️ El origen y destino no pueden ser el mismo.");
                return;
            }

            double tiempo = Double.parseDouble(txtRutaTiempo.getText());
            double costo = Double.parseDouble(txtRutaCosto.getText());

            if (tiempo < 0 || costo < 0) {
                updateStatus("⚠️ Los valores de tiempo y costo no pueden ser negativos.");
                return;
            }

            double dx = destino.getCoorx() - origen.getCoorx();
            double dy = destino.getCoory() - origen.getCoory();
            double distanciaKm = Math.round((Math.sqrt((dx * dx) + (dy * dy)) / 30.0) * 100.0) / 100.0;

            // Invento un nombre de línea genérico por ahora (Ej: "Línea P1-P2"). Luego Wilmary, puedes agregar un TextField para esto porfa.
            String nombreLinea = "Línea " + origen.getId() + "-" + destino.getId();

            // Pasamos los objetos como tal. El boolean nos dice si fue exitoso o duplicado.
            boolean conectada = sistemaInfo.agregarRuta(origen, destino, nombreLinea, tiempo, costo, distanciaKm);

            if (conectada) {
                dbGestor.saveGrafo(sistemaInfo);
                dibujarGrafoVisual();
                updateStatus("✅ Conexión creada exitosamente.");
                txtRutaTiempo.clear();
                txtRutaCosto.clear();
            } else {
                updateStatus("⚠️ La ruta ya existe hacia ese destino.");
            }

        } catch (NumberFormatException e) {
            updateStatus("⚠️ Error: Solo ingrese números en Tiempo y Costo.");
        } catch (Exception e) {
            updateStatus("⚠️ Error inesperado al conectar.");
        }
    }

    private void handleEliminarRuta() {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();

        if (origen == null || destino == null) {
            updateStatus("⚠️ Seleccione origen y destino para eliminar.");
            return;
        }

        // Pasamos OBJETOS
        if (sistemaInfo.eliminarRuta(origen, destino)) {
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            updateStatus("🗑️ Línea eliminada.");
        } else {
            updateStatus("❌ No se encontró la ruta o ya fue eliminada.");
        }
    }

    private void handleCalcularRuta() {
        try {
            Parada origen = comboCalcOrigen.getValue();
            Parada destino = comboCalcDestino.getValue();
            CriterioPesos criterio = comboCriterio.getValue();

            if (origen == null || destino == null || criterio == null) {
                updateStatus(" Seleccione origen, destino y criterio.");
                return;
            }

            CalculadoraRutas.ResultadoCamino res = new CalculadoraRutas().calcularRutaIdeal(sistemaInfo,
                    origen.getId(), destino.getId(), criterio);

            if (res != null) {
                dibujarGrafoConCamino(res.paradas);

                String mensaje = "";
                switch (criterio) {
                    case TIEMPO:
                        mensaje = "Tiempo estimado: " + res.costoTotal + " minutos.";
                        break;
                    case COSTO:
                        mensaje = "Costo del viaje: $" + res.costoTotal;
                        break;
                    case DISTANCIA:
                        double distRedondeada = Math.round(res.costoTotal * 100.0) / 100.0;
                        mensaje = "Distancia a recorrer: " + distRedondeada + " Km.";
                        break;
                    case TRANSBORDOS:
                        int conexiones = res.paradas.size() - 1;
                        mensaje = "Ruta más directa: " + conexiones + " trasbordos/tramos.";
                        break;
                }

                updateStatus(" Ruta encontrada | " + mensaje);

                // Limpiamos los ComboBox de búsqueda al terminar
                limpiarCamposViaje();

            } else {
                updateStatus(" No hay ruta disponible o están desconectadas.");
            }
        } catch (Exception e) {
            updateStatus(" Error en el cálculo.");
        }
    }

    private void dibujarGrafoVisual() {
        if (graphPane == null) return;
        graphPane.getChildren().clear();

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        Map<String, Set<Ruta>> adyacencia = sistemaInfo.getAdyacencia();

        for (String idOrigen : adyacencia.keySet()) {
            Parada p = paradas.get(idOrigen);
            if (p == null) continue;

            for (Ruta r : adyacencia.get(idOrigen)) {
                Parada d = paradas.get(r.getIdDestino());
                if (d != null) {
                    crearFlecha(p, d, Color.BLACK, 2.5, 0.7);
                }
            }
        }

        for (Parada p : paradas.values()) {
            double x = p.getCoorx()*ZOOM+100;
            double y = p.getCoory()*ZOOM+100;

            Circle c = new Circle(x, y, 12, Color.web("#1e3799"));

            // Evento para borrar la parada con Clic Derecho
            c.setOnMouseClicked(event -> {
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                    alerta.setTitle("Confirmar Eliminación");
                    alerta.setHeaderText("¿Eliminar la parada " + p.getNombre() + "?");
                    alerta.setContentText("Esta acción es irreversible y borrará las rutas conectadas.");

                    java.util.Optional<ButtonType> resultado = alerta.showAndWait();

                    if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                        if (sistemaInfo.eliminarParada(p.getId())) {
                            dbGestor.saveGrafo(sistemaInfo);
                            dibujarGrafoVisual();
                            actualizarComboBoxesParadas(); // Actualizamos las listas
                            updateStatus("Parada eliminada: " + p.getNombre());
                        }
                    } else {
                        updateStatus("Eliminación cancelada.");
                    }
                }
            });
            c.setStroke(Color.WHITE); c.setStrokeWidth(2);

            Label nameLabel = new Label(p.getNombre());
            nameLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); " + "-fx-padding: 2 5 2 5; -fx-background-radius: 5; " + "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 11px; " + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");
            nameLabel.setLayoutX(x - 20);
            nameLabel.setLayoutY(y + 18);

            Tooltip tt = new Tooltip("ID: " + p.getId());
            Tooltip.install(c, tt);

            graphPane.getChildren().addAll(c, nameLabel);
        }
    }

    private void crearFlecha(Parada origen, Parada destino, Color color, double grosor, double opacidad) {
        double x1 = origen.getCoorx() * ZOOM + 100;
        double y1 = origen.getCoory() * ZOOM + 100;
        double x2 = destino.getCoorx() * ZOOM + 100;
        double y2 = destino.getCoory() * ZOOM + 100;

        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(color);
        l.setStrokeWidth(grosor);
        l.setOpacity(opacidad);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double angulo = Math.atan2(dy, dx);
        double distanciaCorte = 15.0;
        double puntoPuntaX = x2 - distanciaCorte * Math.cos(angulo);
        double puntoPuntaY = y2 - distanciaCorte * Math.sin(angulo);
        double tamanoCabeza = 12.0;
        double xBase1 = puntoPuntaX - tamanoCabeza * Math.cos(angulo - Math.toRadians(30));
        double yBase1 = puntoPuntaY - tamanoCabeza * Math.sin(angulo - Math.toRadians(30));
        double xBase2 = puntoPuntaX - tamanoCabeza * Math.cos(angulo + Math.toRadians(30));
        double yBase2 = puntoPuntaY - tamanoCabeza * Math.sin(angulo + Math.toRadians(30));

        Polygon punta = new Polygon();
        punta.getPoints().addAll(new Double[]{
                puntoPuntaX, puntoPuntaY, xBase1, yBase1, xBase2, yBase2
        });

        punta.setFill(color);
        punta.setOpacity(opacidad);

        graphPane.getChildren().addAll(l, punta);
    }


    private void dibujarGrafoConCamino(List<String> camino) {
        dibujarGrafoVisual();
        Map<String, Parada> paradas = sistemaInfo.getParadas();
        for (int i = 0; i < camino.size() - 1; i++) {
            Parada p1 = paradas.get(camino.get(i)); Parada p2 = paradas.get(camino.get(i+1));
            if (p1 != null && p2 != null) {
                crearFlecha(p1, p2, Color.web("#e67e22"), 6.0, 1.0);
            }
        }
    }

    private void dibujarPuntoTemporal(double x, double y) {
        dibujarGrafoVisual();
        Circle preview = new Circle(x, y, 8, Color.web("#2f3542", 0.5));
        graphPane.getChildren().add(preview);
    }

    private void updateStatus(String msg) { if (lblEstado != null) lblEstado.setText(msg); }
}
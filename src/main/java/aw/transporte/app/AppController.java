package aw.transporte.app;

import aw.transporte.data.JsonGestor;
import aw.transporte.model.CriterioPesos;
import aw.transporte.model.Parada;
import aw.transporte.model.Ruta;
import aw.transporte.structure.Grafo;
import aw.transporte.logic.CalculadoraRutas;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.Map;
import java.util.List;

public class AppController {

    @FXML private TextField txtParadaId, txtParadaNombre, txtRutaOrigen, txtRutaDestino, txtRutaTiempo, txtRutaCosto, txtCalcOrigen, txtCalcDestino;
    @FXML private Button btnAgregarParada, btnEliminarParada, btnAgregarRuta, btnCalcular;
    @FXML private ComboBox<CriterioPesos> comboCriterio;
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
                    "-fx-background-image: url('" + imagePath + "'); " +
                            "-fx-background-size: cover; " +
                            "-fx-background-position: center; " +
                            "-fx-opacity: 0.9; " +
                            "-fx-background-color: #ffffff;"
            );
        } catch (Exception e) { System.out.println("No se encontró la imagen."); }

        if (comboCriterio != null) comboCriterio.setItems(FXCollections.observableArrayList(CriterioPesos.values()));

        graphPane.setOnMouseClicked(event -> {
            clickX = (event.getX() - 100) / ZOOM;
            clickY = (event.getY() - 100) / ZOOM;
            dibujarPuntoTemporal(event.getX(), event.getY());
        });

        btnAgregarParada.setOnAction(e -> handleAgregarParada());
        btnEliminarParada.setOnAction(e -> handleEliminarParada());
        btnAgregarRuta.setOnAction(e -> handleAgregarRuta());
        btnCalcular.setOnAction(e -> handleCalcularRuta());

        dibujarGrafoVisual();
    }

    private void handleEliminarParada() {
        String id = txtParadaId.getText().trim();
        if (sistemaInfo.eliminarParada(id)) {
            dbGestor.saveGrafo(sistemaInfo);
            updateStatus("Parada " + id + " eliminada.");
            dibujarGrafoVisual();
        } else { updateStatus("ID no existe."); }
    }

    private void dibujarGrafoVisual() {
        if (graphPane == null) return;
        graphPane.getChildren().clear();
        Map<String, Parada> paradas = sistemaInfo.getParadas();

        // 1. VÉRTICES (LÍNEAS NEGRAS Y MÁS GRUESAS)
        for (Parada p : paradas.values()) {
            if (p.getRutas() != null) {
                for (Ruta r : p.getRutas()) {
                    Parada d = paradas.get(r.getIdDestino());
                    if (d != null) {
                        Line l = new Line(p.getCoorx()*ZOOM+100, p.getCoory()*ZOOM+100, d.getCoorx()*ZOOM+100, d.getCoory()*ZOOM+100);
                        l.setStroke(Color.BLACK); // Color Negro
                        l.setStrokeWidth(2.5);   // Un poco más grueso
                        l.setOpacity(0.7);       // Sutil transparencia para que se vea el mapa
                        graphPane.getChildren().add(l);
                    }
                }
            }
        }

        // 2. PARADAS Y NOMBRES (CON BACKGROUND PARA LECTURA)
        for (Parada p : paradas.values()) {
            double x = p.getCoorx()*ZOOM+100;
            double y = p.getCoory()*ZOOM+100;

            Circle c = new Circle(x, y, 12, Color.web("#1e3799"));
            c.setStroke(Color.WHITE); c.setStrokeWidth(2);

            // NOMBRES EN NEGRO CON PEQUEÑO FONDO (Uso de Label como fondo)
            Label nameLabel = new Label(p.getNombre());
            nameLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.7); " +
                    "-fx-padding: 2; -fx-background-radius: 3; " +
                    "-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 11px;");
            nameLabel.setLayoutX(x - 20);
            nameLabel.setLayoutY(y + 18);

            Tooltip tt = new Tooltip("ID: " + p.getId());
            Tooltip.install(c, tt);

            graphPane.getChildren().addAll(c, nameLabel);
        }
    }

    private void handleAgregarParada() {
        try {
            sistemaInfo.agregarParada(new Parada(txtParadaId.getText(), txtParadaNombre.getText(), clickX, clickY));
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            updateStatus("Guardado.");
        } catch (Exception e) { updateStatus("Error."); }
    }

    private void handleAgregarRuta() {
        try {
            sistemaInfo.agregarRuta(txtRutaOrigen.getText(), txtRutaDestino.getText(),
                    Double.parseDouble(txtRutaTiempo.getText()), Double.parseDouble(txtRutaCosto.getText()), 0);
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
        } catch (Exception e) { updateStatus("Error."); }
    }

    private void handleCalcularRuta() {
        try {
            CalculadoraRutas.ResultadoCamino res = new CalculadoraRutas().calcularRutaIdeal(sistemaInfo,
                    txtCalcOrigen.getText(), txtCalcDestino.getText(), comboCriterio.getValue());
            if (res != null) {
                dibujarGrafoConCamino(res.paradas);
                updateStatus("Costo: " + res.costoTotal);
            }
        } catch (Exception e) { updateStatus("Error."); }
    }

    private void dibujarGrafoConCamino(List<String> camino) {
        dibujarGrafoVisual();
        Map<String, Parada> paradas = sistemaInfo.getParadas();
        for (int i = 0; i < camino.size() - 1; i++) {
            Parada p1 = paradas.get(camino.get(i)); Parada p2 = paradas.get(camino.get(i+1));
            if (p1 != null && p2 != null) {
                Line path = new Line(p1.getCoorx()*ZOOM+100, p1.getCoory()*ZOOM+100, p2.getCoorx()*ZOOM+100, p2.getCoory()*ZOOM+100);
                path.setStroke(Color.web("#e67e22")); // Color Naranja Neón para el camino elegido
                path.setStrokeWidth(5);
                graphPane.getChildren().add(path);
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
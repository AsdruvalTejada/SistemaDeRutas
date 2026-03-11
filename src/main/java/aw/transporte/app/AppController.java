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
import javafx.scene.shape.Polygon;
import java.util.Map;
import java.util.List;

public class AppController {

    @FXML private TextField txtParadaId, txtParadaNombre, txtRutaOrigen, txtRutaDestino, txtRutaTiempo, txtRutaCosto, txtCalcOrigen, txtCalcDestino;
    @FXML private Button btnAgregarParada, btnAgregarRuta, btnEliminarRuta, btnCalcular;
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
    }

    private void limpiarCamposParadas() {
        txtParadaId.clear();
        txtParadaNombre.clear();
    }

    private void limpiarCamposRutas() {
        txtRutaOrigen.clear();
        txtRutaDestino.clear();
        txtRutaTiempo.clear();
        txtRutaCosto.clear();
    }

    private void handleEliminarRuta() {
        String ori = txtRutaOrigen.getText().trim();
        String des = txtRutaDestino.getText().trim();

        // Ejecuta el código de tu amigo
        if (sistemaInfo.eliminarRuta(ori, des)) {
            dbGestor.saveGrafo(sistemaInfo);

            // AHORA SÍ: Como el dibujo lee de la adyacencia y tu amigo borró de ahí,
            // al llamar a esto la flecha DESAPARECERÁ del mapa.
            dibujarGrafoVisual();

            updateStatus("Línea eliminada.");
            limpiarCamposRutas();
        } else {
            updateStatus("No se encontró la ruta en ese sentido.");
        }
    }

    private void handleAgregarParada() {
        try {
            // 1. Le pedimos al Grafo que genere un ID seguro y automático
            String nuevoId = sistemaInfo.generarId();

            // 2. Creamos la parada con ese ID generado
            sistemaInfo.agregarParada(new Parada(nuevoId, txtParadaNombre.getText(), clickX, clickY));
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();

            updateStatus("Parada guardada automáticamente como: " + nuevoId);

            // 3. Limpiamos solo el nombre
            txtParadaNombre.clear();
            if (txtParadaId != null) txtParadaId.clear(); // Por si aún no lo borras del FXML

        } catch (Exception e) {
            updateStatus("Error al guardar parada.");
        }
    }

    private void handleAgregarRuta() {
        try {
            sistemaInfo.agregarRuta(txtRutaOrigen.getText(), txtRutaDestino.getText(),
                    Double.parseDouble(txtRutaTiempo.getText()), Double.parseDouble(txtRutaCosto.getText()), 0);
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            updateStatus("Conexión creada.");
            limpiarCamposRutas();
        } catch (Exception e) { updateStatus("Error: Verifica los datos de la ruta."); }
    }

    private void handleCalcularRuta() {
        try {
            CalculadoraRutas.ResultadoCamino res = new CalculadoraRutas().calcularRutaIdeal(sistemaInfo,
                    txtCalcOrigen.getText(), txtCalcDestino.getText(), comboCriterio.getValue());
            if (res != null) {
                dibujarGrafoConCamino(res.paradas);
                updateStatus("Costo total: " + res.costoTotal);
            }
        } catch (Exception e) { updateStatus("Error en el cálculo."); }
    }

    private void dibujarGrafoVisual() {
        if (graphPane == null) return;
        graphPane.getChildren().clear();

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        Map<String, List<Ruta>> adyacencia = sistemaInfo.getAdyacencia();

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

                    // 1. Creamos la ventana de confirmación
                    Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
                    alerta.setTitle("Confirmar Eliminación");
                    alerta.setHeaderText("¿Eliminar la parada " + p.getNombre() + "?");
                    alerta.setContentText("Esta acción es irreversible y también borrará todas las rutas conectadas a esta parada.");

                    // 2. Mostramos la ventana y esperamos a que el usuario responda
                    java.util.Optional<ButtonType> resultado = alerta.showAndWait();

                    // 3. Si el usuario presiona "OK", procedemos con la destrucción
                    if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                        if (sistemaInfo.eliminarParada(p.getId())) {
                            dbGestor.saveGrafo(sistemaInfo);
                            dibujarGrafoVisual(); // Redibujamos para que desaparezca
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
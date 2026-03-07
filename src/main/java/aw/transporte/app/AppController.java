package aw.transporte.app;

import aw.transporte.data.JsonGestor;
import aw.transporte.model.Parada;
import aw.transporte.structure.Grafo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.Map;

public class AppController {

    // Vinculación exacta con los fx:id de tu FXML
    @FXML private TextField txtParadaId, txtParadaNombre, txtParadaX, txtParadaY;
    @FXML private Button btnAgregarParada, btnEliminarParada;
    @FXML private TextArea txtConsola;

    // Este no está en tu FXML aún, pero lo dejamos preparado
    @FXML private Pane graphPane;

    private Grafo sistemaInfo;
    private JsonGestor dbGestor;
    private static final double ZOOM = 5.0;

    @FXML
    public void initialize() {
        dbGestor = new JsonGestor();
        sistemaInfo = dbGestor.fetchGrafoData();

        logConsole("✅ Sistema iniciado correctamente.");

        // Solo asignamos eventos si los botones existen en el FXML
        if (btnAgregarParada != null) {
            btnAgregarParada.setOnAction(e -> handleAgregarParada());
        }
        if (btnEliminarParada != null) {
            btnEliminarParada.setOnAction(e -> handleEliminarParada());
        }

        dibujarGrafoVisual();
    }

    private void handleAgregarParada() {
        try {
            String id = txtParadaId.getText().trim();
            String nom = txtParadaNombre.getText().trim();
            double x = Double.parseDouble(txtParadaX.getText());
            double y = Double.parseDouble(txtParadaY.getText());

            if (id.isEmpty() || nom.isEmpty()) throw new Exception("Campos vacíos.");

            sistemaInfo.agregarParada(new Parada(id, nom, x, y));
            dbGestor.saveGrafo(sistemaInfo);

            logConsole("📍 Parada " + id + " añadida con éxito.");
            limpiarCampos();
            dibujarGrafoVisual();
        } catch (Exception e) {
            logConsole("❌ Error: " + e.getMessage());
        }
    }

    private void handleEliminarParada() {
        String id = txtParadaId.getText().trim();
        if (!id.isEmpty()) {
            sistemaInfo.eliminarParada(id);
            dbGestor.saveGrafo(sistemaInfo);
            logConsole("🗑️ Parada " + id + " eliminada.");
            dibujarGrafoVisual();
        }
    }

    private void dibujarGrafoVisual() {
        // Si no tienes el Pane de dibujo en el FXML, esto no hará nada
        if (graphPane == null) return;

        graphPane.getChildren().clear();
        Map<String, Parada> paradas = sistemaInfo.getParadas();

        for (Parada p : paradas.values()) {
            double vX = p.getCoorx() * ZOOM + 50;
            double vY = p.getCoory() * ZOOM + 50;

            Circle nodo = new Circle(vX, vY, 10, Color.web("#4CAF50"));
            Text etiqueta = new Text(vX - 10, vY + 25, p.getId());
            etiqueta.setFont(Font.font("System", FontWeight.BOLD, 12));

            graphPane.getChildren().addAll(nodo, etiqueta);
        }
    }

    private void logConsole(String msg) {
        if (txtConsola != null) txtConsola.appendText(msg + "\n");
    }

    private void limpiarCampos() {
        txtParadaId.clear(); txtParadaNombre.clear();
        txtParadaX.clear(); txtParadaY.clear();
    }
}
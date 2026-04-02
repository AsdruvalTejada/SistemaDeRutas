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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.*;

public class AppController {

    @FXML private TextField txtParadaNombre, txtRutaTiempo, txtRutaCosto;
    @FXML private Button btnAgregarParada, btnAgregarRuta, btnEliminarRuta, btnCalcular;
    @FXML private ComboBox<CriterioPesos> comboCriterio;

    @FXML private ComboBox<Parada> comboRutaOrigen, comboRutaDestino, comboCalcOrigen, comboCalcDestino;

    @FXML private Label lblEstado;
    @FXML private Pane graphPane;
    @FXML private AnchorPane anchorMapa;
    @FXML private ComboBox<Parada> comboParadaModificar;
    @FXML private TextField txtNuevoNombreParada;
    @FXML private Button btnModificarParada;
    @FXML private Button btnModificarRuta;
    @FXML private VBox panelFlotante;
    @FXML private ComboBox<String> comboNombreLinea;

    @FXML private TabPane tabPanePrincipal;
    @FXML private Tab tabParadas;
    @FXML private Tab tabConexiones;

    private double xOffset = 0;
    private double yOffset = 0;

    private Grafo sistemaInfo;
    private JsonGestor dbGestor;
    private static final double ZOOM = 7.0;
    private double clickX, clickY;

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

        if (panelFlotante != null) {

            // Cambiamos el cursor a una mano abierta para indicar que se puede mover
            panelFlotante.setStyle("-fx-cursor: open-hand;");
            panelFlotante.setOnMousePressed(event -> {
                panelFlotante.setStyle("-fx-cursor: closed-hand;");
                // Calculamos la diferencia entre el ratón y la posición actual de traslación
                xOffset = event.getSceneX() - panelFlotante.getTranslateX();
                yOffset = event.getSceneY() - panelFlotante.getTranslateY();
            });

            panelFlotante.setOnMouseDragged(event -> {
                // Movemos el panel visualmente sumando la traslación
                panelFlotante.setTranslateX(event.getSceneX() - xOffset);
                panelFlotante.setTranslateY(event.getSceneY() - yOffset);
            });

            panelFlotante.setOnMouseReleased(event -> {
                panelFlotante.setStyle("-fx-cursor: open-hand;");
            });
        } else {
            System.out.println("No se puede mover");
        }
        btnAgregarParada.setOnAction(e -> handleAgregarParada());
        btnAgregarRuta.setOnAction(e -> handleAgregarRuta());
        btnEliminarRuta.setOnAction(e -> handleEliminarRuta());
        btnCalcular.setOnAction(e -> handleCalcularRuta());
        btnModificarParada.setOnAction(e -> handleModificarParada());
        btnModificarRuta.setOnAction(e -> handleModificarRuta());


        dibujarGrafoVisual();
        actualizarComboBoxesParadas();
        aplicarFijadorDeTexto(comboRutaOrigen, "Seleccione Origen");
        aplicarFijadorDeTexto(comboRutaDestino, "Seleccione Destino");
        aplicarFijadorDeTexto(comboCalcOrigen, "Punto de Partida");
        aplicarFijadorDeTexto(comboCalcDestino, "Punto de Llegada");
        aplicarFijadorDeTextoCriterio(comboCriterio, "Criterio de Viaje");
    }

    public void configurarPermisos(boolean esAdmin) {
        if (!esAdmin) {
            if (tabPanePrincipal != null && tabParadas != null && tabConexiones != null) {
                tabPanePrincipal.getTabs().remove(tabParadas);
                tabPanePrincipal.getTabs().remove(tabConexiones);
            }
            updateStatus("Modo Pasajero: Solo lectura y cálculo de rutas.");
        } else {
            updateStatus("Modo Administrador: Acceso total.");
        }
    }
    private void actualizarComboBoxesParadas() {
        List<Parada> listaParadas = new ArrayList<>(sistemaInfo.getParadas().values());
        listaParadas.sort(Comparator.comparing(Parada::getNombre));
        ObservableList<Parada> opciones = FXCollections.observableArrayList(listaParadas);

        if (comboRutaOrigen != null) comboRutaOrigen.setItems(opciones);
        if (comboRutaDestino != null) comboRutaDestino.setItems(opciones);
        if (comboCalcOrigen != null) comboCalcOrigen.setItems(opciones);
        if (comboCalcDestino != null) comboCalcDestino.setItems(opciones);
        comboParadaModificar.getItems().setAll(sistemaInfo.getParadas().values());
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

    private void handleModificarParada() {
        Parada paradaSeleccionada = comboParadaModificar.getValue();
        String nuevoNombre = txtNuevoNombreParada.getText().trim();

        if (paradaSeleccionada == null) {
            updateStatus(" Seleccione una parada de la lista para modificar.");
            return;
        }
        if (nuevoNombre.isEmpty()) {
            updateStatus(" El nuevo nombre no puede estar vacío.");
            return;
        }

        paradaSeleccionada.setNombre(nuevoNombre);

        dbGestor.saveGrafo(sistemaInfo);
        dibujarGrafoVisual();
        actualizarComboBoxesParadas();

        txtNuevoNombreParada.clear();
        comboParadaModificar.getSelectionModel().clearSelection();
        updateStatus(" Parada modificada exitosamente a: " + nuevoNombre);
    }

    private void handleAgregarRuta() {
        try {
            Parada origen = comboRutaOrigen.getValue();
            Parada destino = comboRutaDestino.getValue();

            if (origen == null || destino == null) {
                updateStatus(" Seleccione origen y destino.");
                return;
            }
            if (origen.getId().equals(destino.getId())) {
                updateStatus(" El origen y destino no pueden ser el mismo.");
                return;
            }

            double tiempo = Double.parseDouble(txtRutaTiempo.getText());
            double costo = Double.parseDouble(txtRutaCosto.getText());

            if (tiempo < 0 || costo < 0) {
                updateStatus(" Los valores de tiempo y costo no pueden ser negativos.");
                return;
            }

            double dx = destino.getCoorx() - origen.getCoorx();
            double dy = destino.getCoory() - origen.getCoory();
            double distanciaKm = Math.round((Math.sqrt((dx * dx) + (dy * dy)) / 30.0) * 100.0) / 100.0;


            String nombreLinea = comboNombreLinea.getValue();
            if (nombreLinea == null || nombreLinea.trim().isEmpty()) {
                updateStatus(" Por favor, seleccione o escriba el nombre de la línea.");
                return;
            }

            boolean conectada = sistemaInfo.agregarRuta(origen, destino, nombreLinea, tiempo, costo, distanciaKm);

            if (conectada) {
                dbGestor.saveGrafo(sistemaInfo);
                dibujarGrafoVisual();
                actualizarComboBoxLineas();
                updateStatus(" Conexión creada exitosamente.");
                txtRutaTiempo.clear();
                txtRutaCosto.clear();
            } else {
                updateStatus(" La ruta ya existe hacia ese destino.");
            }

        } catch (NumberFormatException e) {
            updateStatus(" Error: Solo ingrese números en Tiempo y Costo.");
        } catch (Exception e) {
            updateStatus(" Error inesperado al conectar.");
        }
    }

    private void handleEliminarRuta() {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();

        if (origen == null || destino == null) {
            updateStatus(" Seleccione origen y destino para eliminar.");
            return;
        }

        if (sistemaInfo.eliminarRuta(origen, destino)) {
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            updateStatus(" Línea eliminada.");
        } else {
            updateStatus(" No se encontró la ruta o ya fue eliminada.");
        }
    }

    private void handleModificarRuta() {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();

        if (origen == null || destino == null) {
            updateStatus(" Seleccione origen y destino para modificar.");
            return;
        }

        try {
            double nuevoTiempo = Double.parseDouble(txtRutaTiempo.getText());
            double nuevoCosto = Double.parseDouble(txtRutaCosto.getText());

            Set<Ruta> rutasOrigen = sistemaInfo.getAdyacencia().get(origen.getId());

            if (rutasOrigen != null) {
                for (Ruta r : rutasOrigen) {
                    if (r.getIdDestino().equals(destino.getId())) {

                        r.getPesos().put(aw.transporte.model.CriterioPesos.TIEMPO, nuevoTiempo);
                        r.getPesos().put(aw.transporte.model.CriterioPesos.COSTO, nuevoCosto);

                        dbGestor.saveGrafo(sistemaInfo);
                        updateStatus(" Conexión actualizada exitosamente.");
                        txtRutaTiempo.clear();
                        txtRutaCosto.clear();
                        return;
                    }
                }
            }
            updateStatus(" No existe una conexión entre estas paradas.");

        } catch (NumberFormatException e) {
            updateStatus(" Por favor, ingrese valores numéricos válidos.");
        }
    }

    private void actualizarComboBoxLineas() {
        if (comboNombreLinea == null) return;

        Set<String> lineasUnicas = new HashSet<>();
        for (Set<aw.transporte.model.Ruta> rutas : sistemaInfo.getAdyacencia().values()) {
            for (aw.transporte.model.Ruta r : rutas) {
                lineasUnicas.add(r.getNombreLinea());
            }
        }

        comboNombreLinea.setItems(FXCollections.observableArrayList(lineasUnicas));
        comboNombreLinea.setEditable(true);
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
                        int transbordosReales = (int) res.costoTotal;
                        mensaje = "Ruta óptima: " + transbordosReales + " transbordo(s) necesario(s).";
                        break;
                }

                updateStatus(" Ruta encontrada | " + mensaje);

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
                    // Revisamos si el destino también tiene una ruta de vuelta hacia este origen
                    boolean tieneVuelta = false;
                    Set<Ruta> rutasDestino = adyacencia.get(d.getId());
                    if (rutasDestino != null) {
                        for (Ruta vuelta : rutasDestino) {
                            if (vuelta.getIdDestino().equals(p.getId())) {
                                tieneVuelta = true;
                                break;
                            }
                        }
                    }
                    Color colorConexion = tieneVuelta ? Color.BLACK : Color.web("#222222");
                    crearFlecha(p, d, colorConexion, 2.5, 1.0);
                }
            }
        }

        // 2. Dibujar las paradas (Nodos)
        for (Parada p : paradas.values()) {
            double x = p.getCoorx() * ZOOM + 100;
            double y = p.getCoory() * ZOOM + 100;

            Circle c = new Circle(x, y, 12, Color.web("#1e3a8a")); // Azul oscuro institucional
            c.setStroke(Color.WHITE);
            c.setStrokeWidth(2);
            c.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));
            c.setOnMouseEntered(e -> c.setCursor(javafx.scene.Cursor.HAND));

            c.setOnMousePressed(e -> c.setCursor(javafx.scene.Cursor.CLOSED_HAND));

            c.setOnMouseDragged(e -> {
                // Movimiento visual inmediato
                c.setCenterX(e.getX());
                c.setCenterY(e.getY());
            });

            c.setOnMouseReleased(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    c.setCursor(javafx.scene.Cursor.HAND);

                    double nuevaX = (c.getCenterX() - 100) / ZOOM;
                    double nuevaY = (c.getCenterY() - 100) / ZOOM;

                    p.setCoorx(nuevaX);
                    p.setCoory(nuevaY);

                    dbGestor.saveGrafo(sistemaInfo);
                    dibujarGrafoVisual(); // Redibujar para actualizar las flechas a la nueva posición
                    updateStatus("Posición de '" + p.getNombre() + "' actualizada.");
                }
            });

            c.setOnMouseClicked(event -> {
                event.consume();

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
                            actualizarComboBoxesParadas();
                            updateStatus("Parada eliminada: " + p.getNombre());
                        }
                    }
                }
            });

            Label nameLabel = new Label(p.getNombre());
            nameLabel.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                            "-fx-padding: 3 8; " +
                            "-fx-background-radius: 10; " +
                            "-fx-text-fill: #1e3a8a; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 11px; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);"
            );

            nameLabel.setMouseTransparent(true);

            nameLabel.setLayoutX(x - 20);
            nameLabel.setLayoutY(y + 18);

            Tooltip tt = new Tooltip("ID: " + p.getId());
            Tooltip.install(c, tt);

            // Añadir todo al panel
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
        l.setStrokeWidth(2.8);
        l.setOpacity(1.0);

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
        punta.setOpacity(1.0);

        graphPane.getChildren().addAll(l, punta);
    }

    private void dibujarGrafoConCamino(List<String> camino) {
        dibujarGrafoVisual();

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        for (int i = 0; i < camino.size() - 1; i++) {
            Parada p1 = paradas.get(camino.get(i));
            Parada p2 = paradas.get(camino.get(i + 1));

            if (p1 != null && p2 != null) {
                crearFlecha(p1, p2, Color.web("#27ae60"), 6.0, 1.0);
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
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

    @FXML private Tab tabUsuarios;
    @FXML private TextField txtNuevoAdminUser;
    @FXML private PasswordField txtNuevoAdminClave;
    @FXML private TextField txtNuevoAdminCorreo;
    @FXML private Button btnRegistrarAdmin;
    @FXML private Button btnCerrarSesion;
    @FXML private ComboBox<String> comboUsuariosAdmin;
    @FXML private TextField txtModAdminUser;
    @FXML private PasswordField txtModAdminClave;
    @FXML private TextField txtModAdminCorreo;
    @FXML private Button btnModificarAdmin;
    @FXML private Button btnEliminarAdmin;

    @FXML private VBox panelAlternativa;
    @FXML private Label lblInfoAlternativa;
    @FXML private Button btnSiguienteAlternativa;
    @FXML private Button btnElegirAlternativa;
    @FXML private Button btnVerTablaRutas;

    @FXML private Tab tabDiagnostico;
    @FXML private Button btnAuditoriaBFS;
    @FXML private Button btnMatrizFloyd;
    @FXML private ComboBox<CriterioPesos> comboCriterioMatriz;

    private CalculadoraRutas.ResultadoCamino rutaPrincipalMemoria;
    private List<CalculadoraRutas.ResultadoCamino> listaAlternativas;
    private int indiceAlternativaActual = 0;
    private CriterioPesos criterioMemoria;

    private boolean usuarioEsAdmin = false;
    private double xOffset = 0;
    private double yOffset = 0;

    private Grafo sistemaInfo;
    private JsonGestor dbGestor;
    private static final double ZOOM = 7.0;
    private double clickX, clickY;

    /**
     * Función: initialize
     * Objetivo: Método ejecutado automáticamente por JavaFX al cargar la vista. Inicializa las
     * bases de datos (JsonGestor), configura los eventos del ratón en el mapa (clics, arrastre)
     * y enlaza los botones con sus respectivas acciones.
     */
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
        if (comboCriterioMatriz != null) {
            comboCriterioMatriz.setItems(FXCollections.observableArrayList(CriterioPesos.values()));
        }

        btnAgregarParada.setOnAction(e -> handleAgregarParada());
        btnAgregarRuta.setOnAction(e -> handleAgregarRuta());
        btnEliminarRuta.setOnAction(e -> handleEliminarRuta());
        btnCalcular.setOnAction(e -> handleCalcularRuta());
        btnModificarParada.setOnAction(e -> handleModificarParada());
        btnModificarRuta.setOnAction(e -> handleModificarRuta());
        btnRegistrarAdmin.setOnAction(e -> handleRegistrarAdmin());
        btnCerrarSesion.setOnAction(e -> handleCerrarSesion());
        btnModificarAdmin.setOnAction(e -> handleModificarAdmin());
        btnEliminarAdmin.setOnAction(e -> handleEliminarAdmin());
        comboUsuariosAdmin.setOnAction(e -> cargarDatosEnCampos());
        btnElegirAlternativa.setOnAction(e -> handleElegirAlternativa());
        btnVerTablaRutas.setOnAction(e -> handleVerTablaRutas());
        btnSiguienteAlternativa.setOnAction(e -> handleSiguienteAlternativa());
        btnAuditoriaBFS.setOnAction(e -> handleAuditoriaBFS());
        btnMatrizFloyd.setOnAction(e -> handleMatrizFloyd());

        dibujarGrafoVisual();
        actualizarComboBoxesParadas();
        aplicarFijadorDeTexto(comboRutaOrigen, "Seleccione Origen");
        aplicarFijadorDeTexto(comboRutaDestino, "Seleccione Destino");
        aplicarFijadorDeTexto(comboCalcOrigen, "Punto de Partida");
        aplicarFijadorDeTexto(comboCalcDestino, "Punto de Llegada");
        aplicarFijadorDeTextoCriterio(comboCriterio, "Criterio de Viaje");
    }

    /**
     * Función: configurarPermisos
     * Objetivo: Ajustar dinámicamente la visibilidad de las pestañas y herramientas de la interfaz
     * según el rol del usuario logueado (Pasajero o Administrador).
     * @param esAdmin (boolean) True si el usuario tiene privilegios administrativos, false si es solo lectura.
     */
    public void configurarPermisos(boolean esAdmin) {
        this.usuarioEsAdmin = esAdmin;
        if (!esAdmin) {
            if (tabPanePrincipal != null) {
                tabPanePrincipal.getTabs().remove(tabParadas);
                tabPanePrincipal.getTabs().remove(tabConexiones);
                tabPanePrincipal.getTabs().remove(tabUsuarios);
                tabPanePrincipal.getTabs().remove(tabDiagnostico); // <-- Pasajero no ve diagnóstico
            }
            updateStatus("Modo Pasajero: Solo lectura y cálculo de rutas.");
        } else {
            updateStatus("Modo Administrador: Acceso total.");
            actualizarComboUsuarios();
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

    /**
     * Función: handleCalcularRuta
     * Objetivo: Capturar los nodos de origen/destino y el criterio seleccionado para solicitar al motor
     * lógico (CalculadoraRutas) el camino óptimo y sus alternativas. Renderiza el primer resultado en pantalla.
     */
    @FXML
    private void handleCalcularRuta() {
        try {
            Parada origen = comboCalcOrigen.getValue();
            Parada destino = comboCalcDestino.getValue();
            CriterioPesos criterio = comboCriterio.getValue();

            if (origen == null || destino == null || criterio == null) {
                updateStatus(" Seleccione origen, destino y criterio.");
                return;
            }

            if (origen.getId().equals(destino.getId())) {
                updateStatus("Error: El punto de origen y destino no pueden ser el mismo.");
                return;
            }

            criterioMemoria = criterio; // <-- GUARDAMOS EL CRITERIO EN MEMORIA
            CalculadoraRutas motor = new CalculadoraRutas();
            rutaPrincipalMemoria = motor.calcularRutaIdeal(sistemaInfo, origen.getId(), destino.getId(), criterio);

            if (rutaPrincipalMemoria != null) {
                listaAlternativas = motor.obtenerAlternativas(sistemaInfo, origen.getId(), destino.getId(), criterio);

                if (listaAlternativas != null && !listaAlternativas.isEmpty()) {
                    // Empezamos en el índice 0 (La ruta principal)
                    indiceAlternativaActual = 0;
                    panelAlternativa.setVisible(true);
                    panelAlternativa.setManaged(true);

                    // Mostramos la información de la ruta PRINCIPAL en letras verdes
                    lblInfoAlternativa.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    lblInfoAlternativa.setText("Mostrando: Principal\n" + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal));

                    // Dibujamos la ruta PRINCIPAL en verde (false = sin puntos naranjas)
                    dibujarGrafoConCaminoEspecial(rutaPrincipalMemoria.paradas, false);

                    // Apagamos el botón de fijar porque ya estamos viendo la principal
                    btnElegirAlternativa.setDisable(true);

                    updateStatus("Ruta Principal: " + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal));

                } else {
                    // Si no hay alternativas, solo dibujamos la principal y ocultamos el panel
                    dibujarGrafoConCaminoEspecial(rutaPrincipalMemoria.paradas, false);
                    panelAlternativa.setVisible(false);
                    panelAlternativa.setManaged(false);
                    updateStatus("Ruta única encontrada | " + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal));
                }

            } else {
                updateStatus(" No hay ruta disponible entre estos puntos.");
                panelAlternativa.setVisible(false);
                panelAlternativa.setManaged(false);
            }
        } catch (Exception e) {
            updateStatus(" Error en el cálculo.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSiguienteAlternativa() {
        if (listaAlternativas == null || listaAlternativas.isEmpty()) return;

        indiceAlternativaActual++;

        if (indiceAlternativaActual > listaAlternativas.size()) {
            indiceAlternativaActual = 0;
        }

        if (indiceAlternativaActual == 0) {
            dibujarGrafoConCaminoEspecial(rutaPrincipalMemoria.paradas, false);

            lblInfoAlternativa.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            lblInfoAlternativa.setText("Mostrando: Principal\n" + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal)); // Usa memoria

            btnElegirAlternativa.setDisable(true);
            updateStatus("Visualizando Ruta Principal");
        } else {
            CalculadoraRutas.ResultadoCamino alt = listaAlternativas.get(indiceAlternativaActual - 1);

            dibujarGrafoConCaminoEspecial(alt.paradas, true);

            lblInfoAlternativa.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            lblInfoAlternativa.setText("Alternativa " + indiceAlternativaActual + " de " + listaAlternativas.size() +
                    "\n" + obtenerTextoCriterio(criterioMemoria, alt.costoTotal)); // Usa memoria

            btnElegirAlternativa.setDisable(false);
            updateStatus("Visualizando Alternativa " + indiceAlternativaActual);
        }
    }

    @FXML
    private void handleElegirAlternativa() {
        // Protección anti-crashes
        if (indiceAlternativaActual == 0 || listaAlternativas == null || listaAlternativas.isEmpty()) return;

        // 1. Guardamos la ruta principal antigua temporalmente
        CalculadoraRutas.ResultadoCamino rutaVieja = rutaPrincipalMemoria;

        // 2. Fijamos la alternativa elegida como la nueva principal
        rutaPrincipalMemoria = listaAlternativas.get(indiceAlternativaActual - 1);

        // 3. ¡EL TRUCO (SWAP)! Metemos la ruta vieja en el lugar de la alternativa que acabamos de sacar
        listaAlternativas.set(indiceAlternativaActual - 1, rutaVieja);

        // 4. La dibujamos de color verde sólido (false = sin puntos)
        dibujarGrafoConCaminoEspecial(rutaPrincipalMemoria.paradas, false);
        lblInfoAlternativa.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        lblInfoAlternativa.setText("¡Ruta Fijada con Éxito!\n" + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal));

        btnElegirAlternativa.setDisable(true);
        updateStatus(" ¡Has fijado una ruta alternativa como tu camino final!");
    }

    /**
     * Función: handleVerTablaRutas
     * Objetivo: Generar y desplegar una ventana emergente (Stage) con una tabla interactiva (TableView)
     * que compara detalladamente la ruta principal y las alternativas (Algoritmo de Yen). Permite fijar una ruta.
     */
    @FXML
    private void handleVerTablaRutas() {
        if (rutaPrincipalMemoria == null) {
            updateStatus(" Debes calcular una ruta primero para ver la tabla.");
            return;
        }

        javafx.stage.Stage stageTabla = new javafx.stage.Stage();
        stageTabla.setTitle("Comparativa de Rutas Disponibles");

        TableView<FilaRuta> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FilaRuta, String> colOpcion = new TableColumn<>("Tipo de Ruta");
        colOpcion.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().opcion()));
        colOpcion.setPrefWidth(120);
        colOpcion.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        TableColumn<FilaRuta, String> colCamino = new TableColumn<>("Recorrido (Paradas)");
        colCamino.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().camino()));
        colCamino.setPrefWidth(350);

        TableColumn<FilaRuta, String> colValor = new TableColumn<>("Costo / Tiempo");
        colValor.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().valor()));
        colValor.setPrefWidth(120);
        colValor.setStyle("-fx-alignment: CENTER; -fx-text-fill: #e67e22; -fx-font-weight: bold;");

        tabla.getColumns().addAll(colOpcion, colCamino, colValor);

        ObservableList<FilaRuta> datosTabla = FXCollections.observableArrayList();

        // Le ponemos ID 0 a la ruta principal
        String caminoPrincipal = String.join(" ➔ ", rutaPrincipalMemoria.paradas);
        datosTabla.add(new FilaRuta(0, "Plan A (Principal)", caminoPrincipal, obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal)));

        if (listaAlternativas != null) {
            for (int i = 0; i < listaAlternativas.size(); i++) {
                CalculadoraRutas.ResultadoCamino alt = listaAlternativas.get(i);
                String caminoAlt = String.join(" ➔ ", alt.paradas);
                // Le ponemos ID del 1 en adelante a las alternativas
                datosTabla.add(new FilaRuta(i + 1, "Alternativa " + (i + 1), caminoAlt, obtenerTextoCriterio(criterioMemoria, alt.costoTotal)));
            }
        }

        tabla.setItems(datosTabla);

        Button btnFijarDesdeTabla = new Button("Fijar Ruta Seleccionada");
        btnFijarDesdeTabla.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-cursor: hand;");
        btnFijarDesdeTabla.setDisable(true); // Apagado hasta que toquen una fila

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, viejaSeleccion, nuevaSeleccion) -> {
            if (nuevaSeleccion != null) {
                btnFijarDesdeTabla.setDisable(false);
            }
        });

        btnFijarDesdeTabla.setOnAction(e -> {
            FilaRuta filaElegida = tabla.getSelectionModel().getSelectedItem();
            if (filaElegida != null) {
                // Si el ID es mayor a 0, significa que eligió una alternativa
                if (filaElegida.id() > 0) {
                    // --- ¡AQUÍ ESTÁ EL TRUCO (SWAP)! ---
                    CalculadoraRutas.ResultadoCamino rutaVieja = rutaPrincipalMemoria;
                    rutaPrincipalMemoria = listaAlternativas.get(filaElegida.id() - 1);
                    listaAlternativas.set(filaElegida.id() - 1, rutaVieja);
                }

                // 1. Dibujamos en verde la ruta seleccionada en el mapa grande
                dibujarGrafoConCaminoEspecial(rutaPrincipalMemoria.paradas, false);

                // 2. Actualizamos el panel principal
                lblInfoAlternativa.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                lblInfoAlternativa.setText("¡Ruta Fijada con Éxito!\n" + obtenerTextoCriterio(criterioMemoria, rutaPrincipalMemoria.costoTotal));
                btnElegirAlternativa.setDisable(true);
                updateStatus(" ¡Has fijado tu camino desde la tabla comparativa!");

                // 3. Cerramos la ventanita de la tabla
                stageTabla.close();
            }
        });

        // --- DISEÑO DE LA VENTANA ---
        VBox layoutTabla = new VBox(10);
        layoutTabla.setStyle("-fx-padding: 20; -fx-background-color: #f1f2f6;");

        Label lblTitulo = new Label("Análisis de Rutas Alternativas");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        Label lblSubtitulo = new Label("Haz clic en una ruta de la lista y presiona el botón para seleccionarla.");
        lblSubtitulo.setStyle("-fx-text-fill: #7f8fa6;");

        javafx.scene.layout.HBox panelBoton = new javafx.scene.layout.HBox(btnFijarDesdeTabla);
        panelBoton.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 10 0 0 0;");

        layoutTabla.getChildren().addAll(lblTitulo, lblSubtitulo, tabla, panelBoton);

        javafx.scene.Scene scene = new javafx.scene.Scene(layoutTabla, 750, 400);
        stageTabla.setScene(scene);
        stageTabla.show();
    }

    /**
     * Función: handleAuditoriaBFS
     * Objetivo: Ejecutar el algoritmo de búsqueda en anchura (BFS) desde un nodo aleatorio para detectar
     * paradas huérfanas o desconectadas en el sistema, y mostrar el diagnóstico en un cuadro de alerta.
     */
    @FXML
    private void handleAuditoriaBFS() {
        if (sistemaInfo.getParadas().isEmpty()) {
            updateStatus("El mapa está vacío, no hay nada que auditar.");
            return;
        }

        // Tomamos cualquier parada al azar para iniciar el escaneo
        String paradaInicio = sistemaInfo.getParadas().keySet().iterator().next();

        CalculadoraRutas motor = new CalculadoraRutas();
        Set<String> huerfanas = motor.auditoriaConectividad(sistemaInfo, paradaInicio);

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Reporte de Auditoría (BFS)");

        if (huerfanas.isEmpty()) {
            alerta.setHeaderText(" Estado de la red: SALUDABLE");
            alerta.setContentText("Todas las paradas están correctamente conectadas. No hay puntos ciegos en el sistema.");
        } else {
            alerta.setAlertType(Alert.AlertType.WARNING);
            alerta.setHeaderText("️ Advertencia: Se encontraron paradas huérfanas");

            // Traducimos los ids a nombres reales para que sea legible
            List<String> nombresHuerfanas = new ArrayList<>();
            for (String id : huerfanas) {
                nombresHuerfanas.add(sistemaInfo.getParadas().get(id).getNombre());
            }

            alerta.setContentText("Las siguientes paradas no pueden ser alcanzadas desde el nodo principal:\n" + String.join(", ", nombresHuerfanas));
        }
        alerta.showAndWait();
    }

    /**
     * Función: handleMatrizFloyd
     * Objetivo: Invocar el algoritmo de Floyd-Warshall para calcular las distancias mínimas globales de
     * todos contra todos, y renderizar el resultado construyendo columnas dinámicamente en una tabla JavaFX.
     */

    @FXML
    private void handleMatrizFloyd() {
        if (sistemaInfo.getParadas().isEmpty()) return;
        CriterioPesos criterio = comboCriterioMatriz.getValue();
        if (criterio == null) {
            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alerta.setTitle("Acción Requerida");
            alerta.setHeaderText(null);
            alerta.setContentText("️ Por favor, seleccione un criterio (Tiempo, Costo, etc.) en el menú desplegable antes de generar la matriz global.");
            alerta.showAndWait();
            return; // Detenemos la ejecución aquí si no hay criterio seleccionado
        }

        CalculadoraRutas motor = new CalculadoraRutas();
        CalculadoraRutas.ResultadoMatrizGlobal resultadoGlobal = motor.calcularRutasGlobales(sistemaInfo, criterio);

        javafx.stage.Stage stageMatriz = new javafx.stage.Stage();
        stageMatriz.setTitle("Matriz Global de Rutas - Criterio: " + criterio);

        TableView<String[]> tabla = new TableView<>();

        TableColumn<String[], String> colOrigen = new TableColumn<>("Origen \\ Destino");
        colOrigen.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        colOrigen.setStyle("-fx-font-weight: bold; -fx-background-color: #ecf0f1;");
        tabla.getColumns().add(colOrigen);

        int totalNodos = resultadoGlobal.indiceAParadaId.size();

        for (int i = 0; i < totalNodos; i++) {
            final int colIndex = i + 1;
            String nombreDestino = sistemaInfo.getParadas().get(resultadoGlobal.indiceAParadaId.get(i)).getNombre();

            TableColumn<String[], String> colDinamica = new TableColumn<>(nombreDestino);
            colDinamica.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[colIndex]));
            colDinamica.setStyle("-fx-alignment: CENTER;");
            tabla.getColumns().add(colDinamica);
        }

        ObservableList<String[]> filas = FXCollections.observableArrayList();
        for (int fila = 0; fila < totalNodos; fila++) {
            String[] datosFila = new String[totalNodos + 1];
            datosFila[0] = sistemaInfo.getParadas().get(resultadoGlobal.indiceAParadaId.get(fila)).getNombre(); // Nombre origen

            for (int col = 0; col < totalNodos; col++) {
                double valor = resultadoGlobal.matrizDistancias[fila][col];
                if (valor == Double.MAX_VALUE) {
                    datosFila[col + 1] = "∞";
                } else if (valor == 0.0) {
                    datosFila[col + 1] = "-";
                } else {
                    datosFila[col + 1] = String.format("%.1f", valor);
                }
            }
            filas.add(datosFila);
        }

        tabla.setItems(filas);

        VBox layout = new VBox(tabla);
        VBox.setVgrow(tabla, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 900, 500);
        stageMatriz.setScene(scene);
        stageMatriz.show();
    }

    private void dibujarGrafoConCaminoEspecial(List<String> camino, boolean esAlternativa) {
        dibujarGrafoVisual(); // Limpia base
        Color colorCamino = esAlternativa ? Color.web("#e67e22") : Color.web("#27ae60");

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        for (int i = 0; i < camino.size() - 1; i++) {
            Parada p1 = paradas.get(camino.get(i));
            Parada p2 = paradas.get(camino.get(i + 1));
            if (p1 != null && p2 != null) {
                crearFlecha(p1, p2, colorCamino, 6.0, 1.0, esAlternativa);
            }
        }
    }

    private String obtenerTextoCriterio(CriterioPesos criterio, double valor) {
        return switch (criterio) {
            case TIEMPO -> "Tiempo: " + valor + " mins.";
            case COSTO -> "Costo: $" + valor;
            case DISTANCIA -> "Distancia: " + String.format("%.2f", valor) + " Km.";
            case TRANSBORDOS -> "Transbordos: " + (int) valor;
        };
    }

    private void handleRegistrarAdmin() {
        String user = txtNuevoAdminUser.getText().trim();
        String clave = txtNuevoAdminClave.getText().trim();
        String correo = txtNuevoAdminCorreo.getText().trim();

        if (user.isEmpty() || clave.isEmpty() || correo.isEmpty()) {
            updateStatus(" Por favor, llene todos los campos.");
            return;
        }

        // GUARDAMOS EN EL JSON
        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();

        // Verificamos que el usuario no exista ya
        for (aw.transporte.model.Usuario u : lista) {
            if (u.getUsername().equals(user)) {
                updateStatus(" Error: El usuario ya existe.");
                return;
            }
        }

        lista.add(new aw.transporte.model.Usuario(user, clave, correo));
        gestor.guardarUsuarios(lista);

        updateStatus(" ¡Éxito! Usuario '" + user + "' registrado.");
        actualizarComboUsuarios();
        txtNuevoAdminUser.clear(); txtNuevoAdminClave.clear(); txtNuevoAdminCorreo.clear();
    }

    // Actualiza el ComboBox cada vez que hacemos un cambio
    public void actualizarComboUsuarios() {
        if (comboUsuariosAdmin == null) return;
        comboUsuariosAdmin.getItems().clear();
        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();
        for (aw.transporte.model.Usuario u : lista) {
            comboUsuariosAdmin.getItems().add(u.getUsername());
        }
    }

    // Llena los campos de texto cuando seleccionas un usuario en el ComboBox
    private void cargarDatosEnCampos() {
        String usuarioSeleccionado = comboUsuariosAdmin.getValue();
        if (usuarioSeleccionado == null) return;

        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();

        for (aw.transporte.model.Usuario u : lista) {
            if (u.getUsername().equals(usuarioSeleccionado)) {
                txtModAdminUser.setText(u.getUsername());
                txtModAdminClave.setText(u.getPassword());
                txtModAdminCorreo.setText(u.getCorreo());
                break;
            }
        }
    }

    private void handleModificarAdmin() {
        String originalUser = comboUsuariosAdmin.getValue();
        if (originalUser == null) {
            updateStatus(" Por favor, seleccione un usuario de la lista.");
            return;
        }

        String newUser = txtModAdminUser.getText().trim();
        String newClave = txtModAdminClave.getText().trim();
        String newCorreo = txtModAdminCorreo.getText().trim();

        if (newUser.isEmpty() || newClave.isEmpty() || newCorreo.isEmpty()) {
            updateStatus(" Todos los campos son obligatorios para modificar.");
            return;
        }

        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();

        // Buscamos el usuario viejo y lo reemplazamos por el nuevo
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getUsername().equals(originalUser)) {
                lista.set(i, new aw.transporte.model.Usuario(newUser, newClave, newCorreo));
                break;
            }
        }

        gestor.guardarUsuarios(lista);
        updateStatus(" ¡Usuario modificado exitosamente!");
        actualizarComboUsuarios();
    }

    private void handleEliminarAdmin() {
        String targetUser = comboUsuariosAdmin.getValue();
        if (targetUser == null) {
            updateStatus(" Seleccione el usuario que desea eliminar.");
            return;
        }

        if (targetUser.equals("admin")) {
            updateStatus(" Acción denegada: No puede eliminar al administrador principal.");
            return;
        }

        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();

        // Eliminamos si el nombre coincide
        lista.removeIf(u -> u.getUsername().equals(targetUser));
        gestor.guardarUsuarios(lista);

        updateStatus(" Usuario eliminado del sistema.");
        txtModAdminUser.clear(); txtModAdminClave.clear(); txtModAdminCorreo.clear();
        actualizarComboUsuarios();
    }

    private void handleCerrarSesion() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Login.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Inicio de Sesión - Sistema de Rutas");

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            stage.setScene(scene);

            stage.setResizable(false);
            stage.sizeToScene();
            stage.show();

            // Cierra la ventana principal gigante
            javafx.stage.Stage mainStage = (javafx.stage.Stage) btnCerrarSesion.getScene().getWindow();
            mainStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error al intentar cerrar sesión.");
        }
    }

    /**
     * Función: dibujarGrafoVisual
     * Objetivo: Renderizar en el lienzo de la interfaz (graphPane) todas las paradas (nodos circulares)
     * y sus conexiones (aristas) almacenadas en la estructura de datos del Grafo actual.
     */
    private void dibujarGrafoVisual() {
        if (graphPane == null) return;
        graphPane.getChildren().clear();

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        Map<String, Set<Ruta>> adyacencia = sistemaInfo.getAdyacencia();

        // 1. Dibujar las conexiones (Rutas / Flechas)
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
                    crearFlecha(p, d, colorConexion, 2.5, 1.0, false);
                }
            }
        }

        // 2. Dibujar las paradas (Nodos / Círculos)
        for (Parada p : paradas.values()) {
            double x = p.getCoorx() * ZOOM + 100;
            double y = p.getCoory() * ZOOM + 100;

            Circle c = new Circle(x, y, 12, Color.web("#1e3a8a"));
            c.setStroke(Color.WHITE);
            c.setStrokeWidth(2);
            c.setEffect(new javafx.scene.effect.DropShadow(5, Color.BLACK));


            // 1. Mostrar la manito solo si es admin
            c.setOnMouseEntered(e -> {
                if (usuarioEsAdmin) c.setCursor(javafx.scene.Cursor.HAND);
            });

            // 2. Al presionar el clic
            c.setOnMousePressed(e -> {
                if (!usuarioEsAdmin) return; // BLOQUEO PASAJERO
                c.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            });

            // 3. Al arrastrar
            c.setOnMouseDragged(e -> {
                if (!usuarioEsAdmin) return; // BLOQUEO PASAJERO

                // Movimiento visual inmediato
                c.setCenterX(e.getX());
                c.setCenterY(e.getY());
            });

            // 4. Al soltar el clic (Guardar en la base de datos)
            c.setOnMouseReleased(e -> {
                if (!usuarioEsAdmin) return; // BLOQUEO PASAJERO

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

            // 5. Al hacer Clic Derecho (Eliminar Parada)
            c.setOnMouseClicked(event -> {
                event.consume();

                if (!usuarioEsAdmin) return; // ¡BLOQUEO PASAJERO PARA QUE NO BORRE PARADAS!

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

    /**
     * Función: crearFlecha
     * Objetivo: Dibujar un vector direccional (línea con punta de flecha) entre dos nodos en el mapa.
     * @param origen        (Parada) Nodo geométrico de salida.
     * @param destino       (Parada) Nodo geométrico de llegada.
     * @param color         (Color) Color de la arista según el estado (verde = óptima, naranja = alternativa).
     * @param grosor        (double) Ancho del trazo de la línea.
     * @param opacidad      (double) Nivel de transparencia de la conexión.
     * @param esAlternativa (boolean) Si es true, el trazo de la línea se renderiza de forma punteada.
     */
    private void crearFlecha(Parada origen, Parada destino, Color color, double grosor, double opacidad, boolean esAlternativa) {
        double x1 = origen.getCoorx() * ZOOM + 100;
        double y1 = origen.getCoory() * ZOOM + 100;
        double x2 = destino.getCoorx() * ZOOM + 100;
        double y2 = destino.getCoory() * ZOOM + 100;

        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(color);
        l.setStrokeWidth(grosor);
        l.setOpacity(opacidad);

        // Si es una ruta alternativa, la hacemos punteada
        if (esAlternativa) {
            l.getStrokeDashArray().addAll(10d, 10d);
        }

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
                crearFlecha(p1, p2, Color.web("#27ae60"), 6.0, 1.0, false);
            }
        }
    }

    private void dibujarPuntoTemporal(double x, double y) {
        dibujarGrafoVisual();
        Circle preview = new Circle(x, y, 8, Color.web("#2f3542", 0.5));
        graphPane.getChildren().add(preview);
    }

    private void updateStatus(String msg) { if (lblEstado != null) lblEstado.setText(msg); }// Molde de datos para la Tabla Comparativa (Ahora con ID secreto)

    /**
     * Record: FilaRuta
     * Objetivo: Estructura de datos inmutable (Molde) utilizada para rellenar las celdas de la
     * Tabla Comparativa de Rutas. Incluye un ID secreto para vinculación interna.
     */
    public record FilaRuta(int id, String opcion, String camino, String valor) {}
}
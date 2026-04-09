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

    @FXML private CheckBox chkModoEdicion;

    private Ruta rutaEnEdicion = null;
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
    private Map<String, Color> mapaColoresLineas = new HashMap<>();
    private double leyendaX = 20;
    private double leyendaY = 20;

    /**
     * Función: initialize
     * Objetivo: Método principal del ciclo de vida de JavaFX. Se ejecuta automáticamente
     * justo después de que el archivo FXML ha sido cargado. Su propósito es preparar el
     * estado inicial de la aplicación.
     */
    @FXML
    public void initialize() {
        // Inicialización de la base de datos
        dbGestor = new JsonGestor();
        sistemaInfo = dbGestor.fetchGrafoData();

        // Configuración del fondo del mapa
        try {
            String imagePath = getClass().getResource("/mapa_fondo.png").toExternalForm();
            anchorMapa.setStyle(
                    "-fx-background-image: url('" + imagePath + "'); " + "-fx-background-size: cover; " + "-fx-background-position: center; " + "-fx-opacity: 0.9; " + "-fx-background-color: #ffffff;"
            );
        } catch (Exception e) {
            updateStatus("Advertencia: Imagen de fondo no cargada.");
        }

        if (comboCriterio != null) {
            comboCriterio.setItems(FXCollections.observableArrayList(CriterioPesos.values()));
        }
        if (comboCriterioMatriz != null) {
            comboCriterioMatriz.setItems(FXCollections.observableArrayList(CriterioPesos.values()));
        }

        // Eventos del ratón en el lienzo (Mapa)
        graphPane.setOnMouseClicked(event -> {
            clickX = (event.getX() - 100) / ZOOM;
            clickY = (event.getY() - 100) / ZOOM;
            dibujarPuntoTemporal(event.getX(), event.getY());
            updateStatus("Ubicación marcada.");
        });

        // Configuración del panel flotante
        if (panelFlotante != null) {
            // Cambiamos el cursor a una mano abierta para indicar que se puede mover
            panelFlotante.setStyle("-fx-cursor: open-hand;");

            panelFlotante.setOnMousePressed(event -> {
                panelFlotante.setStyle("-fx-cursor: closed-hand;");
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
            updateStatus("Error interno: Interfaz de panel flotante no encontrada.");
        }

        // Enlace de Botones con Funciones
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
        btnAuditoriaBFS.setOnAction(e -> handleAuditoriaBFS());
        btnMatrizFloyd.setOnAction(e -> handleMatrizFloyd());
        comboRutaOrigen.setOnAction(e -> evaluarEstadoDeCalle());
        comboRutaDestino.setOnAction(e -> evaluarEstadoDeCalle());
        comboRutaOrigen.setOnAction(e -> evaluarEstadoDeCalle());
        comboRutaDestino.setOnAction(e -> evaluarEstadoDeCalle());
        comboNombreLinea.setOnAction(e -> aplicarAutoCompletado());
        if (chkModoEdicion != null) {
            chkModoEdicion.setOnAction(e -> alternarModo());
        }
        // Renderizado inicial y aplicación de placeholders a ComboBoxes
        dibujarGrafoVisual();
        actualizarComboBoxesParadas();
        actualizarComboBoxLineas();
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
     * @param esAdmin (boolean) True si el usuario tiene privilegios administrativos.
     */
    public void configurarPermisos(boolean esAdmin) {
        this.usuarioEsAdmin = esAdmin;
        if (!esAdmin) {
            if (tabPanePrincipal != null) {
                tabPanePrincipal.getTabs().remove(tabParadas);
                tabPanePrincipal.getTabs().remove(tabConexiones);
                tabPanePrincipal.getTabs().remove(tabUsuarios);
                tabPanePrincipal.getTabs().remove(tabDiagnostico);
            }
            updateStatus("Modo Pasajero: Solo lectura y cálculo de rutas.");
        } else {
            updateStatus("Modo Administrador: Acceso total.");
            actualizarComboUsuarios();
        }
    }

    /**
     * Función: actualizarComboBoxesParadas
     * Objetivo: Refrescar todos los menús desplegables de paradas en la interfaz, cargándolos
     * con los datos más recientes del grafo y ordenándolos alfabéticamente por nombre.
     */
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

    /**
     * Función: limpiarCamposParadas
     * Objetivo: Borrar el contenido del campo de texto utilizado para nombrar nuevas paradas.
     */
    private void limpiarCamposParadas() {
        txtParadaNombre.clear();
    }

    /**
     * Función: aplicarFijadorDeTexto
     * Objetivo: Inyectar un texto por defecto (placeholder) en un ComboBox de Paradas cuando no hay ninguna selección activa.
     * @param combo         (ComboBox<Parada>) El elemento UI a modificar.
     * @param textoFantasma (String) El texto indicativo a mostrar (ej. "Seleccione Origen").
     */
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

    /**
     * Función: aplicarFijadorDeTextoCriterio
     * Objetivo: Inyectar un texto por defecto en el ComboBox de Criterios (Tiempo, Costo, etc.).
     * @param combo         (ComboBox<CriterioPesos>) El elemento UI a modificar.
     * @param textoFantasma (String) El texto indicativo a mostrar.
     */
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

    /**
     * Función: handleAgregarParada
     * Objetivo: Crear una nueva entidad 'Parada' en la estructura del Grafo utilizando
     * las coordenadas del último clic en el mapa y guardarla en la base de datos JSON asegurando que no existan nombres duplicados.
     */
    private void handleAgregarParada() {
        String nombreParada = txtParadaNombre.getText().trim();

        if (nombreParada.isEmpty()) {
            updateStatus("Olvidaste ponerle nombre a la parada.");
            return;
        }

        boolean nombreExiste = sistemaInfo.getParadas().values().stream().anyMatch(p -> p.getNombre().equalsIgnoreCase(nombreParada));

        if (nombreExiste) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Validación de Parada");
            alerta.setHeaderText("Nombre Duplicado");
            alerta.setContentText("Ya existe una parada con el nombre '" + nombreParada + "'. Por favor, elige otro.");
            alerta.showAndWait();
            return; // Detenemos la creación de la parada
        }

        try {
            String nuevoId = sistemaInfo.generarId();
            sistemaInfo.agregarParada(new Parada(nuevoId, nombreParada, clickX, clickY));
            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            actualizarComboBoxesParadas();

            updateStatus("Parada guardada automáticamente como: " + nuevoId);
            limpiarCamposParadas();

        } catch (Exception e) {
            updateStatus("Error crítico al guardar la parada.");
        }
    }

    /**
     * Función: handleModificarParada
     * Objetivo: Actualizar el nombre de una parada validando que el nuevo nombre no le pertenezca a otra.
     */
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

        boolean nombreExiste = sistemaInfo.getParadas().values().stream().anyMatch(p -> !p.getId().equals(paradaSeleccionada.getId()) && p.getNombre().equalsIgnoreCase(nuevoNombre));

        if (nombreExiste) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Validación de Parada");
            alerta.setHeaderText("Nombre Duplicado");
            alerta.setContentText("Ya existe otra parada llamada '" + nuevoNombre + "'.");
            alerta.showAndWait();
            return; // Detenemos la modificación
        }

        paradaSeleccionada.setNombre(nuevoNombre);

        dbGestor.saveGrafo(sistemaInfo);
        dibujarGrafoVisual();
        actualizarComboBoxesParadas();

        txtNuevoNombreParada.clear();
        comboParadaModificar.getSelectionModel().clearSelection();
        updateStatus(" Parada modificada exitosamente a: " + nuevoNombre);
    }

    /**
     * Función: validContinuidad
     * Objetivo: Asegurar que los nuevos tramos de una línea existente se conecten a ella físicamente,
     * evitando la creación de "islas" desconectadas en el sistema de transporte.
     * @param idOrigen    (String) ID del nodo de inicio propuesto.
     * @param idDestino   (String) ID del nodo de destino propuesto.
     * @param nombreLinea (String) Nombre de la línea a evaluar.
     * @return            (boolean) True si la conexión es legal, False si rompe la topología de red.
     */
    private boolean validContinuidad(String idOrigen, String idDestino, String nombreLinea) {
        if (sistemaInfo == null || sistemaInfo.getAdyacencia() == null) return true;

        boolean existeEnSistema = false;
        boolean origenConectado = false;
        boolean destinoConectado = false;

        // Recorremos todo el grafo buscando si la línea ya existe
        for (Map.Entry<String, Set<Ruta>> entry : sistemaInfo.getAdyacencia().entrySet()) {
            String nodoActual = entry.getKey();

            for (Ruta ruta : entry.getValue()) {
                // Comparamos ignorando mayúsculas y espacios extra
                if (ruta.getNombreLinea().trim().equalsIgnoreCase(nombreLinea.trim())) {
                    existeEnSistema = true; // ¡La línea ya existe en el mapa!

                    // Verificamos si este tramo toca el origen o destino que intentamos conectar
                    if (nodoActual.equals(idOrigen) || ruta.getIdDestino().equals(idOrigen)) {
                        origenConectado = true;
                    }
                    if (nodoActual.equals(idDestino) || ruta.getIdDestino().equals(idDestino)) {
                        destinoConectado = true;
                    }
                }
            }
        }

        // Regla: Si no existe, se permite (nueva línea). Si existe, el origen o el destino deben tocarla.
        return !existeEnSistema || origenConectado || destinoConectado;
    }

    /**
     * Función: handleAgregarRuta
     * Objetivo: Capturar datos de la UI para crear una nueva arista dirigida (conexión) entre dos paradas.
     * Calcula la distancia Euclidiana, valida la regla de continuidad de línea y guarda en JSON.
     */
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

//            Agregamos esta validacion
            if (!validContinuidad(origen.getId(), destino.getId(), nombreLinea)) {
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Error de Continuidad de Red");
                alerta.setHeaderText(" Fragmentación de Línea Detectada");
                alerta.setContentText("La '" + nombreLinea + "' ya existe en otra parte del mapa. No puedes crear un tramo aislado.\n\nPor favor, conéctalo a un nodo de la línea existente o utiliza un nombre diferente para esta nueva ruta.");
                alerta.showAndWait();
                updateStatus(" Conexión cancelada: Violación de continuidad.");
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

    private void handleModificarRuta() {
        if (rutaEnEdicion == null) {
            updateStatus(" Para actualizar, primero seleccione una línea existente en esta calle.");
            return;
        }

        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();
        String nuevaLinea = comboNombreLinea.getValue();

        try {
            boolean cambioRealizado = false;

            if (nuevaLinea != null && !nuevaLinea.trim().isEmpty() && !rutaEnEdicion.getNombreLinea().equalsIgnoreCase(nuevaLinea)) {
                if (!validContinuidad(origen.getId(), destino.getId(), nuevaLinea)) {
                    Alert alerta = new Alert(Alert.AlertType.ERROR);
                    alerta.setTitle("Error de Continuidad");
                    alerta.setContentText("No puedes cambiar el nombre a '" + nuevaLinea + "' porque fragmentarías el mapa.");
                    alerta.showAndWait();
                    return;
                }
                rutaEnEdicion.setNombreLinea(nuevaLinea);
                cambioRealizado = true;
            }

            String strTiempo = txtRutaTiempo.getText().trim();
            String strCosto = txtRutaCosto.getText().trim();

            if (!strTiempo.isEmpty()) {
                double nuevoTiempo = Double.parseDouble(strTiempo);
                if (nuevoTiempo < 0) throw new NumberFormatException();
                rutaEnEdicion.getPesos().put(aw.transporte.model.CriterioPesos.TIEMPO, nuevoTiempo);
                cambioRealizado = true;
            }

            if (!strCosto.isEmpty()) {
                double nuevoCosto = Double.parseDouble(strCosto);
                if (nuevoCosto < 0) throw new NumberFormatException();
                rutaEnEdicion.getPesos().put(aw.transporte.model.CriterioPesos.COSTO, nuevoCosto);
                cambioRealizado = true;
            }

            if (cambioRealizado) {
                dbGestor.saveGrafo(sistemaInfo);
                dibujarGrafoVisual();
                actualizarComboBoxLineas();
                evaluarEstadoDeCalle(" Conexión actualizada exitosamente.");
            } else {
                updateStatus(" No se ingresaron nuevos valores para modificar.");
            }

        } catch (NumberFormatException e) {
            updateStatus(" Error: Ingrese valores numéricos válidos en tiempo y costo.");
        }
    }

    /**
     * Función: handleEliminarRuta
     * Objetivo: Eliminar una conexión. Implementa una Validación Predictiva (What-If):
     * Borra la ruta temporalmente en memoria, ejecuta BFS, y si detecta que la red
     * se fractura, lanza una advertencia antes de guardar los cambios en disco.
     */
    private void handleEliminarRuta() {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();

        if (origen == null || destino == null) {
            updateStatus("Seleccione origen y destino para eliminar.");
            return;
        }

        if (rutaEnEdicion != null) {
            sistemaInfo.getAdyacencia().get(origen.getId()).remove(rutaEnEdicion);

            CalculadoraRutas motor = new CalculadoraRutas();
            String paradaInicio = sistemaInfo.getParadas().keySet().iterator().next();
            Set<String> huerfanas = motor.auditoriaConectividad(sistemaInfo, paradaInicio);

            if (!huerfanas.isEmpty()) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Advertencia Critica de Conectividad");
                alerta.setHeaderText("Riesgo de Paradas Aisladas");

                alerta.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

                alerta.setContentText("Si eliminas la linea '" + rutaEnEdicion.getNombreLinea() + "', se detecta que " + huerfanas.size() + " parada(s) quedaran completamente desconectadas del mapa.\n\n¿Estas seguro de que deseas aplicar este cierre vial?");

                ButtonType btnContinuar = new ButtonType("Si, eliminar", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alerta.getButtonTypes().setAll(btnContinuar, btnCancelar);

                Optional<ButtonType> resultado = alerta.showAndWait();

                if (resultado.isPresent() && resultado.get() == btnCancelar) {
                    sistemaInfo.getAdyacencia().get(origen.getId()).add(rutaEnEdicion);
                    updateStatus("Eliminacion cancelada por seguridad.");
                    return;
                }
            }

            dbGestor.saveGrafo(sistemaInfo);
            dibujarGrafoVisual();
            evaluarEstadoDeCalle("Linea '" + rutaEnEdicion.getNombreLinea() + "' eliminada con exito.");

        } else {
            updateStatus("Seleccione una linea existente de la lista para poder eliminarla.");
        }
    }

    /**
     * Función: actualizarComboBoxLineas
     * Objetivo: Barrer el grafo en busca de todos los nombres de líneas únicos (HashSet)
     * para mantener el menú desplegable de "Conexiones" siempre actualizado.
     */
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

//    Sobrecarga de metodos
    /**
     * Llamada normal cuando el usuario solo está curioseando las calles.
     */
    private void evaluarEstadoDeCalle() {
        evaluarEstadoDeCalle(null);
    }

    /**
     * Motor principal: Refresca la UI pero respeta si le pasamos un mensaje de éxito.
     */
    private void evaluarEstadoDeCalle(String mensajeExito) {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();
        rutaEnEdicion = null;
        txtRutaTiempo.clear();
        txtRutaCosto.clear();
        actualizarComboBoxLineas();

        if (origen == null || destino == null || chkModoEdicion == null) return;

        Set<Ruta> rutasOrigen = sistemaInfo.getAdyacencia().get(origen.getId());
        boolean hayLineas = false;

        if (rutasOrigen != null) {
            for (Ruta r : rutasOrigen) {
                if (r.getIdDestino().equals(destino.getId())) {
                    hayLineas = true;
                    break;
                }
            }
        }

        if (hayLineas) {
            chkModoEdicion.setVisible(true);
            chkModoEdicion.setSelected(false);
            if (mensajeExito != null) {
                updateStatus(mensajeExito);
            } else {
                updateStatus(" Hay líneas aquí. Marque la casilla arriba si desea editarlas.");
            }
        } else {
            chkModoEdicion.setVisible(false);
            if (mensajeExito != null) {
                updateStatus(mensajeExito);
            } else {
                updateStatus(" Calle vacía. Ingrese los datos para crear una nueva línea.");
            }
        }
    }

    /**
     * Qué pasa cuando el usuario le da clic al CheckBox.
     */
    private void alternarModo() {
        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();
        if (origen == null || destino == null) return;

        if (chkModoEdicion.isSelected()) {
            Set<Ruta> rutasOrigen = sistemaInfo.getAdyacencia().get(origen.getId());
            List<String> lineasActivas = new ArrayList<>();
            for (Ruta r : rutasOrigen) {
                if (r.getIdDestino().equals(destino.getId())) {
                    lineasActivas.add(r.getNombreLinea());
                }
            }
            comboNombreLinea.setItems(FXCollections.observableArrayList(lineasActivas));
            comboNombreLinea.setPromptText("Elija la línea a editar...");
            updateStatus(" MODO EDICIÓN: Elija una línea para autocompletar.");
        } else {
            actualizarComboBoxLineas();
            rutaEnEdicion = null;
            txtRutaTiempo.clear();
            txtRutaCosto.clear();
            updateStatus(" MODO CREACIÓN: Escriba o seleccione una línea.");
        }
    }

    /**
     * Auto-completa los datos solo si el CheckBox está marcado y elige una línea.
     */
    private void aplicarAutoCompletado() {
        if (chkModoEdicion != null && !chkModoEdicion.isSelected()) {
            rutaEnEdicion = null;
            return;
        }

        Parada origen = comboRutaOrigen.getValue();
        Parada destino = comboRutaDestino.getValue();
        String linea = comboNombreLinea.getValue();

        if (origen == null || destino == null || linea == null) return;

        Set<Ruta> rutas = sistemaInfo.getAdyacencia().get(origen.getId());
        if (rutas != null) {
            for (Ruta r : rutas) {
                if (r.getIdDestino().equals(destino.getId()) && r.getNombreLinea().equalsIgnoreCase(linea)) {
                    rutaEnEdicion = r;
                    txtRutaTiempo.setText(String.valueOf(r.getPesos().get(aw.transporte.model.CriterioPesos.TIEMPO)));
                    txtRutaCosto.setText(String.valueOf(r.getPesos().get(aw.transporte.model.CriterioPesos.COSTO)));
                    updateStatus(" Datos cargados. Modifique y guarde.");
                    return;
                }
            }
        }
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

    /**
     * Función: handleSiguienteAlternativa
     * Objetivo: Ciclar a través de la lista de rutas alternativas (generadas por el Alg. de Yen)
     * en la interfaz, actualizando el mapa con trazos punteados (color naranja) de forma dinámica.
     */
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
            lblInfoAlternativa.setText("Alternativa " + indiceAlternativaActual + " de " + listaAlternativas.size() + "\n" + obtenerTextoCriterio(criterioMemoria, alt.costoTotal)); // Usa memoria

            btnElegirAlternativa.setDisable(false);
            updateStatus("Visualizando Alternativa " + indiceAlternativaActual);
        }
    }

    /**
     * Función: handleElegirAlternativa
     * Objetivo: Sobrescribir la ruta principal actual con la alternativa visualizada en ese momento
     * (realizando un intercambio en las variables de memoria), para confirmarla como el plan de viaje oficial.
     */
    @FXML
    private void handleElegirAlternativa() {
        if (indiceAlternativaActual == 0 || listaAlternativas == null || listaAlternativas.isEmpty()) return;

        // 1. Guardamos la ruta principal antigua temporalmente
        CalculadoraRutas.ResultadoCamino rutaVieja = rutaPrincipalMemoria;

        // 2. Fijamos la alternativa elegida como la nueva principal
        rutaPrincipalMemoria = listaAlternativas.get(indiceAlternativaActual - 1);

        // 3. El intercambio entramos la ruta vieja en el lugar de la alternativa que acabamos de sacar
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
                    // --- AQUÍ ESTÁ EL INTERCAMBIO ---
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
        alerta.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        if (huerfanas.isEmpty()) {
            alerta.setHeaderText("Estado de la red: SALUDABLE");
            alerta.setContentText("Todas las paradas están correctamente conectadas. No hay puntos ciegos en el sistema.");
        } else {
            alerta.setAlertType(Alert.AlertType.WARNING);
            alerta.setHeaderText("Advertencia: Se encontraron paradas huerfanas");

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
            return;
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

//        Crea las filas y columnas de la matriz
        for (int i = 0; i < totalNodos; i++) {
            final int colIndex = i + 1;
            String nombreDestino = sistemaInfo.getParadas().get(resultadoGlobal.indiceAParadaId.get(i)).getNombre();

            TableColumn<String[], String> colDinamica = new TableColumn<>(nombreDestino);
            colDinamica.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()[colIndex]));
            colDinamica.setStyle("-fx-alignment: CENTER;");
            tabla.getColumns().add(colDinamica);
        }

//        Muestra los valores de la matriz y explica lo que significa cada valor
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

    /**
     * Función: obtenerColorPorLinea
     * Objetivo: Asignar un color único a cada línea dinámicamente. Si la línea es nueva,
     * calcula un nuevo color usando el Radio Áureo para asegurar contraste visual.
     */
    private Color obtenerColorPorLinea(String linea) {
        if (linea == null || linea.trim().isEmpty()) return Color.BLACK;

        // Si ya le habíamos asignado un color a esta línea, lo devolvemos
        if (mapaColoresLineas.containsKey(linea)) {
            return mapaColoresLineas.get(linea);
        }

        // Si es una línea nueva, generamos un color único matemáticamente
        // 137.508 es el ángulo de oro, garantiza que los colores se distribuyan bien en el círculo cromático
        double hue = (mapaColoresLineas.size() * 137.508) % 360.0;
        Color nuevoColor = Color.hsb(hue, 0.8, 0.8); // Color vivo y brillante

        // Guardamos el color para que todos los tramos de esta línea sean iguales
        mapaColoresLineas.put(linea, nuevoColor);
        return nuevoColor;
    }

    /**
     * Función: dibujarLeyenda
     * Objetivo: Renderizar un panel flotante que indica qué color corresponde a cada línea.
     */
    private void dibujarLeyenda() {
        if (mapaColoresLineas.isEmpty()) return;

        VBox leyenda = new VBox(8);

        // 1. ¡CLAVE! Usamos la memoria en lugar de números fijos
        leyenda.setLayoutX(leyendaX);
        leyenda.setLayoutY(leyendaY);

        String estiloBase = "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                "-fx-padding: 15; " +
                "-fx-border-color: #bdc3c7; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5); ";

        leyenda.setStyle(estiloBase + "-fx-cursor: open-hand;");

        Label titulo = new Label("🚇 Rutas Activas");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        leyenda.getChildren().add(titulo);

        for (Map.Entry<String, Color> entry : mapaColoresLineas.entrySet()) {
            javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(8);
            fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Circle colorDot = new Circle(7, entry.getValue());
            colorDot.setStroke(Color.DARKGRAY);
            Label nombre = new Label(entry.getKey());
            nombre.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e; -fx-font-weight: bold;");
            fila.getChildren().addAll(colorDot, nombre);
            leyenda.getChildren().add(fila);
        }

        final double[] dragDelta = new double[2];

        leyenda.setOnMousePressed(e -> {
            leyenda.setStyle(estiloBase + "-fx-cursor: closed-hand;");
            dragDelta[0] = leyenda.getLayoutX() - e.getSceneX();
            dragDelta[1] = leyenda.getLayoutY() - e.getSceneY();
        });

        leyenda.setOnMouseDragged(e -> {
            // Actualizamos visualmente el movimiento
            leyenda.setLayoutX(e.getSceneX() + dragDelta[0]);
            leyenda.setLayoutY(e.getSceneY() + dragDelta[1]);

            // 2. ¡CLAVE! Guardamos la nueva posición en la memoria global del controlador
            leyendaX = leyenda.getLayoutX();
            leyendaY = leyenda.getLayoutY();
        });

        leyenda.setOnMouseReleased(e -> {
            leyenda.setStyle(estiloBase + "-fx-cursor: open-hand;");
        });

        graphPane.getChildren().add(leyenda);
    }

    /**
     * Función: dibujarGrafoConCaminoEspecial
     * Objetivo: Limpiar el lienzo y redibujar solo una ruta específica (camino) destacada visualmente.
     * @param camino        (List<String>) Secuencia de IDs de paradas a resaltar.
     * @param esAlternativa (boolean) Dictamina el color y estilo (true = naranja punteado, false = verde sólido).
     */
    private void dibujarGrafoConCaminoEspecial(List<String> camino, boolean esAlternativa) {
        dibujarGrafoVisual(); // Limpia base
        Color colorCamino = esAlternativa ? Color.web("#e67e22") : Color.web("#27ae60");

        Map<String, Parada> paradas = sistemaInfo.getParadas();
        for (int i = 0; i < camino.size() - 1; i++) {
            Parada p1 = paradas.get(camino.get(i));
            Parada p2 = paradas.get(camino.get(i + 1));
            if (p1 != null && p2 != null) {
                // Le pasamos "Ruta Seleccionada" para el tooltip
                crearFlecha(p1, p2, colorCamino, 6.0, 1.0, esAlternativa, "Ruta Viaje");
            }
        }
    }

    /**
     * Función: obtenerTextoCriterio
     * Objetivo: Formatear matemáticamente y agregar sufijos al valor numérico resultante
     * de una búsqueda, basándose en la unidad de medida del criterio actual.
     * @param criterio (CriterioPesos) El contexto del valor (ej. COSTO, TIEMPO).
     * @param valor    (double) El número bruto a formatear.
     * @return         (String) Cadena amigable para el usuario (ej. "Costo: $5.0").
     */
    private String obtenerTextoCriterio(CriterioPesos criterio, double valor) {
        return switch (criterio) {
            case TIEMPO -> "Tiempo: " + valor + " mins.";
            case COSTO -> "Costo: $" + valor;
            case DISTANCIA -> "Distancia: " + String.format("%.2f", valor) + " Km.";
            case TRANSBORDOS -> "Transbordos: " + (int) valor;
        };
    }

    /**
     * Función: handleRegistrarAdmin
     * Objetivo: Capturar los datos de la pestaña de Usuarios para registrar un nuevo perfil
     * administrativo (impidiendo duplicados) y persistirlo usando el GestorUsuarios.
     */
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

        // Verificamos que el usuario no exista ya recorriendolo en la base de datos
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

    /**
     * Función: actualizarComboUsuarios
     * Objetivo: Volver a cargar la lista de administradores desde el archivo JSON al ComboBox
     * para reflejar cambios (nuevos registros o eliminaciones) de forma instantánea en la UI.
     */
    public void actualizarComboUsuarios() {
        if (comboUsuariosAdmin == null) return;
        comboUsuariosAdmin.getItems().clear();
        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> lista = gestor.cargarUsuarios();
        for (aw.transporte.model.Usuario u : lista) {
            comboUsuariosAdmin.getItems().add(u.getUsername());
        }
    }

    /**
     * Función: cargarDatosEnCampos
     * Objetivo: Autocompletar los campos de texto (Usuario, Clave, Correo) de la sección "Modificar/Eliminar"
     * cuando el administrador selecciona un nombre del menú desplegable.
     */
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

    /**
     * Función: handleModificarAdmin
     * Objetivo: Aplicar y guardar los cambios realizados sobre un perfil de administrador
     * existente tras seleccionarlo en la interfaz.
     */
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

    /**
     * Función: handleEliminarAdmin
     * Objetivo: Borrar el perfil de un administrador seleccionado del registro permanente,
     * bloqueando siempre la eliminación del superusuario principal ("admin").
     */
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

    /**
     * Función: handleCerrarSesion
     * Objetivo: Terminar la sesión actual, cierra la vista principal y recargar el formulario
     * FXML de Login en una nueva ventana aislada, segura y pequeña.
     */
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
                    // Usamos el color asignado dinámicamente a la línea
                    Color colorConexion = obtenerColorPorLinea(r.getNombreLinea());

                    // Le pasamos el nombre de la línea para el Tooltip (Globo de texto)
                    crearFlecha(p, d, colorConexion, 3.0, 0.8, false, r.getNombreLinea());
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
        dibujarLeyenda();
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
    private void crearFlecha(Parada origen, Parada destino, Color color, double grosor, double opacidad, boolean esAlternativa, String nombreLinea) {
        double x1 = origen.getCoorx() * ZOOM + 100;
        double y1 = origen.getCoory() * ZOOM + 100;
        double x2 = destino.getCoorx() * ZOOM + 100;
        double y2 = destino.getCoory() * ZOOM + 100;

        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(color);
        l.setStrokeWidth(grosor);
        l.setOpacity(opacidad);

        if (esAlternativa) l.getStrokeDashArray().addAll(10d, 10d);

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
        punta.getPoints().addAll(new Double[]{ puntoPuntaX, puntoPuntaY, xBase1, yBase1, xBase2, yBase2 });
        punta.setFill(color);
        punta.setOpacity(1.0);

        // --- MAGIA UX: TOOLTIP EN LA FLECHA ---
        Tooltip tt = new Tooltip("Línea: " + (nombreLinea != null ? nombreLinea : "Desconocida"));
        tt.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Tooltip.install(l, tt);
        Tooltip.install(punta, tt);
        // --------------------------------------

        graphPane.getChildren().addAll(l, punta);
    }

    /**
     * Función: dibujarPuntoTemporal
     * Objetivo: Renderizar un pequeño círculo semitransparente como previsualización
     * en las coordenadas donde el usuario ha hecho clic sobre el mapa.
     * @param x (double) Coordenada en X.
     * @param y (double) Coordenada en Y.
     */
    private void dibujarPuntoTemporal(double x, double y) {
        dibujarGrafoVisual();
        Circle preview = new Circle(x, y, 8, Color.web("#2f3542", 0.5));
        graphPane.getChildren().add(preview);
    }

    /**
     * Función: updateStatus
     * Objetivo: Actualizar el componente visual (Label) de la esquina inferior
     * para notificar al usuario sobre eventos, errores o éxitos del sistema.
     * @param msg (String) El mensaje a mostrar.
     */
    private void updateStatus(String msg) {
        if (lblEstado != null) lblEstado.setText(msg);
    }

    // Molde de datos para la Tabla Comparativa (Ahora con ID secreto)

    /**
     * Record: FilaRuta
     * Objetivo: Estructura de datos inmutable (Molde) utilizada para rellenar las celdas de la
     * Tabla Comparativa de Rutas. Incluye un ID secreto para vinculación interna.
     */
    public record FilaRuta(int id, String opcion, String camino, String valor) {}
}
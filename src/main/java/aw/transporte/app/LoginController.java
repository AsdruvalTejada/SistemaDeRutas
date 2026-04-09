package aw.transporte.app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Clase: LoginController
 * Objetivo: Controlador de la interfaz gráfica de inicio de sesión.
 * Gestiona el enrutamiento de usuarios (Pasajeros vs Administradores) y ejecuta
 * el protocolo de seguridad de Autenticación de Dos Factores (2FA) vía correo electrónico.
 */
public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnAdmin;
    @FXML private Button btnPasajero;
    @FXML private Label lblErrorLogin;

    /**
     * Función: initialize
     * Objetivo: Vincular las acciones iniciales de los botones de la interfaz
     * con sus respectivos métodos lógicos.
     */
    @FXML
    public void initialize() {
        btnAdmin.setOnAction(e -> handleLoginAdmin());
        btnPasajero.setOnAction(e -> abrirPantallaPrincipal(false));
    }

    /**
     * Función: abrirPantallaPrincipal
     * Objetivo: Cargar la vista principal del sistema (vista.fxml), inyectar el controlador
     * principal y configurar dinámicamente los permisos de la interfaz gráfica.
     * @param esAdmin (boolean) True si ingresa un administrador, False si es un pasajero (modo lectura).
     */
    private void abrirPantallaPrincipal(boolean esAdmin) {
        try {
            java.net.URL url = getClass().getResource("/vista.fxml");

            if (url == null) {
                lblErrorLogin.setText("No se encontró vista.fxml en la carpeta resources.");
                lblErrorLogin.setVisible(true);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);

            AppController appController = new AppController();
            loader.setController(appController);

            javafx.scene.Parent root = loader.load();

            appController.configurarPermisos(esAdmin);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Sistema de Rutas de Transporte");
            stage.setScene(new javafx.scene.Scene(root));
            stage.setMaximized(true);
            stage.show();

            javafx.stage.Stage loginStage = (javafx.stage.Stage) btnPasajero.getScene().getWindow();
            loginStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            lblErrorLogin.setText("Error al cargar la aplicación principal.");
            lblErrorLogin.setVisible(true);
        }
    }

    /**
     * Función: handleLoginAdmin
     * Objetivo: Validar las credenciales introducidas contra la base de datos JSON de usuarios.
     * Si son correctas y existe un correo asociado, dispara un código 2FA y bloquea el acceso
     * hasta que se valide correctamente en un cuadro de diálogo emergente.
     */
    private void handleLoginAdmin() {
        String user = txtUsuario.getText().trim();
        String pass = txtPassword.getText().trim();

        aw.transporte.data.GestorUsuarios gestor = new aw.transporte.data.GestorUsuarios();
        java.util.List<aw.transporte.model.Usuario> usuarios = gestor.cargarUsuarios();

        boolean credencialesValidas = false;
        String correoDestino = "";

        // 1. Buscamos si el usuario y la clave coinciden
        for (aw.transporte.model.Usuario u : usuarios) {
            if (u.getUsername().equals(user) && u.getPassword().equals(pass)) {
                credencialesValidas = true;
                correoDestino = u.getCorreo();
                break;
            }
        }

        // 2. Si las credenciales son correctas, pasamos al Paso 2 de seguridad
        if (credencialesValidas) {

            if (correoDestino != null && !correoDestino.isEmpty()) {
                // Generamos el código y disparamos el correo
                String codigoReal = aw.transporte.logic.MailService.enviarCodigoVerificacion(correoDestino, user);

                // Mostramos la ventanita emergente pidiendo el código
                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                dialog.setTitle("Verificación de Seguridad");
                dialog.setHeaderText(" Hemos enviado un código a:\n" + correoDestino);
                dialog.setContentText("Ingrese el código de 6 dígitos:");

                java.util.Optional<String> resultado = dialog.showAndWait();

                // Si el usuario presiona OK y el código coincide
                if (resultado.isPresent() && resultado.get().equals(codigoReal)) {
                    abrirPantallaPrincipal(true); // ¡ACCESO CONCEDIDO!
                } else {
                    lblErrorLogin.setText("Acceso denegado: Código incorrecto o cancelado.");
                    lblErrorLogin.setVisible(true);
                }
            } else {
                // Si el usuario no tiene correo registrado (ej: el admin por defecto) entra directo
                abrirPantallaPrincipal(true);
            }

        } else {
            lblErrorLogin.setText("Usuario o contraseña incorrectos.");
            lblErrorLogin.setVisible(true);
        }
    }
}
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

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnAdmin;
    @FXML private Button btnPasajero;
    @FXML private Label lblErrorLogin;

    @FXML
    public void initialize() {
        btnAdmin.setOnAction(e -> handleLoginAdmin());
        btnPasajero.setOnAction(e -> abrirPantallaPrincipal(false));
    }

    private void handleLoginAdmin() {
        String user = txtUsuario.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.equals("admin") && pass.equals("1234")) {
            // ¡AQUÍ LUEGO AGREGAREMOS EL CÓDIGO DEL CORREO JAVAMAIL!
            abrirPantallaPrincipal(true);
        } else {
            lblErrorLogin.setText("Usuario o contraseña incorrectos.");
            lblErrorLogin.setVisible(true);
        }
    }

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
}
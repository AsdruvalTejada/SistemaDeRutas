package aw.transporte.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));

        Scene scene = new Scene(loader.load());
        stage.setTitle("Inicio de Sesión - Sistema de Rutas");
        stage.setScene(scene);

        stage.setMaximized(false);
        stage.setResizable(false);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
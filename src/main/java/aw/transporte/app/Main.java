package aw.transporte.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Cargamos la vista
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista.fxml"));

        // Le inyectamos el controlador limpio
        loader.setController(new AppController());

        Scene scene = new Scene(loader.load());
        stage.setTitle("Sistema de Rutas - Avance Estudiante B");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
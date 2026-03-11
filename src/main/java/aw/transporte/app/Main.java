package aw.transporte.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista.fxml"));
        loader.setController(new AppController());

        Scene scene = new Scene(loader.load());
        stage.setTitle("Sistema de Rutas");
        stage.setScene(scene);

        //Forzamos a que la aplicación inicie en pantalla completa para mejor visualización del mapa
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
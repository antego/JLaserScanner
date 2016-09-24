package com.github.antego.laserscanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


public class MainWindow extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        Pane root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("Laser Scanner");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(we -> controller.onClose());
        primaryStage.show();
    }
}

package org.antego.dev;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        Pane root = loader.load();
        Controller controller = loader.getController();
        primaryStage.setTitle("Laser Scanner");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(we -> controller.onClose());
        primaryStage.show();
        controller.setRootElement(root);
    }


    public static void main(String[] args) {
        System.loadLibrary("opencv_native");
        launch(args);
    }
}

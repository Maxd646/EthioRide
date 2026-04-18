package com.ethioride.driver.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Navigator {
    private static Stage primaryStage;

    public static void init(Stage stage) { primaryStage = stage; }

    public static void navigateTo(String fxmlPath) {
        try {
            URL resource = Navigator.class.getResource(fxmlPath);
            if (resource == null) throw new IOException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(resource);
            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Stage getStage() { return primaryStage; }
}

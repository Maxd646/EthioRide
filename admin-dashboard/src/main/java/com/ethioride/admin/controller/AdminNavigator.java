package com.ethioride.admin.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class AdminNavigator {
    private static Stage stage;

    public static void init(Stage s) { stage = s; }

    public static void navigateTo(String fxmlPath) {
        try {
            URL resource = AdminNavigator.class.getResource(fxmlPath);
            if (resource == null) throw new IOException("FXML not found: " + fxmlPath);
            Parent root = FXMLLoader.load(resource);
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Stage getStage() { return stage; }
}

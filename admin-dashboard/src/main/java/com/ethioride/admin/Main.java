package com.ethioride.admin;

import com.ethioride.admin.controller.AdminNavigator;
import com.ethioride.shared.utils.I18n;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        I18n.load("i18n/messages");

        primaryStage.setTitle("EthioRide — System Monitor");
        primaryStage.setMinWidth(520);
        primaryStage.setMinHeight(600);

        AdminNavigator.init(primaryStage);
        AdminNavigator.navigateTo("/ui/admin_login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

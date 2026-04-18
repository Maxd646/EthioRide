package com.ethioride.driver;

import com.ethioride.driver.ui.navigation.Navigator;
import com.ethioride.shared.utils.I18n;
import javafx.application.Application;
import javafx.stage.Stage;

public class DriverApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        I18n.load("i18n/messages");

        primaryStage.setTitle("EthioRide — Driver Dashboard");
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(600);

        Navigator.init(primaryStage);
        Navigator.navigateTo("/ui/views/driver_login.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

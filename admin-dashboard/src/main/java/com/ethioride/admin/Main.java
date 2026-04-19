package com.ethioride.admin;

import com.ethioride.admin.ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("EthioRide — Admin Dashboard");
        stage.setMinWidth(520);
        stage.setMinHeight(600);
        stage.setResizable(false);
        new LoginScreen(stage).show();
    }
    public static void main(String[] args) { launch(args); }
}

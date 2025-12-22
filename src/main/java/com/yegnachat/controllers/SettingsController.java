package com.yegnachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class SettingsController {

    private Stage stage;

    @FXML
    private Button closeButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button editBioButton;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        closeButton.setOnAction(e -> stage.close());

        logoutButton.setOnAction(e -> {
            System.out.println("Logging out...");
        });

        editBioButton.setOnAction(e -> {
            System.out.println("Edit bio clicked");
        });
    }
}

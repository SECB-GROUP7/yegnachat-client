package com.yegnachat.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatController {

    @FXML
    private javafx.scene.control.Button settingsButton;

    private Stage chatStage;
    private Stage settingsStage;

    public void setChatStage(Stage chatStage) {
        this.chatStage = chatStage;
    }

    @FXML
    public void initialize() {
        settingsButton.setOnAction(this::openSettingsWindow);
    }

    private void openSettingsWindow(ActionEvent event) {
        try {
            if (settingsStage == null) {
                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource("/com/yegnachat/client/settings.fxml"));

                BorderPane root = loader.load();

                settingsStage = new Stage();
                settingsStage.setTitle("Settings");
                settingsStage.setResizable(false);
                settingsStage.setScene(new Scene(root));

                //  MODAL TO CHAT WINDOW
                Stage owner = (Stage) settingsButton.getScene().getWindow();
                settingsStage.initOwner(owner);
                settingsStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

                // CLOSE if chat closes
                owner.setOnHidden(e -> settingsStage.close());

                SettingsController controller = loader.getController();
                controller.setStage(settingsStage);
            }

            if (!settingsStage.isShowing()) {
                settingsStage.show();      // ✅ NOT showAndWait
            } else {
                settingsStage.toFront();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

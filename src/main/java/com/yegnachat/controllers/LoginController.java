package com.yegnachat.controllers;

import com.yegnachat.server.Client;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    public void connect(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/chat.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            ChatController chatController = fxmlLoader.getController();

            Socket socket = new Socket(hostField.getText(), Integer.parseInt(portField.getText()));
            Client client = new Client(usernameField.getText(), socket, chatController);
            client.listenForMessages();

            chatController.setClient(client);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Yegna Chat - " + usernameField.getText());
            stage.show();

            connectButton.getScene().getWindow().hide();

            statusLabel.setText("Connected!");

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error connecting to server");
        }
    }
}

package com.yegnachat.controllers;

import com.yegnachat.server.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ChatController {

    @FXML
    private VBox chat_messageBox;
    @FXML
    private TextField chat_messageField;
    @FXML
    private Button chat_sendButton;
    @FXML
    private ScrollPane chat_scrollPane;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
        setupSendButton();
    }

    private void setupSendButton() {
        chat_sendButton.setOnAction(event -> {
            String message = chat_messageField.getText();
            if (!message.isEmpty()) {
                client.sendMessage(message);
                addMessageToBox("Me: " + message);
                chat_messageField.clear();
            }
        });
    }

    public void addMessageToBox(String message) {
        Platform.runLater(() -> {
            chat_messageBox.getChildren().add(new Text(message));
            chat_scrollPane.vvalueProperty().bind(chat_messageBox.heightProperty());
        });
    }
}

package com.yegnachat.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Navigator {
    public static void switchTo(Node anyNodeInScene, String fxmlName, String title) throws IOException {
        Stage stage = (Stage) anyNodeInScene.getScene().getWindow();
        Parent root = FXMLLoader.load(Objects.requireNonNull(Navigator.class.getResource("/com/yegnachat/client/" + fxmlName)));
        stage.setScene(new Scene(root));
        if (title != null) stage.setTitle(title);
    }
}

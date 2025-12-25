package com.yegnachat.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class GroupInfoController {

    @FXML
    private VBox root;

    @FXML
    private Button closeButton;

    @FXML
    private ImageView groupAvatar;

    @FXML
    protected Label groupNameLabel;

    @FXML
    protected Label groupAboutLabel;

    @FXML
    private VBox membersBox;

    @FXML
    private Button addMemberButton;

    @FXML
    private Button leaveGroupButton;

    private Runnable closeCallback;

    @FXML
    public void initialize() {
        // Controller-owned CSS
        root.getStylesheets().add(
                getClass().getResource("/css/group-info.css").toExternalForm()
        );

        closeButton.setOnAction(e -> {
            if (closeCallback != null) {
                closeCallback.run();
            }
        });

        // Temporary placeholders (visual sanity)
        groupNameLabel.setText("Group Name");
        groupAboutLabel.setText("Group description goes here");
    }

    /* ===== future hooks ===== */

    public void setCloseCallback(Runnable closeCallback) {
        this.closeCallback = closeCallback;
    }

    public VBox getMembersBox() {
        return membersBox;
    }
}

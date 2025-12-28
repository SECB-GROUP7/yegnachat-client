package com.yegnachat.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yegnachat.net.ChatClientSocket;
import com.yegnachat.session.Session;
import com.yegnachat.util.ImageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FeedController {
    @FXML
    private VBox feedContainer;

    @FXML
    private Button profileButton, refreshButton, newPostButton;

    @FXML
    private ScrollPane feedScroll;

    private ChatClientSocket socket;

    @FXML
    public void initialize() {
        socket = Session.getSocket();
        if (socket == null) throw new IllegalStateException("Socket missing");

        // Button actions
        profileButton.setOnAction(e -> openProfile());
        refreshButton.setOnAction(e -> loadFeed());
        newPostButton.setOnAction(e -> openNewPostDialog());

        // Initial feed load
        loadFeed();
    }

    private void openProfile() {
        System.out.println("[FEED] Profile button clicked");
        // Implement profile view logic
    }

    private void loadFeed() {
        System.out.println("[FEED] Loading feed...");

        feedContainer.getChildren().clear();

        JsonObject payload = new JsonObject();
        payload.addProperty("limit", 20);
        payload.addProperty("offset", 0);

        JsonObject request = new JsonObject();
        request.addProperty("type", "list_feed_posts");
        request.add("payload", payload);

        socket.send(request);

        socket.setOnMessage(msg -> {
            String type = msg.has("type") ? msg.get("type").getAsString() : "unknown";
            if ("list_feed_posts_response".equals(type)) {
                Platform.runLater(() -> populateFeed(msg.getAsJsonObject("payload").getAsJsonArray("posts")));
            }
        });
    }


    private void populateFeed(JsonArray posts) {
        feedContainer.getChildren().clear();

        for (var p : posts) {
            JsonObject post = p.getAsJsonObject();

            VBox postCard = new VBox(10);
            postCard.getStyleClass().add("post-card");

            // User info
            JsonObject user = post.getAsJsonObject("user");

            String usernameText =
                    user != null && user.has("username")
                            ? user.get("username").getAsString()
                            : "Unknown";

            String avatarUrl =
                    user != null && user.has("avatar_url")
                            ? user.get("avatar_url").getAsString()
                            : "";

            Label username = new Label(usernameText);
            ImageView avatar = new ImageView(ImageUtil.loadAvatar(avatarUrl));

            avatar.setFitWidth(40);
            avatar.setFitHeight(40);

            // Content
            Label content = new Label(post.get("content").getAsString());
            content.setWrapText(true);
            content.getStyleClass().add("post-content");

    // Likes
            boolean likedByMe = post.has("liked_by_me") && post.get("liked_by_me").getAsBoolean();
//            Button likeButton = new Button(likedByMe ? "Unlike" : "Like");
//            likeButton.setOnAction(e -> {
//                JsonObject likeMsg = new JsonObject();
//                likeMsg.addProperty("type", likedByMe ? "unlike_post" : "like_post");
//                JsonObject payload = new JsonObject();
//                payload.addProperty("post_id", post.get("post_id").getAsLong());
//                likeMsg.add("payload", payload);
//                socket.send(likeMsg);
//
//                likeButton.setText(likedByMe ? "Like" : "Unlike");
//                post.addProperty("liked_by_me", !likedByMe);
//            });


            // Comments
            Button commentButton = new Button("View Comments");
            commentButton.setOnAction(e -> openCommentsDialog(post.get("post_id").getAsLong()));

            HBox actions = new HBox(10, commentButton);

            postCard.getChildren().addAll(avatar, username, content);

            if (post.has("image_url")) {
                String imageUrl = post.get("image_url").getAsString();
                if (!imageUrl.isBlank()) {
                    ImageView postImage = new ImageView(ImageUtil.loadAvatar(imageUrl));
                    postImage.setPreserveRatio(true);
                    postImage.setFitWidth(420);
                    postImage.getStyleClass().add("post-image");

                    postCard.getChildren().addAll(postImage);
                }
                postCard.getChildren().add(actions);
            }

            feedContainer.getChildren().add(postCard);
        }
    }

    private void openCommentsDialog(long postId) {
        try {
            VBox dialogRoot = new VBox(10);
            dialogRoot.setStyle("-fx-padding: 20; -fx-background-color: white;");

            Label title = new Label("Comments");
            VBox commentsBox = new VBox(5);
            ScrollPane scrollPane = new ScrollPane(commentsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);

            TextField commentField = new TextField();
            commentField.setPromptText("Write a comment...");
            Button sendCommentBtn = new Button("Send");

            sendCommentBtn.setOnAction(ev -> {
                String content = commentField.getText().trim();
                if (!content.isEmpty()) {
                    JsonObject msg = new JsonObject();
                    msg.addProperty("type", "add_comment");
                    JsonObject payload = new JsonObject();
                    payload.addProperty("post_id", postId);
                    payload.addProperty("content", content);
                    msg.add("payload", payload);
                    socket.send(msg);

                    commentField.clear();
                }
            });

            VBox inputBox = new VBox(5, commentField, sendCommentBtn);
            dialogRoot.getChildren().addAll(title, scrollPane, inputBox);

            Stage stage = new Stage();
            stage.setTitle("Comments");
            stage.setScene(new Scene(dialogRoot));
            stage.show();

            // Load comments
            JsonObject request = new JsonObject();
            request.addProperty("type", "list_comments");
            JsonObject payload = new JsonObject();
            payload.addProperty("post_id", postId);
            request.add("payload", payload);
            socket.send(request);

            // Handle incoming comments
            socket.setOnMessage(msg -> {
                String type = msg.has("type") ? msg.get("type").getAsString() : "unknown";
                if ("list_comments_response".equals(type)) {
                    Platform.runLater(() -> {
                        commentsBox.getChildren().clear();
                        for (var c : msg.getAsJsonObject("payload").getAsJsonArray("comments")) {
                            JsonObject comment = c.getAsJsonObject();

                            JsonObject userObj = comment.has("user") && !comment.get("user").isJsonNull()
                                    ? comment.getAsJsonObject("user")
                                    : null;

                            String username = userObj != null && userObj.has("username") && !userObj.get("username").isJsonNull()
                                    ? userObj.get("username").getAsString()
                                    : "Unknown";

                            String content = comment.has("content") && !comment.get("content").isJsonNull()
                                    ? comment.get("content").getAsString()
                                    : "";

                            Label lbl = new Label(username + ": " + content);
                            commentsBox.getChildren().add(lbl);
                        }

                    });
                } else if ("add_comment_response".equals(type)) {
                    Platform.runLater(() -> {
                        JsonObject c = msg.getAsJsonObject("payload");
                        if (c.get("post_id").getAsLong() == postId) {
                            Label lbl = new Label("You: " + commentField.getText().trim());
                            commentsBox.getChildren().add(lbl);
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open comments").show();
        }
    }

    private void openNewPostDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yegnachat/client/post_dialog.fxml"));
            VBox dialogRoot = loader.load();

            // Get the controls
            TextArea contentField = (TextArea) dialogRoot.lookup("#contentField");
            Button uploadBtn = (Button) dialogRoot.lookup("#uploadImageBtn");
            Label imageLabel = (Label) dialogRoot.lookup("#imageNameLabel");
            Button postBtn = (Button) dialogRoot.lookup(".post-btn");
            Button cancelBtn = (Button) dialogRoot.lookup(".cancel-btn");

            final File[] selectedImage = new File[1]; // holder for chosen image

            uploadBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Image");
                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
                );
                File file = fileChooser.showOpenDialog(feedContainer.getScene().getWindow());
                if (file != null) {
                    selectedImage[0] = file;
                    imageLabel.setText(file.getName());
                }
            });

            cancelBtn.setOnAction(e -> ((Stage) dialogRoot.getScene().getWindow()).close());

            postBtn.setOnAction(e -> {
                String content = contentField.getText().trim();
                File image = selectedImage[0];

                if (content.isEmpty() && image == null) {
                    new Alert(Alert.AlertType.WARNING, "Post cannot be empty").show();
                    return;
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("content", content);

                boolean hasImage = image != null;
                payload.addProperty("has_image", hasImage);

                if (hasImage) {
                    payload.addProperty("image_size", image.length());
                    payload.addProperty("mime", ImageUtil.detectMime(image));
                }

                JsonObject msg = new JsonObject();
                msg.addProperty("type", "create_post");
                msg.add("payload", payload);

                try {
                    // send JSON header
                    socket.send(msg); // MUST end with newline internally

                    //immediately stream raw image
                    if (hasImage) {
                        try (InputStream in = new FileInputStream(image)) {
                            socket.sendRaw(in, image.length());
                        }
                    }

                    ((Stage) dialogRoot.getScene().getWindow()).close();
                    loadFeed(); // optional refresh

                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to create post").show();
                }
            });



            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create Post");
            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);
            dialogStage.initOwner(feedContainer.getScene().getWindow());
            dialogStage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open post dialog").show();
        }
    }

}

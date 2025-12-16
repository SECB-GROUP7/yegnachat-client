package com.yegnachat.server;

import com.google.gson.Gson;
import com.yegnachat.protocol.JsonMessage;
import com.yegnachat.server.auth.AuthService;
import com.yegnachat.server.auth.SessionInfo;
import com.yegnachat.server.chat.ChatService;
import com.yegnachat.server.user.UserService;
import com.yegnachat.models.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MessageRouter {

    private final Gson gson = new Gson();
    private final AuthService authService;
    private final ChatService chatService;
    private final UserService userService;

    public MessageRouter(AuthService authService, ChatService chatService, UserService userService) {
        this.authService = authService;
        this.chatService = chatService;
        this.userService = userService;
    }

    public String route(String json, ClientHandler sender) {
        JsonMessage msg = gson.fromJson(json, JsonMessage.class);

        try {
            return switch (msg.getType()) {

                case "login" -> {
                    Map<?, ?> p = (Map<?, ?>) msg.getPayload();
                    SessionInfo s = authService.login(
                            p.get("username").toString(),
                            p.get("password").toString()
                    );

                    if (s == null) {
                        yield gson.toJson(new JsonMessage("login_response", Map.of("status", "error")));
                    }

                    sender.setSession(s);

                    yield gson.toJson(new JsonMessage("login_response", Map.of(
                            "status", "ok",
                            "token", s.getToken(),
                            "user_id", s.getUserId()
                    )));
                }

                case "send_message" -> {
                    if (sender.getSession() == null) {
                        yield gson.toJson(new JsonMessage("error", "Not authenticated"));
                    }

                    Map<?, ?> p = (Map<?, ?>) msg.getPayload();
                    String content = p.get("content").toString();

                    if (p.containsKey("receiver_id")) {
                        int receiverId = Integer.parseInt(p.get("receiver_id").toString());
                        chatService.savePrivateMessage(sender.getSession().getUserId(), receiverId, content);
                        ClientHandler.sendToUser(receiverId, json);
                    }

                    // Group message
                    if (p.containsKey("group_id")) {
                        int groupId = Integer.parseInt(p.get("group_id").toString());
                        chatService.saveGroupMessage(sender.getSession().getUserId(), groupId, content);

                        List<Integer> members = chatService.getGroupMembers(groupId);
                        ClientHandler.sendToUsers(members, json);
                    }

                    yield null;
                }

                case "fetch_history" -> {
                    if (sender.getSession() == null) {
                        yield gson.toJson(new JsonMessage("error", "Not authenticated"));
                    }

                    Map<?, ?> p = (Map<?, ?>) msg.getPayload();
                    String type = p.get("chat_type").toString(); // "private" or "group"

                    if ("private".equals(type)) {
                        int otherUserId = Integer.parseInt(p.get("user_id").toString());
                        List<String> history = chatService.fetchPrivateHistory(sender.getSession().getUserId(), otherUserId);

                        yield gson.toJson(new JsonMessage("fetch_history_response", Map.of(
                                "status", "ok",
                                "chat_type", "private",
                                "messages", history
                        )));
                    } else if ("group".equals(type)) {
                        int groupId = Integer.parseInt(p.get("group_id").toString());
                        List<String> history = chatService.fetchGroupHistory(groupId);

                        yield gson.toJson(new JsonMessage("fetch_history_response", Map.of(
                                "status", "ok",
                                "chat_type", "group",
                                "messages", history
                        )));
                    } else {
                        yield gson.toJson(new JsonMessage("error", "Unknown chat type"));
                    }
                }

                case "get_user" -> {
                    if (sender.getSession() == null) {
                        yield gson.toJson(new JsonMessage("error", "Not authenticated"));
                    }

                    Map<?, ?> p = (Map<?, ?>) msg.getPayload();
                    int userId = Integer.parseInt(p.get("user_id").toString());
                    User user = userService.getById(userId);

                    if (user == null) {
                        yield gson.toJson(new JsonMessage("get_user_response", Map.of("status", "error")));
                    }

                    yield gson.toJson(new JsonMessage("get_user_response", Map.of(
                            "status", "ok",
                            "user", Map.of(
                                    "id", user.getId(),
                                    "username", user.getUsername(),
                                    "avatar_url", user.getAvatarUrl(),
                                    "bio", user.getBio()
                            )
                    )));
                }

                case "list_users" -> {
                    if (sender.getSession() == null) {
                        yield gson.toJson(new JsonMessage("error", "Not authenticated"));
                    }

                    List<User> users = userService.listAllUsersExcept(sender.getSession().getUserId());

                    List<Map<String, Object>> result = users.stream()
                            .map(u -> Map.<String, Object>of(
                                    "id", u.getId(),
                                    "username", u.getUsername(),
                                    "avatar_url", u.getAvatarUrl()
                            ))
                            .toList();

                    yield gson.toJson(new JsonMessage("list_users_response", Map.of(
                            "status", "ok",
                            "users", result
                    )));
                }

                case "signup" -> {
                    Map<String, Object> p = (Map<String, Object>) msg.getPayload();

                    String username = p.get("username").toString();
                    String password = p.get("password").toString();
                    String avatarUrl = p.containsKey("avatar_url") ? p.get("avatar_url").toString() : "";
                    String bio = p.containsKey("bio") ? p.get("bio").toString() : "";

                    try {
                        boolean ok = userService.createUser(username, password, avatarUrl, bio);
                        if (ok) {
                            yield gson.toJson(new JsonMessage("signup_response", Map.of("status", "ok")));
                        } else {
                            yield gson.toJson(new JsonMessage("signup_response", Map.of("status", "error", "message", "Username already exists")));
                        }
                    } catch (Exception e) {
                        yield gson.toJson(new JsonMessage("signup_response", Map.of("status", "error", "message", e.getMessage())));
                    }
                }
                case "list_group_members" -> {
                    if (sender.getSession() == null) {
                        yield gson.toJson(new JsonMessage("error", "Not authenticated"));
                    }

                    Map<?, ?> p = (Map<?, ?>) msg.getPayload();
                    int groupId = Integer.parseInt(p.get("group_id").toString());

                    List<Integer> memberIds = chatService.getGroupMembers(groupId);

                    yield gson.toJson(new JsonMessage("list_group_members_response", Map.of(
                            "status", "ok",
                            "group_id", groupId,
                            "members", memberIds
                    )));
                }

                default -> gson.toJson(new JsonMessage("error", "Unknown message type"));
            };

        } catch (SQLException e) {
            return gson.toJson(new JsonMessage("error", "Database error: " + e.getMessage()));
        } catch (Exception e) {
            return gson.toJson(new JsonMessage("error", e.getMessage()));
        }
    }
}

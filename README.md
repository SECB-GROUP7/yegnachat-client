# YegnaChat Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

YegnaChat Client is a **JavaFX-based desktop chat client** for interacting with the YegnaChat Server. It supports login/signup, private & group messaging, group management, feed posts, and user profile management.

---

## Table of Contents

* [Features](#features)  
* [Requirements](#requirements)  
* [Environment Variables](#environment-variables)  
* [Setup](#setup)  
* [Running the Client](#running-the-client)  
* [Contributing](#contributing)  
* [License](#license)  

---

## Features

* User signup and login  
* Private messaging between users  
* Group creation, adding/removing users, leaving groups  
* Fetch chat history (private & group)  
* List users and group members  
* Feed support: posts, likes, comments, follow/unfollow  
* User profile management (avatar, bio, language preferences)  
* Supports server-side session and authentication  

---

## Requirements

* Java 17+  
* JavaFX 20+ (or compatible version)  
* Maven or Gradle (optional, if building with a build tool)  
* YegnaChat Server running  

---

## Environment Variables

Create a `.env` file in the project root with the following values:

```env
HOST=localhost
PORT=9090
TRANSLATION_URL=https://script.google.com/macros/s/AKfycbwZQ1cbo4VBVxcOnrqufdcXF2wPZgbKyHtyNgRYuwQEkYXcgWotFiQj7mMsKeRyBYOVVg/exec
```

> **Note:**  
> The client does not require database configuration. The `HOST` and `PORT` should match the server's host and port. `TRANSLATION_URL` is used for client-side text translation features.

---

## Setup

1. **Clone the repository**

```bash
git clone https://github.com/SECB-GROUP7/yegnachat-client.git
cd yegnachat-client
```

2. **Configure `.env`** as shown in [Environment Variables](#environment-variables).

3. **Install dependencies** (if using Maven):

```bash
mvn clean install
```

---

## Running the Client

### Using IDE (IntelliJ/NetBeans/Eclipse)

1. Open the project in your IDE.  
2. Run `com.yegnachat.client.ChatClient` as a JavaFX Application.  
3. The login screen will appear. Enter your credentials to connect to the server.

### Using Command Line

```bash
# Compile
javac -d out/production/client src/main/java/com/yegnachat/client/ChatClient.java

# Run
java -cp out/production/client com.yegnachat.client.ChatClient
```

---

## JSON Requests

The client communicates with the server using **JSON messages**. The server-side `Readme.md` has full examples. Typical requests include:

* `signup`  
* `login`  
* `send_message` (private & group)  
* `fetch_history`  
* `create_group`, `add_user_to_group`, `leave_group`  
* `list_users`, `list_group_members`, `list_groups_for_user`  
* `feed`: `create_post`, `like_post`, `add_comment`, `list_feed_posts`  
* User profile updates: `set_bio`, `set_password`, `set_preferred_language`  

---

## Contributing

1. Fork the repository  
2. Create a branch (`git checkout -b feature/xyz`)  
3. Make changes and commit (`git commit -m "feat: your message"`)  
4. Push (`git push origin feature/xyz`)  
5. Open a Pull Request  

---

## License

MIT License © SECB-GROUP7  

---

This README provides all you need to **run the client**, **connect to the server**, and **understand the supported features and endpoints**.


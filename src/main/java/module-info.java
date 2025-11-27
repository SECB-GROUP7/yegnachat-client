module com.yegnachat.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens com.yegnachat.client to javafx.fxml;
    exports com.yegnachat.client;
    exports com.yegnachat.controllers;
    opens com.yegnachat.controllers to javafx.fxml;
}
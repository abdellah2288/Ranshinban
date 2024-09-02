package com.ranshinban.ranshinban.utils;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class popupWindow
{
    static public void raisePopupWindow(String message,String title)
    {
        Stage window = new Stage();
        VBox root = new VBox();
        Label label = new Label(message);
        Scene scene = new Scene(root);
        root.setPadding(new Insets(20,20,20,20));
        root.getChildren().add(label);

        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);

        window.setScene(scene);
        window.sizeToScene();
        window.setResizable(false);
        window.setTitle(title);
        window.getIcons().add(new Image(popupWindow
                .class
                .getClassLoader()
                .getResourceAsStream("ranshinban-40x40.png")));
        window.show();
    }
}

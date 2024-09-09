package com.ranshinban.ranshinban;

import com.ranshinban.ranshinban.BLE.Scanner;
import com.ranshinban.ranshinban.Wireless.httpClient;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application
{
    public static void main(String[] args)
    {
        Application.launch(args);
    }
    public void start(Stage mainStage)
    {

        VBox scannerPanel = Scanner.scannerPanel();

        scannerPanel.setPadding(new Insets(10, 10, 10, 10));

        Scene mainScene = new Scene(scannerPanel, 800, 600);

        scannerPanel.prefWidthProperty().bind(mainScene.widthProperty());
        mainStage.setScene(mainScene);
        mainStage.setMinWidth(640);
        mainStage.setMinHeight(480);
        mainStage.sizeToScene();
        mainStage.setTitle("Ranshinban");
        mainStage.getIcons().add(new Image(Main.class.getClassLoader().getResourceAsStream("ranshinban-256x256.png")));
        mainStage.show();
        mainStage.setOnCloseRequest(e->
        {
            System.exit(0);
        });
    }
}
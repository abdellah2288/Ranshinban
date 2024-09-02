package com.ranshinban.ranshinban.utils;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SerialDebug
{
    static public void serialDebug()
    {
        TextField serialInputField = new TextField();
        TextArea serialOutputField = new TextArea();
        Button sendButton = new Button("Send");
        ComboBox<String> baudRateBox = new ComboBox<>();
        HBox inputBox = new HBox();
        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();

        baudRateBox.getItems().addAll("9600", "19200", "38400", "57600","115200");

        baudRateBox.setValue("115200");
        baudRateBox
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                (v,oldVal,newVal) ->
                {
                    SerialHandler.setBaudRate(Integer.parseInt(newVal));
                }
        );


        sendButton.setOnMouseClicked((Event e)->
        {
            String serialInput = serialInputField.getText() == null ? "" : serialInputField.getText();
            SerialHandler.print(serialInput);
        });

        serialInputField.setOnKeyPressed((KeyEvent e)->
        {
            if(e.getCode() == KeyCode.ENTER)
            {
                String serialInput = serialInputField.getText() == null ? "" : serialInputField.getText();
                SerialHandler.print(serialInput);
            }
        });

        serialInputField.prefWidthProperty().bind(inputBox.widthProperty().divide(2));
        sendButton.prefWidthProperty().bind(inputBox.widthProperty().divide(6));
        baudRateBox.prefWidthProperty().bind(inputBox.widthProperty().multiply(2).divide(6));

        inputBox.getChildren().addAll(serialInputField, sendButton, baudRateBox);
        inputBox.setSpacing(5);

        serialOutputField.prefWidthProperty().bind(root.widthProperty());
        VBox.setVgrow(serialOutputField, Priority.ALWAYS);

        root.getChildren().addAll(inputBox, serialOutputField);
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.setTitle("Serial Console");
        stage.getIcons().add(new Image(SerialDebug
                .class
                .getClassLoader()
                .getResourceAsStream("ranshinban-256x256.png")));
        stage.show();

        new Thread(
                ()->
                {
                    while(stage.isShowing())
                    {
                        try
                        {
                            if(SerialHandler.portOpen())
                            {
                                Platform.runLater(()->
                                        {
                                            serialOutputField.appendText(SerialHandler.getDebugBuffer());
                                        });
                            }
                            Thread.sleep(17);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }
}

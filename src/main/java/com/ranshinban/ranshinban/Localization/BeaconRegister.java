package com.ranshinban.ranshinban.Localization;
import com.ranshinban.ranshinban.utils.errorWindow;
import com.ranshinban.ranshinban.BLE.Beacon;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeaconRegister
{
    static private ArrayList<Beacon> registeredBeacons = new ArrayList<> ();
    static private ListView<Beacon> beaconList = null;
    static private final File beaconDB = new File(System.getProperty("user.home")+"/Documents/Ranshinban","beaconDB");

    public static void setupBeacon(Beacon beacon)
    {
        Stage mainStage = new Stage();
        VBox root = new VBox();
        Scene mainScene = new Scene(root);

        TextField beaconName = new TextField();
        TextField beaconAddress = new TextField();
        TextField beaconXCoordinate = new TextField();
        TextField beaconYCoordinate = new TextField();

        Label beaconNameLabel = new Label("Beacon Name");
        Label beaconAddressLabel = new Label("Beacon Address");
        Label beaconXCoordinateLabel = new Label("Beacon X Coordinate");
        Label beaconYCoordinateLabel = new Label("Beacon Y Coordinate");

        Button saveButton = new Button("Save");
        saveButton.setDisable(true);

        beaconName.setText(beacon.getDeviceName());

        beaconAddress.textProperty().addListener((v,oldVal,newVal)->
                {
                    try
                    {
                        Double.parseDouble(beaconXCoordinate.getText());
                        Double.parseDouble(beaconYCoordinate.getText());

                        Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[-:]){5}([0-9A-Fa-f]{2})");
                        Matcher matcher = pattern.matcher(beaconAddress.getText());
                        saveButton.setDisable(!matcher.find());
                    }
                    catch(Exception e)
                    {
                        saveButton.setDisable(true);
                    }
                }
        );

        beaconXCoordinate.setText(beacon.getxCoordinate().toString());
        beaconXCoordinate.textProperty().addListener((v,oldVal,newVal)->
        {
            try
            {
                Double.parseDouble(newVal);
                Double.parseDouble(beaconYCoordinate.getText());

                Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[-:]){5}([0-9A-Fa-f]{2})");
                Matcher matcher = pattern.matcher(beaconAddress.getText());
                saveButton.setDisable(!matcher.find());
            }
            catch(Exception e)
            {
                saveButton.setDisable(true);
            }
        });

        beaconYCoordinate.setText(beacon.getyCoordinate().toString());
        beaconYCoordinate.textProperty().addListener((v,oldVal,newVal)->
        {
            try
            {
                Double.parseDouble(newVal);
                Double.parseDouble(beaconXCoordinate.getText());

                Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[-:]){5}([0-9A-Fa-f]{2})");
                Matcher matcher = pattern.matcher(beaconAddress.getText());
                saveButton.setDisable(!matcher.find());
            }
            catch(Exception e)
            {
                saveButton.setDisable(true);
            }
        });

        beaconAddress.setText(beacon.getMacAddress());

        saveButton.setOnAction(e -> {
            Beacon _beacon = new Beacon(beaconName.getText(),beaconAddress.getText(),0,0);
            _beacon.setxCoordinate(Double.parseDouble(beaconXCoordinate.getText()));
            _beacon.setyCoordinate(Double.parseDouble(beaconYCoordinate.getText()));
            registerBeacon(_beacon
                    ,true
                    ,false
            );
            //refreshBeaconListView();
            mainStage.close();
        });

        root.setSpacing(10);
        root.getChildren().addAll(beaconNameLabel
                ,beaconName
                ,beaconAddressLabel
                ,beaconAddress
                ,beaconXCoordinateLabel
                ,beaconXCoordinate
                ,beaconYCoordinateLabel
                ,beaconYCoordinate
                ,saveButton
        );
        root.setPadding(new Insets(10,10,10,10));

        mainStage.setScene(mainScene);
        mainStage.setTitle("Register beacon");
        mainStage.setWidth(500);
        mainStage.getIcons().add(new Image(BeaconRegister
                .class
                .getClassLoader()
                .getResourceAsStream("ranshinban-256x256.png")));
        mainStage.setResizable(false);
        mainStage.show();
    }
    public static void registerBeacon(Beacon beacon,Boolean append, Boolean ignoreRegisteredCheck)
    {
        try
        {
            if(checkIfRegistered(beacon) && !ignoreRegisteredCheck)
            {
                errorWindow.raiseErrorWindow("Beacon is already in database");
                return;
            }

            synchronized (beaconDB)
            {
                beaconDB.getParentFile().mkdir();
                if(!beaconDB.exists()) beaconDB.createNewFile();

                FileWriter fileWriter = new FileWriter(beaconDB,append);

                fileWriter.write(beacon.getDeviceName()
                        + ","
                        + beacon.getMacAddress()
                        + ","
                        + beacon.getxCoordinate()
                        + ","
                        + beacon.getyCoordinate()
                        + ","
                        + beacon.getReferenceRSSI()
                        + ";\n");
                fileWriter.flush();
                fileWriter.close();
            }
            refreshBeaconList();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            errorWindow.raiseErrorWindow(e.getStackTrace().toString());
        }
    }
    public static void refreshBeaconList()
    {
        try
        {
            synchronized (beaconDB)
            {
                beaconDB.getParentFile().mkdir();
                if(!beaconDB.exists()) beaconDB.createNewFile();

                BufferedReader reader = new BufferedReader(new FileReader(beaconDB));

                ArrayList<String> lines = new ArrayList<>();

                for(String line = reader.readLine(); line != null; line = reader.readLine())
                {
                    lines.add(line);
                }

                registeredBeacons.clear();

                if(lines.size() == 0) return;

                for(String line : lines)
                {
                    String[] beaconData = line.split(",");
                    if(beaconData.length < 5) continue;
                    Beacon parsedBeacon = new Beacon(beaconData[0],beaconData[1],0,0);
                    parsedBeacon.setxCoordinate(Double.parseDouble(beaconData[2]));
                    parsedBeacon.setyCoordinate(Double.parseDouble(beaconData[3]));
                    parsedBeacon.setReferenceRSSI(Integer.parseInt(beaconData[4].replaceAll(";","")));
                    registeredBeacons.add(parsedBeacon);
                }
            }

        }
        catch (Exception e)
        {
            System.out.println("PARSING ERROR");
            e.printStackTrace();
            errorWindow.raiseErrorWindow(e.getMessage());
        }
    }
    public static Beacon[] getRegisteredBeacons()
    {
        refreshBeaconList();

        return ((ArrayList<Beacon>) registeredBeacons.clone()).toArray(new Beacon[0]);
    }
    public static void showBeaconList()
    {
        VBox root = new VBox();
        HBox controlBox = new HBox();
        Button modifyButton = new Button("Modify");
        Button removeButton = new Button("Remove");

        beaconList = new ListView();
        beaconList.getItems().addAll(registeredBeacons);
        beaconList.setOnMouseClicked(e ->
        {
            if(e.getClickCount() >= 2)
            {
                if(beaconList.getSelectionModel().getSelectedItem() != null)
                {
                    Beacon beacon = beaconList.getSelectionModel().getSelectedItem();
                    removeBeacon(beacon);
                    setupBeacon(beacon);
                    if(!checkIfRegistered(beacon)) registerBeacon(beacon,true,false);
                }
            }
        });
        beaconList.getSelectionModel().selectedItemProperty().addListener(
                (v,oldVal,newVal)->{
                 if(newVal != null)
                 {
                     modifyButton.setDisable(false);
                     removeButton.setDisable(false);
                 }
                 else
                 {
                     modifyButton.setDisable(true);
                     removeButton.setDisable(true);
                 }
                });
        refreshBeaconList();

        beaconList.getItems().clear();
        beaconList.getItems().addAll((ArrayList<Beacon>) registeredBeacons.clone());

        controlBox.setSpacing(10);
        controlBox.getChildren().addAll(modifyButton,removeButton);

        modifyButton.prefWidthProperty().bind(controlBox.widthProperty().divide(3));
        modifyButton.setDisable(true);
        modifyButton.setOnAction(e ->
        {
            Beacon beacon = beaconList.getSelectionModel().getSelectedItem();
            removeBeacon(beacon);
            setupBeacon(beacon);
            if(!checkIfRegistered(beacon)) registerBeacon(beacon,true,false);
        });


        removeButton.prefWidthProperty().bind(modifyButton.prefWidthProperty());
        removeButton.setDisable(true);
        removeButton.setOnAction(
                e->
                {
                    removeBeacon((Beacon) beaconList.getSelectionModel().getSelectedItem());
                    beaconList.getItems().remove(beaconList.getSelectionModel().getSelectedItem());
                });


        root.setSpacing(10);
        root.setPadding(new Insets(10,10,10,10));
        root.getChildren().addAll(controlBox,beaconList);

        Scene scene = new Scene(root);
        Stage stage = new Stage();

        stage.setScene(scene);

        stage.sizeToScene();
        stage.setTitle("Beacon List");
        stage.show();

    }
    static private void showBeaconProperties(Beacon beacon)
    {
        if(beacon == null) return;

        VBox root = new VBox();
        Scene mainScene = new Scene(root);
        Stage stage = new Stage();

        Label beaconInfo = new Label();
        beaconInfo.setText("Name: " + beacon.getDeviceName() + "\n"
        + "MAC Address: " + beacon.getMacAddress() + "\n"
        + "X coordinate: " + beacon.getxCoordinate() + "\n"
        + "Y coordinate: " + beacon.getyCoordinate() + "\n"
        );
        beaconInfo.setTextAlignment(TextAlignment.LEFT);

        root.getChildren().add(beaconInfo);
        root.setPadding(new Insets(10,10,10,10));

        stage.setScene(mainScene);
        stage.setTitle("Beacon Properties");
        stage.sizeToScene();
        stage.setResizable(false);
        stage.show();
    }
    private static void rewriteBeaconDB()
    {

        if( registeredBeacons.size() == 0 || registeredBeacons.get(0) == null)
        {
            try
            {
                synchronized (beaconDB)
                {
                    if(!beaconDB.exists())
                    {
                        beaconDB.getParentFile().mkdir();
                        beaconDB.createNewFile();
                    }

                    FileWriter fileWriter = new FileWriter(beaconDB,false);
                    fileWriter.write("");
                    fileWriter.close();
                }
            }
            catch (Exception e)
            {
                errorWindow.raiseErrorWindow(e.getMessage());
                System.out.println(e.getStackTrace());
            }
        }
        else
        {
            Beacon first = registeredBeacons.get(0);
            registerBeacon(first
                    ,false
                    ,true
            );
            for(Beacon beacon : registeredBeacons.subList(1, registeredBeacons.size()))
            {
                registerBeacon(beacon
                        ,true
                        ,true
                );
            }
        }
    }
    static private void removeBeacon(Beacon beacon)
    {
        registeredBeacons.remove(beacon);
        synchronized (beaconDB)
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(beaconDB));

                ArrayList<String> lines = new ArrayList<>();

                Pattern pattern = Pattern.compile("^.*" + beacon.getMacAddress().trim() + ".*$");

                String line = reader.readLine();

                while(line != null)
                {
                    if(!pattern.matcher(line.trim()).matches())
                    {
                        lines.add(line + "\n");
                    }
                    line = reader.readLine();
                }
                reader.close();

                BufferedWriter writer = new BufferedWriter(new FileWriter(beaconDB));
                PrintWriter printWriter = new PrintWriter(writer);

                printWriter.write(String.join("", lines));
                printWriter.close();
                writer.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorWindow.raiseErrorWindow(e.getMessage());
            }
        }
    }
    static private boolean checkIfRegistered(Beacon beacon)
    {
        return registeredBeacons.contains(beacon);
    }
    static public void refreshBeaconListView()
    {
        if(beaconList == null) return;
        refreshBeaconList();
        beaconList.getItems().clear();
        beaconList.getItems().addAll(registeredBeacons);
    }
    static public void updateBeacon(Beacon beacon)
    {
        synchronized (beaconDB)
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(beaconDB));

                ArrayList<String> lines = new ArrayList<>();

                Pattern pattern = Pattern.compile("^.*" + beacon.getMacAddress().trim() + ".*$");

                String line = reader.readLine();

                while(line != null)
                {
                    if(!pattern.matcher(line.trim()).matches())
                    {
                        lines.add(line);
                    }
                    else
                    {
                        lines.add(beacon.getDeviceName() + "," + beacon.getMacAddress() + "," + beacon.getxCoordinate() + "," + beacon.getyCoordinate() +"," + beacon.getReferenceRSSI() + ";\n");
                    }
                    line = reader.readLine();
                }
                reader.close();
                BufferedWriter writer = new BufferedWriter(new FileWriter(beaconDB));
                PrintWriter printWriter = new PrintWriter(writer);
                printWriter.write(String.join("\n", lines));
                printWriter.close();
                writer.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorWindow.raiseErrorWindow(e.getStackTrace().toString());
            }
        }
    }
}

package com.ranshinban.ranshinban.BLE;

import com.ranshinban.ranshinban.Localization.BeaconMapper;
import com.ranshinban.ranshinban.Localization.BeaconRegister;
import com.ranshinban.ranshinban.Localization.RssiCalibrator;

import com.fazecast.jSerialComm.SerialPort;
import com.ranshinban.ranshinban.utils.SerialDebug;
import com.ranshinban.ranshinban.utils.SerialHandler;
import com.ranshinban.ranshinban.utils.errorWindow;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;

public class Scanner
{
    static private volatile Beacon[] scannedBeacons = null;
    static private TableView<Beacon> scannedBeaconsTable = new TableView<>();

    static private Button scanButton;
    static private Button debugButton;
    static private Button registeredBeaconsButton;
    static private Button rssiCalibrationButton;
    static private Button generateMapButton;

    private static ComboBox serialBox = new ComboBox();

    static private ImageView scannerIcon;

    static private volatile boolean scanActive = false;

    static private Thread scannerThread = new Thread(()->
    {
        while(true)
        {
            Platform.runLater(() ->
            {
                    scannerIcon.setVisible(scanActive);
                    scanButton.setText(scanActive ? "Stop" : "Start");
            });

            if (scanActive && !SerialHandler.portOpen())
            {
                scanActive = false;
                Platform.runLater(()-> errorWindow.raiseErrorWindow("BLE scanner error: could not open port"));
            }

            while (SerialHandler.portOpen() && scanActive)
            {
                try
                {
                    Platform.runLater(()->{
                        if(RssiCalibrator.calibrating()) rssiCalibrationButton.setDisable(true);
                        else rssiCalibrationButton.setDisable(false);
                    });
                    while (!SerialHandler.scanAvailable())
                    {
                        Platform.runLater(()->{
                            if(RssiCalibrator.calibrating()) rssiCalibrationButton.setDisable(true);
                            else rssiCalibrationButton.setDisable(false);
                        });
                        if(!scanActive) break;
                        Thread.sleep(100);
                    }
                    scannedBeacons = stringToBeacon(SerialHandler.getScanBuffer());
                    if(scannedBeacons != null)
                    {
                        for(Beacon beacon : scannedBeacons)
                        {
                            beacon.setRefreshed(true);
                        }
                    }
                    updateScannedBeaconTable();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            try
            {
                Thread.sleep(16);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    });
    static private Beacon[] stringToBeacon(String input)
    {
        if(input == null || input.length() == 0) return null;

        String[] results = input.split("\n");
        ArrayList<Beacon> beacons = new ArrayList<>();

        for(String result : results)
        {
            if(result.replaceAll("\\s","").equalsIgnoreCase("END;"))
            {
                continue;
            }
            String[] split = result.split(";");

            HashMap<String,String> parsedResult = new HashMap<>();

            for(String section : split)
            {
                String[] sectionSplit = section.split("=");
                if(sectionSplit.length < 2) return null;
                parsedResult.put(sectionSplit[0].replaceAll("\\s+$","")
                        , section.split("=")[1].replaceAll("^\\s+",""));
            }

            Beacon resultingBeacon = new Beacon(parsedResult.get("device_name")
                ,parsedResult.get("address")
                ,Integer.parseInt(parsedResult.get("rssi"))
                ,Integer.parseInt(parsedResult.get("tx_power")));

            beacons.add(resultingBeacon);
        }
        scannedBeacons = beacons.toArray(new Beacon[beacons.size()]);
        return scannedBeacons;
    }
    static public VBox scannerPanel()
    {
        VBox vbox = new VBox();
        HBox controlBox = new HBox();

        vbox.setSpacing(10);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.setMaxHeight(Double.MAX_VALUE);

        vbox.setFillWidth(true);

        scanButton = new Button("Scan");
        debugButton = new Button("Debug");
        registeredBeaconsButton = new Button("Browse");
        rssiCalibrationButton = new Button("Calibrate");
        generateMapButton = new Button("Map");


        scanButton.prefWidthProperty().bind(controlBox.widthProperty().divide(6));
        debugButton.prefWidthProperty().bind(scanButton.prefWidthProperty());
        registeredBeaconsButton.prefWidthProperty().bind(scanButton.prefWidthProperty());
        rssiCalibrationButton.prefWidthProperty().bind(scanButton.prefWidthProperty());
        generateMapButton.prefWidthProperty().bind(scanButton.prefWidthProperty());
        serialBox.prefWidthProperty().bind(scanButton.prefWidthProperty().multiply(2));

        scannerIcon = new ImageView(
                new Image(
                Scanner.class
                .getClassLoader()
                .getResourceAsStream("scanner.gif")
                )
        );
        scannerIcon.setFitWidth(30);
        scannerIcon.setFitHeight(30);
        scannerIcon.setVisible(false);

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.prefWidthProperty().bind(
                    controlBox.widthProperty()
                            .subtract(scanButton.prefWidthProperty().multiply(6))
                            .subtract(scannerIcon.getFitWidth())
        );
        separator.setStyle(" ");

        controlBox.setSpacing(10);

        TableColumn<Beacon,String> nameCol = new TableColumn<>("Device Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeviceName()));
        TableColumn<Beacon,String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMacAddress()));
        TableColumn<Beacon,Integer> rssiCol = new TableColumn<>("RSSI");
        rssiCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRssi()).asObject());
        TableColumn<Beacon,Integer> txPowerCol = new TableColumn<>("Tx Power");
        txPowerCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTxPower()).asObject());

        nameCol.prefWidthProperty().bind(vbox.widthProperty().divide(3));
        addressCol.prefWidthProperty().bind(vbox.widthProperty().divide(3));
        rssiCol.prefWidthProperty().bind(vbox.widthProperty().divide(6));
        txPowerCol.prefWidthProperty().bind(vbox.widthProperty().divide(6));

        scannedBeaconsTable.getColumns().add(nameCol);
        scannedBeaconsTable.getColumns().add(addressCol);
        scannedBeaconsTable.getColumns().add(rssiCol);
        scannedBeaconsTable.getColumns().add(txPowerCol);

        scannedBeaconsTable.prefHeightProperty().bind(vbox.heightProperty());

        serialBox.setOnMouseClicked((Event e)->
        {
            serialBox.getItems().clear();
            SerialPort[] serialPorts = SerialPort.getCommPorts();
            for(SerialPort serialPort : serialPorts)
            {
                serialBox.getItems().add(serialPort);
            }
        });
        serialBox.setConverter(new StringConverter<SerialPort>()
                               {
                                   public String toString(SerialPort port)
                                   {
                                       return port == null ? null : port.getSystemPortName();
                                   }
                                   public SerialPort fromString(String string)
                                   {
                                       return null;
                                   }
                               }
        );
        serialBox.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) ->
        {
            if(newValue != null)
            {
                SerialHandler.openPort((SerialPort) newValue,115200);
            }
        });

        scanButton.setOnAction(e ->
        {
            scanActive = !scanActive;
        });

        debugButton.setOnAction(e ->
        {
            SerialDebug.serialDebug();
        });

        registeredBeaconsButton.setOnAction(
                e ->
                {
                    BeaconRegister.showBeaconList();
                }
        );
        rssiCalibrationButton.setOnAction(
                e ->
                {
                    RssiCalibrator.setupRssiReference();
                }
        );

        generateMapButton.setOnAction(
                e ->
                {
                    BeaconMapper.beaconMapHelper();
                }
        );
        scannedBeaconsTable.setOnMouseClicked(e->
        {
            if(e.getClickCount() >= 2 && scannedBeaconsTable.getSelectionModel().getSelectedItem() != null)
            {
                BeaconRegister.setupBeacon(scannedBeaconsTable.getSelectionModel().getSelectedItem());
            }
        });

        controlBox.getChildren().addAll(scanButton,debugButton,registeredBeaconsButton,rssiCalibrationButton,generateMapButton,serialBox,separator,scannerIcon);
        vbox.getChildren().addAll(controlBox, scannedBeaconsTable);

        scannerThread.start();

        return vbox;
    }
    static private void updateScannedBeaconTable()
    {
        if (scannedBeacons != null)
        {
            ObservableList<Beacon> beacons = FXCollections.observableArrayList(scannedBeacons);

            Platform.runLater(()->
            {
                scannedBeaconsTable.getItems().clear();
                scannedBeaconsTable.setItems(beacons);
            });
        }
    }
    static public Beacon[] getScannedBeacons()
    {
        return scannedBeacons;
    }
    static public Boolean isActive()
    {
        return scanActive;
    }
    static public Beacon getBeacon(String address,boolean checkIfNew)
    {
        if(scannedBeacons == null) return null;
        for(Beacon beacon : scannedBeacons)
        {
            if(beacon.getMacAddress().equals(address))
            {
                if(!beacon.isRefreshed() && checkIfNew) return null;
                beacon.setRefreshed(false);
                return beacon;
            }
        }
        return null;
    }

}

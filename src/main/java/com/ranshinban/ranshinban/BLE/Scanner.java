package com.ranshinban.ranshinban.BLE;

import com.ranshinban.ranshinban.Localization.BeaconMapper;
import com.ranshinban.ranshinban.Localization.BeaconRegister;
import com.ranshinban.ranshinban.Localization.RssiCalibrator;

import com.fazecast.jSerialComm.SerialPort;
import com.ranshinban.ranshinban.Wireless.httpClient;
import com.ranshinban.ranshinban.utils.SerialDebug;
import com.ranshinban.ranshinban.utils.SerialHandler;
import com.ranshinban.ranshinban.utils.errorWindow;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import org.controlsfx.glyphfont.FontAwesome;

import java.util.ArrayList;
import java.util.HashMap;

public class Scanner
{
    static private volatile Beacon[] scannedBeacons = null;
    static private TableView<Beacon> scannedBeaconsTable = new TableView<>();

    static private Button registeredBeaconsButton;
    static private Button rssiCalibrationButton;
    static private Button generateMapButton;
    static private Button serialConnectButton;
    static private Button connectButton;

    static private SVGPath serialSVG = new SVGPath();
    static private SVGPath internetSVG = new SVGPath();

    static private  TextField urlField = new TextField();

    static private  ComboBox serialBox = new ComboBox();

    static private ImageView scannerIcon;

    static private Thread scannerThread = new Thread(()->
    {
        while(true)
        {
            Platform.runLater(() -> scannerIcon.setVisible(SerialHandler.portOpen() || httpClient.isActivated()));
            try
            {
            while (SerialHandler.portOpen())
            {
                while (!SerialHandler.scanAvailable())
                {
                    if(!SerialHandler.portOpen()) break;
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
            while(httpClient.isActivated())
            {
                httpClient.requestBeaconList(urlField.getText());
                scannedBeacons = stringToBeacon(httpClient.getResponseProperty().get());
                if(scannedBeacons != null)
                {
                    for(Beacon beacon : scannedBeacons)
                    {
                        beacon.setRefreshed(true);
                    }
                }
                updateScannedBeaconTable();
                Thread.sleep(100);
            }
                Thread.sleep(16);
            }
            catch (Exception e)
            {
                httpClient.deactivateClient();
                SerialHandler.closePort();
                Platform.runLater(()->
                {
                    serialConnectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
                    connectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
                    errorWindow.raiseErrorWindow(e.getMessage());
                });

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
        HBox controlGrid = new HBox();
        HBox controlBox = new HBox();

        DoubleBinding controlsWidth = controlBox.widthProperty().divide(4.5);

        vbox.setSpacing(10);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.setMaxHeight(Double.MAX_VALUE);

        vbox.setFillWidth(true);

        registeredBeaconsButton = new Button("Browse");
        rssiCalibrationButton = new Button("Calibrate");
        generateMapButton = new Button("Map");
        serialConnectButton = new Button();
        connectButton = new Button();

        serialConnectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
        serialConnectButton.getStyleClass().clear();
        serialConnectButton.setStyle(null);

        connectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
        connectButton.getStyleClass().clear();
        connectButton.setStyle(null);

        registeredBeaconsButton.prefWidthProperty().bind(controlsWidth);
        rssiCalibrationButton.prefWidthProperty().bind(controlsWidth);
        generateMapButton.prefWidthProperty().bind(controlsWidth);

        connectButton.prefWidthProperty().bind(controlsWidth.divide(4));
        serialConnectButton.prefWidthProperty().bind(controlsWidth.divide(4));
        serialBox.prefWidthProperty().bind(controlsWidth.multiply(2));
        urlField.prefWidthProperty().bind(controlsWidth.multiply(2));

        scannerIcon = new ImageView(
                new Image(
                Scanner.class
                .getClassLoader()
                .getResourceAsStream("scanner.gif")
                )
        );
        scannerIcon.setFitWidth(50);
        scannerIcon.setFitHeight(50);
        scannerIcon.setVisible(false);

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.prefWidthProperty().bind(controlBox.widthProperty().subtract(controlGrid.widthProperty()));
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


        connectButton.setOnAction(e ->
        {
            if(httpClient.isActivated())
            {
                httpClient.deactivateClient();
                connectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
            }
            else
            {
                SerialHandler.closePort();
                serialConnectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
                connectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_ON,"2em"));
                httpClient.activateClient();
            }
        });
        serialConnectButton.setOnAction(e ->
        {
           if(serialBox.getSelectionModel().getSelectedItem() == null)
           {
               errorWindow.raiseErrorWindow("No Serial port selected");
               return;
           }
           if(SerialHandler.portOpen())
           {
               SerialHandler.closePort();
               serialConnectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
           }
           else
           {
               httpClient.deactivateClient();
               SerialHandler.openPort((SerialPort) serialBox.getSelectionModel().getSelectedItem(),115200);
               serialConnectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_ON,"2em"));
               connectButton.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.TOGGLE_OFF,"2em"));
           }
        });

        registeredBeaconsButton.setOnAction(
                e ->
                {
                    BeaconRegister.showBeaconList();
                }
        );

        rssiCalibrationButton.disableProperty().bind(RssiCalibrator.calibrating());
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
                BeaconRegister.setupBeacon(scannedBeaconsTable.getSelectionModel().getSelectedItem(),false);
            }
        });

        urlField.setPromptText("Enter server URL");

        controlGrid.setSpacing(5);

        VBox column1 = new VBox();
        VBox column2 = new VBox();
        VBox column3 = new VBox();
        VBox column4 = new VBox();

        column1.setSpacing(5);
        column2.setSpacing(5);
        column3.setSpacing(5);
        column4.setSpacing(5);

        column1.getChildren().addAll(registeredBeaconsButton,rssiCalibrationButton);
        column2.getChildren().addAll(generateMapButton);
        column3.getChildren().addAll(urlField,serialBox);
        column4.getChildren().addAll(connectButton,serialConnectButton);

        controlGrid.getChildren().addAll(column1,column2,column3,column4);

        controlBox.getChildren().addAll(controlGrid,separator,scannerIcon);
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
        return SerialHandler.portOpen() || httpClient.isActivated();
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

    /* DO YOU REALLY HAVE TO DO THIS IF YOU WANT TO WORK WITH SVGs? LORD GIVE ME STRENGTH*/
    static private SVGPath getUSBsvg()
    {
        SVGPath path = new SVGPath();
        path.setContent("M14.347 6.4h-3.68v3.68h1.6v2.56c0 0.587 0 0.96-0.587 1.547l-3.68 3.627v-14.88l0.747 1.227 0.907-0.533-2.187-3.627-2.187 3.627 0.907 0.533 0.747-1.227v19.253l-3.36-3.36c-0.587-0.587-0.907-1.067-0.907-1.44v-2.507c0.907-0.213 1.6-1.067 1.6-2.080 0-1.173-0.96-2.133-2.133-2.133s-2.133 0.96-2.133 2.133c0 1.013 0.693 1.813 1.6 2.080v2.507c0 0.907 0.64 1.653 1.227 2.187l4.16 4.16v2.987c-1.227 0.267-2.133 1.333-2.133 2.613 0 1.493 1.173 2.667 2.667 2.667s2.667-1.173 2.667-2.667c0-1.28-0.907-2.347-2.133-2.613v-7.36l4.427-4.427c0.907-0.907 0.907-1.653 0.907-2.293v-2.56h1.013v-3.68zM1.067 12.8c0-0.587 0.48-1.067 1.067-1.067s1.067 0.48 1.067 1.067c0 0.587-0.48 1.067-1.067 1.067s-1.067-0.48-1.067-1.067zM9.067 29.333c0 0.907-0.693 1.6-1.6 1.6s-1.6-0.693-1.6-1.6c0-0.907 0.693-1.6 1.6-1.6s1.6 0.693 1.6 1.6zM11.733 7.467h1.547v1.547h-1.547v-1.547z\n"
        );
        path.setStroke(Color.web("#000000"));
        path.setStrokeWidth(0.1);
        path.setScaleX(0.8);
        path.setScaleY(0.8);
        path.setRotate(90);
        return path;
    }
    static private SVGPath getWebsvg()
    {
        SVGPath path = new SVGPath();
        path.setContent(
                "M30,11H25V21h2V18h3a2.0027,2.0027,0,0,0,2-2V13A2.0023,2.0023,0,0,0,30,11Zm-3,5V13h3l.001,3Z"
                + "M10 13 12 13 12 21 14 21 14 13 16 13 16 11 10 11 10 13"
                + "M23 11 17 11 17 13 19 13 19 21 21 21 21 13 23 13 23 11"
                + "M6 11 6 15 3 15 3 11 1 11 1 21 3 21 3 17 6 17 6 21 8 21 8 11 6 11"
        );
        path.setStroke(Color.web("#000000"));
        path.setStrokeWidth(0.01);

        return path;
    }
}

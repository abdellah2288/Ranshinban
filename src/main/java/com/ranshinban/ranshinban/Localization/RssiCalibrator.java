package com.ranshinban.ranshinban.Localization;

import com.ranshinban.ranshinban.BLE.Beacon;
import com.ranshinban.ranshinban.BLE.Scanner;
import com.ranshinban.ranshinban.utils.errorWindow;
import com.ranshinban.ranshinban.utils.popupWindow;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;


public class RssiCalibrator
{
    static private volatile Beacon selectedBeacon = null;

    static private final NumberAxis rssiValues = new NumberAxis();
    static private final NumberAxis sampleCount = new NumberAxis();

    static private final ScatterChart<Number,Number> rssiPlot = new ScatterChart(sampleCount,rssiValues);
    static private final XYChart.Series<Number,Number> rssiSeries = new XYChart.Series<>();

    static private final TextField averageField = new TextField();
    static private final TextField sampleCountField = new TextField();

    static private final ListView<Beacon> beaconList = new ListView<>();

    static private volatile int sampleCounter = 0;

    static private final Button getReferenceButton = new Button("Start Calibration");

    static private final ProgressBar calibrationProgress = new ProgressBar();

    static private final Stage mainStage = new Stage();

    static private volatile boolean calibrating = false;

    static public void setupRssiReference()
    {
        if(!Scanner.isActive())
        {
            errorWindow.raiseErrorWindow("BLE Scanner inactive");
            return;
        }

        rssiPlot.getData().clear();
        rssiPlot.getData().add(rssiSeries);

        rssiValues.setLabel("RSSI");
        sampleCount.setLabel("SAMPLE COUNT");

        TextField beaconName = new TextField();
        TextField beaconAddress = new TextField();
        TextField absoluteX = new TextField();
        TextField absoluteY = new TextField();


        Label beaconNameLabel = new Label("Name");
        Label beaconAddressLabel = new Label("Address");
        Label absoluteXLabel = new Label("Absolute X");
        Label absoluteYLabel = new Label("Absolute Y");
        Label averageLabel = new Label("RSSI average");
        Label sampleCountLabel = new Label("Number of calibration samples");

        Button refreshButton = new Button("⟳");
        Button exportButton = new Button("Export Data");



        HBox root = new HBox();
        HBox selectorControls = new HBox();
        VBox informationCard = new VBox();
        VBox beaconSelector = new VBox();
        Scene mainScene = new Scene(root);

        root.setSpacing(10);
        root.setPadding(new Insets(20,20,20,20));

        selectorControls.setSpacing(10);

        informationCard.setSpacing(10);

        beaconSelector.setSpacing(10);

        getReferenceButton.setDisable(true);

        exportButton.setOnAction(
                e ->
                {
                    FileChooser fileChooser = new FileChooser();
                    File file = fileChooser.showSaveDialog(mainStage);
                    try
                    {
                        FileWriter fileWriter = new FileWriter(file);
                        for(XYChart.Data<Number,Number> data : rssiSeries.getData())
                        {
                            fileWriter.write(data.getXValue().toString() + "," + data.getYValue().toString() + "\n");
                        }
                        fileWriter.close();
                    }
                    catch (Exception ex)
                    {
                        errorWindow.raiseErrorWindow(ex.getMessage());
                    }
                });

        refreshButton.setOnAction(
                e ->
                {
                    beaconList.getItems().clear();
                    beaconList.getItems().addAll(BeaconRegister.getRegisteredBeacons());
                });

        getReferenceButton.setOnAction(
                e ->
                {
                    if(calibrating)
                    {
                        calibrating = false;
                    }
                    else
                    {
                        calibrating = true;
                        new Thread(()-> refreshRSSI()).start();
                    }
                }
        );

        sampleCountField.textProperty().addListener(
                (v,oldVal,newVal)->{
                    try
                    {
                        Integer.parseInt(newVal);
                        if(selectedBeacon != null) getReferenceButton.setDisable(false);
                    }
                    catch (NumberFormatException ex)
                    {
                        getReferenceButton.setDisable(true);
                    }
                }
        );

        beaconList.setOnMouseClicked(e->
        {
            if(e.getClickCount() >= 2 &&
                    beaconList.getSelectionModel().getSelectedItem() != null)
            {
                Beacon beacon = beaconList.getSelectionModel().getSelectedItem();
                beaconName.setText(beacon.getDeviceName());
                beaconAddress.setText(beacon.getMacAddress());
                absoluteX.setText(String.valueOf(beacon.getxCoordinate()));
                absoluteY.setText(String.valueOf(beacon.getyCoordinate()));
                selectedBeacon = beacon;
                rssiSeries.getData().clear();
                rssiSeries.setName(beacon.getDeviceName());
            }
        });
        beaconName.setEditable(false);
        beaconAddress.setEditable(false);
        absoluteX.setEditable(false);
        absoluteY.setEditable(false);
        averageField.setEditable(false);

        selectorControls.getChildren().addAll(exportButton,getReferenceButton);

        beaconSelector.getChildren().addAll(refreshButton,beaconList);

        informationCard.getChildren().addAll(beaconNameLabel
                ,beaconName
                ,beaconAddressLabel
                ,beaconAddress
                ,absoluteXLabel
                ,absoluteX
                ,absoluteYLabel
                ,absoluteY
                ,averageLabel
                ,averageField
                ,sampleCountLabel
                ,sampleCountField
                ,selectorControls
                ,calibrationProgress
        );

        calibrationProgress.prefWidthProperty().bind(informationCard.widthProperty());
        calibrationProgress.setVisible(false);

        informationCard.prefWidthProperty().bind(mainStage.widthProperty().divide(3));
        beaconSelector.prefWidthProperty().bind(mainStage.widthProperty().divide(3));
        rssiPlot.prefWidthProperty().bind(mainStage.widthProperty().divide(2));

        root.getChildren().addAll(informationCard,rssiPlot,beaconSelector);
        mainStage.setScene(mainScene);
        mainStage.setTitle("Rssi calibration");
        mainStage.setMinHeight(480);
        mainStage.setMinWidth(640);
        mainStage.setWidth(1024);
        mainStage.getIcons().add(new Image(RssiCalibrator
                .class
                .getClassLoader()
                .getResourceAsStream("ranshinban-256x256.png")));
        mainStage.show();

    }
    static private void refreshRSSI()
    {
        sampleCounter = 0;

        rssiSeries.getData().clear();

        Platform.runLater(()-> {
            sampleCountField.setDisable(true);
            getReferenceButton.setText("Stop calibration");
            beaconList.setDisable(true);
        });

        while(mainStage.isShowing())
        {
            try
            {

                while (selectedBeacon == null)
                {
                    Thread.sleep(20);
                }

                Platform.runLater(()-> calibrationProgress.setVisible(true));

                Beacon  beacon = Scanner.getBeacon(selectedBeacon.getMacAddress(), true);

                while (beacon == null)
                {
                    beacon = Scanner.getBeacon(selectedBeacon.getMacAddress(), true);
                    if(!calibrating)
                    {
                        Platform.runLater(()-> {
                            calibrationProgress.setVisible(false);
                            sampleCountField.setDisable(false);
                            getReferenceButton.setText("Start calibration");
                            beaconList.setDisable(false);
                        });
                        return;
                    }
                    Thread.sleep(20);
                }
                final Beacon _beacon = beacon;
                Platform.runLater(
                        () ->
                        {
                            sampleCounter++;
                            calibrationProgress.setProgress(sampleCounter/Double.valueOf(sampleCountField.getText()));
                            rssiSeries.getData().add(new XYChart.Data<>(sampleCounter,_beacon.getRssi()));
                            averageField.setText(String.valueOf(getAverageRSSI()));
                        }
                );

                if(sampleCounter == Integer.valueOf(sampleCountField.getText()))
                {
                    if(selectedBeacon != null)
                    {
                        selectedBeacon.setReferenceRSSI((int) Math.round(getAverageRSSI()));
                        BeaconRegister.updateBeacon(selectedBeacon);
                        Platform.runLater(()-> popupWindow.raisePopupWindow("Beacon calibration done !","Beacon Calibration"));
                    }
                    else
                    {
                        Platform.runLater(() -> errorWindow.raiseErrorWindow("No beacon selected"));
                    }
                    Platform.runLater(()-> {
                        calibrationProgress.setVisible(false);
                        sampleCountField.setDisable(false);
                        getReferenceButton.setText("Start calibration");
                        beaconList.setDisable(false);
                    });
                    break;
                }

                Thread.sleep(20);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Platform.runLater(() -> errorWindow.raiseErrorWindow(e.getMessage()));
                Platform.runLater(()-> {
                    sampleCountField.setDisable(false);
                    getReferenceButton.setText("Start calibration");
                    beaconList.setDisable(false);
                });
            }
        }

    }
    static private double getAverageRSSI()
    {
        Double total = 0.0;
        for(XYChart.Data<Number,Number> data : rssiSeries.getData())
        {
        total += data.getYValue().doubleValue();
        }
        return total/sampleCounter;
    }
    static public ReadOnlyBooleanProperty calibrating()
    {
        return mainStage.showingProperty();
    }
}

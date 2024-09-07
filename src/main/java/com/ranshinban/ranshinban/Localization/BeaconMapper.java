package com.ranshinban.ranshinban.Localization;

import com.ranshinban.ranshinban.BLE.Beacon;
import com.ranshinban.ranshinban.BLE.Scanner;
import com.ranshinban.ranshinban.utils.errorWindow;
import javafx.application.Platform;
import com.lemmingapex.trilateration.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class BeaconMapper
{
    static private volatile double environmentalConstant = 2.0;
    static private double circleResolution = 200.0;
    static private volatile HashMap<Beacon,XYChart.Series<Number,Number>> beaconData = new HashMap<>();

    static private volatile boolean triLaterate = false;
    static private volatile boolean logEnabled = false;
    static private volatile XYChart.Series target = new XYChart.Series<>();

    static private NumberAxis xAxis = new NumberAxis();
    static private NumberAxis yAxis = new NumberAxis();

    static private LineChart<Number,Number> beaconMap = new LineChart<>(xAxis, yAxis);
    static private HashMap<Beacon,Sphere> mapper3DObjects = new HashMap<>();

    static private  TableView<Beacon> beaconTable = new TableView<>();

    static private Stage mainStage = new Stage();
    static private Stage mapper3DStage = new Stage();

    static private Group mapper3DContainer = new Group();

    static private Scene mapper3DScene = new Scene(mapper3DContainer);

    static private Button trilaterateButton = new Button("▶");
    static private Button logDataButton = new Button("Enable logging");

    static private Label positionLabel = new Label("");

    static private final double  SCALE_FACTOR_3D = 25;

    static private boolean loggingEnabled = false;

    static private File logFile = null;

    static public void beaconMapHelper()
    {
        HBox root = new HBox();
        VBox configBox = new VBox();

        Button refreshButton = new Button("⟳");
        Button display3DMapButton = new Button("3D");

        Label envConstLabel = new Label("Environmental Constant");
        Label circleResolutionLabel = new Label("Circle Resolution");
        Label envConstErrorLabel = new Label("Value outside of range [2:4]");
        Label circleResolutionErrorLabel = new Label();
        Label axisSortingPolicyLabel = new Label("Axis sorting policy");

        TextField envConstTextField = new TextField("2");
        TextField circleResolutionTextField = new TextField("200");

        ChoiceBox<LineChart.SortingPolicy> sortingPolicySelector = new ChoiceBox<>()
        {{
           getItems().addAll(LineChart.SortingPolicy.X_AXIS, LineChart.SortingPolicy.Y_AXIS, LineChart.SortingPolicy.NONE);
        }};

        TableColumn<Beacon,String> beaconNameColumn = new TableColumn<>();
        beaconNameColumn.setCellValueFactory(data -> new SimpleStringProperty( data.getValue().getDeviceName() ));
        beaconNameColumn.setText("Name");

        TableColumn<Beacon,String> beaconRssiColumn = new TableColumn<>();
        beaconRssiColumn.setCellValueFactory(data -> new SimpleStringProperty( String.valueOf(data.getValue().getRssi()) ));
        beaconRssiColumn.setText("RSSI");

        beaconTable.getColumns().addAll(beaconNameColumn,beaconRssiColumn);
        beaconTable.prefWidthProperty().bind(configBox.prefWidthProperty());

        beaconRssiColumn.prefWidthProperty().bind(beaconTable.prefWidthProperty().divide(2));
        beaconNameColumn.prefWidthProperty().bind(beaconTable.prefWidthProperty().divide(2));

        sortingPolicySelector.getSelectionModel().select(LineChart.SortingPolicy.NONE);

        beaconMap.axisSortingPolicyProperty().bind(sortingPolicySelector.valueProperty());

        envConstErrorLabel.setVisible(false);
        circleResolutionErrorLabel.setVisible(false);

        trilaterateButton.setDisable(true);

        envConstTextField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                {
                    try
                    {
                        double fieldValue = Double.parseDouble(newValue);
                        if(fieldValue > 4 || fieldValue < 2)
                        {
                            envConstErrorLabel.setText(" Environmental factor cannot be less than 2 ");
                            envConstErrorLabel.setVisible(true);
                        }
                        else
                        {
                            envConstErrorLabel.setVisible(false);
                            environmentalConstant = fieldValue;
                        }
                    }
                    catch (Exception e)
                    {
                        envConstErrorLabel.setText(newValue + " is not a valid number");
                        envConstErrorLabel.setVisible(true);
                    }
                }
        );

        circleResolutionTextField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                {
                    try
                    {
                        double resolutionValue = Double.parseDouble(newValue);
                        if(resolutionValue < 0)
                        {
                            circleResolutionErrorLabel.setText("resolution must be a positive number");
                            circleResolutionErrorLabel.setVisible(true);
                        }
                        else
                        {
                            circleResolutionErrorLabel.setVisible(false);
                            circleResolution = resolutionValue;
                        }
                    }
                    catch (Exception e)
                    {
                        circleResolutionErrorLabel.setText(newValue + " is not a valid number");
                        circleResolutionErrorLabel.setVisible(true);
                    }
                }
        );

        trilaterateButton.setTooltip(new Tooltip("Trilaterate"));
        trilaterateButton.setOnAction(
                e ->
                {
                    triLaterate = !triLaterate;
                    trilaterateButton.setText(triLaterate ? "⏹" : "▶" );
                }
        );

        refreshButton.setOnAction(
                e ->
                {
                    beaconTable.getItems().clear();
                    beaconTable.getItems().addAll(BeaconRegister.getRegisteredBeacons());
                }
                );

        display3DMapButton.disableProperty().bind(mapper3DStage.showingProperty());
        display3DMapButton.setOnAction(
                e ->
                {
                    start3Dmapper();
                }
        );

        beaconTable.setOnMouseClicked(
                e ->
                {
                    if(e.getClickCount() >= 2)
                    {
                        if(beaconTable.getSelectionModel().getSelectedItem() != null)
                        {
                            Beacon selectedBeacon = beaconTable.getSelectionModel().getSelectedItem();
                            if(!beaconData.containsKey(selectedBeacon))
                            {
                                beaconData.put(selectedBeacon, new XYChart.Series<>());
                                beaconData.get(selectedBeacon).setName(selectedBeacon.toString());
                                beaconMap.getData().add(beaconData.get(selectedBeacon));
                            }
                            else
                            {
                                if(beaconMap.getData().contains(beaconData.get(selectedBeacon)))
                                {
                                    beaconMap.getData().remove(beaconData.get(selectedBeacon));
                                }
                                else
                                {
                                    beaconMap.getData().add(beaconData.get(selectedBeacon));
                                }
                            }
                        }
                    }
                }
        );
        logDataButton.setOnMouseClicked(
                e ->
                {
                    try
                    {
                        if(loggingEnabled)
                        {
                            logFile = null;
                            loggingEnabled = false;
                            logDataButton.setText("Enable logging");
                        }
                        else
                        {
                            logDataButton.setText("Disable logging");
                            FileChooser fileChooser = new FileChooser();
                            logFile = fileChooser.showSaveDialog(mainStage);
                            loggingEnabled = true;
                        }
                    }
                    catch(Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
        );


        target.setName("target");

        beaconMap.prefWidthProperty().bind(mainStage.widthProperty().multiply(3.0/4.0));
        beaconMap.prefHeightProperty().bind(mainStage.heightProperty());

        beaconMap.setScaleX(1);
        beaconMap.setScaleY(1);

        configBox.prefWidthProperty().bind(mainStage.widthProperty().multiply(1.0/4.0));
        configBox.setSpacing(5);
        configBox.setPadding(new Insets(5,5,5,5));
        configBox.getChildren().addAll(
                axisSortingPolicyLabel
                ,sortingPolicySelector
                ,envConstLabel
                , envConstTextField
                ,envConstErrorLabel
                , circleResolutionLabel
                , circleResolutionTextField
                , circleResolutionErrorLabel
                ,trilaterateButton
                ,positionLabel
                ,refreshButton
                ,logDataButton
                ,display3DMapButton
                , beaconTable
        );

        trilaterateButton.prefWidthProperty().bind(configBox.prefWidthProperty());
        refreshButton.prefWidthProperty().bind(configBox.prefWidthProperty());
        logDataButton.prefWidthProperty().bind(configBox.prefWidthProperty());
        display3DMapButton.prefWidthProperty().bind(configBox.prefWidthProperty());

        root.getChildren().addAll(beaconMap, configBox);

        Scene scene = new Scene(root);

        mainStage.setScene(scene);
        mainStage.setTitle("Beacon Map");
        mainStage.setMinWidth(800);
        mainStage.setMinHeight(600);
        mainStage.getIcons().add(new Image(BeaconMapper
                .class
                .getClassLoader()
                .getResourceAsStream("ranshinban-256x256.png")));
        mainStage.show();
        new Thread(()-> refreshMap()).start();
    }

    static private double approximateRadius(Beacon beacon, int RSSI)
    {
        return Math.pow(10.0,(beacon.getReferenceRSSI() - RSSI)/(10 * environmentalConstant));
    }

    static private XYChart.Data[] drawBeaconCircle(Beacon beacon)
    {
            Beacon _beacon =Scanner.getBeacon(beacon.getMacAddress(),true);

            ArrayList<XYChart.Data> circleData = new ArrayList<>();

            if(_beacon == null) return null;

            double circleRadius = approximateRadius(beacon,_beacon.getRssi());
            double centerX = beacon.getxCoordinate();
            double centerY = beacon.getyCoordinate();

            for (int i = 0; i <= circleResolution; i++)
            {
                var t = 2 * Math.PI * i / circleResolution;
                var x = centerX + circleRadius * Math.cos(t);
                var y = centerY + circleRadius * Math.sin(t);
                circleData.add(new XYChart.Data(x, y));
            }

            beacon.setRssi(_beacon.getRssi());
            beacon.setRadius(circleRadius);

            return circleData.toArray(new XYChart.Data[0]);
    }

    static private void refreshMap()
    {
        while(mainStage.isShowing())
        {
            try
            {

                for(Beacon beacon : beaconData.keySet())
                {
                    XYChart.Data[] newSeries = drawBeaconCircle(beacon);

                    if(newSeries != null)
                    {
                        if(triLaterate && beaconData.keySet().size() >= 2)
                        {
                            double[] coordinates = trilateratePosition();
                            Platform.runLater(()->positionLabel.setText("X: " + coordinates[0]
                                    + "\n\nY: " + coordinates[1]
                                    +"\n\nZ: " + coordinates[2]
                            ));
                        }
                        else if(beaconData.keySet().size() < 2)
                        {
                            Platform.runLater(()-> trilaterateButton.setDisable(true));
                        }
                        else if(trilaterateButton.isDisable())
                        {
                            Platform.runLater(()-> trilaterateButton.setDisable(false));
                        }
                        if(loggingEnabled && logFile != null)
                        {
                            FileWriter writer = new FileWriter(logFile,true);
                            BufferedWriter bufferedWriter = new BufferedWriter(writer);
                            bufferedWriter.write(
                                    LocalDateTime.now().getHour()
                                            +"." + LocalDateTime.now().getMinute()
                                            +"."+LocalDateTime.now().getSecond()
                                    +","
                                    +beacon.getDeviceName()
                                    +","
                                    +beacon.getMacAddress()
                                    +","
                                    +beacon.getReferenceRSSI()
                                    +","
                                    +beacon.getRssi()
                                    +","
                                    +beacon.getxCoordinate()
                                    +","
                                    +beacon.getyCoordinate()
                                    +","
                                    +beacon.getzCoordinate()
                                    +","
                                    +beacon.getRadius()
                                    +"\n"
                            );
                            bufferedWriter.flush();
                            bufferedWriter.close();
                            writer.close();
                        }
                        Platform.runLater(()->
                        {
                            beaconData.get(beacon).getData().clear();
                            beaconData.get(beacon).getData().addAll(newSeries);
                            if(mapper3DStage.isShowing())
                            {
                                if(mapper3DObjects.get(beacon) == null)
                                {
                                    mapper3DObjects.put(beacon,new Sphere());

                                    PhongMaterial material = new PhongMaterial();
                                    material.setDiffuseColor(Color.web("#"+macToColor(beacon.getMacAddress()),0.6));
                                    mapper3DObjects.get(beacon).setMaterial(material);
                                }
                                mapper3DObjects.get(beacon).setRadius(beacon.getRadius()*SCALE_FACTOR_3D);
                                mapper3DObjects.get(beacon).translateXProperty().set(beacon.getxCoordinate()*SCALE_FACTOR_3D);
                                mapper3DObjects.get(beacon).translateYProperty().set(beacon.getyCoordinate()*SCALE_FACTOR_3D);
                                mapper3DObjects.get(beacon).translateZProperty().set(beacon.getzCoordinate()*SCALE_FACTOR_3D);
                                if(!mapper3DContainer.getChildren().contains(mapper3DObjects.get(beacon)))
                                {
                                    mapper3DContainer.getChildren().add(mapper3DObjects.get(beacon));
                                }

                            }
                            beaconTable.refresh();
                        }
                        );
                    }
                }
                Thread.sleep(20);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Platform.runLater(() -> errorWindow.raiseErrorWindow(e.getMessage()));
            }
        }
        beaconMap.getData().clear();
    }

    static private double[] trilateratePosition()
    {
        ArrayList<Double[]> centers = new ArrayList<>();
        ArrayList<Double> radii = new ArrayList<>();

        NonLinearLeastSquaresSolver solver;
        Point3D approximatePosition = new Point3D(0,0,0);

        if(beaconData.keySet().size() < 2)
        {
            Platform.runLater(() -> errorWindow.raiseErrorWindow("Trilateration requires at least 3 beacons!"));
            return null;
        }
        for(Beacon beacon : beaconData.keySet())
        {
         centers.add(new Double[]{beacon.getxCoordinate(), beacon.getyCoordinate(), beacon.getzCoordinate()});
         radii.add(beacon.getRadius());
        }

        double[][] _centers = new double[centers.size()][3];
        double[] _radii = new double[radii.size()];

        for(int i = 0; i < _centers.length; i++)
        {
            _centers[i][0] = centers.get(i)[0];
            _centers[i][1] = centers.get(i)[1];
            _centers[i][2] = centers.get(i)[2];
        }
        for(int i = 0; i < _radii.length; i++)
        {
            _radii[i] = radii.get(i);
        }

        solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(_centers,_radii), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();
        double[] solution = optimum.getPoint().toArray();
        return solution;
    }

    static private void start3Dmapper()
    {
        Camera mainCamera = new PerspectiveCamera(true);

        Rotate yRotate = new Rotate(0,Rotate.Y_AXIS);
        Rotate xRotate = new Rotate(0,Rotate.X_AXIS);
        Rotate zRotate = new Rotate(0,Rotate.Z_AXIS);

        Line xAxis = new Line();
        Line yAxis = new Line();

        xAxis.startXProperty().bind(mapper3DScene.widthProperty().multiply(-1));
        xAxis.endXProperty().bind(mapper3DScene.widthProperty());
        xAxis.setStrokeWidth(0.5);
        xAxis.setStroke(Paint.valueOf("#7FFF00"));

        yAxis.startYProperty().bind(xAxis.startXProperty());
        yAxis.endYProperty().bind(xAxis.endXProperty());
        yAxis.setStrokeWidth(0.5);
        yAxis.setStroke(Paint.valueOf("#FF0000"));

        mapper3DContainer.getChildren().addAll(xAxis,yAxis);
        mapper3DContainer.getTransforms().addAll(xRotate,yRotate,zRotate);

        mainCamera.setNearClip(1);
        mainCamera.setFarClip(10000);
        mainCamera.translateXProperty().set(0);
        mainCamera.translateYProperty().set(0);
        mainCamera.translateZProperty().set(-100);

        mapper3DScene.setCamera(mainCamera);
        mapper3DScene.setFill(Color.web("#424242"));


        mapper3DStage.setScene(mapper3DScene);
        mapper3DStage.setHeight(600);
        mapper3DStage.setWidth(800);
        mapper3DStage.setTitle("3D mapper (Experimental)");
        mapper3DStage.show();

        mapper3DScene.setOnKeyPressed(event -> {
           switch(event.getCode())
           {
               case S:
                   mainCamera.translateZProperty().set(mainCamera.translateZProperty().get() - 10);
                   break;
               case W:
                   mainCamera.translateZProperty().set(mainCamera.translateZProperty().get() + 10);
                   break;
               case E:
                   yRotate.angleProperty().set(yRotate.angleProperty().get() + 2);
                   break;
               case Q:
                   yRotate.angleProperty().set(yRotate.angleProperty().get() - 2);
                   break;
               case R:
                   zRotate.angleProperty().set(zRotate.angleProperty().get() + 2);
                   break;
               case F:
                   zRotate.angleProperty().set(zRotate.angleProperty().get() - 2);
                   break;
               case D:
                   xRotate.angleProperty().set(xRotate.angleProperty().get() + 2);
                   break;
               case A:
                   xRotate.angleProperty().set(xRotate.angleProperty().get() - 2);
                   break;
           }
        });
    }

    static private String macToColor(String macAddress)
    {
        String strippedAddress = macAddress.replaceAll(":","").replaceAll("[A-Z:a-z]","");
        String hexColor = String.format("%06X", ((int) Math.round(Double.parseDouble(strippedAddress))) % 16777215 );
        return hexColor;

    }
}
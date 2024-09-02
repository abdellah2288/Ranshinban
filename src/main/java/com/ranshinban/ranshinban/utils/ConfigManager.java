package com.ranshinban.ranshinban.utils;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager
{
    private static final VBox configPane = new VBox();
    private static final Stage configStage = new Stage();
    private static final TextField csvPath = new TextField();
    private static final TextField beaconDBPath = new TextField();

    private static final ComboBox<String> algorithmBox = new ComboBox<>()
    {{
        getItems().addAll("Trilateration","Min Max","K-NN");
    }};
    private static final CheckBox filterBox = new CheckBox("10% cut off");

    private static final Label algorithmLabel = new Label("Localization method");
    private static final Label csvPathLabel = new Label("CSV Path");
    private static final Label beaconDBPathLabel = new Label("Beacon DB Path");

    private static final Button applyButton = new Button("Apply");
    private static final Button csvPathPicker = new Button("...");
    private static final Button beaconDBPicker = new Button("...");

    private static final FileChooser filePicker = new FileChooser();

    private static final Stage motherStage = null;

    private static Map<String,String> settingsMap = new HashMap<>()
    {{
        put("csvPath","");
        put("beaconDBPath","/resources/text/beaconDB");
        put("localizationAlgo","");
        put("filterData","");
    }};

    private static final Path configPath = Paths.get(System.getProperty("user.home") + "/.config/ranshinban/config.conf");

    public static void updateSettings()
    {
        try
        {
            File configFile = new File(configPath.toString());
            if(!configFile.exists())
            {
                configFile.getParentFile().mkdir();
                configFile.createNewFile();
            }

            settingsMap = Parser.parseFile(configFile,settingsMap.keySet());

            csvPath.setText(settingsMap.get("csvPath"));
            beaconDBPath.setText(settingsMap.get("beaconDBPath"));

            algorithmBox.getSelectionModel().select(Integer.getInteger(settingsMap.get("algorithm") == null ? "0" : settingsMap.get("algorithm")));

            filterBox.setSelected(settingsMap.get("filterData").equalsIgnoreCase("true"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    public static void showConfigBox()
    {
        HBox csvPathBox = new HBox();
        csvPathBox.setSpacing(5);
        csvPathBox.getChildren().addAll(csvPath, csvPathPicker);

        HBox beaconDBPathBox = new HBox();
        beaconDBPathBox.setSpacing(5);
        beaconDBPathBox.getChildren().addAll(beaconDBPath, beaconDBPicker);

        csvPathPicker.setOnMouseClicked((Event e) -> { filePickerCallBack(0); });
        beaconDBPicker.setOnMouseClicked((Event e) -> { filePickerCallBack(1); });
        applyButton.setOnMouseClicked((Event e) -> {applyConfig();});

        configPane.setPadding(new Insets(10,10,10,10));
        configPane.getChildren().removeAll();
        configPane.setSpacing(10);
        configPane.getChildren().addAll
                (
                        algorithmLabel,
                        algorithmBox,
                        filterBox,
                        csvPathLabel,
                        csvPathBox,
                        beaconDBPathLabel,
                        beaconDBPathBox,
                        applyButton
                );

        Scene scene = new Scene(configPane);

        configStage.setScene(scene);
        configStage.setTitle("Config");
        configStage.setMinHeight(300);
        configStage.setMinWidth(500);
        configStage.sizeToScene();
        configStage.show();
    }
    public static void applyConfig()
    {
        settingsMap.put("csvPath",csvPath.getText());
        settingsMap.put("beaconDBPath",beaconDBPath.getText());
        if(algorithmBox.getSelectionModel() != null  && algorithmBox.getSelectionModel().getSelectedItem() != null)
        {
            settingsMap.put("localizationAlgo",
                    algorithmBox.getSelectionModel()
                    .getSelectedItem()
                    .compareToIgnoreCase("trilateration") == 0 ? "0" : "1");
        }
        settingsMap.put("filterData",filterBox.isSelected() ? "true" : "false");
        try
        {
            File configFile = new File(configPath.toString());
            if(!configFile.exists())
            {
                configFile.getParentFile().mkdir();
                configFile.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(configFile,false);
            for(Map.Entry<String,String> entry : settingsMap.entrySet())
            {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                fileWriter.write(entry.getKey()+" = "+entry.getValue()+";\n");
            }
            fileWriter.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private static void filePickerCallBack(int type)
    {
        switch(type)
        {
            case 0:
                File csvFile = filePicker.showOpenDialog(motherStage);
                csvPath.setText(csvFile.getAbsolutePath());
                break;
            case 1:
                File beaconDBFile = filePicker.showOpenDialog(motherStage);
                beaconDBPath.setText(beaconDBFile.getAbsolutePath());
                break;
        }
    }
    public static String getConfigValue(String key)
    {
        return settingsMap.get(key);
    }
}

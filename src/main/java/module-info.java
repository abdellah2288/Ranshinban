module com.dotonbori.dotonbori {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.fazecast.jSerialComm;
    requires com.google.gson;
    requires java.sql;
    requires java.desktop;
    requires trilateration;
    requires commons.math3;

    opens com.ranshinban.ranshinban to javafx.fxml;
    exports com.ranshinban.ranshinban;
}
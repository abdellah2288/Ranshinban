
<p align="center">
  <img src="https://github.com/user-attachments/assets/93c66ebf-15c6-436d-baed-4c632f3f9556" />
</p>

# Hoshi no Ranshinban (Star Compass)

**Hoshi no Ranshinban**, or "Star Compass," is an open-source BLE Indoor Localization Assistant. It enables precise benchmarking and calibration of BLE beacons, using RSSI and trilateration to estimate device positions relative to reference beacons. The project offers an intuitive interface for real-time testing, calibration, and visualization, making it an ideal tool for developers and researchers working on indoor positioning systems.

## Features

- **Serial Communication with BLE Boards**: Communicates directly with your BLE-enabled board over serial.
- **HTTP Communication support**: Recieve scan data from a remote server over HTTP.
- **RSSI & Trilateration**: Utilizes RSSI (Received Signal Strength Indicator), Friis equations and trilateration to estimate the position of devices.
- **Beacon Benchmarking & Calibration**: Helps in benchmarking and calibrating BLE beacons for accurate positioning.
- **Adjustable Path Loss Exponent**: Fine-tune the path loss exponent to match your environment for more accurate results.
- **Real-Time 2D & Experimental 3D Mapper**: Visualize calculated distances and positions in both 2D and experimental 3D views.
- **Data logging**: Enables the user to log their calibration and mapping data for easy documentation and analysis.
- **User-Friendly Interface**: An intuitive UI for easy calibration, visualization, and testing.

  ## Working with Ranshinban

  ### Sending scan data

The application can recieve data over both serial and http (the two modes are mutually exclusive), the application expects data to be in the following format:
```
address = DEVICE_ADDRESS;device_name = DEVICE_NAME;tx_power= TX_POWER;rssi = RSSI;
END;
```
With ```END;``` signifying the end of the scan. It is recommended that the HTTP server doesnt send any data if no new data is recieved from the scanner.

### Registering a beacon
In the scan list, double clicking a beacon will popup a registration window. These beacon's info may be modified in the **Registered beacons** List, accessible by clicking on the "Browse" button in the main window.
### RSSI Calibration

Some devices don't advertise their transmitted (not to be confused with transmit) power. So in order to get over that, the application provides the user with a calibration helper.
To calibrate a BLE beacon:
-**Place the scanner 1 meter away from the beacon**
- **Register the beacon**: See section above
- **Refresh the beacon list**: Click on the refresh button in the calibration helper window, this will show a list of registered beacons, double on a beacon to calibrate it.
- **Insert a sample count**: The calibration helper averages the rssi over the number of samples taken, the average is what will be used as the reference RSSI for distance estimation.
- **Click on start calibration**: After clicking on the start calibration button, the helper will start averaging over the number of samples provided, a graph in the middle shows RSSI fluctuations. The user may export the calibration data to a csv file at any point of the process.

### Mapping

The mapper displays a real time 2D map of the estimations. Naturally, knowing only the distance D between the beacon and the scanner will give us a circle of radius D where the scanner is positioned relative to the beacon. In order to get an estimate of the scanner's position in 3D space, at least 3 beacons are needed. The environmental constant or path loss exponent accounts for losses in the environment and should be tuned for better results.
The user may log mapping data by clicking on the **Enable logging** button.

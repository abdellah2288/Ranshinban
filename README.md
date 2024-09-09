
# Hoshi no Ranshinban (Star Compass) ![ranshinban-256x256](https://github.com/user-attachments/assets/93c66ebf-15c6-436d-baed-4c632f3f9556) 

**Hoshi no Ranshinban**, or "Star Compass," is an open-source BLE Indoor Localization Assistant. It enables precise benchmarking and calibration of BLE beacons, using RSSI and trilateration to estimate device positions relative to reference beacons. The project offers an intuitive interface for real-time testing, calibration, and visualization, making it an ideal tool for developers and researchers working on indoor positioning systems.

## Features

- **Serial Communication with BLE Boards**: Communicates directly with your BLE-enabled board over serial. (TCP support coming soon!)
- **RSSI & Trilateration**: Utilizes RSSI (Received Signal Strength Indicator) and trilateration based on Friis equations to estimate the position of devices.
- **Beacon Benchmarking & Calibration**: Helps in benchmarking and calibrating BLE beacons for accurate positioning.
- **Adjustable Path Loss Exponent**: Fine-tune the path loss exponent to match your environment for more accurate results.
- **Real-Time 2D & Experimental 3D Mapper**: Visualize calculated distances and positions in both 2D and experimental 3D views.
- **User-Friendly Interface**: An intuitive UI for easy calibration, visualization, and testing.
- **Future Features**:
    - TCP support for more flexible communication.
    - MinMax and KNN algorithms for improved accuracy and flexibility.

package com.ranshinban.ranshinban.BLE;

public class Beacon
{
    private String deviceName;
    private String macAddress;
    private int rssi = 0;
    private int txPower = 0;
    private boolean refreshed = false;

    private double xCoordinate = 0.0;
    private double yCoordinate = 0.0;
    private double radius = 0.0;
    private int referenceRSSI = 0;


    public Beacon(String name, String macAddress, int rssi, int txPower)
    {
        this.deviceName = name;
        this.macAddress = macAddress;
        this.rssi = rssi;
        this.txPower = txPower;
    }
    public String toString()
    {
        return deviceName == null ? macAddress : deviceName;
    }
    public String getDeviceName()
    {
        return this.deviceName;
    }
    public String getMacAddress()
    {
        return this.macAddress;
    }
    public int getRssi()
    {
        return this.rssi;
    }

    public int getTxPower()
    {
        return this.txPower;
    }
    public void setDeviceName(String name)
    {
        this.deviceName = name;
    }
    public void setMacAddress(String macAddress)
    {
        this.macAddress = macAddress;
    }
    public void setRssi(int rssi)
    {
        this.rssi = rssi;
    }
    public void setTxPower(int txPower)
    {
        this.txPower = txPower;
    }

    public Double getxCoordinate()
    {
        return xCoordinate;
    }

    public void setxCoordinate(Double xCoordinate)
    {
        this.xCoordinate = xCoordinate;
    }

    public Double getyCoordinate()
    {
        return yCoordinate;
    }
    public void setyCoordinate(Double yCoordinate)
    {
        this.yCoordinate = yCoordinate;
    }

    public int getReferenceRSSI()
    {
        return referenceRSSI;
    }

    public void setReferenceRSSI(int referenceRSSI)
    {
        this.referenceRSSI = referenceRSSI;
    }

    public boolean isRefreshed()
    {
        return refreshed;
    }

    public void setRefreshed(boolean refreshed)
    {
        this.refreshed = refreshed;
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius(double radius)
    {
        this.radius = radius;
    }
    @Override
    public boolean equals(Object beacon)
    {
        if(beacon == null) return false;
        if(beacon.getClass() != Beacon.class) return false;
        return this.macAddress.equals(((Beacon) beacon).getMacAddress());
    }
}

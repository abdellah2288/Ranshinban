package com.ranshinban.ranshinban.utils;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;

public class SerialHandler
{
    static final String scanCutOff = "END";
    static private final StringBuilder scannerBuffer = new StringBuilder();
    static private final StringBuilder debugBuffer = new StringBuilder();

    static private String scanBuffer = null;
    static private SerialPort currentPort = null;

    static public boolean openPort(SerialPort serialPort,int baudRate)
    {
        if(currentPort != null) closePort();
        if(serialPort == null) return false;

        currentPort = serialPort;
        currentPort.openPort();
        currentPort.setBaudRate(baudRate);
        currentPort.addDataListener(
                new SerialPortDataListener()
                {
                    public int getListeningEvents()
                    {
                        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                    }
                    public void serialEvent(SerialPortEvent event)
                    {
                        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                        byte[] recievedData = new byte[serialPort.bytesAvailable()];

                        serialPort.readBytes(recievedData, recievedData.length);
                        scannerBuffer.append(new String(recievedData, StandardCharsets.UTF_8));
                        debugBuffer.append(new String(recievedData, StandardCharsets.UTF_8));
                        if(scannerBuffer.toString().contains(scanCutOff))
                        {
                            scanBuffer = scannerBuffer.toString();
                            scannerBuffer.delete(0, scannerBuffer.length());
                        }
                    }
                }
        );

        return serialPort.openPort();
    }

    static public void closePort()
    {
        if(currentPort != null) currentPort.closePort();
        scannerBuffer.setLength(0);
        scanBuffer = null;
        currentPort = null;
    }

    static public boolean portOpen()
    {
        return currentPort != null && currentPort.openPort();
    }

    static public int getSerialBufferSize()
    {
        return scannerBuffer.length();
    }

    static public String getScannerBuffer()
    {
        return scannerBuffer.toString();
    }

    static public void clearBuffer()
    {
        scannerBuffer.setLength(0);
    }

    static public String getScanBuffer()
    {
        String _scanBuffer = scanBuffer;
        scanBuffer = null;
        return _scanBuffer;
    }

    static public boolean scanAvailable()
    {
        return scanBuffer != null;
    }
    static public void setBaudRate(int baudRate)
    {
        if(currentPort != null) currentPort.setBaudRate(baudRate);
    }
    static public void print(String data)
    {
        if(currentPort != null)
        {
            currentPort.writeBytes(data.getBytes(),data.getBytes().length);
            currentPort.flushIOBuffers();
        }
    }
    static public String getDebugBuffer()
    {
        String contents = debugBuffer.toString();
        debugBuffer.delete(0, debugBuffer.length());
        return contents;
    }
}

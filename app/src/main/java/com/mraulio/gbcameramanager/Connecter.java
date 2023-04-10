package com.mraulio.gbcameramanager;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.util.List;

public class Connecter {
    static UsbDeviceConnection connection;
    static SerialInputOutputManager usbIoManager;

    static UsbManager manager = MainActivity.manager;
    static UsbSerialPort port = null;
    public static  void connect(Context context, TextView tv) {
        manager = (UsbManager) tv.getContext().getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());


        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            if (port.isOpen()) port.close();
            port.open(connection);
            port.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//            tv.append("Puerto abierto y parametros puestos");

        } catch (Exception e) {
            System.out.println(e.toString());
            tv.append(e.toString());
            Toast.makeText(context, "Error en el connect." + e.toString(), Toast.LENGTH_SHORT).show();

        }

        //USE IN ARDUINO MODE ONLY
        usbIoManager = new SerialInputOutputManager(port, usbIoManager.getListener());
//        usbIoManager.start();
//        tv.append("Conectado");
    }
}

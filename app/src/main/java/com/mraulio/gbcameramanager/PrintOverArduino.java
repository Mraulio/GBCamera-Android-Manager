package com.mraulio.gbcameramanager;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class PrintOverArduino {
    private final String INIT = "88 33 01 00 00 00 01 00 00 00";//81 00
    private final String PRINT = "88 33 02 00 04 00 01 03 E4 7D 6B 01 81 00";//E4 standard palette, 7F max print intensity - 81 00
    private final String DATA_COMMAND = "88 33 04 00 80 02";
    private final String EMPTY_DATA = "88 33 04 00 00 00 04 00 00 00";
    private final String END_DATA = "00 00";
    private final String START_CHECKSUM = "04 00 80 02";//First part of the checksum, next will be the data
    byte[] checksum;
    public static double count = 0;
    public static double percentage = 0;
    //DATA at the bottom of the Class
    private final int TIMEOUT_SEND = 0;//0 so there is no TIMEOUT       120 working for INIT, PRINT, EMPTY
    private final int TIMEOUT_READ = 100;

    public void sendImage(UsbSerialPort port, TextView textView) {
        try {
//            String all = INIT+" "+mysteryPacket+" "+mysteryPacket+" "+mysteryPacket+" "+EMPTY_DATA+" "+PRINT;
            String all = INIT + " " + DATA_COMMAND + " " + rawRiopar.replaceAll(System.lineSeparator(), " ") + " " + EMPTY_DATA + " " + PRINT;
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    sendCommand(port, textView, all, TIMEOUT_SEND);
//                }
//            }, 0);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    sendCommand(port, textView, all, TIMEOUT_SEND);
//                }
//            }, 1200);

//            sendCommand(port, textView, all, TIMEOUT_SEND);
//            sendCommand(port, textView, all, TIMEOUT_SEND);

//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    sendCommand(port, textView, INIT, TIMEOUT_SEND);
//                }
//            }, 10);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    sendCommand(port, textView, copiedDataPacket, TIMEOUT_SEND);
//                }
//            }, 10);
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    sendCommand(port, textView, PRINT, TIMEOUT_SEND);
//                }
//            }, 10);
        } catch (Exception e) {
            textView.append(e.toString());
        }
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendCommand(port,textView,copiedDataPacket,TIMEOUT_SEND);
//            }
//        }, 10);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendCommand(port,textView,EMPTY_DATA,TIMEOUT_SEND);
//            }
//        }, 0);
//        sendCommand(port,textView,copiedDataPacket,2000);
//        sendCommand(port,textView,EMPTY_DATA,TIMEOUT_SEND);
//        sendCommand(port,textView,copiedDataPacket,1200);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                sendCommand(port,textView,PRINT,TIMEOUT_SEND);
//            }
//        }, 10);

//        sendData(port, textView);
//        sendDataCommand(port, textView);
//        sendCommand(port,textView,EMPTY_DATA,2000);
//        sendEndDataCommand(port,textView);
//        sendPrintCommand(port, textView);
    }


    public void sendInitCommand(UsbSerialPort port, TextView textView) {
        String[] hexValues = INIT.split(" ");
        byte[] byteArray = new byte[hexValues.length];
        for (int i = 0; i < hexValues.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
//            System.out.println(byteArray[i]);
        }
//        textView.append("\nSending INIT Command\n");
//        for(byte b :byteArray)
//        {
//            textView.append(String.format("%02X ", b));
//        }
        try {
            port.write(byteArray, TIMEOUT_SEND); // Sends all bytes
//            byte[] buffer = new byte[1]; // Buffer to read response
//
//            for (int i = 0; i < byteArray.length+2; i++) {
//                int numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
////                System.out.println("Response received: " + buffer[0]);
//                textView.append("Response received " +i+": "+ String.format("%02X ",buffer[0]) + "\n");
//            }
        } catch (Exception e) {
            System.out.println(e.toString());
            textView.append("Error.\n" + e.toString());
        }
    }

    public byte[] checksumCalc(byte[] tileData) {
        int checksum = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] startBytes = getCommandBytes(START_CHECKSUM);
        try {
            outputStream.write(startBytes);
            outputStream.write(tileData);
        } catch (Exception e) {
        }
        byte[] data = outputStream.toByteArray();
        //Iterate through the byte array, adding each byte to the checksum. Make sure to use the bitwise AND operator to mask off the higher bits of the bytes, since the checksum is only 2 bytes long.
        for (byte b : data) {
            checksum = (checksum + (b & 0xff)) & 0xffff;
        }
        //Convert the checksum to a byte array in little endian order, using bitwise operations to extract the lower and higher bytes.
        byte[] checksumBytes = new byte[2];
        checksumBytes[0] = (byte) (checksum & 0xff);
        checksumBytes[1] = (byte) ((checksum >> 8) & 0xff);

        return checksumBytes;
//        System.out.println(String.format("%02X %02X", checksumBytes[0], checksumBytes[1]));
    }

    private byte[] createData(TextView tv) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(getCommandBytes(INIT));

        } catch (Exception e) {
        }

//        List<byte[]> chunkList = new ArrayList<>();
        String data_nospace = onlyTileData.replaceAll(System.lineSeparator(), " ").replaceAll(" ", "");
        int len2 = data_nospace.length();
        byte[] bytesTileData = new byte[len2 / 2];
        for (int i = 0; i < len2; i += 2) {
            bytesTileData[i / 2] = (byte) ((Character.digit(data_nospace.charAt(i), 16) << 4)
                    + Character.digit(data_nospace.charAt(i + 1), 16));
        }
        int chunkSize = 640;//each data packet size, 640 + 4 from the start checksum
        for (int i = 0; i < bytesTileData.length; i += chunkSize) {
            int chunkLength = Math.min(chunkSize, bytesTileData.length - i);
            byte[] chunk = Arrays.copyOfRange(bytesTileData, i, i + chunkLength);
            try {
                outputStream.write(getCommandBytes(DATA_COMMAND));
                outputStream.write(chunk);//I write the 640 bytes
                outputStream.write(checksumCalc(chunk)); //I write the Checksum, need to calculate it with start_checksum+data
                outputStream.write(getCommandBytes(END_DATA));//The last 2 bytes

            } catch (Exception e) {
                tv.append(e.toString());
            }
//            System.out.println(chunk.length);
            // haz algo con el chunk, como enviarlo a través de la red o guardarlo en un archivo
        }
        try {
            outputStream.write(getCommandBytes(EMPTY_DATA));
            outputStream.write(getCommandBytes(PRINT));
        } catch (Exception e) {
            tv.append("Error EMPTY, PRINT" + e.toString());

        }
        tv.append(""+outputStream.toByteArray().length);

        return outputStream.toByteArray();
    }
    private byte[] createDataWithImageBytes(TextView tv, byte[] imageBytes) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(getCommandBytes(INIT));

        } catch (Exception e) {
        }
        tv.append(""+imageBytes.length);
//        List<byte[]> chunkList = new ArrayList<>();
//        String data_nospace = onlyTileData.replaceAll(System.lineSeparator(), " ").replaceAll(" ", "");
//        int len2 = data_nospace.length();
//        byte[] bytesTileData = new byte[len2 / 2];
//        for (int i = 0; i < len2; i += 2) {
//            bytesTileData[i / 2] = (byte) ((Character.digit(data_nospace.charAt(i), 16) << 4)
//                    + Character.digit(data_nospace.charAt(i + 1), 16));
//        }
        int chunkSize = 640;//each data packet size, 640 + 4 from the start checksum
        for (int i = 0; i < imageBytes.length; i += chunkSize) {
            int chunkLength = Math.min(chunkSize, imageBytes.length - i);
            byte[] chunk = Arrays.copyOfRange(imageBytes, i, i + chunkLength);
            try {
                outputStream.write(getCommandBytes(DATA_COMMAND));
                outputStream.write(chunk);//I write the 640 bytes
                outputStream.write(checksumCalc(chunk)); //I write the Checksum, need to calculate it with start_checksum+data
                outputStream.write(getCommandBytes(END_DATA));//The last 2 bytes

            } catch (Exception e) {
                tv.append(e.toString());
            }
//            System.out.println(chunk.length);
            // haz algo con el chunk, como enviarlo a través de la red o guardarlo en un archivo
        }

        try {
            outputStream.write(getCommandBytes(EMPTY_DATA));
            outputStream.write(getCommandBytes(PRINT));
        } catch (Exception e) {
            tv.append("Error EMPTY, PRINT" + e.toString());

        }
        tv.append(""+outputStream.toByteArray().length);
        return outputStream.toByteArray();
    }

    /**
     *
     */

    private byte[] getCommandBytes(String data2) {
        String data = data2.replaceAll(" ", "");
        int len2 = data.length();
        byte[] bytes = new byte[len2 / 2];
        for (int i = 0; i < len2; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4)
                    + Character.digit(data.charAt(i + 1), 16));
        }
        return bytes;
    }

    public void sendThread(UsbDeviceConnection connection, UsbDevice usbDevice, TextView textView) {
        UsbEndpoint endpoint = null;
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    endpoint = usbEndpoint;
                    break;
                }
            }
            if (endpoint != null) {
                break;
            }
        }
        if (endpoint == null) {
            textView.append("No se pudo encontrar un endpoint válido");
        }
        UsbEndpoint finalEndpoint = endpoint;

//        MainActivity.freeTv = false;

//        /**
//         * Sending this works
//         */
//        String all = INIT + " " + DATA_COMMAND + " " + mysteryPacket.replaceAll(System.lineSeparator(), " ") + " " + EMPTY_DATA + " " + PRINT;
//        String[] hexValues = all.split(" ");
//        byte[] byteArray = new byte[hexValues.length];
////        textView.append("length: "+hexValues.length);
//        for (int i = 0; i < hexValues.length; i++) {
//            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
////            System.out.println(byteArray[i]);
//        }
//        /**
//         *
//         */

//        byte[] byteArray = createData(textView);
        byte[] byteArray = createDataWithImageBytes(textView,Methods.gbcImagesList.get(MainActivity.printIndex).getImageBytes());
        textView.append("byte array length:" + byteArray.length);
        percentage = byteArray.length;

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Enviar datos por USB
                connection.bulkTransfer(finalEndpoint, byteArray, byteArray.length, 0);
                // Esperar 20000ms después de enviar el último byte

//                MainActivity.freeTv = true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count = 0;

                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                connection.bulkTransfer(finalEndpoint, byteArray, byteArray.length, 0);

                // Realizar cualquier otra operación necesaria después de la espera de 1200ms
            }
        });
        sendThread.start();
    }


    public void sendDataCommand(UsbSerialPort port, TextView textView) {
        String[] hexValues = DATA_COMMAND.split(" ");
        byte[] byteArray = new byte[hexValues.length];
        for (int i = 0; i < hexValues.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
//            System.out.println(byteArray[i]);
        }
//        textView.append("\nSending DATA Command\n");
//        for(byte b :byteArray)
//        {
//            textView.append(String.format("%02X ", b));
//        }
        try {
            port.write(byteArray, 100); // Sends all bytes
//            byte[] buffer = new byte[1]; // Buffer to read response
//
//            for (int i = 0; i < byteArray.length; i++) {
//                int numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
////                System.out.println("Byte sent: " + byteArray[i] + ", response received: " + buffer[0]);
//                textView.append("Byte sent: " + byteArray[i] + ", response received " +i+": "+buffer[0]+"\n");
//            }
        } catch (Exception e) {
            System.out.println(e.toString());
            textView.append("Error.\n" + e.toString());
        }
    }
//    public void sendData(UsbSerialPort port, TextView textView) {
//        String[] hexValues = DATA.split(" ");
//        byte[] byteArray = new byte[hexValues.length];
//        for (int i = 0; i < hexValues.length; i++) {
//            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
////            System.out.println(byteArray[i]);
//        }
////        textView.append("\nSending DATA\n"+byteArray.length+"\n");
//        checksum = calculateChecksum(byteArray);
//
////        for(byte b :byteArray)
////        {
////            textView.append(String.format("%02X ", b));
////        }
//        try {
//            port.write(byteArray, 120); // Sends all bytes
////            byte[] buffer = new byte[1]; // Buffer to read response
////            int i = 0;
////            int numBytesRead= 0;
////            for (; i < byteArray.length; i++) {
////                numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
//////                System.out.println("Byte sent: " + byteArray[i] + ", response received: " + buffer[0]);
//////                textView.append("Byte sent: " + byteArray[i] + ", response received: " + buffer[0] + "\n");
////            }
////            textView.append("Bytes sent: " + i + ", response bytes received: " + numBytesRead + "\n");
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//            textView.append("Error.\n" + e.toString());
//        }
//    }


    private void sendCommand(UsbSerialPort port, TextView textView, String data, int timeout) {
        String[] hexValues = data.split(" ");
        byte[] byteArray = new byte[hexValues.length];
//        textView.append("length: "+hexValues.length);
        for (int i = 0; i < hexValues.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
//            System.out.println(byteArray[i]);
        }
//        textView.append("\nSending DATA Command\n");
//        for(byte b :byteArray)
//        {
//            textView.append(String.format("%02X ", b));
//        }
        try {
            port.write(byteArray, timeout); // Sends all bytes
//            byte[] buffer = new byte[1]; // Buffer to read response
//
//            for (int i = 0; i < byteArray.length; i++) {
//                int numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
//                System.out.println("Byte sent: " + byteArray[i] + ", response received: " + buffer[0]);
//                textView.append("Byte sent: " + byteArray[i] + ", response received: " + buffer[0]+"\n");
//            }
        } catch (Exception e) {
            System.out.println(e.toString());
            textView.append("Error.\n" + e.toString());
        }
    }

    public void sendEndDataCommand(UsbSerialPort port, TextView textView) {
//        String[] hexValues = end_data_checksum.split(" ");
//        byte[] byteArray = new byte[hexValues.length];
//        for (int i = 0; i < checksum.length; i++) {
//            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
////            System.out.println(byteArray[i]);
//        }
//        textView.append("\nSending END DATA Command\nChecksum: ");
        for (byte b : checksum) {
            textView.append(String.format("%02X ", b));
        }
        try {
            port.write(checksum, TIMEOUT_SEND); // Sends all bytes
//            byte[] buffer = new byte[1]; // Buffer to read response
//
//            for (int i = 0; i < checksum.length+2; i++) {
//                int numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
////                System.out.println("Byte sent: " + checksum[i] + ", response received: " + buffer[0]);
//                textView.append("Response received: " +i+": "+ String.format("%02X ",buffer[0])+"\n");
//            }
        } catch (Exception e) {
            System.out.println(e.toString());
            textView.append("Error.\n" + e.toString());
        }
    }

    public void sendPrintCommand(UsbSerialPort port, TextView textView) {
        String[] hexValues = PRINT.split(" ");
        byte[] byteArray = new byte[hexValues.length];
        for (int i = 0; i < hexValues.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexValues[i], 16);
            System.out.println(byteArray[i]);
        }
//        textView.append("\nSending PRINT Command\n");
//        for(byte b :byteArray)
//        {
//            textView.append(String.format("%02X ", b));
//        }
        try {
            port.write(byteArray, TIMEOUT_SEND); // Sends all bytes
//            byte[] buffer = new byte[1]; // Buffer to read response
//
//            for (int i = 0; i < byteArray.length+2; i++) {
//                int numBytesRead = port.read(buffer, TIMEOUT_READ); // Reads the response of each byte
////                System.out.println("Response received: " + buffer[0]);
//                textView.append("Response received " +i+": "+ String.format("%02X ",buffer[0]) + "\n");
//            }
        } catch (Exception e) {
            System.out.println(e.toString());
            textView.append("Error.\n" + e.toString());
        }
    }


    String copiedDataPacket = "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 82 00 C5 00 AA 00 75 00 AE 00 DF 00 3A 80 FD 00 A2 00 77 00 FA 00 FD 00 78 80 FC 00 38 00 7C 00 02 01 12 01 2B 00 13 00 03 00 00 01 03 00 01 00 C8 BF 38 DF 48 BF 20 D7 A5 42 DA 05 B4 0B F0 0F 59 A0 0F F0 31 DA 54 FB 45 BA 1C FF A9 FE 53 FD F4 0A E1 1C D2 2C FF 00 AE 30 D0 37 B3 97 D3 17 80 00 C4 00 02 00 02 00 84 02 09 04 0F 00 15 00 C8 00 5D 00 18 00 04 10 20 00 40 00 00 00 10 40 82 00 15 00 2F 00 10 0F 06 02 03 02 01 02 03 00 C8 02 DD 00 2F 00 37 00 25 02 16 01 9B 00 97 48 72 CC 28 D7 9C 63 9C 67 6F 82 F4 03 BF 02 DB 24 EF 24 77 C0 FA A2 2D D2 54 EA BF 64 DF 20 6F 90 EB 00 DF 00 99 00 5D 00 A8 00 55 00 AB 00 FF 00 87 28 FD 00 21 08 DC 01 AB 01 D5 01 A0 01 57 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 A0 00 D5 00 AA 00 D5 00 A0 00 45 00 A2 00 53 00 B8 00 74 00 8A 00 57 00 8A 00 45 00 8A 00 55 00 00 00 45 00 00 00 41 00 82 00 C6 00 2A 00 57 00 A0 0F B0 4F 5A A7 C9 37 56 AB 39 C7 56 AB C0 3F 88 FF 80 FF AF FE 54 FF E5 FA 6C F3 B5 EA 0D F2 34 92 56 B0 52 B8 F7 18 FA 18 F9 10 92 20 FB 00 00 00 01 00 03 00 05 00 08 00 04 00 00 00 10 00 08 00 4C 00 A8 00 21 10 00 00 05 00 A0 00 40 00 02 00 07 00 05 02 03 00 00 00 15 01 01 00 01 00 87 D8 E1 DC 9A E8 A9 5E 70 8F 21 DE 65 9A FF 00 AA 44 8B 54 C2 3C 33 44 E8 20 74 00 22 00 35 00 AA 00 3D 40 8A 00 47 00 2F 01 17 01 00 03 01 03 A8 00 A5 50 DE 20 51 2E B6 09 ED 10 AA 00 D7 00 8A 00 D1 00 A2 00 F7 00 AB 00 FF 00 A3 00 F7 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 58 90 00 00";
    String almostBlackPacket = "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 01 81 00";
    String blackRaphael = "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 06 7E 00 00";
    String mysteryPacket = "C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 18 00 3C 00 3C 00 18 00 01 00 01 00 00 18 00 3C C0 C0 06 00 06 00 C0 00 E0 00 E0 00 C0 00 00 06 00 00 00 00 00 00 00 00 40 00 01 01 03 02 07 04 0E 0F 1F 31 3E 60 7D C0 F9 84 F3 0F EF 1F F8 34 00 00 00 80 80 80 80 80 F0 F0 F8 F8 F8 F8 80 80 00 05 00 10 00 00 00 10 00 00 00 10 00 00 00 10 00 55 00 00 FF FF FE 9C BE DC FE 9C BE DC FE 80 00 55 00 00 C0 C0 C0 C0 CF CF DF D8 DF D3 D7 DB 00 55 00 00 07 07 07 04 E7 E6 F7 76 B3 32 B3 32 00 55 00 00 E7 E7 67 64 67 66 67 66 63 62 63 62 00 55 00 00 E0 E0 60 60 63 63 67 66 67 64 65 66 00 55 00 00 01 01 01 01 F9 F9 FD 1D ED CD ED CD 00 55 00 00 F0 F0 B0 30 B0 30 B0 30 B0 30 B0 30 00 00 01 41 02 02 02 42 02 02 02 42 02 02 02 42 FE FE 83 FF FE 82 82 FE FE FE 82 FE FE FE 82 FE 00 00 00 00 80 80 80 80 E2 80 E0 80 E0 80 E0 80 03 00 00 60 00 60 00 03 00 07 00 07 00 03 60 60 00 18 00 3C 00 3C 00 18 00 80 00 80 18 18 3C 3C C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 C3 D8 00 3C 00 18 00 01 00 01 18 18 3C 3C 3C 3C 18 18 00 06 00 C0 00 E0 00 E0 00 C0 06 06 06 06 C0 C0 07 04 05 07 07 07 43 03 01 03 01 01 03 03 00 00 FD E1 FD F1 CF F0 FD CE FF AF BF C3 7F E0 1F 3F E0 E0 70 98 F8 08 F0 18 E0 F0 80 C5 80 80 00 00 00 00 00 10 00 00 00 50 00 00 00 50 00 00 00 15 BE DC FE 9C BE DC FE 9C FF FF FF FF 00 00 00 55 DF D0 D7 DB DF D3 DF D8 DF DF CF CF 00 00 00 55 B3 32 F3 F2 33 32 73 72 F3 F3 E3 E3 00 00 00 55 63 62 63 62 63 62 63 62 E3 E3 E3 E3 00 00 00 55 67 64 65 66 67 64 67 66 E7 E7 E3 E3 00 00 00 55 ED CD ED CD ED CD FD 1D FD FD F9 F9 00 00 00 55 B0 30 B0 30 F0 F0 B0 30 F0 F0 F0 F0 00 00 00 55 02 02 02 42 02 02 02 42 02 02 02 42 03 03 00 40 FE FE 00 00 00 00 06 C0 06 C0 00 00 FF FF FF 00 E0 80 E0 80 E0 80 E2 80 E0 80 E0 80 E0 80 E0 00 60 60 03 03 07 07 07 07 03 03 60 00 60 00 03 00 3C 3C 18 18 80 80 80 80 18 00 3C 00 3C 00 18 00 C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B C3 1B 69 FE 00 00";
    String copiedDataPacket2 = "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 58 90 00 00";

    String rawFromSerial =
            "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 03 00 00 00 00 00 00 00 00 00 00 00 00 3F 3F C0 FF 00 00 00 00 00 00 00 00 00 00 00 00 FC FC 03 FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 C0 C0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 02 03 04 07 18 1F 20 3F 40 7F 1C 1F 60 7F 80 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 38 F8 06 FE 01 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 00 00 80 80 40 C0 20 E0 18 F8 04 FC 02 FE 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 6D 2E 00 00\n" +
                    "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 01 01 02 03 04 07 0A 0F 08 0F 14 1F 80 FF 00 FF 10 FF 00 FF 00 FF 40 FF 10 FF 80 FF 00 FF 00 FF 00 FE 00 F8 00 E0 00 C0 00 80 00 00 00 FF 00 E0 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 FF 00 FF 00 7F 00 1F 00 07 00 03 00 01 00 00 01 FF 00 FF 08 FF 00 FF 00 FF 02 FF 08 FF 01 FF 00 00 80 80 80 80 40 C0 20 E0 50 F0 10 F0 28 F8 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 10 1F 22 3F 28 3F 28 3F 45 7F 6A 7F 55 7F A6 FF 08 FE 40 FC 82 F8 18 F0 46 F0 88 E0 32 E0 A9 C0 88 00 00 00 22 00 00 00 AA 00 44 00 AA 00 11 00 88 00 00 00 22 00 00 00 AA 00 44 00 AA 00 17 00 88 00 00 00 22 00 00 00 AA 00 44 00 AA 00 F1 00 91 00 00 00 44 00 00 00 D5 00 22 00 D5 00 08 00 10 7F 02 3F 41 1F 18 0F 62 0F 11 07 4C 07 95 03 08 F8 44 FC 14 FC 14 FC A2 FE 56 FE AA FE 65 FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 75 4C 00 00\n" +
                    "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FA FF ED FF BB FF FD FF EF FF FF FF FD FF DF FF 96 C0 69 C0 D6 80 ED 80 D6 80 F9 80 D7 80 FD 80 AA 00 55 00 AA 00 55 00 EB 00 57 00 FB 00 87 11 BF 00 7D 07 EF 1F D5 3F FB 7F 55 FF FC FF D8 FE FE 00 5F E0 FF F8 57 FC FF FE 55 FF 3F FF 1D 3F D5 00 2A 00 D5 00 AA 00 D5 00 EA 00 DA 00 F5 80 69 03 96 03 6B 01 B7 01 6B 01 9F 01 6B 01 BF 01 5F FF B7 FF DD FF BF FF F7 FF FF FF BF FF FB FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF 7F 7F 7F 7F 7F 7F 7F 7F 3F 3F 3F 3F D7 80 FC 81 F3 86 FE 81 FF C0 FD C0 F7 C0 FF C0 07 71 67 91 07 11 87 91 A7 71 FB 10 FB 00 73 00 F1 FC 70 FF F0 FF F0 FF F8 FF 74 FF FF FF FF 7F 0F 1F 07 DF 0F DF 0F FF 1F FF 37 FF FF FF FF FE EE 80 75 80 EF 80 F5 80 EF 80 DF 00 DF 00 CE 00 AB 01 5F 01 AF 01 7F 01 FF 03 BF 03 EF 03 FF 03 FF FF FF FF FE FE FE FE FE FE FE FE FC FC FC FC 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 CC 9F 00 00\n" +
                    "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 3F 3F 3F 1F 1F 1F 1F 0F 0F 0F 0F 07 07 03 3F 03 FF FF E0 FD F0 FF F0 FF F8 FF FC FF FE FF FF FF FF FD 00 DE 00 FF 00 77 00 FF 00 FF 00 FF 00 FF 80 FF 3F FF 1F 7F 07 3F 00 C7 00 F8 00 FF 00 FF 00 FF FC FF F8 FE E0 FD 00 E3 00 1F 00 FF 00 FF 00 BF 00 7B 00 FF 00 6E 00 FF 00 FF 00 FF 00 FF 01 FF 07 BF 0F FF 0F FF 1F FF 3F FF 7F FF FF FF FF FC FC F8 F8 F8 F8 F0 F0 F0 F0 E0 E0 C0 FC C0 FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FC 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 03 04 07 00 03 00 07 00 07 0C 0F 33 3C CC F3 3F CF F5 3F 00 FF 00 FF 00 FF 00 FF FE 01 01 FE FE FD 56 FD 01 FF 02 FF 05 FF 0A FF 15 FF 0A FF 81 7F 80 7F FF FF BF FF 5F FF AA FF 55 FF AA FF 55 FF 2A FF FF C0 FF E0 FF F8 FF FF 5F FF AA FF 55 FF 00 FF FF 00 FF 00 FF 00 FF 00 FF E0 AA FF 54 FF 00 FF FF 00 FF 00 FF 00 FF 00 FF 07 AA FF 15 FF 00 FF FF 03 FF 07 FF 1F FF FF F5 FF AA FF 55 FF 00 FF FF FF FE FF F5 FF AA FF 55 FF AA FF 55 FF A8 FF 80 FF 80 FF 40 FF A0 FF 50 FF A0 FF 01 FE 01 FE 00 FF 00 FF 00 FF 00 FF 7F 80 80 7F 7F BF 6A BF 00 C0 00 E0 00 E0 30 F0 CC 3C 33 CF FC F3 AF FC 00 00 00 00 00 00 00 00 00 00 00 00 C0 C0 20 E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 04 C6 00 00\n" +
                    "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 04 07 03 07 07 07 07 05 07 07 07 07 07 07 07 DA FF FF FF 6D FF FF FF DF FF FF FF FF FF FF FF AE FD FE FD AE FD FE FD 76 FD FE FD FE FD FE FD 40 BF FF 80 40 FF A0 FF 55 FF AA FF 55 FF AA FF 00 FF FF 00 00 FF 00 FF 55 FF AA FF 55 FF AA FF 00 FF C0 3F 3F C0 00 FF 55 FF AA FF 55 FF AA FF 00 FF 00 FF FF 00 00 FF 55 FF AA FF 55 FF AA FF 00 FF 00 FF FF 00 00 FF 55 FF AA FF 55 FF AA FF 00 FF 03 FC FC 03 00 FF 55 FF AA FF 55 FF AA FF 00 FF FF 00 00 FF 00 FF 55 FF AA FF 55 FF AA FF 03 FD FE 01 05 FF 0A FF 55 FF AA FF 55 FF AA FF 75 BF 7F BF 75 BF 7F BF 6E BF 7F BF 7F BF 7F BF 5B FF FF FF B6 FF FF FF FB FF FF FF FF FF FF FF E0 20 E0 C0 E0 E0 E0 E0 A0 E0 E0 E0 E0 E0 E0 E0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 07 07 07 07 07 07 07 0F 0F 0F 0F 0F 0F 0F 0F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FD FE FD FE FD FE FD FE FD FE FD FE FD FE FD 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 55 FF AA FF 7F BF 7F BF 7F BF 7F BF 7F BF 7F BF 7F BF 7F BF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF E0 E0 E0 E0 E0 E0 E0 E0 F0 F0 F0 F0 F0 F0 F0 F0 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 F8 3D 00 00\n" +
                    "88 33 04 00 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 07 07 08 00 00 00 00 00 00 00 00 00 00 00 0F 30 CC C3 30 00 00 00 00 00 00 00 00 00 00 00 FF FF 00 FF 00 0F 0F 0F 0F 0F 0F 0F 0F 0F 0F 00 FF FF 00 FF 00 FF FF FF FF FF FF FF FF FF FF 00 FF FF 00 FF 00 FE FD FE FD FF FE FF FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA 7F FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 55 FF AA FF FF FF 00 FF FF 00 FF 00 55 FF AA FF 54 FF AB FE FF FF 00 FF FF 00 FF 00 7F BF 7F BF FF 7F FF FF FF FF 00 FF FF 00 FF 00 FF FF FF FF FF FF FF FF FF FF 00 FF FF 00 FF 00 F0 F0 F0 F0 F0 F0 F0 F0 F0 F0 00 FF FF 00 FF 00 00 00 00 00 00 00 00 00 00 00 00 FC FF 00 FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 CF C3 30 00 00 00 00 00 00 00 00 00 00 00 00 00 E0 E0 10 0C 30 31 40 67 80 55 80 E3 00 D7 00 E3 00 7F 80 8A 40 10 80 2A 80 15 80 3F 80 3F 80 3F 80 3F 80 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 AA 00 00 00 AA 00 55 00 FF 00 FF 00 FF 00 FF 00 B1 0C 04 02 A8 02 54 02 FC 02 FC 02 FC 02 FC 02 30 0C 9C 02 E6 01 AE 01 FB 00 ED 00 FF 00 FE 01 FA 16 00 00\n" +
                    "88 33 04 00 80 02 00 7F 00 80 7F 80 53 80 67 80 53 80 67 80 53 80 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FE 00 00 FF 00 FF 00 FF 00 FF 00 FF 00 FF 00 00 FF 00 01 FE 01 F8 01 FC 01 F8 01 FC 01 F8 01 67 80 53 80 67 80 53 80 67 80 53 80 67 80 53 80 FF 00 FF 00 FF 00 FF 00 FF 00 FA 05 FC 03 F9 07 FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 AA 55 00 FF FF FF FF 00 FF 00 FF 00 FF 00 FF 00 BF 40 1F E0 BF C0 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 D1 39 00 00\n" +
                    "88 33 04 00 80 02 67 80 53 80 67 80 53 80 67 80 53 80 67 80 53 80 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FC FB FC F1 FF E6 FF F4 DF 8D FE D3 FC EF F0 69 17 3F FF FF FF 80 FF AF 50 FE 00 55 00 EB 00 D5 00 F7 F8 FC FF 3F FF B7 5F BF 00 50 00 E0 00 5F 3F DC 23 B5 40 DB E0 C6 F1 AB 7F 7F 1F 3F 0F F5 00 FB 00 F5 00 F8 00 D5 00 32 C0 F5 C0 FA E0 51 00 A0 00 54 00 80 00 55 00 2B 00 55 00 0F 00 57 00 2B 00 55 00 3F 00 55 00 BF 00 5A 05 4F BF AA 55 C0 3F A0 5F F0 0F D7 00 7F 80 4B F4 F8 FF 82 7D 08 FF 56 FD 09 FE 92 7D F5 0A D5 00 60 80 FE 01 FF 00 DD 00 FF 00 F5 00 FF 00 F5 00 FF 00 77 00 FE 00 55 00 FE 00 55 00 BB 00 55 00 8B 00 7F 00 3F 00 55 00 BF 00 7B 04 BF 00 55 00 FF 00 6D 10 BB 00 75 00 FB 00 76 01 FE 01 BE 41 7D 82 A5 50 5E A0 BD 40 1F E0 2A D5 05 FA 5A F5 E9 FE 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 67 80 53 80 67 80 53 80 67 80 53 80 67 80 53 80 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF F7 FF FF E3 CB F4 E7 F8 CB F4 C7 F8 4A F5 87 F8 6D D0 9E E0 55 00 E3 00 55 00 E2 00 F5 00 FB 00 55 00 BF 00 70 00 20 00 00 00 00 00 47 00 A4 03 F9 07 A4 7B 1F 07 03 0F 1F 0F 2F 1F 5F 3F FF FF FF FF FF FF F5 F0 EE F0 F9 F4 F6 F8 F3 FC F9 FE FE FD FE FF 16 01 3F 03 5F 07 3F 0F 6E 1D AF 18 8E 71 9F E0 77 FF ED F2 E5 D0 78 80 F5 00 F8 00 95 40 7E 80 7F FF FF 03 54 01 03 00 55 00 23 00 51 00 80 00 C0 F0 F4 F8 FF FC 3F FF 9F 7F CF 3F EF 1F CF 3F 55 00 3B 00 55 00 7F 80 9F F0 FF F8 F7 FC FD FE 55 00 AB 00 56 01 FF 00 7E 01 FD 02 AA 55 C1 3E 6E 11 FD 02 A9 54 5F A0 B6 41 7D 83 A7 5F 2F FF AE 51 FB 00 5D 00 43 BC 5E FF FF FF FF FF 88 FF F5 7F CF 3F 07 01 00 00 40 00 B0 C0 FC F0 FD FE 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 24 62 00 00\n" +
                    "88 33 04 00 80 02 67 80 53 80 67 80 53 80 67 80 53 80 67 80 53 80 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FF FF FE F8 F4 C0 C0 00 00 00 00 00 00 00 00 00 D7 03 03 03 07 03 06 03 00 07 0A 07 0C 07 0D 0E 35 C0 7B 80 D5 00 FF 00 AA 55 FF 00 D4 00 EB 00 57 00 CB 00 D7 00 8F 00 41 07 0E 03 59 07 B4 0B F5 7F FF FF 7D FF 7F FF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF C6 F1 DF E0 ED D0 DF E0 CB F4 E7 F8 D2 FD E1 FE D5 00 BA 00 DD 00 FE 00 6D 10 F8 00 94 40 FE 00 51 00 02 00 45 00 03 00 54 03 2F 03 59 07 F4 0B 47 3F CB 3F 67 1F E2 1F A1 5F 03 FF D5 FF EE FF FC FF FE FF FF FF FF FF FF FF BF FF FF FF FF FF 96 7D 20 FF 15 FF AF FF D7 FF FF FF D1 FF E2 FF 74 FF EF F0 F6 C1 AF C0 36 C1 9F E0 35 C0 7B 80 AB 54 CE 30 8D 70 18 E0 A5 50 F8 00 D0 00 E0 00 9F 7F BB 07 13 07 0C 03 16 01 14 0B 14 0F 30 0F 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 67 80 53 80 67 80 53 80 67 80 53 80 67 80 53 80 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 FD 03 F9 07 00 00 00 00 40 00 80 00 55 00 FB 00 95 40 7E 80 16 0D 39 0E 72 1D F9 3F 3F 7F FF 3F 3D 7F F8 3F D6 01 BE 03 EA 15 92 ED 25 DF 3F FF 5F FF FF FF 8E 71 9F E0 AB 54 51 AE 2A D5 00 FF 5F FF FF FF 7F FF F7 0F 54 01 FA 00 A5 50 1F E0 CA F5 E0 FF FF FF FF FF 7F FF DF 3F 67 1F FF 03 D6 01 FF 00 FF FF FF FF FF FF FF FF FA FD F7 F8 FD F0 77 F8 CB F4 CF F0 CD F0 C7 F8 6B D4 1F E0 AB 54 7F 80 55 00 A0 00 54 00 88 00 05 00 03 00 56 01 FF 0F 11 7F E9 3F 60 1F C4 3B 6A 15 CA 3F 9E 7D 1F E0 DD FF FF FF FF FF FF FF FF FF FF FF 9F 7F ED 03 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF F5 FF FA FF FC FF FC FF FC FF F8 FF F6 FD F9 FE B5 40 3B C0 37 C0 7F 80 F5 00 FA 00 54 00 EC 00 40 00 80 00 41 00 8B 00 16 01 0C 03 19 07 0A 07 68 17 3C 03 7E 01 DC 23 48 F7 BA FF FD FF FF FF 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 9F E0 BF C0 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 FC 01 F8 01 7E 6D 0 00";

    String otherRaw = "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FF FE FE FE FE FE FE FE FE FE FE FF FF FF FF 1C FF 3E 1C 0C 1C 04 0C 0C 04 24 04 FF FF FF FF 63 FF 07 63 43 23 63 3F 02 62 02 62 FF FF FF FF FF FF FF FF FF FF F7 FE 03 01 11 00 FF FF FF FF FF FF 8F FF 47 8F 05 03 00 8E C8 8C FF FF FF FF FF FF FF FF FF FF 7F 9F 0E 04 E0 46 FF FF FF FF FF FF FF FF FF FF E7 FF 41 03 21 01 FF FF FF FF F8 FF F0 F8 F0 F8 E0 F8 C0 80 18 00 FF FF FF FF FF FF 7F FF 7E FF 6D F3 61 C0 44 8C FF FF FF FF BF FF BF 1F 5F 0F 0F 1F 9F FF 3F 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FE FE FE FE FE FE FE FE FE FF FF FF FF FF FF 24 10 30 10 30 18 3C 18 18 3C FF FF FF FF FF FF 02 62 02 62 02 62 42 22 02 62 FF FF FF FF FF FF 19 30 19 30 19 30 19 30 11 38 FF FF FF FF FF FF 4C 88 4C 88 4C 88 48 8C CC 8E FF FF FF FF FF FF 06 00 FA 04 7A E4 A2 44 06 0C FF FF FF FF FF FF 20 31 21 30 20 31 21 31 61 31 FF FF FF FF FF FF 00 18 00 18 00 18 98 00 C0 80 FF FF FF FF FF FF 04 8C 84 0C 04 8C 48 84 61 C0 FF FF FF FF FF FF 7F 3F 7F 3F 7F 3F 3F 7F 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF C6 0C 00 00\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 04 FB AA 55 05 FA 8A 75 C0 3F A8 57 40 BF AA 55 00 FF AA 55 17 E8 AA 55 14 EB AA 55 25 DA 98 77 00 FF A2 5D 12 ED AA 55 44 BB AA 55 13 EC AA 55 40 BF AA 55 40 BF AA 55 04 FB AA 55 55 AA AA 55 00 FF AA 55 00 FF AA 55 04 FB AA 55 13 EC A8 57 01 FE AF 50 57 A8 AA 55 DC 23 AA 55 D7 28 E8 17 FD 02 DE 01 FF 00 66 11 FC 03 DA 05 75 8A 81 7F 40 BF 80 7F 00 FF 80 7F 40 BF A0 5F F0 0F 01 FF 00 FF 00 FF 00 FF D8 77 00 FF 02 FD 00 FF 57 FF 02 FF 1D F7 00 FF 08 F7 04 FB 80 7F 00 FF D7 FF BF FF 57 FF 8E FF 01 FF 00 FF 00 FF 00 FF F7 FF EA FF 55 FF BA FF 55 FF 2B FF 81 7F 00 FF 4D F0 F8 FF 5D FF FE FF 55 FF FB FF 55 FF 0E FF 55 00 FB 00 6D D0 EB FC DD FF 3F FF 55 FF AA FF 55 00 A2 00 55 00 EA 00 35 C0 A6 F8 D4 FF CF FF 55 00 A2 00 50 00 80 00 50 00 00 00 D0 00 90 E0 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 D4 2B AA 55 D1 2E AA 55 54 AB AA 55 58 A7 AA 55 0C F3 AA 55 45 BA AA 55 C4 3B AA 55 51 AE AA 55 48 B7 AA 55 75 8A AA 55 CC 33 AA 55 12 ED AA 55 C5 3A AA 55 55 AA AA 55 CD 32 AA 55 5D A2 AA 55 CC 33 AA 55 55 AA AA 55 CD 32 AA 55 77 88 AA 55 DD 22 AA 55 F7 08 AB 54 FF 00 B5 40 FF 00 AA 55 7F 80 BA 45 7F 80 6A 15 FF 00 FA 05 FF 00 A8 57 D0 2F AA 55 FD 02 EA 15 7D 82 AA 55 D7 28 28 D7 14 EB 2A D5 71 8E AA 55 DD 22 AA 55 DF 20 89 77 06 FB 8A 75 04 FB A8 57 7C 83 BA 45 7F 80 81 7F 04 FB A0 5F 40 BF A0 5F 55 AA AA 55 75 8A 88 77 01 FE 0A F5 11 EE 8A 75 DF 20 AB 54 D7 28 A5 5F 54 AB AA 55 7D 82 AA 55 DF 20 AA 55 D7 28 55 FF 3A FF 85 7F C0 3F EA 15 55 AA AA 55 77 88 55 FF A3 FF 54 FF EC FF 95 7F C0 3F A8 57 7D 82 70 FC 6E FE 53 FF 80 FF 55 FF AA FF 55 FF 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 06 8E 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 50 AF AA 55 40 BF AA 55 54 AB AA 55 55 AA AA 55 4D B2 AA 55 51 AE AA 55 5D A2 AA 55 DA 25 AA 55 CD 32 AA 55 D8 27 AA 55 5C A3 AA 55 3D C2 AA 55 4F B0 AA 55 55 AA AA 55 5D A2 AA 55 75 8A AA 55 CD 32 AA 55 7F 80 AB 54 CF 30 AB 54 CF 30 AF 50 FF 00 AA 55 FD 02 77 00 FF 00 55 00 FF 00 FA 05 DD 22 AA 55 F3 0C FA 05 FF 00 DA 05 F7 08 AA 55 DD 22 AA 55 7F 80 AA 55 DD 22 AA 55 5F A0 AA 55 DD 22 AA 55 7D 82 AA 55 FF 00 EA 15 FF 00 AA 55 DD 22 AB 54 77 88 EA 15 FD 02 AA 55 77 88 AE 51 DF 20 AA 55 FD 02 AA 55 FD 02 AE 51 FF 00 AA 55 DD 22 AA 55 75 8A AA 55 5D A2 AB 54 DD 22 AA 55 5F A0 AA 55 7F 80 AA 55 FD 02 AA 55 5F A0 AA 55 55 AA AA 55 5F A0 AA 55 DF 20 AA 55 7F 80 AA 55 7D 82 AA 55 57 A8 AA 55 5D A2 AA 55 75 8A E9 17 FC 03 AB 54 D7 28 AA 55 DD 22 AA 55 75 8A FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 44 BB AA 55 51 AE AA 55 45 BA AA 55 53 AC AA 55 4F B0 AB 54 07 F8 AB 54 51 AE AA 55 50 AF AE 51 FF 00 FC 01 DF 20 AE 51 DC 23 21 DF 30 CF AA 55 DD 22 AA 55 01 FE D4 7F 13 EF B9 47 FC 03 AF 50 FF 00 AD 50 7F 80 B7 40 7F 80 97 40 7F 80 77 00 FF 00 FD 00 FF 00 77 00 FF 00 55 00 FF 00 7A 05 FD 02 EA 15 FD 02 6A 15 FD 02 6A 15 DF 20 AA 55 DF 20 AA 55 7F 80 EE 11 FF 00 EA 15 FF 00 AA 55 5D A2 AA 55 77 88 AA 55 FD 02 AB 54 FF 00 AA 55 F5 0A AA 55 75 8A AA 55 FD 02 EA 15 DF 20 AA 55 55 AA AA 55 FF 00 AA 55 75 8A AE 51 5F A0 AA 55 5D A2 AA 55 FF 00 AA 55 FD 02 AA 55 F7 08 EA 15 DD 22 AA 55 D5 2A EA 15 DD 22 EB 14 D7 28 AE 51 5F A0 AA 55 7F 80 AA 55 FF 00 AA 55 7F 80 AA 55 5D A2 AA 55 5D A2 AA 55 FC 03 AA 55 F7 08 AA 55 DD 22 AA 55 7D 82 AA 55 DD 22 AA 55 DF 20 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 9E 7A 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 54 AB AA 55 50 AF 8A 75 05 FA AA 55 7F 80 A9 57 7D 82 BA 45 75 8A EA 15 DC 23 AA 55 D4 2B BA 47 F0 0F E8 17 E1 1E EB 15 02 FF A5 5F 0F FF 72 05 64 9B 2A D5 00 FF 5D FF 83 FF 4E F1 C7 F8 77 00 FF 00 AD 50 07 F8 50 FF F4 FF 55 FF 7F FF 75 00 FF 00 A9 54 21 FE E6 5D 78 87 19 C7 3C C3 6A 15 D7 28 AA 55 FF 00 EA 15 FD 02 EA 15 77 88 AB 54 DD 22 AA 55 FF 00 AA 55 DD 22 BE 41 FF 00 AE 51 DD 22 AA 55 FF 00 AA 55 FD 02 AA 55 FD 02 AA 55 FD 02 AA 55 77 88 EE 11 DD 22 AA 55 7F 80 AA 55 FF 00 AA 55 75 8A AA 55 DD 22 AA 55 75 8A AE 51 DD 22 BA 45 FF 00 EA 15 FD 02 AA 55 FF 00 EE 11 DD 22 FA 05 7D 82 EA 15 5D A2 BB 44 7F 80 AA 55 77 88 AA 55 7F 80 AA 55 F5 0A AA 55 F7 08 EA 15 DF 20 AA 55 5F A0 AE 51 DD 22 AA 55 7D 82 AA 55 7D 82 AA 55 5F A0 AA 55 D5 2A AA 55 D7 28 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF F6 01 FF 00 AA 55 FF 00 A6 51 FF 00 EA 15 7F 80 EA 15 C8 37 AA 55 E4 1B A3 5D C0 3F A2 5D C0 3F 9D 7F 3C FF 54 FF F8 FF 52 FD 73 FC 57 FC 59 FE 57 FC F9 FE 53 FC F3 FC DA F5 8E 71 DB 05 F1 0F D5 7F BB 7F D5 7F 7F FF 57 FF 7F FF 55 FF FF FF 32 C5 DC E3 40 FF 81 FE 6A D5 1F E0 6B D4 4F F0 AA 55 7D 82 AA 55 FF 00 AA 55 DD 22 AA 55 F5 0A AE 51 DF 20 AA 55 7D 82 FA 05 FD 02 AA 55 FF 00 EA 15 DD 22 EA 15 D7 28 AA 55 FD 02 AA 55 FD 02 EE 11 DD 22 BA 45 FD 02 AA 55 FD 02 AA 55 FD 02 AA 55 FF 00 EA 15 FF 00 EA 15 F5 0A AA 55 42 BF AE 51 DF 20 AA 55 F7 08 AE 51 7F 80 AA 55 1D E2 AA 55 DF 20 AA 55 75 8A AE 51 DF 20 AA 55 FF 00 AA 55 D7 28 AA 55 F5 0A AA 55 FD 02 AA 55 75 8A AA 55 FD 02 AA 55 FD 02 AA 55 D4 2B AA 55 55 AA AA 55 D5 2A AA 55 75 8A EA 15 DD 22 AA 55 75 8A FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 47 8D 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AE 51 0F F0 AA 55 41 BE AA 55 C0 3F AA 55 74 8B AA 55 C8 37 AA 55 E1 1E AA 55 40 BF AA 55 11 EE F5 7F 1F FF 35 DF 02 FF AA 55 04 FB AA 55 04 FB A5 5F BB FF 74 FF 80 FF 8A 75 44 BB A8 57 04 FB 75 FF 3E FF A5 5F 40 BF AC 57 20 FF 54 FF DC FF 4A F5 0D F2 CB 74 17 E8 AA 55 C5 3A EA 15 4F B0 AA 55 FF 00 BA 45 FF 00 AA 55 DD 22 AA 55 FF 00 EA 15 DF 20 AA 55 7F 80 EA 15 E5 1A AA 55 F7 08 AE 51 FF 00 AA 55 FF 00 FA 05 FF 00 AB 54 FF 00 EA 15 FD 02 BA 45 FC 03 EA 15 DD 22 BA 45 FF 00 91 7F 1F E0 35 C0 3F C0 37 C0 1F E0 A8 57 42 BF 6E D1 0F F0 8A 75 87 78 8E 71 45 BA 02 FD 82 FF AA 55 5C A3 AB 54 5D A2 AA 55 DC 23 A8 57 10 EF AA 55 DD 22 AA 55 5F A0 4A F5 45 FA 16 FD F0 0F AA 55 55 AA AA 55 D5 2A AA 55 C5 3A AA 55 55 AA AA 55 DD 22 AA 55 57 A8 AA 55 55 AA AA 55 D5 2A FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF A9 57 40 BF AD 57 10 EF BE 55 01 FE AA 55 94 6B 0A F5 04 FB 02 FD 40 BF B8 57 00 FF 82 7D E0 1F AA 55 44 BB 2A D5 01 FE AA 55 00 FF AA 55 40 BF A9 57 00 FF AA 55 10 EF AA 55 44 BB AA 55 0F F0 55 FF B1 FF 55 FF 00 FF 8A 75 CC 33 AA 55 78 87 0A F5 9F E0 2A D5 17 E8 AB 54 0F F0 AA 55 00 FF A2 55 FF 00 AA 55 FF 00 EE 11 FF 00 BE 41 5D A2 EB 14 DD 22 AA 55 58 A7 AA 55 55 AA AA 55 00 FF FE 01 DD 22 AB 54 57 A8 AA 55 4F B0 AA 55 57 A8 EA 15 FC 03 AA 55 7C 83 EB 17 52 AF A4 5F 41 BE 91 7F 0E F1 22 DD 0B FC 96 7D 09 FE 93 7C 17 F8 75 FF 7A FF DD 7F 2F FF D5 7F 2B FF 5D FF 6F FF 5A F5 E0 FF 54 FF A8 FF 75 FF B2 FF CE F1 97 E8 79 07 FC 03 98 47 7C 83 28 D7 02 FF 42 FD 05 FA AA 55 15 EA 2A D5 5D A2 AA 55 7C 83 AA 55 55 AA AA 55 55 AA AA 55 77 88 AA 55 5D A2 AA 55 71 8E FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF A1 8E 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 D4 2B AA 55 80 7F 99 77 00 FF 00 FF 00 FF AA 55 40 BF 28 D7 00 FF 88 77 00 FF 88 77 00 FF EA 55 00 FF 6A D5 01 FE 9A 75 00 FF 6A D5 04 FB AB 54 4F B0 AB 54 05 FA AA 55 10 EF BA 45 78 87 AA 55 DF 20 AA 55 7D 82 AA 55 7D 82 9E 41 7F 80 AA 55 55 AA AA 55 FD 02 BA 45 FC 03 DE 01 FD 02 AA 55 DC 23 AA 55 19 E6 AA 55 54 AB AA 55 50 AF AA 55 05 FA A2 5D 41 BE AA 55 C4 3B AA 55 6F 90 AA 55 4D B2 AA 55 17 E8 AA 55 05 FA AA 55 17 E8 A6 5D C9 3E 9F 7C 89 7E 94 7F 4E BF A5 5F 02 FF BE 71 DC 23 59 07 FB 07 E5 1F 02 FF 50 FF A0 FF 5F FF FB FF D5 FF FE FF 55 FF 03 FF 09 F7 80 FF A4 DF 1A EF 24 DF DA AF 27 DF B2 CF 3C C7 9D E2 8A 75 C5 3A 92 7D 01 FE 82 7D D9 3E 92 7D C9 3E AA 55 5D A2 AA 55 95 6A AA 55 DC 23 AA 55 55 AA AA 55 55 AA AA 55 55 AA AA 55 D4 2B AA 55 01 FE FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 88 77 00 FF A2 5D 00 FF AA 55 44 BB AA 55 10 EF 8A 75 00 FF A2 5D 00 FF AA 55 10 EF AA 55 07 F8 0A F5 0C F3 AA 55 00 FF A9 57 46 BF BD 5F 5F BF AA 55 01 FE EB 54 01 FE 3A D5 89 F6 4A F5 84 FB 77 00 FF 00 5B 04 F9 06 6A 15 FD 02 AA 55 31 CE 7A 05 FD 02 5A 05 F1 0E EA 15 04 FB AA 55 5D A2 AA 55 C4 3B AA 55 05 FA AA 55 54 AB AA 55 55 AA AA 55 54 AB AA 55 50 AF A0 5F 00 FF AA 55 40 BF AA 55 54 AB AA 55 17 E8 AA 55 04 FB AA 55 14 EB A8 57 44 BB 2A D5 14 EB AA 55 04 FB 4A F5 8F F0 89 77 53 AF A9 57 53 AF A9 57 54 AB AA 55 15 EA D8 F7 B8 FF D2 FD F9 FE 76 FD F0 FF 02 FD 01 FE 47 F0 2F F0 32 FD 0A FF 9F 7F 4B BF A5 5F 10 EF 16 7D 30 FF 52 FD E8 FF 72 FD A4 FB EA D5 15 EA AA 55 E0 1F AA 55 51 AE AA 55 44 BB AA 55 50 AF AA 55 45 BA A2 5D 01 FE 80 7F 00 FF 2A D5 15 EA FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 0F 8F 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 0B F5 84 FB D5 FF FF FF 7D FF 3F FF 55 FF 01 FF 8A 75 01 FE 2A D5 41 FE 58 F7 C8 FF D4 FF FE FF 9F 7F FF 3F 97 7F BE 7F 97 7F 5F BF A0 5F 80 FF 4A F5 CC F3 CA F5 DD E2 EA D5 0D F2 AA 55 17 E8 AA 55 D4 2B AA 55 FF 00 AA 55 DC 23 AB 54 5F A0 AA 55 F5 0A AA 55 7D 82 AA 55 FF 00 AA 55 7F 80 AA 55 57 A8 AA 55 75 8A AA 55 FF 00 AA 55 FD 02 A8 57 FD 02 AA 55 45 BA A2 5D 81 7E AA 55 51 AE A9 57 54 AB AB 55 F4 0B 88 77 C0 3F A8 57 40 BF 5A F5 E0 FF 56 FD B8 FF 90 7F 00 FF 2A D5 30 CF AA 55 40 BF AA 55 50 AF A8 57 54 AB 2A D5 14 EB AA 55 54 AB 2A D5 14 EB AA 55 44 BB AA 55 35 CA A8 57 44 BB AA 55 54 AB A8 57 40 BF A2 5D 51 AE AA 55 04 FB AA 55 00 FF AA 55 04 FB 8A 75 61 9E AA 55 04 FB AA 55 00 FF 91 7F 04 FB AA 55 51 AE 0A F5 04 FB 0A F5 00 FF 0A F5 00 FF AA 55 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 9D 77 00 FF 45 FF 00 FF 98 77 80 7F AA 55 D1 2E 55 FF 37 FF 55 FF 00 FF 91 7F 00 FF A2 5D 00 FF 52 FD F8 FF 55 FF FF FF D5 7F 03 FF 45 FF 00 FF AA 55 05 FA 4A F5 88 FF 50 FF BD FF 5D FF 2B FF AA 55 5F A0 AA 55 15 EA AA 55 00 FF 58 F7 48 FF AA 55 FF 00 AA 55 55 AA AA 55 4D B2 AA 55 10 EF AE 51 FD 02 AA 55 75 8A AA 55 D5 2A AA 55 5D A2 AA 55 80 7F A8 57 F1 0E AA 55 D5 2A AA 55 F7 08 A0 5F 00 FF AA 55 00 FF A8 57 00 FF 85 7F 9F 7F AA 55 00 FF 2A D5 01 FE 28 D7 01 FE 0A F5 81 FE A8 57 54 AB AA 55 54 AB A0 5F 40 BF A0 5F 00 FF E8 17 50 AF BA 45 FD 02 A8 57 44 BB 08 F7 00 FF A2 5D 40 BF B2 5D 00 FF 88 77 00 FF 20 DF 04 FB AA 55 1C E3 AA 55 11 EE AA 55 44 BB 2A D5 01 FE AA 55 54 AB 2A D5 10 EF AA 55 50 AF AA 55 50 AF AA 55 C1 3E AA 55 1D E2 AC 53 14 EB 8A 75 01 FE FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF DD 94 00 0C\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 2A 55 D4 2B 7A 05 FF 00 76 01 3F 00 55 00 2F 00 AA 55 54 AB AA 55 FF 00 EA 15 FF 00 FF 00 FF 00 81 7F 00 FF A2 5D C4 3B AA 55 F8 07 AA 55 F7 08 D5 7F 00 FF 10 FF 00 FF 98 77 00 FF AA 55 00 FF D7 FF 3F FF 55 FF 0A FF 15 FF 00 FF 87 7D 00 FF 4A F5 90 FF D5 FF FE FF 55 FF 13 FF 45 FF 02 FF AA 55 45 BA 2A D5 80 FF 50 FF F9 FF 75 FF BF FF AB 54 DF 20 A9 54 52 AC AB 54 01 FE 56 FD E8 FF 97 7F BF 7F B5 7F BB 7F 75 7F FB 3F DF 3F 70 8F 4A F5 E1 FE CA F5 C5 FA CA F5 CC F3 EA D5 14 EB 88 77 00 FF 86 7D 00 FF 85 7F 00 FF 20 DF 00 FF 80 7F 00 FF 20 DF 00 FF 10 FF 00 FF 80 7F 00 FF 02 FD 01 FE 02 FD 01 FE 2A D5 01 FE 02 FD 05 FA AA 55 44 BB AA 55 50 AF AA 55 41 BE AA 55 14 EB 8A 75 45 BA AA 55 15 EA AA 55 44 BB 2A D5 10 EF 2A D5 20 FF 92 7D 01 FE 9E 75 40 BF 02 FD 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 55 00 03 00 55 00 02 00 15 00 02 00 05 00 00 00 77 00 FF 00 55 00 FF 00 57 00 3F 00 55 00 2F 00 6E 11 FF 00 DF 00 FF 00 77 00 FF 00 55 00 FF 00 AA 55 D4 2B AA 55 FE 01 62 15 FF 00 FE 01 FF 00 A8 57 40 BF AA 55 72 8D AA 55 CD 32 AA 55 FF 00 85 7F 00 FF A3 5D 40 BF AA 55 F0 0F AA 55 F3 0C 57 FF 3B FF 15 FF 02 FF B5 5F 02 FF 88 77 41 BE 7F FF FF FF 5F FF EE FF 57 FF 2B FF C5 7F 00 FF 09 F7 80 FF 54 FF ED FF 7D FF FF FF 55 FF 8F FF 2A D5 44 BB 0A F5 00 FF 74 FF A2 FF F5 FF FF FF 09 F7 04 FB 4A F5 04 FB 4A F5 80 FF 50 FF E8 FF 09 F7 00 FF 02 FD 01 FE 82 7D 00 FF 22 DD 01 FE 0A F5 44 BB AA 55 54 AB AA 55 D4 2B AA 55 55 AA AA 55 44 BB AA 55 51 AE AA 55 44 BB AA 55 51 AE A8 57 C0 3F A8 57 10 EF AA 55 10 EF A2 5D 00 FF AA 55 40 BF 08 F7 01 FE A8 57 00 FF 22 DD 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 46 8B 00 0C\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FF FC FE FC F8 F9 F1 E3 F3 FF FF FF FF FF FF 77 8F 0E 07 87 7E FC FE FE FC FF FF FF FF FF FF BF 7E 3C 3E 1E 3C 18 3C 18 3C FF FF FF FF FF FF 77 FB F1 73 61 73 21 63 20 43 FF FF FF FF FF FF 7B 87 81 03 83 3F 3F 3F 37 0F FF FF FF FF FF FF BB C7 C3 83 D9 83 C1 9B 82 83 FF FF FF FF FF FF DE E3 C3 80 25 98 19 3C 78 3C FF FF FF FF FF FF BB 7D 71 39 03 33 23 87 CF 87 FF FF AF D5 FB D5 FB D5 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF E3 F3 F7 E3 E3 F3 F0 F3 F0 F8 FA FC FF FF FF FF F9 FC 9C 08 88 18 5B 91 03 13 07 33 FF FF FF FF 3D 98 3A 19 93 19 D1 93 C3 93 A7 D3 FF FF FF FF 48 03 18 0B 80 1B B3 18 33 B8 B1 FA FF FF FF FF 0F 07 37 0F 3F 7F 03 7F 01 03 07 03 FF FF FF FF 83 82 38 91 3C 99 A8 11 01 03 8B 07 FF FF FF FF 79 3C 7D 38 78 39 4B 31 81 03 CB 87 FF FF FF FF 8F CF 8F DF CF 9F CF 9F CF 9F FF 9F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 47 24 00 0C";

    String rawRiopar = "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FF FE FE FE FE FE FE FE FE FE FE FF FF FF FF 1C FF 3E 1C 0C 1C 04 0C 0C 04 24 04 FF FF FF FF 63 FF 07 63 43 23 63 3F 02 62 02 62 FF FF FF FF FF FF FF FF FF FF F7 FE 03 01 11 00 FF FF FF FF FF FF 8F FF 47 8F 05 03 00 8E C8 8C FF FF FF FF FF FF FF FF FF FF 7F 9F 0E 04 E0 46 FF FF FF FF FF FF FF FF FF FF E7 FF 41 03 21 01 FF FF FF FF F8 FF F0 F8 F0 F8 E0 F8 C0 80 18 00 FF FF FF FF FF FF 7F FF 7E FF 6D F3 61 C0 44 8C FF FF FF FF BF FF BF 1F 5F 0F 0F 1F 9F FF 3F 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FE FE FE FE FE FE FE FE FE FF FF FF FF FF FF 24 10 30 10 30 18 3C 18 18 3C FF FF FF FF FF FF 02 62 02 62 02 62 42 22 02 62 FF FF FF FF FF FF 19 30 19 30 19 30 19 30 11 38 FF FF FF FF FF FF 4C 88 4C 88 4C 88 48 8C CC 8E FF FF FF FF FF FF 06 00 FA 04 7A E4 A2 44 06 0C FF FF FF FF FF FF 20 31 21 30 20 31 21 31 61 31 FF FF FF FF FF FF 00 18 00 18 00 18 98 00 C0 80 FF FF FF FF FF FF 04 8C 84 0C 04 8C 48 84 61 C0 FF FF FF FF FF FF 7F 3F 7F 3F 7F 3F 3F 7F 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF C6 0C 00 00\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 5B 04 F7 08 57 00 FD 02 5A 05 FF 00 BF 40 3F C0 6A 15 FF 00 9A 45 B5 4A 4E 31 ED 12 AE 51 7F 80 AA 55 E6 19 BB 54 43 BC 8A 75 E5 3A A8 57 80 7F AE 51 CC 33 A8 57 53 AC AA 55 40 BF AA 55 41 BE 8A 75 20 FF 8E 75 43 BC 80 7F 00 FF 93 7D 01 FF 65 00 C2 20 F5 00 C2 00 D1 20 50 80 35 C0 9F E0 10 00 03 00 45 00 02 00 50 00 38 00 5D 00 7C 00 55 00 BA 00 55 00 AE 00 3D 00 0E 00 45 00 2F 00 5A 05 ED 02 5D 02 AD 02 15 02 AB 00 15 00 8A 00 75 F8 28 F4 01 FC 7E E0 25 D0 BF 00 DF 00 BF 00 75 00 E2 00 55 00 9F 00 57 00 BF 00 5E 01 F7 08 57 00 BF 00 57 00 FF 00 66 11 FC 03 BE 41 76 89 BE 41 FF 00 BE 41 7F 80 AF 50 FF 00 2B D4 47 B8 B7 40 FF 00 D5 00 FF 00 F4 01 7F 80 AB 54 5D A2 6A 15 FC 03 BA 45 D1 2E AA 55 C1 3E AA 55 10 EF A8 57 45 BA A2 5D 77 88 B7 40 5F A0 AF 50 41 BE FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF B2 45 B7 48 8A 55 34 CB BE 41 F7 08 BE 41 35 CA 66 11 BF 00 97 40 7F 80 EE 11 FF 00 AD 50 67 98 88 77 40 BF A6 5D E6 1B 89 77 A0 7F D0 7F 00 FF 98 77 00 FF 82 7D 08 FF 90 7F 00 FF 16 FD 03 FC B1 5E 42 BF 93 7D 89 7E 96 79 13 FC 92 7D 4F B8 EA 15 D1 22 E4 53 4C B3 8A 75 0C F3 AE 51 CB 30 C5 30 FB 00 C4 00 0E 80 D5 00 00 80 F5 00 6E 80 17 00 0F 00 1D 00 3E 00 55 00 06 00 45 00 02 00 55 00 80 00 D5 00 80 00 55 00 AB 00 55 00 FE 00 55 00 2B 00 56 01 FF 00 5E 01 B4 0B 7A 05 F0 0F 7A 05 FD 02 5B 04 FD 02 EE 11 5D A2 AA 55 51 AE EA 15 D5 2A A2 5D 30 CF 88 77 43 BC AB 54 71 8E 8A 75 05 FA 2E D1 0F F0 88 77 59 A6 BA 45 D5 2A 68 17 D7 28 A6 59 45 BA AB 54 17 E8 2B D4 36 C9 0A F5 07 F8 2A D5 01 FE 5A F5 25 FA A2 5D 70 8F EA 15 C5 3A A2 5D 71 8E 80 7F 40 BF A2 5D 81 FE FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 49 56 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF AA 55 FC 03 BA 45 FF 00 E6 11 74 8B BE 41 1D E2 AE 51 45 BA AA 55 71 8E AA 55 D8 27 2A D5 CD 32 15 FF 33 FF D5 7F 2C FF 55 FF 2B FF 55 FF 0E FF 0E F5 80 FF 2B D5 32 EF 0A F5 02 FD 22 DD 00 FF 82 7D 06 F9 23 DC 71 8E 2A D5 15 EA 49 F4 42 BC A4 51 E3 18 FC 01 7B 80 AD 50 95 2A 15 00 80 00 B5 40 FB 00 FD 00 FF 00 69 16 45 BA 59 04 03 04 55 00 F3 00 F5 00 FF 00 6F 10 FE 00 1B 44 D4 2B 55 00 ED 02 B6 41 E6 19 C9 37 46 BB C0 1F 03 FC 29 57 CC 33 0A F5 31 EF 8F 77 42 BF A8 57 40 BF AA 55 20 FF 40 FF C0 FF 51 FF 20 FF 51 FF 20 FF B8 47 00 FF 50 FF 84 FF 11 FF B3 FF 55 FF AA FF A6 51 7F 80 37 C0 5F A0 8E 71 85 FA 2A D5 80 FF B7 40 FD 02 BA 45 77 88 68 17 FD 02 B9 47 41 BE E0 1F 72 8F AA 55 14 EB 8A 75 36 E9 AB 54 05 FA 88 77 48 BF 22 DD 50 AF 29 D7 F4 0B E9 17 44 BB FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 9A 75 0D F2 8A 75 0F F0 12 FD 03 FC 2A D5 01 FE AD 53 52 AD A8 57 0D F2 AA 55 58 A7 A9 57 00 FF 55 FF 22 FF 52 FD 00 FF 91 7F 60 BF 55 FF 0A FF 18 F7 04 FB 92 7D A1 FE 55 FF 0A FC 5D F0 A0 C0 A8 54 CA 30 C4 30 40 80 01 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 05 00 3F 00 77 00 FF 00 07 00 01 02 45 03 C7 00 06 41 E3 02 85 42 EE 00 48 17 59 A6 98 77 44 BB 88 77 20 7F 14 7F 62 3F AA 75 A0 FF 5C FF 28 FF 04 FF 12 EF 22 DF 03 FF 88 77 00 FF 01 FF 22 FF D5 7F 32 FF D7 FF 28 FF 91 7F 02 FF 01 FF 40 FF 50 FF 88 FF 24 DF 00 FF 55 FF CB FF 55 FF 80 FF 01 FF 01 FF 14 FF 28 FF 48 F7 82 FF 45 FF 98 FF 34 FF FF FF FF FF FF FF 80 7F 03 FF 12 FD 0B FC 94 FF 0D FE C5 FC CF F8 A8 57 81 FE A8 57 4B BE A1 5F C0 3F FA 05 F4 0B AB 57 43 BF 95 7F 2E FF 55 FF 2B FF BD 5F 0F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF B0 81 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 88 77 E4 1B AA 55 00 FF 89 77 C0 3F D9 07 11 EF 8D 77 00 FF BD 57 80 7F 05 FF 22 FF 01 FC F0 E0 03 FF 8C FE 3C D0 E1 C2 DF 00 0B 04 44 00 30 00 C4 00 C0 40 74 40 E0 40 13 00 3F 00 5B 04 72 0D 11 00 3B 00 57 00 AC 03 7E 01 F6 09 FA 05 15 CA B1 40 FB 00 6C 11 5B A0 AD 50 1F E0 A5 50 F6 08 D5 00 67 80 95 41 EE 01 74 01 8A 01 C5 01 8F 01 F4 7F FE FF 3D FF 3F FF 3F FF 0F FF 7D FF FF FF 11 FF 0A FF C7 FF AF FF 77 FF FF FF 7F FF FF FF 50 FF E8 FF FC FF FE FF F5 FF FF FF FF FF FF FF A8 57 00 FF 0D F7 0E FF 57 FF EB FF F7 FF EB FF D3 7F FF FF 5F FF BF FF 7F FF 6F FF 57 FF FF FF FF FF FF FF FC FF FE FF FD FF FF FF FF FF FF FF DF F0 0B F0 27 F0 FF E0 77 C0 BB C0 55 80 AF 00 77 00 AA 00 FD 00 FF 00 77 00 FF 00 5F 00 FF 00 7F 1F 9F 3F 5F 3F FF 3F FF 7F BF 7F FF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF D2 65 C0 3F B5 58 12 E0 55 00 A0 00 41 00 08 00 55 80 23 00 54 00 AB 00 51 00 03 00 52 05 82 0F 11 00 02 01 15 0C 66 98 4D 30 D5 2A 8A 75 D7 28 49 B4 8D 72 13 CC B0 4F 68 57 05 FA 2A D5 D1 2E AA 55 B7 48 2D D0 1E E0 A5 50 F9 00 F5 01 E8 02 FD 00 F7 08 DD 00 F7 08 CD 10 BC 00 54 80 F8 00 0B 05 8C 03 53 05 13 0C 08 17 6A 17 69 17 58 2F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF F7 FF EF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FF FF FC FD FC FC F8 FD F0 F3 F0 F5 E0 EA C0 57 00 BB 00 50 07 AF 06 5F 06 BF 0F 55 00 AF 00 74 01 FE 01 5F 01 FF 03 59 07 BF 03 5D 07 FF 0F 7F FF FF FF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF D0 B4 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 51 00 3B 00 56 01 1E 03 69 17 40 3F 02 7D C0 3F 00 7F 83 FC 2A D5 24 DB AA 55 04 F3 A2 5D 05 FA 88 57 2D D2 CA 35 17 E8 AF 50 7F 80 AD 50 FF 00 0B 74 0F F0 A6 51 FE 01 76 01 FF 00 FD 00 F1 00 DD 00 F7 0C 98 47 78 8F 58 07 FA 07 F5 FF FF FF 64 10 C8 30 AC 50 7E 80 ED 10 EF 10 FD 00 F3 FC 28 57 9C 63 1A 65 F1 0E 6A 15 F0 0F 72 1D C0 3F FF FF FF FF FF FF FF FF 7F FF FF FF FF FF 83 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FE FC FD FC F8 F8 FD C0 E3 80 55 00 22 00 55 00 2A 00 55 00 02 00 55 00 B3 00 15 40 BE 00 55 00 3A 00 55 00 CA 20 75 0F BF 0F 4F 1F EF 1F 7F 1F 9F 3F 7F 3F AF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF A8 57 C0 3F A2 5D 05 FA AB 54 DF 20 AB 54 FF 00 AA 55 47 B8 B8 45 FF 00 6E 11 FF 00 FE 01 ED 12 76 01 F7 08 F9 04 F6 08 E4 10 FB 00 D6 01 EF 03 D3 01 2E 03 5D 07 7B 0C 59 37 A0 7F C2 FD E4 FF FF FF 7F FF 14 FF 05 FF 95 7F 02 FF 1D F3 06 FF 57 FF FB FF 5F FF 7F FF 5F FF 7E FF 55 FF 2A FF DA F5 FF FF F7 FF FF FF FF FF FF FF CF FF BF FF A0 5F 84 FB FD FF FF FF FF FF FF FF 7F FF FF FF 17 FF 26 FF 4C F7 AE FF 57 FF FE FF FF FF FF FF FF FF BF FF 2A D5 00 FF 05 FF A2 FF 5D FF FF FF FF FF FF FF B7 67 08 FF 40 FF 2B FF 4D FF EB FE F5 F0 E2 F0 D5 E0 E8 C0 55 80 80 00 54 00 00 00 55 00 02 00 55 00 22 00 15 00 02 00 55 00 08 00 75 00 B3 00 55 00 AE 00 55 00 BB 00 55 00 05 02 1F 7F FF 00 6F 1F AF 7F FD 7F 2F FF D7 FF CF 3F FF FF 0F FF 16 C1 CC FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 12 B7 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 7B 04 FF 00 AB 54 F7 08 6A 15 F7 08 AD 50 FE 00 6B 14 EE 10 E5 10 7B 80 56 01 8E 03 5F 07 7F 0F 65 1F EE 1F 9D 7F 34 FF 57 FF C9 FF D5 FF B8 FF D4 7F 20 FF 50 FF 80 FF 01 FF C0 FF 01 FF 20 FF D1 7F 80 FF 5D FF 02 FF C5 7F 22 FF 42 FD B0 FF 93 7F 28 FF 45 FF 13 FF 1D F7 01 FF 09 F7 0A FF 77 FF 32 FF 15 FF 62 FF 5D FF A0 FF 55 FF A8 FF 5F FF 2B FF 75 FF AC FF 51 FF C3 FF 51 FF 08 FF FF FF 87 FF 0F FF BF FF 77 FF 1F FF 5F FF AF FF 7F FF BF FF DF FF FF FF FF FF FF FF 7F FF FE FF FD FE EE DC F9 FC F0 F8 75 F0 F2 E0 F0 C0 C0 C0 51 00 00 00 55 00 00 00 55 00 02 00 55 00 00 00 55 00 00 00 55 00 00 00 51 00 00 00 55 00 80 00 1B 07 1B 07 0B 07 16 0F 1D 0F 2B 1F 75 1F 0E 3F F3 FC FF FF FF FF CF FF 55 FF BF FF DF FF DF FF BF 7F F3 FC FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 35 40 F3 00 5C 01 7C 03 7D 07 FB 0F 75 1F AF 7F 57 3F BE 7F DD FF FB FF 75 FF FB FF D5 FF FE FF F5 FF 83 FF DD FF 8F FF 11 FF 8A FF 55 FF FB FF 11 FF B2 FF 54 FF FA FF 54 FF E8 FF 76 FD 80 FF 1D FF 24 FF 71 DF 00 FF C0 7F 02 FF 23 DD 01 FE 55 FF E2 FF 55 FF 00 FF 04 FF 4A FF 44 FF 32 FF 04 FF 02 FF C3 FD 00 FF 13 FF 02 FF 55 FF 2B FF B1 7F 41 FF D1 FF 22 FF 4D F7 08 FF 91 7F 8A FF 16 FF 21 FF 51 FF CA FF 50 FF B8 FF 75 FF 30 FF DE FD F3 FF 55 FF EB FE 8F 74 06 FC 2D D8 00 F8 85 C0 A0 00 54 00 03 00 15 00 00 00 55 00 00 00 15 00 00 00 55 00 00 00 11 00 00 00 40 00 00 00 15 00 00 00 45 00 02 00 55 00 03 00 14 01 0A 01 55 3F A2 7F D5 7F 0A FF 95 FF 22 FF 55 FF 00 FF 7F FF 3F FF 3F FF 3F FF 7F FF FF FF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF F8 A6 00 08\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 55 FF 3A FF 55 FF FF FF 75 FF FF FF DD FF BF FF 77 FF 23 FF 55 FF E3 FF 55 FF FE FF 45 FF FB FF 55 FF B2 FF 57 FF E8 FF 54 FF F8 FF D1 FF EA FF 4E F5 80 FF 88 77 00 FF 19 F7 10 FF 67 DD 20 FF 00 FF 00 FF 20 DF 00 FF D9 77 01 FF 43 FD 00 FF 81 7F 04 FB 8D 77 82 FF 15 FF 29 FF 55 FF 4F BF 44 FF A2 FF 4D FF E4 FF 75 FF B7 FF 55 FF 6F FF C9 77 A2 FF F0 FF EA FF 55 FF E2 FF 15 FF 8C FF 54 FF 8A FF 44 FF 00 FF 00 FF 00 FF 09 F6 03 FE B5 50 02 F0 A5 40 40 80 D1 00 80 00 00 00 00 00 10 00 00 00 40 00 00 00 00 00 00 00 44 00 00 00 01 00 00 00 01 00 00 00 14 00 00 00 15 00 02 00 15 03 24 03 10 07 08 07 18 07 30 0F 50 0F B0 0F 11 FF 01 FF 01 FF 03 FF 99 77 0B FF 47 FF 0F FF F7 FF FB FF FF FF FB FF FD FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 77 FF FF FF 45 FF 0B FF 5F FF FF FF 57 FF AE FF 55 FF F2 FF F4 FF E8 FF 7D FF FE FF 5C FF DE FF 5E F1 86 FB 0D FF 40 FF 54 FF 2B FF 55 FF A2 FF 14 FF 00 FF 55 FF 09 FF 54 FF A8 FF 54 FF A0 FF 51 FF 20 FF 14 FF 22 FF 51 FF 3A FF 57 FF 8F FF 55 FF 4B FF 51 FF 2B FF 55 FF BE FF 57 FF E3 FF 57 FF A2 FF 54 FF AE FF FD F7 3A FF 55 FF AB FF 55 FF 20 FF 15 FF 28 FF 55 FF AA FF 44 FF E8 FF 9C 74 00 FC 10 F8 98 F0 10 E0 40 E0 00 C0 80 80 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 11 00 00 00 00 00 00 00 11 00 00 00 05 00 02 00 01 00 00 00 41 00 01 00 68 17 10 2F 6A 15 C0 3F 28 57 40 BF AA 55 44 BB 17 FF 2F FF 1F FF 6F BF 1F FF 1F FF 1F FF BF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 60 A6 00 0C\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 55 FF BA FF 55 FF EE FF 55 FF 2F FF 55 FF 82 FF 51 FF AB FF 55 FF 4A FF 55 FF A2 FF 55 FF 78 FF 35 FF 20 FF 51 FF 4A FF 55 FF 30 FF 45 FF 80 FF 11 FF F1 FF 50 FF AA FF 55 FF 29 FF 55 FF 42 FF 55 FF 28 FF 55 FF 8A FF 1D F7 20 FF 45 FF 02 FF 77 FF BF FF 55 FF 0A FF 55 FF 1A FF 15 FF 6A FF 5F FD BA FF 77 FF EE FF 55 FF AA FF 55 FF B2 FF 55 FF 4A FF F2 DE 86 FC 51 FC 28 F8 55 F0 20 E0 00 00 00 00 00 00 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 05 00 00 00 10 00 00 00 00 00 00 00 16 01 03 00 16 01 04 03 14 03 0C 03 1A 05 1D 02 89 77 50 AF A3 5D 10 EF A9 57 42 BF AD 57 13 EF FF 7F 3F FF 7F FF FF FF 7F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 15 FF 00 FF 11 FF 02 FF 11 FF 0A FF 55 FF 88 FF 55 FF 02 FF 55 FF 48 FF 55 FF 02 FF 51 FF A8 FF 50 FF 0A FF 55 FF AA FF 51 FF 00 FF 44 FF 00 FF 10 FF 0A FF 41 FF 50 AF 89 77 10 FF 51 FF 20 FF 14 FF 28 FF 51 FF C0 FF 48 F7 00 FF 52 FD 20 FF 95 7F 02 FF 00 FF 00 FF 08 F7 00 FF A0 5F 00 FF 55 FF 28 FF 15 FF 01 FF 90 7F 02 FC 0C F4 08 F8 64 C0 C0 C0 80 80 00 00 00 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 01 00 00 00 05 00 01 00 0A 15 3D 02 6A 15 D5 2A 6A 15 FD 02 2A 55 FF 00 A9 57 C3 3F A7 5F DB 2F A1 5F CF 3F 85 7F 4F BF FF FF FF FF FF FF FF FF FF FF FF FF F5 FF EE FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF C5 8F 00 0C\n" +
            "88 33 04 00 80 02 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FE FF FC FE FC F8 F9 F1 E3 F3 FF FF FF FF FF FF 77 8F 0E 07 87 7E FC FE FE FC FF FF FF FF FF FF BF 7E 3C 3E 1E 3C 18 3C 18 3C FF FF FF FF FF FF 77 FB F1 73 61 73 21 63 20 43 FF FF FF FF FF FF 7B 87 81 03 83 3F 3F 3F 37 0F FF FF FF FF FF FF BB C7 C3 83 D9 83 C1 9B 82 83 FF FF FF FF FF FF DE E3 C3 80 25 98 19 3C 78 3C FF FF FF FF FF FF BB 7D 71 39 03 33 23 87 CF 87 FF FF AF D5 FB D5 FB D5 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF E3 F3 F7 E3 E3 F3 F0 F3 F0 F8 FA FC FF FF FF FF F9 FC 9C 08 88 18 5B 91 03 13 07 33 FF FF FF FF 3D 98 3A 19 93 19 D1 93 C3 93 A7 D3 FF FF FF FF 48 03 18 0B 80 1B B3 18 33 B8 B1 FA FF FF FF FF 0F 07 37 0F 3F 7F 03 7F 01 03 07 03 FF FF FF FF 83 82 38 91 3C 99 A8 11 01 03 8B 07 FF FF FF FF 79 3C 7D 38 78 39 4B 31 81 03 CB 87 FF FF FF FF 8F CF 8F DF CF 9F CF 9F CF 9F FF 9F FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 47 24 00 0C";
    public static String onlyTileData = "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FE FF FE FE FE FE FE FE FE FE FE FE\n" +
            "FF FF FF FF 1C FF 3E 1C 0C 1C 04 0C 0C 04 24 04\n" +
            "FF FF FF FF 63 FF 07 63 43 23 63 3F 02 62 02 62\n" +
            "FF FF FF FF FF FF FF FF FF FF F7 FE 03 01 11 00\n" +
            "FF FF FF FF FF FF 8F FF 47 8F 05 03 00 8E C8 8C\n" +
            "FF FF FF FF FF FF FF FF FF FF 7F 9F 0E 04 E0 46\n" +
            "FF FF FF FF FF FF FF FF FF FF E7 FF 41 03 21 01\n" +
            "FF FF FF FF F8 FF F0 F8 F0 F8 E0 F8 C0 80 18 00\n" +
            "FF FF FF FF FF FF 7F FF 7E FF 6D F3 61 C0 44 8C\n" +
            "FF FF FF FF BF FF BF 1F 5F 0F 0F 1F 9F FF 3F 7F\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FE FE FE FE FE FE FE FE FE FE FF FF FF FF FF FF\n" +
            "24 10 30 10 30 18 3C 18 18 3C FF FF FF FF FF FF\n" +
            "02 62 02 62 02 62 42 22 02 62 FF FF FF FF FF FF\n" +
            "19 30 19 30 19 30 19 30 11 38 FF FF FF FF FF FF\n" +
            "4C 88 4C 88 4C 88 48 8C CC 8E FF FF FF FF FF FF\n" +
            "06 00 FA 04 7A E4 A2 44 06 0C FF FF FF FF FF FF\n" +
            "20 31 21 30 20 31 21 31 61 31 FF FF FF FF FF FF\n" +
            "00 18 00 18 00 18 98 00 C0 80 FF FF FF FF FF FF\n" +
            "04 8C 84 0C 04 8C 48 84 61 C0 FF FF FF FF FF FF\n" +
            "7F 3F 7F 3F 7F 3F 3F 7F 7F FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "AA 55 15 EA 2B D4 55 AA AB 54 55 AA 2B D4 7D 82\n" +
            "77 00 FB 00 DD 00 FF 00 FD 00 FE 00 F5 00 FE 00\n" +
            "55 00 AA 00 55 00 AA 00 55 00 FA 00 55 00 EA 00\n" +
            "55 00 00 00 55 00 80 00 11 00 22 00 54 00 08 00\n" +
            "10 00 00 00 45 00 00 00 10 00 00 00 54 00 00 00\n" +
            "00 00 00 00 00 00 00 00 01 00 00 00 40 00 00 00\n" +
            "15 00 02 00 04 00 00 00 55 00 00 00 15 00 02 00\n" +
            "15 00 22 00 15 00 0A 00 15 00 2B 00 55 00 AF 00\n" +
            "57 00 FA 00 F5 00 FC 00 75 00 F2 00 D4 00 E8 00\n" +
            "50 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "7F 00 3F 00 5F 00 3F 00 56 01 3D 02 5B 04 3F 00\n" +
            "54 00 A0 00 51 00 82 00 51 00 A0 00 54 00 C0 00\n" +
            "10 00 22 00 04 00 00 00 00 00 00 00 40 00 02 00\n" +
            "01 00 02 00 00 00 00 00 01 00 00 00 41 00 00 00\n" +
            "01 00 02 00 00 00 02 00 00 00 2A 00 44 00 00 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "AA 55 07 F8 AB 54 1F E0 AE 51 4D B2 AB 54 57 A8\n" +
            "F7 00 FF 00 D5 00 FF 00 F5 00 BF 00 F5 00 FF 00\n" +
            "55 00 BA 00 55 00 E8 00 55 00 AA 00 55 00 EA 00\n" +
            "55 00 A2 00 54 00 22 00 55 00 22 00 55 00 AA 00\n" +
            "15 00 00 00 55 00 00 00 50 00 22 00 54 00 A0 00\n" +
            "11 00 00 00 54 00 02 00 51 00 02 00 45 00 00 00\n" +
            "15 00 23 00 55 00 0A 00 17 00 2B 00 55 00 AA 00\n" +
            "57 00 BD 02 5F 00 FF 00 75 00 BA 00 55 00 EA 00\n" +
            "D0 00 A0 00 50 00 80 00 50 00 80 00 40 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "5F 00 3D 02 5F 00 2D 02 1A 05 3D 02 17 00 0B 00\n" +
            "51 00 A2 00 55 00 E0 00 D1 00 F0 00 D5 00 F8 00\n" +
            "11 00 22 00 50 00 00 00 10 00 00 00 44 00 00 00\n" +
            "10 00 20 00 00 00 00 00 05 00 02 00 54 00 00 00\n" +
            "00 00 22 00 44 00 80 00 00 00 02 00 44 00 08 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "2E D1 0F F0 AB 54 DF A0 AA 55 45 BA 2B D4 1F E0\n" +
            "77 00 FB 00 FF 00 FE 00 77 00 FF 00 FF 00 FF 00\n" +
            "55 00 3A 00 55 00 AA 00 55 00 BA 00 55 00 EA 00\n" +
            "55 00 A2 00 54 00 A8 00 55 00 22 00 55 00 A0 00\n" +
            "15 00 20 00 54 00 00 00 11 00 22 00 54 00 00 00\n" +
            "15 00 08 00 45 00 02 00 15 00 20 00 55 00 00 00\n" +
            "55 00 2B 00 55 00 02 00 15 00 03 00 55 00 0F 00\n" +
            "75 00 AA 00 55 00 BA 00 55 00 AB 00 55 00 FF 00\n" +
            "10 00 80 00 54 00 80 00 51 00 22 00 55 00 AA 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 40 00 A2 00\n" +
            "00 00 00 00 00 00 00 00 10 00 20 00 55 00 8A 00\n" +
            "15 00 2B 00 14 00 2A 00 15 00 AA 00 55 00 BA 00\n" +
            "F5 00 7A 80 FD 00 FF 00 55 00 2B 00 55 00 8A 00\n" +
            "51 00 20 00 50 00 A2 00 51 00 AA 00 45 00 E8 00\n" +
            "51 00 28 00 45 00 88 00 51 00 A2 00 55 00 00 00\n" +
            "11 00 28 00 50 00 28 00 11 00 2A 00 55 00 8A 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "2A D5 0D F2 2B D4 17 E8 2A D5 1D E2 2B D4 17 E8\n" +
            "F7 00 FA 00 D5 00 FF 00 F5 00 FA 00 F5 00 FF 00\n" +
            "51 00 AA 00 55 00 AA 00 55 00 BA 00 55 00 A8 00\n" +
            "55 00 28 00 55 00 88 00 55 00 00 00 55 00 00 00\n" +
            "51 00 A0 00 50 00 80 00 50 00 00 00 44 00 00 00\n" +
            "51 00 00 00 40 00 00 00 00 00 00 00 04 00 00 00\n" +
            "16 01 3D 02 4A 15 35 0A 60 1F CB 3F 15 7F 4A 3F\n" +
            "F7 00 FF 00 AB 54 87 F8 02 FD 80 FF 50 FF E8 FF\n" +
            "55 00 FB 00 FD 00 D7 28 88 77 14 EB 02 FD 11 EE\n" +
            "55 00 EA 00 75 00 FF 00 FA 05 55 AA 28 D7 11 EE\n" +
            "55 00 BA 00 7F 00 FF 00 A8 57 74 AB 20 DF 82 FF\n" +
            "55 00 BA 00 F5 00 FE 00 CD 30 17 E8 29 D4 89 FE\n" +
            "17 00 0B 00 45 00 0A 00 11 00 2A 00 55 00 EA 00\n" +
            "51 00 AA 00 55 00 AA 00 75 00 AA 00 55 00 EA 00\n" +
            "51 00 22 00 55 00 A2 00 55 00 AA 00 55 00 AA 00\n" +
            "55 00 2A 00 55 00 A2 00 55 00 AA 00 55 00 AA 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "0E F1 17 E8 0B F4 95 EA 0B F4 95 EA 0B F4 85 FA\n" +
            "75 00 FF 00 F5 00 FE 00 F5 00 FF 00 FD 00 FE 00\n" +
            "55 00 A2 00 55 00 AA 00 55 00 AA 00 54 00 AA 00\n" +
            "15 00 00 00 45 00 88 00 51 00 22 00 14 00 00 00\n" +
            "41 00 00 00 00 00 00 00 50 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00\n" +
            "57 3F 2B 1F 65 1F 36 0F 47 3F C2 3F 07 7F C8 3F\n" +
            "75 FF FA FF 5D FF BE FF 57 FF BB FF 67 FF BB FF\n" +
            "20 FF 82 FF 74 FF A8 FF 75 FF AA FF 54 FF AE FF\n" +
            "A1 5F 40 BF F1 DF 2A FF 57 FF AA FF 55 FF BA FF\n" +
            "21 FF A8 FF 4D FF 8B FF 97 FF BB FF 7F FF FF FF\n" +
            "20 FF 82 FF 70 FF EE FF FB FF EA FF F5 FF EA FF\n" +
            "D5 00 5A A0 3D C0 8E F0 8D 70 A6 F8 4D F0 9E E0\n" +
            "57 00 AB 00 55 00 AB 00 55 00 AB 00 55 00 AA 00\n" +
            "55 00 AA 00 D5 00 AA 00 55 00 AA 00 55 00 AA 00\n" +
            "55 00 AA 00 55 00 AA 00 55 00 AA 00 55 00 AA 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "EA D5 85 FA EA D5 81 FE 8A F5 81 FE C1 FE 83 FC\n" +
            "F5 00 3F 80 B5 40 FE 00 E5 10 7B 80 DF 00 FF 00\n" +
            "55 00 AA 00 55 00 A8 00 55 00 BA 00 55 00 E8 00\n" +
            "11 00 00 00 40 00 00 00 51 00 00 00 50 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00\n" +
            "85 7F CA 3F 84 7F 88 7F 8D 77 CA 3F 4D 37 4A 3F\n" +
            "75 FF AA FF 51 FF 28 FF 15 FF 2A FF 05 FF 08 FF\n" +
            "46 FF 8A FF 45 FF 80 FF 50 FF 2A FF 55 FF 8A FF\n" +
            "53 FF 83 FF 75 DF 0A FF 01 FF 82 FF 44 FF A2 FF\n" +
            "FF FF FF FF FF FF FB FF 57 FF AA FF 5F FF AA FF\n" +
            "FD FF FE FF FC FF EA FF D0 FF A0 FF D4 FF E8 FF\n" +
            "11 E0 62 80 11 C0 60 80 51 80 60 80 C0 00 40 80\n" +
            "55 00 3A 00 55 00 A2 00 55 00 2A 00 54 00 0A 00\n" +
            "55 00 BA 00 55 00 AA 00 55 00 AA 00 55 00 AA 00\n" +
            "55 00 BA 00 55 00 EA 00 75 00 BB 00 55 00 AE 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "C6 F9 81 FE DA F5 A1 FE CA F5 E0 FF C2 FD E0 FF\n" +
            "A7 50 FF 00 AF 50 7F 80 BF 40 FF 00 EF 10 7F 80\n" +
            "55 00 AB 00 D4 00 EA 00 F5 00 FF 00 55 00 FE 00\n" +
            "15 00 02 00 55 00 A0 00 55 00 AA 00 55 00 EE 00\n" +
            "10 00 00 00 44 00 00 00 55 00 22 00 54 00 A8 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 00\n" +
            "21 1F 32 0F 00 1F 10 0F 18 07 0C 03 05 00 00 00\n" +
            "10 FF 86 FB 45 FF 8B FE 40 FF 80 FF A0 5F D0 2F\n" +
            "55 FF 2A FF 57 FF AC FF 19 FF 22 FF 01 FF 00 FF\n" +
            "44 FF A2 FF 05 FF 8B FF 45 FF 22 FF 44 FF 81 FE\n" +
            "2D FF 82 FF 47 FF 88 FF 50 FF 08 FF 0B F4 BE C0\n" +
            "50 FF A1 FE 3B D4 16 E8 34 C0 60 80 40 00 00 00\n" +
            "C0 00 80 00 00 00 00 00 01 00 00 00 00 00 00 00\n" +
            "11 00 2A 00 04 00 00 00 01 00 22 00 05 00 08 00\n" +
            "55 00 AB 00 55 00 AA 00 15 00 AA 00 55 00 AA 00\n" +
            "55 00 AA 00 55 00 AE 00 F7 00 2B 00 D5 00 AA 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "DE F5 E4 FB C2 FD E0 FF F2 FD F1 FE F2 FD E8 FF\n" +
            "BF 40 7F 80 AB 54 7B 84 AF 50 45 BA EA 15 5F A0\n" +
            "F5 00 FB 00 95 40 FF 00 FF 00 FF 00 9F 40 FF 00\n" +
            "77 00 BB 00 5F 00 FF 00 77 00 FF 00 7F 00 FF 00\n" +
            "55 00 B2 00 55 00 EE 00 F5 00 FE 00 AF 50 17 E8\n" +
            "40 00 80 00 40 00 80 00 50 00 80 00 55 00 F2 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "38 07 0E 03 06 01 03 00 01 00 00 00 00 00 00 00\n" +
            "11 FF 02 FF 02 FD 41 BE 88 77 1F 20 00 00 00 00\n" +
            "08 F7 05 FA 2D D0 70 80 C0 00 00 00 00 00 00 00\n" +
            "F0 00 A0 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "11 00 22 00 10 00 00 00 11 00 22 00 04 00 02 00\n" +
            "55 00 2A 00 15 00 0A 00 55 00 AA 00 55 00 AA 00\n" +
            "77 00 BB 00 55 00 AA 00 57 00 AB 00 5F 00 AA 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "F0 FF FC FF F0 FF FC FF FE FF FC FF FC FF FE FF\n" +
            "AE 51 55 AA BF 40 57 A8 2A D5 55 AA 2B D4 17 E8\n" +
            "BF 40 FF 00 FF 00 FF 00 F6 01 DF 20 BD 40 7F 80\n" +
            "7E 01 BD 02 D2 05 FD 02 6F 10 FF 00 57 00 BF 00\n" +
            "87 78 41 BE 84 7F 48 BF A0 5F C2 3F F5 0F F2 0F\n" +
            "55 00 FB 00 DD 00 1F E0 0B F4 04 FB 24 FF 4A FF\n" +
            "50 00 B0 00 D5 00 EE 00 FF 00 9D 62 8A 75 C1 FE\n" +
            "00 00 00 00 00 00 80 00 50 00 F8 00 BD 40 5F A0\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 41 00 EA 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 04 00 8A 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00\n" +
            "01 00 00 00 00 00 00 00 10 00 00 00 45 00 00 00\n" +
            "11 00 02 00 01 00 02 00 15 00 A2 00 45 00 A2 00\n" +
            "11 00 22 00 55 00 2A 00 55 00 AA 00 55 00 AA 00\n" +
            "57 00 BF 00 7D 00 FF 00 77 00 BB 00 77 00 FF 00\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FE FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "29 D6 05 FA 0A F5 85 FA 42 FD 85 FA CE F5 C9 FE\n" +
            "AF 50 5F A0 BF 40 5E A0 A5 50 5F A0 BF 40 7F 80\n" +
            "57 00 BB 00 55 00 AE 00 55 00 AA 00 55 00 E8 00\n" +
            "58 07 FC 03 56 01 8B 00 55 00 AB 00 55 00 2B 00\n" +
            "11 FF 80 FF BE 41 FE 00 55 00 E0 00 50 00 E0 00\n" +
            "15 FF A2 FF 84 7F B0 0F 06 01 00 00 00 00 00 00\n" +
            "0E F1 A0 FF 81 FF A0 FF 11 FF E2 3F 20 1F 0C 03\n" +
            "F5 00 DF 20 2A D5 95 EA 0A F5 04 FB 44 FF 01 FE\n" +
            "55 00 BB 00 AB 54 57 A8 0A F5 00 FF 2A D5 10 EF\n" +
            "50 00 BA 00 DD 00 FF 00 EF 10 5D A2 8A 75 05 FA\n" +
            "11 00 2A 00 55 00 FF 00 F7 00 55 AA AA 55 45 BA\n" +
            "55 00 BA 00 D5 00 FE 00 F7 00 47 B8 2B D4 17 E8\n" +
            "51 00 A2 00 55 00 AA 00 55 00 BB 00 D5 00 EA 00\n" +
            "75 00 2B 00 55 00 AA 00 57 00 BB 00 5D 00 BF 00\n" +
            "7F 00 BF 00 5F 00 FF 00 7E 01 FF 00 7E 01 FD 02\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "C0 FF EC FB F4 FF F9 FE F4 FF FA FF FC FF FE FF\n" +
            "AF 50 5D A2 2F D0 5F A0 9A 65 4D B2 3E C1 D7 A8\n" +
            "55 00 E2 00 F5 00 FA 00 F5 00 FB 00 F5 00 FE 00\n" +
            "16 01 3D 02 5A 05 BD 02 5E 01 BC 03 5A 05 FD 02\n" +
            "D0 00 F0 00 A5 50 5E A0 0B F4 25 FA 64 FF 0A FF\n" +
            "00 00 00 00 00 00 80 00 D0 00 FA 00 7F 80 84 FB\n" +
            "01 00 00 00 00 00 00 00 00 00 00 00 44 00 7A 80\n" +
            "FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "5E 01 03 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "80 7F C0 3F 1E 01 02 00 01 00 00 00 00 00 00 00\n" +
            "09 F7 08 FF A0 5F D5 2A 17 00 2A 00 54 00 80 00\n" +
            "0B F4 15 EA 2A D5 F5 0A EE 11 BD 02 5F 00 2F 00\n" +
            "FF 00 FF 00 FD 00 7E 80 FF 00 7F 80 FF 00 FF 00\n" +
            "77 00 FB 00 FF 00 FF 00 7E 01 DF 20 FF 00 FF 00\n" +
            "FE 01 FD 02 FE 01 D5 2A EE 11 D5 2A FA 05 54 AB\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FE FF FF FF FE FF FF FF FF FF FF FF FF FF\n" +
            "0A F5 95 EA 63 DC A1 FE 42 FD E1 FE DA F5 E5 FA\n" +
            "F5 00 FB 00 F5 00 FF 00 D7 00 FB 00 95 40 FF 00\n" +
            "5E 01 FC 03 5A 05 EF 00 DE 01 BF 00 55 00 AA 00\n" +
            "95 7F 1A FF D6 7F 0E FF D5 3F 42 BF FA 05 FF 00\n" +
            "00 FF B6 FF 50 FF A4 FF 56 FF 83 FF 17 FD 10 EF\n" +
            "1F E0 A0 FF 40 FF 88 FF 44 FF A8 FF 44 FF 80 FF\n" +
            "55 00 7E 80 23 DC 31 EE 00 FF 20 FF 20 DF 80 FF\n" +
            "00 00 A0 00 D5 00 17 E8 0A F5 00 FF 02 FD 11 EE\n" +
            "05 00 2A 00 55 00 BE 00 F5 00 5F A0 BF 40 1F E0\n" +
            "10 00 20 00 50 00 E0 00 50 00 A0 00 40 00 80 00\n" +
            "17 00 3B 00 5D 00 2A 00 17 00 2B 00 17 00 2A 00\n" +
            "FF 00 FD 02 FF 00 FF 00 FF 00 BD 02 7E 01 FF 00\n" +
            "FF 00 FD 02 FF 00 F5 0A 7E 01 FD 02 EE 11 F7 08\n" +
            "EC 13 D0 2F AE 51 D5 2A CA 35 54 AB EA 15 55 AA\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "D0 FF F1 FE F4 FF FA FF FC FF FA FF FC FF FE FF\n" +
            "A5 50 5F A0 BD 40 7E 80 AF 50 5F A0 2F D0 9F E0\n" +
            "55 00 BA 00 55 00 80 00 50 00 A0 00 54 00 E0 00\n" +
            "55 00 A2 00 54 00 08 00 50 00 00 00 10 00 00 00\n" +
            "57 00 A2 00 40 00 00 00 00 00 00 00 00 00 00 00\n" +
            "A0 5F 2F 00 01 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 FF 00 FF 5F 00 0B 00 01 00 00 00 00 00 00 00\n" +
            "08 F7 45 BA EA 15 EF 00 55 00 2A 00 05 00 00 00\n" +
            "AF 50 56 A8 FD 00 FE 00 75 00 AA 00 54 00 80 00\n" +
            "50 00 A0 00 40 00 00 00 11 00 00 00 00 00 00 00\n" +
            "17 00 22 00 05 00 0A 00 51 00 22 00 45 00 02 00\n" +
            "FF 00 BF 00 7F 00 FF 00 77 00 BF 00 DC 01 FD 02\n" +
            "EE 11 DD 22 FE 01 D7 28 FE 01 DD 22 FF 00 D7 28\n" +
            "A8 57 54 AB AA 55 D4 2B AE 51 54 AB AA 55 D0 2F\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FA FF F9 FF EA FF F5 FF FA FF F5 FF E8 FF\n" +
            "0F F0 9D E2 43 FC 81 FE 52 FD 09 FE 50 FF AB FE\n" +
            "50 00 E0 00 D0 00 E0 00 D5 00 FA 00 95 40 7E 80\n" +
            "04 00 00 00 00 00 00 00 50 00 20 00 54 00 28 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "10 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
            "15 00 02 00 45 00 0B 00 15 00 23 00 57 00 AB 00\n" +
            "77 00 BF 00 5F 00 EF 00 7F 00 FD 02 7F 00 FF 00\n" +
            "FE 01 D5 2A FE 01 FD 02 FA 05 55 AA AA 55 D7 28\n" +
            "A9 57 5C A3 EE 11 56 A9 FE 01 7E 81 F7 01 EC 03\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FE FF FC FE FC F8 F9 F1 E3 F3\n" +
            "FF FF FF FF FF FF 77 8F 0E 07 87 7E FC FE FE FC\n" +
            "FF FF FF FF FF FF BF 7E 3C 3E 1E 3C 18 3C 18 3C\n" +
            "FF FF FF FF FF FF 77 FB F1 73 61 73 21 63 20 43\n" +
            "FF FF FF FF FF FF 7B 87 81 03 83 3F 3F 3F 37 0F\n" +
            "FF FF FF FF FF FF BB C7 C3 83 D9 83 C1 9B 82 83\n" +
            "FF FF FF FF FF FF DE E3 C3 80 25 98 19 3C 78 3C\n" +
            "FF FF FF FF FF FF BB 7D 71 39 03 33 23 87 CF 87\n" +
            "FF FF AF D5 FB D5 FB D5 FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "E3 F3 F7 E3 E3 F3 F0 F3 F0 F8 FA FC FF FF FF FF\n" +
            "F9 FC 9C 08 88 18 5B 91 03 13 07 33 FF FF FF FF\n" +
            "3D 98 3A 19 93 19 D1 93 C3 93 A7 D3 FF FF FF FF\n" +
            "48 03 18 0B 80 1B B3 18 33 B8 B1 FA FF FF FF FF\n" +
            "0F 07 37 0F 3F 7F 03 7F 01 03 07 03 FF FF FF FF\n" +
            "83 82 38 91 3C 99 A8 11 01 03 8B 07 FF FF FF FF\n" +
            "79 3C 7D 38 78 39 4B 31 81 03 CB 87 FF FF FF FF\n" +
            "8F CF 8F DF CF 9F CF 9F CF 9F FF 9F FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF\n" +
            "FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF";
}

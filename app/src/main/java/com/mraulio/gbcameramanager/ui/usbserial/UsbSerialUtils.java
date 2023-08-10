package com.mraulio.gbcameramanager.ui.usbserial;

import static com.mraulio.gbcameramanager.MainActivity.magicCheck;

public class UsbSerialUtils {

    public static boolean magicIsReal(byte[] bytes) {
        //Could add more places to check for magic. First 4 occurrences: 0x10D2, 0x11AB, 0x11D0,0x11F5,
        int startPosition = 0x2FB1;
        int numberOfBytes = 5;

        if (magicCheck) {
            byte[] magicPattern = {(byte) 0x4D, (byte) 0x61, (byte) 0x67, (byte) 0x69, (byte) 0x63};

            for (int i = 0; i < numberOfBytes; i++) {
                if (bytes[startPosition + i] != magicPattern[i]) {
                    return false;
                }
            }
            return true;//The Magic bytes exist
        }
        //I do a FF check just in case magic is not checked and the save part is empty
        for (int i = startPosition; i < startPosition + numberOfBytes; i++) {
            if (bytes[i] == (byte) 0xFF) {
                return false;
            }
        }
        return true;//Save has data (Maybe no Magic)
    }

}

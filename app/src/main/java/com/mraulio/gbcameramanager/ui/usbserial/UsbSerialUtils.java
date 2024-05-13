package com.mraulio.gbcameramanager.ui.usbserial;

import static com.mraulio.gbcameramanager.utils.StaticValues.magicCheck;

import java.io.File;

public class UsbSerialUtils {

    /**
     * Method to check for Magic bytes, or no FF bytes if magicCheck is false
     *
     * @param bytes the bytes of the save file
     * @return true if Magic or not FF bytes
     */
    public static boolean magicIsReal(byte[] bytes) {
        //Could add more places to check for magic. First 4 occurrences: 0x10D2, 0x11AB, 0x11D0,0x11F5,
        int[] magicStartPositions = {0x10D2, 0x11AB, 0x11D0, 0x11F5, 0x2FB1};
        int numberOfBytes = 5;

        if (magicCheck) {
            final byte[] MAGIC_PATTERN = {(byte) 0x4D, (byte) 0x61, (byte) 0x67, (byte) 0x69, (byte) 0x63};

            for (int j = 0; j < magicStartPositions.length; j++) {
                for (int i = 0; i < numberOfBytes; i++) {
                    if (bytes[magicStartPositions[j] + i] != MAGIC_PATTERN[i]) {
                        return false;
                    }
                }
            }
            return true;//The Magic bytes exist
        }

        //I do a FF check just in case magic is not checked and the save part is empty
        for (int i = magicStartPositions[0]; i < magicStartPositions[0] + numberOfBytes; i++) {
            if (bytes[i] == (byte) 0xFF) {
                return false;
            }
        }
        return true;//Save has data (Maybe no Magic)
    }

    public static boolean deleteFolderRecursive(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolderRecursive(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return folder.delete();
    }

}

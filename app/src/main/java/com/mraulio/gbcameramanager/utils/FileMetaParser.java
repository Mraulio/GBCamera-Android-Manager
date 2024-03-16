package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.CharMap.CHAR_MAP_INT;
import static com.mraulio.gbcameramanager.utils.CharMap.CHAR_MAP_JP;
import static com.mraulio.gbcameramanager.utils.CharMap.charMapDateDigit;
import static com.mraulio.gbcameramanager.utils.Utils.bytesToHex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on this https://github.com/HerrZatacke/gb-printer-web/blob/master/src/javascript/tools/transformSav/getFileMeta.js
 */
public class FileMetaParser {

    public HashMap<String, Object> getFileMeta(byte[] data, int baseAddress, boolean cartIsJP) {
        int cartIndex = (baseAddress / 0x1000) - 2;
        int albumIndex = cartIndex >= 0 ? data[0x11b2 + cartIndex] : 64;

        byte[] userId = Arrays.copyOfRange(data, baseAddress + 0x00F00, baseAddress + 0x00F03 + 1);

        byte[] userName = Arrays.copyOfRange(data, baseAddress + 0x00F04, baseAddress + 0x00F0C + 1);
        byte genderAndBloodType = data[baseAddress + 0x00F0D];

        byte[] birthDate = Arrays.copyOfRange(data, baseAddress + 0x00F0E, baseAddress + 0x00F11 + 1);

        byte[] comment = Arrays.copyOfRange(data, baseAddress + 0x00F15, baseAddress + 0x00F2F + 1);

        boolean isCopy = data[baseAddress + 0x00F33] != 0;

        byte frameNumber = data[baseAddress + 0x00F54];

        String parsedUserId = parseUserId(userId,cartIsJP);
        String parsedBirthDate = parseBirthDate(birthDate, cartIsJP);
        String parsedUserName = convertToReadable(userName, cartIsJP);
        String parsedGender = parseGender(genderAndBloodType);
        String parsedBloodType = parseBloodType(genderAndBloodType);
        String parsedComment = convertToReadable(comment, cartIsJP);

        HashMap<String, Object> meta = new HashMap<>();
        meta.put("userId", parsedUserId);
        meta.put("birthDate", parsedBirthDate);
        meta.put("userName", parsedUserName);
        meta.put("gender", parsedGender);
        meta.put("bloodType", parsedBloodType);
        meta.put("comment", parsedComment);
        meta.put("isCopy", isCopy);
        meta.put("frameIndex", frameNumber);
        return meta;
    }

    public static String parseBirthDate(byte[] birthDate, boolean cartIsJP) {
        String[] parts = new String[4];
        for (int i = 0; i < birthDate.length; i++) {
            parts[i] = convertDigit(birthDate[i]);
        }

        String fullYear = concatYear(parts[0], parts[1]);

        if (cartIsJP) {
            return fullYear + "年" + parts[2] + "月" + parts[3] + "日";
        } else {
            return parts[2] + "/" + parts[3] + "/" + fullYear;
        }
    }

    public static String parseUserId(byte[] userId, boolean cartIsJP) {
        String[] digits = new String[userId.length];
        for (int i = 0; i < userId.length; i++) {
            digits[i] = convertDigit(userId[i]);
            if (digits[i].equals("--")) {
                digits[i] = "00";
            }
        }

        String prefix = cartIsJP ? "PC-" : "GC-";

        return prefix + String.join("", digits);
    }

    private static String concatYear(String year1, String year2) {
        if (year1.equals("--") && !year2.equals("--")) {
            return "00" + year2;
        } else if (!year1.equals("--") && year2.equals("--")) {
            return year1 + "00";
        } else {
            return year1 + year2;
        }
    }

    private static String convertDigit(byte byteValue) {
        if (byteValue == 0) {
            return "--";
        }

        int upperIndex = (byteValue >> 4) & 0x0F;
        int lowerIndex = byteValue & 0x0F;//0x0F = 0b00001111

        char upperFormat = charMapDateDigit.charAt(upperIndex);
        char lowerFormat = charMapDateDigit.charAt(lowerIndex);

        return "" + upperFormat + lowerFormat;
    }


    private String convertToReadable(byte[] data, boolean cartIsJP) {
        StringBuilder result = new StringBuilder();
        for (byte value : data) {
            // Convert the byte to an int without a sign
            int unsignedValue = (value) & 0xFF;
            Map<Integer, String> charMap = cartIsJP ? CHAR_MAP_JP : CHAR_MAP_INT;
            String mappedValue = charMap.get((int) unsignedValue);
            if (mappedValue != null) {
                result.append(mappedValue);
            } else {
                result.append(" ");
            }
        }
        return result.toString();
    }

    private String parseGender(byte b) {
        if ((b & 0x01) != 0) {
            return "m";
        }
        if ((b & 0x02) != 0) {
            return "f";
        }
        return "-";
    }

    private String parseBloodType(byte b) {
        switch (b & 0x1C) {
            case 0x04:
                return "A";
            case 0x08:
                return "B";
            case 0x0C:
                return "O";
            case 0x10:
                return "AB";
            default:
                return "-";
        }
    }
}
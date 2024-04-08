package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.CharMap.CHAR_MAP_INT;
import static com.mraulio.gbcameramanager.utils.CharMap.CHAR_MAP_JP;
import static com.mraulio.gbcameramanager.utils.CharMap.charMapDateDigit;
import static com.mraulio.gbcameramanager.utils.HomebrewRomsMetaParser.parseHomebrewRomMetadata;
import static com.mraulio.gbcameramanager.utils.Utils.bytesToHex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on this https://github.com/HerrZatacke/gb-printer-web/blob/master/src/javascript/tools/transformSav/getFileMeta.js
 */
public class FileMetaParser {

    public LinkedHashMap<String, String> getFileMeta(byte[] data, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        boolean cartIsJP;
        switch (saveTypeIntJpHk) {
            case JP:
            case HK:
                cartIsJP = true;
                break;
            default:
                cartIsJP = false;
        }

        byte[] userId = Arrays.copyOfRange(data, 0x00000, 0x00003 + 1);

        byte[] userName = Arrays.copyOfRange(data, 0x00004, 0x0000C + 1);
        byte genderAndBloodType = data[0x0000D];

        byte[] birthDate = Arrays.copyOfRange(data, 0x0000E, 0x00011 + 1);

        byte[] comment = Arrays.copyOfRange(data, 0x00015, 0x0002F + 1);

        boolean isCopy = data[0x00033] != 0;

        byte frameNumber = data[0x00054];

        String parsedUserId = parseUserId(userId, cartIsJP);
        String parsedBirthDate = parseBirthDate(birthDate, cartIsJP);
        String parsedUserName = convertToReadable(userName, cartIsJP);
        String parsedGender = parseGender(genderAndBloodType);
        String parsedBloodType = parseBloodType(genderAndBloodType);
        String parsedComment = convertToReadable(comment, cartIsJP);


        LinkedHashMap<String, String> meta = new LinkedHashMap<>();
        meta.put("origin", saveTypeNames.get(saveTypeIntJpHk.name()));
        meta.put("userId", parsedUserId);
        meta.put("userName", parsedUserName);
        meta.put("birthDate", parsedBirthDate);
        meta.put("gender", parsedGender);
        meta.put("bloodType", parsedBloodType);
        meta.put("comment", parsedComment);
        meta.put("isCopy", String.valueOf(isCopy));
        meta.put("frameIndex", String.valueOf(frameNumber));
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
        return result.toString().trim();
    }

    private String parseGender(byte b) {
        if ((b & 0x01) != 0) {
            return "♂";
        }
        if ((b & 0x02) != 0) {
            return "♀";
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

    public static HashMap<String, String> saveTypeNames = new HashMap<String, String>() {{
        put("INT", "International Camera");
        put("JP", "Japanese Camera");
        put("HK", "Hello Kitty Camera");
    }};

}
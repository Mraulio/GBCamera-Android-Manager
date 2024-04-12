package com.mraulio.gbcameramanager.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class HomebrewRomValues {

    public static final int MASK_CAPTURE = 0b00000010;
    public static final int MASK_EDGE_EXCLUSIVE = 0b10000000;
    public static final int MASK_EDGE_OP_MODE = 0b01100000;
    public static final int MASK_GAIN = 0b00011111;
    public static final int MASK_EDGE_RATIO = 0b01110000;
    public static final int MASK_INVERT_OUTPUT = 0b00001000;
    public static final int MASK_VOLTAGE_REF = 0b00000111;
    public static final int MASK_ZERO_POINT = 0b11000000;
    public static final int MASK_VOLTAGE_OUT = 0b00111111;
    public static final int MASK_DITHER_SET = 0b00000001;
    public static final int MASK_DITHER_ON_OFF = 0b00000010;

    //For Photo:
    public static final int MASK_DITHERING = 0b00001111;
    public static final int MASK_DITHERING_HIGHLIGHT = 0b00010000;
    public static final int MASK_CPU_FAST = 0b10000000;

    public static final HashMap<Integer, String> VALUES_CAPTURE = new HashMap<Integer, String>() {{
        put(0b00000010, "positive");
        put(0b00000000, "negative");
    }};

    public static final HashMap<Integer, String> VALUES_GAIN = new HashMap<Integer, String>() {{
        put(0b00000000, "14.0");   // 140 (gbcam gain:  5.01)
        put(0b00000001, "15.5");   // 155
        put(0b00000010, "17.0");   // 170
        put(0b00000011, "18.5");   // 185
        put(0b00000100, "20.0");   // 200 (gbcam gain: 10.00)
        put(0b00010000, "20.0 (d)");   // 200Dup
        put(0b00000101, "21.5");   // 215
        put(0b00010001, "21.5 (d)");   // 215Dup
        put(0b00000110, "23.0");   // 230
        put(0b00010010, "23.0 (d)");   // 230Dup
        put(0b00000111, "24.5");   // 245
        put(0b00010011, "24.5 (d)");   // 245Dup
        put(0b00001000, "26.0");   // 260 (gbcam gain: 19.95)
        put(0b00010100, "26.0 (d)");   // 260Dup
        put(0b00010101, "27.5");   // 275
        put(0b00001001, "29.0");   // 290
        put(0b00010110, "29.0 (d)");   // 290Dup
        put(0b00010111, "30.5");   // 305
        put(0b00001010, "32.0");   // 320 (gbcam gain: 39.81)
        put(0b00011000, "32.0 (d)");   // 320Dup
        put(0b00001011, "35.0");   // 350
        put(0b00011001, "35.0 (d)");   // 350Dup
        put(0b00001100, "38.0");   // 380
        put(0b00011010, "38.0 (d)");   // 380Dup
        put(0b00001101, "41.0");   // 410
        put(0b00011011, "41.0 (d)");   // 410Dup
        put(0b00011100, "44.0");   // 440
        put(0b00001110, "45.5");   // 455
        put(0b00011101, "47.0");   // 470
        put(0b00001111, "51.5");   // 515
        put(0b00011110, "51.5 (d)");   // 515Dup
        put(0b00011111, "57.5");   // 575
    }};

    public static final HashMap<Integer, String> VALUES_EDGE_OP_MODE = new HashMap<Integer, String>() {{
        put(0b00000000, "none");
        put(0b00100000, "horizontal");
        put(0b01000000, "vertical");
        put(0b01100000, "2d");
    }};

    public static final HashMap<Integer, String> VALUES_EDGE_EXCLUSIVE = new HashMap<Integer, String>() {{
        put(0b10000000, "on");
        put(0b00000000, "off");
    }};

    public static final HashMap<Integer, String> VALUES_EDGE_RATIO = new HashMap<Integer, String>() {{
        put(0b00000000, "50%"); // 050
        put(0b00010000, "75%"); // 075
        put(0b00100000, "100%"); // 100
        put(0b00110000, "125%"); // 125
        put(0b01000000, "200%"); // 200
        put(0b01010000, "300%"); // 300
        put(0b01100000, "400%"); // 400
        put(0b01110000, "500%"); // 500
    }};

    public static final HashMap<Integer, String> VALUES_INVERT_OUTPUT = new HashMap<Integer, String>() {{
        put(0b00001000, "Inverted");
        put(0b00000000, "Normal");
    }};

    public static final HashMap<Integer, String> VALUES_VOLTAGE_REF = new HashMap<Integer, String>() {{
        put(0b00000000, "0.0V"); //00v
        put(0b00000001, "0.5V"); //05v
        put(0b00000010, "1.0V"); //10v
        put(0b00000011, "1.5V"); //15v
        put(0b00000100, "2.0V"); //20v
        put(0b00000101, "2.5V"); //25v
        put(0b00000110, "3.0V"); //30v
        put(0b00000111, "3.5V"); //35v
    }};

    public static final HashMap<Integer, String> VALUES_ZERO_POINT = new HashMap<Integer, String>() {{
        put(0b00000000, "disabled");
        put(0b10000000, "positive");
        put(0b01000000, "negative");
    }};


    public static final HashMap<Integer, String> VALUES_VOTAGE_OUT = new HashMap<Integer, String>() {{
        put(0b00011111, "-0.992mV"); // neg992
        put(0b00011110, "-0.960mV"); // neg960
        put(0b00011101, "-0.928mV"); // neg928
        put(0b00011100, "-0.896mV"); // neg896
        put(0b00011011, "-0.864mV"); // neg864
        put(0b00011010, "-0.832mV"); // neg832
        put(0b00011001, "-0.800mV"); // neg800
        put(0b00011000, "-0.768mV"); // neg768
        put(0b00010111, "-0.736mV"); // neg736
        put(0b00010110, "-0.704mV"); // neg704
        put(0b00010101, "-0.672mV"); // neg672
        put(0b00010100, "-0.640mV"); // neg640
        put(0b00010011, "-0.608mV"); // neg608
        put(0b00010010, "-0.576mV"); // neg576
        put(0b00010001, "-0.544mV"); // neg544
        put(0b00010000, "-0.512mV"); // neg512
        put(0b00001111, "-0.480mV"); // neg480
        put(0b00001110, "-0.448mV"); // neg448
        put(0b00001101, "-0.416mV"); // neg416
        put(0b00001100, "-0.384mV"); // neg384
        put(0b00001011, "-0.352mV"); // neg352
        put(0b00001010, "-0.320mV"); // neg320
        put(0b00001001, "-0.288mV"); // neg288
        put(0b00001000, "-0.256mV"); // neg256
        put(0b00000111, "-0.224mV"); // neg224
        put(0b00000110, "-0.192mV"); // neg192
        put(0b00000101, "-0.160mV"); // neg160
        put(0b00000100, "-0.128mV"); // neg128
        put(0b00000011, "-0.096mV"); // neg096
        put(0b00000010, "-0.064mV"); // neg064
        put(0b00000001, "-0.032mV"); // neg032
        put(0b00000000, "-0.000mV"); // neg000
        put(0b00100000, "0.000mV"); // pos000
        put(0b00100001, "0.032mV"); // pos032
        put(0b00100010, "0.064mV"); // pos064
        put(0b00100011, "0.096mV"); // pos096
        put(0b00100100, "0.128mV"); // pos128
        put(0b00100101, "0.160mV"); // pos160
        put(0b00100110, "0.192mV"); // pos192
        put(0b00100111, "0.224mV"); // pos224
        put(0b00101000, "0.256mV"); // pos256
        put(0b00101001, "0.288mV"); // pos288
        put(0b00101010, "0.320mV"); // pos320
        put(0b00101011, "0.352mV"); // pos352
        put(0b00101100, "0.384mV"); // pos384
        put(0b00101101, "0.416mV"); // pos416
        put(0b00101110, "0.448mV"); // pos448
        put(0b00101111, "0.480mV"); // pos480
        put(0b00110000, "0.512mV"); // pos512
        put(0b00110001, "0.544mV"); // pos544
        put(0b00110010, "0.576mV"); // pos576
        put(0b00110011, "0.608mV"); // pos608
        put(0b00110100, "0.640mV"); // pos640
        put(0b00110101, "0.672mV"); // pos672
        put(0b00110110, "0.704mV"); // pos704
        put(0b00110111, "0.736mV"); // pos736
        put(0b00111000, "0.768mV"); // pos768
        put(0b00111001, "0.800mV"); // pos800
        put(0b00111010, "0.832mV"); // pos832
        put(0b00111011, "0.864mV"); // pos864
        put(0b00111100, "0.896mV"); // pos896
        put(0b00111101, "0.928mV"); // pos928
        put(0b00111110, "0.960mV"); // pos960
        put(0b00111111, "0.992mV"); // pos992
    }};

    public static final HashMap<Integer, String> VALUES_DITHER = new HashMap<Integer, String>() {{
        put(0b00000000, "High"); //setHigh
        put(0b00000001, "Low"); //setLow
        put(0x00000000, "On");
        put(0x00000010, "Off");
    }};

    public static final HashMap<Integer, String> VALUES_DITHER_HIGHLIGHT = new HashMap<Integer, String>() {{
        put(0b00010000, "High"); //setHigh
        put(0b00000000, "Low"); //setLow
    }};

    public static final HashMap<Integer, String> VALUES_DITHERING = new HashMap<Integer, String>() {{
        put(0b00000000, "Off");
        put(0b00000001, "Default");
        put(0b00000010, "2x2");
        put(0b00000011, "Grid");
        put(0b00000100, "Maze");
        put(0b00000101, "Nest");
        put(0b00000110, "Fuzz");
        put(0b00000111, "Vertical");
        put(0b00001000, "Horizontal");
        put(0b00001001, "Mix");
    }};

    public static final HashMap<String, Integer> BYTE_OFFSERTS_PXLR = new HashMap<String, Integer>() {{
        put("thumbnailByteCapture", 0x00);
        put("thumbnailByteEdgegains", 0x10);
        put("thumbnailByteExposureHigh", 0x20);
        put("thumbnailByteExposureLow", 0x30);
        put("thumbnailByteEdmovolt", 0xC6);
        put("thumbnailByteVoutzero", 0xD6);
        put("thumbnailByteDitherset", 0xE6);
        put("thumbnailByteContrast", 0xF6);
    }};

    public static final LinkedHashMap<String, Integer> BYTE_OFFSET_PHOTO = new LinkedHashMap<String, Integer>() {{
        put("thumbnailByteCapture", 0xC8);
        put("thumbnailByteEdgegains", 0xC9);
        put("thumbnailByteExposureHigh", 0xCB); // Photo seems to "swap" the bytes only when sending to the sensor
        put("thumbnailByteExposureLow", 0xCA); // So for metadata just flip them back
        put("thumbnailByteEdmovolt", 0xCC);
        put("thumbnailByteVoutzero", 0xCD);
        put("thumbnailByteContrast", 0xDF);
        put("thumbnailByteDithering", 0xEB);

    }};

    public static final HashMap<String, String> SAVE_TYPE_NAMES = new HashMap<String, String>() {{
        put("ORIGINAL", "Original");
        put("PXLR", "PXLR");
        put("PHOTO", "Photo!");

    }};

}


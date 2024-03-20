package com.mraulio.gbcameramanager.utils;


import static com.mraulio.gbcameramanager.utils.HomebrewRomValues.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class HomebrewRomsMetaParser {

    private enum SAVE_TYPE {
        ORIGINAL,
        PXLR,
        PHOTO
    }

    private static SAVE_TYPE saveType;

    private static String getExposureTime(int exposureHigh, int exposureLow, boolean cpuFast) {
        float exposureTimeMultiplierHigh = 0.016f;
        float exposureTimeMultiplierLow = 4.096f;

        //if cpufast (double speed Photo! mode) exposure time is 1/2 of the register
        float timeMs = ((exposureTimeMultiplierHigh * exposureHigh) +
                (exposureTimeMultiplierLow * exposureLow)) * (cpuFast ? 0.5f : 1.0f);

        return timeMs < 10 ? String.format("%.1fms", timeMs) : String.format("%.0fms", Math.floor(timeMs));
    }

    private static String getCaptureMode(int captureMode) {
        String value = VALUES_CAPTURE.get(captureMode);
        return value != null ? value : "unknown";
    }

    private static String getEdgeExclusive(int edgeExclusive) {
        String value = VALUES_EDGE_EXCLUSIVE.get(edgeExclusive);
        return value != null ? value : "unknown";
    }

    private static String getEdgeOpMode(int edgeOperation) {
        String value = VALUES_EDGE_OP_MODE.get(edgeOperation);
        return value != null ? value : "unknown";
    }

    private static String getGain(int gain) {
        String value = VALUES_GAIN.get(gain);
        return value != null ? value : "unknown";
    }

    private static String getEdgeMode(int edgeMode) {
        String value = VALUES_EDGE_RATIO.get(edgeMode);
        return value != null ? value : "unknown";
    }

    private static String getInvertOut(int invertOut) {
        String value = VALUES_INVERT_OUTPUT.get(invertOut);
        return value != null ? value : "unknown";
    }

    private static String getVoltageRef(int vRef) {
        String value = VALUES_VOLTAGE_REF.get(vRef);
        return value != null ? value : "unknown";
    }

    private static String getZeroPoint(int zeroPoint) {
        String value = VALUES_ZERO_POINT.get(zeroPoint);
        return value != null ? value : "unknown";
    }

    private static String getVoltageOut(int vOut) {
        String value = VALUES_VOTAGE_OUT.get(vOut);
        return value != null ? value : "unknown";
    }

    private static String getDitherSetPXLR(int ditherSet) {
        String set = VALUES_DITHER.containsKey(ditherSet & MASK_DITHER_SET) ? VALUES_DITHER.get(ditherSet & MASK_DITHER_SET) : "unknown";
        String onOff = VALUES_DITHER.containsKey(ditherSet & MASK_DITHER_ON_OFF) ? VALUES_DITHER.get(ditherSet & MASK_DITHER_ON_OFF) : "unknown";

        return set + "/" + onOff;
    }

    private static String getDithering(int dithering) {
        String dith = VALUES_DITHERING.containsKey(dithering & MASK_DITHERING) ? VALUES_DITHERING.get(dithering & MASK_DITHERING) : "unknown";
        return dith;
    }

    private static String getDitherHighLight(int ditherHighLight) {
        String highLight = VALUES_DITHER_HIGHLIGHT.containsKey(ditherHighLight & MASK_DITHERING_HIGHLIGHT) ? VALUES_DITHER_HIGHLIGHT.get(ditherHighLight & MASK_DITHERING_HIGHLIGHT) : "unknown";
        return highLight;
    }

    private static boolean getCpuFast(int cpuFast) {
        return (cpuFast & MASK_CPU_FAST) != 0;
    }

    static void checkSaveType(byte[] thumb) {
        int[] whiteLines = {
                0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF,
                0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF,
                0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF,
                0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF
        };

        boolean anyNonFF = false;
        boolean allZeros = true;

        for (int address : whiteLines) {
            if ((thumb[address] & 0xFF) != 0xFF) {
                anyNonFF = true;
            }
            if (thumb[address] != 0x00) {
                allZeros = false;
            }
        }
        if (allZeros) {
            saveType = SAVE_TYPE.ORIGINAL;
        } else if (anyNonFF) {
            saveType = SAVE_TYPE.PHOTO;
        } else {
            saveType = SAVE_TYPE.PXLR;
        }
    }


    public static LinkedHashMap<String, String> parseHomebrewRomMetadata(byte[] thumbnail) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try {
            HashMap<String, Integer> offsets = null;
            checkSaveType(thumbnail);
            boolean cpuFast = false;

            switch (saveType) {
                case ORIGINAL:
                    return new LinkedHashMap<>();
                case PXLR:
                    offsets = BYTE_OFFSERTS_PXLR;
                    break;
                case PHOTO:
                    offsets = BYTE_OFFSET_PHOTO;
                    break;
            }

            int thumbnailByteDitheringHLInvertCpuFast = 0;

            if (saveType == SAVE_TYPE.PHOTO){ // Doing this first to later calculate the exposure time correctly
                thumbnailByteDitheringHLInvertCpuFast = thumbnail[offsets.get("thumbnailByteDithering") & 0xFF];
                cpuFast = getCpuFast(thumbnailByteDitheringHLInvertCpuFast);
            }

            int exposureHigh = thumbnail[offsets.get("thumbnailByteExposureHigh")] & 0xFF;
            int exposureLow = thumbnail[offsets.get("thumbnailByteExposureLow")] & 0xFF;
            int captureMode = thumbnail[offsets.get("thumbnailByteCapture")] & MASK_CAPTURE;
            int edgeExclusive = thumbnail[offsets.get("thumbnailByteEdgegains")] & MASK_EDGE_EXCLUSIVE;
            int edgeOperation = thumbnail[offsets.get("thumbnailByteEdgegains")] & MASK_EDGE_OP_MODE;
            int gain = thumbnail[offsets.get("thumbnailByteEdgegains")] & MASK_GAIN;
            int edgeMode = thumbnail[offsets.get("thumbnailByteEdmovolt")] & MASK_EDGE_RATIO;
            int invertOut = thumbnail[offsets.get("thumbnailByteEdmovolt")] & MASK_INVERT_OUTPUT;
            int vRef = thumbnail[offsets.get("thumbnailByteEdmovolt")] & MASK_VOLTAGE_REF;
            int zeroPoint = thumbnail[offsets.get("thumbnailByteVoutzero")] & MASK_ZERO_POINT;
            int vOut = thumbnail[offsets.get("thumbnailByteVoutzero")] & MASK_VOLTAGE_OUT;

            int contrast = thumbnail[offsets.get("thumbnailByteContrast") & 0xFF];

            result.put("saveType", SAVE_TYPE_NAMES.get(saveType.name()));
            result.put("exposure", getExposureTime(exposureHigh, exposureLow, cpuFast));
            result.put("captureMode", getCaptureMode(captureMode));
            result.put("edgeExclusive", getEdgeExclusive(edgeExclusive));
            result.put("edgeOperation", getEdgeOpMode(edgeOperation));
            result.put("gain", getGain(gain));
            result.put("edgeMode", getEdgeMode(edgeMode));
            result.put("invertOut", getInvertOut(invertOut));
            result.put("voltageRef", getVoltageRef(vRef));
            result.put("zeroPoint", getZeroPoint(zeroPoint));
            result.put("vOut", getVoltageOut(vOut));
            result.put("contrast", String.valueOf(contrast + (saveType == SAVE_TYPE.PHOTO ? 0 : 1)));

            if (saveType == SAVE_TYPE.PHOTO) {
                result.put("dithering", getDithering(thumbnailByteDitheringHLInvertCpuFast));
                result.put("ditheringLight", getDitherHighLight(thumbnailByteDitheringHLInvertCpuFast));
                result.put("cpuFast", String.valueOf(cpuFast));
            } else {
                int ditherSet = thumbnail[offsets.get("thumbnailByteDitherset") & 0xFF];//In PXLR
                result.put("ditherset", getDitherSetPXLR(ditherSet));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

}

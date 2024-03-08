package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.MainActivity.selectedTags;
import static com.mraulio.gbcameramanager.MainActivity.sharedPreferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.Codec;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Class with puclic static variables and methods that are shared alongside the app
 */
public class Utils {
    static File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    static File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


    public static final File MAIN_FOLDER = new File(downloadDirectory, "GBCamera Manager");
    public static final File SAVE_FOLDER = new File(MAIN_FOLDER, "Save dumps");
    public static final File IMAGES_FOLDER = new File(picturesDirectory, "GBCamera Manager");
    public static final File IMAGES_JSON = new File(MAIN_FOLDER, "Images json");
    public static final File TXT_FOLDER = new File(MAIN_FOLDER, "Hex images");
    public static final File PALETTES_FOLDER = new File(MAIN_FOLDER, "Palettes json");
    public static final File FRAMES_FOLDER = new File(MAIN_FOLDER, "Frames json");
    public static final File ARDUINO_HEX_FOLDER = new File(MAIN_FOLDER, "Arduino Printer Hex");
    public static final File PHOTO_DUMPS_FOLDER = new File(MAIN_FOLDER, "PHOTO Rom Dumps");


    public static LinkedHashMap<String, String> frameGroupsNames = new LinkedHashMap<>();

    public static final int[] ROTATION_VALUES = {0, 90, 180, 270};
    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static List<GbcFrame> framesList = new ArrayList<>();
    public static HashMap<String, Bitmap> imageBitmapCache = new HashMap<>();
    public static HashMap<String, GbcFrame> hashFrames = new HashMap<>();
    public static HashMap<String, GbcPalette> hashPalettes = new HashMap<>();

    public static LinkedHashSet<String> tagsHash = new LinkedHashSet<>();

    //Auxiliar method to convert byte[] to hexadecimal String
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] encodeImage(Bitmap bitmap, String paletteId) throws IOException {
        Codec decoder = new ImageCodec(160, bitmap.getHeight(), false);
        return decoder.encodeInternal(bitmap, paletteId);
    }

    public static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));
        }
        return bytes;
    }

    public static void makeDirs() {
        List<File> listFiles = new ArrayList<>();
        listFiles.add(MAIN_FOLDER);
        listFiles.add(SAVE_FOLDER);
        listFiles.add(IMAGES_FOLDER);
        listFiles.add(IMAGES_JSON);
        listFiles.add(TXT_FOLDER);
        listFiles.add(PALETTES_FOLDER);
        listFiles.add(FRAMES_FOLDER);
        listFiles.add(ARDUINO_HEX_FOLDER);
        listFiles.add(PHOTO_DUMPS_FOLDER);

        for (File file : listFiles) {
            try {
                file.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static LinkedHashSet<String> retrieveTags(List<GbcImage> gbcImages) {
        tagsHash.clear();
        for (GbcImage gbcImage : gbcImages) {
            for (String tag : gbcImage.getTags()) {
                tagsHash.add(tag);
            }
        }
        return tagsHash;
    }

    public static void saveTagsSet(List<String> tagsList) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tagsList);
        editor.putString("selected_tags", json);
        editor.apply();
    }

    public static ArrayList<String> getSelectedTags() {
        try {
            ArrayList<String> arrayList;

            if (!selectedTags.isEmpty()) {
                arrayList = new Gson().fromJson(selectedTags, new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                arrayList = new ArrayList<>();
            }
            return arrayList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static AlertDialog loadingDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ProgressBar progressBar = new ProgressBar(context);
        builder.setCancelable(false);

        builder.setView(progressBar);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    public static Bitmap rotateBitmap(Bitmap originalBitmap, GbcImage gbcImage) {
        Matrix matrix = new Matrix();
        matrix.postRotate(ROTATION_VALUES[gbcImage.getRotation()]);
        Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
        return rotatedBitmap;
    }

    public static Bitmap transparentBitmap(Bitmap bitmap, GbcFrame gbcFrame) {
        HashSet<int[]> transparencyHS = null;
        if (gbcFrame.getTransparentPixelPositions().size() == 0) {
            transparencyHS = transparencyHashSet(gbcFrame.getFrameBitmap());
            if (transparencyHS.size() == 0) {
                transparencyHS = generateDefaultTransparentPixelPositions(gbcFrame.getFrameBitmap());
            }
            gbcFrame.setTransparentPixelPositions(transparencyHS);
        } else {
            transparencyHS = gbcFrame.getTransparentPixelPositions();
        }

        int transparentPixel = Color.argb(0, 0, 0, 0);
        for (int[] position : transparencyHS) {
            bitmap.setPixel(position[0], position[1], transparentPixel);
        }
        return bitmap;
    }

    public static HashSet<int[]> transparencyHashSet(Bitmap bitmap) {
        HashSet<int[]> transparentPixelPositions = new HashSet<>();
        // Iterate through the bitmap pixels
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) == 0) {
                    int[] pos = {x, y};
                    transparentPixelPositions.add(pos);
                }
            }
        }
        return transparentPixelPositions;
    }

    public static HashSet<int[]> generateDefaultTransparentPixelPositions(Bitmap bitmap) {
        HashSet<int[]> transparentPixelPositions = new HashSet<>();

        int bitmapHeight = bitmap.getHeight();
        int innerBitmapWidth = 128;
        int innerBitmapHeight = 112;
        int startX = 16;
        int startY = 16;
        if (bitmapHeight == 224) startY = 40;

        for (int y = startY; y < startY + innerBitmapHeight; y++) {
            for (int x = startX; x < startX + innerBitmapWidth; x++) {
                int[] pos = {x, y};
                transparentPixelPositions.add(pos);
            }
        }
        return transparentPixelPositions;
    }

    public static String generateHashFromBytes(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        String hashHex = Utils.bytesToHex(hash);
        return hashHex;
    }


    public static String removeNumbersFromEnd(String input) {
        return input.replaceAll("\\d+$", "");
    }
}


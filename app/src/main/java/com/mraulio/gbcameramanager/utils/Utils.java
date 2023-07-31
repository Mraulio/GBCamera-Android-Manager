package com.mraulio.gbcameramanager.utils;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.Codec;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Class with puclic static variables and methods that are shared alongside the app
 */
public class Utils {
    static File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    static File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


    public static final File MAIN_FOLDER = new File(downloadDirectory, "GBCamera Manager");
    public static final File SAVE_FOLDER = new File(MAIN_FOLDER, "Save dumps");
    public static final File IMAGES_FOLDER = new File(picturesDirectory, "GBCamera Manager");
    public static final File TXT_FOLDER = new File(MAIN_FOLDER, "Hex images");
    public static final File PALETTES_FOLDER = new File(MAIN_FOLDER, "Palettes json");
    public static final File ARDUINO_HEX_FOLDER = new File(MAIN_FOLDER, "Arduino Printer Hex");
    public static final File PHOTO_DUMPS_FOLDER = new File(MAIN_FOLDER, "PHOTO Rom Dumps");


    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static List<GbcFrame> framesList = new ArrayList<>();
    public static HashMap<String, Bitmap> imageBitmapCache = new HashMap<>();
    public static HashMap<String, GbcFrame> hashFrames = new HashMap<>();
    public static HashMap<String, GbcPalette> hashPalettes = new HashMap<>();

    //Auxiliar method to convert byte[] to hexadecimal String
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] encodeImage(Bitmap bitmap) throws IOException {
        Codec decoder = new ImageCodec(new IndexedPalette(hashPalettes.get("bw").getPaletteColorsInt()), 160, bitmap.getHeight());
        return decoder.encodeInternal(bitmap);
    }

    public static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        System.out.println(data.length());
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));
        }
        System.out.println(bytes.length);
        return bytes;
    }

    public static void makeDirs() {
        List<File> listFiles = new ArrayList<>();
        listFiles.add(MAIN_FOLDER);
        listFiles.add(SAVE_FOLDER);
        listFiles.add(IMAGES_FOLDER);
        listFiles.add(TXT_FOLDER);
        listFiles.add(PALETTES_FOLDER);
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

    /**
     * Average HDR method
     *
     * @param bitmaps List of bitmaps for the average
     * @return returns the averaged image
     */
    public static Bitmap combineImages(List<Bitmap> bitmaps) {
        if (bitmaps == null || bitmaps.isEmpty()) {
            throw new IllegalArgumentException("List of images cannot be empty.");
        }

        // Make sure all images have the same dimensions
        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();
        for (Bitmap bitmap : bitmaps) {
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                throw new IllegalArgumentException("All images must have same dimensions.");
            }
        }

        // Create a new Bitmap to store the combined image
        Bitmap combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create an array to store the values of the pixels of all images
        int numImages = bitmaps.size();
        int[][] pixelValues = new int[numImages][width * height];

        // Obtain the values of the pixels of all images
        for (int i = 0; i < numImages; i++) {
            bitmaps.get(i).getPixels(pixelValues[i], 0, width, 0, 0, width, height);
        }

        // Create a new array to store the average values of the combined pixels
        int[] combinedPixels = new int[width * height];

        // Calculate the average of each channel (red,green, blue) for each pixel
        for (int i = 0; i < width * height; i++) {
            int alpha = Color.alpha(pixelValues[0][i]); // Alfa channel is not changed
            int red = 0;
            int green = 0;
            int blue = 0;

            // Adds the value of the pixels for each color channel
            for (int j = 0; j < numImages; j++) {
                red += Color.red(pixelValues[j][i]);
                green += Color.green(pixelValues[j][i]);
                blue += Color.blue(pixelValues[j][i]);
            }

            // Calculates the average value of each color channel
            red /= numImages;
            green /= numImages;
            blue /= numImages;

            // Combines the values of the color channels to form the final pixel
            combinedPixels[i] = Color.argb(alpha, red, green, blue);
        }

        // Sets the combined pixels in the final Bitmap
        combinedBitmap.setPixels(combinedPixels, 0, width, 0, 0, width, height);

        return combinedBitmap;
    }


}
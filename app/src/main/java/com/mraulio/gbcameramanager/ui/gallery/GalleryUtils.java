package com.mraulio.gbcameramanager.ui.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.Deflater;

public class GalleryUtils {
    public static void saveImage(List<GbcImage> gbcImages) {
        LocalDateTime now = LocalDateTime.now();
        String fileNameBase = "gbcImage_";
        String extension = MainActivity.exportPng ? ".png" : ".txt";
        for (int i = 0; i < gbcImages.size(); i++) {
            GbcImage gbcImage = gbcImages.get(i);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            String fileName = fileNameBase + GalleryFragment.dtf.format(now);

            if (gbcImages.size() > 1) {
                fileName += "_" + (i + 1);
            }

            fileName += extension;
            if (MainActivity.exportPng) {
                File file = new File(Utils.IMAGES_FOLDER, fileName);

                if (image.getHeight() == 144 && image.getWidth() == 160 && GalleryFragment.crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                File file = new File(Utils.TXT_FOLDER, fileName);

                //Saving txt without cropping it
                try {
                    //Need to change the palette to bw so the encodeImage method works
                    image = GalleryFragment.paletteChanger("bw", gbcImage.getImageBytes(), GalleryFragment.filteredGbcImages.get(0), false, false, false);
                    StringBuilder txtBuilder = new StringBuilder();
                    //Appending these commands so the export is compatible with
                    // https://herrzatacke.github.io/gb-printer-web/#/import
                    // and https://mofosyne.github.io/arduino-gameboy-printer-emulator/GameBoyPrinterDecoderJS/gameboy_printer_js_decoder.html
                    txtBuilder.append("{\"command\":\"INIT\"}\n" +
                            "{\"command\":\"DATA\",\"compressed\":0,\"more\":1}\n");
                    String txt = Utils.bytesToHex(Utils.encodeImage(image, "bw"));
                    txt = addSpacesAndNewLines(txt).toUpperCase();
                    txtBuilder.append(txt);
                    txtBuilder.append("\n{\"command\":\"DATA\",\"compressed\":0,\"more\":0}\n" +
                            "{\"command\":\"PRNT\",\"sheets\":1,\"margin_upper\":1,\"margin_lower\":3,\"pallet\":228,\"density\":64 }");
                    FileWriter fileWriter = new FileWriter(file);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(txtBuilder.toString());
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (MainActivity.exportPng) {
            Utils.toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved) + MainActivity.exportSize);
        } else Utils.toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved_txt));
    }

    private static String addSpacesAndNewLines(String input) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (i > 0 && i % 32 == 0) {  //Add a new line every 32 chars
                sb.append("\n");
                count = 0;
            } else if (count == 2) {  // Add a space every 2 chars
                sb.append(" ");
                count = 0;
            }
            sb.append(input.charAt(i));
            count++;
        }
        return sb.toString();
    }

    static void shareImage(List<Bitmap> bitmaps, Context context) {
        ArrayList<Uri> imageUris = new ArrayList<>();
        FileOutputStream fileOutputStream = null;

        try {
            for (int i = 0; i < bitmaps.size(); i++) {
                Bitmap bitmap = bitmaps.get(i);

                if ((bitmap.getHeight() / MainActivity.exportSize) == 144 && (bitmap.getWidth() / MainActivity.exportSize) == 160 && GalleryFragment.crop) {
                    bitmap = Bitmap.createBitmap(bitmap, 16 * MainActivity.exportSize, 16 * MainActivity.exportSize, 128 * MainActivity.exportSize, 112 * MainActivity.exportSize);
                }

                File file = new File(context.getExternalCacheDir(), "shared_image_" + i + ".png");
                fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();

                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                imageUris.add(uri);
            }

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/png");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (Exception e) {
            Utils.toast(context, "Exception");
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Average HDR method
     *
     * @param bitmaps List of bitmaps for the average
     * @return returns the averaged image
     */
    public static Bitmap averageImages(List<Bitmap> bitmaps) {
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

    public static Bitmap Paperize(Bitmap inputBitmap) {
        //intensity map for printer head with threshold
        int mul = 20;
        int overlapping = 4;
        Bitmap pixelSampleBitmap;
        pixelSampleBitmap = BitmapFactory.decodeResource(MainActivity.fab.getResources(), R.drawable.pixel_sample);

        int height = inputBitmap.getHeight();
        int width = inputBitmap.getWidth();
        int newWidth = width * 20;
        int newHeight = height * 20;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, inputBitmap.getConfig());


        int[][] streaks = new int[height][width];
        Random random = new Random();
//        for (int i = 0; i < width; i++) {
//            int start = random.nextInt(2); // Generar 0 o 1 aleatoriamente
//            for (int j = 0; j < height; j++) {
//                streaks[j][i] = start;
//                //you can change the streak length here
//                if (random.nextDouble() < 0.2) {
//                    start = random.nextInt(2); // Generar 0 o 1 aleatoriamente si se cumple la condición
//                }
//            }
//        }
        // Tamaño de la región que deseas copiar (20x20)
        int regionSize = 20;

        for (int y = 0; y < inputBitmap.getHeight(); y++) {
            for (int x = 0; x < inputBitmap.getWidth(); x++) {
                int color = inputBitmap.getPixel(x, y);
                int randomRegionX = random.nextInt(50) * regionSize;
                // Calcular la posición correspondiente en el nuevo Bitmap
                int newX = x * regionSize;
                int newY = y * regionSize;

                // Determinar la zona de pixelSampleBitmap según el color
                if (color == Color.parseColor("#FFFFFF")) {
                    // Color blanco (#FFFFFF), no se coge nada de pixelSampleBitmap
                    for (int dy = 0; dy < regionSize; dy++) {
                        for (int dx = 0; dx < regionSize; dx++) {
                            newBitmap.setPixel(newX + dx, newY + dy, Color.WHITE);
                        }
                    }
                } else if (color == Color.parseColor("#AAAAAA")) {
                    // Color aaaaaa, coger la 3a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 2 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                } else if (color == Color.parseColor("#555555")) {
                    // Color 555555, coger la 2a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 1 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                } else if (color == Color.parseColor("#000000")) {
                    // Color 000000, coger la 1a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 0 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                }
            }
        }
        return newBitmap;
    }


    public static String encodeData(String value) {
        byte[] inputBytes = value.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater();
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int length = deflater.deflate(buffer);
            outputStream.write(buffer, 0, length);
        }

        deflater.end();
        byte[] compressedBytes = outputStream.toByteArray();
        return new String(compressedBytes, StandardCharsets.ISO_8859_1);
    }
}

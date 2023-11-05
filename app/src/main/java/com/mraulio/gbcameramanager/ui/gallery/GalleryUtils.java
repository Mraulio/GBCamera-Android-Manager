package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.MainActivity.exportSquare;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

public class GalleryUtils {
    public static void saveImage(List<GbcImage> gbcImages, Context context) {
        LocalDateTime now = null;
        Date nowDate = new Date();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            now = LocalDateTime.now();
        }

        String fileNameBase = "gbcImage_";
        String extension = MainActivity.exportPng ? ".png" : ".txt";
        for (int i = 0; i < gbcImages.size(); i++) {
            GbcImage gbcImage = gbcImages.get(i);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            String fileName = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                fileName = fileNameBase + dtf.format(now);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                fileName = fileNameBase + sdf.format(nowDate);
            }

            if (gbcImages.size() > 1) {
                fileName += "_" + (i + 1);
            }

            fileName += extension;
            if (MainActivity.exportPng) {
                File file = new File(Utils.IMAGES_FOLDER, fileName);

                if (image.getHeight() == 144 && image.getWidth() == 160 && GalleryFragment.crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                //For the wild frames
                else if (image.getHeight() == 224 && GalleryFragment.crop) {
                    image = Bitmap.createBitmap(image, 16, 40, 128, 112);
                }
                //Rotate the image
                image = rotateBitmap(image, gbcImage);

                //Make square if checked in settings
                if (exportSquare) {
                    image = makeSquareImage(image);
                }

                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    mediaScanner(file, context);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                File file = new File(Utils.TXT_FOLDER, fileName);

                //Saving txt without cropping it
                try {
                    //Need to change the palette to bw so the encodeImage method works
                    image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

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
            image.recycle();
        }
        if (MainActivity.exportPng) {
            Utils.toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved) + MainActivity.exportSize);
        } else
            Utils.toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved_txt));
    }

    public static Bitmap makeSquareImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int dimension = Math.max(width, height); // Square side

        Bitmap squaredImage = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squaredImage);

        // Draws original image centered inside the white square
        int left = (dimension - width) / 2;
        int top = (dimension - height) / 2;
        canvas.drawColor(Color.WHITE); // Fills the white backgrond
        canvas.drawBitmap(image, left, top, null); // Draws original image

        return squaredImage;
    }

    public static void mediaScanner(File file, Context context) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
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

    static void shareImage(List<GbcImage> gbcImages, Context context) {
        ArrayList<Uri> imageUris = new ArrayList<>();
        FileOutputStream fileOutputStream = null;

        try {
            for (int i = 0; i < gbcImages.size(); i++) {
                GbcImage gbcImage = gbcImages.get(i);
                Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());

                if (image.getHeight() == 144 && image.getWidth() == 160 && GalleryFragment.crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                //For the wild frames
                else if (image.getHeight() == 224 && GalleryFragment.crop) {
                    image = Bitmap.createBitmap(image, 16, 40, 128, 112);
                }
                //Rotate the image
                image = rotateBitmap(image, gbcImage);

                //Make square if checked in settings
                if (exportSquare) {
                    image = makeSquareImage(image);
                }

                File file = new File(context.getExternalCacheDir(), "shared_image_" + i + ".png");
                fileOutputStream = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
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

    public static Bitmap stitchImages(List<Bitmap> bitmaps, boolean stitchBottom) {
        // Make sure all images have the same dimensions
        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();

        for (Bitmap bitmap : bitmaps) {
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                throw new IllegalArgumentException("All images must have same dimensions.");
            }
        }
        int cropX = 16;
        int cropY = (height == 224) ? 40 : 16;
        int newWidth = stitchBottom ? (width - (cropX * 2)) : (width - (cropX * 2)) * bitmaps.size();
        int newHeight = stitchBottom ? (height - (cropY * 2)) * bitmaps.size() : (height - (cropY * 2));

        Bitmap stitchedImage = Bitmap.createBitmap(newWidth, newHeight, bitmaps.get(0).getConfig());
        Canvas canvas = new Canvas(stitchedImage);
        int destX = 0;
        int destY = 0;

        for (Bitmap bitmap : bitmaps) {
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, 128, 112);

            if (stitchBottom) {
                canvas.drawBitmap(croppedBitmap, 0, destY, null);
                destY += 112;
            } else {
                canvas.drawBitmap(croppedBitmap, destX, 0, null);
                destX += 128;
            }
        }

        return stitchedImage;
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

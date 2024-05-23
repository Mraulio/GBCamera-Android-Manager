package com.mraulio.gbcameramanager.ui.importFile;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.paletteChanger;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.Utils.hashPalettes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class ImageConversionUtils {

    public static Bitmap resizeImage(Bitmap originalBitmap, GbcImage gbcImage) {

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        if (originalWidth == 160 && originalHeight == 144 || originalWidth == 160 && originalHeight % 16 == 0) {//Regular image, 160 width and *16 height
            boolean hasAllColors = checkPaletteColors(originalBitmap);
            if (!hasAllColors) {
                //Check if it only has 4 colors. If it does, convert them to bw palette. Else convert to gray scale and dither
                if (has4Colors(originalBitmap)){
                    originalBitmap = from4toBw(originalBitmap);
                }else{
                    originalBitmap = convertToGrayScale(originalBitmap);
                    originalBitmap = ditherImage(originalBitmap);
                    LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("Type", "Transformed");
                    gbcImage.setImageMetadata(metadata);
                    gbcImage.getTags().add(FILTER_TRANSFORMED);
                }
            }
            return originalBitmap;
        } else {
            float scaledFactor = originalWidth / 160.0f;
            if (originalHeight / scaledFactor == 1.0f || originalHeight % (16 * scaledFactor) == 0) {//The image is a regular image scaled
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) (originalWidth / scaledFactor), (int) (originalHeight / scaledFactor), false);
                boolean hasAllColors = checkPaletteColors(scaledBitmap);
                if (!hasAllColors) {
                    //Check if it only has 4 colors. If it does, convert them to bw palette. Else convert to gray scale and dither
                    if (has4Colors(scaledBitmap)){
                        scaledBitmap = from4toBw(scaledBitmap);
                    }else{
                        scaledBitmap = convertToGrayScale(scaledBitmap);
                        scaledBitmap = ditherImage(scaledBitmap);
                        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                        metadata.put("Type", "Transformed");
                        gbcImage.setImageMetadata(metadata);
                        gbcImage.getTags().add(FILTER_TRANSFORMED);
                    }
                }
                return scaledBitmap;
            } else {

                //For non framed images
                int noFrameWidth = 128;
                int noFrameHeight = 112;
                Bitmap framelessBitmap = null;

                boolean isNonFramed = false;

                if (originalWidth == noFrameWidth && originalHeight == noFrameHeight || originalWidth == 160 && originalHeight % 16 == 0) {//Regular image, 160 width and *16 height
                    framelessBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                    isNonFramed = true;
                } else {
                    float scaledFramelessFactor = originalWidth / 128.0f;

                    if (originalHeight / scaledFramelessFactor == 112) {
                        framelessBitmap = Bitmap.createScaledBitmap(originalBitmap, 128, 112, false);
                        isNonFramed = true;
                    }
                }

                if (isNonFramed) {
                    //Adding a frame to the image so it's 160x144
//                    if (hasJoeyJrPalette(framelessBitmap)) {
//                        framelessBitmap = convertJoeyPalette(framelessBitmap);
//                    }
                    boolean hasAllColors = checkPaletteColors(framelessBitmap);
                    if (!hasAllColors) {
                        //Check if it only has 4 colors. If it does, convert them to bw palette. Else convert to gray scale and dither
                        if (has4Colors(framelessBitmap)){
                            framelessBitmap = from4toBw(framelessBitmap);
                        }else{
                            framelessBitmap = convertToGrayScale(framelessBitmap);
                            framelessBitmap = ditherImage(framelessBitmap);
                            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                            metadata.put("Type", "Transformed");
                            gbcImage.setImageMetadata(metadata);
                            gbcImage.getTags().add(FILTER_TRANSFORMED);
                        }
                    }

                    Bitmap framed = Utils.hashFrames.get(StaticValues.defaultFrameId).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    try {
                        byte[] imageBytes = Utils.encodeImage(framed, "bw");
                        framed = paletteChanger("bw", imageBytes, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Canvas canvas = new Canvas(framed);
                    canvas.drawBitmap(framelessBitmap, 16, Utils.hashFrames.get(StaticValues.defaultFrameId).isWildFrame() ? 40 : 16, null);
                    return framed;

                } else {

                    if (originalBitmap.getHeight() < originalBitmap.getWidth()) {
                        originalBitmap = rotateBitmapImport(originalBitmap, 90);
                    }
                    originalWidth = originalBitmap.getWidth();
                    originalHeight = originalBitmap.getHeight();
                    int targetWidth = 160;

                    // Calculates height adjusted to original image proportions
                    int targetHeight = Math.round((float) originalHeight * targetWidth / originalWidth);

                    // Calculates a factor of 16 height
                    int croppedHeight = (targetHeight / 16) * 16;

                    // Scales the image to the 160 width, keeping the height in proportion
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, croppedHeight, false);
                    boolean hasAllColors = checkPaletteColors(scaledBitmap);

                    if (!hasAllColors) {
                        //Check if it only has 4 colors. If it does, convert them to bw palette. Else convert to gray scale and dither
                        if (has4Colors(scaledBitmap)){
                            scaledBitmap = from4toBw(scaledBitmap);
                        }else{
                            scaledBitmap = convertToGrayScale(scaledBitmap);
                            scaledBitmap = ditherImage(scaledBitmap);
                            LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                            metadata.put("Type", "Transformed");
                            gbcImage.setImageMetadata(metadata);
                            gbcImage.getTags().add(FILTER_TRANSFORMED);
                        }
                    }

                    return scaledBitmap;
                }
            }
        }
    }

//    /**
//     * Method to check if an image was  JoeyJr imported image
//     *
//     * @param image Image to be checked
//     * @return
//     */
//    private static boolean hasJoeyJrPalette(Bitmap image) {
//        //
//        Set<String> originalColors = new HashSet<>(Arrays.asList("#000000", "#808080", "#C0C0C0", "#FFFFFF"));
//        for (int y = 0; y < image.getHeight(); y++) {
//            for (int x = 0; x < image.getWidth(); x++) {
//                int pixelColor = image.getPixel(x, y);
//                String hexColor = String.format("#%06X", (0xFFFFFF & pixelColor));
//                if (!originalColors.contains(hexColor)) {
//                    return false;  // Not a JoeyJr image
//                }
//            }
//        }
//        return true;  // Image has JoeyJr palette
//    }
//
//    /**
//     * Transform JoeyJr image into a valid palette image
//     *
//     * @param image
//     * @return
//     */
//    public static Bitmap convertJoeyPalette(Bitmap image) {
//
//        Bitmap newImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
//
//        for (int y = 0; y < image.getHeight(); y++) {
//            for (int x = 0; x < image.getWidth(); x++) {
//                int pixelColor = image.getPixel(x, y);
//
//                // Convert to hex
//                String hexColor = String.format("#%06X", (0xFFFFFF & pixelColor));
//
//                String newHexColor;
//                if ("#808080".equals(hexColor)) {
//                    newHexColor = "#555555";
//                } else if ("#C0C0C0".equals(hexColor)) {
//                    newHexColor = "#AAAAAA";
//                } else {
//                    newHexColor = hexColor;
//                }
//
//                int newColor = Color.parseColor(newHexColor);
//                newImage.setPixel(x, y, newColor);
//            }
//        }
//
//        return newImage;
//    }

    static boolean has4Colors(Bitmap bitmap) {

        Set<Integer> uniqueColors = new HashSet<>();

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                uniqueColors.add(pixel);
                if (uniqueColors.size() > 4) {
                    return false;
                }
            }
        }
        if (uniqueColors.size() == 4) {
            return true;
        } else return false;
    }

    public static Bitmap from4toBw(Bitmap bitmap) {
        Bitmap b = bitmap.copy(bitmap.getConfig(),true);
        int height = b.getHeight();
        int width = b.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = b.getPixel(x, y);
                int grayValue = (int) (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114);

                int closestColor = findClosestColor(grayValue, hashPalettes.get("bw").getPaletteColorsInt());

                b.setPixel(x, y, closestColor);
            }
        }
        return b;
    }

    public static Bitmap convertToGrayScale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap grayScaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int grayValue = (int) (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114);
                grayScaleBitmap.setPixel(x, y, Color.rgb(grayValue, grayValue, grayValue));
            }
        }


        return grayScaleBitmap;
    }

    private static int findClosestColor(int grayValue, int[] paletteColors) {
        int closestColor = paletteColors[0];
        int minDifference = Math.abs(grayValue - calculateGrayValue(paletteColors[0]));

        for (int color : paletteColors) {
            int difference = Math.abs(grayValue - calculateGrayValue(color));
            if (difference < minDifference) {
                minDifference = difference;
                closestColor = color;
            }
        }

        return closestColor;
    }

    private static int calculateGrayValue(int color) {
        return (int) (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114);
    }

    /**
     * Dithering the image to the bw palette using Bayer Matrix, and enhancing the edges.
     * https://github.com/Raphael-Boichot/PC-to-Game-Boy-Printer-interface/blob/5584b0dacc92ee1b9cae5abe3e51ee02d9aa4cbd/Octave_Interface/image_rectifier.m#L32C1-L69C6
     */
    public static Bitmap ditherImage(Bitmap originalBitmap) {
        int[] Dithering_patterns = {
                0x2A, 0x5E, 0x9B, 0x51, 0x8B, 0xCA, 0x33, 0x69, 0xA6, 0x5A, 0x97, 0xD6, 0x44, 0x7C, 0xBA,
                0x37, 0x6D, 0xAA, 0x4D, 0x87, 0xC6, 0x40, 0x78, 0xB6, 0x30, 0x65, 0xA2, 0x57, 0x93, 0xD2,
                0x2D, 0x61, 0x9E, 0x54, 0x8F, 0xCE, 0x4A, 0x84, 0xC2, 0x3D, 0x74, 0xB2, 0x47, 0x80, 0xBE,
                0x3A, 0x71, 0xAE
        };
        int counter = 0;
        int[][] Bayer_matDG_B = new int[4][4];
        int[][] Bayer_matLG_DG = new int[4][4];
        int[][] Bayer_matW_LG = new int[4][4];

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                Bayer_matDG_B[y][x] = Dithering_patterns[counter];
                counter++;
                Bayer_matLG_DG[y][x] = Dithering_patterns[counter];
                counter++;
                Bayer_matW_LG[y][x] = Dithering_patterns[counter];
                counter++;
            }
        }

        int blockHeight = 4;
        int blockWidth = 4;

        int bitmapHeight = originalBitmap.getHeight();
        int bitmapWidth = originalBitmap.getWidth();

        originalBitmap = enhanceEdges(originalBitmap);

        int[][] Bayer_matDG_B_2D = new int[bitmapHeight][bitmapWidth];
        int[][] Bayer_matLG_DG_2D = new int[bitmapHeight][bitmapWidth];
        int[][] Bayer_matW_LG_2D = new int[bitmapHeight][bitmapWidth];

        for (int y = 0; y < bitmapHeight; y += blockHeight) {
            for (int x = 0; x < bitmapWidth; x += blockWidth) {
                for (int blockY = 0; blockY < blockHeight; blockY++) {
                    for (int blockX = 0; blockX < blockWidth; blockX++) {
                        int offsetX = x + blockX;
                        int offsetY = y + blockY;
                        Bayer_matDG_B_2D[offsetY][offsetX] = Bayer_matDG_B[blockY][blockX];
                        Bayer_matLG_DG_2D[offsetY][offsetX] = Bayer_matLG_DG[blockY][blockX];
                        Bayer_matW_LG_2D[offsetY][offsetX] = Bayer_matW_LG[blockY][blockX];
                    }
                }
            }
        }
        for (int y = 0; y < bitmapHeight; y++) {
            for (int x = 0; x < bitmapWidth; x++) {
                int pixel = Color.red(originalBitmap.getPixel(x, y)); // Get the grayscale value

                int pixel_out;
                if (pixel < Bayer_matDG_B_2D[y][x]) {
                    pixel_out = 0;
                } else if (pixel < Bayer_matLG_DG_2D[y][x]) {
                    pixel_out = 85;
                } else if (pixel < Bayer_matW_LG_2D[y][x]) {
                    pixel_out = 170;
                } else {
                    pixel_out = 255;
                }
                originalBitmap.setPixel(x, y, Color.rgb(pixel_out, pixel_out, pixel_out));
            }
        }
        return originalBitmap;
    }

    public static Bitmap enhanceEdges(Bitmap a) {
        int width = a.getWidth();
        int height = a.getHeight();
        double alpha = 0.2;//0 for no enhancement, best up to 0.5
        Bitmap edge = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    edge.setPixel(x, y, a.getPixel(x, y));  //Keep border original pixel
                } else {
                    int pixel = a.getPixel(x, y);
                    int pixelAbove = a.getPixel(x, y - 1);
                    int pixelBelow = a.getPixel(x, y + 1);
                    int pixelLeft = a.getPixel(x - 1, y);
                    int pixelRight = a.getPixel(x + 1, y);

                    //Color.red, blue and green are the same because it's greyscale
                    int newPixelValue = (int) (Color.red(pixel) + (
                            ((Color.red(pixel) - Color.red(pixelAbove)) +
                                    (Color.red(pixel) - Color.red(pixelBelow)) +
                                    (Color.red(pixel) - Color.red(pixelLeft)) +
                                    (Color.red(pixel) - Color.red(pixelRight))) * alpha
                    ));

                    newPixelValue = Math.min(255, Math.max(0, newPixelValue));

                    edge.setPixel(x, y, Color.rgb(newPixelValue, newPixelValue, newPixelValue));
                }
            }
        }
        return edge;
    }

    public static boolean containsColor(int[] colors, int targetColor) {
        for (int color : colors) {
            if (color == targetColor) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkPaletteColors(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        boolean hasAllColors = true;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = bitmap.getPixel(x, y);
                //Checking with the "bw" palette
                if (!containsColor(hashPalettes.get("bw").getPaletteColorsInt(), pixelColor) && Color.alpha(pixelColor) != 0) {
                    hasAllColors = false;
                    break;
                }
            }
            if (!hasAllColors) {
                break;
            }
        }
        return hasAllColors;
    }

    public static Bitmap rotateBitmapImport(Bitmap originalBitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
        return rotatedBitmap;
    }

}

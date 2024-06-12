package com.mraulio.gbcameramanager.ui.importFile;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColorQuantizer {

    public static class RGB {
        int r, g, b;

        public RGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int toInt() {
            return Color.rgb(r, g, b);
        }
    }

    public static class Palette {
        int k;
        RGB[] colors;

        public Palette(int k) {
            this.k = k;
            this.colors = new RGB[k];
            for (int i = 0; i < k; i++) {
                this.colors[i] = new RGB(0, 0, 0);
            }
        }
    }

    public static int[] kMeansColorReduction(Bitmap bitmap, int k) {
        Bitmap scaledBitmap = getScaledBitmap(bitmap, 500, 500);
        int width = scaledBitmap.getWidth();
        int height = scaledBitmap.getHeight();
        List<RGB> image = new ArrayList<>();

        // Populate image array with RGB values from the bitmap
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = scaledBitmap.getPixel(x, y);
                image.add(new RGB(Color.red(pixel), Color.green(pixel), Color.blue(pixel)));
            }
        }

        Palette palette = new Palette(k);
        Random random = new Random();
        // Initialize centroids randomly
        for (int i = 0; i < k; i++) {
            int index = random.nextInt(image.size());
            palette.colors[i] = image.get(index);
        }

        int[] labels = new int[image.size()];
        int[] counts = new int[k];
        RGB[] sums = new RGB[k];
        for (int i = 0; i < k; i++) {
            sums[i] = new RGB(0, 0, 0);
        }

        for (int iter = 0; iter < 10; iter++) {
            // Assign pixels to nearest centroid
            for (int i = 0; i < image.size(); i++) {
                RGB pixel = image.get(i);
                double minDist = Double.MAX_VALUE;
                int label = 0;
                for (int j = 0; j < k; j++) {
                    double dist = colorDistance(pixel, palette.colors[j]);
                    if (dist < minDist) {
                        minDist = dist;
                        label = j;
                    }
                }
                labels[i] = label;
            }

            // Reset counts and sums
            for (int i = 0; i < k; i++) {
                counts[i] = 0;
                sums[i].r = 0;
                sums[i].g = 0;
                sums[i].b = 0;
            }

            // Accumulate counts and sums
            for (int i = 0; i < image.size(); i++) {
                int label = labels[i];
                RGB pixel = image.get(i);
                sums[label].r += pixel.r;
                sums[label].g += pixel.g;
                sums[label].b += pixel.b;
                counts[label]++;
            }

            // Recompute centroids
            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    palette.colors[i].r = sums[i].r / counts[i];
                    palette.colors[i].g = sums[i].g / counts[i];
                    palette.colors[i].b = sums[i].b / counts[i];
                }
            }
        }

        // Convert centroids to an array of color ints
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = palette.colors[i].toInt();
        }
        return result;
    }

    private static double colorDistance(RGB c1, RGB c2) {
        return Math.pow(c1.r - c2.r, 2) + Math.pow(c1.g - c2.g, 2) + Math.pow(c1.b - c2.b, 2);
    }

    private static Bitmap getScaledBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxWidth || height > maxHeight) {
            float scaleWidth = ((float) maxWidth) / width;
            float scaleHeight = ((float) maxHeight) / height;
            float scaleFactor = Math.min(scaleWidth, scaleHeight);

            int scaledWidth = Math.round(width * scaleFactor);
            int scaledHeight = Math.round(height * scaleFactor);

            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        } else return bitmap;
    }
}

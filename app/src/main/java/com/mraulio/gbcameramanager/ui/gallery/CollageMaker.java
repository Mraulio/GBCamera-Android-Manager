package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class CollageMaker {

    public static Bitmap createCollage(List<Bitmap> bitmaps, int value, boolean crop, boolean horizontalOrientation, boolean halfFrame, int extraPaddingMultiplier, int paddingColor) {

        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();

        for (Bitmap bitmap : bitmaps) {
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                throw new IllegalArgumentException("Todas las imágenes deben tener las mismas dimensiones.");
            }
        }

        boolean verticalImage = false;

        //Check if the images are rotated
        if (height == 160 && (width == 144) || width == 224) {
            verticalImage = true;
        }

        int totalImages = bitmaps.size();
        if (totalImages == 0) {
            return null;
        }

        if (crop) {
            int cropX;
            int cropY;
            if (!verticalImage) {
                cropX = 16;
                cropY = (height == 224) ? 40 : 16;
            } else {
                cropX = (height == 224) ? 40 : 16;
                cropY = 16;
            }
            List<Bitmap> croppedBitmaps = new ArrayList<>();
            for (Bitmap bt : bitmaps) {
                Bitmap croppedBitmap = Bitmap.createBitmap(bt, cropX, cropY, verticalImage ? 112 : 128, verticalImage ? 128 : 112);
                croppedBitmaps.add(croppedBitmap);
            }
            bitmaps = croppedBitmaps;
        }

        int rows = 0;
        int columns = 0;

        if (!horizontalOrientation) {
            columns = value;
            rows = Math.max(1, (int) Math.ceil((double) totalImages / columns));
        } else {
            rows = value;
            columns = Math.max(1, (int) Math.ceil((double) totalImages / rows));
        }

        int collageWidth = bitmaps.get(0).getWidth() * columns;
        int collageHeight = bitmaps.get(0).getHeight() * rows;

        if (halfFrame && !crop) {
            collageWidth -= (columns - 1) * 16;//Substracting 8 px for each frame that is cropped
            collageHeight -= (rows - 1) * 16;//Substracting 8 px for each frame that is cropped
        }

        Bitmap collageBitmap = Bitmap.createBitmap(collageWidth, collageHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(collageBitmap);
        canvas.drawColor(paddingColor);

        Paint paint = new Paint();

        int bitmapIndex = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (bitmapIndex < totalImages) {
                    Bitmap bitmap = bitmaps.get(bitmapIndex);

                    // Calculate horizontal offset
                    int horizontalOffset = 0;
                    if (halfFrame && !crop && col != 0) {
                        horizontalOffset = -8 * (col - 1);
                        bitmap = Bitmap.createBitmap(bitmap, 8, 0, verticalImage ? 136 : 152, verticalImage ? 160 : 144);//Crop 50% of the left frame
                    }

                    // Calculate vertical offset
                    int verticalOffset = 0;
                    if (halfFrame && !crop && row != 0) {
                        verticalOffset = -8 * (row - 1);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 8, bitmap.getWidth(), verticalImage ? 152 : 136);//Crop 50% of the top frame
                    }
                    Matrix matrix = new Matrix();
                    matrix.setTranslate((col * bitmap.getWidth()) + horizontalOffset, (row * bitmap.getHeight()) + verticalOffset);
                    canvas.drawBitmap(bitmap, matrix, paint);
                    bitmapIndex++;
                } else {
                    break;
                }
            }
        }

        if (extraPaddingMultiplier > 0) {
            collageBitmap = addPadding(collageBitmap, extraPaddingMultiplier, paddingColor);
        }

        return collageBitmap;
    }

    public static Bitmap addPadding(Bitmap originalBitmap, int paddingMult, int paddingColor) {

        int paddingSize = paddingMult * 8;

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap paddedBitmap = Bitmap.createBitmap(width + paddingSize * 2, height + paddingSize * 2, originalBitmap.getConfig());

        Canvas canvas = new Canvas(paddedBitmap);

        canvas.drawColor(paddingColor);

        canvas.drawBitmap(originalBitmap, paddingSize, paddingSize, null);

        return paddedBitmap;

    }

    public static void applyBorderToIV(ImageView imageView, int colorBackground) {

        GradientDrawable borderDrawable = new GradientDrawable();

        borderDrawable.setColor(colorBackground);

        borderDrawable.setCornerRadius(10);

        borderDrawable.setStroke(2, Color.parseColor("#000000"));

        imageView.setBackground(borderDrawable);
    }
}

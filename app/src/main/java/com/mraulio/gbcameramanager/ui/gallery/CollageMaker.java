package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class CollageMaker {

    public static Bitmap createCollage(List<Bitmap> bitmaps, int value, boolean crop, boolean horizontalOrientation, boolean halfFrame) {

        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();

        for (Bitmap bitmap : bitmaps) {
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                throw new IllegalArgumentException("Todas las imágenes deben tener las mismas dimensiones.");
            }
        }

        int totalImages = bitmaps.size();
        if (totalImages == 0) {
            return null;
        }


        if (crop) {
            int cropX = 16;
            int cropY = (height == 224) ? 40 : 16;
            List<Bitmap> croppedBitmaps = new ArrayList<>();
            for (Bitmap bt : bitmaps) {
                Bitmap croppedBitmap = Bitmap.createBitmap(bt, cropX, cropY, 128, 112);
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
        canvas.drawColor(0xFFFFFFFF);

        Paint paint = new Paint();

        int bitmapIndex = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (bitmapIndex < totalImages) {
                    Bitmap bitmap = bitmaps.get(bitmapIndex);

                    // Calcular el desplazamiento horizontal
                    int horizontalOffset = 0;
                    if (halfFrame && !crop && col != 0) {
                        horizontalOffset = -8 * (col-1); // Si no es la primera columna, ajustar el desplazamiento horizontal
                        bitmap = Bitmap.createBitmap(bitmap, 8, 0, 152, 144);//Crop 50% of the left frame
                    }

                    // Calcular el desplazamiento vertical
                    int verticalOffset = 0;
                    if (halfFrame && !crop && row != 0) {
                        verticalOffset = -8 * (row-1); // Si no es la primera fila, ajustar el desplazamiento vertical
                        bitmap = Bitmap.createBitmap(bitmap, 0, 8, bitmap.getWidth(), 136);//Crop 50% of the top frame
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

        return collageBitmap;
    }
}

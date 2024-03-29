package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class CollageMaker {

        public static Bitmap createCollage(List<Bitmap> bitmaps, int value, boolean crop, boolean horizontalOrientation) {

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
                for (Bitmap bt: bitmaps){
                    Bitmap croppedBitmap = Bitmap.createBitmap(bt, cropX, cropY, 128, 112);
                    croppedBitmaps.add(croppedBitmap);
                }
                bitmaps = croppedBitmaps;

            }

            int rows = 0;
            int columns = 0;

            // Ajustamos el número de filas o columnas según el valor pasado y la orientación horizontal
            if (!horizontalOrientation) {
                columns = value;
                rows = Math.max(1, (int) Math.ceil((double) totalImages / columns));
            } else {
                rows = value;
                columns = Math.max(1, (int) Math.ceil((double) totalImages / rows));
            }

            // Calculamos el ancho y alto del collage
            int collageWidth = bitmaps.get(0).getWidth() * columns;
            int collageHeight = bitmaps.get(0).getHeight() * rows;

            Bitmap collageBitmap = Bitmap.createBitmap(collageWidth, collageHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(collageBitmap);
            canvas.drawColor(0xFFFFFFFF); // Rellenar el fondo con color blanco

            Paint paint = new Paint();

            int bitmapIndex = 0;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    if (bitmapIndex < totalImages) {
                        Bitmap bitmap = bitmaps.get(bitmapIndex);
                        Matrix matrix = new Matrix();
                        matrix.setTranslate(col * bitmap.getWidth(), row * bitmap.getHeight());
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

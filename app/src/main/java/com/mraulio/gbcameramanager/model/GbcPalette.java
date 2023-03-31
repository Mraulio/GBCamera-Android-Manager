package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GbcPalette {

    private int[] paletteColors;
    private String name;

    public GbcPalette() {
    }

    public GbcPalette(int[] paletteColors, String name) {
        this.paletteColors = paletteColors;
        this.name = name;
    }

    public int[] getPaletteColors() {
        return paletteColors;
    }

    public void setPaletteColors(int[] paletteColors) {
        this.paletteColors = paletteColors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap paletteViewer() {
        int[] colors = this.getPaletteColors();
        // Divide el ancho del ImageView por cuatro para obtener el ancho de cada sección
        int widthHeigh = 300;
        int sectionWidth = widthHeigh / 4;

        // Crea un objeto Bitmap con el tamaño del ImageView y formato ARGB_8888
        Bitmap bitmap = Bitmap.createBitmap(widthHeigh, widthHeigh, Bitmap.Config.ARGB_8888);

        // Obtén el objeto Canvas del bitmap para poder dibujar en él
        Canvas canvas = new Canvas(bitmap);


// Dibuja un rectángulo para cada sección del ImageView y establece el color correspondiente del array
        for (int i = 0; i < colors.length; i++) {
            Rect rect = new Rect(i * sectionWidth, 0, (i + 1) * sectionWidth, widthHeigh);
            Paint paint = new Paint();
            paint.setColor(colors[i]);
            canvas.drawRect(rect, paint);
        }
        return bitmap;

    }

    public int getIndex(int rgb) {
        for (int i = 0; i < paletteColors.length; ++i) {
            if (paletteColors[i] == rgb) {
                return i;
            }
        }
        throw new IllegalArgumentException("Specified RGB colour does not exist in the indexed palette");
    }
}

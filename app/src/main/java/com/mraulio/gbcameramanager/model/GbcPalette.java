package com.mraulio.gbcameramanager.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class GbcPalette {
    @PrimaryKey
    @NonNull
    private String paletteId;

    @ColumnInfo(name = "palette_colors")
    private String paletteColors;

    public GbcPalette() {
    }

    public void setPaletteColors(String paletteColors) {
        this.paletteColors = paletteColors;
    }
    public String getPaletteColors() {
        return this.paletteColors;
    }
    public GbcPalette(String paletteColors, String name) {
        this.paletteColors = paletteColors;
        this.paletteId = name;
    }

    //Because the database can't store int[]
    public int[] getPaletteColorsInt() {
        if (paletteColors == null) {
            return null;
        }
        String[] colors = paletteColors.split(",");
        int[] intColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            intColors[i] = Integer.parseInt(colors[i]);
        }
        return intColors;
    }

    public void setPaletteColors(int[] colors) {
        StringBuilder builder = new StringBuilder();
        for (int color : colors) {
            builder.append(color).append(",");
        }
        paletteColors = builder.toString();
    }

    public String getPaletteId() {
        return paletteId;
    }

    public void setPaletteId(String paletteId) {
        this.paletteId = paletteId;
    }

    public Bitmap paletteViewer() {
        int[] colors = this.getPaletteColorsInt();
        // Divide el ancho del ImageView por cuatro para obtener el ancho de cada sección
        int widthHeigh = 100;
        int sectionWidth = widthHeigh / 4;

        // Crea un objeto Bitmap con el tamaño del ImageView y formato ARGB_8888
        Bitmap bitmap = Bitmap.createBitmap(widthHeigh, widthHeigh/2, Bitmap.Config.ARGB_8888);

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
}

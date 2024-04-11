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

    @ColumnInfo(name = "palette_name")
    private String paletteName;

    @ColumnInfo(name = "palette_colors")
    private String paletteColors;

    @ColumnInfo(name = "is_favorite", defaultValue = "false")
    private boolean isFavorite;

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
        this.isFavorite = false;
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

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getPaletteId() {
        return paletteId;
    }

    public void setPaletteId(String paletteId) {
        this.paletteId = paletteId;
    }

    public String getPaletteName() {
        return paletteName;
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }

    public Bitmap paletteViewer() {
        int[] colors = this.getPaletteColorsInt();
        int widthHeigh = 100;
        int sectionWidth = widthHeigh / 4;

        Bitmap bitmap = Bitmap.createBitmap(widthHeigh, widthHeigh/2, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);


        for (int i = 0; i < colors.length; i++) {
            Rect rect = new Rect(i * sectionWidth, 0, (i + 1) * sectionWidth, widthHeigh);
            Paint paint = new Paint();
            paint.setColor(colors[i]);
            canvas.drawRect(rect, paint);
        }
        return bitmap;
    }
}

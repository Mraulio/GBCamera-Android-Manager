/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mraulio.gbcameramanager.gameboycameralib.constants;


/**
 *
 * @author Raúl Miras Vidal
 */
import android.graphics.Color;

/**
 * Indexed palette representation with default choices for Gameboy colour conversion
 */
public class IndexedPalette {

    /**
     * Colours that are an approximation of the original Gameboy LCD green
     */
    public static final int[] GAMEBOY_LCD_PALETTE = {
            Color.rgb(155, 188, 15),
            Color.rgb(139, 172, 15),
            Color.rgb(48, 98, 48),
            Color.rgb(15, 56, 15)
    };

    /**
     * Colours evenly distributed across RGB range
     */
    public static final int[] EVEN_DIST_PALETTE = {
            Color.rgb(255, 255, 255),
            Color.rgb(170, 170, 170),
            Color.rgb(85, 85, 85),
            Color.rgb(0, 0, 0)
    };

    /**
     * Colours (assumed) evenly distributed across "intensity" (luminance?) range, taken from the GB_CAMERA_DUMP app
     */
    public static final int[] GB_CAMERA_DUMP_PALETTE = {
            Color.rgb(255, 255, 255),
            Color.rgb(192, 192, 192),
            Color.rgb(128, 128, 128),
            Color.rgb(0, 0, 0)
    };

    private int[] palette;

//    public IndexedPalette() {
//        this(EVEN_DIST_PALETTE);
//    }

    public IndexedPalette(int[] palette) {
        this.palette = palette;
    }

    public int[] getPalette() {
        return this.palette;
    }

    public int getRGB(int index) {
        return palette[index];
    }

    public int getIndex(int rgb) {
        for (int i=0; i<palette.length;++i) {
            if (palette[i] == rgb) {
                return i;
            }
        }
        throw new IllegalArgumentException("Specified RGB colour does not exist in the indexed palette");
    }
}

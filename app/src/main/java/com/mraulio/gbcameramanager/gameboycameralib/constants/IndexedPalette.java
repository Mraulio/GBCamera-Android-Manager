/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mraulio.gbcameramanager.gameboycameralib.constants;

import android.graphics.Color;

/**
 * Modified from https://github.com/KodeMunkie/gameboycameralib
 * Indexed palette representation with default choices for Gameboy colour conversion
 */
public class IndexedPalette {

    /**
     * Colours evenly distributed across RGB range
     */
    public static final int[] EVEN_DIST_PALETTE = {
            Color.rgb(255, 255, 255),
            Color.rgb(170, 170, 170),
            Color.rgb(85, 85, 85),
            Color.rgb(0, 0, 0)
    };

    private int[] palette;

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

        int alpha = (rgb >> 24) & 0xFF;
        if (alpha == 0) {
            return 0;
        }
        for (int i = 0; i < palette.length; ++i) {
            if (palette[i] == rgb) {
                return i;
            }
        }
        throw new IllegalArgumentException("Specified RGB colour does not exist in the indexed palette\nRGB: " + rgb
        +"\nIP: " + palette[0] + "" + palette[1] + "" + palette[2] + "" + palette[3]);
    }
}

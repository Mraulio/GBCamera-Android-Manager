package com.mraulio.gbcameramanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.Arrays;
import java.util.Locale;
public class StartCreation {




    public static void addFrames(Context context) {
        int width = 160;
        int height = 144;
        int[] pixels = new int[width * height];

        //Nintendo frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nintendo_frame);
        GbcFrame nintendoframe = new GbcFrame();
        nintendoframe.setFrameName("Nintendo_Frame");
        nintendoframe.setFrameBitmap(bitmap);
        Methods.hashFrames.put(nintendoframe.getFrameName(),nintendoframe);
        Methods.framesList.add(nintendoframe);

//Nintendo frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
//        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.gbcamera_manager_frame);
//        GbcFrame myframe = new GbcFrame();
//        myframe.setFrameName("GBCManager Frame");
//        myframe.setFrameBitmap(bitmap);
//        Methods.framesList.add(myframe);

        Arrays.fill(pixels, Color.BLACK);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame blackFrame = new GbcFrame();
        blackFrame.setFrameName("Black_Frame");
        blackFrame.setFrameBitmap(bitmap);
        Methods.framesList.add(blackFrame);
        Methods.hashFrames.put(blackFrame.getFrameName(),blackFrame);


        //White frame
        Arrays.fill(pixels, Color.WHITE);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame whiteFrame = new GbcFrame();
        whiteFrame.setFrameName("White_frame");
        whiteFrame.setFrameBitmap(bitmap);
        Methods.framesList.add(whiteFrame);
        Methods.hashFrames.put(whiteFrame.getFrameName(),whiteFrame);

    }

    public static void addPalettes() {
        //Palette GAMEBOY_LCD_PALETTE
        int[] GAMEBOY_LCD_PALETTE = {
                Color.rgb(155, 188, 15),
                Color.rgb(139, 172, 15),
                Color.rgb(48, 98, 48),
                Color.rgb(15, 56, 15)
        };
        int[] EVEN_DIST_PALETTE = {
                Color.rgb(255, 255, 255),
                Color.rgb(170, 170, 170),
                Color.rgb(85, 85, 85),
                Color.rgb(0, 0, 0)
        };
        GbcPalette gbcPalette1 = new GbcPalette();
        gbcPalette1.setPaletteColors(EVEN_DIST_PALETTE);
        gbcPalette1.setPaletteId("bw".toLowerCase(Locale.ROOT));
        Methods.gbcPalettesList.add(gbcPalette1);
        GbcPalette gbcPalette2 = new GbcPalette();
        gbcPalette2.setPaletteColors(GAMEBOY_LCD_PALETTE);
        gbcPalette2.setPaletteId("DMG".toLowerCase(Locale.ROOT));
        Methods.gbcPalettesList.add(gbcPalette2);

        //Adding palettes from here https://www.npmjs.com/package/gb-palettes
        int[] cmyk_palette = {
                Color.parseColor("#ffff00"),
                Color.parseColor("#0be8fd"),
                Color.parseColor("#fb00fa"),
                Color.parseColor("#373737")
        };
        GbcPalette gbcPalette3 = new GbcPalette();
        gbcPalette3.setPaletteColors(cmyk_palette);
        gbcPalette3.setPaletteId("CMYK".toLowerCase(Locale.ROOT));//Lower case to be compatible with web app
        Methods.gbcPalettesList.add(gbcPalette3);

        int[] tram_palette = {
                Color.parseColor("#f3c677"),
                Color.parseColor("#e64a4e"),
                Color.parseColor("#912978"),
                Color.parseColor("#0c0a3e")
        };
        GbcPalette gbcPalette5 = new GbcPalette();
        gbcPalette5.setPaletteColors(tram_palette);
        gbcPalette5.setPaletteId("tpa".toLowerCase(Locale.ROOT));
        Methods.gbcPalettesList.add(gbcPalette5);

        //My won palettes
        int[] cute_palette = {
                Color.parseColor("#ffc36d"),
                Color.parseColor("#fe6f9b"),
                Color.parseColor("#c64ab3"),
                Color.parseColor("#7b50b9")
        };
        GbcPalette gbcPalette4 = new GbcPalette();
        gbcPalette4.setPaletteColors(cute_palette);
        gbcPalette4.setPaletteId("Cute".toLowerCase(Locale.ROOT));
        Methods.gbcPalettesList.add(gbcPalette4);

        int[] pinko_palette = {
                Color.parseColor("#ffa2f3"),
                Color.parseColor("#ce83c5"),
                Color.parseColor("#8813ce"),
                Color.parseColor("#370853")
        };
        GbcPalette gbcPalette6 = new GbcPalette();
        gbcPalette6.setPaletteColors(pinko_palette);
        gbcPalette6.setPaletteId("pinko".toLowerCase(Locale.ROOT));
        Methods.gbcPalettesList.add(gbcPalette6);
    }
}

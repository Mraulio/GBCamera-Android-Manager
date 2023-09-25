package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.Utils.generateDefaultTransparentPixelPositions;
import static com.mraulio.gbcameramanager.utils.Utils.transparencyHashSet;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcFrame;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
        try {
            nintendoframe.setFrameBytes(Utils.encodeImage(bitmap, "bw"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashSet<int[]> transparencyHS = transparencyHashSet(bitmap);
        if (transparencyHS.size() == 0) {
            transparencyHS = generateDefaultTransparentPixelPositions(bitmap);
        }
        nintendoframe.setTransparentPixelPositions(transparencyHS);
        Utils.hashFrames.put(nintendoframe.getFrameName(),nintendoframe);
        Utils.framesList.add(nintendoframe);

        //Own frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.gbcamera_manager_frame);
        GbcFrame myframe = new GbcFrame();
        myframe.setFrameName("GBCManager_Frame");
        myframe.setFrameBitmap(bitmap);
        try {
            myframe.setFrameBytes(Utils.encodeImage(bitmap, "bw"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        transparencyHS = transparencyHashSet(bitmap);
        if (transparencyHS.size() == 0) {
            transparencyHS = generateDefaultTransparentPixelPositions(bitmap);
        }
        myframe.setTransparentPixelPositions(transparencyHS);
        Utils.hashFrames.put(myframe.getFrameName(),myframe);
        Utils.framesList.add(myframe);

        Arrays.fill(pixels, Color.BLACK);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame blackFrame = new GbcFrame();
        blackFrame.setFrameName("Black_Frame");

        blackFrame.setFrameBitmap(bitmap);
        Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(),true);
        bitmap = transparentBitmap(bitmapCopy, blackFrame);
        blackFrame.setFrameBitmap(bitmap);
        try {
            blackFrame.setFrameBytes(Utils.encodeImage(bitmap, "bw"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.hashFrames.put(blackFrame.getFrameName(),blackFrame);
        Utils.framesList.add(blackFrame);

        //White frame
        Arrays.fill(pixels, Color.WHITE);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame whiteFrame = new GbcFrame();
        whiteFrame.setFrameName("White_frame");

        whiteFrame.setFrameBitmap(bitmap);
        bitmapCopy = bitmap.copy(bitmap.getConfig(),true);
        bitmap = transparentBitmap(bitmapCopy, whiteFrame);
        whiteFrame.setFrameBitmap(bitmap);
        try {
            whiteFrame.setFrameBytes(Utils.encodeImage(bitmap, "bw"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.hashFrames.put(whiteFrame.getFrameName(),whiteFrame);
        Utils.framesList.add(whiteFrame);

    }

}

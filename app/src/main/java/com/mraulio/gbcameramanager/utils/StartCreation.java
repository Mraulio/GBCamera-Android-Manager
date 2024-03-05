package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.Utils.generateDefaultTransparentPixelPositions;
import static com.mraulio.gbcameramanager.utils.Utils.generateHashFromBytes;
import static com.mraulio.gbcameramanager.utils.Utils.transparencyHashSet;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcFrame;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
public class StartCreation {

    public static void addFrames(Context context) {
        int width = 160;
        int height = 144;
        int[] pixels = new int[width * height];

        //Nintendo frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.nintendo_frame);
        GbcFrame nintendoFrame = new GbcFrame();
        nintendoFrame.setFrameName("nintendo_frame");
        nintendoFrame.setFrameId("gbcam01");
        HashMap<String,String> frameGroupNamesHash = new HashMap<>();
        frameGroupNamesHash.put("gbc","Default frames");
        nintendoFrame.setFrameGroupsNames(frameGroupNamesHash);
        nintendoFrame.setFrameBitmap(bitmap);
        try {
            byte[] nintendoFrameBytes =  Utils.encodeImage(bitmap, "bw");
            nintendoFrame.setFrameBytes(nintendoFrameBytes);
            String nintendoFrameHash = generateHashFromBytes(nintendoFrameBytes);
            nintendoFrame.setFrameHash(nintendoFrameHash);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        HashSet<int[]> transparencyHS = transparencyHashSet(bitmap);
        if (transparencyHS.size() == 0) {
            transparencyHS = generateDefaultTransparentPixelPositions(bitmap);
        }
        nintendoFrame.setTransparentPixelPositions(transparencyHS);
        Utils.hashFrames.put(nintendoFrame.getFrameName(),nintendoFrame);
        Utils.framesList.add(nintendoFrame);

        //Own frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.gbcamera_manager_frame);
        GbcFrame myframe = new GbcFrame();
        myframe.setFrameName("gbcmanager_frame");
        myframe.setFrameId("gbcam02");
        myframe.setFrameBitmap(bitmap);
        try {
            byte[] myFrameBytes =  Utils.encodeImage(bitmap, "bw");
            myframe.setFrameBytes(myFrameBytes);
            String myFrameHash = generateHashFromBytes(myFrameBytes);
            myframe.setFrameHash(myFrameHash);

            myframe.setFrameBytes(Utils.encodeImage(bitmap, "bw"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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
        blackFrame.setFrameName("black_frame");
        blackFrame.setFrameId("gbcam03");

        blackFrame.setFrameBitmap(bitmap);
        Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(),true);
        bitmap = transparentBitmap(bitmapCopy, blackFrame);
        blackFrame.setFrameBitmap(bitmap);
        try {
            byte[] blackFrameBytes = Utils.encodeImage(bitmap, "bw");
            blackFrame.setFrameBytes(blackFrameBytes);
            String blackFrameHash = generateHashFromBytes(blackFrameBytes);
            blackFrame.setFrameHash(blackFrameHash);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Utils.hashFrames.put(blackFrame.getFrameName(),blackFrame);
        Utils.framesList.add(blackFrame);

        //White frame
        Arrays.fill(pixels, Color.WHITE);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame whiteFrame = new GbcFrame();
        whiteFrame.setFrameName("white_frame");
        whiteFrame.setFrameId("gbcam04");

        whiteFrame.setFrameBitmap(bitmap);
        bitmapCopy = bitmap.copy(bitmap.getConfig(),true);
        bitmap = transparentBitmap(bitmapCopy, whiteFrame);
        whiteFrame.setFrameBitmap(bitmap);
        try {
            byte[] whiteFrameBytes = Utils.encodeImage(bitmap, "bw");
            whiteFrame.setFrameBytes(whiteFrameBytes);
            String whiteFrameHash = generateHashFromBytes(whiteFrameBytes);
            whiteFrame.setFrameHash(whiteFrameHash);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Utils.hashFrames.put(whiteFrame.getFrameName(),whiteFrame);
        Utils.framesList.add(whiteFrame);

    }

}

package com.mraulio.gbcameramanager;

import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_HEIGHT;
import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_WIDTH;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Methods {

    public static List<Bitmap> completeImageList = new ArrayList<>();
    public static List<Bitmap> imageList = new ArrayList<>();
    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static List<byte[]> listImageBytes = new ArrayList<>();

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
    protected static List<GbcImage> imageList100 = null;

    public static void extractSavImages(Context context) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        LocalDateTime now = LocalDateTime.now();
        File latestFile = null;
        try {
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File savFile = new File(downloadsDirectory + "/gbc.sav");
            //To extract last dumped file
//            File[] files = folder.listFiles();
//            if (files != null && files.length > 0) {
//                Arrays.sort(files, new Comparator<File>() {
//                    public int compare(File f1, File f2) {
//                        return Long.compare(f2.lastModified(), f1.lastModified());
//                    }
//                });
//                latestFile = files[0];
////                tv.append("\nThe name of the last SAV file is: " + latestFile.getName() + ".\n" +
////                        "Size: " + latestFile.length() / 1024 + "KB");
//            }
//            if (savFile.length() / 1024 == 128) {

            //Extract the images
            listImageBytes = extractor.extractBytes(savFile);
//            imageList = extractor.extract(savFile);

            for (byte[] imageBytes : listImageBytes) {
                GbcImage gbcImage = new GbcImage();
                gbcImage.setImageBytes(imageBytes);
//                if (nameIndex%2==0)
//                    gbcImage.setImageBytes(cambiarPaleta(imageBytes,1));
//                else
//                    gbcImage.setBitmap(imageBytes);
                gbcImage.setName("Image " + (GbcImage.numImages));
                gbcImagesList.add(gbcImage);
                ImageCodec imageCodec = new ImageCodec(0, IMAGE_WIDTH, IMAGE_HEIGHT);
                completeImageList.add(imageCodec.decodeWithPalette(0, gbcImage.getImageBytes()));
            }

        } catch (IOException e) {
            Toast toast = Toast.makeText(context, "Error\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();

            e.printStackTrace();
        }
    }


}

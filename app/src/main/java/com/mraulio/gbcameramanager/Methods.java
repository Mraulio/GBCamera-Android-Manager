package com.mraulio.gbcameramanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Methods {

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
//    protected static List<Bitmap> imageList = null;

    public static void extractSavImages(Context context) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
//        imageList = null;
        LocalDateTime now = LocalDateTime.now();
        File latestFile = null;
        try {
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File savFile = new File(downloadsDirectory + "/PHOTO_2023-01-14_17-32-04_Ramen_Gansos.sav");
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
                MainActivity.imageList = extractor.extract(savFile);
//                tv.append("\nThe image list has: " + imageList.size() + " images.");

//            for (int i = 0; i < imageList.size(); i++) {
//                String fileName = "image_";
//                fileName += dtf.format(now) + "_" + i + ".png";
////                        saveImage(imageList.get(i), "/Images_" + dtf.format(now) + "/" + fileName);
//            }
//            if (imageList.size() < itemsPerPage) {
//                itemsPerPage = imageList.size();
//            }
//                ImageAdapter imageAdapter = new ImageAdapter(getApplicationContext(), imageList, imageList.size());
//                gridView.setAdapter(imageAdapter);
//            } else {
////                tv.append("\nNOT A GOOD SAVE DUMP.");
//            }
//            Toast toast = Toast.makeText(context, MainActivity.imageList.size(), Toast.LENGTH_LONG);
//            toast.show();

        } catch (IOException e) {
            Toast toast = Toast.makeText(context, "Error\n"+e.toString(), Toast.LENGTH_LONG);
            toast.show();

            e.printStackTrace();
        }
//        return imageList;
    }


}

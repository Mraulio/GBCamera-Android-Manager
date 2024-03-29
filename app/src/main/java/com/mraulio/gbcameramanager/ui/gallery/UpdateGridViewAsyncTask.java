package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class UpdateGridViewAsyncTask extends AsyncTask<Void, Void, Void> {

    //I could add a isCancelled flag
    @Override
    protected Void doInBackground(Void... voids) {
        //Store the indexes because if I call it again, it will use startIndex with a different value and crash
        int newStartIndex = GalleryFragment.startIndex;
        int newEndIndex = GalleryFragment.endIndex;

        List<String> currentPageHashes = new ArrayList<>();
        ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
        //foreach index
        int index = 0;
        //Loop for each gbcImage in a sublist in all gbcImages objects for the current page
        for (GbcImage gbcImage : GalleryFragment.filteredGbcImages.subList(newStartIndex, newEndIndex)) {
            //Add the hashcode to the list of current hashes
            String imageHash = gbcImage.getHashCode();
            currentPageHashes.add(imageHash);
            byte[] imageBytes;
            Bitmap image = GalleryFragment.diskCache.get(imageHash);
            Utils.imageBitmapCache.put(imageHash, image);

//                Get the image bytes from the database for the current gbcImage
            if (image == null) {
                if (!GalleryFragment.loadingDialog.isShowing())
                    publishProgress();
                imageBytes = imageDataDao.getDataByImageId(imageHash);
                //Set the image bytes to the object
                gbcImage.setImageBytes(imageBytes);
                if (gbcImage.getFramePaletteId()==null){
                    gbcImage.setFramePaletteId("bw");
                }
                //Create the image bitmap
                int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height, gbcImage.isLockFrame());
                GbcFrame gbcFrame = Utils.hashFrames.get(gbcImage.getFrameId());
                if (gbcFrame == null){
                    gbcFrame= Utils.hashFrames.get("Nintendo_Frame");
                }
                image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), Utils.hashPalettes.get(gbcImage.getFramePaletteId()).getPaletteColorsInt(), imageBytes, gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcFrame.isWildFrame());
                //Add the bitmap to the cache
                Utils.imageBitmapCache.put(imageHash, image);
                GalleryFragment.diskCache.put(imageHash, image);
                //Do a frameChange to create the Bitmap of the image
                try {
                    //Only do frameChange if the image is 144 height AND THE FRAME IS NOT EMPTY (AS SET WHEN READING WITH ARDUINO PRINTER EMULATOR)
                    if ((image.getHeight() == 144 || image.getHeight() == 160) && !gbcImage.getFrameId().equals("") /*&& !gbcImage.getFrameId().equals("Nintendo_Frame")*/)
                        image = GalleryFragment.frameChange(gbcImage, gbcImage.getFrameId(),gbcImage.isInvertPalette(),gbcImage.isInvertFramePalette(),gbcImage.isLockFrame(),true);
                    Utils.imageBitmapCache.put(gbcImage.getHashCode(), image);
                    GalleryFragment.diskCache.put(imageHash, image);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            index++;
        }
        GalleryFragment.gbcImagesForPage = GalleryFragment.filteredGbcImages.subList(newStartIndex, newEndIndex);
        //Create a list of bitmaps to use in the adapter, getting the bitmaps from the cache map for each image in the current page
        List<Bitmap> bitmapList = new ArrayList<>();
        for (GbcImage gbcImage : GalleryFragment.gbcImagesForPage) {
            bitmapList.add(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
        }
        GalleryFragment.customGridViewAdapterImage = new CustomGridViewAdapterImage(GalleryFragment.gridView.getContext(), R.layout.row_items, GalleryFragment.filteredGbcImages.subList(newStartIndex, newEndIndex), bitmapList, false, false, true, GalleryFragment.selectedImages);
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        if (!GalleryFragment.loadingDialog.isShowing())
            GalleryFragment.loadingDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        //Notifies the adapter
        GalleryFragment.gridView.setAdapter(GalleryFragment.customGridViewAdapterImage);
        GalleryFragment.loadingDialog.dismiss();
    }
}
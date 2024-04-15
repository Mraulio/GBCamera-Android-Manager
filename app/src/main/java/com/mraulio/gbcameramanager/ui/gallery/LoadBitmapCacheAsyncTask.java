package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.frameChange;

import java.util.List;

import javax.xml.transform.Result;

public class LoadBitmapCacheAsyncTask extends AsyncTask<Void, Void, Result> {
    private List<Integer> indexesToLoad;
    private AsyncTaskCompleteListener<Result> listener;
    private LoadingDialog loadDialog;

    public LoadBitmapCacheAsyncTask(List<Integer> indexesToLoad, LoadingDialog loadDialog, AsyncTaskCompleteListener<Result> listener) {
        this.indexesToLoad = indexesToLoad;
        this.listener = listener;
        this.loadDialog = loadDialog;
    }

    //I could add a isCancelled flag
    @Override
    protected Result doInBackground(Void... voids) {
        ImageDataDao imageDataDao = StaticValues.db.imageDataDao();
        //foreach index
        for (int i : indexesToLoad) {
            GbcImage gbcImage = GalleryFragment.filteredGbcImages.get(i);
            //Add the hashcode to the list of current hashes
            String imageHash = gbcImage.getHashCode();
            byte[] imageBytes;
            Bitmap image = GalleryFragment.diskCache.get(imageHash);
            Utils.imageBitmapCache.put(imageHash, image);

//                Get the image bytes from the database for the current gbcImage
            if (image == null) {
                imageBytes = imageDataDao.getDataByImageId(imageHash);
                //Set the image bytes to the object
                gbcImage.setImageBytes(imageBytes);
                if (gbcImage.getFramePaletteId() == null) {
                    gbcImage.setFramePaletteId("bw");
                }
                //Create the image bitmap
                int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height);
                GbcFrame gbcFrame = Utils.hashFrames.get(gbcImage.getFrameId());

                image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes, gbcImage.isInvertPalette());//Add the bitmap to the cache
                Utils.imageBitmapCache.put(imageHash, image);
                GalleryFragment.diskCache.put(imageHash, image);
                //Do a frameChange to create the Bitmap of the image
                try {
                    //Only do frameChange if the image is 144 height AND THE FRAME IS NOT EMPTY (AS SET WHEN READING WITH ARDUINO PRINTER EMULATOR)
                    if (image.getHeight() == 144 && gbcImage.getFrameId() != null) {
                        image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), true);
                    }
                    Utils.imageBitmapCache.put(gbcImage.getHashCode(), image);
                    GalleryFragment.diskCache.put(imageHash, image);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        //Notifies the adapter
        if (listener != null) {
            listener.onTaskComplete(result); //Notify the finalization from the interface
        }


    }
}
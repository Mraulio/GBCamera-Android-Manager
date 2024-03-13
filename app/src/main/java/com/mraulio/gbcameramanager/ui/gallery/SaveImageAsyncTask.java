package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.TextView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
    List<GbcImage> gbcImagesList;
    List<Bitmap> bitmapList;
    Context context;
    TextView tvFileName;
    int numImagesAdded;

    public SaveImageAsyncTask(List<GbcImage> gbcImagesList, List<Bitmap> bitmapList, Context context, TextView tvFileName, int numImagesAdded) {
        this.gbcImagesList = gbcImagesList;
        this.bitmapList = bitmapList;
        this.context = context;
        this.tvFileName = tvFileName;
        this.numImagesAdded = numImagesAdded;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        List<GbcImage> newGbcImages = new ArrayList<>();
        List<ImageData> newImageDatas = new ArrayList<>();
        for (int i = 0; i < gbcImagesList.size(); i++) {
            GbcImage gbcImage = gbcImagesList.get(i);
            GbcImage.numImages++;
            numImagesAdded++;
            ImageData imageData = new ImageData();
            imageData.setImageId(gbcImage.getHashCode());
            imageData.setData(gbcImage.getImageBytes());
            newImageDatas.add(imageData);
            Utils.gbcImagesList.add(gbcImage);
            Utils.gbcImagesListHolder.add(gbcImage);
            newGbcImages.add(gbcImage);
            Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmapList.get(i));
        }

        ImageDao imageDao = MainActivity.db.imageDao();
        ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
        //Need to insert first the gbcImage because of the Foreign Key
        for (GbcImage gbcImage : gbcImagesList) {
            imageDao.insert(gbcImage);
        }
        for (ImageData imageData : newImageDatas) {
            imageDataDao.insert(imageData);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        tvFileName.setText(numImagesAdded + context.getString(R.string.done_adding_images));
        retrieveTags(gbcImagesList);
        checkSorting();
        Utils.toast(context, context.getString(R.string.images_added) + numImagesAdded);
    }
}


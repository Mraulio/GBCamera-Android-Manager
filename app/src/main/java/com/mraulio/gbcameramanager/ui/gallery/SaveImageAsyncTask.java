package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
    List<GbcImage> gbcImagesListToSave;
    List<Bitmap> bitmapList;
    Context context;
    TextView tvFileName;
    int numImagesAdded;
    CustomGridViewAdapterImage customGridViewAdapterImage;
    LoadingDialog loadDialogSave;

    public SaveImageAsyncTask(List<GbcImage> gbcImagesList, List<Bitmap> bitmapList, Context context, TextView tvFileName,
                              int numImagesAdded, CustomGridViewAdapterImage customGridViewAdapterImage, LoadingDialog loadDialogSave) {
        this.gbcImagesListToSave = gbcImagesList;
        this.bitmapList = bitmapList;
        this.context = context;
        this.tvFileName = tvFileName;
        this.numImagesAdded = numImagesAdded;
        this.customGridViewAdapterImage = customGridViewAdapterImage;
        this.loadDialogSave = loadDialogSave;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        List<GbcImage> newGbcImages = new ArrayList<>();
        List<ImageData> newImageDatas = new ArrayList<>();
        for (int i = 0; i < gbcImagesListToSave.size(); i++) {
            GbcImage gbcImage = gbcImagesListToSave.get(i);
            GbcImage.numImages++;
            numImagesAdded++;
            ImageData imageData = new ImageData();
            imageData.setImageId(gbcImage.getHashCode());
            imageData.setData(gbcImage.getImageBytes());
            newImageDatas.add(imageData);
            Utils.gbcImagesList.add(gbcImage);
            newGbcImages.add(gbcImage);
            Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmapList.get(i));
            GalleryFragment.diskCache.put(gbcImage.getHashCode(), bitmapList.get(i));
        }

        ImageDao imageDao = StaticValues.db.imageDao();
        ImageDataDao imageDataDao = StaticValues.db.imageDataDao();

        //Need to insert first the gbcImage because of the Foreign Key
        imageDao.insertManyImages(newGbcImages);

        imageDataDao.insertManyDatas(newImageDatas);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (tvFileName != null) {
            tvFileName.setText(numImagesAdded + context.getString(R.string.done_adding_images));
        }
        loadDialogSave.dismissDialog();
        retrieveTags(gbcImagesListToSave);
        checkSorting(context);
        GalleryFragment gf = new GalleryFragment();
        gf.updateFromMain(context);
        Utils.toast(context, context.getString(R.string.images_added) + numImagesAdded);
    }
}
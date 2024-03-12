package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.List;


public class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
    List<GbcImage> gbcImagesList;
    List<ImageData> imageDataList;
    Context context;
    TextView tvFileName;
    int numImagesAdded;
    
    public SaveImageAsyncTask(List<GbcImage> gbcImagesList, List<ImageData> imageDataList,Context context, TextView tvFileName, int numImagesAdded) {
        this.gbcImagesList = gbcImagesList;
        this.imageDataList = imageDataList;
        this.context = context;
        this.tvFileName = tvFileName;
        this.numImagesAdded = numImagesAdded;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ImageDao imageDao = MainActivity.db.imageDao();
        ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
        //Need to insert first the gbcImage because of the Foreign Key
        for (GbcImage gbcImage : gbcImagesList) {
            imageDao.insert(gbcImage);
        }
        for (ImageData imageData : imageDataList) {
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


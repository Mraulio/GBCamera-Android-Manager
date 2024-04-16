package com.mraulio.gbcameramanager.ui.gallery;

import android.os.AsyncTask;

import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.StaticValues;

//Method to update an image to the database in the background
public class UpdateImageAsyncTask extends AsyncTask<Void, Void, Void> {
    private GbcImage gbcImage;

    //Stores the image passes as parameter in the constructor
    public UpdateImageAsyncTask(GbcImage gbcImage) {
        this.gbcImage = gbcImage;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ImageDao imageDao = StaticValues.db.imageDao();
        imageDao.update(gbcImage);
        return null;
    }
}
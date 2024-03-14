package com.mraulio.gbcameramanager.ui.frames;

import android.os.AsyncTask;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.model.GbcFrame;

public class UpdateFrameAsyncTask extends AsyncTask<Void, Void, Void> {

    private GbcFrame gbcFrame;

    //Stores the image passes as parameter in the constructor
    public UpdateFrameAsyncTask(GbcFrame gbcFrame) {
        this.gbcFrame = gbcFrame;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        FrameDao frameDao = MainActivity.db.frameDao();
        frameDao.update(gbcFrame);
        return null;
    }
}

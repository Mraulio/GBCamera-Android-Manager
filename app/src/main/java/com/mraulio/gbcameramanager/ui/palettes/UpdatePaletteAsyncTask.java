package com.mraulio.gbcameramanager.ui.palettes;

import android.os.AsyncTask;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.model.GbcPalette;

public class UpdatePaletteAsyncTask extends AsyncTask<Void, Void, Void>  {
    private GbcPalette gbcPalette;

    //Stores the image passes as parameter in the constructor
    public UpdatePaletteAsyncTask(GbcPalette gbcPalette) {
        this.gbcPalette = gbcPalette;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        PaletteDao paletteDao = MainActivity.db.paletteDao();
        paletteDao.update(gbcPalette);
        return null;
    }
}

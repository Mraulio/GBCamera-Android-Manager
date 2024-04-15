package com.mraulio.gbcameramanager.ui.palettes;

import android.os.AsyncTask;

import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.utils.StaticValues;

public class UpdatePaletteAsyncTask extends AsyncTask<Void, Void, Void>  {
    private GbcPalette gbcPalette;

    //Stores the image passes as parameter in the constructor
    public UpdatePaletteAsyncTask(GbcPalette gbcPalette) {
        this.gbcPalette = gbcPalette;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        PaletteDao paletteDao = StaticValues.db.paletteDao();
        paletteDao.update(gbcPalette);
        return null;
    }
}

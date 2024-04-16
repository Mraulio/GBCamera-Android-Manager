package com.mraulio.gbcameramanager.ui.palettes;

import static com.mraulio.gbcameramanager.utils.Utils.sortPalettes;

import android.os.AsyncTask;

import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.utils.StaticValues;

public class SavePaletteAsyncTask extends AsyncTask<Void, Void, Void> {
    //To add the new palette as a parameter
    private GbcPalette gbcPalette;
    private boolean save;

    public SavePaletteAsyncTask(GbcPalette gbcPalette, boolean save) {
        this.gbcPalette = gbcPalette;
        this.save = save;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        PaletteDao paletteDao = StaticValues.db.paletteDao();
        if (save) {
            paletteDao.insert(gbcPalette);
        } else {
            paletteDao.delete(gbcPalette);
        }
        sortPalettes();
        return null;
    }
}

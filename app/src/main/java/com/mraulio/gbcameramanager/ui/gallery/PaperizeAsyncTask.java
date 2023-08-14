package com.mraulio.gbcameramanager.ui.gallery;


import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.crop;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.dtf;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.filteredGbcImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.loadingDialog;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.paletteChanger;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class PaperizeAsyncTask extends AsyncTask<Void, Void, Void> {
    private List<Integer> gbcImagesList;
    private Context context;

    public PaperizeAsyncTask(List<Integer> gbcImagesList, Context context) {
        this.gbcImagesList = gbcImagesList;
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        int index = 1;
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);
        for (int i : gbcImagesList) {
            Bitmap bw_image = paletteChanger("bw", filteredGbcImages.get(i).getImageBytes(), filteredGbcImages.get(i), false, false, filteredGbcImages.get(i).isInvertPalette());
            Bitmap paperized = GalleryUtils.Paperize(bw_image);
            File file = null;
            if (gbcImagesList.size() > 1)
                file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + "_" + (index) + ".png");
            else {
                file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + ".png");
            }

            if (paperized.getHeight() == 144 && paperized.getWidth() == 160 && crop) {
                paperized = Bitmap.createBitmap(paperized, 16, 16, 128, 112);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                Bitmap scaled = Bitmap.createScaledBitmap(paperized, paperized.getWidth(), paperized.getHeight(), false);
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out);

            } catch (IOException e) {
                e.printStackTrace();
            }
            index++;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Utils.toast(context, context.getString(R.string.saved_paperized_toast));
        loadingDialog.dismiss();
    }
}
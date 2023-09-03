package com.mraulio.gbcameramanager.ui.gallery;


import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.filteredGbcImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.loadingDialog;
import static com.mraulio.gbcameramanager.ui.gallery.PaperUtils.paperize;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.mraulio.gbcameramanager.model.GbcImage;

import java.io.IOException;
import java.util.List;

public class PaperizeAsyncTask extends AsyncTask<Void, Void, Void> {
    private List<Integer> gbcImagesList;
    private Context context;
    private int paperColor;
    private ImageView ivPaperized;
    private List<Bitmap> paperizedBitmaps;
    private boolean onlyImage;

    public PaperizeAsyncTask(List<Integer> gbcImagesList, int paperColor, List<Bitmap> paperizedBitmaps,  ImageView ivPaperized,boolean onlyImage, Context context) {
        this.gbcImagesList = gbcImagesList;
        this.paperColor = paperColor;
        this.paperizedBitmaps = paperizedBitmaps;
        this.ivPaperized = ivPaperized;
        this.onlyImage = onlyImage;
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        for (int i : gbcImagesList) {
            GbcImage gbcImage =  filteredGbcImages.get(i);
            Bitmap bw_image= null;
            try {
                bw_image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap paperized = paperize(bw_image, paperColor,onlyImage, context);
            paperizedBitmaps.add(paperized);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        ivPaperized.setImageBitmap(paperizedBitmaps.get(0));
        loadingDialog.dismiss();
    }
}
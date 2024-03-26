package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.NumberPicker;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CloneDialog {
    Context context;
    List<Integer> selectedImages;
    CustomGridViewAdapterImage customGridViewAdapterImage;
    List<GbcImage> filteredGbcImages;

    public CloneDialog(Context context, List<Integer> selectedImages, CustomGridViewAdapterImage customGridViewAdapterImage, List<GbcImage> filteredGbcImages) {
        this.context = context;
        this.selectedImages = selectedImages;
        this.customGridViewAdapterImage = customGridViewAdapterImage;
        this.filteredGbcImages = filteredGbcImages;
    }

    public void createCloneDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.action_clone));

        final NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(100);
        builder.setView(numberPicker);

        builder.setPositiveButton(context.getString(R.string.action_clone), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedNumber = numberPicker.getValue();
                saveClonedImages(selectedNumber);
            }
        });

        builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveClonedImages(int numberOfClones) {
        List<GbcImage> gbcImagesToClone = new ArrayList<>();
        Collections.sort(selectedImages);
        for (int i : selectedImages) {
            gbcImagesToClone.add(filteredGbcImages.get(i));
        }
        List<GbcImage> clonedImages = new ArrayList<>();
        List<Bitmap> clonedBitmaps = new ArrayList<>();
        long timeMs = System.currentTimeMillis();
        for (GbcImage gbcImage : gbcImagesToClone) {
            for (int i = 0; i < numberOfClones; i++) {
                GbcImage clonedImage = gbcImage.clone();
                long timeMsThisClone = timeMs + i;
                String timeString = String.valueOf(timeMsThisClone);
                String lastFiveDigits = timeString.substring(Math.max(0, timeString.length() - 5));
                String phrase = "clone" + lastFiveDigits;
                String name = new String(gbcImage.getName());
                name += "-clone";
                StringBuilder modifiedString = new StringBuilder(gbcImage.getHashCode());
                clonedImage.setName(name);
                try {
                    modifiedString.replace(modifiedString.length() - 10, modifiedString.length(), phrase);
                } catch (Exception e) {
                    e.printStackTrace();
                    modifiedString.append("clonedBadLength" + timeString);
                }
                String clonedHash = modifiedString.toString();
                System.out.println(clonedHash);
                System.out.println("---");
                clonedImage.setHashCode(clonedHash);

                HashSet tags = new HashSet(clonedImage.getTags());
                tags.add("Cloned");
                clonedImage.setTags(tags);
                clonedImages.add(clonedImage);
                Bitmap originalBitmap = imageBitmapCache.get(gbcImage.getHashCode());
                Bitmap clonedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                clonedBitmaps.add(clonedBitmap);
            }
        }
        new SaveImageAsyncTask(clonedImages, clonedBitmaps, context, null, 0, customGridViewAdapterImage).execute();
    }
}

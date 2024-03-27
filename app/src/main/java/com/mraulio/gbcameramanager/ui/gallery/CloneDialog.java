package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.NumberPicker;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.LoadingDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.transform.Result;

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
        List<Integer> indexesToLoad = new ArrayList<>();
        for (int i : selectedImages) {
            gbcImagesToClone.add(filteredGbcImages.get(i));

            String hashCode = filteredGbcImages.get(i).getHashCode();
            if (imageBitmapCache.get(hashCode) == null) {
                indexesToLoad.add(i);
            }
        }
        LoadingDialog loadDialogCache = new LoadingDialog(context, "Loading cache");
        AlertDialog loadingDialogCache = loadDialogCache.showDialog();
        if (loadingDialogCache != null && !loadingDialogCache.isShowing()) {
            loadingDialogCache.show();
        }
        //Need to get the bitmaps if they are not created
        LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadingDialogCache,new AsyncTaskCompleteListener<Result>() {
            @Override
            public void onTaskComplete(Result result) {
                List<GbcImage> clonedImages = new ArrayList<>();
                List<Bitmap> clonedBitmaps = new ArrayList<>();
                long timeMs = System.currentTimeMillis();
                boolean alreadyIncluded;
                for (int j = 0; j < gbcImagesToClone.size(); j++) {
                    GbcImage gbcImage = gbcImagesToClone.get(j);
                    timeMs += j;
                    for (int i = 0; i < numberOfClones; i++) {
                        GbcImage clonedImage = gbcImage.clone();

                        long timeMsThisClone = timeMs + i + numberOfClones;
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

                        int aux = 1;

                        do {//Mostly needed for clones of clones
                            alreadyIncluded = false;
                            for (GbcImage image : gbcImagesList) {
                                if (image.getHashCode().equals(clonedHash)) {
                                    alreadyIncluded = true;
                                    break;
                                }
                            }
                            for (GbcImage image : clonedImages) {
                                if (image.getHashCode().equals(clonedHash)) {
                                    alreadyIncluded = true;
                                    break;
                                }
                            }
                            if (alreadyIncluded) {//Change the hash
                                String lastNumbersString = clonedHash.substring(clonedHash.length() - 5);
                                int lastNumbers = Integer.parseInt(lastNumbersString, 10);//Base 10 to keep left 0
                                int sum = lastNumbers + aux++;
                                clonedHash = clonedHash.replace(lastNumbersString, String.format("%05d", sum));
                            }
                        } while (alreadyIncluded);

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


                LoadingDialog loadDialogSave = new LoadingDialog(context, "Saving data");
                AlertDialog loadingDialogSave = loadDialogSave.showDialog();
                if (loadingDialogSave != null) {
                    loadingDialogSave.show();
                }

                new SaveImageAsyncTask(clonedImages, clonedBitmaps, context, null, 0, customGridViewAdapterImage,loadingDialogSave).execute();
            }
        });
        asyncTask.execute();

    }
}
package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_DUPLICATED;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.NumberPicker;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.LoadingDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class DuplicateDialog {
    Context context;
    List<Integer> selectedImages;
    CustomGridViewAdapterImage customGridViewAdapterImage;
    List<GbcImage> filteredGbcImages;
    List<GbcImage> gbcImagesToDuplicate = new ArrayList<>();
    List<Integer> indexesToLoad = new ArrayList<>();

    public DuplicateDialog(Context context, List<Integer> selectedImages, CustomGridViewAdapterImage customGridViewAdapterImage, List<GbcImage> filteredGbcImages, Activity activity) {
        this.context = context;
        this.selectedImages = selectedImages;
        this.customGridViewAdapterImage = customGridViewAdapterImage;
        this.filteredGbcImages = filteredGbcImages;
    }

    public void createDuplicateDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.action_duplicate));

        final NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(10);
        builder.setView(numberPicker);

        builder.setPositiveButton(context.getString(R.string.action_duplicate), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selectedNumber = numberPicker.getValue();
                saveDuplicatedImages(selectedNumber);
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

    private void saveDuplicatedImages(int numberOfDuplicates) {
        Collections.sort(selectedImages);
        for (int i : selectedImages) {
            gbcImagesToDuplicate.add(filteredGbcImages.get(i));

            String hashCode = filteredGbcImages.get(i).getHashCode();
            if (imageBitmapCache.get(hashCode) == null) {
                indexesToLoad.add(i);
            }
        }
        LoadingDialog loadDialogCache = new LoadingDialog(context, context.getString(R.string.load_cache));
        loadDialogCache.showDialog();
        //Need to get the bitmaps if they are not created
        LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialogCache, result -> {
            CreateDuplicatesAsyncTask createDuplicatesAsyncTask = new CreateDuplicatesAsyncTask(loadDialogCache, numberOfDuplicates);
            createDuplicatesAsyncTask.execute();
        });
        asyncTask.execute();

    }

    private class CreateDuplicatesAsyncTask extends AsyncTask<Void, Void, Void> {
        LoadingDialog loadDialogCache;
        int numberOfDuplicats;

        List<Bitmap> duplicatedBitmaps = new ArrayList<>();
        List<GbcImage> duplicatedImages = new ArrayList<>();

        public CreateDuplicatesAsyncTask(LoadingDialog loadDialogCache, int numberOfDuplicates) {
            this.loadDialogCache = loadDialogCache;
            this.numberOfDuplicats = numberOfDuplicates;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            long timeMs = System.currentTimeMillis();
            boolean alreadyIncluded;
            int totalDuplicates = gbcImagesToDuplicate.size() * numberOfDuplicats;
            int currentDuplicatedImages = 0;
            for (int j = 0; j < gbcImagesToDuplicate.size(); j++) {
                GbcImage gbcImage = gbcImagesToDuplicate.get(j);
                timeMs += j;
                for (int i = 0; i < numberOfDuplicats; i++) {
                    GbcImage duplicatedImage = gbcImage.clone();
                    loadDialogCache.setLoadingDialogText("Creating duplicates: " + currentDuplicatedImages++ + "/" + totalDuplicates);

                    long timeMsThisDuplicate = timeMs + i + numberOfDuplicats;
                    String timeString = String.valueOf(timeMsThisDuplicate);
                    String lastFiveDigits = timeString.substring(Math.max(0, timeString.length() - 5));
                    String phrase = "duped" + lastFiveDigits;
                    String name = new String(gbcImage.getName());
                    name += "-duplicate";
                    StringBuilder modifiedString = new StringBuilder(gbcImage.getHashCode());
                    duplicatedImage.setName(name);
                    try {
                        modifiedString.replace(modifiedString.length() - 10, modifiedString.length(), phrase);
                    } catch (Exception e) {
                        e.printStackTrace();
                        modifiedString.append("dupeddbadlength" + timeString);
                    }
                    String duplicatedHash = modifiedString.toString();

                    int aux = 1;

                    do {//Mostly needed for duplicates of duplicates
                        alreadyIncluded = false;
                        for (GbcImage image : gbcImagesList) {
                            if (image.getHashCode().equals(duplicatedHash)) {
                                alreadyIncluded = true;
                                break;
                            }
                        }
                        for (GbcImage image : duplicatedImages) {
                            if (image.getHashCode().equals(duplicatedHash)) {
                                alreadyIncluded = true;
                                break;
                            }
                        }
                        if (alreadyIncluded) {//Change the hash
                            String lastNumbersString = duplicatedHash.substring(duplicatedHash.length() - 5);
                            int lastNumbers = Integer.parseInt(lastNumbersString, 10);//Base 10 to keep left 0
                            int sum = lastNumbers + aux++;
                            duplicatedHash = duplicatedHash.replace(lastNumbersString, String.format("%05d", sum));
                        }
                    } while (alreadyIncluded);

                    duplicatedImage.setHashCode(duplicatedHash);

                    HashSet tags = new HashSet(duplicatedImage.getTags());
                    tags.add(FILTER_DUPLICATED);
                    duplicatedImage.setTags(tags);
                    duplicatedImages.add(duplicatedImage);
                    Bitmap originalBitmap = imageBitmapCache.get(gbcImage.getHashCode());
                    Bitmap duplicatedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                    duplicatedBitmaps.add(duplicatedBitmap);

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            loadDialogCache.dismissDialog();

            LoadingDialog loadDialogSave = new LoadingDialog(context, context.getString(R.string.load_saving_dup));
            loadDialogSave.showDialog();

            new SaveImageAsyncTask(duplicatedImages, duplicatedBitmaps, context, null, 0, customGridViewAdapterImage, loadDialogSave).execute();
        }
    }
}

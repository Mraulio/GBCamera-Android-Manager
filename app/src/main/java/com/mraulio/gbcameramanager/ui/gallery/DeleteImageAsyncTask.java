package com.mraulio.gbcameramanager.ui.gallery;


import static com.mraulio.gbcameramanager.MainActivity.lastSeenGalleryImage;
import static com.mraulio.gbcameramanager.MainActivity.showEditMenuButton;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.encodeData;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.reloadTags;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesListHolder;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;

import android.app.Activity;
import android.os.AsyncTask;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.Collections;
import java.util.List;

public class DeleteImageAsyncTask extends AsyncTask<Void, Void, Void> {
    //        private int imageIndex;
    private List<Integer> listImagesIndexes;
    private Activity activity;

    public DeleteImageAsyncTask(List<Integer> listImagesIndexes, Activity activity) {
        this.listImagesIndexes = listImagesIndexes;
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        for (int imageIndex : listImagesIndexes) {
            String hashCode = GalleryFragment.filteredGbcImages.get(imageIndex).getHashCode();
            GbcImage gbcImage = GalleryFragment.filteredGbcImages.get(imageIndex);
            ImageDao imageDao = MainActivity.db.imageDao();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            ImageData imageData = imageDataDao.getImageDataByid(hashCode);
            imageDao.delete(gbcImage);
            imageDataDao.delete(imageData);
            Utils.imageBitmapCache.remove(hashCode);
            GalleryFragment.diskCache.remove(hashCode);
            GbcImage.numImages--;
        }
        //Doing this after deleting the images
        //IMPROVE THIS
        Collections.sort(listImagesIndexes);
        for (int index = listImagesIndexes.size(); index > 0; index--) {
            int image = listImagesIndexes.get(index - 1);
            String imageHash = GalleryFragment.filteredGbcImages.get(image).getHashCode();
            GalleryFragment.filteredGbcImages.remove(image);
            for (int i = 0; i < gbcImagesList.size(); i++) {
                GbcImage gbcImage = gbcImagesList.get(i);
                if (gbcImage.getHashCode().equals(imageHash)) {
                    gbcImagesList.remove(i);
                }
            }
            for (int i = 0; i < gbcImagesListHolder.size(); i++) {
                GbcImage gbcImage = gbcImagesListHolder.get(i);
                if (gbcImage.getHashCode().equals(imageHash)) {
                    gbcImagesListHolder.remove(i);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        GalleryFragment.itemsPerPage = MainActivity.imagesPage;//Because it's changed when going to the last page on the updateGridView
        GalleryFragment.selectedImages.clear();
        GalleryFragment.selectionMode[0] = false;
        GalleryFragment.tv.setText(GalleryFragment.tv.getContext().getString(R.string.total_images) + GbcImage.numImages);
        //Resetting the lastSeenGalleryImage to 0 if any image is deleted
        lastSeenGalleryImage = 0;

        //Update lastPage and CurrentPage after deleting

        if (GalleryFragment.currentPage > (GalleryFragment.filteredGbcImages.size() / GalleryFragment.itemsPerPage) - 1) {
            GalleryFragment.currentPage = ((GalleryFragment.filteredGbcImages.size() - 1) / GalleryFragment.itemsPerPage);
            GalleryFragment.editor.putInt("current_page", GalleryFragment.currentPage);
            GalleryFragment.editor.apply();
            GalleryFragment.lastPage = GalleryFragment.currentPage;
        }
        GalleryFragment.tv_page.setText((GalleryFragment.currentPage + 1) + " / " + (GalleryFragment.lastPage + 1));
        MainActivity.fab.hide();

        retrieveTags(gbcImagesList);
        reloadTags();
        showEditMenuButton = false;
        activity.invalidateOptionsMenu();

        GalleryFragment.loadingDialog.dismiss();
        GalleryFragment.updateGridView();
    }
}
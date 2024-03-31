package com.mraulio.gbcameramanager.ui.gallery;


import static com.mraulio.gbcameramanager.ui.gallery.CollageMaker.addPadding;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.filteredGbcImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;

import static com.mraulio.gbcameramanager.ui.gallery.PaperUtils.paperize;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.LoadingDialog;

import java.io.IOException;
import java.util.List;

public class PaperizeAsyncTask extends AsyncTask<Void, Void, Void> {
    private List<Integer> gbcImagesList;
    private Context context;
    private int paperColor;
    private ImageView ivPaperized;
    private List<Bitmap> paperizedBitmaps;
    private boolean onlyImage;
    private LoadingDialog loadDialog;

    public PaperizeAsyncTask(List<Integer> gbcImagesList, int paperColor, List<Bitmap> paperizedBitmaps, ImageView ivPaperized, boolean onlyImage, Context context, LoadingDialog loadDialog) {
        this.gbcImagesList = gbcImagesList;
        this.paperColor = paperColor;
        this.paperizedBitmaps = paperizedBitmaps;
        this.ivPaperized = ivPaperized;
        this.onlyImage = onlyImage;
        this.context = context;
        this.loadDialog = loadDialog;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        for (int i : gbcImagesList) {
            GbcImage gbcImage = filteredGbcImages.get(i);
            Bitmap bw_image = null;
            try {
                bw_image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                //Not rotate wild frames
                if (!hashFrames.get(gbcImage.getFrameId()).isWildFrame() ||
                        (bw_image.getHeight() > 144 && gbcImage.getRotation() == 2)) {//If image is higher than a normal one, only rotate if it's 180º
                    bw_image = rotateBitmap(bw_image, gbcImage);
                }

                //If image is rotated sideways, add 8px on each side to print it in that orientation
                if (bw_image.getWidth() == 144 && bw_image.getHeight() == 160) {
                    bw_image = addPadding(bw_image, 1, Color.parseColor("#FFFFFF"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap paperized = paperize(bw_image, paperColor, onlyImage, context);
            paperizedBitmaps.add(paperized);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        ivPaperized.setImageBitmap(paperizedBitmaps.get(0));

        final long imageViewHeight = ivPaperized.getHeight();
        // Starting position outside the screen
        ivPaperized.setTranslationY(imageViewHeight);
        final long slideDuration = 700; // Total duration of the ending animation
        final long stopDuration = 700; // Duration of every stop
        final float stopPosition1 = -(imageViewHeight * 0.15f-imageViewHeight);
        final float stopPosition2 = -(imageViewHeight * 0.30f-imageViewHeight);
        final float stopPosition3 = -(imageViewHeight * 0.45f-imageViewHeight);
        final float stopPosition4 = -(imageViewHeight * 0.60f-imageViewHeight);
        final float stopPosition5 = -(imageViewHeight * 0.75f-imageViewHeight);
        final float stopPosition6 = -(imageViewHeight * 0.90f-imageViewHeight);

        // Create a set of animators for the stops
        AnimatorSet stopAnimation1 = createStopAnimation(ivPaperized, stopPosition1, stopDuration);
        AnimatorSet stopAnimation2 = createStopAnimation(ivPaperized, stopPosition2, stopDuration);
        AnimatorSet stopAnimation3 = createStopAnimation(ivPaperized, stopPosition3, stopDuration);
        AnimatorSet stopAnimation4 = createStopAnimation(ivPaperized, stopPosition4, stopDuration);
        AnimatorSet stopAnimation5 = createStopAnimation(ivPaperized, stopPosition5, stopDuration);
        AnimatorSet stopAnimation6 = createStopAnimation(ivPaperized, stopPosition6, stopDuration);


        // Create ab ObjectAnimator for the movement
        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(ivPaperized, "translationY", 0);
        slideAnimator.setDuration(slideDuration);
        slideAnimator.setInterpolator(new AccelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(stopAnimation1, stopAnimation2, stopAnimation3, stopAnimation4,stopAnimation5,stopAnimation6, slideAnimator);

        animatorSet.start();
        loadDialog.dismissDialog();
    }

    private AnimatorSet createStopAnimation(ImageView imageView, float position, long duration) {
        ObjectAnimator stopAnimator = ObjectAnimator.ofFloat(imageView, "translationY", position);
        stopAnimator.setDuration(duration);

        AnimatorSet stopAnimation = new AnimatorSet();
        stopAnimation.play(stopAnimator);

        return stopAnimation;
    }
}
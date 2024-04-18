package com.mraulio.gbcameramanager.ui.importFile;


import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.paletteChanger;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.checkPaletteColors;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.convertToGrayScale;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.ditherImage;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.resizeImage;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.finalListBitmaps;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.finalListImages;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_TRANSFORMED;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;


import com.canhub.cropper.CropImageView;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;


public class TransformImage {

    Bitmap originalBitmap;
    Bitmap prevBitmap;
    GbcImage gbcImage;
    Context context;
    Bitmap croppedBitmap;
    GridView gridViewImport;
    Bitmap originalBitmapCopy;

    public TransformImage(Bitmap originalBitmap, GbcImage gbcImage, Context context, GridView gridViewImport) {
        this.originalBitmap = originalBitmap;
        this.gbcImage = gbcImage;
        this.context = context;
        this.gridViewImport = gridViewImport;
        originalBitmapCopy = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    public void createTransformDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.image_transform);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        TextView tvBrightness = dialog.findViewById(R.id.tv_brightness);
        TextView tvContrast = dialog.findViewById(R.id.tv_contrast);

        Switch swOriginalRatio = dialog.findViewById(R.id.sw_original_ratio);
        SeekBar sbRotate = dialog.findViewById(R.id.sb_rotate);
        SeekBar sbBrightness = dialog.findViewById(R.id.sb_brightness);
        SeekBar sbContrast = dialog.findViewById(R.id.sb_contrast);
        CropImageView cropImageView = dialog.findViewById(R.id.crop_iv);
        cropImageView.setImageBitmap(originalBitmapCopy);
        cropImageView.setMinCropResultSize(128, 112);
        cropImageView.setMinimumWidth(128);
        cropImageView.setMinimumHeight(112);
        cropImageView.setScaleType(CropImageView.ScaleType.FIT_CENTER);
        ImageView ivTransformed = dialog.findViewById(R.id.iv_transformed);
        final boolean[] convertedOnce = {false};

        Button btnReload = dialog.findViewById(R.id.btn_reload_transform);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_accept);


        tvBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sbBrightness.setProgress(255);
            }
        });

        tvContrast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sbContrast.setProgress(100);
            }
        });


        swOriginalRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swOriginalRatio.isChecked()) {
                    cropImageView.setAspectRatio(128, 112);
                    cropImageView.setFixedAspectRatio(true);

                } else {
                    cropImageView.setFixedAspectRatio(false);
                }
            }
        });
        sbContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Bitmap adjustedBitmap = changeContrast(originalBitmapCopy, sbContrast.getProgress() - 100);
                adjustedBitmap = changeBrightness(adjustedBitmap, sbBrightness.getProgress() - 255);
                cropImageView.setImageBitmap(adjustedBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Bitmap adjustedBitmap = changeContrast(originalBitmapCopy, sbContrast.getProgress() - 100);
                adjustedBitmap = changeBrightness(adjustedBitmap, sbBrightness.getProgress() - 255);
                cropImageView.setImageBitmap(adjustedBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sbRotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Matrix matrix = new Matrix();
                matrix.postRotate(sbRotate.getProgress());
                originalBitmapCopy = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
                Bitmap adjustedBitmap = changeContrast(originalBitmapCopy, sbContrast.getProgress() - 100);
                adjustedBitmap = changeBrightness(adjustedBitmap, sbBrightness.getProgress() - 255);
                cropImageView.setImageBitmap(adjustedBitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadImage(convertedOnce, cropImageView, ivTransformed, swOriginalRatio.isChecked());
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (convertedOnce[0]) {
                    finalListBitmaps.set(0, croppedBitmap);
                    prevBitmap = finalListBitmaps.get(0);
                    byte[] hash;
                    try {
                        hash = MessageDigest.getInstance("SHA-256").digest(gbcImage.getImageBytes());
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    String hashHex = Utils.bytesToHex(hash);
                    gbcImage.setHashCode(hashHex);
                    LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("Type", "Transformed");
                    gbcImage.setImageMetadata(metadata);
                    gbcImage.getTags().add(FILTER_TRANSFORMED);
                    Adapter adapter = new CustomGridViewAdapterImage(context, R.layout.row_items, finalListImages, finalListBitmaps, true, true, false, null);
                    gridViewImport.setAdapter((ListAdapter) adapter);

                    dialog.dismiss();
                }
            }
        });

        dialog.getWindow().setAttributes(lp);
        dialog.show();
    }

    private void reloadImage(boolean[] convertedOnce, CropImageView cropImageView, ImageView ivTransformed, boolean cropOriginal) {
        convertedOnce[0] = true;
        croppedBitmap = cropImageView.getCroppedImage();
        if (cropOriginal) {
            croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 128, 112, false);
        }
        croppedBitmap = resizeImage(croppedBitmap, gbcImage);

        try {
            byte[] imageBytes = Utils.encodeImage(croppedBitmap, "bw");
            gbcImage.setImageBytes(imageBytes);
            croppedBitmap = paletteChanger(gbcImage.getPaletteId(), imageBytes, gbcImage.isInvertPalette());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ivTransformed.setImageBitmap(Bitmap.createScaledBitmap(croppedBitmap, croppedBitmap.getWidth() * 3, croppedBitmap.getHeight() * 3, false));
    }

    private Bitmap changeBrightness(Bitmap src, int brightnessValue) {
        Bitmap dest = Bitmap.createBitmap(
                src.getWidth(), src.getHeight(), src.getConfig());

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[]{
                1, 0, 0, 0, brightnessValue, // Red
                0, 1, 0, 0, brightnessValue, // Green
                0, 0, 1, 0, brightnessValue, // Blue
                0, 0, 0, 1, 0 // Alpha
        });

        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(src, 0, 0, paint);

        return dest;
    }

    private Bitmap changeContrast(Bitmap src, float contrastValue) {
        Bitmap dest = Bitmap.createBitmap(
                src.getWidth(), src.getHeight(), src.getConfig());

        float contrast = (100 + contrastValue) / 100f;
        float offset = (float) (128 * (1 - contrast));

        ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, offset, // Red
                0, contrast, 0, 0, offset, // Green
                0, 0, contrast, 0, offset, // Blue
                0, 0, 0, 1, 0 // Alpha
        });

        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(src, 0, 0, paint);

        return dest;
    }


}

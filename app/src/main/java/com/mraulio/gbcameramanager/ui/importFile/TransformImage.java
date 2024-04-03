package com.mraulio.gbcameramanager.ui.importFile;


import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.checkPaletteColors;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.convertToGrayScale;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.ditherImage;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.resizeImage;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.finalListBitmaps;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.finalListImages;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Matrix;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SeekBar;


import com.canhub.cropper.CropImageView;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class TransformImage {

    Bitmap originalBitmap;
    Bitmap prevBitmap;
    GbcImage gbcImage;
    Context context;
    Bitmap croppedBitmap;
    GridView gridViewImport;

    public TransformImage(Bitmap originalBitmap,GbcImage gbcImage, Context context,GridView gridViewImport) {
        this.originalBitmap = originalBitmap;
        this.gbcImage = gbcImage;
        this.context = context;
        this.gridViewImport = gridViewImport;
    }


    public void createTransformDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.image_transform);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        SeekBar sbRotate = dialog.findViewById(R.id.sb_rotate);
        CropImageView cropImageView = dialog.findViewById(R.id.cropImageView);
        cropImageView.setImageBitmap(originalBitmap);
        cropImageView.setAspectRatio(128, 112);
        cropImageView.setMinCropResultSize(128, 112);
        cropImageView.setMinimumWidth(128);
        cropImageView.setMinimumHeight(112);
        cropImageView.setScaleType(CropImageView.ScaleType.FIT_CENTER);
        ImageView ivTransformed = dialog.findViewById(R.id.iv_transformed);
        final boolean[] convertedOnce = {false};

        Button btnCrop = dialog.findViewById(R.id.btn_crop);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_accept);

        sbRotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Matrix matrix = new Matrix();
                matrix.postRotate(sbRotate.getProgress());
                cropImageView.setImageBitmap(Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Método no utilizado en este ejemplo
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Método no utilizado en este ejemplo
            }
        });


        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                convertedOnce[0] = true;
                croppedBitmap = cropImageView.getCroppedImage();
                croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 128, 112, false);
                croppedBitmap = resizeImage(croppedBitmap);
                boolean hasAllColors = checkPaletteColors(croppedBitmap);
                if (!hasAllColors) {
                    croppedBitmap = convertToGrayScale(croppedBitmap);
                    croppedBitmap = ditherImage(croppedBitmap);
                }
                ivTransformed.setImageBitmap(Bitmap.createScaledBitmap(croppedBitmap, croppedBitmap.getWidth() * 3, croppedBitmap.getHeight() * 3, false));

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
                    try {
                        byte[] imageBytes = Utils.encodeImage(prevBitmap, "bw");
                        gbcImage.setImageBytes(imageBytes);
                        byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                        String hashHex = Utils.bytesToHex(hash);
                        gbcImage.setHashCode(hashHex);
                        Adapter adapter = new CustomGridViewAdapterImage(context, R.layout.row_items, finalListImages, finalListBitmaps, true, true, false, null);
                        gridViewImport.setAdapter((ListAdapter) adapter);
                    } catch (IOException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    dialog.dismiss();
                }
            }
        });

        dialog.getWindow().setAttributes(lp);
        dialog.show();
    }


}

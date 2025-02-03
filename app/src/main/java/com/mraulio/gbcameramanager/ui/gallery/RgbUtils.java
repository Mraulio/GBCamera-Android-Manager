package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.makeSquareImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSize;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSquare;
import static com.mraulio.gbcameramanager.utils.Utils.encodeImage;
import static com.mraulio.gbcameramanager.utils.Utils.hashPalettes;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.importFile.newpalette.SimpleItemTouchHelperCallback;
import com.mraulio.gbcameramanager.utils.FourThumbSeekBar;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RgbUtils {

    List<Bitmap> rgbnBitmaps;
    List<GbcImage> gbcImages;
    Context context;
    boolean crop, extraGallery, addNeutral = true;
    FourThumbSeekBar ftRed, ftGreen, ftBlue;

    Bitmap rgbImage;
    HashMap<String, byte[]> imageBytesHash = new HashMap<>();
    final float[] redFactor = {1.0f};
    final float[] greenFactor = {1.0f};
    final float[] blueFactor = {1.0f};
    GridAdapterRGB gridAdapter;
    ImageView rgbImageView;

    public interface OnRgbSaved {
        void onButtonRgbSaved();
    }

    public RgbUtils(Context context, List<Bitmap> rgbnBitmaps, boolean extraGallery, @Nullable List<GbcImage> gbcImages) {
        this.context = context;
        this.rgbnBitmaps = rgbnBitmaps;
        this.extraGallery = extraGallery;
        this.gbcImages = gbcImages;
    }


    public void showRgbDialog(OnRgbSaved listener) {


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("RGB");

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.rgb_dialog, null);
        builder.setView(dialogView);

        LinearLayout lySbs = dialogView.findViewById(R.id.ly_sbs);
        LinearLayout lyFt = dialogView.findViewById(R.id.ly_fourThumbs);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_rgb);
        Button btnSave = dialogView.findViewById(R.id.btn_save_rgb);
        Switch swNeutral = dialogView.findViewById(R.id.sw_neutral);
        Switch swCrop = dialogView.findViewById(R.id.sw_crop_rgb);

        rgbImageView = dialogView.findViewById(R.id.rgb_image);

        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_RGB);
        recyclerView.setLayoutManager(new GridLayoutManager(context, rgbnBitmaps.size()));


        if (!extraGallery) {
            for (int i = 0; i < gbcImages.size(); i++) {
                GbcImage gbcImage = gbcImages.get(i);
                try {
                    Bitmap image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                    byte[] imageBytes = encodeImage(image, "bw");
                    imageBytesHash.put(gbcImage.getHashCode(), imageBytes);

                    rgbnBitmaps.set(i, image);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        gridAdapter = new GridAdapterRGB(rgbnBitmaps);
        recyclerView.setAdapter(gridAdapter);
        if (extraGallery) {
            lySbs.setVisibility(View.VISIBLE);
            lyFt.setVisibility(View.GONE);

            SeekBar sbRedFactor = dialogView.findViewById(R.id.sb_redFactor);
            SeekBar sbGreenFactor = dialogView.findViewById(R.id.sb_greenFactor);
            SeekBar sbBlueFactor = dialogView.findViewById(R.id.sb_blueFactor);
            TextView tvRedFactor = dialogView.findViewById(R.id.tv_redFactor);
            TextView tvGreenFactor = dialogView.findViewById(R.id.tv_greenFactor);
            TextView tvBlueFactor = dialogView.findViewById(R.id.tv_blueFactor);

            sbRedFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    redFactor[0] = sbRedFactor.getProgress() / 100.0f;
                    tvRedFactor.setText(sbRedFactor.getProgress() + "%");
                    updateRgbImage();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            sbGreenFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    greenFactor[0] = sbGreenFactor.getProgress() / 100.0f;
                    tvGreenFactor.setText(sbGreenFactor.getProgress() + "%");

                    updateRgbImage();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            sbBlueFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    blueFactor[0] = sbBlueFactor.getProgress() / 100.0f;
                    tvBlueFactor.setText(sbBlueFactor.getProgress() + "%");

                    updateRgbImage();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

        } else {
            ftRed = dialogView.findViewById(R.id.ft_red);
            ftGreen = dialogView.findViewById(R.id.ft_green);
            ftBlue = dialogView.findViewById(R.id.ft_blue);

            ftRed.setOnThumbMoveListener(new OnThumbMoveListener() {
                @Override
                public void onThumbMove(int[] thumbValues) {
                    int[] convertedValues = new int[4];
                    for (int i = 0; i < thumbValues.length; i++) {
                        int x = rgbToArgb(thumbValues[i]);
                        convertedValues[i] = x;
                    }

                    ImageCodec imageCodec = new ImageCodec(160, imageBytesHash.get(gbcImages.get(0).getHashCode()).length / 40);//imageBytes.length/40 to get the height of the image
                    Bitmap image = imageCodec.decodeWithPalette(convertedValues, imageBytesHash.get(gbcImages.get(0).getHashCode()), true);
                    rgbnBitmaps.set(0, image);

                    gridAdapter.notifyDataSetChanged();
                    updateRgbImage();
                }
            });

            ftGreen.setOnThumbMoveListener(thumbValues -> {
                int[] convertedValues = new int[4];
                for (int i = 0; i < thumbValues.length; i++) {
                    int x = rgbToArgb(thumbValues[i]);
                    convertedValues[i] = x;
                }

                ImageCodec imageCodec = new ImageCodec(160, imageBytesHash.get(gbcImages.get(1).getHashCode()).length / 40);//imageBytes.length/40 to get the height of the image
                Bitmap image = imageCodec.decodeWithPalette(convertedValues, imageBytesHash.get(gbcImages.get(1).getHashCode()), true);
                rgbnBitmaps.set(1, image);
                gridAdapter.notifyDataSetChanged();

                updateRgbImage();

            });

            ftBlue.setOnThumbMoveListener(thumbValues -> {
                int[] convertedValues = new int[4];
                for (int i = 0; i < thumbValues.length; i++) {
                    int x = rgbToArgb(thumbValues[i]);
                    convertedValues[i] = x;
                }
                ImageCodec imageCodec = new ImageCodec(160, imageBytesHash.get(gbcImages.get(2).getHashCode()).length / 40);//imageBytes.length/40 to get the height of the image
                Bitmap image = imageCodec.decodeWithPalette(convertedValues, imageBytesHash.get(gbcImages.get(2).getHashCode()), true);

                rgbnBitmaps.set(2, image);
                gridAdapter.notifyDataSetChanged();
                updateRgbImage();

            });
        }

        if (rgbnBitmaps.size() != 4) {
            swNeutral.setVisibility(View.GONE);
        }

        swNeutral.setOnClickListener(v -> {
            addNeutral = swNeutral.isChecked();
            updateRgbImage();

        });

        if (rgbnBitmaps.get(0).getHeight() != 144 && rgbnBitmaps.get(0).getHeight() != 224) {
            swCrop.setVisibility(View.GONE);
        } else {
            swCrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    crop = swCrop.isChecked();
                    updateRgbImage();

                }
            });
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(gridAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        gridAdapter.setOnItemMovedListener((fromPosition, toPosition) -> {
            recyclerView.post(() -> {
                GridAdapterRGB.ViewHolder fromViewHolder = (GridAdapterRGB.ViewHolder) recyclerView.findViewHolderForAdapterPosition(fromPosition);
                GridAdapterRGB.ViewHolder toViewHolder = (GridAdapterRGB.ViewHolder) recyclerView.findViewHolderForAdapterPosition(toPosition);

                if (fromViewHolder != null) {
                    updateImageViewBackground(fromViewHolder, fromPosition);
                }
                if (toViewHolder != null) {
                    updateImageViewBackground(toViewHolder, toPosition);
                }
            });

            // Swap the elements in the lists
            if (!extraGallery) {
                Collections.swap(gbcImages, fromPosition, toPosition);
                resetThumbs();
            } else {
                List<Bitmap> newRgbBitmapList = new ArrayList<>();
                for (int i = 0; i < gridAdapter.getItemCount(); i++) {
                    newRgbBitmapList.add(gridAdapter.getItems().get(i));
                }
                rgbnBitmaps = newRgbBitmapList;
            }
            updateRgbImage();

        });

        rgbImage = combineImages(rgbnBitmaps, redFactor[0], greenFactor[0], blueFactor[0]);
        rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));

        AlertDialog dialog = builder.create();

        dialog.show();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDateTime now = null;
                Date nowDate = new Date();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    now = LocalDateTime.now();
                }
                File file;
                String prefix = "RGB_";
                prefix += extraGallery ? "extra_" : "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateLocale + "_HH-mm-ss");

                    file = new File(Utils.IMAGES_FOLDER, prefix + dtf.format(now) + ".png");
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());
                    file = new File(Utils.IMAGES_FOLDER, prefix + sdf.format(nowDate) + ".png");
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap bitmap = Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * (extraGallery ? 1 : exportSize), rgbImage.getHeight() * (extraGallery ? 1 : exportSize), false);
                    //Make square if checked in settings
                    if (exportSquare) {
                        bitmap = makeSquareImage(bitmap);
                    }
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast toast = Toast.makeText(context, context.getString(R.string.toast_saved) + " RGB", Toast.LENGTH_LONG);
                    toast.show();
                    mediaScanner(file, context);
                    showNotification(context, file);

                    if (listener != null) {
                        if (listener != null) {
                            listener.onButtonRgbSaved();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void resetThumbs() {
        ftRed.resetThumbs();
        ftGreen.resetThumbs();
        ftBlue.resetThumbs();

        for (int i = 0; i < gbcImages.size(); i++) {
            GbcImage gbcImage = gbcImages.get(i);
            ImageCodec imageCodec = new ImageCodec(160, imageBytesHash.get(gbcImage.getHashCode()).length / 40);//imageBytes.length/40 to get the height of the image
            Bitmap image = imageCodec.decodeWithPalette(hashPalettes.get("bw").getPaletteColorsInt(), imageBytesHash.get(gbcImage.getHashCode()), false);
            rgbnBitmaps.set(i, image);
        }

        gridAdapter.notifyDataSetChanged();
        updateRgbImage();
    }

    private void updateRgbImage() {

        rgbImage = combineImages(rgbnBitmaps, redFactor[0], greenFactor[0], blueFactor[0]);
        rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));
    }

    private void updateImageViewBackground(GridAdapterRGB.ViewHolder viewHolder, int position) {
        switch (position) {
            case 0:
                viewHolder.mImageView.setBackgroundColor(Color.RED);
                break;
            case 1:
                viewHolder.mImageView.setBackgroundColor(Color.GREEN);
                break;
            case 2:
                viewHolder.mImageView.setBackgroundColor(Color.BLUE);
                break;
            case 3:
                viewHolder.mImageView.setBackgroundColor(Color.BLACK);
                break;
            default:
                break;
        }
    }

    public static int rgbToArgb(int x) {
        int alpha = 255;
        int argb = (alpha << 24) | (x << 16) | (x << 8) | x;
        return argb;
    }

    private Bitmap combineImages(List<Bitmap> bitmapsRGB, float redFactor, float greenFactor, float blueFactor) {

        Bitmap firstImage;
        Bitmap secondImage;
        Bitmap thirdImage;
        Bitmap fourthImage = null;

        firstImage = bitmapsRGB.get(0);
        secondImage = bitmapsRGB.get(1);
        thirdImage = bitmapsRGB.get(2);

        if (bitmapsRGB.size() == 4) {
            fourthImage = bitmapsRGB.get(3);
        }

        int width = firstImage.getWidth();
        int height = firstImage.getHeight();
        Bitmap combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int red = Color.red(firstImage.getPixel(x, y));
                    int green = Color.green(secondImage.getPixel(x, y));
                    int blue = Color.blue(thirdImage.getPixel(x, y));
                    int neutral = fourthImage != null && addNeutral ? Color.red(fourthImage.getPixel(x, y)) : 255;

                    red = Math.min((int) (red * redFactor), 255);
                    green = Math.min((int) (green * greenFactor), 255);
                    blue = Math.min((int) (blue * blueFactor), 255);

                    int combinedColor;
                    if (fourthImage != null) {
                        int finalRed = blendChannel(red, neutral);
                        int finalGreen = blendChannel(green, neutral);
                        int finalBlue = blendChannel(blue, neutral);
                        combinedColor = Color.rgb(finalRed, finalGreen, finalBlue);
                    } else {
                        combinedColor = Color.rgb(red, green, blue);
                    }

                    combined.setPixel(x, y, combinedColor);
                }
            }
        } catch (IllegalArgumentException e) {
            Utils.toast(context, context.getString(R.string.sizes_exception));
        }

        if (crop) {
            if (height == 144) {
                combined = Bitmap.createBitmap(combined, 16, 16, 128, 112);
            }
            //For the wild frames
            else if (height == 224) {
                combined = Bitmap.createBitmap(combined, 16, 40, 128, 112);
            }
        }

        return combined;
    }

    private int blendChannel(int color, int neutral) {
        return (int) (color * (neutral / 255.0));
    }


}

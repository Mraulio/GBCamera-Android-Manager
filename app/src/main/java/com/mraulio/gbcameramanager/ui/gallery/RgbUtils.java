package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.makeSquareImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSize;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSquare;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.importFile.newpalette.SimpleItemTouchHelperCallback;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RgbUtils {

    List<Bitmap> rgbnBitmaps;
    Context context;
    boolean crop, extraGallery, addNeutral = true;

    Bitmap rgbImage;

    private OnRgbSaved onRgbSaved;

    public interface OnRgbSaved {
        void onButtonRgbSaved();
    }

    public RgbUtils(Context context, List<Bitmap> rgbnBitmaps, boolean extraGallery) {
        this.context = context;
        this.rgbnBitmaps = rgbnBitmaps;
        this.extraGallery = extraGallery;
    }


    public void showRgbDialog(OnRgbSaved listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("RGB");

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.rgb_dialog, null);

        builder.setView(dialogView);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_rgb);
        Button btnSave = dialogView.findViewById(R.id.btn_save_rgb);
        Switch swNeutral = dialogView.findViewById(R.id.sw_neutral);
        Switch swCrop = dialogView.findViewById(R.id.sw_crop_rgb);

        if (rgbnBitmaps.size() != 4) {
            swNeutral.setVisibility(View.GONE);
        }
        ImageView rgbImageView = dialogView.findViewById(R.id.rgb_image);

        swNeutral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNeutral = swNeutral.isChecked();
                rgbImage = combineImages(rgbnBitmaps);
                rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));
            }
        });

        if (rgbnBitmaps.get(0).getHeight() != 144 && rgbnBitmaps.get(0).getHeight() != 224) {
            swCrop.setVisibility(View.GONE);
        } else {
            swCrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    crop = swCrop.isChecked();
                    rgbImage = combineImages(rgbnBitmaps);
                    rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));
                }
            });
        }

        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_RGB);
        recyclerView.setLayoutManager(new GridLayoutManager(context, rgbnBitmaps.size()));

        GridAdapterRGB gridAdapter = new GridAdapterRGB(rgbnBitmaps);
        recyclerView.setAdapter(gridAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(gridAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        gridAdapter.setOnItemMovedListener(new GridAdapterRGB.OnItemMovedListener() {
            @Override
            public void onItemMoved(int fromPosition, int toPosition) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        GridAdapterRGB.ViewHolder fromViewHolder = (GridAdapterRGB.ViewHolder) recyclerView.findViewHolderForAdapterPosition(fromPosition);
                        GridAdapterRGB.ViewHolder toViewHolder = (GridAdapterRGB.ViewHolder) recyclerView.findViewHolderForAdapterPosition(toPosition);

                        if (fromViewHolder != null) {
                            updateImageViewBackground(fromViewHolder, fromPosition);
                        }
                        if (toViewHolder != null) {
                            updateImageViewBackground(toViewHolder, toPosition);
                        }
                    }
                });
                List<Bitmap> newRgbBitmapList = new ArrayList<>();
                for (int i = 0; i < gridAdapter.getItemCount(); i++) {
                    newRgbBitmapList.add(gridAdapter.getItems().get(i));
                }
                rgbnBitmaps = newRgbBitmapList;
                rgbImage = combineImages(rgbnBitmaps);
                rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap(rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));
            }
        });

        rgbImage = combineImages(rgbnBitmaps);
        rgbImageView.setImageBitmap(extraGallery ? rgbImage : Bitmap.createScaledBitmap( rgbImage, rgbImage.getWidth() * 4, rgbImage.getHeight() * 4, false));

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

    private Bitmap combineImages(List<Bitmap> bitmapsRGB) {

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
            Utils.toast(context, context.getString(R.string.hdr_exception));
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

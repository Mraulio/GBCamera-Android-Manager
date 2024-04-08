package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.MainActivity.SORT_MODE.*;
import static com.mraulio.gbcameramanager.MainActivity.db;
import static com.mraulio.gbcameramanager.MainActivity.exportSquare;
import static com.mraulio.gbcameramanager.MainActivity.sortDescending;
import static com.mraulio.gbcameramanager.MainActivity.sortMode;
import static com.mraulio.gbcameramanager.MainActivity.sortModeEnum;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.currentPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.diskCache;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.editor;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.galleryActivity;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.hiddenFilterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectedFilterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updateGridView;
import static com.mraulio.gbcameramanager.ui.gallery.MetadataValues.metadataTexts;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.hashPalettes;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.saveTagsSet;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;
import static com.mraulio.gbcameramanager.utils.Utils.tagsHash;
import static com.mraulio.gbcameramanager.utils.Utils.toast;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;

import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ddyos.unicode.exifinterface.UnicodeExifInterface;
import com.google.gson.Gson;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

public class GalleryUtils {
    public static void saveImage(List<GbcImage> gbcImages, Context context, boolean crop) {
        LocalDateTime now = null;
        Date nowDate = new Date();
        File file = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            now = LocalDateTime.now();
        }

        String fileNameBase = "gbcImage_";
        String extension = MainActivity.exportPng ? ".png" : ".txt";
        for (int i = 0; i < gbcImages.size(); i++) {
            GbcImage gbcImage = gbcImages.get(i);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            String fileName = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                fileName = fileNameBase + dtf.format(now);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                fileName = fileNameBase + sdf.format(nowDate);
            }

            if (gbcImages.size() > 1) {
                fileName += "_" + (i + 1);
            }

            fileName += extension;
            if (MainActivity.exportPng) {
                file = new File(Utils.IMAGES_FOLDER, fileName);

                if (image.getHeight() == 144 && image.getWidth() == 160 && crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                //For the wild frames
                else if (image.getHeight() == 224 && crop) {
                    image = Bitmap.createBitmap(image, 16, 40, 128, 112);
                }
                //Rotate the image
                image = rotateBitmap(image, gbcImage);

                //Make square if checked in settings
                if (exportSquare) {
                    image = makeSquareImage(image);
                }

                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();

                    //Create the metadata text
                    StringBuilder stringBuilder = new StringBuilder();
                    LinkedHashMap lhm = gbcImage.getImageMetadata();
                    stringBuilder.append("Origin: GBCamera Android Manager\n");
                    stringBuilder.append("Palette: " + hashPalettes.get(gbcImage.getPaletteId()).getPaletteName() + " (" + gbcImage.getPaletteId() + ")\n");
                    if (gbcImage.getFrameId() != null)
                        stringBuilder.append("Frame: " + hashFrames.get(gbcImage.getFrameId()).getFrameName() + " (" + gbcImage.getFrameId() + ")\n");

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    stringBuilder.append("Creation date: " + sdf.format(gbcImage.getCreationDate()) + "\n");

                    if (lhm != null) { //Last seen images don't have metadata
                        for (Object key : lhm.keySet()) {
                            if (key.equals("frameIndex")) continue;
                            String metadata = metadataTexts.get(key);
                            String value = (String) lhm.get(key);
                            if (metadata == null) {
                                metadata = (String) key;
                            }
                            stringBuilder.append(metadata).append(": ").append(value).append("\n");
                            if (key.equals("isCopy")) stringBuilder.append("\n");
                        }
                    }

                    mediaScanner(file, context);

                    String metadataComment = stringBuilder.toString();

                    try {
                        //For recent android API, the other library gives error
                        ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, metadataComment);
                        exifInterface.saveAttributes();
                    } catch (Exception e) {
                        //For older androids APIs that give error with PNGs
                        try {
                            UnicodeExifInterface exifInterface = new UnicodeExifInterface(file.getAbsolutePath());
                            exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, metadataComment);
                            exifInterface.saveAttributes();
                        } catch (Exception ex) {
                            e.printStackTrace();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                file = new File(Utils.TXT_FOLDER, fileName);

                //Saving txt without cropping it
                try {
                    //Need to change the palette to bw so the encodeImage method works
                    image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                    StringBuilder txtBuilder = new StringBuilder();
                    //Appending these commands so the export is compatible with
                    // https://herrzatacke.github.io/gb-printer-web/#/import
                    // and https://mofosyne.github.io/arduino-gameboy-printer-emulator/GameBoyPrinterDecoderJS/gameboy_printer_js_decoder.html
                    txtBuilder.append("{\"command\":\"INIT\"}\n" +
                            "{\"command\":\"DATA\",\"compressed\":0,\"more\":1}\n");
                    String txt = Utils.bytesToHex(Utils.encodeImage(image, "bw"));
                    txt = addSpacesAndNewLines(txt).toUpperCase();
                    txtBuilder.append(txt);
                    txtBuilder.append("\n{\"command\":\"DATA\",\"compressed\":0,\"more\":0}\n" +
                            "{\"command\":\"PRNT\",\"sheets\":1,\"margin_upper\":1,\"margin_lower\":3,\"pallet\":228,\"density\":64 }");
                    FileWriter fileWriter = new FileWriter(file);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(txtBuilder.toString());
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            showNotification(context, file);
        }
        if (MainActivity.exportPng) {
            toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved) + MainActivity.exportSize);
        } else
            toast(MainActivity.fab.getContext(), MainActivity.fab.getContext().getString(R.string.toast_saved_txt));
    }

    public static Bitmap makeSquareImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int dimension = Math.max(width, height); // Square side

        Bitmap squaredImage = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squaredImage);

        // Draws original image centered inside the white square
        int left = (dimension - width) / 2;
        int top = (dimension - height) / 2;
        canvas.drawColor(Color.WHITE); // Fills the white backgrond
        canvas.drawBitmap(image, left, top, null); // Draws original image

        return squaredImage;
    }

    public static void mediaScanner(File file, Context context) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{file.getAbsolutePath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }

    public static String addSpacesAndNewLines(String input) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (i > 0 && i % 32 == 0) {  //Add a new line every 32 chars
                sb.append("\n");
                count = 0;
            } else if (count == 2) {  // Add a space every 2 chars
                sb.append(" ");
                count = 0;
            }
            sb.append(input.charAt(i));
            count++;
        }
        return sb.toString();
    }

    static void shareImage(List<GbcImage> gbcImages, Context context, boolean crop) {
        ArrayList<Uri> imageUris = new ArrayList<>();
        FileOutputStream fileOutputStream = null;

        try {
            for (int i = 0; i < gbcImages.size(); i++) {
                GbcImage gbcImage = gbcImages.get(i);
                Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());

                if (image.getHeight() == 144 && image.getWidth() == 160 && crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                //For the wild frames
                else if (image.getHeight() == 224 && crop) {
                    image = Bitmap.createBitmap(image, 16, 40, 128, 112);
                }
                //Rotate the image
                image = rotateBitmap(image, gbcImage);

                //Make square if checked in settings
                if (exportSquare) {
                    image = makeSquareImage(image);
                }
                image = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                File file = new File(context.getExternalCacheDir(), "shared_image_" + i + ".png");
                fileOutputStream = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();

                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                imageUris.add(uri);
            }

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/png");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share"));
        } catch (Exception e) {
            toast(context, "Exception");
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Average HDR method
     *
     * @param bitmaps List of bitmaps for the average
     * @return returns the averaged image
     */
    public static Bitmap averageImages(List<Bitmap> bitmaps) {
        if (bitmaps == null || bitmaps.isEmpty()) {
            throw new IllegalArgumentException("List of images cannot be empty.");
        }

        // Make sure all images have the same dimensions
        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();
        for (Bitmap bitmap : bitmaps) {
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                throw new IllegalArgumentException("All images must have same dimensions.");
            }
        }

        // Create a new Bitmap to store the combined image
        Bitmap combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create an array to store the values of the pixels of all images
        int numImages = bitmaps.size();
        int[][] pixelValues = new int[numImages][width * height];

        // Obtain the values of the pixels of all images
        for (int i = 0; i < numImages; i++) {
            bitmaps.get(i).getPixels(pixelValues[i], 0, width, 0, 0, width, height);
        }

        // Create a new array to store the average values of the combined pixels
        int[] combinedPixels = new int[width * height];

        // Calculate the average of each channel (red,green, blue) for each pixel
        for (int i = 0; i < width * height; i++) {
            int alpha = Color.alpha(pixelValues[0][i]); // Alfa channel is not changed
            int red = 0;
            int green = 0;
            int blue = 0;

            // Adds the value of the pixels for each color channel
            for (int j = 0; j < numImages; j++) {
                red += Color.red(pixelValues[j][i]);
                green += Color.green(pixelValues[j][i]);
                blue += Color.blue(pixelValues[j][i]);
            }

            // Calculates the average value of each color channel
            red /= numImages;
            green /= numImages;
            blue /= numImages;

            // Combines the values of the color channels to form the final pixel
            combinedPixels[i] = Color.argb(alpha, red, green, blue);
        }

        // Sets the combined pixels in the final Bitmap
        combinedBitmap.setPixels(combinedPixels, 0, width, 0, 0, width, height);

        return combinedBitmap;
    }


    public static String encodeData(String value) {
        byte[] inputBytes = value.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater();
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int length = deflater.deflate(buffer);
            outputStream.write(buffer, 0, length);
        }

        deflater.end();
        byte[] compressedBytes = outputStream.toByteArray();
        return new String(compressedBytes, StandardCharsets.ISO_8859_1);
    }

    public static void sortImages(Context context, DisplayMetrics displayMetrics) {
        SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.sort_dialog, null);
        Dialog dialog = new Dialog(context);
        dialog.setContentView(dialogView);
        int screenHeight = displayMetrics.heightPixels;
        int desiredHeight = (int) (screenHeight * 0.5);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, desiredHeight);

        RadioButton sortCreationDate = dialog.findViewById(R.id.rbSortDate);
        RadioButton sortTitle = dialog.findViewById(R.id.rbSortTitle);
        RadioButton sortImportDate = dialog.findViewById(R.id.rbSortImportDate);
        RadioButton sortAsc = dialog.findViewById(R.id.rbSortAsc);
        RadioButton sortDesc = dialog.findViewById(R.id.rbSortDesc);

        switch (sortModeEnum) {
            case CREATION_DATE:
                sortCreationDate.setChecked(true);
                break;

            case IMPORT_DATE:
                sortImportDate.setChecked(true);
                break;

            case TITLE:
                sortTitle.setChecked(true);
                break;
        }

        if (!sortDescending) {
            sortAsc.setChecked(true);
        } else {
            sortDesc.setChecked(true);
        }
        sortCreationDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortByDate(gbcImagesList, sortDescending);
                sortMode = CREATION_DATE.name();
                sortModeEnum = MainActivity.SORT_MODE.valueOf(sortMode);
                editor.putString("sort_by_date", CREATION_DATE.name());
                editor.apply();
                checkSorting(context);
                updateGridView();

            }
        });
        sortImportDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortByTitle(gbcImagesList, sortDescending);
                sortMode = IMPORT_DATE.name();
                sortModeEnum = MainActivity.SORT_MODE.valueOf(sortMode);
                editor.putString("sort_by_date", IMPORT_DATE.name());
                editor.apply();
                checkSorting(context);

            }
        });
        sortTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortByTitle(gbcImagesList, sortDescending);
                sortMode = TITLE.name();
                sortModeEnum = MainActivity.SORT_MODE.valueOf(sortMode);
                editor.putString("sort_by_date", TITLE.name());
                editor.apply();
                checkSorting(context);
                updateGridView();
            }
        });
        sortAsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortDescending = false;
                editor.putBoolean("sort_descending", false);
                editor.apply();
                checkSorting(context);
                updateGridView();

            }
        });
        sortDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortDescending = true;
                editor.putBoolean("sort_descending", true);
                editor.apply();
                checkSorting(context);
                updateGridView();

            }
        });
        dialog.show();
    }

    public static void checkSorting(Context context) {
        switch (sortModeEnum) {
            case CREATION_DATE:
                sortByDate(gbcImagesList, sortDescending);
                break;
            case IMPORT_DATE:
                LoadingDialog loadingDialog = new LoadingDialog(context, "");
                loadingDialog.showDialog();
                Thread thread = new Thread(() -> {

                    ImageDao imageDao = db.imageDao();

                    gbcImagesList = imageDao.getAll();
                    if (sortDescending) {
                        Collections.reverse(gbcImagesList);
                    }
                    galleryActivity.runOnUiThread(() -> {
                        updateGridView();
                        loadingDialog.dismissDialog();

                    });

                });
                thread.start();

                break;
            case TITLE:
                sortByTitle(gbcImagesList, sortDescending);

                break;
        }
    }

    @SuppressLint("ResourceAsColor")
    public static void showFilterDialog(Context context, LinkedHashSet<String> hashTags, DisplayMetrics displayMetrics) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.tags_dialog, null);

        TextView selectedTagsTextView = dialogView.findViewById(R.id.selectedTagsTextView);
        TextView hiddenTagsTv = dialogView.findViewById(R.id.hiddenTagsTv);

        Drawable drawableNotSelected = ContextCompat.getDrawable(context, R.drawable.ic_not_selected);

        Drawable drawableHidden = ContextCompat.getDrawable(context, R.drawable.ic_hidden_tag);
        drawableHidden.setColorFilter(context.getColor(R.color.listview_selected), PorterDuff.Mode.SRC_ATOP);

        Drawable drawableSelected = ContextCompat.getDrawable(context, R.drawable.ic_selected);
        drawableSelected.setColorFilter(context.getColor(R.color.save_color), PorterDuff.Mode.SRC_ATOP);

        Dialog dialog = new Dialog(context);
        dialog.setContentView(dialogView);
        int screenHeight = displayMetrics.heightPixels;
        int desiredHeight = (int) (screenHeight * 0.8);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, desiredHeight);

        LinearLayout buttonLayout = dialog.findViewById(R.id.buttonLayout);

        HashSet<String> selectedTags = new HashSet<>(selectedFilterTags);
        HashSet<String> hiddenTags = new HashSet<>(hiddenFilterTags);

        LinkedHashSet newTagsSetWithTopFavorite = new LinkedHashSet();
        newTagsSetWithTopFavorite.add("__filter:favourite__"); //adding it in case it doesn't exist, so it appears at the top with the comparator
        newTagsSetWithTopFavorite.addAll(hashTags);
        Iterator<String> tagIterator = newTagsSetWithTopFavorite.iterator();

        updateSelectedTagsText(selectedTagsTextView, hiddenTagsTv, selectedTags, hiddenTags);
        List<CheckBox> checkBoxList = new ArrayList<>();
        //Dynamically add checkboxes
        while (tagIterator.hasNext()) {

            String item = tagIterator.next();
            CheckBox checkBox = new CheckBox(context);
            checkBox.setButtonDrawable(drawableNotSelected);
            if (selectedTags.contains(item)) {
                checkBox.setButtonDrawable(drawableSelected);
            } else if (hiddenTags.contains(item)) {
                checkBox.setButtonDrawable(drawableHidden);
            }

            if (item.equals("__filter:favourite__")) {
                item = "Favourite \u2764\ufe0f";//The heart emoticon
            } else if (item.equals("__filter:duplicated__")) {
                item = "Duplicated \uD83D\uDC11";
            } else if (item.equals("__filter:transformed__")) {
                item = "Transformed \uD83D\uDD04";
            }

            checkBox.setCompoundDrawablePadding(10);
            checkBox.setText(item);
            checkBox.setTextSize(20);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String selectedTag = ((Button) v).getText().toString();

                    if (selectedTag.equals("Favourite \u2764\ufe0f")) {
                        selectedTag = "__filter:favourite__";//Reverse the tag
                    } else if (selectedTag.equals("Duplicated \uD83D\uDC11")) {
                        selectedTag = "__filter:duplicated__";
                    } else if (selectedTag.equals("Transformed \uD83D\uDD04")) {
                        selectedTag = "__filter:transformed__";
                    }

                    if (selectedTags.contains(selectedTag)) {
                        selectedTags.remove(selectedTag);
                        hiddenTags.add(selectedTag);

                        updateSelectedTagsText(selectedTagsTextView, hiddenTagsTv, selectedTags, hiddenTags);
                        checkBox.setButtonDrawable(drawableHidden);

                    } else {
                        if (hiddenTags.contains(selectedTag)) {
                            hiddenTags.remove(selectedTag);
                            checkBox.setButtonDrawable(drawableNotSelected);

                        } else {
                            selectedTags.add(selectedTag);
                            checkBox.setButtonDrawable(drawableSelected);
                        }
                        updateSelectedTagsText(selectedTagsTextView, hiddenTagsTv, selectedTags, hiddenTags);
                    }
                }
            });
            checkBoxList.add(checkBox);
            buttonLayout.addView(checkBox);
        }

        Button btnClear = dialog.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            selectedTags.clear();
            hiddenTags.clear();
            for (CheckBox cb : checkBoxList) {
                cb.setButtonDrawable(drawableNotSelected);
            }
            updateSelectedTagsText(selectedTagsTextView, hiddenTagsTv, selectedTags, hiddenTags);

        });

        Button btnAccept = dialog.findViewById(R.id.btnAccept);
        btnAccept.setOnClickListener(v -> {

            selectedFilterTags = selectedTags;
            Gson gson = new Gson();
            MainActivity.selectedTags = gson.toJson(selectedTags);
            hiddenFilterTags = hiddenTags;
            MainActivity.hiddenTags = gson.toJson(hiddenTags);

            saveTagsSet(selectedTags, false);
            saveTagsSet(hiddenTags, true);
            editor.putInt("current_page", 0).apply();
            currentPage = 0;
            updateGridView();

            dialog.dismiss();
        });

        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * To check the tags after deleting images
     */
    public static void reloadTags() {
        retrieveTags(gbcImagesList);
        //If the list of tags contains a tag that doesn't exist anymore, delete it from the list
        List<String> selectedTagsToDeleteHolder = new ArrayList<>();
        List<String> hiddenTagsToDeleteHolder = new ArrayList<>();
        for (String tag : selectedFilterTags) {
            if (!tagsHash.contains(tag)) {
                selectedTagsToDeleteHolder.add(tag);
            }
        }
        for (String tag : hiddenFilterTags) {
            if (!tagsHash.contains(tag)) {
                hiddenTagsToDeleteHolder.add(tag);
            }
        }

        //To not have concurrency
        for (String st : selectedTagsToDeleteHolder) {
            if (selectedFilterTags.contains(st)) {
                selectedFilterTags.remove(st);
            }
        }
        for (String st : hiddenTagsToDeleteHolder) {
            if (hiddenFilterTags.contains(st)) {
                hiddenFilterTags.remove(st);
            }
        }
        saveTagsSet(selectedFilterTags, false);
        saveTagsSet(hiddenFilterTags, true);
    }

    /**
     * Updates the textview with the selected tags
     *
     * @param selectedTagsTv
     * @param selectedTags
     */
    public static void updateSelectedTagsText(TextView selectedTagsTv, TextView hiddenTagsTV, HashSet<String> selectedTags, HashSet<String> notShowingTags) {
        StringBuilder selectedTagsBuilder = new StringBuilder();
        for (String tag : selectedTags) {
            if (tag.equals("__filter:favourite__")) {
                tag = "Favourite \u2764\ufe0f";
            } else if (tag.equals("__filter:duplicated__")) {
                tag = "Duplicated \uD83D\uDC11";
            } else if (tag.equals("__filter:transformed__")) {
                tag = "Transformed \uD83D\uDD04";
            }
            selectedTagsBuilder.append(tag).append(", ");
        }

        StringBuilder notShowingTagsSB = new StringBuilder();
        for (String tag : notShowingTags) {
            if (tag.equals("__filter:favourite__")) {
                tag = "Favourite \u2764\ufe0f";
            }
            if (tag.equals("__filter:duplicated__")) {
                tag = "Duplicated \uD83D\uDC11";
            } else if (tag.equals("__filter:transformed__")) {
                tag = "Transformed \uD83D\uDD04";
            }
            notShowingTagsSB.append(tag).append(", ");
        }
        String notShowingString = notShowingTagsSB.toString();
        if (!notShowingString.isEmpty()) {
            notShowingString = notShowingString.substring(0, notShowingString.length() - 2);
        }
        SpannableString string = new SpannableString(notShowingString);
        string.setSpan(new StrikethroughSpan(), 0, notShowingString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        String selectedTagsText = selectedTagsBuilder.toString();
        if (!selectedTagsText.isEmpty()) {
            selectedTagsText = selectedTagsText.substring(0, selectedTagsText.length() - 2);
        }

        selectedTagsTv.setText(selectedTagsText);
        hiddenTagsTV.setText(string);
    }

    @SuppressLint("NewApi")
    public static void sortByDate(List<GbcImage> gbcImagesList, boolean descending) {
        Comparator<GbcImage> comparator = Comparator.comparing(GbcImage::getCreationDate);
        if (descending) {
            comparator = comparator.reversed();
        }
        Collections.sort(gbcImagesList, comparator);
    }

    @SuppressLint("NewApi")
    public static void sortByTitle(List<GbcImage> gbcImagesList, boolean descending) {
        Comparator<GbcImage> comparator = new Comparator<GbcImage>() {
            @Override
            public int compare(GbcImage image1, GbcImage image2) {
                int titleComparison = image1.getName().compareTo(image2.getName());
                // If names are the same, compare by date
                if (titleComparison == 0) {
                    return image1.getCreationDate().compareTo(image2.getCreationDate());
                }
                return titleComparison;
            }
        };

        if (descending) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(gbcImagesList, comparator);
    }


    public static boolean compareTags(List<String> list1, List<String> list2) {
        boolean editingTags = false;
        if (list1.size() != list2.size()) {
            editingTags = true;
        } else {
            for (String st : list1) {
                if (!list2.contains(st)) {
                    editingTags = true;
                    break;//Don't keep checking
                }
            }
        }
        return editingTags;
    }

    public static Bitmap frameChange(GbcImage gbcImage, String frameId, boolean invertImagePalette,
                                     boolean invertFramePalette, boolean keepFrame, Boolean save) throws IOException {
        Bitmap resultBitmap;
        gbcImage.setFrameId(frameId);
        GbcFrame gbcFrame = hashFrames.get(frameId);

        //If image has a null frame but has the size of a "framable" image, create the placeholder frame
        if (gbcFrame == null && keepFrame && ((gbcImage.getImageBytes().length / 40) == 144 || (gbcImage.getImageBytes().length / 40) == 224)) {
            Bitmap originalBwBitmap = paletteChanger("bw", gbcImage.getImageBytes(), false, false);

            gbcFrame = new GbcFrame();
            gbcFrame.setFrameBitmap(originalBwBitmap);
            gbcFrame.setWildFrame(originalBwBitmap.getHeight() == 144 ? false : true);
        }

        if (gbcFrame != null && ((gbcImage.getImageBytes().length / 40) == 144 || (gbcImage.getImageBytes().length / 40) == 224)) {

            int yIndexActualImage = 16;// y Index where the actual image starts
            if ((gbcImage.getImageBytes().length / 40) == 224) {
                yIndexActualImage = 40;
            }
            int yIndexNewFrame = 16;
            boolean isWildFrameNow = gbcFrame.isWildFrame();
            if (isWildFrameNow) yIndexNewFrame = 40;

            Bitmap framed = gbcFrame.getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
            resultBitmap = Bitmap.createBitmap(framed.getWidth(), framed.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(resultBitmap);
            String paletteId = gbcImage.getPaletteId();
            if (save != null && !save) //In the cases I don't need to save it, the palette is bw (Hex, json exports, paperize, printing)
                paletteId = "bw";
            Bitmap setToPalette = paletteChanger(paletteId, gbcImage.getImageBytes(), keepFrame, invertImagePalette);
            Bitmap croppedBitmap = Bitmap.createBitmap(setToPalette, 16, yIndexActualImage, 128, 112); //Getting the internal 128x112 image
            canvas.drawBitmap(croppedBitmap, 16, yIndexNewFrame, null);
            String framePaletteId = gbcImage.getFramePaletteId();
            if (!keepFrame) {
                framePaletteId = gbcImage.getPaletteId();
                invertFramePalette = gbcImage.isInvertPalette();
            }

            if (save != null && !save) //In the cases I don't need to save it, the palette is bw (Hex, json exports, paperize, printing)
                framePaletteId = "bw";

            byte[] frameBytes = gbcFrame.getFrameBytes();
            if (frameBytes == null) {
                try {
                    frameBytes = Utils.encodeImage(gbcFrame.getFrameBitmap(), "bw");
                    gbcFrame.setFrameBytes(frameBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            framed = paletteChanger(framePaletteId, frameBytes, true, invertFramePalette);
            framed = transparentBitmap(framed, gbcFrame);

            canvas.drawBitmap(framed, 0, 0, null);
        } else {
            gbcImage.setFrameId(frameId);
            String imagePaletteId = gbcImage.getPaletteId();
            if (save != null && !save) //In the cases I don't need to save it, the palette is bw (Hex, json exports, paperize, printing)
                imagePaletteId = "bw";
            resultBitmap = paletteChanger(imagePaletteId, gbcImage.getImageBytes(), keepFrame, invertImagePalette);
        }
        //Because when exporting to json, hex or printing I use this method but don't want to keep the changes
        if (save != null && save) {
            diskCache.put(gbcImage.getHashCode(), resultBitmap);
            new UpdateImageAsyncTask(gbcImage).execute();
        }
        return resultBitmap;
    }

    //Change palette
    public static Bitmap paletteChanger(String paletteId, byte[] imageBytes, boolean keepFrame,
                                        boolean invertPalette) {
        ImageCodec imageCodec = new ImageCodec(160, imageBytes.length / 40);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(paletteId).getPaletteColorsInt(), imageBytes, invertPalette);

        return image;
    }

}

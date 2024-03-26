package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.MainActivity.exportSize;
import static com.mraulio.gbcameramanager.MainActivity.lastSeenGalleryImage;
import static com.mraulio.gbcameramanager.MainActivity.showEditMenuButton;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.averageImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.encodeData;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.showFilterDialog;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.sortImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.stitchImages;

import static com.mraulio.gbcameramanager.ui.gallery.MainImageDialog.newPosition;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.getHiddenTags;
import static com.mraulio.gbcameramanager.utils.Utils.getSelectedTags;
import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;
import static com.mraulio.gbcameramanager.utils.Utils.tagsHash;
import static com.mraulio.gbcameramanager.utils.Utils.toast;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.utils.AnimatedGifEncoder;
import com.mraulio.gbcameramanager.utils.DiskCache;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.Result;

import pl.droidsonroids.gif.GifDrawable;

public class GalleryFragment extends Fragment {
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    static UsbDeviceConnection connection;
    static UsbSerialPort port = null;
    public static GridView gridView;
    static AlertDialog loadingDialog;
    static SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
    static HashSet<String> selectedFilterTags = new HashSet<>();
    static HashSet<String> hiddenFilterTags = new HashSet<>();
    static List<GbcImage> filteredGbcImages = new ArrayList<>();
    static boolean updatingFromChangeImage = false;
    static List<Integer> selectedImages = new ArrayList<>();
    static StringBuilder sbTitle = new StringBuilder();
    static int itemsPerPage = MainActivity.imagesPage;
    static int startIndex = 0;
    static int endIndex = 0;

    public static int currentPage;
    static int lastPage = 0;
    public static TextView tvResponseBytes;
    static boolean crop = false;
    boolean showPalettes = true;
    static TextView tv_page;
    boolean keepFrame = false;
    public static CustomGridViewAdapterImage customGridViewAdapterImage;
    static List<Bitmap> imagesForPage;
    static List<GbcImage> gbcImagesForPage;
    public static TextView tv;
    DisplayMetrics displayMetrics;
    public static DiskCache diskCache;

    static boolean[] selectionMode = {false};
    static boolean alreadyMultiSelect = false;
    static AlertDialog deleteDialog;

    public GalleryFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        MainActivity.currentFragment = MainActivity.CURRENT_FRAGMENT.GALLERY;
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        tv = view.findViewById(R.id.text_gallery);
        gridView = view.findViewById(R.id.gridView);
        loadingDialog = Utils.loadingDialog(getContext(), null);
        setHasOptionsMenu(true);
        diskCache = new DiskCache(getContext());

        Button btnPrevPage = view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = view.findViewById(R.id.btnNextPage);
        Button btnFirstPage = view.findViewById(R.id.btnFirstPage);
        Button btnLastPage = view.findViewById(R.id.btnLastPage);

        tv_page = view.findViewById(R.id.tv_page);

        tv_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog spinnerDialog = numberPickerPageDialog(getContext());

                spinnerDialog.show();
            }
        });

        view.setOnTouchListener(new OnSwipeTouchListener.OnSwipesTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                nextPage();
            }

            @Override
            public void onSwipeRight() {
                prevPage();
            }
        });

        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevPage();
            }
        });

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage();
            }
        });
        btnFirstPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage > 0) {
                    currentPage = 0;
                    updateGridView();
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                    editor.putInt("current_page", currentPage);
                    editor.apply();
                }
            }

        });
        btnLastPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage < lastPage) {
                    currentPage = lastPage;
                    updateGridView();
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                    editor.putInt("current_page", currentPage);
                    editor.apply();
                }
            }
        });

        /**
         * Dialog when clicking an image
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (!selectionMode[0]) {
                    MainImageDialog mainImageDialog = new MainImageDialog(selectionMode[0], gridView, crop, keepFrame, currentPage, lastPage, position,
                            itemsPerPage, filteredGbcImages, lastSeenGalleryImage, getContext(), displayMetrics, showPalettes, getActivity(),
                            port, usbIoManager, tvResponseBytes, connection, tv, manager, null, null);
                    mainImageDialog.showImageDialog();
                } else {
                    int globalImageIndex;
                    if (currentPage != lastPage) {
                        globalImageIndex = position + (currentPage * itemsPerPage);
                    } else {
                        globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
                    }
                    if (selectedImages.contains(globalImageIndex)) {
                        selectedImages.remove(Integer.valueOf(globalImageIndex));

                    } else if (!selectedImages.contains(globalImageIndex)) {
                        selectedImages.add(globalImageIndex);
                    }
                    if (selectedImages.size() == 0) {
                        hideSelectionOptions();
                    } else {
                        updateTitleText();
                        if (selectedImages.size() > 1) {
                            showEditMenuButton = true;
                        } else {
                            showEditMenuButton = false;
                        }
                        getActivity().invalidateOptionsMenu();
                    }
                    customGridViewAdapterImage.notifyDataSetChanged();
                }
            }
        });
        //LongPress on an image start selection Mode
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (!selectionMode[0]) MainActivity.fab.show();

                //I have to do this here, on onCreateView there was a crash
                if (MainActivity.fab != null && !MainActivity.fab.hasOnClickListeners()) {
                    MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideSelectionOptions();
                        }
                    });
                }
                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
                }
                if (selectionMode[0]) {

                    Collections.sort(selectedImages);

                    int firstImage = selectedImages.get(0);
                    selectedImages.clear();
                    selectedImages.add(globalImageIndex);
                    if (firstImage < globalImageIndex) {
                        selectedImages.clear();
                        for (int i = firstImage; i < globalImageIndex; i++) {
                            if (!selectedImages.contains(i)) {
                                selectedImages.add(i);
                            }
                        }
                        selectedImages.add(globalImageIndex);
                    } else if (firstImage > globalImageIndex) {
                        for (int i = firstImage; i > globalImageIndex; i--) {
                            if (!selectedImages.contains(i)) {
                                selectedImages.add(i);
                            }
                        }
                    }
                    alreadyMultiSelect = true;
                    if (selectedImages.size() > 1) {
                        showEditMenuButton = true;
                    } else {
                        showEditMenuButton = false;
                    }
                    getActivity().invalidateOptionsMenu();
                    updateTitleText();

                } else {
                    selectedImages.add(globalImageIndex);
                    selectionMode[0] = true;
                    alreadyMultiSelect = false;
                    updateTitleText();
                }
                customGridViewAdapterImage.notifyDataSetChanged();

                return true;
            }
        });

        if (MainActivity.doneLoading) updateFromMain();

        return view;
    }

    private static void updateTitleText() {
        if (!selectedFilterTags.isEmpty() || !hiddenFilterTags.isEmpty()) {
            sbTitle.append(tv.getContext().getString(R.string.filtered_images) + filteredGbcImages.size());
        } else {
            sbTitle.append(tv.getContext().getString(R.string.total_images) + filteredGbcImages.size());
        }
        if (selectedImages.size() > 0) {
            sbTitle.append("  " + tv.getContext().getString(R.string.selected_images) + selectedImages.size());
        }
        tv.setText(sbTitle.toString());
        sbTitle.setLength(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_multi_edit:
                if (selectionMode[0] && selectedImages.size() > 1) {
                    MainImageDialog mainImageDialog = new MainImageDialog(selectionMode[0], gridView, crop, keepFrame, currentPage, lastPage, 0,
                            itemsPerPage, filteredGbcImages, lastSeenGalleryImage, getContext(), displayMetrics, showPalettes, getActivity(),
                            port, usbIoManager, tvResponseBytes, connection, tv, manager, selectedImages, customGridViewAdapterImage);
                    mainImageDialog.showImageDialog();
                } else Utils.toast(getContext(), getString(R.string.select_minimum_toast));

                return true;

            case R.id.action_filter_tags:
                if (selectionMode[0]) {
                    Utils.toast(getContext(), getString(R.string.unselect_all_toast));
                } else {
                    showFilterDialog(getContext(), tagsHash, displayMetrics);
                }
                return true;

            case R.id.action_sort:
                if (selectionMode[0]) {
                    Utils.toast(getContext(), getString(R.string.unselect_all_toast));
                } else {
                    sortImages(getContext(), displayMetrics);
                }
                return true;

            case R.id.action_stitch:
                if (!selectedImages.isEmpty()) {
                    //If there are too many images selected, the resulting image to show will be too big (because of the *6 in the ImageView)
                    if (selectedImages.size() > 40) {
                        toast(getContext(), getString(R.string.stitch_too_many_images));
                        return true;
                    }
                    final Bitmap[] stitchedImage = new Bitmap[1];
                    final boolean[] stitchBottom = {true};
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    List<Bitmap> stitchBitmapList = new ArrayList<>();
                    List<GbcImage> stitchGbcImage = new ArrayList<>();
                    View stitchView = inflater.inflate(R.layout.stitch_dialog, null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    ImageView imageView = stitchView.findViewById(R.id.iv_stitch);
                    GridView gridViewStitch = stitchView.findViewById(R.id.gridViewStitch);
                    RadioButton rbStitchBottom = stitchView.findViewById(R.id.rbBottom);
                    RadioButton rbStitchRight = stitchView.findViewById(R.id.rbRight);

                    rbStitchBottom.setChecked(true);
                    rbStitchBottom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stitchBottom[0] = true;
                            stitchedImage[0] = stitchImages(stitchBitmapList, stitchBottom[0]);
                            Bitmap bitmap = Bitmap.createScaledBitmap(stitchedImage[0], stitchedImage[0].getWidth() * 5, stitchedImage[0].getHeight() * 5, false);
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                    rbStitchRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            stitchBottom[0] = false;
                            stitchedImage[0] = stitchImages(stitchBitmapList, stitchBottom[0]);
                            Bitmap bitmap = Bitmap.createScaledBitmap(stitchedImage[0], stitchedImage[0].getWidth() * 5, stitchedImage[0].getHeight() * 5, false);
                            imageView.setImageBitmap(bitmap);
                        }
                    });

                    builder.setView(stitchView);
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                        stitchGbcImage.add(filteredGbcImages.get(i));

                    }
                    builder.setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                        LocalDateTime now = null;
                        Date nowDate = new Date();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            now = LocalDateTime.now();
                        }
                        File file = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

                            file = new File(Utils.IMAGES_FOLDER, "Stitch_" + dtf.format(now) + ".png");
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                            file = new File(Utils.IMAGES_FOLDER, "Stitch_" + sdf.format(nowDate) + ".png");
                        }
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            Bitmap bitmap = Bitmap.createScaledBitmap(stitchedImage[0], stitchedImage[0].getWidth() * exportSize, stitchedImage[0].getHeight() * exportSize, false);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + getString(R.string.stitch), Toast.LENGTH_LONG);
                            toast.show();
                            mediaScanner(file, getContext());
                            showNotification(getContext(), file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    });

                    loadingDialog.show();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {
                            gridViewStitch.setAdapter(new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, stitchGbcImage, stitchBitmapList, false, false, false, null));

                            for (int i : selectedImages) {
                                Bitmap image = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
//                                image = rotateBitmap(image, (filteredGbcImages.get(i)));//Not rotating them now
                                stitchBitmapList.add(image);
                            }
                            try {
                                stitchedImage[0] = stitchImages(stitchBitmapList, stitchBottom[0]);
                                Bitmap bitmap = Bitmap.createScaledBitmap(stitchedImage[0], stitchedImage[0].getWidth() * 5, stitchedImage[0].getHeight() * 5, false);
                                imageView.setImageBitmap(bitmap);
                                builder.setView(stitchView);

                                AlertDialog dialog = builder.create();
                                dialog.show();
                            } catch (IllegalArgumentException e) {
                                Utils.toast(getContext(), getString(R.string.hdr_exception));
                            }
                        }
                    });
                    asyncTask.execute();
                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;

            case R.id.action_clone:
                if (selectionMode[0]) {
                    List<GbcImage> gbcImagesToClone = new ArrayList<>();
                    Collections.sort(selectedImages);
                    for (int i : selectedImages) {
                        gbcImagesToClone.add(filteredGbcImages.get(i));
                    }
                    List<GbcImage> clonedImages = new ArrayList<>();
                    List<Bitmap> clonedBitmaps = new ArrayList<>();
                    for (GbcImage gbcImage : gbcImagesToClone) {
                        GbcImage clonedImage = gbcImage.clone();
                        long timeMs = System.currentTimeMillis();
                        String timeString = String.valueOf(timeMs);
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
                            modifiedString.append("clonedBadLength" + System.currentTimeMillis());
                        }
                        String clonedHash = modifiedString.toString();
                        clonedImage.setHashCode(clonedHash);

                        HashSet tags = new HashSet(clonedImage.getTags());
                        tags.add("Cloned");
                        clonedImage.setTags(tags);
                        clonedImages.add(clonedImage);
                        Bitmap originalBitmap = imageBitmapCache.get(gbcImage.getHashCode());
                        Bitmap clonedBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                        clonedBitmaps.add(clonedBitmap);
                    }
                    new SaveImageAsyncTask(clonedImages, clonedBitmaps, getContext(), null, 0, customGridViewAdapterImage).execute();
                }
                return true;

            case R.id.action_delete:
                if (!selectedImages.isEmpty()) {
                    Collections.sort(selectedImages);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getString(R.string.delete_all_title));

                    GridView deleteImageGridView = new GridView(getContext());
                    deleteImageGridView.setNumColumns(4);
                    deleteImageGridView.setPadding(30, 10, 30, 10);
                    List<Bitmap> deleteBitmapList = new ArrayList<>();
                    List<GbcImage> deleteGbcImage = new ArrayList<>();
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                        deleteGbcImage.add(filteredGbcImages.get(i));
                    }

                    builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadingDialog.show();
                            new DeleteImageAsyncTask(selectedImages, getActivity()).execute();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //No action
                        }
                    });
                    loadingDialog.show();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {
                            for (int i : selectedImages) {
                                deleteBitmapList.add(imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()));
                            }
                            deleteImageGridView.setAdapter(new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, deleteGbcImage, deleteBitmapList, false, false, false, null));
                            builder.setView(deleteImageGridView);
                            deleteDialog = builder.create();
                            deleteDialog.show();

                        }
                    });
                    asyncTask.execute();

                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;
            case R.id.action_average:
                if (!selectedImages.isEmpty()) {
                    crop = false;
                    final Bitmap[] averaged = new Bitmap[1];
                    LayoutInflater inflater = LayoutInflater.from(getContext());

                    View averageView = inflater.inflate(R.layout.average_dialog, null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    builder.setView(averageView);

                    CheckBox cbAverageCrop = averageView.findViewById(R.id.cb_average_crop);
                    ImageView imageView = averageView.findViewById(R.id.iv_average);
                    cbAverageCrop.setOnClickListener(v -> {
                        if (!crop) {
                            crop = true;
                        } else {
                            crop = false;
                        }
                    });
                    builder.setTitle("HDR!");

                    imageView.setPadding(10, 10, 10, 10);
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }
                    builder.setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                        LocalDateTime now = null;
                        Date nowDate = new Date();
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            now = LocalDateTime.now();
                        }
                        File file = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

                            file = new File(Utils.IMAGES_FOLDER, "HDR" + dtf.format(now) + ".png");
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                            file = new File(Utils.IMAGES_FOLDER, "HDR" + sdf.format(nowDate) + ".png");

                        }
                        //Regular image
                        if (averaged[0].getHeight() == 144 * 6 && averaged[0].getWidth() == 160 * 6 && crop) {
                            averaged[0] = Bitmap.createBitmap(averaged[0], 16 * 6, 16 * 6, 128 * 6, 112 * 6);
                        }
                        //Rotated image
                        else if (averaged[0].getHeight() == 160 * 6 && averaged[0].getWidth() == 144 * 6 && crop) {
                            averaged[0] = Bitmap.createBitmap(averaged[0], 16 * 6, 16 * 6, 112 * 6, 128 * 6);
                        }
                        //Regular Wild frame
                        else if (averaged[0].getHeight() == 224 * 6 && averaged[0].getWidth() == 160 * 6 && crop) {
                            averaged[0] = Bitmap.createBitmap(averaged[0], 16 * 6, 40 * 6, 128 * 6, 112 * 6);
                        }
                        //Rotated Wild frame
                        else if (averaged[0].getHeight() == 160 * 6 && averaged[0].getWidth() == 224 * 6 && crop) {
                            averaged[0] = Bitmap.createBitmap(averaged[0], 40 * 6, 16 * 6, 112 * 6, 128 * 6);
                        }
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            averaged[0].compress(Bitmap.CompressFormat.PNG, 100, out);
                            Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + "HDR!", Toast.LENGTH_LONG);
                            toast.show();
                            mediaScanner(file, getContext());
                            showNotification(getContext(), file);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    });
                    loadingDialog.show();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {
                            List<Bitmap> listBitmaps = new ArrayList<>();

                            for (int i : selectedImages) {
                                Bitmap image = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
                                image = rotateBitmap(image, (filteredGbcImages.get(i)));
                                listBitmaps.add(image);

                            }
                            try {
                                Bitmap bitmap = averageImages(listBitmaps);
                                averaged[0] = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false);
                                imageView.setImageBitmap(averaged[0]);

                                AlertDialog dialog = builder.create();
                                dialog.show();
                            } catch (IllegalArgumentException e) {
                                Utils.toast(getContext(), getString(R.string.hdr_exception));
                            }
                        }
                    });
                    asyncTask.execute();
                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;
            case R.id.action_gif:
                //Using this library https://github.com/nbadal/android-gif-encoder

                if (!selectedImages.isEmpty()) {
                    List<Integer> sortedList = new ArrayList<>(selectedImages);
                    final List<Integer>[] listInUse = new List[]{selectedImages};
                    Collections.sort(sortedList);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("GIF!");

                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View dialogView = inflater.inflate(R.layout.animation_dialog, null);

                    builder.setView(dialogView);
                    TextView tv_animation = dialogView.findViewById(R.id.tv_animation);
                    Button reload_anim = dialogView.findViewById(R.id.btnReload);
                    CheckBox cb_loop = dialogView.findViewById(R.id.cb_loop);
                    CheckBox cbSort = dialogView.findViewById(R.id.cbSort);
                    final int[] loop = {0};
                    cb_loop.setOnClickListener(v -> {
                        if (cb_loop.isChecked()) {
                            loop[0] = 0;//0 to infinite loop
                        } else {
                            loop[0] = -1;//-1 to not repeat
                        }
                    });

                    ImageView imageView = dialogView.findViewById(R.id.animation_image);
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(30, 10, 30, 10);
                    SeekBar seekBar = dialogView.findViewById(R.id.animation_seekbar);
                    final int[] fps = {10};
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            fps[0] = progress;
                            tv_animation.setText(fps[0] + " fps");

                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });


                    builder.setPositiveButton(getString(R.string.btn_save), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LocalDateTime now = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                now = LocalDateTime.now();
                            }
                            Date nowDate = new Date();
                            File gifFile = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                                gifFile = new File(Utils.IMAGES_FOLDER, "GIF_" + dtf.format(now) + ".gif");
                            } else {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                                gifFile = new File(Utils.IMAGES_FOLDER, "GIF_" + sdf.format(nowDate) + ".gif");

                            }

                            try (FileOutputStream out = new FileOutputStream(gifFile)) {

                                out.write(bos.toByteArray());
                                mediaScanner(gifFile, getContext());
                                showNotification(getContext(), gifFile);
                                Utils.toast(getContext(), getString(R.string.toast_saved) + "GIF!");

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }
                    reload_anim.setOnClickListener(v -> {
                        loadingDialog.show();
                        LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, new AsyncTaskCompleteListener<Result>() {
                            @Override
                            public void onTaskComplete(Result result) {
                                bos.reset();
                                AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                                encoder.setRepeat(loop[0]);
                                encoder.setFrameRate(fps[0]);
                                encoder.start(bos);
                                List<Bitmap> bitmapList = new ArrayList<Bitmap>();

                                if (cbSort.isChecked())
                                    listInUse[0] = sortedList;
                                else {
                                    listInUse[0] = selectedImages;
                                }
                                for (int i : listInUse[0]) {
                                    Bitmap bitmap = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
                                    bitmap = rotateBitmap(bitmap, (filteredGbcImages.get(i)));
                                    bitmapList.add(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 4, bitmap.getHeight() * 4, false));
                                }

                                for (Bitmap bitmap : bitmapList) {
                                    encoder.addFrame(bitmap);
                                }
                                encoder.finish();
                                byte[] gifBytes = bos.toByteArray();
                                GifDrawable gifDrawable = null;
                                try {
                                    gifDrawable = new GifDrawable(gifBytes);
                                    gifDrawable.start(); // Starts the animation
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                imageView.setImageDrawable(gifDrawable);
                            }
                        });
                        asyncTask.execute();
                    });
                    loadingDialog.show();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, result -> {
                        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                        encoder.setRepeat(loop[0]);
                        encoder.setFrameRate(fps[0]);
                        encoder.start(bos);
                        List<Bitmap> bitmapList = new ArrayList<Bitmap>();

                        for (int i : selectedImages) {
                            Bitmap bitmap = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
                            bitmap = rotateBitmap(bitmap, (filteredGbcImages.get(i)));
                            bitmapList.add(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 4, bitmap.getHeight() * 4, false));
                        }

                        for (Bitmap bitmap : bitmapList) {
                            encoder.addFrame(bitmap);
                        }
                        encoder.finish();
                        byte[] gifBytes = bos.toByteArray();
                        GifDrawable gifDrawable = null;
                        try {
                            gifDrawable = new GifDrawable(gifBytes);
                            gifDrawable.start(); // Starts the animation
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        imageView.setImageDrawable(gifDrawable);

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    });
                    asyncTask.execute();

                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;
            case R.id.action_json:
                if (!selectedImages.isEmpty()) {
                    Collections.sort(selectedImages);
                    JSONObject jsonObject = new JSONObject();
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }
                    loadingDialog.show();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, result -> {

                        try {
                            JSONObject stateObject = new JSONObject();
                            JSONArray imagesArray = new JSONArray();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                            for (int i = 0; i < selectedImages.size(); i++) {
                                GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));
                                JSONObject imageObject = new JSONObject();
                                imageObject.put("hash", gbcImage.getHashCode());
                                imageObject.put("created", sdf.format(gbcImage.getCreationDate()));
                                imageObject.put("title", gbcImage.getName());
                                imageObject.put("tags", new JSONArray(gbcImage.getTags()));
                                imageObject.put("palette", gbcImage.getPaletteId());
                                imageObject.put("framePalette", gbcImage.getFramePaletteId());
                                imageObject.put("invertFramePalette", gbcImage.isInvertFramePalette());
                                imageObject.put("frame", gbcImage.getFrameId());
                                imageObject.put("invertPalette", gbcImage.isInvertPalette());
                                imageObject.put("lockFrame", gbcImage.isLockFrame());
                                imageObject.put("rotation", gbcImage.getRotation());
                                imagesArray.put(imageObject);
                            }
                            stateObject.put("images", imagesArray);
                            stateObject.put("lastUpdateUTC", System.currentTimeMillis() / 1000);
                            jsonObject.put("state", stateObject);
                            for (int i = 0; i < selectedImages.size(); i++) {
                                GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));
                                String txt = Utils.bytesToHex(gbcImage.getImageBytes());//Sending the original image bytes, not the one with the actual frame
                                StringBuilder sb = new StringBuilder();
                                for (int j = 0; j < txt.length(); j++) {
                                    if (j > 0 && j % 32 == 0) {
                                        sb.append("\n");
                                    }
                                    sb.append(txt.charAt(j));
                                }
                                String tileData = sb.toString();
                                String deflated = encodeData(tileData);
                                jsonObject.put(gbcImage.getHashCode(), deflated);

                            }
                            String jsonString = jsonObject.toString(2);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());

                            String fileName = "imagesJson" + dateFormat.format(new Date()) + ".json";

                            File file = new File(Utils.IMAGES_JSON, fileName);

                            try (FileWriter fileWriter = new FileWriter(file)) {
                                fileWriter.write(jsonString);
                                Utils.toast(getContext(), getString(R.string.json_backup_saved));
                                showNotification(getContext(), file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                    asyncTask.execute();
                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;
            default:
                break;
        }
        return false;
    }

//    private void connect() {
//        manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
//        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
//        if (availableDrivers.isEmpty()) {
//            return;
//        }
//        // Open a connection to the first available driver.
//        UsbSerialDriver driver = availableDrivers.get(0);
//        connection = manager.openDevice(driver.getDevice());
//
//        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
//        try {
//            if (port.isOpen()) port.close();
//            port.open(connection);
//            port.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//
//        } catch (Exception e) {
//            tv.append(e.toString());
//            Toast.makeText(getContext(), "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();
//        }
//
//        usbIoManager = new SerialInputOutputManager(port, this);
//    }

    public static Bitmap frameChange(GbcImage gbcImage, String frameId,
                                     boolean invertImagePalette, boolean invertFramePalette, boolean keepFrame, Boolean save) throws
            IOException {
        Bitmap resultBitmap;
        gbcImage.setFrameId(frameId);
        GbcFrame gbcFrame = Utils.hashFrames.get(frameId);
        if ((gbcImage.getImageBytes().length / 40) == 144 && gbcFrame != null || (gbcImage.getImageBytes().length / 40) == 224 && gbcFrame != null) {
//            boolean wasWildFrame = Utils.hashFrames.get(gbcImage.getFrameId()).isWildFrame();
            //To safecheck, maybe it's an image added with a wild frame size
//            if (!wasWildFrame && (gbcImage.getImageBytes().length / 40) == 224) {
//                wasWildFrame = true;
//            }
//            if (wasWildFrame)
//                yIndexActualImage= 40;
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
            framed = transparentBitmap(framed, Utils.hashFrames.get(gbcImage.getFrameId()));

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
        ImageCodec imageCodec = new ImageCodec(160, imageBytes.length / 40, keepFrame);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(paletteId).getPaletteColorsInt(), imageBytes, invertPalette);

        return image;
    }

    public static void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGridView();
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            editor.putInt("current_page", currentPage);
            editor.apply();
        }
    }

    public static void nextPage() {
        if (currentPage < lastPage) {
            currentPage++;
            updateGridView();
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            editor.putInt("current_page", currentPage);
            editor.apply();
        }
    }

    private AlertDialog numberPickerPageDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        NumberPicker numberPicker = new NumberPicker(context);
        builder.setTitle(getString(R.string.page_selector_dialog));
        builder.setView(numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(lastPage + 1);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setValue(currentPage + 1);

        // Disable keyboard
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        AlertDialog dialog = builder.create();

        numberPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedValue = numberPicker.getValue();

                if (selectedValue != currentPage + 1) {
                    currentPage = selectedValue - 1;
                    updateGridView();
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                }
                dialog.hide();
            }
        });
        return dialog;
    }

    public void updateFromMain() {
        if (Utils.gbcImagesList.size() > 0) {
            retrieveTags(gbcImagesList);
            checkSorting();
            selectedFilterTags = getSelectedTags();
            hiddenFilterTags = getHiddenTags();
            updateGridView();
            updateTitleText();

            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));

        } else {
            tv.setText(tv.getContext().getString(R.string.no_images));
        }
    }

    //Method to update the gallery gridview
    public static void updateGridView() {
        //Bitmap list to store current page bitmaps
        filteredGbcImages = new ArrayList<>();

        if (selectedFilterTags.isEmpty() && hiddenFilterTags.isEmpty()) {
            filteredGbcImages = Utils.gbcImagesList;
        } else {
            filteredGbcImages.clear();
            for (GbcImage gbcImageToFilter : Utils.gbcImagesList) {
                boolean containsAllTags = true;
                for (String tag : selectedFilterTags) {
                    if (!gbcImageToFilter.getTags().contains(tag)) {
                        containsAllTags = false;
                        break; //Doesn't keep checking the rest of the tags
                    }
                }
                for (String tag : hiddenFilterTags) {
                    if (gbcImageToFilter.getTags().contains(tag)) {
                        containsAllTags = false;
                        break; //Doesn't keep checking the rest of the tags
                    }
                }
                if (containsAllTags) {
                    filteredGbcImages.add(gbcImageToFilter);
                }
            }
        }
        imagesForPage = new ArrayList<>();
        itemsPerPage = MainActivity.imagesPage;
        //In case the list of images is shorter than the pagination size
        if (filteredGbcImages.size() < itemsPerPage) {
            itemsPerPage = filteredGbcImages.size();
        }
        try {
            if (filteredGbcImages.size() > 0)//In case all images are deleted
            {
                lastPage = (filteredGbcImages.size() - 1) / itemsPerPage;

                //In case the last page is not complete
                if (currentPage == lastPage && (filteredGbcImages.size() % itemsPerPage) != 0) {
                    itemsPerPage = filteredGbcImages.size() % itemsPerPage;
                    startIndex = filteredGbcImages.size() - itemsPerPage;
                    endIndex = filteredGbcImages.size();

                } else {
                    startIndex = currentPage * itemsPerPage;
                    endIndex = Math.min(startIndex + itemsPerPage, filteredGbcImages.size());
                }
                boolean doAsync = false;
                //The bitmaps come from the BitmapCache map, using the gbcimage hashcode
                for (GbcImage gbcImage : filteredGbcImages.subList(startIndex, endIndex)) {
                    if (!imageBitmapCache.containsKey(gbcImage.getHashCode())) {
                        doAsync = true;
                    }
                }

                if (doAsync) {
                    new UpdateGridViewAsyncTask().execute();
                } else {
                    List<Bitmap> bitmapList = new ArrayList<>();
                    for (GbcImage gbcImage : filteredGbcImages.subList(startIndex, endIndex)) {
                        bitmapList.add(imageBitmapCache.get(gbcImage.getHashCode()));
                    }
                    customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, filteredGbcImages.subList(startIndex, endIndex), bitmapList, false, false, true, selectedImages);
                    if (updatingFromChangeImage) {
                        gridView.performItemClick(gridView.getChildAt(newPosition), newPosition, gridView.getAdapter().getItemId(newPosition));
                        updatingFromChangeImage = false;
                    }
                    gridView.setAdapter(customGridViewAdapterImage);
                }
                tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                updateTitleText();
            } else {
                if (Utils.gbcImagesList.isEmpty()) {
                    tv.setText(tv.getContext().getString(R.string.no_images));
                } else
                    tv.setText(tv.getContext().getString(R.string.no_filtered_images));
                tv_page.setText("");
                gridView.setAdapter(null);
            }
        } catch (Exception e) {
            //In case there is an exception, recover the app by going to first page
            e.printStackTrace();
            currentPage = 0;
            editor.putInt("current_page", currentPage);
            editor.apply();
            updateGridView();

        }
    }

    private void hideSelectionOptions() {
        showEditMenuButton = false;
        selectedImages.clear();
        selectionMode[0] = false;
        gridView.setAdapter(customGridViewAdapterImage);
        MainActivity.fab.hide();
        updateTitleText();
        getActivity().invalidateOptionsMenu();
    }
}
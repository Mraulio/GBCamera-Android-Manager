package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;


import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.ui.usbserial.PrintOverArduino;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.utils.AnimatedGifEncoder;
import com.mraulio.gbcameramanager.utils.DiskCache;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.zip.Deflater;

import javax.xml.transform.Result;

import pl.droidsonroids.gif.GifDrawable;

public class GalleryFragment extends Fragment implements SerialInputOutputManager.Listener {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    static UsbDeviceConnection connection;
    static UsbSerialPort port = null;
    public static GridView gridView;
    private static AlertDialog loadingDialog;
    SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
    static List<String> filterTags = new ArrayList<>();
    static List<GbcImage> filteredGbcImages = new ArrayList<>();
    final int[] globalImageIndex = new int[1];
    static List<Integer> selectedImages = new ArrayList<>();

    private static int itemsPerPage = MainActivity.imagesPage;
    static int startIndex = 0;
    static int endIndex = 0;
    public static int currentPage;
    static int lastPage = 0;
    public static TextView tvResponseBytes;
    boolean crop = false;
    boolean showPalettes = true;
    static TextView tv_page;
    boolean keepFrame = false;
    public static CustomGridViewAdapterImage customGridViewAdapterImage;
    static List<Bitmap> imagesForPage;
    static List<GbcImage> gbcImagesForPage;
    public static TextView tv;
    DisplayMetrics displayMetrics;
    static DiskCache diskCache;

    static boolean selectionMode = false;
    static boolean alreadyMultiSelect = false;
    static AlertDialog deleteDialog;

    public GalleryFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MainActivity.current_fragment = MainActivity.CURRENT_FRAGMENT.GALLERY;
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        tv = view.findViewById(R.id.text_gallery);
        gridView = view.findViewById(R.id.gridView);
        loadingDialog = Utils.loadingDialog(getContext());
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

        view.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
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
                    updateGridView(currentPage);
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
                    updateGridView(currentPage);
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

                if (!selectionMode) {
                    crop = false;
                    keepFrame = false;
                    //Obtain selected image
                    int globalImageIndex;
                    if (currentPage != lastPage) {
                        globalImageIndex = position + (currentPage * itemsPerPage);
                    } else {
                        globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
                    }
                    final Bitmap[] selectedImage = {Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode())};
                    // Create custom dialog
                    final Dialog dialog = new Dialog(getContext());
                    dialog.setContentView(R.layout.custom_dialog);
                    dialog.setCancelable(true);//So it closes when clicking outside or back button

                    ImageView imageView = dialog.findViewById(R.id.image_view);
                    Button btn_paperize = dialog.findViewById(R.id.btnPaperize);
                    if (MainActivity.showPaperizeButton) {
                        btn_paperize.setVisibility(View.VISIBLE);
                    }
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
                    int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
                    imageView.setMaxHeight(maxHeight);

                    Button printButton = dialog.findViewById(R.id.print_button);
                    if (MainActivity.printingEnabled) {
                        printButton.setVisibility(View.VISIBLE);
                    } else printButton.setVisibility(GONE);

                    Button shareButton = dialog.findViewById(R.id.share_button);
                    Button saveButton = dialog.findViewById(R.id.save_button);
                    Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                    GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                    GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                    CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                    CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);
                    CheckBox cbInvert = dialog.findViewById(R.id.cbInvert);

                    showPalettes = true;

                    if (filteredGbcImages.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                        imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                    }
                    if (filteredGbcImages.get(globalImageIndex).isLockFrame()) {
                        keepFrame = true;
                        cbFrameKeep.setChecked(true);
                    }
                    if (filteredGbcImages.get(globalImageIndex).isInvertPalette()) {
                        cbInvert.setChecked(true);
                    }
                    cbInvert.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
                            gbcImage.setInvertPalette(cbInvert.isChecked());
                            Bitmap bitmap = paletteChanger(gbcImage.getPaletteId(), gbcImage.getImageBytes(), gbcImage, keepFrame, true, gbcImage.isInvertPalette());

                            Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                            new SaveImageAsyncTask(gbcImage).execute();
                            updateGridView(currentPage);

                        }
                    });
                    btn_paperize.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List<Integer> indexToPaperize = new ArrayList<>();
                            indexToPaperize.add(globalImageIndex);
                            if (!loadingDialog.isShowing()) {
                                loadingDialog.show();
                            }
                            new PaperizeAsyncTask(indexToPaperize).execute();
                        }
                    });
                    imageView.setOnClickListener(new View.OnClickListener() {
                        private int clickCount = 0;
                        private final Handler handler = new Handler();
                        private final Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                //Single tap action
                                showCustomDialog(globalImageIndex);
                                clickCount = 0;
                            }
                        };

                        @Override
                        public void onClick(View v) {
                            clickCount++;
                            if (clickCount == 1) {
                                // Start timer to detect the double tap
                                handler.postDelayed(runnable, 300);
                            } else if (clickCount == 2) {

                                // Stop timer and make double tap action
                                handler.removeCallbacks(runnable);
                                if (filteredGbcImages.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                                    List<String> tags = filteredGbcImages.get(globalImageIndex).getTags();
                                    for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                        String nombre = iter.next();
                                        if (nombre.equals("__filter:favourite__")) {
                                            iter.remove();
                                        }
                                        filteredGbcImages.get(globalImageIndex).setTags(tags);
                                        if (!filterTags.isEmpty())
                                            dialog.dismiss();
                                        imageView.setBackgroundColor(getContext().getColor(R.color.white));
                                    }
                                } else {
                                    filteredGbcImages.get(globalImageIndex).addTag("__filter:favourite__");
                                    imageView.setBackgroundColor(getContext().getColor(R.color.favorite));

                                }
                                clickCount = 0;
                                //To save the image with the favorite tag to the database
                                new SaveImageAsyncTask(filteredGbcImages.get(globalImageIndex)).execute();
                                updateGridView(currentPage);
                            }
                        }
                    });

                    FramesFragment.CustomGridViewAdapterFrames frameAdapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Utils.framesList, false, false);
                    int frameIndex = 0;
                    for (int i = 0; i < Utils.framesList.size(); i++) {
                        if (Utils.framesList.get(i).getFrameName().equals(filteredGbcImages.get(globalImageIndex).getFrameId())) {
                            frameIndex = i;
                            break;
                        }
                    }

                    frameAdapter.setLastSelectedPosition(frameIndex);
                    gridViewFrames.setAdapter(frameAdapter);

                    //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
                    if (selectedImage[0].getHeight() != 144) {
                        cbFrameKeep.setVisibility(GONE);
                        paletteFrameSelButton.setVisibility(GONE);
                    }

                    cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (keepFrame) keepFrame = false;
                            else keepFrame = true;
                            GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
                            Bitmap bitmap = paletteChanger(gbcImage.getPaletteId(), gbcImage.getImageBytes(), gbcImage, keepFrame, true, gbcImage.isInvertPalette());

                            gbcImage.setLockFrame(keepFrame);
                            Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                            new SaveImageAsyncTask(gbcImage).execute();
                            updateGridView(currentPage);

                        }
                    });
                    cbCrop.setOnClickListener(v -> {
                        if (!crop) {
                            crop = true;
                        } else {
                            crop = false;
                        }
                    });

                    gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                            //Action when clicking a frame inside the Dialog
                            Bitmap framed = null;
                            filteredGbcImages.get(globalImageIndex).setFrameId(Utils.framesList.get(selectedFrameIndex).getFrameName());//Need to set the frame index before changing it because if not it's not added to db

                            try {
                                framed = frameChange(filteredGbcImages.get(globalImageIndex), Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode()), Utils.framesList.get(selectedFrameIndex).getFrameName(), keepFrame);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(framed, framed.getWidth() * 6, framed.getHeight() * 6, false));
//                            selectedImage[0] = framed;
                            Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), framed);
                            frameAdapter.setLastSelectedPosition(selectedFrameIndex);
                            frameAdapter.notifyDataSetChanged();
                            updateGridView(currentPage);
                        }
                    });
                    CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Utils.gbcPalettesList, false, false);
                    int paletteIndex = 0;
                    for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {
                        if (Utils.gbcPalettesList.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex).getPaletteId())) {
                            paletteIndex = i;
                            break;
                        }
                    }
                    adapterPalette.setLastSelectedPosition(paletteIndex);
                    gridViewPalette.setAdapter(adapterPalette);
                    gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int palettePosition, long id) {
                            //Action when clicking a palette inside the Dialog
                            Bitmap changedImage;

                            //Set the new palette to the gbcImage
                            filteredGbcImages.get(globalImageIndex).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                            changedImage = paletteChanger(filteredGbcImages.get(globalImageIndex).getPaletteId(), filteredGbcImages.get(globalImageIndex).getImageBytes(), filteredGbcImages.get(globalImageIndex), filteredGbcImages.get(globalImageIndex).isLockFrame(), true, filteredGbcImages.get(globalImageIndex).isInvertPalette());
                            Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), changedImage);

                            filteredGbcImages.get(globalImageIndex).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                            adapterPalette.setLastSelectedPosition(palettePosition);
                            adapterPalette.notifyDataSetChanged();
                            Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), changedImage);
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(changedImage, changedImage.getWidth() * 6, changedImage.getHeight() * 6, false));
                            updateGridView(currentPage);
                        }
                    });
                    paletteFrameSelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (showPalettes) {
                                showPalettes = false;
                                paletteFrameSelButton.setText(getString(R.string.btn_show_palettes));
                                gridViewPalette.setVisibility(GONE);
                                gridViewFrames.setVisibility(View.VISIBLE);

                            } else {
                                showPalettes = true;
                                paletteFrameSelButton.setText(getString(R.string.btn_show_frames));
                                gridViewFrames.setVisibility(GONE);
                                gridViewPalette.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                    printButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                connect();
                                usbIoManager.start();
                                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                View dialogView = getLayoutInflater().inflate(R.layout.print_dialog, null);
                                tvResponseBytes = dialogView.findViewById(R.id.tvResponseBytes);
                                builder.setView(dialogView);

                                builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();
                                //PRINT IMAGE
                                PrintOverArduino printOverArduino = new PrintOverArduino();

                                printOverArduino.banner = false;
                                try {
                                    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                                    if (availableDrivers.isEmpty()) {
                                        return;
                                    }
                                    UsbSerialDriver driver = availableDrivers.get(0);
                                    List printList = new ArrayList();
                                    printList.add(filteredGbcImages.get(globalImageIndex));
                                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, printList);
                                } catch (Exception e) {
                                    tv.append(e.toString());
                                    Toast toast = Toast.makeText(getContext(), getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                                    toast.show();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    shareButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Bitmap image = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode());
                            Bitmap sharedBitmap = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                            List sharedList = new ArrayList();
                            sharedList.add(sharedBitmap);
                            shareImage(sharedList);
                        }
                    });
                    saveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List saveList = new ArrayList();
                            saveList.add(filteredGbcImages.get(globalImageIndex));
                            saveImage(saveList);
                        }
                    });

                    //Configure the dialog to occupy 80% of screen
                    int screenWidth = displayMetrics.widthPixels;
                    int desiredWidth = (int) (screenWidth * 0.8);
                    Window window = dialog.getWindow();
                    window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

                    //To only dismiss it instead of cancelling when clicking outside it
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
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
                    customGridViewAdapterImage.notifyDataSetChanged();
                }
            }
        });
        //LongPress on an image start selection Mode
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                if (!selectionMode) MainActivity.fab.show();

                //I have to do this here, on onCreateView there was a crash
                if (MainActivity.fab != null && !MainActivity.fab.hasOnClickListeners()) {
                    MainActivity.fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedImages.clear();
                            selectionMode = false;
                            gridView.setAdapter(customGridViewAdapterImage);
                            MainActivity.fab.hide();
                        }
                    });
                }
                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
                }
                if (selectionMode) {
                    Collections.sort(selectedImages);

                    int firstImage = selectedImages.get(0);
                    selectedImages.clear();
                    selectedImages.add(globalImageIndex);
                    int lastImage = selectedImages.get(selectedImages.size() - 1);
                    if (firstImage < globalImageIndex) {
                        for (int i = firstImage; i < lastImage; i++) {
                            if (!selectedImages.contains(i)) {
                                selectedImages.add(i);
                            }
                        }
                    } else if (firstImage > globalImageIndex) {
                        for (int i = firstImage; i > lastImage; i--) {
                            if (!selectedImages.contains(i)) {
                                selectedImages.add(i);
                            }
                        }
                    }
                    alreadyMultiSelect = true;

                } else {
                    selectedImages.add(globalImageIndex);
                    selectionMode = true;
                    alreadyMultiSelect = false;
                }

                customGridViewAdapterImage.notifyDataSetChanged();

                return true;
            }
        });

        if (MainActivity.doneLoading) updateFromMain();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
        if (filterTags.isEmpty()) {
            menu.getItem(1).setTitle(getString(R.string.filter_favorites_item));
        } else {
            menu.getItem(1).setTitle(getString(R.string.remove_filter_item));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_multi_edit:
                if (selectionMode && selectedImages.size() > 1) {
                    Collections.sort(selectedImages);
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (Utils.imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }
                    final Dialog dialog = new Dialog(getContext());

                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {
                            globalImageIndex[0] = selectedImages.get(0);
                            CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Utils.gbcPalettesList, false, false);
                            FramesFragment.CustomGridViewAdapterFrames frameAdapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Utils.framesList, false, false);

                            crop = false;
                            keepFrame = false;

                            final Bitmap[] selectedImage = {Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode())};
                            // Create custom dialog
                            dialog.setContentView(R.layout.custom_dialog);
                            dialog.setCancelable(true);//So it closes when clicking outside or back button
                            ImageView imageView = dialog.findViewById(R.id.image_view);
                            LinearLayout layoutSelected = dialog.findViewById(R.id.ly_selected_images);
                            layoutSelected.setVisibility(View.VISIBLE);
                            List<Bitmap> selectedBitmaps = new ArrayList<>();
                            List<GbcImage> selectedGbcImages = new ArrayList<>();
                            for (int i : selectedImages) {
                                selectedBitmaps.add(Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()));
                                selectedGbcImages.add(filteredGbcImages.get(i));
                            }

                            imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
                            int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
                            imageView.setMaxHeight(maxHeight);

                            Button printButton = dialog.findViewById(R.id.print_button);
                            if (MainActivity.printingEnabled) {
                                printButton.setVisibility(View.VISIBLE);
                            } else printButton.setVisibility(GONE);

                            printButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        connect();
                                        usbIoManager.start();
                                        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        View dialogView = getLayoutInflater().inflate(R.layout.print_dialog, null);
                                        tvResponseBytes = dialogView.findViewById(R.id.tvResponseBytes);
                                        builder.setView(dialogView);

                                        builder.setNegativeButton(getString(R.string.dialog_close_button), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        });

                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                        //PRINT IMAGE
                                        PrintOverArduino printOverArduino = new PrintOverArduino();

                                        printOverArduino.banner = false;
                                        try {
                                            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                                            if (availableDrivers.isEmpty()) {
                                                return;
                                            }
                                            UsbSerialDriver driver = availableDrivers.get(0);

                                            printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, selectedGbcImages);
                                        } catch (Exception e) {
                                            tv.append(e.toString());
                                            Toast toast = Toast.makeText(getContext(), getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                                            toast.show();
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            Button shareButton = dialog.findViewById(R.id.share_button);
                            Button saveButton = dialog.findViewById(R.id.save_button);
                            Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                            GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                            GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                            CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                            CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);
                            CheckBox cbInvert = dialog.findViewById(R.id.cbInvert);

                            showPalettes = true;
                            Button btn_paperize = dialog.findViewById(R.id.btnPaperize);
                            if (MainActivity.showPaperizeButton) {
                                btn_paperize.setVisibility(View.VISIBLE);
                            }
                            btn_paperize.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!loadingDialog.isShowing())
                                        loadingDialog.show();
                                    new PaperizeAsyncTask(selectedImages).execute();
                                }
                            });
                            if (filteredGbcImages.get(globalImageIndex[0]).getTags().contains("__filter:favourite__")) {
                                imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                            }
                            if (filteredGbcImages.get(globalImageIndex[0]).isLockFrame()) {
                                keepFrame = true;
                                cbFrameKeep.setChecked(true);
                            }
                            cbCrop.setOnClickListener(v -> {
                                if (!crop) {
                                    crop = true;
                                } else {
                                    crop = false;
                                }
                            });

                            if (filteredGbcImages.get(globalImageIndex[0]).isInvertPalette()) {
                                cbInvert.setChecked(true);
                            }
                            cbInvert.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    for (int i : selectedImages) {
                                        GbcImage gbcImage = filteredGbcImages.get(i);
                                        gbcImage.setInvertPalette(cbInvert.isChecked());
                                        Bitmap bitmap = paletteChanger(gbcImage.getPaletteId(), gbcImage.getImageBytes(), gbcImage, keepFrame, true, gbcImage.isInvertPalette());

                                        Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), bitmap);
                                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                                        new SaveImageAsyncTask(gbcImage).execute();
                                    }
                                    reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);
                                    updateGridView(currentPage);

                                }
                            });
                            int frameIndex = 0;
                            for (int i = 0; i < Utils.framesList.size(); i++) {
                                if (Utils.framesList.get(i).getFrameName().equals(filteredGbcImages.get(globalImageIndex[0]).getFrameId())) {
                                    frameIndex = i;
                                    break;
                                }
                            }

                            frameAdapter.setLastSelectedPosition(frameIndex);
                            gridViewFrames.setAdapter(frameAdapter);

                            imageView.setOnClickListener(new View.OnClickListener() {
                                private int clickCount = 0;
                                private final Handler handler = new Handler();
                                private final Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        //Single tap action
                                        clickCount = 0;
                                    }
                                };

                                @Override
                                public void onClick(View v) {
                                    clickCount++;
                                    if (clickCount == 1) {
                                        // Start timer to detect the double tap
                                        handler.postDelayed(runnable, 300);
                                    } else if (clickCount == 2) {

                                        // Stop timer and make double tap action
                                        handler.removeCallbacks(runnable);

                                        //Get the favorite status of the first selected image
                                        boolean isFav = filteredGbcImages.get(globalImageIndex[0]).getTags().contains("__filter:favourite__");
                                        for (int i : selectedImages) {
                                            if (isFav) {
                                                List<String> tags = filteredGbcImages.get(i).getTags();
                                                for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                                    String nombre = iter.next();
                                                    if (nombre.equals("__filter:favourite__")) {
                                                        iter.remove();

                                                    }
                                                    filteredGbcImages.get(i).setTags(tags);
                                                    imageView.setBackgroundColor(getContext().getColor(R.color.white));
                                                }
                                                if (!filterTags.isEmpty())
                                                    dialog.dismiss();
                                            } else {
                                                filteredGbcImages.get(i).addTag("__filter:favourite__");
                                                imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                                            }
                                            //To save the image with the favorite tag to the database
                                            new SaveImageAsyncTask(filteredGbcImages.get(i)).execute();
                                        }
                                        reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);

                                        clickCount = 0;
                                        updateGridView(currentPage);
                                    }
                                }
                            });

                            //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
                            if (selectedImage[0].getHeight() != 144) {
                                cbFrameKeep.setVisibility(GONE);
                                paletteFrameSelButton.setVisibility(GONE);
                            }

                            cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (keepFrame) keepFrame = false;
                                    else keepFrame = true;
                                    for (int i : selectedImages) {
                                        GbcImage gbcImage = filteredGbcImages.get(i);
                                        //In case in the multiselect there are bigger images than standard
                                        if (Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()).getHeight() == 144) {
                                            gbcImage.setLockFrame(keepFrame);
                                            Bitmap bitmap = paletteChanger(gbcImage.getPaletteId(), gbcImage.getImageBytes(), gbcImage, keepFrame, true, gbcImage.isInvertPalette());
                                            Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), bitmap);
                                            new SaveImageAsyncTask(gbcImage).execute();
                                        }
                                    }
                                    Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                                    imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                                    reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);

                                    updateGridView(currentPage);

                                }
                            });

                            gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                                    //Action when clicking a frame inside the Dialog
                                    Bitmap framed = null;

                                    try {
                                        for (int i : selectedImages) {
                                            filteredGbcImages.get(i).setFrameId(Utils.framesList.get(selectedFrameIndex).getFrameName());//Need to set the frame index before changing it because if not it's not added to db

                                            framed = frameChange(filteredGbcImages.get(i), Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()), Utils.framesList.get(selectedFrameIndex).getFrameName(), filteredGbcImages.get(i).isLockFrame());
                                            Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), framed);

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());

                                    imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                                    frameAdapter.setLastSelectedPosition(selectedFrameIndex);
                                    frameAdapter.notifyDataSetChanged();
                                    reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);

                                    updateGridView(currentPage);
                                }
                            });
                            int paletteIndex = 0;
                            for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {
                                if (Utils.gbcPalettesList.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getPaletteId())) {
                                    paletteIndex = i;
                                    break;
                                }
                            }
                            adapterPalette.setLastSelectedPosition(paletteIndex);
                            gridViewPalette.setAdapter(adapterPalette);
                            gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int palettePosition, long id) {
                                    //Action when clicking a palette inside the Dialog
                                    Bitmap changedImage = null;
                                    for (int i : selectedImages) {
                                        filteredGbcImages.get(i).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());

                                        changedImage = paletteChanger(filteredGbcImages.get(i).getPaletteId(), filteredGbcImages.get(i).getImageBytes(), filteredGbcImages.get(i), filteredGbcImages.get(i).isLockFrame(), true, filteredGbcImages.get(i).isInvertPalette());
                                        Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), changedImage);

                                    }
                                    adapterPalette.setLastSelectedPosition(palettePosition);
                                    adapterPalette.notifyDataSetChanged();

                                    reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);
                                    Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                                    imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                                    updateGridView(currentPage);
                                }
                            });
                            paletteFrameSelButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (showPalettes) {
                                        showPalettes = false;
                                        paletteFrameSelButton.setText(getString(R.string.btn_show_palettes));
                                        gridViewPalette.setVisibility(GONE);
                                        gridViewFrames.setVisibility(View.VISIBLE);

                                    } else {
                                        showPalettes = true;
                                        paletteFrameSelButton.setText(getString(R.string.btn_show_frames));
                                        gridViewFrames.setVisibility(GONE);
                                        gridViewPalette.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                            shareButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    List<Bitmap> sharedList = new ArrayList<>();
                                    for (int i : selectedImages) {
                                        Bitmap image = Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
                                        Bitmap sharedBitmap = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                                        sharedList.add(sharedBitmap);
                                    }
                                    shareImage(sharedList);
                                }
                            });
                            saveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    saveImage(selectedGbcImages);
                                }
                            });

                            int screenWidth = displayMetrics.widthPixels;
                            int desiredWidth = (int) (screenWidth * 0.8);
                            Window window = dialog.getWindow();
                            window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

                            //To only dismiss it instead of cancelling when clicking outside it
                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    dialog.dismiss();
                                }
                            });
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, adapterPalette, frameAdapter);

                        }

                    });
                    asyncTask.execute();

                    dialog.show();
                    customGridViewAdapterImage.notifyDataSetChanged();
                } else Utils.toast(getContext(), getString(R.string.select_minimum_toast));

                return true;

            case R.id.action_filter_favorite:
                if (selectionMode) {
                    Utils.toast(getContext(), getString(R.string.unselect_all_toast));
                } else {
                    if (filterTags.isEmpty()) {
                        filterTags.add("__filter:favourite__");
                        item.setTitle(getString(R.string.remove_filter_item));
                    } else {
                        filterTags.clear();
                        item.setTitle(getString(R.string.filter_favorites_item));
                    }
                    currentPage = 0;
                    updateGridView(currentPage);
                    return true;
                }
                break;

            case R.id.action_delete:
                if (!selectedImages.isEmpty()) {
                    Collections.sort(selectedImages);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getString(R.string.delete_all_title));//Add to strings

                    GridView deleteImageGridView = new GridView(getContext());
                    deleteImageGridView.setNumColumns(4);
                    deleteImageGridView.setPadding(30, 10, 30, 10);
                    List<Bitmap> deleteBitmapList = new ArrayList<>();
                    List<GbcImage> deleteGbcImage = new ArrayList<>();
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (Utils.imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                        deleteGbcImage.add(filteredGbcImages.get(i));
                    }

                    builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadingDialog.show();
                            new DeleteImageAsyncTask(selectedImages).execute();
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
                                deleteBitmapList.add(Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()));
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
                        if (Utils.imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }
                    builder.setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                        LocalDateTime now = LocalDateTime.now();
                        File file = new File(Utils.IMAGES_FOLDER, "HDR" + dtf.format(now) + ".png");
                        if (averaged[0].getHeight() == 144 * 6 && averaged[0].getWidth() == 160 * 6 && crop) {
                            averaged[0] = Bitmap.createBitmap(averaged[0], 16 * 6, 16 * 6, 128 * 6, 112 * 6);
                        }
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            averaged[0].compress(Bitmap.CompressFormat.PNG, 100, out);
                            Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + "HDR!", Toast.LENGTH_LONG);
                            toast.show();
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
                                listBitmaps.add(Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()));
                            }
                            try {
                                Bitmap bitmap = Utils.averageImages(listBitmaps);
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
                    Collections.sort(selectedImages);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("GIF!");

                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View dialogView = inflater.inflate(R.layout.animation_dialog, null);

                    builder.setView(dialogView);
                    TextView tv_animation = dialogView.findViewById(R.id.tv_animation);
                    Button reload_anim = dialogView.findViewById(R.id.btn_animation);
                    CheckBox cb_loop = dialogView.findViewById(R.id.cb_loop);
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
                            LocalDateTime now = LocalDateTime.now();

                            File file = new File(Utils.IMAGES_FOLDER, "GIF_" + dtf.format(now) + ".gif");

                            try (FileOutputStream out = new FileOutputStream(file)) {

                                out.write(bos.toByteArray());
                                Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + "GIF!", Toast.LENGTH_LONG);
                                toast.show();
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
                        if (Utils.imageBitmapCache.get(hashCode) == null) {
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

                                for (int i : selectedImages) {
                                    Bitmap bitmap = Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
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
                            Bitmap bitmap = Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode());
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
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    JSONObject jsonObject = new JSONObject();
                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (Utils.imageBitmapCache.get(hashCode) == null) {
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
                                imageObject.put("frame", gbcImage.getFrameId());
                                imageObject.put("invertPalette", gbcImage.isInvertPalette());
                                imageObject.put("lockFrame", gbcImage.isLockFrame());
                                imagesArray.put(imageObject);
                            }
                            stateObject.put("images", imagesArray);
                            stateObject.put("lastUpdateUTC", System.currentTimeMillis() / 1000);
                            jsonObject.put("state", stateObject);
                            for (int i = 0; i < selectedImages.size(); i++) {
                                GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));
                                Bitmap imageBitmap = paletteChanger("bw", gbcImage.getImageBytes(), gbcImage, false, false, gbcImage.isInvertPalette());

                                String txt = Utils.bytesToHex(Utils.encodeImage(imageBitmap, "bw"));
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

    private void connect() {
        manager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }
        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            if (port.isOpen()) port.close();
            port.open(connection);
            port.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        } catch (Exception e) {
            tv.append(e.toString());
            Toast.makeText(getContext(), "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();
        }

        usbIoManager = new SerialInputOutputManager(port, this);
    }


    public static Bitmap frameChange(GbcImage gbcImage, Bitmap bitmap, String selectedFrameId, boolean keepFrame) throws IOException {
        Bitmap framed = null;
        Bitmap framedAux;
        if ((gbcImage.getImageBytes().length / 40) == 144) {
            //I need to use copy because if not it's inmutable bitmap
            framed = Utils.hashFrames.get(selectedFrameId).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
            framedAux = framed.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvasAux = new Canvas(framedAux);
            Bitmap setToPalette = paletteChanger("bw", gbcImage.getImageBytes(), gbcImage, keepFrame, false, false);
            Bitmap croppedBitmapAux = Bitmap.createBitmap(setToPalette, 16, 16, 128, 112);//Need to put this to palette 0
            canvasAux.drawBitmap(croppedBitmapAux, 16, 16, null);
            if (!keepFrame) {
                framed = paletteChanger(gbcImage.getPaletteId(), Utils.encodeImage(framed, "bw"), gbcImage, keepFrame, true, gbcImage.isInvertPalette());
                framed = framed.copy(Bitmap.Config.ARGB_8888, true);//To make it mutable
            }
            Canvas canvas = new Canvas(framed);
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 16, 16, 128, 112);
            canvas.drawBitmap(croppedBitmap, 16, 16, null);

            try {
                byte[] imageBytes = Utils.encodeImage(framedAux, "bw");//Use the framedAux because it doesn't a different palette to encode
                gbcImage.setImageBytes(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return bitmap;
        }
        diskCache.put(gbcImage.getHashCode(), framed);
        new SaveImageAsyncTask(gbcImage).execute();
        return framed;
    }

//    public static Bitmap frameChange(GbcImage gbcImage, Bitmap bitmap, String selectedFrameId, boolean keepFrame) throws IOException {
//        if ((gbcImage.getImageBytes().length / 40) == 144) {
//            Bitmap framed = Utils.hashFrames.get(selectedFrameId).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
//            Canvas canvas = new Canvas(framed);
//            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 16, 16, 128, 112); //Getting the internal 128x112 image
//            canvas.drawBitmap(croppedBitmap, 16, 16, null);
////            byte[] imageBytes = Utils.encodeImage(bitmap, gbcImage.getPaletteId());
//            byte[] frameBytes = Utils.encodeImage(framed, gbcImage.getPaletteId());
//            framed = paletteChanger(gbcImage.getPaletteId(), frameBytes, gbcImage, keepFrame, true);
////        framed = paletteChanger(gbcImage.getPaletteId(), gbcImage.getImageBytes(), gbcImage, keepFrame);
//        gbcImage.setFrameId(selectedFrameId);
////        framed = paletteChanger(gbcImage.getPaletteId(), Utils.encodeImage(framed), gbcImage, keepFrame);
//        gbcImage.setImageBytes(frameBytes);
//            new SaveImageAsyncTask(gbcImage).execute();
//            return framed;
//        } else return bitmap;
//    }


    //Change palette
    public static Bitmap paletteChanger(String paletteId, byte[] imageBytes, GbcImage gbcImage, boolean keepFrame, boolean save, boolean invertPalette) {

        ImageCodec imageCodec = new ImageCodec(160, imageBytes.length / 40, keepFrame);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(paletteId).getPaletteColorsInt(), imageBytes, invertPalette);

        new SaveImageAsyncTask(gbcImage).execute();
        if (save)
            diskCache.put(gbcImage.getHashCode(), image);
        return image;
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    private void showCustomDialog(int globalImageIndex) {
        Bitmap bitmap = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode());

        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.single_image_dialog);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false));
        TextView tvImageName = dialog.findViewById(R.id.tv_imageName);
        tvImageName.setText(filteredGbcImages.get(globalImageIndex).getName());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            editor.putInt("current_page", currentPage);
            editor.apply();
        }
    }

    private void nextPage() {
        if (currentPage < lastPage) {
            currentPage++;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            editor.putInt("current_page", currentPage);
            editor.apply();
        }
    }

    private AlertDialog numberPickerPageDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        NumberPicker numberPicker = new NumberPicker(context);
        builder.setTitle(getString(R.string.page_selector_dialog));//Add to string xml
        builder.setView(numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(lastPage + 1);
        numberPicker.setWrapSelectorWheel(false);
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
                    updateGridView(currentPage);
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                }
                dialog.hide();
            }
        });
        return dialog;
    }

    private void reloadLayout(LinearLayout layoutSelected, ImageView imageView, CheckBox keepFrameCb, CheckBox invertCb, CustomGridViewAdapterPalette adapterPalette, FramesFragment.CustomGridViewAdapterFrames frameAdapter) {
        layoutSelected.removeAllViews();
        for (int i = 0; i < selectedImages.size(); i++) {
            GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));
            ImageView imageViewMini = new ImageView(getContext());
            imageViewMini.setId(i);
            imageViewMini.setPadding(5, 3, 5, 2);
            imageViewMini.setImageBitmap(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
            if (gbcImage.getTags().contains("__filter:favourite__")) {
                imageViewMini.setBackgroundColor(getContext().getColor(R.color.favorite));
            }
            imageViewMini.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageViewId = view.getId(); // Get the ImageView id
                    Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
                    globalImageIndex[0] = selectedImages.get(imageViewId);
                    boolean isFav = gbcImage.getTags().contains("__filter:favourite__");
                    if (isFav) {
                        imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                    } else {
                        imageView.setBackgroundColor(getContext().getColor(R.color.white));
                    }
                    if (gbcImage.isLockFrame()) {
                        keepFrameCb.setChecked(true);
                    } else keepFrameCb.setChecked(false);
                    if (gbcImage.isInvertPalette()) {
                        invertCb.setChecked(true);
                    } else invertCb.setChecked(false);
                    int paletteIndex = 0;

                    for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {
                        if (Utils.gbcPalettesList.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getPaletteId())) {
                            paletteIndex = i;
                            break;
                        }
                    }
                    adapterPalette.setLastSelectedPosition(paletteIndex);
                    adapterPalette.notifyDataSetChanged();
                    int frameIndex = 0;
                    for (int i = 0; i < Utils.framesList.size(); i++) {
                        if (Utils.framesList.get(i).getFrameName().equals(filteredGbcImages.get(globalImageIndex[0]).getFrameId())) {
                            frameIndex = i;
                            break;
                        }
                    }

                    frameAdapter.setLastSelectedPosition(frameIndex);
                    frameAdapter.notifyDataSetChanged();
                }
            });
            layoutSelected.addView(imageViewMini);
        }
    }

    private void shareImage(List<Bitmap> bitmaps) {
        ArrayList<Uri> imageUris = new ArrayList<>();
        FileOutputStream fileOutputStream = null;

        try {
            for (int i = 0; i < bitmaps.size(); i++) {
                Bitmap bitmap = bitmaps.get(i);

                if ((bitmap.getHeight() / MainActivity.exportSize) == 144 && (bitmap.getWidth() / MainActivity.exportSize) == 160 && crop) {
                    bitmap = Bitmap.createBitmap(bitmap, 16 * MainActivity.exportSize, 16 * MainActivity.exportSize, 128 * MainActivity.exportSize, 112 * MainActivity.exportSize);
                }

                File file = new File(getActivity().getExternalCacheDir(), "shared_image_" + i + ".png");
                fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();

                Uri uri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", file);
                imageUris.add(uri);
            }

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("image/png");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share"));
        } catch (Exception e) {
            Utils.toast(getContext(), "Exception");
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

    private void saveImage(List<GbcImage> gbcImages) {
        LocalDateTime now = LocalDateTime.now();
        String fileNameBase = "gbcImage_";
        String extension = MainActivity.exportPng ? ".png" : ".txt";
        for (int i = 0; i < gbcImages.size(); i++) {
            GbcImage gbcImage = gbcImages.get(i);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            String fileName = fileNameBase + dtf.format(now);

            if (gbcImages.size() > 1) {
                fileName += "_" + (i + 1);
            }

            fileName += extension;
            if (MainActivity.exportPng) {
                File file = new File(Utils.IMAGES_FOLDER, fileName);

                if (image.getHeight() == 144 && image.getWidth() == 160 && crop) {
                    image = Bitmap.createBitmap(image, 16, 16, 128, 112);
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                File file = new File(Utils.TXT_FOLDER, fileName);

                //Saving txt without cropping it
                try {
                    //Need to change the palette to bw so the encodeImage method works
                    image = paletteChanger("bw", gbcImage.getImageBytes(), filteredGbcImages.get(0), false, false, false);
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
        }
        if (MainActivity.exportPng) {
            Utils.toast(getContext(), getString(R.string.toast_saved) + MainActivity.exportSize);
        } else Utils.toast(getContext(), getString(R.string.toast_saved_txt));

    }


    private String addSpacesAndNewLines(String input) {
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

    public void updateFromMain() {
        if (Utils.gbcImagesList.size() > 0) {
            updateGridView(currentPage);
            tv.setText(tv.getContext().getString(R.string.total_images) + GbcImage.numImages);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));

        } else {
            tv.setText(tv.getContext().getString(R.string.no_images));
        }
    }

    //Method to update the gallery gridview
    public static void updateGridView(int page) {
        //Bitmap list to store current page bitmaps
        filteredGbcImages = new ArrayList<>();
        if (filterTags.isEmpty()) {
            filteredGbcImages = Utils.gbcImagesList;
        } else {
            filteredGbcImages.clear();
            for (GbcImage gbcImageToFilter : Utils.gbcImagesList) {
                if (gbcImageToFilter.getTags().contains(filterTags.get(0))) {
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
        if (filteredGbcImages.size() > 0)//In case all images are deleted
        {
            lastPage = (filteredGbcImages.size() - 1) / itemsPerPage;

            //In case the last page is not complete
            if (currentPage == lastPage && (filteredGbcImages.size() % itemsPerPage) != 0) {
                itemsPerPage = filteredGbcImages.size() % itemsPerPage;
                startIndex = filteredGbcImages.size() - itemsPerPage;
                endIndex = filteredGbcImages.size();

            } else {
                startIndex = page * itemsPerPage;
                endIndex = Math.min(startIndex + itemsPerPage, filteredGbcImages.size());
            }
            boolean doAsync = false;

            //The bitmaps come from the BitmapCache map, using the gbcimage hashcode
            for (GbcImage gbcImage : filteredGbcImages.subList(startIndex, endIndex)) {
                if (!Utils.imageBitmapCache.containsKey(gbcImage.getHashCode())) {
                    doAsync = true;
                }
            }
            if (doAsync) {
//                if (!loadingDialog.isShowing())
//                    loadingDialog.show();
                new UpdateGridViewAsyncTask().execute();
            } else {
                List<Bitmap> bitmapList = new ArrayList<>();
                for (GbcImage gbcImage : filteredGbcImages.subList(startIndex, endIndex)) {
                    bitmapList.add(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
                }
                customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, filteredGbcImages.subList(startIndex, endIndex), bitmapList, false, false, true, selectedImages);
                gridView.setAdapter(customGridViewAdapterImage);
            }
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            tv.setText(tv.getContext().getString(R.string.total_images) + GbcImage.numImages);

        } else {
            if (Utils.gbcImagesList.isEmpty())
                tv.setText(tv.getContext().getString(R.string.no_images));
            else
                tv.setText(tv.getContext().getString(R.string.no_favorites));
            tv_page.setText("");
            gridView.setAdapter(null);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        BigInteger bigInt = new BigInteger(1, data);
        String hexString = bigInt.toString(16);
        // Make sure the string is of pair length
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        // Format the string in 2 chars blocks
        hexString = String.format("%0" + (hexString.length() + hexString.length() % 2) + "X", new BigInteger(hexString, 16));
        hexString = hexString.replaceAll("..", "$0 ");//To separate with spaces every hex byte
        String finalHexString = hexString;
        getActivity().runOnUiThread(() -> {
            tvResponseBytes.append(finalHexString);
        });
    }

    @Override
    public void onRunError(Exception e) {

    }

    private static class UpdateGridViewAsyncTask extends AsyncTask<Void, Void, Void> {

        //I could add a isCancelled flag
        @Override
        protected Void doInBackground(Void... voids) {
            //Store the indexes because if I call it again, it will use startIndex with a different value and crash
            int newStartIndex = startIndex;
            int newEndIndex = endIndex;

            List<String> currentPageHashes = new ArrayList<>();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            //foreach index
            int index = 0;
            //Loop for each gbcImage in a sublist in all gbcImages objects for the current page
            for (GbcImage gbcImage : filteredGbcImages.subList(newStartIndex, newEndIndex)) {
                //Add the hashcode to the list of current hashes
                String imageHash = gbcImage.getHashCode();
                currentPageHashes.add(imageHash);
                byte[] imageBytes;
                Bitmap image = diskCache.get(imageHash);
                Utils.imageBitmapCache.put(imageHash, image);

//                Get the image bytes from the database for the current gbcImage
                if (image == null) {
                    if (!loadingDialog.isShowing())
                        publishProgress();
                    imageBytes = imageDataDao.getDataByImageId(imageHash);
                    //Set the image bytes to the object
                    gbcImage.setImageBytes(imageBytes);
                    //Create the image bitmap
                    int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                    ImageCodec imageCodec = new ImageCodec(160, height, gbcImage.isLockFrame());
                    image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes, gbcImage.isInvertPalette());
                    //Add the bitmap to the cache
                    Utils.imageBitmapCache.put(imageHash, image);
                    diskCache.put(imageHash, image);
                    //Do a frameChange to create the Bitmap of the image
                    try {
                        //Only do frameChange if the image is 144 height AND THE FRAME IS NOT EMPTY (AS SET WHEN READING WITH ARDUINO PRINTER EMULATOR)
                        if (image.getHeight() == 144 && !gbcImage.getFrameId().equals("") /*&& !gbcImage.getFrameId().equals("Nintendo_Frame")*/)
                            image = frameChange(filteredGbcImages.get(newStartIndex + index), Utils.imageBitmapCache.get(filteredGbcImages.get(newStartIndex + index).getHashCode()), filteredGbcImages.get(newStartIndex + index).getFrameId(), filteredGbcImages.get(newStartIndex + index).isLockFrame());
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), image);
                        diskCache.put(imageHash, image);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                index++;
            }
            gbcImagesForPage = filteredGbcImages.subList(newStartIndex, newEndIndex);
            //Create a list of bitmaps to use in the adapter, getting the bitmaps from the cache map for each image in the current page
            List<Bitmap> bitmapList = new ArrayList<>();
            for (GbcImage gbcImage : gbcImagesForPage) {
                bitmapList.add(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
            }
            customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, filteredGbcImages.subList(newStartIndex, newEndIndex), bitmapList, false, false, true, selectedImages);
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (!loadingDialog.isShowing())
                loadingDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Notifies the adapter
            gridView.setAdapter(customGridViewAdapterImage);
            loadingDialog.dismiss();
        }
    }

    private static class LoadBitmapCacheAsyncTask extends AsyncTask<Void, Void, Result> {
        private List<Integer> indexesToLoad;
        private AsyncTaskCompleteListener<Result> listener;

        public LoadBitmapCacheAsyncTask(List<Integer> indexesToLoad, AsyncTaskCompleteListener<Result> listener) {
            this.indexesToLoad = indexesToLoad;
            this.listener = listener;
        }

        //I could add a isCancelled flag
        @Override
        protected Result doInBackground(Void... voids) {
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            //foreach index
            for (int i : indexesToLoad) {
                GbcImage gbcImage = filteredGbcImages.get(i);
                //Add the hashcode to the list of current hashes
                String imageHash = gbcImage.getHashCode();
                byte[] imageBytes;
                Bitmap image = diskCache.get(imageHash);
                Utils.imageBitmapCache.put(imageHash, image);

//                Get the image bytes from the database for the current gbcImage
                if (image == null) {
                    imageBytes = imageDataDao.getDataByImageId(imageHash);
                    //Set the image bytes to the object
                    gbcImage.setImageBytes(imageBytes);
                    //Create the image bitmap
                    int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                    ImageCodec imageCodec = new ImageCodec(160, height, gbcImage.isLockFrame());
                    image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes, gbcImage.isInvertPalette());
                    //Add the bitmap to the cache
                    Utils.imageBitmapCache.put(imageHash, image);
                    diskCache.put(imageHash, image);
                    //Do a frameChange to create the Bitmap of the image
                    try {
                        //Only do frameChange if the image is 144 height AND THE FRAME IS NOT EMPTY (AS SET WHEN READING WITH ARDUINO PRINTER EMULATOR)
                        if (image.getHeight() == 144 && !gbcImage.getFrameId().equals(""))
                            image = frameChange(filteredGbcImages.get(i), Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()), filteredGbcImages.get(i).getFrameId(), filteredGbcImages.get(i).isLockFrame());
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), image);
                        diskCache.put(imageHash, image);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Result result) {
            //Notifies the adapter
            if (listener != null) {
                listener.onTaskComplete(result); //Notify the finalization from the interface
            }
            loadingDialog.dismiss();
        }
    }


    private class DeleteImageAsyncTask extends AsyncTask<Void, Void, Void> {
        //        private int imageIndex;
        private List<Integer> listImagesIndexes;

        public DeleteImageAsyncTask(List<Integer> listImagesIndexes) {
            this.listImagesIndexes = listImagesIndexes;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int imageIndex : listImagesIndexes) {
                String hashCode = filteredGbcImages.get(imageIndex).getHashCode();
                GbcImage gbcImage = filteredGbcImages.get(imageIndex);
                ImageDao imageDao = MainActivity.db.imageDao();
                ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
                ImageData imageData = imageDataDao.getImageDataByid(hashCode);
                imageDao.delete(gbcImage);
                imageDataDao.delete(imageData);
                Utils.imageBitmapCache.remove(hashCode);
                diskCache.remove(hashCode);
                GbcImage.numImages--;
            }
            //Doing this after deleting the images
            //IMPROVE THIS
            Collections.sort(listImagesIndexes);
            for (int index = listImagesIndexes.size(); index > 0; index--) {
                int image = listImagesIndexes.get(index - 1);
                String imageHash = filteredGbcImages.get(image).getHashCode();
                filteredGbcImages.remove(image);
                for (int i = 0; i < Utils.gbcImagesList.size(); i++) {
                    GbcImage gbcImage = Utils.gbcImagesList.get(i);
                    if (gbcImage.getHashCode().equals(imageHash)) {
                        Utils.gbcImagesList.remove(i);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            itemsPerPage = MainActivity.imagesPage;//Because it's changed when going to the last page on the updateGridView
            selectedImages.clear();
            selectionMode = false;
            tv.setText(tv.getContext().getString(R.string.total_images) + GbcImage.numImages);
            //Update lastPage and CurrentPage after deleting

            if (currentPage > (filteredGbcImages.size() / itemsPerPage) - 1) {
                currentPage = ((filteredGbcImages.size() - 1) / itemsPerPage);
                editor.putInt("current_page", currentPage);
                editor.apply();
                lastPage = currentPage;
            }
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
            MainActivity.fab.hide();
            loadingDialog.dismiss();
            updateGridView(currentPage);
        }
    }


    //Method to update an image to the database in the background
    public static class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
        private GbcImage gbcImage;

        //Stores the image passes as parameter in the constructor
        public SaveImageAsyncTask(GbcImage gbcImage) {
            this.gbcImage = gbcImage;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ImageDao imageDao = MainActivity.db.imageDao();
            imageDao.update(gbcImage);
            return null;
        }
    }

    public static class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
        Context context;
        int layoutResourceId;
        List<GbcImage> data = new ArrayList<GbcImage>();
        private List<Bitmap> images;
        private boolean checkDuplicate;
        private boolean showName, multiSelect;
        private List<Integer> selectedImages;

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate,
                                          boolean showName, boolean multiSelect, List<Integer> selectedImages) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
            this.checkDuplicate = checkDuplicate;
            this.showName = showName;
            this.multiSelect = multiSelect;
            this.selectedImages = selectedImages;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RecordHolder holder = null;
            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new RecordHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
                holder.imageItem = (ImageView) row.findViewById(R.id.imageView);

                row.setTag(holder);
            } else {
                holder = (RecordHolder) row.getTag();
            }
            Bitmap image = images.get(position);
            String name = data.get(position).getName();
            String hash = data.get(position).getHashCode();
            List<String> hashToCheck = new ArrayList<>();
            if (multiSelect && selectedImages != null && !selectedImages.isEmpty()) {
                for (int i : selectedImages) {
                    hashToCheck.add(filteredGbcImages.get(i).getHashCode());
                }
                row.setBackgroundColor(hashToCheck.contains(hash) ? context.getColor(R.color.teal_700) : Color.WHITE);
            }

            Boolean fav = data.get(position).getTags().contains("__filter:favourite__");
            holder.imageItem.setBackgroundColor(fav ? context.getColor(R.color.favorite) : Color.WHITE);
            Boolean dup = false;
            if (checkDuplicate) {
                for (GbcImage gbcImage : Utils.gbcImagesList) {
                    //Compare the hash value with the value of the new image hash
                    if (gbcImage.getHashCode().equals(hash)) {
                        //If hash is equals means the image already exists
                        dup = true;
                    }
                }
            }
            if (showName) {
                holder.txtTitle.setTextColor(dup ? context.getResources().getColor(R.color.duplicated) : Color.BLACK);
                if (name.equals("")) {
                    holder.txtTitle.setText("*No title*");
                } else
                    holder.txtTitle.setText(name);
            } else {
                holder.txtTitle.setVisibility(GONE);
            }

            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;
        }
    }

    public class PaperizeAsyncTask extends AsyncTask<Void, Void, Void> {
        private List<Integer> gbcImagesList;

        public PaperizeAsyncTask(List<Integer> gbcImagesList) {
            this.gbcImagesList = gbcImagesList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int index = 1;
            LocalDateTime now = LocalDateTime.now();
            String date = dtf.format(now);
            for (int i : gbcImagesList) {
                Bitmap bw_image = paletteChanger("bw", filteredGbcImages.get(i).getImageBytes(), filteredGbcImages.get(i), false, false, filteredGbcImages.get(i).isInvertPalette());
                Bitmap paperized = Paperize(bw_image);
                File file = null;
                if (gbcImagesList.size() > 1)
                    file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + "_" + (index) + ".png");
                else {
                    file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + ".png");
                }

                if (paperized.getHeight() == 144 && paperized.getWidth() == 160 && crop) {
                    paperized = Bitmap.createBitmap(paperized, 16, 16, 128, 112);
                }
                try (FileOutputStream out = new FileOutputStream(file)) {
                    Bitmap scaled = Bitmap.createScaledBitmap(paperized, paperized.getWidth(), paperized.getHeight(), false);
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                index++;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Utils.toast(getContext(), getString(R.string.saved_paperized_toast));
            loadingDialog.dismiss();
        }
    }

    private Bitmap Paperize(Bitmap inputBitmap) {
        //intensity map for printer head with threshold
        int mul = 20;
        int overlapping = 4;
        Bitmap pixelSampleBitmap;
        pixelSampleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pixel_sample);

        int height = inputBitmap.getHeight();
        int width = inputBitmap.getWidth();
        int newWidth = width * 20;
        int newHeight = height * 20;
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, inputBitmap.getConfig());


        int[][] streaks = new int[height][width];
        Random random = new Random();
//        for (int i = 0; i < width; i++) {
//            int start = random.nextInt(2); // Generar 0 o 1 aleatoriamente
//            for (int j = 0; j < height; j++) {
//                streaks[j][i] = start;
//                //you can change the streak length here
//                if (random.nextDouble() < 0.2) {
//                    start = random.nextInt(2); // Generar 0 o 1 aleatoriamente si se cumple la condición
//                }
//            }
//        }
        // Tamaño de la región que deseas copiar (20x20)
        int regionSize = 20;

        for (int y = 0; y < inputBitmap.getHeight(); y++) {
            for (int x = 0; x < inputBitmap.getWidth(); x++) {
                int color = inputBitmap.getPixel(x, y);
                int randomRegionX = random.nextInt(50) * regionSize;
                // Calcular la posición correspondiente en el nuevo Bitmap
                int newX = x * regionSize;
                int newY = y * regionSize;

                // Determinar la zona de pixelSampleBitmap según el color
                if (color == Color.parseColor("#FFFFFF")) {
                    // Color blanco (#FFFFFF), no se coge nada de pixelSampleBitmap
                    for (int dy = 0; dy < regionSize; dy++) {
                        for (int dx = 0; dx < regionSize; dx++) {
                            newBitmap.setPixel(newX + dx, newY + dy, Color.WHITE);
                        }
                    }
                } else if (color == Color.parseColor("#AAAAAA")) {
                    // Color aaaaaa, coger la 3a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 2 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                } else if (color == Color.parseColor("#555555")) {
                    // Color 555555, coger la 2a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 1 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                } else if (color == Color.parseColor("#000000")) {
                    // Color 000000, coger la 1a fila de 20x20 píxeles de pixelSampleBitmap
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 0 * regionSize, regionSize, regionSize);
                    Canvas canvas = new Canvas(newBitmap);
                    canvas.drawBitmap(regionBitmap, newX, newY, null);
                }
            }
        }
        return newBitmap;
    }

    /**
     * Detects left and right swipes across a view.
     * https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }
}
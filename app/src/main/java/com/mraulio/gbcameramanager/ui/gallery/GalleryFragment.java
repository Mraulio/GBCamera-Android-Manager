package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkFilterPass;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSize;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSquare;
import static com.mraulio.gbcameramanager.utils.StaticValues.filterByDate;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.gbxcart.GBxCartConstants.BAUDRATE;
import static com.mraulio.gbcameramanager.ui.gallery.CollageMaker.addPadding;
import static com.mraulio.gbcameramanager.ui.gallery.CollageMaker.applyBorderToIV;
import static com.mraulio.gbcameramanager.ui.gallery.CollageMaker.createCollage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.averageImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.encodeData;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.makeSquareImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.showFilterDialog;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.sortImages;

import static com.mraulio.gbcameramanager.ui.gallery.PaperUtils.paperDialog;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.getHiddenTags;
import static com.mraulio.gbcameramanager.utils.Utils.getSelectedTags;
import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;
import static com.mraulio.gbcameramanager.utils.Utils.tagsHash;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.ui.usbserial.PrintOverArduino;
import com.mraulio.gbcameramanager.utils.AnimatedGifEncoder;
import com.mraulio.gbcameramanager.utils.DiskCache;
import com.mraulio.gbcameramanager.utils.HorizontalNumberPicker;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.TouchImageView;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
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

public class GalleryFragment extends Fragment implements SerialInputOutputManager.Listener {
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    static UsbDeviceConnection connection;
    static UsbSerialPort port = null;
    public static GridView gridView;
    static LoadingDialog loadDialog;
    static SharedPreferences.Editor editor = StaticValues.sharedPreferences.edit();
    static HashSet<String> selectedFilterTags = new HashSet<>();
    static HashSet<String> hiddenFilterTags = new HashSet<>();
    static List<GbcImage> filteredGbcImages = new ArrayList<>();
    static boolean updatingFromChangeImage = false;
    static List<Integer> selectedImages = new ArrayList<>();
    static StringBuilder sbTitle = new StringBuilder();
    static int itemsPerPage = StaticValues.imagesPage;
    static int startIndex = 0;
    static int endIndex = 0;
    static Activity galleryActivity;
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

    public static boolean[] selectionMode = {false};
    static boolean alreadyMultiSelect = false;
    static AlertDialog deleteDialog;

    public GalleryFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        galleryActivity = getActivity();


        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.GALLERY;
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        tv = view.findViewById(R.id.text_gallery);
        gridView = view.findViewById(R.id.gridView);
        loadDialog = new LoadingDialog(getContext(), null);
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
                    MainImageDialog mainImageDialog = new MainImageDialog(gridView, keepFrame, lastPage, position,
                            filteredGbcImages, getContext(), displayMetrics, showPalettes, getActivity(),
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
                        hideSelectionOptions(getActivity());
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

                if (!selectionMode[0]) StaticValues.fab.show();

                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
                }
                if (selectionMode[0]) {

                    Collections.sort(selectedImages);

                    int firstImage = selectedImages.get(0);
                    int lastImage = selectedImages.get(selectedImages.size()-1);
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
                        for (int i = lastImage; i > globalImageIndex; i--) {
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

        if (MainActivity.doneLoading) updateFromMain(getContext());

        return view;
    }

    private static void updateTitleText() {
        if (!selectedFilterTags.isEmpty() || !hiddenFilterTags.isEmpty() || filterByDate) {
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
                    MainImageDialog mainImageDialog = new MainImageDialog(gridView, keepFrame, lastPage, 0,
                            filteredGbcImages, getContext(), displayMetrics, showPalettes, getActivity(),
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

            case R.id.action_collage:
                if (!selectedImages.isEmpty()) {
                    //If there are too many images selected, the resulting image to show will be too big (because of the *6 in the ImageView)
                    int scaledCollage = 4;
                    int maxZoom = 10;
                    if (selectedImages.size() > 40) {
                        scaledCollage = 1;
                        maxZoom = 30;
                    }
                    if (selectedImages.size() > 200) {
                        toast(getContext(), getString(R.string.collage_too_many_images));
                        return true;
                    }

                    final Bitmap[] collagedImage = new Bitmap[1];
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    List<Bitmap> collageBitmapList = new ArrayList<>();
                    View collageView = inflater.inflate(R.layout.collage_dialog, null);
                    TouchImageView imageView = collageView.findViewById(R.id.iv_collage);
                    imageView.setMaxZoom(maxZoom);

                    Button btnReloadCollage = collageView.findViewById(R.id.btnReloadCollage);
                    Button btnSaveCollage = collageView.findViewById(R.id.save_btn_collage);
                    Button btnCancel = collageView.findViewById(R.id.cancel_button);

                    Button btnPrint = collageView.findViewById(R.id.print_button_collage);
                    Button btnPaperizeCollage = collageView.findViewById(R.id.btn_paperize_collage);

                    btnPrint.setVisibility(StaticValues.printingEnabled ? VISIBLE : GONE);
                    btnPaperizeCollage.setVisibility(StaticValues.showPaperizeButton ? VISIBLE : GONE);


                    Switch swCropCollage = collageView.findViewById(R.id.swCropCollage);
                    Switch swHorizontalOrientation = collageView.findViewById(R.id.sw_orientation);
                    Switch swHalfFrame = collageView.findViewById(R.id.sw_half_frame);
                    TextView tvExtraPadding = collageView.findViewById(R.id.tv_extra_padding);
                    SeekBar swExtraPadding = collageView.findViewById(R.id.sb_extra_padding);
                    ImageView ivPaddingColor = collageView.findViewById(R.id.iv_padding_color);
                    TextView tvNPCols = collageView.findViewById(R.id.tvNPCols);
                    HorizontalNumberPicker nPColsRows = collageView.findViewById(R.id.numberPickerCols);
                    nPColsRows.setMax(30);
                    nPColsRows.setMin(1);

                    final int[] colsRowsValue = {1};
                    final int[] lastPicked = {Color.parseColor("#FFFFFF")};
                    final int[] extraPaddingMultiplier = {0};

                    btnPrint.setOnClickListener(view -> {
                        Bitmap printBitmap = getPrintBitmap(colsRowsValue[0], lastPicked[0], swCropCollage.isChecked(), swHorizontalOrientation.isChecked(), swHalfFrame.isChecked(), extraPaddingMultiplier[0]);
                        if (printBitmap != null) {
                            try {
//                                imageView.setImageBitmap(Bitmap.createScaledBitmap(printBitmap, printBitmap.getWidth() * 5, printBitmap.getHeight() * 5, false));
                                connect();
                                usbIoManager.start();
                                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.print_dialog, null);
                                tvResponseBytes = dialogView.findViewById(R.id.tvResponseBytes);
                                builder.setView(dialogView);

                                builder.setNegativeButton(getString(R.string.dialog_close_button), (dialog, which) -> {
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
                                    List<byte[]> imageByteList = new ArrayList();

                                    imageByteList.add(Utils.encodeImage(printBitmap, "bw"));
                                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, imageByteList);
                                } catch (Exception e) {
                                    tv.append(e.toString());
                                    Toast toast = Toast.makeText(getContext(), getContext().getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                                    toast.show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    btnPaperizeCollage.setOnClickListener(view -> {
                        Bitmap printBitmap = getPrintBitmap(colsRowsValue[0], lastPicked[0], swCropCollage.isChecked(), swHorizontalOrientation.isChecked(), swHalfFrame.isChecked(), extraPaddingMultiplier[0]);
                        if (printBitmap != null) {
                            List<Bitmap> printHolder = new ArrayList<>();
                            printHolder.add(printBitmap);
                            paperDialog(printHolder, getContext());
                        }
                    });

                    ivPaddingColor.setOnClickListener(v -> ColorPickerDialogBuilder
                            .with(getContext())
                            .setTitle(getString(R.string.choose_color))
                            .initialColor(lastPicked[0])
                            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                            .density(12)
                            .showAlphaSlider(false)
                            .setOnColorSelectedListener(selectedColor -> Utils.toast(getContext(), getString(R.string.selected_color) + Integer.toHexString(selectedColor).substring(2).toUpperCase()))
                            .setPositiveButton("OK", new ColorPickerClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                    applyBorderToIV(ivPaddingColor, selectedColor);
                                    lastPicked[0] = selectedColor;

                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                            })
                            .build()
                            .show());
                    Dialog dialog = new Dialog(getContext());

                    tvExtraPadding.setText(getString(R.string.tv_extra_padding) + extraPaddingMultiplier[0]);
                    swExtraPadding.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            extraPaddingMultiplier[0] = progress;
                            tvExtraPadding.setText(getString(R.string.tv_extra_padding) + extraPaddingMultiplier[0]);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

                    swHorizontalOrientation.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (swHorizontalOrientation.isChecked()) {
                                tvNPCols.setText("Rows");
                            } else {
                                tvNPCols.setText("Cols");
                            }
                        }
                    });
                    int finalScaledCollage = scaledCollage;
                    btnReloadCollage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            colsRowsValue[0] = nPColsRows.getValue();
                            collagedImage[0] = createCollage(collageBitmapList, colsRowsValue[0], swCropCollage.isChecked(), swHorizontalOrientation.isChecked(), swHalfFrame.isChecked(), extraPaddingMultiplier[0], lastPicked[0]);
                            Bitmap bitmap = Bitmap.createScaledBitmap(collagedImage[0], collagedImage[0].getWidth() * finalScaledCollage, collagedImage[0].getHeight() * finalScaledCollage, false);
                            imageView.setImageBitmap(bitmap);
                        }
                    });

                    List<Integer> indexesToLoad = new ArrayList<>();
                    for (int i : selectedImages) {
                        String hashCode = filteredGbcImages.get(i).getHashCode();
                        if (imageBitmapCache.get(hashCode) == null) {
                            indexesToLoad.add(i);
                        }
                    }

                    btnSaveCollage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            LocalDateTime now = null;
                            Date nowDate = new Date();
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                now = LocalDateTime.now();
                            }
                            File file = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateLocale + "_HH-mm-ss");

                                file = new File(Utils.IMAGES_FOLDER, "Collage_" + dtf.format(now) + ".png");
                            } else {
                                SimpleDateFormat sdf = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());
                                file = new File(Utils.IMAGES_FOLDER, "Collage_" + sdf.format(nowDate) + ".png");
                            }
                            try (FileOutputStream out = new FileOutputStream(file)) {
                                Bitmap bitmap = Bitmap.createScaledBitmap(collagedImage[0], collagedImage[0].getWidth() * exportSize, collagedImage[0].getHeight() * exportSize, false);
                                //Make square if checked in settings
                                if (exportSquare) {
                                    bitmap = makeSquareImage(bitmap);
                                }
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + getString(R.string.collage), Toast.LENGTH_LONG);
                                toast.show();
                                mediaScanner(file, getContext());
                                showNotification(getContext(), file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    btnCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    loadDialog.showDialog();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {

                            for (int i : selectedImages) {
                                Bitmap image = rotateBitmap(imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()), filteredGbcImages.get(i));
                                collageBitmapList.add(image);
                            }
                            try {
                                collagedImage[0] = createCollage(collageBitmapList, colsRowsValue[0], swCropCollage.isChecked(), swHorizontalOrientation.isChecked(), swHalfFrame.isChecked(), extraPaddingMultiplier[0], lastPicked[0]);
                                Bitmap bitmap = Bitmap.createScaledBitmap(collagedImage[0], collagedImage[0].getWidth() * finalScaledCollage, collagedImage[0].getHeight() * finalScaledCollage, false);
                                imageView.setImageBitmap(bitmap);
                                dialog.setContentView(collageView);
                                int screenHeight = displayMetrics.heightPixels;
                                int desiredHeight = screenHeight;
                                Window window = dialog.getWindow();
                                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, desiredHeight);
                                lastPicked[0] = collagedImage[0].getPixel(0, 0);
                                applyBorderToIV(ivPaddingColor, lastPicked[0]);
                                dialog.show();
                            } catch (IllegalArgumentException e) {
                                Utils.toast(getContext(), getString(R.string.hdr_exception));
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            loadDialog.dismissDialog();
                        }
                    });
                    asyncTask.execute();
                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;

            case R.id.action_duplicate:
                if (selectionMode[0]) {
                    DuplicateDialog duplicateDialog = new DuplicateDialog(getContext(), selectedImages, customGridViewAdapterImage, filteredGbcImages, getActivity());
                    duplicateDialog.createDuplicateDialog();
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
                            loadDialog.showDialog();
                            loadDialog.setLoadingDialogText("");
                            new DeleteImageAsyncTask(selectedImages, getActivity(), loadDialog).execute();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadDialog.dismissDialog();
                        }
                    });
                    loadDialog.showDialog();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, new AsyncTaskCompleteListener<Result>() {
                        @Override
                        public void onTaskComplete(Result result) {
                            loadDialog.dismissDialog();
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
                    TouchImageView imageView = averageView.findViewById(R.id.iv_average);
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
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateLocale + "_HH-mm-ss");

                            file = new File(Utils.IMAGES_FOLDER, "HDR" + dtf.format(now) + ".png");
                        } else {
                            SimpleDateFormat sdf = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());
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
                    loadDialog.setLoadingDialogText("");
                    loadDialog.showDialog();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, new AsyncTaskCompleteListener<Result>() {
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
                            loadDialog.dismissDialog();
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
                    Switch swLoop = dialogView.findViewById(R.id.swLoop);
                    Switch swSort = dialogView.findViewById(R.id.swSort);
                    Switch swCrop = dialogView.findViewById(R.id.swCrop);

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
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateLocale + "_HH-mm-ss");
                                gifFile = new File(Utils.IMAGES_FOLDER, "GIF_" + dtf.format(now) + ".gif");
                            } else {
                                SimpleDateFormat sdf = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());
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
                        loadDialog.showDialog();
                        LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, new AsyncTaskCompleteListener<Result>() {
                            @Override
                            public void onTaskComplete(Result result) {
                                bos.reset();
                                AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                                encoder.setRepeat(swLoop.isChecked() ? 0 : -1);
                                encoder.setFrameRate(fps[0]);
                                encoder.start(bos);
                                List<Bitmap> bitmapList = new ArrayList<>();

                                listInUse[0] = swSort.isChecked() ? sortedList : selectedImages;

                                for (int i : listInUse[0]) {
                                    Bitmap bitmap = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()).copy(imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()).getConfig(), true);
                                    if (swCrop.isChecked()) {
                                        if (bitmap.getHeight() == 144 && bitmap.getWidth() == 160) {
                                            bitmap = Bitmap.createBitmap(bitmap, 16, 16, 128, 112);
                                        }
                                        //For the wild frames
                                        else if (bitmap.getHeight() == 224 && crop) {
                                            bitmap = Bitmap.createBitmap(bitmap, 16, 40, 128, 112);
                                        }
                                    }
                                    bitmap = rotateBitmap(bitmap, (filteredGbcImages.get(i)));
                                    bitmapList.add(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * exportSize, bitmap.getHeight() * exportSize, false));
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
                                loadDialog.dismissDialog();
                            }
                        });
                        asyncTask.execute();
                    });

                    loadDialog.setLoadingDialogText("");
                    loadDialog.showDialog();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, result -> {
                        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                        encoder.setRepeat(swLoop.isChecked() ? 0 : -1);
                        encoder.setFrameRate(fps[0]);
                        encoder.start(bos);
                        List<Bitmap> bitmapList = new ArrayList<>();

                        for (int i : selectedImages) {
                            Bitmap bitmap = imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()).copy(imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()).getConfig(), true);
                            bitmap = rotateBitmap(bitmap, (filteredGbcImages.get(i)));
                            bitmapList.add(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * exportSize, bitmap.getHeight() * exportSize, false));
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
                        loadDialog.dismissDialog();
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
                    loadDialog.showDialog();
                    LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadDialog, result -> {
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
                            SimpleDateFormat dateFormat = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());

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
                        loadDialog.dismissDialog();

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

    public void updateFromMain(Context context) {
        if (Utils.gbcImagesList.size() > 0) {
            retrieveTags(gbcImagesList);
            checkSorting(context);
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

        if (selectedFilterTags.isEmpty() && hiddenFilterTags.isEmpty() && !filterByDate) {
            filteredGbcImages = Utils.gbcImagesList;
        } else {
            filteredGbcImages.clear();

            for (GbcImage gbcImageToFilter : Utils.gbcImagesList) {
                if (checkFilterPass(gbcImageToFilter)) {
                    filteredGbcImages.add(gbcImageToFilter);
                }
            }
        }

        imagesForPage = new ArrayList<>();
        itemsPerPage = StaticValues.imagesPage;
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
                        MainImageDialog.fastImageChange();
                        updatingFromChangeImage = false;
                    }
                    MainImageDialog.isChanging = false;
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
            if (itemsPerPage * currentPage >= filteredGbcImages.size()) {
                prevPage();
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
            port.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        } catch (Exception e) {
            tv.append(e.toString());
            Toast.makeText(getContext(), "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();
        }

        usbIoManager = new SerialInputOutputManager(port, this);
    }

    public static void hideSelectionOptions(Activity activity) {
        showEditMenuButton = false;
        selectedImages.clear();
        selectionMode[0] = false;
        gridView.setAdapter(customGridViewAdapterImage);
        StaticValues.fab.hide();
        updateTitleText();
        activity.invalidateOptionsMenu();
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

    private Bitmap getPrintBitmap(int colsRowsValue, int lastPicked, boolean swCropCollageChecked, boolean swHorizontalOrientationChecked, boolean swHalfFrameChecked, int extraPaddingMultiplier) {
        final int PRINT_WIDTH = 160; //  Prints need to be 160px in width
        List<Bitmap> collageBwBitmaps = new ArrayList<>();

        // Need to change all images to B&W and redo the collage first for the encoding to work
        for (int i : selectedImages) {
            GbcImage gbcImage = filteredGbcImages.get(i);
            //Need to change the palette to bw so the encodeImage method works
            Bitmap image;
            try {
                image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            image = rotateBitmap(image, gbcImage);

            collageBwBitmaps.add(image);

        }
        if (lastPicked != Color.parseColor("#000000")) {//If border is not black, make it always white.
            lastPicked = Color.parseColor("#FFFFFF");
        }

        Bitmap printBitmap = createCollage(collageBwBitmaps, colsRowsValue, swCropCollageChecked, swHorizontalOrientationChecked, swHalfFrameChecked, extraPaddingMultiplier, lastPicked);

        if (swHorizontalOrientationChecked) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            printBitmap = Bitmap.createBitmap(printBitmap, 0, 0, printBitmap.getWidth(), printBitmap.getHeight(), matrix, false);
        }

        if (printBitmap.getWidth() <= PRINT_WIDTH || (printBitmap.getWidth() > PRINT_WIDTH && colsRowsValue == 1)) {

            //If only 1 column or row, adjust the padding to fit the 160px wide
            int paddingMult = ((PRINT_WIDTH - printBitmap.getWidth()) / 8) / 2;
            if (paddingMult != 0) {
                //Add padding on each side to center it
                printBitmap = addPadding(printBitmap, paddingMult, Color.parseColor("#FFFFFF"));
            }
            return printBitmap;

        } else {
            // Calculate the proportional height
            int originalWidth = printBitmap.getWidth();
            int originalHeight = printBitmap.getHeight();
            float aspectRatio = (float) originalHeight / originalWidth;
            int desiredHeight = Math.round(160 * aspectRatio);

            // Adjust height to be multiple of 16
            while (desiredHeight % 16 != 0) {
                desiredHeight++;
            }
            // Scalate the bitmap to new size
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(printBitmap, 160, desiredHeight, false);

            // If new size if not multiple of 16, add white pixels at the bottom
            if (desiredHeight % 16 != 0) {
                int extraHeight = 16 - (desiredHeight % 16);
                Bitmap adjustedBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), desiredHeight + extraHeight, scaledBitmap.getConfig());
                Canvas canvas = new Canvas(adjustedBitmap);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(scaledBitmap, 0, 0, null);
                return adjustedBitmap;
            } else {
                return scaledBitmap;
            }
        }

    }

}
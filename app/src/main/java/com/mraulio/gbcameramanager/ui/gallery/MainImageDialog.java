package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectionMode;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.lastSeenGalleryImage;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.gbxcart.GBxCartConstants.BAUDRATE;
import static com.mraulio.gbcameramanager.ui.gallery.CollageMaker.addPadding;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.currentPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.itemsPerPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectedFilterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.nextPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.prevPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updateGridView;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updatingFromChangeImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.reloadTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.saveImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.shareImage;
import static com.mraulio.gbcameramanager.ui.gallery.PaperUtils.paperDialog;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.framesList;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.ui.usbserial.PrintOverArduino;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.transform.Result;

public class MainImageDialog implements SerialInputOutputManager.Listener {
    static final Bitmap[] selectedImage = new Bitmap[1];
    static final FramesFragment.CustomGridViewAdapterFrames[] frameAdapter = new FramesFragment.CustomGridViewAdapterFrames[1];
    static int frameIndex = 0;
    static int paletteIndex = 0;
    static CustomGridViewAdapterPalette adapterPalette;
    private static boolean keepFrame;
    private static int lastPage;
    private static int position;
    private static List<GbcImage> filteredGbcImages;
    private static Context context;
    private DisplayMetrics displayMetrics;
    private static boolean showPalettes;
    private Activity activity;
    UsbSerialPort port;
    SerialInputOutputManager usbIoManager;
    TextView tvResponseBytes;
    UsbDeviceConnection connection;
    TextView tv;
    public static boolean isChanging = false;
    UsbManager manager;
    GridView gridView;
    List<Integer> selectedImages;
    CustomGridViewAdapterImage customGridViewAdapterImage;
    private int imageViewMiniIndex = 0;
    static final int[] globalImageIndex = new int[1];
    Button printButton;
    Button btnPaperize;
    Button shareButton;
    Button saveButton;
    static Button paletteFrameSelButton;
    Button rotateButton;
    static ImageView imageView;
    static GridView gridViewPalette;
    static GridView gridViewFrames;
    static CheckBox cbFrameKeep;
    CheckBox cbCrop;
    static CheckBox cbInvert;
    static Spinner spFrameGroupsImage;
    static GbcImage gbcImage;

    public MainImageDialog (GridView gridView, boolean keepFrame, int lastPage, int position,
                           List<GbcImage> filteredGbcImages,  Context context, DisplayMetrics displayMetrics,
                           boolean showPalettes, Activity activity, UsbSerialPort port, SerialInputOutputManager usbIoManager,
                           TextView tvResponseBytes, UsbDeviceConnection connection, TextView tv, UsbManager manager, List<Integer> selectedImages, CustomGridViewAdapterImage customGridViewAdapterImage) {
        this.gridView = gridView;
        this.keepFrame = keepFrame;
        this.lastPage = lastPage;
        this.position = position;
        this.filteredGbcImages = filteredGbcImages;
        this.context = context;
        this.displayMetrics = displayMetrics;
        this.showPalettes = showPalettes;
        this.activity = activity;
        this.port = port;
        this.usbIoManager = usbIoManager;
        this.tvResponseBytes = tvResponseBytes;
        this.connection = connection;
        this.tv = tv;
        this.manager = manager;
        this.selectedImages = selectedImages;
        this.customGridViewAdapterImage = customGridViewAdapterImage;
    }

    public void showImageDialog() {
        if (!selectionMode[0]) {
            keepFrame = false;
            //Obtain selected image
            isChanging = false;
            if (currentPage != lastPage) {
                globalImageIndex[0] = position + (currentPage * itemsPerPage);
            } else {
                globalImageIndex[0] = filteredGbcImages.size() - (itemsPerPage - position);
            }
            //Put the last seen image as this one
            lastSeenGalleryImage = globalImageIndex[0];

            selectedImage[0] = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());

            gbcImage = filteredGbcImages.get(globalImageIndex[0]);

            // Create custom dialog
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.image_main_dialog);
            dialog.setCancelable(true);//So it closes when clicking outside or back button
            View dialogBackground = dialog.findViewById(android.R.id.content).getRootView();
            LinearLayout mainLayout = dialog.findViewById(R.id.main_image_layout);
            mainLayout.setOnClickListener(null);//So the fast image change doesn't happen when missclicking buttons inside the actual dialog
            imageView = dialog.findViewById(R.id.image_view);
            printButton = dialog.findViewById(R.id.print_button);
            btnPaperize = dialog.findViewById(R.id.btn_paperize_collage);

            shareButton = dialog.findViewById(R.id.share_button);
            saveButton = dialog.findViewById(R.id.save_button);
            paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
            rotateButton = dialog.findViewById(R.id.btnRotate);

            gridViewPalette = dialog.findViewById(R.id.gridViewPal);
            gridViewFrames = dialog.findViewById(R.id.gridViewFra);
            cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
            cbCrop = dialog.findViewById(R.id.cbCrop);
            cbInvert = dialog.findViewById(R.id.cbInvert);
            spFrameGroupsImage = dialog.findViewById(R.id.spFrameGroupsImage);

            dialogBackground.setOnTouchListener(new View.OnTouchListener() {
                boolean eventHandled = false;
                float downY = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    final int SWIPE_THRESHOLD = 200;
                    float y = event.getY();
                    float x = event.getX();
                    float upY = 0;
                    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

                    int topTwoThirdsHeight = screenHeight * 2 / 3;
                    int leftHalf = screenWidth / 2;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downY = event.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            upY = event.getY();
                            if (upY > (downY + SWIPE_THRESHOLD)) {
                                //Swipe down
                                if (!isChanging) {
                                    changeImage(true, globalImageIndex[0]);
                                }
                            } else if (upY < (downY - SWIPE_THRESHOLD)) {
                                //Swipe up
                                if (!isChanging) {
                                    changeImage(false, globalImageIndex[0]);
                                }
                            } else {//a click
                                if (y < topTwoThirdsHeight) {
                                    if (eventHandled) {
                                        return true; //If event already handled, so it doesn't add up
                                    }
                                    if (!isChanging) {
                                        changeImage(!(x > leftHalf), globalImageIndex[0]);
                                    }
                                } else {
                                    dialog.dismiss();
                                }
                            }
                            break;
                    }
                    return true;
                }
            });

            if (StaticValues.showPaperizeButton) {
                btnPaperize.setVisibility(VISIBLE);
            }

            selectedImage[0] = rotateBitmap(selectedImage[0], gbcImage);
            imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0],
                    selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
            int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
            imageView.setMaxHeight(maxHeight);

            if (StaticValues.printingEnabled) {
                printButton.setVisibility(VISIBLE);
            } else printButton.setVisibility(GONE);

            if (StaticValues.showRotationButton) {
                rotateButton.setVisibility(VISIBLE);
            }

            showPalettes = true;
            if (gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
                imageView.setBackgroundColor(context.getColor(R.color.star_color));
            } else if (gbcImage.getTags().contains(FILTER_FAVOURITE)) {
                imageView.setBackgroundColor(context.getColor(R.color.favorite));
            }
            if (gbcImage.isLockFrame()) {
                keepFrame = true;
                cbFrameKeep.setChecked(true);
            }
            if (!keepFrame && gbcImage.isInvertPalette()) {
                cbInvert.setChecked(true);
            } else if (keepFrame && gbcImage.isInvertFramePalette()) {
                cbInvert.setChecked(true);
            }

            cbInvert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!keepFrame) {
                        gbcImage.setInvertPalette(cbInvert.isChecked());
                    } else
                        gbcImage.setInvertFramePalette(cbInvert.isChecked());
                    try {
                        Bitmap bitmap;
                        if (!keepFrame)
                            bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertPalette(), keepFrame, true);
                        else
                            bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);

                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);
                        bitmap = rotateBitmap(bitmap, gbcImage);

                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                        updateGridView();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (keepFrame) {
                        keepFrame = false;
                        if (gbcImage.isInvertPalette()) {
                            cbInvert.setChecked(true);
                        } else cbInvert.setChecked(false);
                    } else {
                        keepFrame = true;
                        if (gbcImage.isInvertFramePalette()) {
                            cbInvert.setChecked(true);
                        } else cbInvert.setChecked(false);
                    }
                    try {
                        gbcImage.setLockFrame(keepFrame);
                        Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);
                        bitmap = rotateBitmap(bitmap, gbcImage);
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                        updateGridView();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            btnPaperize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Integer> indexToPaperize = new ArrayList<>();
                    List<Bitmap> bitmapsToPaperize = new ArrayList<>();
                    indexToPaperize.add(globalImageIndex[0]);
                    for (int i = 0; i < indexToPaperize.size(); i++) {
                        Bitmap bw_image = null;
                        try {
                            bw_image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                            //Not rotate wild frames
                            if ((gbcImage.getFrameId() != null && !hashFrames.get(gbcImage.getFrameId()).isWildFrame()) ||
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
                        bitmapsToPaperize.add(bw_image);
                    }
                    paperDialog(bitmapsToPaperize, context);
                }
            });
            rotateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap bitmap = Utils.imageBitmapCache.get(gbcImage.getHashCode());
                    int rotation = gbcImage.getRotation();
                    if (rotation != 0) {
                        rotation--;
                    } else rotation = 3;
                    gbcImage.setRotation(rotation);
                    bitmap = rotateBitmap(bitmap, gbcImage);
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                    new UpdateImageAsyncTask(gbcImage).execute();
                    updateGridView();
                }
            });

            imageView.setOnClickListener(new View.OnClickListener() {
                private int clickCount = 0;
                private final Handler handler = new Handler();
                private final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        //Single tap action
                        BigImageDialog bigImageDialog = new BigImageDialog(filteredGbcImages, context, activity);
                        bigImageDialog.showBigImageDialogSingleImage(globalImageIndex[0], imageView, dialog);
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
                        //If image is regular favorite, add the Superfav tag. If it's superfav, remove superfav and favorite
                        if (gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
                            HashSet<String> tags = gbcImage.getTags();
                            for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                String tag = iter.next();
                                if (tag.equals(FILTER_FAVOURITE)) {
                                    iter.remove();
                                }
                                if (tag.equals(FILTER_SUPER_FAVOURITE)) {
                                    iter.remove();
                                }
                                gbcImage.setTags(tags);
                                if (!selectedFilterTags.isEmpty())//Because right now I'm only filtering favourites
                                    dialog.dismiss();
                                imageView.setBackgroundColor(context.getColor(R.color.imageview_bg));

                            }
                        } else if (gbcImage.getTags().contains(FILTER_FAVOURITE) && !gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
                            gbcImage.addTag(FILTER_SUPER_FAVOURITE);
                            imageView.setBackgroundColor(context.getColor(R.color.star_color));
                        } else {
                            gbcImage.addTag(FILTER_FAVOURITE);
                            imageView.setBackgroundColor(context.getColor(R.color.favorite));
                        }

                        retrieveTags(gbcImagesList);
                        clickCount = 0;
                        //To save the image with the favorite tag to the database
                        new UpdateImageAsyncTask(gbcImage).execute();
                        reloadTags();
                        updateGridView();
                    }
                }
            });

            final List<GbcFrame>[] currentlyShowingFrames = new List[]{framesList};

            frameAdapter[0] = new FramesFragment.CustomGridViewAdapterFrames(context, R.layout.frames_row_items, currentlyShowingFrames[0], false, false);

            for (int i = 0; i < framesList.size(); i++) {
                if (framesList.get(i).getFrameId().equals(gbcImage.getFrameId())) {
                    frameIndex = i;
                    break;
                }
            }

            List<GbcFrame> framesWithNull = new ArrayList<>();
            framesWithNull.add(null);
            framesWithNull.addAll(framesList);

            frameAdapter[0].setLastSelectedPosition(frameIndex);
            gridViewFrames.setAdapter(frameAdapter[0]);
            List<String> frameGroupList = new ArrayList<>();
            frameGroupList.add(context.getString(R.string.sp_all_frame_groups));

            List<String> frameGroupIds = new ArrayList<>();//To access with index

            for (LinkedHashMap.Entry<String, String> entry : frameGroupsNames.entrySet()) {
                frameGroupList.add(entry.getValue() + " (" + entry.getKey() + ")");
                frameGroupIds.add(entry.getKey());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, frameGroupList);

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFrameGroupsImage.setAdapter(adapter);
            spFrameGroupsImage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i,
                                           long l) {
                    if (i == 0) {
                        //Show all frames
                        currentlyShowingFrames[0] = framesWithNull;
                    } else {
                        String frameGroupId = frameGroupIds.get(i - 1);
                        List<GbcFrame> currentGroupList = new ArrayList<>();
                        for (GbcFrame gbcFrame : framesList) {
                            String gbcFrameGroup = gbcFrame.getFrameId().substring(0, gbcFrame.getFrameId().length() - 2);//To remove the numbers at the end, always going to be 2 numbers
                            if (gbcFrameGroup.equals(frameGroupId)) {
                                currentGroupList.add(gbcFrame);
                            }
                        }
                        currentlyShowingFrames[0] = currentGroupList;
                    }

                    frameAdapter[0] = new FramesFragment.CustomGridViewAdapterFrames(context, R.layout.frames_row_items, currentlyShowingFrames[0], false, false);

                    //Set the selected frame if it's in the selected group
                    for (int x = 0; x < currentlyShowingFrames[0].size(); x++) {
                        if (currentlyShowingFrames[0].get(x) == null) {
                            frameAdapter[0].setLastSelectedPosition(0);

                        } else if (currentlyShowingFrames[0].get(x).getFrameId().equals(gbcImage.getFrameId())) {
                            frameAdapter[0].setLastSelectedPosition(x);
                        }
                    }
                    gridViewFrames.setAdapter(frameAdapter[0]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
            if (selectedImage[0].getHeight() != 144 && selectedImage[0].getHeight() != 160 && selectedImage[0].getHeight() != 224) {
                cbFrameKeep.setVisibility(GONE);
                paletteFrameSelButton.setVisibility(GONE);
            }

            gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int selectedFrameIndex, long id) {
                    //Action when clicking a frame inside the Dialog
                    try {
                        String frameId = null;
                        if (currentlyShowingFrames[0].get(selectedFrameIndex) != null) {
                            frameId = currentlyShowingFrames[0].get(selectedFrameIndex).getFrameId();
                        }
                        Bitmap bitmap = frameChange(gbcImage, frameId, gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);
                        bitmap = rotateBitmap(bitmap, gbcImage);
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));

                        frameAdapter[0].setLastSelectedPosition(selectedFrameIndex);
                        frameAdapter[0].notifyDataSetChanged();
                        updateGridView();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            adapterPalette = new CustomGridViewAdapterPalette(context, R.layout.palette_grid_item, Utils.sortedPalettes, false, false, false);

            for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
                if (Utils.sortedPalettes.get(i).getPaletteId().equals(gbcImage.getPaletteId())) {
                    paletteIndex = i;
                    break;
                }
            }
            adapterPalette.setLastSelectedImagePosition(paletteIndex);
            if (gbcImage.getFramePaletteId() == null) {
                paletteIndex = 0;
            } else {
                for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
                    if (Utils.sortedPalettes.get(i).getPaletteId().equals(gbcImage.getFramePaletteId())) {
                        paletteIndex = i;
                        break;
                    }
                }
            }
            adapterPalette.setLastSelectedFramePosition(paletteIndex);
            gridViewPalette.setAdapter(adapterPalette);

            gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int palettePosition, long id) {
                    //Action when clicking a palette inside the Dialog
                    //Set the new palette to the gbcImage image or frame
                    if (!keepFrame) {
                        gbcImage.setPaletteId(Utils.sortedPalettes.get(palettePosition).getPaletteId());
                        adapterPalette.setLastSelectedImagePosition(palettePosition);

                    } else {
                        gbcImage.setFramePaletteId(Utils.sortedPalettes.get(palettePosition).getPaletteId());
                        adapterPalette.setLastSelectedFramePosition(palettePosition);

                    }
                    try {
                        Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);

                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);

                        adapterPalette.notifyDataSetChanged();
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);
                        bitmap = rotateBitmap(bitmap, gbcImage);

                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                        updateGridView();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            paletteFrameSelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (showPalettes) {
                        showPalettes = false;
                        paletteFrameSelButton.setText(context.getString(R.string.btn_show_palettes));
                        gridViewPalette.setVisibility(GONE);
                        gridViewFrames.setVisibility(VISIBLE);
                        spFrameGroupsImage.setVisibility(VISIBLE);

                    } else {
                        showPalettes = true;
                        paletteFrameSelButton.setText(context.getString(R.string.btn_show_frames));
                        gridViewFrames.setVisibility(GONE);
                        gridViewPalette.setVisibility(VISIBLE);
                        spFrameGroupsImage.setVisibility(GONE);
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

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        View dialogView = activity.getLayoutInflater().inflate(R.layout.print_dialog, null);
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
                            List<byte[]> imageByteList = new ArrayList();
                            Bitmap image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                            //Not rotate wild frames
                            if ((gbcImage.getFrameId() != null && !hashFrames.get(gbcImage.getFrameId()).isWildFrame()) ||
                                    (image.getHeight() > 144 && gbcImage.getRotation() == 2)) {//If image is higher than a normal one, only rotate if it's 180º
                                image = rotateBitmap(image, gbcImage);
                            }

                            //If image is rotated sideways, add 8px on each side to print it in that orientation
                            if (image.getWidth() == 144 && image.getHeight() == 160) {
                                image = addPadding(image, 1, Color.parseColor("#FFFFFF"));
                            }
                            imageByteList.add(Utils.encodeImage(image, "bw"));
                            printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, imageByteList);
                        } catch (Exception e) {
                            tv.append(e.toString());
                            Toast toast = Toast.makeText(context, context.getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
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
                    List sharedList = new ArrayList();
                    sharedList.add(gbcImage);
                    shareImage(sharedList, context, cbCrop.isChecked());
                }
            });
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List saveList = new ArrayList();
                    saveList.add(gbcImage);
                    saveImage(saveList, context, cbCrop.isChecked());
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

        } else {//Multi Edition
            Collections.sort(selectedImages);
            List<Integer> indexesToLoad = new ArrayList<>();
            for (int i : selectedImages) {
                String hashCode = filteredGbcImages.get(i).getHashCode();
                if (Utils.imageBitmapCache.get(hashCode) == null) {
                    indexesToLoad.add(i);
                }
            }
            imageViewMiniIndex = 0;
            final Dialog dialog = new Dialog(context);
            LoadingDialog loadingDialog = new LoadingDialog(context, context.getString(R.string.load_cache));
            loadingDialog.showDialog();
            LoadBitmapCacheAsyncTask asyncTask = new LoadBitmapCacheAsyncTask(indexesToLoad, loadingDialog, new AsyncTaskCompleteListener<Result>() {
                @Override
                public void onTaskComplete(Result result) {
                    loadingDialog.dismissDialog();
                    globalImageIndex[0] = selectedImages.get(0);

                    keepFrame = false;

                    final Bitmap[] selectedImage = {Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode())};
                    // Create custom dialog
                    dialog.setContentView(R.layout.image_main_dialog);
                    dialog.setCancelable(true);//So it closes when clicking outside or back button
                    ImageView imageView = dialog.findViewById(R.id.image_view);
                    LinearLayout layoutSelected = dialog.findViewById(R.id.ly_selected_images);
                    layoutSelected.setVisibility(VISIBLE);
                    List<Bitmap> selectedBitmaps = new ArrayList<>();
                    List<GbcImage> selectedGbcImages = new ArrayList<>();
                    for (int i : selectedImages) {
                        selectedBitmaps.add(Utils.imageBitmapCache.get(filteredGbcImages.get(i).getHashCode()));
                        selectedGbcImages.add(filteredGbcImages.get(i));
                    }
                    selectedImage[0] = rotateBitmap(selectedImage[0], filteredGbcImages.get(globalImageIndex[0]));

                    imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
                    int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
                    imageView.setMaxHeight(maxHeight);

                    Button printButton = dialog.findViewById(R.id.print_button);
                    if (StaticValues.printingEnabled) {
                        printButton.setVisibility(VISIBLE);
                    } else printButton.setVisibility(GONE);

                    printButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                connect();
                                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                                usbIoManager.start();

                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                View dialogView = activity.getLayoutInflater().inflate(R.layout.print_dialog, null);
                                tvResponseBytes = dialogView.findViewById(R.id.tvResponseBytes);
                                builder.setView(dialogView);

                                builder.setNegativeButton(context.getString(R.string.dialog_close_button), new DialogInterface.OnClickListener() {
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
                                    List<byte[]> imageByteList = new ArrayList<>();
                                    for (GbcImage gbcImage : selectedGbcImages) {

                                        Bitmap image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);

                                        //Not rotate wild frames
                                        if ((gbcImage.getFrameId() != null && !hashFrames.get(gbcImage.getFrameId()).isWildFrame()) ||
                                                (image.getHeight() > 144 && gbcImage.getRotation() == 2)) {//If image is higher than a normal one, only rotate if it's 180º
                                            image = rotateBitmap(image, gbcImage);
                                        }

                                        //If image is rotated sideways, add 8px on each side to print it in that orientation
                                        if (image.getWidth() == 144 && image.getHeight() == 160) {
                                            image = addPadding(image, 1, Color.parseColor("#FFFFFF"));
                                        }
                                        imageByteList.add(Utils.encodeImage(image, "bw"));


                                    }
                                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, imageByteList);
                                } catch (Exception e) {
                                    tv.append(e.toString());
                                    Toast toast = Toast.makeText(context, context.getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                                    toast.show();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    Button shareButton = dialog.findViewById(R.id.share_button);
                    Button saveButton = dialog.findViewById(R.id.save_button);
                    Button rotateButton = dialog.findViewById(R.id.btnRotate);
                    if (StaticValues.showRotationButton) {
                        rotateButton.setVisibility(VISIBLE);
                    }
                    Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                    GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                    GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                    CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                    CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);
                    CheckBox cbInvert = dialog.findViewById(R.id.cbInvert);
                    Spinner spFrameGroupsImage = dialog.findViewById(R.id.spFrameGroupsImage);

                    CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(context, R.layout.palette_grid_item, Utils.sortedPalettes, false, false, false);

                    final List<GbcFrame>[] currentlyShowingFrames = new List[]{Utils.framesList};

                    final FramesFragment.CustomGridViewAdapterFrames[] frameAdapter = {new FramesFragment.CustomGridViewAdapterFrames(context, R.layout.frames_row_items, currentlyShowingFrames[0], false, false)};
                    int frameIndex = 0;
                    for (int i = 0; i < Utils.framesList.size(); i++) {
                        if (Utils.framesList.get(i).getFrameId().equals(filteredGbcImages.get(globalImageIndex[0]).getFrameId())) {
                            frameIndex = i;
                            break;
                        }
                    }

                    List<GbcFrame> framesWithNull = new ArrayList<>();
                    framesWithNull.add(null);
                    framesWithNull.addAll(framesList);

                    frameAdapter[0].setLastSelectedPosition(frameIndex);
                    gridViewFrames.setAdapter(frameAdapter[0]);
                    List<String> frameGroupList = new ArrayList<>();
                    frameGroupList.add(context.getString(R.string.sp_all_frame_groups));
                    List<String> frameGroupIds = new ArrayList<>();//To access with index
                    for (LinkedHashMap.Entry<String, String> entry : frameGroupsNames.entrySet()) {
                        frameGroupList.add(entry.getValue() + " (" + entry.getKey() + ")");
                        frameGroupIds.add(entry.getKey());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                            android.R.layout.simple_spinner_item, frameGroupList);

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spFrameGroupsImage.setAdapter(adapter);

                    spFrameGroupsImage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            if (i == 0) {
                                //Show all frames
                                currentlyShowingFrames[0] = framesWithNull;
                            } else {
                                String frameGroupId = frameGroupIds.get(i - 1);
                                List<GbcFrame> currentGroupList = new ArrayList<>();
                                for (GbcFrame gbcFrame : Utils.framesList) {
                                    String gbcFrameGroup = gbcFrame.getFrameId().substring(0, gbcFrame.getFrameId().length() - 2);//To remove the numbers at the end, always going to be 2 numbers
                                    if (gbcFrameGroup.equals(frameGroupId)) {
                                        currentGroupList.add(gbcFrame);
                                    }
                                }
                                currentlyShowingFrames[0] = currentGroupList;
                            }

                            frameAdapter[0] = new FramesFragment.CustomGridViewAdapterFrames(context, R.layout.frames_row_items, currentlyShowingFrames[0], false, false);

                            //Set the selected frame if it's in the selected group
                            GbcImage gbcImage = filteredGbcImages.get(globalImageIndex[0]);
                            for (int x = 0; x < currentlyShowingFrames[0].size(); x++) {
                                if (currentlyShowingFrames[0].get(x) == null) {
                                    frameAdapter[0].setLastSelectedPosition(0);

                                } else if (currentlyShowingFrames[0].get(x).getFrameId().equals(gbcImage.getFrameId())) {
                                    frameAdapter[0].setLastSelectedPosition(x);
                                }
                            }
                            gridViewFrames.setAdapter(frameAdapter[0]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    showPalettes = true;
                    Button btn_paperize = dialog.findViewById(R.id.btn_paperize_collage);
                    if (StaticValues.showPaperizeButton) {
                        btn_paperize.setVisibility(VISIBLE);
                    }
                    btn_paperize.setVisibility(GONE);

                    rotateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int rotation = filteredGbcImages.get(globalImageIndex[0]).getRotation();
                            rotation = (rotation != 0) ? rotation - 1 : 3;
                            for (int i : selectedImages) {
                                GbcImage gbcImage = filteredGbcImages.get(i);
                                gbcImage.setRotation(rotation);
                                new UpdateImageAsyncTask(gbcImage).execute();
                            }
                            Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                            showing = rotateBitmap(showing, filteredGbcImages.get(globalImageIndex[0]));
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);

                            updateGridView();
                        }
                    });
                    if (filteredGbcImages.get(globalImageIndex[0]).getTags().contains(FILTER_SUPER_FAVOURITE)) {
                        imageView.setBackgroundColor(context.getColor(R.color.star_color));
                    } else if (filteredGbcImages.get(globalImageIndex[0]).getTags().contains(FILTER_FAVOURITE)) {
                        imageView.setBackgroundColor(context.getColor(R.color.favorite));
                    }
                    if (filteredGbcImages.get(globalImageIndex[0]).isLockFrame()) {
                        keepFrame = true;
                        cbFrameKeep.setChecked(true);
                    }

                    if (!keepFrame && filteredGbcImages.get(globalImageIndex[0]).isInvertPalette()) {
                        cbInvert.setChecked(true);
                    } else if (keepFrame && filteredGbcImages.get(globalImageIndex[0]).isInvertFramePalette()) {
                        cbInvert.setChecked(true);
                    }
                    cbInvert.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (int i : selectedImages) {
                                GbcImage gbcImage = filteredGbcImages.get(i);
                                if (!keepFrame) {
                                    gbcImage.setInvertPalette(cbInvert.isChecked());
                                } else
                                    gbcImage.setInvertFramePalette(cbInvert.isChecked());
                                try {
                                    Bitmap bitmap;
                                    if (!keepFrame)
                                        bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertPalette(), keepFrame, true);
                                    else
                                        bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                                    Utils.imageBitmapCache.put(gbcImage.getHashCode(), bitmap);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                            showing = rotateBitmap(showing, filteredGbcImages.get(globalImageIndex[0]));
                            imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);
                            updateGridView();
                        }
                    });
                    for (int i = 0; i < Utils.framesList.size(); i++) {
                        if (Utils.framesList.get(i).getFrameId().equals(filteredGbcImages.get(globalImageIndex[0]).getFrameId())) {
                            frameIndex = i;
                            break;
                        }
                    }

                    frameAdapter[0].setLastSelectedPosition(frameIndex);
                    gridViewFrames.setAdapter(frameAdapter[0]);

                    imageView.setOnClickListener(new View.OnClickListener() {
                        private int clickCount = 0;
                        private final Handler handler = new Handler();
                        private final Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                //Single tap action
                                BigImageDialog bigImageDialog = new BigImageDialog(filteredGbcImages, context, activity);
                                bigImageDialog.showBigImageDialogMultipleImages(selectedImages, imageView, dialog);
                                clickCount = 0;
                            }
                        };

                        @Override
                        public void onClick(View v) {
                            List<Integer> indexesToRemove = new ArrayList<>();
                            clickCount++;
                            if (clickCount == 1) {
                                // Start timer to detect the double tap
                                handler.postDelayed(runnable, 300);
                            } else if (clickCount == 2) {
                                // Stop timer and make double tap action
                                handler.removeCallbacks(runnable);
                                //Get the favorite status of the first selected image
                                boolean isFav = filteredGbcImages.get(globalImageIndex[0]).getTags().contains(FILTER_FAVOURITE);
                                boolean isSuperFav = filteredGbcImages.get(globalImageIndex[0]).getTags().contains(FILTER_SUPER_FAVOURITE);

                                for (int i : selectedImages) {
                                    if (isSuperFav) {
                                        HashSet<String> tags = filteredGbcImages.get(i).getTags();
                                        for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                            String tag = iter.next();
                                            if (tag.equals(FILTER_FAVOURITE)) {
                                                iter.remove();
                                            }
                                            if (tag.equals(FILTER_SUPER_FAVOURITE)) {
                                                iter.remove();
                                            }
                                            filteredGbcImages.get(i).setTags(tags);
                                            imageView.setBackgroundColor(context.getColor(R.color.white));
                                            indexesToRemove.add(i);
                                        }

                                    } else if (isFav && !isSuperFav) {
                                        filteredGbcImages.get(i).addTag(FILTER_SUPER_FAVOURITE);
                                        imageView.setBackgroundColor(context.getColor(R.color.star_color));
                                    } else {
                                        filteredGbcImages.get(i).addTag(FILTER_FAVOURITE);
                                        imageView.setBackgroundColor(context.getColor(R.color.favorite));

                                    }
                                    retrieveTags(gbcImagesList);
                                    //To save the image with the favorite tag to the database
                                    new UpdateImageAsyncTask(filteredGbcImages.get(i)).execute();
                                }
                                if (!selectedFilterTags.isEmpty()) {
                                    dialog.dismiss();
                                }
                                if (selectionMode[0] && !selectedFilterTags.isEmpty()) {
                                    for (int i = indexesToRemove.size(); i > 0; i--) {
                                        filteredGbcImages.remove(indexesToRemove.get(i - 1));
                                    }
                                    selectedImages.clear();
                                    showEditMenuButton = false;
                                    StaticValues.fab.hide();
                                    selectionMode[0] = false;
                                    activity.invalidateOptionsMenu();
                                }

                                reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);
                                clickCount = 0;
                                updateGridView();
                            }
                        }
                    });

                    //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
                    if (selectedImage[0].getHeight() != 144 && selectedImage[0].getHeight() != 160 && selectedImage[0].getHeight() != 224) {
                        cbFrameKeep.setVisibility(GONE);
                        paletteFrameSelButton.setVisibility(GONE);
                    }

                    cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (keepFrame) {
                                keepFrame = false;
                                if (filteredGbcImages.get(globalImageIndex[0]).isInvertPalette()) {
                                    cbInvert.setChecked(true);
                                } else cbInvert.setChecked(false);
                            } else {
                                keepFrame = true;
                                if (filteredGbcImages.get(globalImageIndex[0]).isInvertFramePalette()) {
                                    cbInvert.setChecked(true);
                                } else cbInvert.setChecked(false);
                            }
                            for (int i : selectedImages) {
                                GbcImage gbcImage = filteredGbcImages.get(i);
                                //In case in the multiselect there are bigger images than standard
                                gbcImage.setLockFrame(keepFrame);
                                try {
                                    Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                                    Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), bitmap);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                            Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                            showing = rotateBitmap(showing, filteredGbcImages.get(globalImageIndex[0]));

                            imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);

                            updateGridView();
                        }
                    });

                    gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                            //Action when clicking a frame inside the Dialog

                            try {
                                for (int i : selectedImages) {
                                    GbcImage gbcImage = filteredGbcImages.get(i);
                                    String frameId = null;
                                    if (currentlyShowingFrames[0].get(selectedFrameIndex) != null) {
                                        frameId = currentlyShowingFrames[0].get(selectedFrameIndex).getFrameId();
                                    }
                                    Bitmap framed = frameChange(gbcImage, frameId, gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), true);
                                    Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), framed);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                            showing = rotateBitmap(showing, filteredGbcImages.get(globalImageIndex[0]));

                            imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                            frameAdapter[0].setLastSelectedPosition(selectedFrameIndex);
                            frameAdapter[0].notifyDataSetChanged();
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);

                            updateGridView();
                        }
                    });
                    int paletteIndex = 0;
                    for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
                        if (Utils.sortedPalettes.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getPaletteId())) {
                            paletteIndex = i;
                            break;
                        }
                    }
                    adapterPalette.setLastSelectedImagePosition(paletteIndex);
                    if (filteredGbcImages.get(globalImageIndex[0]).getFramePaletteId() == null) {
                        paletteIndex = 0;
                    } else {
                        for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
                            if (Utils.sortedPalettes.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getFramePaletteId())) {
                                paletteIndex = i;
                                break;
                            }
                        }
                    }
                    adapterPalette.setLastSelectedFramePosition(paletteIndex);
                    gridViewPalette.setAdapter(adapterPalette);
                    gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int palettePosition, long id) {
                            //Action when clicking a palette inside the Dialog

                            if (!keepFrame) {
                                filteredGbcImages.get(globalImageIndex[0]).setPaletteId(Utils.sortedPalettes.get(palettePosition).getPaletteId());
                                adapterPalette.setLastSelectedImagePosition(palettePosition);
                            } else {
                                filteredGbcImages.get(globalImageIndex[0]).setFramePaletteId(Utils.sortedPalettes.get(palettePosition).getPaletteId());
                                adapterPalette.setLastSelectedFramePosition(palettePosition);
                            }
                            boolean showingImageIsLockFrame = filteredGbcImages.get(globalImageIndex[0]).isLockFrame();
                            boolean showingImageIsInvertedImage = filteredGbcImages.get(globalImageIndex[0]).isInvertPalette();
                            boolean showingImageIsInvertedFrame = filteredGbcImages.get(globalImageIndex[0]).isInvertFramePalette();
                            String imagePaletteId = filteredGbcImages.get(globalImageIndex[0]).getPaletteId();
                            String framePaletteId = filteredGbcImages.get(globalImageIndex[0]).getFramePaletteId();

                            //setting the same frame and image palette, and also same settings for lockFrame and Invert palette for every image (frame will still be the same as before)
                            for (int i : selectedImages) {
                                GbcImage gbcImage = filteredGbcImages.get(i);
                                gbcImage.setLockFrame(showingImageIsLockFrame);
                                gbcImage.setInvertPalette(showingImageIsInvertedImage);
                                gbcImage.setInvertFramePalette(showingImageIsInvertedFrame);
                                gbcImage.setPaletteId(imagePaletteId);
                                gbcImage.setFramePaletteId(framePaletteId);
                                try {
                                    Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                                    Utils.imageBitmapCache.put(filteredGbcImages.get(i).getHashCode(), bitmap);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                            adapterPalette.notifyDataSetChanged();
                            reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);
                            Bitmap showing = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex[0]).getHashCode());
                            showing = rotateBitmap(showing, filteredGbcImages.get(globalImageIndex[0]));

                            imageView.setImageBitmap(Bitmap.createScaledBitmap(showing, showing.getWidth() * 6, showing.getHeight() * 6, false));
                            updateGridView();
                        }
                    });

                    paletteFrameSelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (showPalettes) {
                                showPalettes = false;
                                paletteFrameSelButton.setText(context.getString(R.string.btn_show_palettes));
                                gridViewPalette.setVisibility(GONE);
                                gridViewFrames.setVisibility(VISIBLE);
                                spFrameGroupsImage.setVisibility(VISIBLE);

                            } else {
                                showPalettes = true;
                                paletteFrameSelButton.setText(context.getString(R.string.btn_show_frames));
                                gridViewFrames.setVisibility(GONE);
                                gridViewPalette.setVisibility(VISIBLE);
                                spFrameGroupsImage.setVisibility(GONE);

                            }
                        }
                    });
                    shareButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List<GbcImage> sharedList = new ArrayList<>();
                            for (int i : selectedImages) {
                                GbcImage gbcImage = filteredGbcImages.get(i);
                                sharedList.add(gbcImage);
                            }
                            shareImage(sharedList, context, cbCrop.isChecked());
                        }
                    });
                    saveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            saveImage(selectedGbcImages, context, cbCrop.isChecked());
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
                    reloadLayout(layoutSelected, imageView, cbFrameKeep, cbInvert, paletteFrameSelButton, adapterPalette, frameAdapter[0]);
                }

            });
            asyncTask.execute();

            dialog.show();
            customGridViewAdapterImage.notifyDataSetChanged();
        }
    }

    public static void fastImageChange() {

        if (currentPage != lastPage) {
            globalImageIndex[0] = position + (currentPage * itemsPerPage);
        } else {
            globalImageIndex[0] = filteredGbcImages.size() - (itemsPerPage - position);
        }
        //Put the last seen image as this one
        lastSeenGalleryImage = globalImageIndex[0];
        gbcImage = filteredGbcImages.get(globalImageIndex[0]);

        selectedImage[0] = Utils.imageBitmapCache.get(gbcImage.getHashCode());
        selectedImage[0] = rotateBitmap(selectedImage[0], gbcImage);

        imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0],
                selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));

        if (gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
            imageView.setBackgroundColor(context.getColor(R.color.star_color));
        } else if (gbcImage.getTags().contains(FILTER_FAVOURITE)) {
            imageView.setBackgroundColor(context.getColor(R.color.favorite));
        } else {
            imageView.setBackgroundColor(context.getColor(R.color.imageview_bg));
        }

        //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
        if (selectedImage[0].getHeight() != 144 && selectedImage[0].getHeight() != 160 && selectedImage[0].getHeight() != 224) {
            cbFrameKeep.setVisibility(GONE);
            paletteFrameSelButton.setVisibility(GONE);
            showPalettes = true;
            paletteFrameSelButton.setText(context.getString(R.string.btn_show_frames));
            gridViewFrames.setVisibility(GONE);
            gridViewPalette.setVisibility(VISIBLE);
            spFrameGroupsImage.setVisibility(GONE);
        } else {
            cbFrameKeep.setVisibility(VISIBLE);
            paletteFrameSelButton.setVisibility(VISIBLE);
        }
        spFrameGroupsImage.setSelection(0);

        for (int i = 0; i < framesList.size(); i++) {
            if (framesList.get(i).getFrameId().equals(gbcImage.getFrameId())) {
                frameIndex = i;
                break;
            }
        }
        frameAdapter[0].setLastSelectedPosition(frameIndex + 1);
        frameAdapter[0].notifyDataSetChanged();

        for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
            if (Utils.sortedPalettes.get(i).getPaletteId().equals(gbcImage.getPaletteId())) {
                paletteIndex = i;
                break;
            }
        }
        adapterPalette.setLastSelectedImagePosition(paletteIndex);

        if (gbcImage.getFramePaletteId() == null) {
            paletteIndex = 0;
        } else {
            for (int i = 0; i < Utils.sortedPalettes.size(); i++) {

                if (Utils.sortedPalettes.get(i).getPaletteId().equals(gbcImage.getFramePaletteId())) {
                    paletteIndex = i;
                    break;
                }
            }
        }
        adapterPalette.setLastSelectedFramePosition(paletteIndex);
        adapterPalette.notifyDataSetChanged();

        if (gbcImage.isLockFrame()) {
            keepFrame = true;
            cbFrameKeep.setChecked(true);
        } else {
            keepFrame = false;
            cbFrameKeep.setChecked(false);
        }

        if (!keepFrame && gbcImage.isInvertPalette()) {
            cbInvert.setChecked(true);
        } else if (keepFrame && gbcImage.isInvertFramePalette()) {
            cbInvert.setChecked(true);
        } else {
            cbInvert.setChecked(false);
        }

    }

    private void changeImage(boolean prevImage, int globalImageIndex) {
        //Touching 2/3 superior part of the exterior of the dialog
        boolean updateOutside = false; //Because updatingFromChangeImage changes in an asynctask and it's status not to be trusted
        if (!prevImage) {//Touching right part of the screen outside dialog
            if (position == itemsPerPage - 1 && currentPage != lastPage) {
                //We are at the end of the current page.
                //Do a nextPage and select first item if possible
                updatingFromChangeImage = true;
                updateOutside = true;
                position = 0;
                isChanging = true;
                nextPage();
            }
            if (globalImageIndex < filteredGbcImages.size() - 1) {
                if (!updateOutside) {
                    position++;
                    fastImageChange();
                }
            } else {
                toast(context, context.getString(R.string.toast_last_image));
            }
        } else {//Touching left part of the screen outside dialog
            if (position == 0 && currentPage != 0) {
                updatingFromChangeImage = true;
                updateOutside = true;
                position = StaticValues.imagesPage - 1;
                isChanging = true;
                prevPage();
            }
            if (globalImageIndex > 0) {
                if (!updateOutside) {
                    position--;
                    fastImageChange();
                }
            } else {
                toast(context, context.getString(R.string.toast_first_image));
            }
        }
    }

    private void connect() {
        manager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
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
            Toast.makeText(context, "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();
        }

        usbIoManager = new SerialInputOutputManager(port, this);
    }

    private void reloadLayout(LinearLayout layoutSelected, ImageView imageView, CheckBox
            keepFrameCb, CheckBox invertCb, Button
                                      paletteFrameSelButton, CustomGridViewAdapterPalette
                                      adapterPalette, FramesFragment.CustomGridViewAdapterFrames frameAdapter) {
        layoutSelected.removeAllViews();
        List<ImageView> imageViewList = new ArrayList<>();
        for (int i = 0; i < selectedImages.size(); i++) {
            GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));
            ImageView imageViewMini = new ImageView(context);
            imageViewMini.setId(i);
            imageViewMini.setPadding(5, 5, 5, 5);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            imageViewMini.setImageBitmap(rotateBitmap(image, gbcImage));
            if (gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
                imageViewMini.setBackgroundColor(context.getColor(R.color.star_color));
            } else if (gbcImage.getTags().contains(FILTER_FAVOURITE)) {
                imageViewMini.setBackgroundColor(context.getColor(R.color.favorite));
            }
            if (i == imageViewMiniIndex) {
                imageViewMini.setBackgroundColor(context.getColor(R.color.teal_700));
            }
            imageViewList.add(imageViewMini);
            imageViewMini.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageViewId = view.getId(); // Get the ImageView id
                    Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
                    if (image.getHeight() != 144 && image.getHeight() != 160 && image.getHeight() != 224) {
                        keepFrameCb.setVisibility(GONE);
                        paletteFrameSelButton.setVisibility(GONE);
                    } else {
                        keepFrameCb.setVisibility(VISIBLE);
                        paletteFrameSelButton.setVisibility(VISIBLE);
                    }
                    image = rotateBitmap(image, gbcImage);
                    imageViewMiniIndex = imageViewId;
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
                    globalImageIndex[0] = selectedImages.get(imageViewId);
                    boolean isFav = gbcImage.getTags().contains(FILTER_FAVOURITE);
                    boolean isSuperFav = gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE);

                    for (int i = 0; i < selectedImages.size(); i++) {
                        GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));

                        if (i == imageViewMiniIndex) {
                            imageViewList.get(i).setBackgroundColor(context.getColor(R.color.teal_700));
                        } else if (gbcImage.getTags().contains(FILTER_SUPER_FAVOURITE)) {
                            imageViewList.get(i).setBackgroundColor(context.getColor(R.color.star_color));
                        } else if (gbcImage.getTags().contains(FILTER_FAVOURITE)) {
                            imageViewList.get(i).setBackgroundColor(context.getColor(R.color.favorite));
                        } else {
                            imageViewList.get(i).setBackgroundColor(context.getColor(R.color.white));
                        }
                    }
                    if (isSuperFav) {
                        imageView.setBackgroundColor(context.getColor(R.color.star_color));

                    } else if (isFav) {
                        imageView.setBackgroundColor(context.getColor(R.color.favorite));

                    } else {
                        imageView.setBackgroundColor(context.getColor(R.color.white));
                    }
                    if (gbcImage.isLockFrame()) {
                        keepFrameCb.setChecked(true);
                    } else keepFrameCb.setChecked(false);
                    if (!keepFrameCb.isChecked()) {
                        if (gbcImage.isInvertPalette()) {
                            invertCb.setChecked(true);
                        } else invertCb.setChecked(false);
                    } else {
                        if (gbcImage.isInvertFramePalette()) {
                            invertCb.setChecked(true);
                        } else invertCb.setChecked(false);
                    }
                    int paletteIndex = 0;

                    for (int i = 0; i < Utils.sortedPalettes.size(); i++) {
                        if (Utils.sortedPalettes.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getPaletteId())) {
                            paletteIndex = i;
                            break;
                        }
                    }
                    adapterPalette.setLastSelectedImagePosition(paletteIndex);
                    if (filteredGbcImages.get(globalImageIndex[0]).getFramePaletteId() == null) {
                        paletteIndex = 0;
                    } else {
                        for (int i = 0; i < Utils.sortedPalettes.size(); i++) {

                            if (Utils.sortedPalettes.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex[0]).getFramePaletteId())) {
                                paletteIndex = i;
                                break;
                            }
                        }
                    }
                    adapterPalette.setLastSelectedFramePosition(paletteIndex);
                    adapterPalette.notifyDataSetChanged();
                    int frameIndex = 0;
                    for (int i = 0; i < Utils.framesList.size(); i++) {
                        if (Utils.framesList.get(i).getFrameId().equals(filteredGbcImages.get(globalImageIndex[0]).getFrameId())) {
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
        activity.runOnUiThread(() -> {
            tvResponseBytes.append(finalHexString);
        });
    }

    @Override
    public void onRunError(Exception e) {

    }
}
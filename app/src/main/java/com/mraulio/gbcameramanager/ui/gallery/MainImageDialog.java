package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mraulio.gbcameramanager.gbxcart.GBxCartConstants.BAUDRATE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.filterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.nextPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.prevPage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updateGridView;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.reloadTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.saveImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.shareImage;
import static com.mraulio.gbcameramanager.ui.gallery.PaperUtils.paperDialog;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.framesList;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.ui.usbserial.PrintOverArduino;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class MainImageDialog {

    private boolean crop;
    private boolean keepFrame;
    private int currentPage;
    private int lastPage;
    private int position;
    private int itemsPerPage;
    private List<GbcImage> filteredGbcImages;
    private int lastSeenGalleryImage;
    private Context context;
    private DisplayMetrics displayMetrics;
    private boolean showPalettes;
    private Activity activity;
    UsbSerialPort port;
    SerialInputOutputManager usbIoManager;
    TextView tvResponseBytes;
    UsbDeviceConnection connection;
    TextView tv;
    UsbManager manager;
    GridView gridView;

    public MainImageDialog(GridView gridView, boolean crop, boolean keepFrame, int currentPage, int lastPage, int position, int itemsPerPage,
                           List<GbcImage> filteredGbcImages, int lastSeenGalleryImage, Context context, DisplayMetrics displayMetrics,
                           boolean showPalettes, Activity activity, UsbSerialPort port, SerialInputOutputManager usbIoManager,
                           TextView tvResponseBytes, UsbDeviceConnection connection, TextView tv, UsbManager manager) {
        this.gridView = gridView;
        this.crop = crop;
        this.keepFrame = keepFrame;
        this.currentPage = currentPage;
        this.lastPage = lastPage;
        this.position = position;
        this.itemsPerPage = itemsPerPage;
        this.filteredGbcImages = filteredGbcImages;
        this.lastSeenGalleryImage = lastSeenGalleryImage;
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
    }

    public void showImageDialog() {

        crop = false;
        keepFrame = false;
        //Obtain selected image
        int globalImageIndex;
        if (currentPage != lastPage) {
            globalImageIndex = position + (currentPage * itemsPerPage);
        } else {
            globalImageIndex = filteredGbcImages.size() - (itemsPerPage - position);
        }
        //Put the last seen image as this one
        lastSeenGalleryImage = globalImageIndex;

        final Bitmap[] selectedImage = {Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode())};
        // Create custom dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.image_main_dialog);
        dialog.setCancelable(true);//So it closes when clicking outside or back button
        View dialogBackground = dialog.findViewById(android.R.id.content).getRootView();
        dialogBackground.setOnTouchListener(new View.OnTouchListener() {
            boolean eventHandled = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float y = event.getY();
                float x = event.getX();
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

                int topTwoThirdsHeight = screenHeight * 2 / 3;
                int leftHalf = screenWidth / 2;
                int newPosition;
                //Touching 2/3 superior part of the exterior of the dialog
                if (y < topTwoThirdsHeight) {
                    if (eventHandled) {
                        return true; //If event already handled, so it doesn't add up
                    }
                    if (x > leftHalf) {//Touching right part of the screen outside dialog
                        if (position == itemsPerPage - 1 && currentPage != lastPage) {
                            //We are at the end of the current page.
                            //Do a nextPage and select first item if possible
                            nextPage();
                            newPosition = 0;

                        } else {
                            newPosition = position + 1;
                        }
                        if (globalImageIndex < filteredGbcImages.size() - 1) {
                            gridView.performItemClick(gridView.getChildAt(newPosition), newPosition, gridView.getAdapter().getItemId(newPosition));
                            eventHandled = true;
                            dialog.dismiss();
                        } else {
                            toast(context, context.getString(R.string.toast_last_image));
                        }
                    } else {//Touching left part of the screen outside dialog
                        if (position == 0 && currentPage != 0) {
                            prevPage();
                            newPosition = MainActivity.imagesPage-1;
                            //We are at the end of the current page.
                        } else {
                            newPosition = position - 1;
                        }
                        if (globalImageIndex > 0) {
                            gridView.performItemClick(gridView.getChildAt(newPosition), newPosition, gridView.getAdapter().getItemId(newPosition));
                            eventHandled = true;
                            dialog.dismiss();
                        } else {
                            toast(context, context.getString(R.string.toast_first_image));
                        }
                    }
                    return true; // Consumes the event
                }
                return false;
            }
        });
        ImageView imageView = dialog.findViewById(R.id.image_view);
        Button btn_paperize = dialog.findViewById(R.id.btnPaperize);
        if (MainActivity.showPaperizeButton) {
            btn_paperize.setVisibility(VISIBLE);
        }

        selectedImage[0] = rotateBitmap(selectedImage[0], filteredGbcImages.get(globalImageIndex));
        imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
        int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
        imageView.setMaxHeight(maxHeight);

        Button printButton = dialog.findViewById(R.id.print_button);
        if (MainActivity.printingEnabled) {
            printButton.setVisibility(VISIBLE);
        } else printButton.setVisibility(GONE);

        Button shareButton = dialog.findViewById(R.id.share_button);
        Button saveButton = dialog.findViewById(R.id.save_button);
        Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
        Button rotateButton = dialog.findViewById(R.id.btnRotate);

        GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
        GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
        CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
        CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);
        CheckBox cbInvert = dialog.findViewById(R.id.cbInvert);
        Spinner spFrameGroupsImage = dialog.findViewById(R.id.spFrameGroupsImage);

        if (MainActivity.showRotationButton) {
            rotateButton.setVisibility(VISIBLE);
        }
        showPalettes = true;
        if (filteredGbcImages.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
            imageView.setBackgroundColor(context.getColor(R.color.favorite));
        }
        if (filteredGbcImages.get(globalImageIndex).isLockFrame()) {
            keepFrame = true;
            cbFrameKeep.setChecked(true);
        }
        if (!keepFrame && filteredGbcImages.get(globalImageIndex).isInvertPalette()) {
            cbInvert.setChecked(true);
        } else if (keepFrame && filteredGbcImages.get(globalImageIndex).isInvertFramePalette()) {
            cbInvert.setChecked(true);
        }
        cbInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
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
                    if (filteredGbcImages.get(globalImageIndex).isInvertPalette()) {
                        cbInvert.setChecked(true);
                    } else cbInvert.setChecked(false);
                } else {
                    keepFrame = true;
                    if (filteredGbcImages.get(globalImageIndex).isInvertFramePalette()) {
                        cbInvert.setChecked(true);
                    } else cbInvert.setChecked(false);
                }
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
                try {
                    gbcImage.setLockFrame(keepFrame);
                    Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                    Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);
                    bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                    updateGridView();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cbCrop.setOnClickListener(v -> {
            if (!crop) {
                crop = true;
            } else {
                crop = false;
            }
        });

        btn_paperize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Integer> indexToPaperize = new ArrayList<>();
                indexToPaperize.add(globalImageIndex);
                paperDialog(indexToPaperize, context);
            }
        });
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
                Bitmap bitmap = Utils.imageBitmapCache.get(gbcImage.getHashCode());
                int rotation = gbcImage.getRotation();
                if (rotation != 3) {
                    rotation++;
                } else rotation = 0;
                gbcImage.setRotation(rotation);
                bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));
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
                    bigImageDialog.showBigImageDialogSingleImage(globalImageIndex, imageView);
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
                            if (!filterTags.isEmpty())//Because right now I'm only filtering favourites
                                dialog.dismiss();
                            imageView.setBackgroundColor(context.getColor(R.color.imageview_bg));
                        }
                    } else {
                        filteredGbcImages.get(globalImageIndex).addTag("__filter:favourite__");
                        imageView.setBackgroundColor(context.getColor(R.color.favorite));
                    }
                    retrieveTags(gbcImagesList);

                    clickCount = 0;
                    //To save the image with the favorite tag to the database
                    new UpdateImageAsyncTask(filteredGbcImages.get(globalImageIndex)).execute();
                    reloadTags();
                    updateGridView();
                }
            }
        });

        final List<GbcFrame>[] currentlyShowingFrames = new List[]{framesList};

        final FramesFragment.CustomGridViewAdapterFrames[] frameAdapter = {new FramesFragment.CustomGridViewAdapterFrames(context, R.layout.frames_row_items, currentlyShowingFrames[0], false, false)};
        int frameIndex = 0;
        for (int i = 0; i < framesList.size(); i++) {
            if (framesList.get(i).getFrameId().equals(filteredGbcImages.get(globalImageIndex).getFrameId())) {
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
        for (
                LinkedHashMap.Entry<String, String> entry : frameGroupsNames.entrySet()) {
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
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
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
            public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                //Action when clicking a frame inside the Dialog
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);

                try {
                    String frameId = null;
                    if (currentlyShowingFrames[0].get(selectedFrameIndex) != null) {
                        frameId = currentlyShowingFrames[0].get(selectedFrameIndex).getFrameId();
                    }
                    Bitmap bitmap = frameChange(gbcImage, frameId, gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);
                    Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);
                    bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));

                    frameAdapter[0].setLastSelectedPosition(selectedFrameIndex);
                    frameAdapter[0].notifyDataSetChanged();
                    updateGridView();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(context, R.layout.palette_grid_item, Utils.gbcPalettesList, false, false);
        int paletteIndex = 0;
        for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {
            if (Utils.gbcPalettesList.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex).getPaletteId())) {
                paletteIndex = i;
                break;
            }
        }
        adapterPalette.setLastSelectedImagePosition(paletteIndex);
        if (filteredGbcImages.get(globalImageIndex).getFramePaletteId() == null) {
            paletteIndex = 0;
        } else {
            for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {

                if (Utils.gbcPalettesList.get(i).getPaletteId().equals(filteredGbcImages.get(globalImageIndex).getFramePaletteId())) {
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
                GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);

                //Set the new palette to the gbcImage image or frame
                if (!keepFrame) {
                    filteredGbcImages.get(globalImageIndex).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                    adapterPalette.setLastSelectedImagePosition(palettePosition);

                } else {
                    filteredGbcImages.get(globalImageIndex).setFramePaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                    adapterPalette.setLastSelectedFramePosition(palettePosition);

                }
                try {
                    Bitmap bitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), keepFrame, true);

                    Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);

                    adapterPalette.notifyDataSetChanged();
                    Utils.imageBitmapCache.put(filteredGbcImages.get(globalImageIndex).getHashCode(), bitmap);
                    bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));

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
                        GbcImage gbcImage = filteredGbcImages.get(globalImageIndex);
                        List<byte[]> imageByteList = new ArrayList();
                        Bitmap image = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), false);
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
                GbcImage sharedImage = filteredGbcImages.get(globalImageIndex);
//                            Bitmap sharedBitmap = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);
                List sharedList = new ArrayList();
                sharedList.add(sharedImage);
                shareImage(sharedList, context);
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List saveList = new ArrayList();
                saveList.add(filteredGbcImages.get(globalImageIndex));
                saveImage(saveList, context);
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

        usbIoManager = new SerialInputOutputManager(port, (SerialInputOutputManager.Listener) this);
    }
}


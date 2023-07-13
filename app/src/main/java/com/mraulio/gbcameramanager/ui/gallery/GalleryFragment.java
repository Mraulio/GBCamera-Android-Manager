package com.mraulio.gbcameramanager.ui.gallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
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
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GalleryFragment extends Fragment implements SerialInputOutputManager.Listener {

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    static UsbDeviceConnection connection;
    static UsbSerialPort port = null;
    public static GridView gridView;
    private static AlertDialog loadingDialog;

    private static int itemsPerPage = MainActivity.imagesPage;
    static int startIndex = 0;
    static int endIndex = 0;
    public static int currentPage = 0;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);
        loadingDialog = Utils.loadingDialog(getContext());


        Button btnPrevPage = (Button) view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = (Button) view.findViewById(R.id.btnNextPage);
        Button btnFirstPage = (Button) view.findViewById(R.id.btnFirstPage);
        Button btnLastPage = (Button) view.findViewById(R.id.btnLastPage);

        tv_page = (TextView) view.findViewById(R.id.tv_page);
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
                }
            }
        });

        /**
         * Dialog when clicking an image
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedPosition = 0;
                crop = false;
                keepFrame = false;
                // Obtain selected image
                if (currentPage != lastPage) {
                    selectedPosition = position + (currentPage * itemsPerPage);
                } else {
                    selectedPosition = Utils.gbcImagesList.size() - (itemsPerPage - position);
                }
                final Bitmap[] selectedImage = {Utils.imageBitmapCache.get(Utils.gbcImagesList.get(selectedPosition).getHashCode())};
                // Create custom dialog
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);
                dialog.setCancelable(true);//So it closes when clicking outside or back button

                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
                int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
                imageView.setMaxHeight(maxHeight);

                Button printButton = dialog.findViewById(R.id.print_button);
                if (MainActivity.printingEnabled) {
                    printButton.setVisibility(View.VISIBLE);
                } else printButton.setVisibility(View.GONE);

                Button shareButton = dialog.findViewById(R.id.share_button);
                Button saveButton = dialog.findViewById(R.id.save_button);
                Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);

                showPalettes = true;

                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = Utils.gbcImagesList.size() - (itemsPerPage - position);
                }
                if (Utils.gbcImagesList.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                    imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                }
                if (Utils.gbcImagesList.get(globalImageIndex).isLockFrame()) {
                    keepFrame = true;
                    cbFrameKeep.setChecked(true);
                }

                imageView.setOnClickListener(new View.OnClickListener() {
                    private int clickCount = 0;
                    private final Handler handler = new Handler();
                    private final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            //Single tap action
                            showCustomDialog(Utils.imageBitmapCache.get(Utils.gbcImagesList.get(globalImageIndex).getHashCode()));
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
                            if (Utils.gbcImagesList.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                                List<String> tags = Utils.gbcImagesList.get(globalImageIndex).getTags();
                                for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                    String nombre = iter.next();
                                    if (nombre.equals("__filter:favourite__")) {
                                        iter.remove();
                                    }
                                    Utils.gbcImagesList.get(globalImageIndex).setTags(tags);
                                    imageView.setBackgroundColor(getContext().getColor(R.color.white));
                                }
                            } else {
                                Utils.gbcImagesList.get(globalImageIndex).addTag("__filter:favourite__");
                                imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                            }
                            clickCount = 0;
                            updateGridView(currentPage);
                        }
                        //To save the image with the favorite tag to the database
                        new SaveImageAsyncTask(Utils.gbcImagesList.get(globalImageIndex)).execute();
                    }
                });

                FramesFragment.CustomGridViewAdapterFrames frameAdapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Utils.framesList, false, false);
                int frameIndex = 0;
                for (int i = 0; i < Utils.framesList.size(); i++) {
                    if (Utils.framesList.get(i).getFrameName().equals(Utils.gbcImagesList.get(globalImageIndex).getFrameId())) {
                        frameIndex = i;
                        break;
                    }
                }

                frameAdapter.setLastSelectedPosition(frameIndex);
                gridViewFrames.setAdapter(frameAdapter);

                //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
                if (selectedImage[0].getHeight() != 144) {
                    cbFrameKeep.setVisibility(View.GONE);
                    paletteFrameSelButton.setVisibility(View.GONE);
                }

                cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (keepFrame) keepFrame = false;
                        else keepFrame = true;
                        Bitmap bitmap = null;
                        try {
                            bitmap = frameChange(Utils.gbcImagesList.get(globalImageIndex), Utils.imageBitmapCache.get(Utils.gbcImagesList.get(globalImageIndex).getHashCode()), Utils.gbcImagesList.get(globalImageIndex).getFrameId(), keepFrame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        GbcImage gbcImage = Utils.gbcImagesList.get(globalImageIndex);
                        gbcImage.setLockFrame(keepFrame);
                        Utils.imageBitmapCache.put(Utils.gbcImagesList.get(globalImageIndex).getHashCode(), bitmap);
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                        new SaveImageAsyncTask(gbcImage).execute();
                        updateGridView(currentPage);

                    }
                });
                cbCrop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!crop) {
                            crop = true;
                        } else {
                            crop = false;
                        }
                    }
                });
                gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                        //Action when clicking a frame inside the Dialog
                        Bitmap framed = null;
                        Utils.gbcImagesList.get(globalImageIndex).setFrameId(Utils.framesList.get(selectedFrameIndex).getFrameName());//Need to set the frame index before changing it because if not it's not added to db

                        try {
                            framed = frameChange(Utils.gbcImagesList.get(globalImageIndex), Utils.imageBitmapCache.get(Utils.gbcImagesList.get(globalImageIndex).getHashCode()), Utils.framesList.get(selectedFrameIndex).getFrameName(), keepFrame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(framed, framed.getWidth() * 6, framed.getHeight() * 6, false));
                        selectedImage[0] = framed;
                        Utils.imageBitmapCache.put(Utils.gbcImagesList.get(globalImageIndex).getHashCode(), framed);
                        frameAdapter.setLastSelectedPosition(selectedFrameIndex);
                        frameAdapter.notifyDataSetChanged();
                        updateGridView(currentPage);
                    }
                });
                CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Utils.gbcPalettesList, false, false);
                int paletteIndex = 0;
                for (int i = 0; i < Utils.gbcPalettesList.size(); i++) {
                    if (Utils.gbcPalettesList.get(i).getPaletteId().equals(Utils.gbcImagesList.get(globalImageIndex).getPaletteId())) {
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
                        if (!keepFrame) {
                            Utils.gbcImagesList.get(globalImageIndex).setPaletteId("bw");//Need to set this to the palette 0 to then change it with the frame
                        }
                        //Set the new palette to the gbcImage
                        Utils.gbcImagesList.get(globalImageIndex).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                        changedImage = paletteChanger(Utils.gbcImagesList.get(globalImageIndex).getPaletteId(), Utils.gbcImagesList.get(globalImageIndex).getImageBytes(), Utils.gbcImagesList.get(globalImageIndex));
                        Utils.imageBitmapCache.put(Utils.gbcImagesList.get(globalImageIndex).getHashCode(), changedImage);
                        if (keepFrame) {
                            try {
                                changedImage = frameChange(Utils.gbcImagesList.get(globalImageIndex), Utils.imageBitmapCache.get(Utils.gbcImagesList.get(globalImageIndex).getHashCode()), Utils.gbcImagesList.get(globalImageIndex).getFrameId(), keepFrame);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Utils.gbcImagesList.get(globalImageIndex).setPaletteId(Utils.gbcPalettesList.get(palettePosition).getPaletteId());
                        adapterPalette.setLastSelectedPosition(palettePosition);
                        adapterPalette.notifyDataSetChanged();
                        Utils.imageBitmapCache.put(Utils.gbcImagesList.get(globalImageIndex).getHashCode(), changedImage);
                        selectedImage[0] = changedImage;//Needed to save the image with the palette changed without leaving the Dialog
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
                            gridViewPalette.setVisibility(View.GONE);
                            gridViewFrames.setVisibility(View.VISIBLE);

                        } else {
                            showPalettes = true;
                            paletteFrameSelButton.setText(getString(R.string.btn_show_frames));
                            gridViewFrames.setVisibility(View.GONE);
                            gridViewPalette.setVisibility(View.VISIBLE);
                        }
                    }
                });
                printButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            MainActivity.printIndex = globalImageIndex;
//                            UsbSerialFragment.btnPrintImage.callOnClick();//This works.
                            connect();
                            usbIoManager.start();
                            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            View dialogView = getLayoutInflater().inflate(R.layout.print_dialog, null);
                            tvResponseBytes = dialogView.findViewById(R.id.tvResponseBytes);
                            builder.setView(dialogView);
//                            builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    // Acciones a realizar al hacer clic en el botón Aceptar
//                                }
//                            });
                            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Acciones a realizar al hacer clic en el botón Cancelar
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                            //PRINT IMAGE
                            PrintOverArduino printOverArduino = new PrintOverArduino();

                            printOverArduino.oneImage = true;
                            printOverArduino.banner = false;
//                printOverArduino.sendImage(port, tv);
                            try {
                                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                                if (availableDrivers.isEmpty()) {
                                    return;
                                }
                                UsbSerialDriver driver = availableDrivers.get(0);

                                printOverArduino.sendThreadDelay(connection, driver.getDevice(), tvResponseBytes, getContext());
                            } catch (Exception e) {
                                tv.append(e.toString());
                                Toast toast = Toast.makeText(getContext(), getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                                toast.show();
                            }

//                            Toast toast = Toast.makeText(getContext(), getString(R.string.toast_printing), Toast.LENGTH_LONG);
//                            toast.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap sharedBitmap = Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * MainActivity.exportSize, selectedImage[0].getHeight() * MainActivity.exportSize, false);
                        shareImage(sharedBitmap);
                    }
                });
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        crop = false;
                        LocalDateTime now = LocalDateTime.now();
                        String fileName = "gbcImage_";
                        if (MainActivity.exportPng) {
                            fileName += dtf.format(now) + ".png";
                        } else fileName += dtf.format(now) + ".txt";
                        saveImage(Utils.gbcImagesList.get(globalImageIndex), fileName);
                    }
                });

                // Configurar el diálogo para que ocupe el 80% de  la pantalla

                int screenWidth = displayMetrics.widthPixels;
                int desiredWidth = (int) (screenWidth * 0.8);
                Window window = dialog.getWindow();
                window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

                //To only dismiss it instead of cancelling when clicking outside it
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // Acción al presionar fuera del diálogo o el botón de retroceso
                        dialog.dismiss();
                    }
                });
                //Show Dialog
                dialog.show();
            }
        });
        //LongPress on an image to delete it
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = Utils.gbcImagesList.size() - (itemsPerPage - position);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getString(R.string.delete_dialog_gallery) + Utils.gbcImagesList.get(globalImageIndex).getName() + "?");
                builder.setMessage(getString(R.string.sure_dialog));

                // Crear un ImageView y establecer la imagen deseada
                ImageView imageView = new ImageView(getContext());
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(40, 10, 40, 10);
                Bitmap bitmap = Utils.imageBitmapCache.get(Utils.gbcImagesList.get(globalImageIndex).getHashCode());
                int scaler = 6;
                imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * scaler, bitmap.getHeight() * scaler, false));

                // Agregar el ImageView al diseño del diálogo
                builder.setView(imageView);

                builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteImageAsyncTask(Utils.gbcImagesList.get(globalImageIndex).getHashCode(), globalImageIndex).execute();
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //No action
                    }
                });
                // Mostrar el diálogo
                AlertDialog dialog = builder.create();

                int screenWidth = displayMetrics.widthPixels;
                int desiredWidth = (int) (screenWidth * 0.8);
                // Configurar el tamaño del diálogo

                if (bitmap.getHeight() > 144) {
                    int screenHeight = displayMetrics.heightPixels;
                    int desiredHeight = (int) (screenHeight * 0.8);
                    imageView.setMaxHeight((int) (desiredHeight * 0.5));
                }

                dialog.show();
                return true;//true so the normal onItemClick doesn't show
            }
        });


        if (Utils.gbcImagesList.size() > 0 && MainActivity.doneLoading) {//This because if not updateGridView will use sublists on the same list that the MainAcvitity is creating
            updateGridView(currentPage);
        } else {
            tv.setText(getString(R.string.loading));
        }
        tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        if (Utils.gbcImagesList.size() > 0) {
            updateGridView(currentPage);
            tv.setText(tv.getContext().getString(R.string.total_images) + GbcImage.numImages);

        } else {
            tv.setText(tv.getContext().getString(R.string.no_images));
        }
        tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        return view;
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
            Bitmap setToPalette = paletteChanger("bw", gbcImage.getImageBytes(), gbcImage);
            Bitmap croppedBitmapAux = Bitmap.createBitmap(setToPalette, 16, 16, 128, 112);//Need to put this to palette 0
            canvasAux.drawBitmap(croppedBitmapAux, 16, 16, null);
            if (!keepFrame) {
                framed = paletteChanger(gbcImage.getPaletteId(), Utils.encodeImage(framed), gbcImage);
                framed = framed.copy(Bitmap.Config.ARGB_8888, true);//To make it mutable
            }
            Canvas canvas = new Canvas(framed);
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 16, 16, 128, 112);
            canvas.drawBitmap(croppedBitmap, 16, 16, null);

            try {
                byte[] imageBytes = Utils.encodeImage(framedAux);//Use the framedAux because it doesn't a different palette to encode
                gbcImage.setImageBytes(imageBytes);
                for (int i = 0; i < Utils.gbcImagesList.size(); i++) {
                    if (Utils.gbcImagesList.get(i).getHashCode().equals(gbcImage.getHashCode())) {
                        Utils.gbcImagesList.get(i).setImageBytes(imageBytes);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new SaveImageAsyncTask(gbcImage).execute();
        return framed;
    }

    //Cambiar paleta
    public static Bitmap paletteChanger(String paletteId, byte[] imageBytes, GbcImage gbcImage) {
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt()), 160, imageBytes.length / 40);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(paletteId).getPaletteColorsInt(), imageBytes);
        new SaveImageAsyncTask(gbcImage).execute();
        return image;
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    private void showCustomDialog(Bitmap bitmap) {
        // Crear el diálogo personalizado
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.single_image_dialog);

        // Configurar el tamaño del diálogo
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // Obtener el ImageView y configurarlo como desplazable
        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false));

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        // Obtener el botón de cerrar y configurar su acción
        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Mostrar el diálogo personalizado
        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    //This one changes pixel by pixel of the bitmap, but works better with the frames
    public Bitmap paletteChanger2(int newPaletteIndex, Bitmap bitmap, int imageIndex) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int[] oldColors = Utils.hashPalettes.get(Utils.gbcImagesList.get(imageIndex).getPaletteId()).getPaletteColorsInt();
        int[] newColors = Utils.gbcPalettesList.get(newPaletteIndex).getPaletteColorsInt();

        Map<Integer, Integer> colorIndexMap = new HashMap<>();
        for (int i = 0; i < oldColors.length; i++) {
            colorIndexMap.put(oldColors[i], i);
        }

        for (int i = 0; i < pixels.length; i++) {
            int pixelColor = pixels[i];
            if (colorIndexMap.containsKey(pixelColor)) {
                int oldIndex = colorIndexMap.get(pixelColor);
                pixels[i] = newColors[oldIndex];
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(pixels, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        return newBitmap;
    }

    private void prevPage() {

        if (currentPage > 0) {
            currentPage--;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        }
    }

    private void nextPage() {

        if (currentPage < lastPage) {
            currentPage++;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        }
    }

//    //The shared image is stored in Pictures and compressed, then shared. This needs to change.
//    private void shareImage(Bitmap bitmap) {
//        if ((bitmap.getHeight() / MainActivity.exportSize) == 144 && (bitmap.getWidth() / MainActivity.exportSize) == 160 && crop) {
//            bitmap = Bitmap.createBitmap(bitmap, 16 * MainActivity.exportSize, 16 * MainActivity.exportSize, 128 * MainActivity.exportSize, 112 * MainActivity.exportSize);
//        }
//        String bitmapPath = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "image", "share image");
//        Uri bitmapUri = Uri.parse(bitmapPath);
//
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("image/png");
//        intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
//        startActivity(Intent.createChooser(intent, "Share"));
//    }

    private void shareImage(Bitmap bitmap) {
        if ((bitmap.getHeight() / MainActivity.exportSize) == 144 && (bitmap.getWidth() / MainActivity.exportSize) == 160 && crop) {
            bitmap = Bitmap.createBitmap(bitmap, 16 * MainActivity.exportSize, 16 * MainActivity.exportSize, 128 * MainActivity.exportSize, 112 * MainActivity.exportSize);
        }

        File file = new File(getActivity().getExternalCacheDir(), "shared_image.png");
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".fileprovider", file));
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

    private void saveImage(GbcImage gbcImage, String fileName) {
        Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());

        if (MainActivity.exportPng) {
            File file = new File(Utils.IMAGES_FOLDER, fileName);

            if (image.getHeight() == 144 && image.getWidth() == 160 && crop) {
                image = Bitmap.createBitmap(image, 16, 16, 128, 112);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);

                scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + MainActivity.exportSize, Toast.LENGTH_LONG);
                toast.show();
                // PNG is a lossless format, the compression factor (100) is ignored

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File file = new File(Utils.TXT_FOLDER, fileName);

            //Saving txt without cropping it
            try {
                //Need to change the palette to bw so the encodeImage method works
                image = paletteChanger("bw", gbcImage.getImageBytes(), Utils.gbcImagesList.get(0));
                StringBuilder txtBuilder = new StringBuilder();
                //Appending these commands so the export is compatible with
                // https://herrzatacke.github.io/gb-printer-web/#/import
                // and https://mofosyne.github.io/arduino-gameboy-printer-emulator/GameBoyPrinterDecoderJS/gameboy_printer_js_decoder.html
                txtBuilder.append("{\"command\":\"INIT\"}\n" +
                        "{\"command\":\"DATA\",\"compressed\":0,\"more\":1}\n");
                String txt = Utils.bytesToHex(Utils.encodeImage(image));
                txt = addSpacesAndNewLines(txt).toUpperCase();
                txtBuilder.append(txt);
                txtBuilder.append("\n{\"command\":\"DATA\",\"compressed\":0,\"more\":0}\n" +
                        "{\"command\":\"PRNT\",\"sheets\":1,\"margin_upper\":1,\"margin_lower\":3,\"pallet\":228,\"density\":64 }");
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(txtBuilder.toString());
                bufferedWriter.close();
                Utils.toast(getContext(), getString(R.string.toast_saved_txt));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String addSpacesAndNewLines(String input) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < input.length(); i++) {
            if (i > 0 && i % 32 == 0) {  // Agregar salto de línea cada 32 caracteres
                sb.append("\n");
                count = 0;
            } else if (count == 2) {  // Agregar espacio cada 2 caracteres
                sb.append(" ");
                count = 0;
            }
            sb.append(input.charAt(i));
            count++;

        }
        return sb.toString();
    }

    public void updateFromMain() {
//        if (Methods.gbcImagesList.size() > 0) {
//            updateGridView(currentPage);
//            tv.setText(tv.getContext().getString(R.string.total_images) + GbcImage.numImages);
//        } else {
//            tv.setText(tv.getContext().getString(R.string.no_images));
//        }
        tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        updateGridView(currentPage);
    }

    //Method to update the gallery gridview
    public static void updateGridView(int page) {
        //Bitmap list to store current page bitmaps
        imagesForPage = new ArrayList<>();
        itemsPerPage = MainActivity.imagesPage;
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        if (Utils.gbcImagesList.size() < itemsPerPage) {
            itemsPerPage = Utils.gbcImagesList.size();
        }
        lastPage = (Utils.gbcImagesList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage && (Utils.gbcImagesList.size() % itemsPerPage) != 0) {
            itemsPerPage = Utils.gbcImagesList.size() % itemsPerPage;
            startIndex = Utils.gbcImagesList.size() - itemsPerPage;
            endIndex = Utils.gbcImagesList.size();

        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Utils.gbcImagesList.size());
        }
        boolean doAsync = false;

        //The bitmaps come from the BitmapCache map, using the gbcimage hashcode
        for (GbcImage gbcImage : Utils.gbcImagesList.subList(startIndex, endIndex)) {
            if (!Utils.imageBitmapCache.containsKey(gbcImage.getHashCode())) {
                doAsync = true;
            }
        }
        if (doAsync) {
            loadingDialog.show();
            new UpdateGridViewAsyncTask().execute();
        } else {
            List<Bitmap> bitmapList = new ArrayList<>();
            for (GbcImage gbcImage : Utils.gbcImagesList.subList(startIndex, endIndex)) {
                bitmapList.add(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
            }
            customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, Utils.gbcImagesList.subList(startIndex, endIndex), bitmapList, false, false);
            gridView.setAdapter(customGridViewAdapterImage);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        BigInteger bigInt = new BigInteger(1, data);
        String hexString = bigInt.toString(16);
        // Asegurarse de que la cadena tenga una longitud par
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        // Formatear la cadena en bloques de dos caracteres
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
            for (GbcImage gbcImage : Utils.gbcImagesList.subList(newStartIndex, newEndIndex)) {
                //Add the hashcode to the list of current hashes
                String imageHash = gbcImage.getHashCode();
                currentPageHashes.add(imageHash);
                byte[] imageBytes;

                //Get the image bytes from the database for the current gbcImage
                imageBytes = imageDataDao.getDataByImageId(imageHash);
                //Set the image bytes to the object
                gbcImage.setImageBytes(imageBytes);
                //Create the image bitmap
                int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Utils.gbcPalettesList.get(0).getPaletteColorsInt()), 160, height);
                Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes);
                //Add the bitmap to the cache
                Utils.imageBitmapCache.put(imageHash, image);

                //Do a frameChange to create the Bitmap of the image
                try {
                    //Only do frameChange if the image is 144 height AND THE FRAME IS NOT EMPTY (AS SET WHEN READING WITH ARDUINO PRINTER EMULATOR)
                    if (image.getHeight() == 144 && !gbcImage.getFrameId().equals(""))
                        image = frameChange(Utils.gbcImagesList.get(newStartIndex + index), Utils.imageBitmapCache.get(Utils.gbcImagesList.get(newStartIndex + index).getHashCode()), Utils.gbcImagesList.get(newStartIndex + index).getFrameId(), Utils.gbcImagesList.get(newStartIndex + index).isLockFrame());
                    Utils.imageBitmapCache.put(gbcImage.getHashCode(), image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                index++;
            }
            gbcImagesForPage = Utils.gbcImagesList.subList(newStartIndex, newEndIndex);
            //Create a list of bitmaps to use in the adapter, getting the bitmaps from the cache map for each image in the current page
            List<Bitmap> bitmapList = new ArrayList<>();
            for (GbcImage gbcImage : gbcImagesForPage) {
                bitmapList.add(Utils.imageBitmapCache.get(gbcImage.getHashCode()));
            }
            customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, Utils.gbcImagesList.subList(newStartIndex, newEndIndex), bitmapList, false, false);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Notifies the adapter
            gridView.setAdapter(customGridViewAdapterImage);
            loadingDialog.hide();
        }
    }

    private class DeleteImageAsyncTask extends AsyncTask<Void, Void, Void> {
        private String hashCode;
        private int imageIndex;

        public DeleteImageAsyncTask(String hashCode, int imageIndex) {
            this.hashCode = hashCode;
            this.imageIndex = imageIndex;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            GbcImage gbcImage = Utils.gbcImagesList.get(imageIndex);
            ImageDao imageDao = MainActivity.db.imageDao();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            ImageData imageData = imageDataDao.getImageDataByid(hashCode);
            imageDao.delete(gbcImage);
            imageDataDao.delete(imageData);
            Utils.imageBitmapCache.remove(hashCode);
            Utils.gbcImagesList.remove(imageIndex);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Notifies the adapter
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

    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
        Context context;
        int layoutResourceId;
        List<GbcImage> data = new ArrayList<GbcImage>();
        private List<Bitmap> images;
        private boolean checkDuplicate;
        private boolean showName;

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate,
                                          boolean showName) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
            this.checkDuplicate = checkDuplicate;
            this.showName = showName;
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
                holder.txtTitle.setText(name);
            } else {
                holder.txtTitle.setVisibility(View.GONE);
            }

            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;
        }
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
package com.mraulio.gbcameramanager.ui.usbserial;

import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.gbxcart.GBxCartConstants.BAUDRATE;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.deleteFolderRecursive;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.magicIsReal;
import static com.mraulio.gbcameramanager.utils.Utils.saveTypeNames;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.ui.importFile.ImagesImportDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.importFile.HexToTileData;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.gbxcart.GBxCartCommands;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.importFile.ImportFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * By Mraulio
 */
public class UsbSerialFragment extends Fragment implements SerialInputOutputManager.Listener {
    public static File photoFolder;
    static File latestFile;
    boolean ape = false;
    static UsbDeviceConnection connection;
    static UsbSerialPort port = null;
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    String romName = "";
    int numImagesAdded;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    boolean isRomExtracted;
    public static LinearLayout layoutCb;
    public static CheckBox cbLastSeen;
    public static CheckBox cbDeleted;
    static GridView gridView;
    boolean gbxMode = true;
    public static List<File> fullRomFileList = new ArrayList<>();
    public static List<byte[]> fullRomFileBytes = new ArrayList<>();
    static TextView tv;

    TextView tvMode;
    public static Button btnSave, btnReadRomName, btnReadRam, btnFullRom, btnPrintBanner, btnAddImages, btnDelSav, btnDecode, btnDelete;
    RadioButton rbGbx, rbApe;
    public static RadioButton rbPrint;
    RadioGroup rbGroup;
    //    public static Switch swIsCartJpUsb;
    Spinner spSaveType;

    static Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk;
    static List<Bitmap> extractedImagesBitmaps = new ArrayList<>();
    static List<GbcImage> extractedImagesList = new ArrayList<>();
    static List<List<GbcImage>> listActiveImages = new ArrayList<>();
    static List<List<GbcImage>> listDeletedImages = new ArrayList<>();
    static List<List<Bitmap>> listDeletedBitmaps = new ArrayList<>();
    static List<List<Bitmap>> listDeletedBitmapsRedStroke = new ArrayList<>();
    public static List<GbcImage> finalListImages = new ArrayList<>();
    static List<List<Bitmap>> listActiveBitmaps = new ArrayList<>();
    public static List<Bitmap> finalListBitmaps = new ArrayList<>();
    static List<GbcImage> lastSeenImage = new ArrayList<>();
    static List<Bitmap> lastSeenBitmap = new ArrayList<>();
    static LinkedHashMap<GbcImage, Bitmap> importedImagesHashUsb = new LinkedHashMap<>();
    boolean isPhotoSave = false;
    List saveTypes = new ArrayList();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usb_serial, container, false);
        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.USB_SERIAL;

        gridView = view.findViewById(R.id.gridView);
        gridView.setNumColumns(2);//To see the images bigger in case there is corruption extracting with gbxcart
        tv = view.findViewById(R.id.textV);
        tvMode = view.findViewById(R.id.tvMode);
        rbGroup = view.findViewById(R.id.rbGroup);
        tv.setMovementMethod(new ScrollingMovementMethod());
        btnSave = view.findViewById(R.id.btnSave);
        cbLastSeen = view.findViewById(R.id.cbLastSeen);
        cbDeleted = view.findViewById(R.id.cbDeletedImages);
        layoutCb = view.findViewById(R.id.layout_cb);
        spSaveType = view.findViewById(R.id.sp_save_type_usb);

        saveTypes.add("International");
        saveTypes.add("Japanese");
        saveTypes.add("Hello Kitty");

        ArrayAdapter<String> adapterSaveType = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, saveTypes);
        adapterSaveType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSaveType.setAdapter(adapterSaveType);

        saveTypeIntJpHk = Utils.SAVE_TYPE_INT_JP_HK.INT;
        spSaveType.setSelection(0);
        spSaveType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveTypeIntJpHk = Utils.SAVE_TYPE_INT_JP_HK.valueOf(saveTypeNames.get(saveTypes.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        btnFullRom = view.findViewById(R.id.btnFullRom);
        btnReadRomName = view.findViewById(R.id.btnReadRom);
        btnReadRam = view.findViewById(R.id.btnReadRam);

        btnPrintBanner = view.findViewById(R.id.btnPrintBanner);
        btnAddImages = view.findViewById(R.id.btnAddImages);
        btnDelSav = view.findViewById(R.id.btnDelSav);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnDecode = view.findViewById(R.id.btnDecode);


        rbApe = view.findViewById(R.id.rbApe);
        rbGbx = view.findViewById(R.id.rbGbx);
        rbPrint = view.findViewById(R.id.rbPrint);

        List<Integer> sizesInteger = new ArrayList<>();
        sizesInteger.add(0);
        sizesInteger.add(20);
        sizesInteger.add(40);
        sizesInteger.add(80);
        sizesInteger.add(100);
        sizesInteger.add(120);
        sizesInteger.add(150);
        sizesInteger.add(151);
        sizesInteger.add(155);
        sizesInteger.add(160);
        sizesInteger.add(200);
        sizesInteger.add(300);

        List<String> sizes = new ArrayList<>();
        sizes.add("0");
        sizes.add("20");
        sizes.add("40");
        sizes.add("80");
        sizes.add("100");
        sizes.add("120");
        sizes.add("150");
        sizes.add("151");
        sizes.add("155");
        sizes.add("160");
        sizes.add("200");
        sizes.add("300");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, sizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        cbLastSeen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
            }
        });
        cbDeleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
            }
        });

        btnPrintBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //PRINT IMAGE
                PrintOverArduino printOverArduino = new PrintOverArduino();
                printOverArduino.banner = true;
                try {
                    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                    if (availableDrivers.isEmpty()) {
                        return;
                    }
                    UsbSerialDriver driver = availableDrivers.get(0);
                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tv, null);
                } catch (Exception e) {
                    tv.append(e.toString());
                    Toast toast = Toast.makeText(getContext(), getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        btnDelSav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                String title = isRomExtracted ? getString(R.string.delete_photo_folder_dialog) : getString(R.string.delete_sav_dialog);
                builder.setTitle(title);
                String fileToDelete = isRomExtracted ? "folder " + photoFolder.getName() : latestFile.getName();
                builder.setMessage(getString(R.string.sure_delete_sav) + fileToDelete + "?");
                builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Try to delete the file
                        if (isRomExtracted) {
                            if (deleteFolderRecursive(photoFolder)) {
                                toast(getContext(), "PHOTO FOLDER DELETED");
                                tv.setText(getString(R.string.deleted_sav) + photoFolder.getName());

                            } else {
                                toast(getContext(), "COULDNT DELETE PHOTO FOLDER");
                            }
                        } else {
                            if (latestFile.delete()) {
                                toast(getContext(), getString(R.string.toast_sav_deleted));
                                tv.setText(getString(R.string.deleted_sav) + latestFile.getName());

                            } else {
                                toast(getContext(), getString(R.string.toast_couldnt_delete_sav));
                            }
                        }
                        btnAddImages.setVisibility(View.GONE);
                        btnDelSav.setVisibility(View.GONE);
                        layoutCb.setVisibility(View.GONE);
                        listActiveBitmaps.clear();
                        listActiveImages.clear();
                        listDeletedBitmaps.clear();
                        listDeletedImages.clear();
                        listDeletedBitmapsRedStroke.clear();
                        showImages(cbLastSeen, cbDeleted);
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                //Show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        btnAddImages.setOnClickListener(v -> {
            try {
                numImagesAdded = 0;
                List<GbcImage> newGbcImages = new ArrayList<>();
                List<Bitmap> listNewBitmaps = new ArrayList<>();
                List<String> checkDuplicatedImport = new ArrayList<>();
                for (int i = 0; i < finalListImages.size(); i++) {
                    GbcImage gbcImage = finalListImages.get(i);
                    boolean alreadyAdded = false;
                    //If the image already exists (by the hash) it doesn't add it. Same if it's already added
                    for (GbcImage image : Utils.gbcImagesList) {
                        if (image.getHashCode().equals(gbcImage.getHashCode())) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    for (String hashDup : checkDuplicatedImport) {
                        if (hashDup.equals(gbcImage.getHashCode())) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        newGbcImages.add(gbcImage);
                        listNewBitmaps.add(finalListBitmaps.get(i));
                        checkDuplicatedImport.add(gbcImage.getHashCode());
                    }
                }

                if (newGbcImages.size() > 0) {
                    DocumentFile documentFile = null;
                    if (latestFile != null){
                        documentFile = DocumentFile.fromFile(latestFile);
                    }
                    ImagesImportDialog imagesImportDialog = new ImagesImportDialog(newGbcImages, listNewBitmaps, documentFile, getContext(), getActivity(), tv, numImagesAdded);
                    imagesImportDialog.createImagesImportDialog();
                } else {
                    tv.setText(getString(R.string.no_new_images));
                    toast(getContext(), getString(R.string.no_new_images));
                }
            } catch (Exception e) {
                tv.setText("Error en btn add\n" + e.toString());
                e.printStackTrace();
            }
        });

        btnSave.setOnClickListener(v -> saveTv());

        btnDelete.setOnClickListener(v -> tv.setText(""));

        btnDecode.setOnClickListener(v -> {
            //Method to decode the textview data
            extractedImagesBitmaps.clear();
            extractedImagesList.clear();
            try {
                extractHexImages(tv.getText().toString());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            CustomGridViewAdapterImage customGridViewAdapterImage = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, finalListBitmaps, true, true, false, null);
            gridView.setAdapter(customGridViewAdapterImage);
            tv.append(finalListImages.size() + " " + getString(R.string.images));
            btnAddImages.setVisibility(View.VISIBLE);
        });
        btnFullRom.setOnClickListener(v -> {
            isRomExtracted = true;
            btnDelSav.setText(getString(R.string.delete_folder));

            btnAddImages.setVisibility(View.GONE);
            extractedImagesList.clear();
            extractedImagesBitmaps.clear();
            fullRomFileList.clear();
            fullRomDump();
        });

        btnReadRomName.setOnClickListener(v -> {
            completeReadRomName();
        });

        btnReadRam.setOnClickListener(v -> {
            btnDelSav.setText(getString(R.string.btn_delete_sav));

            isRomExtracted = false;
            btnAddImages.setVisibility(View.GONE);
            extractedImagesList.clear();
            extractedImagesBitmaps.clear();
            completeRamDump();
        });

        rbApe.setOnClickListener(v -> arduinoPrinterMode());
        rbPrint.setOnClickListener(v -> printOverArduinoMode());
        rbGbx.setOnClickListener(v -> gbxMode());
        return view;
    }

    public void arduinoPrinterMode() {
        try {
            gbxMode = false;
            tvMode.setVisibility(View.VISIBLE);
            tvMode.setText(getString(R.string.arduino_mode));
            rbGroup.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnDecode.setVisibility(View.VISIBLE);
            btnPrintBanner.setVisibility(View.GONE);
            ape = true;
            connect();
            port.setDTR(true);
            port.setRTS(true);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbIoManager.start();
            tv.append(getString(R.string.tv_connected));
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_arduino) + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void printOverArduinoMode() {
        try {
            StaticValues.printingEnabled = true;
            gbxMode = false;
            ape = false;
            tvMode.setVisibility(View.VISIBLE);
            tvMode.setText(getString(R.string.print_mode));
            rbGroup.setVisibility(View.GONE);
            btnPrintBanner.setVisibility(View.VISIBLE);
            connect();
            port.setDTR(true);
            port.setRTS(true);
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbIoManager.start();
            tv.append(getString(R.string.tv_connected));
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_arduino) + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void gbxMode() {
        gbxMode = true;
        ape = false;
        tvMode.setText(getString(R.string.gbxcart_mode));
        tvMode.setVisibility(View.VISIBLE);
        rbGroup.setVisibility(View.GONE);
        btnReadRam.setVisibility(View.VISIBLE);
        spSaveType.setVisibility(View.VISIBLE);
        btnReadRomName.setVisibility(View.VISIBLE);
        try {
            connect();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error in CONNECT\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            usbIoManager.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            port.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_2, UsbSerialPort.PARITY_NONE);
            port.setDTR(true);
            port.setRTS(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        completeReadRomName();
    }

    public static boolean readSav(File file, byte[] saveBytes, int saveBank) {
        try {
            Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));

            //Check for Magic or FF bytes
            if (!magicIsReal(saveBytes)) {
                return false;
            }
            //Extract the images

            latestFile = file;
            extractedImagesList.clear();
            extractedImagesBitmaps.clear();
            if (file.length() / 1024 == 128) {
                importedImagesHashUsb = extractor.extractGbcImages(saveBytes, file.getName(), saveBank, saveTypeIntJpHk);
                for (HashMap.Entry<GbcImage, Bitmap> entry : importedImagesHashUsb.entrySet()) {
                    GbcImage gbcImage = entry.getKey();
                    Bitmap imageBitmap = entry.getValue();
                    ImageData imageData = new ImageData();
                    imageData.setImageId(gbcImage.getHashCode());
                    imageData.setData(gbcImage.getImageBytes());
                    extractedImagesBitmaps.add(imageBitmap);
                    extractedImagesList.add(gbcImage);
                }

                listActiveImages.add(new ArrayList<>(extractedImagesList.subList(0, extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1)));
                listActiveBitmaps.add(new ArrayList<>(extractedImagesBitmaps.subList(0, extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1)));
                lastSeenImage.add(extractedImagesList.get(extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1));
                lastSeenBitmap.add(extractedImagesBitmaps.get(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1));
                listDeletedImages.add(new ArrayList<>(extractedImagesList.subList(extractedImagesList.size() - StaticValues.deletedCount[saveBank], extractedImagesList.size())));

                listDeletedBitmaps.add(new ArrayList<>(extractedImagesBitmaps.subList(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank], extractedImagesBitmaps.size())));

                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(2);
                int startX = 160;
                int startY = 0;
                int endX = 0;
                int endY = 144;
                listDeletedBitmapsRedStroke.add(new ArrayList<>());
                for (Bitmap bitmap : listDeletedBitmaps.get(saveBank)) {
                    Bitmap copiedBitmap = bitmap.copy(bitmap.getConfig(), true);//Need to get a copy of the original bitmap, or else I'll paint on it
                    Canvas canvas = new Canvas(copiedBitmap);
                    canvas.drawLine(startX, startY, endX, endY, paint);
                    listDeletedBitmapsRedStroke.get(saveBank).add(copiedBitmap);
                }
            } else {
                tv.append(gridView.getContext().getString(R.string.no_good_dump));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void readRomSavs() {
        listActiveImages.clear();
        listActiveBitmaps.clear();
        listDeletedImages.clear();
        listDeletedBitmaps.clear();
        listDeletedBitmapsRedStroke.clear();
        finalListBitmaps.clear();
        finalListImages.clear();
        lastSeenImage.clear();
        lastSeenBitmap.clear();

        tv.append(tv.getContext().getString(R.string.sav_parts) + fullRomFileList.size());
        try {
            for (int i = 0; i < fullRomFileList.size(); i++) {
                readSav(fullRomFileList.get(i), fullRomFileBytes.get(i), i);
            }

            btnAddImages.setVisibility(View.VISIBLE);
            btnDelSav.setVisibility(View.VISIBLE);
            layoutCb.setVisibility(View.VISIBLE);
            showImages(cbLastSeen, cbDeleted);
        } catch (Exception e) {
            e.printStackTrace();
            toast(tv.getContext(), "Error: " + e.toString());
        }
    }

    private void completeReadRomName() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOff(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.setCartType(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOn(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                romName = GBxCartCommands.ReadRomName(port, getContext(), tv);
                tv.setText(getString(R.string.rom_name) + romName);
                //Changing the Spinner if the rom is International, Japanese or Hello Kitty
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOff(port, getContext());
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (romName.startsWith("PHOTO")) {
                    btnFullRom.setVisibility(View.VISIBLE);
                    isPhotoSave = true;
                } else {
                    btnFullRom.setVisibility(View.GONE);
                    isPhotoSave = false;
                }

                //Select the spinner value if HK or JP
                if (romName.trim().equals("POCKETCAMERA")){
                    spSaveType.setSelection(saveTypes.indexOf("Japanese"));
                } else if (romName.trim().equals("POCKETCAMERA_SN")){
                    spSaveType.setSelection(saveTypes.indexOf("Hello Kitty"));
                }
            }
        }, 200);
    }

    private void fullRomDump() {
        tv.setText("");
        tv.append(getString(R.string.dumping_rom_wait));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOff(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.setCartType(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOn(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new GBxCartCommands.ReadPHOTORomAsyncTask(port, getContext(), tv, fullRomFileList, fullRomFileBytes).execute();
            }
        }, 200);
    }

    private void completeRamDump() {

        Handler handlerRam = new Handler();
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOff(port, getContext());
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.setCartType(port, getContext());
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                GBxCartCommands.powerOn(port, getContext());
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                listActiveImages.clear();
                listActiveBitmaps.clear();
                listDeletedImages.clear();
                listDeletedBitmaps.clear();
                listDeletedBitmapsRedStroke.clear();

                new GBxCartCommands.ReadRamAsyncTask(port, getContext(), tv, latestFile).execute();
            }
        }, 200);
    }

    //Refactor
    public static void showImages(CheckBox showLastSeen, CheckBox showDeleted) {
        List<Bitmap> bitmapsAdapterList = new ArrayList<>();
        finalListImages.clear();
        finalListBitmaps.clear();
        if (!showLastSeen.isChecked() && !showDeleted.isChecked()) {
            for (List<GbcImage> gbcImageList : listActiveImages) {
                finalListImages.addAll(gbcImageList);
            }
            for (List<Bitmap> bitmapList : listActiveBitmaps) {
                finalListBitmaps.addAll(bitmapList);
            }

            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);

        } else if (showLastSeen.isChecked() && !showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.add(lastSeenImage.get(i));
            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                finalListBitmaps.add(lastSeenBitmap.get(i));
            }
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);

        } else if (!showLastSeen.isChecked() && showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.addAll(listDeletedImages.get(i));
            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                bitmapsAdapterList.addAll(listActiveBitmaps.get(i));
                finalListBitmaps.addAll(listDeletedBitmaps.get(i));
                bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke.get(i));

            }
        } else if (showLastSeen.isChecked() && showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.add(lastSeenImage.get(i));
                finalListImages.addAll(listDeletedImages.get(i));

            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                bitmapsAdapterList.addAll(listActiveBitmaps.get(i));

                finalListBitmaps.add(lastSeenBitmap.get(i));
                bitmapsAdapterList.add(lastSeenBitmap.get(i));

                finalListBitmaps.addAll(listDeletedBitmaps.get(i));
                bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke.get(i));
            }
        }
        gridView.setAdapter((new CustomGridViewAdapterImage(showLastSeen.getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, true, false, null)));
    }

    private void saveTv() {
        String texto = tv.getText().toString();
        LocalDateTime now = null;
        DateTimeFormatter dtf = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dtf = DateTimeFormatter.ofPattern(dateLocale+"_HH-mm-ss");
            now = LocalDateTime.now();
        }
        String fileName = "hex_";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileName += dtf.format(now) + ".txt";
        } else
            fileName += ".txt";
        File file = new File(Utils.ARDUINO_HEX_FOLDER, fileName);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }

        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_file) + e.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }
        //I create the new directory if it doesn't exists
        try (FileOutputStream outputStream = new FileOutputStream(file); OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);) {

            outputStreamWriter.write(texto);
            Toast toast = Toast.makeText(getContext(), getString(R.string.saved_to_file), Toast.LENGTH_SHORT);
            toast.show();

        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_file) + e.toString(), Toast.LENGTH_SHORT);
            toast.show();
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
//            port.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        } catch (Exception e) {
            tv.append(e.toString());
            Toast.makeText(getContext(), "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();
        }

        //USE IN ARDUINO MODE ONLY
        usbIoManager = new SerialInputOutputManager(port, this);
    }

    //For the Arduino Printer Emulator or Printing over arduino
    @Override
    public void onNewData(byte[] data) {
        if (ape) {
            String msg;
            msg = new String(data);

            getActivity().runOnUiThread(() -> {
                tv.append(msg);
            });
        } else {
            BigInteger bigInt = new BigInteger(1, data);
            String hexString = bigInt.toString(16);
            // Make sure the string has pair length
            if (hexString.length() % 2 != 0) {
                hexString = "0" + hexString;
            }

            // Format the string in blocks of 2 chars
            hexString = String.format("%0" + (hexString.length() + hexString.length() % 2) + "X", new BigInteger(hexString, 16));
            hexString = hexString.replaceAll("..", "$0 ");//To separate with spaces every hex byte
            String finalHexString = hexString;
            getActivity().runOnUiThread(() -> {
                tv.append(finalHexString);

            });
        }
    }

    //For the arduino printing function, try using the other
//    @Override
//    public void onNewData(byte[] data) {
//        //USE ON ARDUINO MODE ONLY
//        for (byte b : data) {
//            if (!String.format("%02X ", b).equals("00 ")) {
//                dataCreate.append(String.format("%02X ", b));
//            }
//        }
//        getActivity().runOnUiThread(() -> {
//            tv.append(dataCreate.toString());
//        });
//    }

    @Override
    public void onRunError(Exception e) {
    }

    public void extractHexImages(String fileContent) throws NoSuchAlgorithmException {
        finalListBitmaps.clear();
        finalListImages.clear();
        List<String> dataList = HexToTileData.separateData(fileContent);
        String data = "";
        int index = 1;
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = ImportFragment.convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(bytes);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            String hashHex = Utils.bytesToHex(hash);
            gbcImage.setHashCode(hashHex);
            ImageData imageData = new ImageData();
            imageData.setImageId(hashHex);
            imageData.setData(bytes);
            gbcImage.setName(index++ + "-" + " arduino");
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(160, height);
            Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), gbcImage.getImageBytes(), false);
            finalListBitmaps.add(image);
            finalListImages.add(gbcImage);
        }
    }

}
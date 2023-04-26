package com.mraulio.gbcameramanager.ui.usbserial;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.Connecter;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.PrintOverArduino;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.HexToTileData;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.gbxcart.PythonToJava;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.ui.importFile.ImportFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * By Mraulio
 */
public class UsbSerialFragment extends Fragment implements SerialInputOutputManager.Listener {
    List<Bitmap> extractedImagesBitmaps = new ArrayList<>();
    List<GbcImage> extractedImagesList = new ArrayList<>();

    File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File latestFile;
    static UsbDeviceConnection connection;
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    String romName = "";
    int numImagesAdded;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    StringBuilder dataCreate = new StringBuilder();

    GridView gridView;
    ImageView img;
    boolean gbxMode = true;
    public static List<File> fullRomFileList = new ArrayList<>();
    //    List<Bitmap> imageList = new ArrayList<>();
    public static boolean freeTv = false;
    DecimalFormat df = new DecimalFormat("#.00");
    static TextView tv, tvSleepTime;
    ;
    TextView tvMode;
    public static Button btnReadSav, boton, btnSave, btnShare, btnShowInfo, btnReadRom, btnPowerOff, btnSCT, btnPowerOn, btnReadRam, btnFullRom, btnPrintImage, btnPrintBanner, btnAddImages, btnDelSav, btnDecode, btnDelete;
    RadioButton rbGbx, rbApe;
    public static RadioButton rbPrint;
    RadioGroup rbGroup;

    static UsbSerialPort port = null;
    Spinner spSleepTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usb_serial, container, false);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

//        img = (ImageView) findViewById(R.id.img);
        gridView = (GridView) view.findViewById(R.id.gridView);
        gridView.setNumColumns(2);//To see the images bigger in case there is corruption extracting with gbxcart
        tv = (TextView) view.findViewById(R.id.textV);
        tvMode = (TextView) view.findViewById(R.id.tvMode);
        rbGroup = (RadioGroup) view.findViewById(R.id.rbGroup);
        tv.setMovementMethod(new ScrollingMovementMethod());
        btnSave = (Button) view.findViewById(R.id.btnSave);

        btnFullRom = (Button) view.findViewById(R.id.btnFullRom);
        btnReadRom = (Button) view.findViewById(R.id.btnReadRom);
        btnReadRam = (Button) view.findViewById(R.id.btnReadRam);
//        btnRomImages = (Button) view.findViewById(R.id.btnROMImages);
        btnPrintImage = (Button) view.findViewById(R.id.btnPrintImage);
        btnPrintBanner = (Button) view.findViewById(R.id.btnPrintBanner);
        btnAddImages = (Button) view.findViewById(R.id.btnAddImages);
        btnDelSav = (Button) view.findViewById(R.id.btnDelSav);
        btnDelete = (Button) view.findViewById(R.id.btnDelete);
        btnDecode = (Button) view.findViewById(R.id.btnDecode);


        rbApe = (RadioButton) view.findViewById(R.id.rbApe);
        rbGbx = (RadioButton) view.findViewById(R.id.rbGbx);
        rbPrint = (RadioButton) view.findViewById(R.id.rbPrint);
        tvSleepTime = (TextView) view.findViewById(R.id.tvSleepTime);
        spSleepTime = (Spinner) view.findViewById(R.id.spSleepTime);

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
        spSleepTime.setAdapter(adapter);

        spSleepTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // I set the export size on the Main activity int as the selected one
                PrintOverArduino.sleepTime = sizesInteger.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Acción que quieres hacer cuando no se selecciona ningún elemento en el Spinner
            }
        });

        btnPrintBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dataCreate.setLength(0);
                //PRINT IMAGE
                PrintOverArduino printOverArduino = new PrintOverArduino();
                printOverArduino.oneImage = false;
                printOverArduino.banner = true;
//                printOverArduino.sendImage(port, tv);
                try {
                    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
                    if (availableDrivers.isEmpty()) {
                        return;
                    }
                    UsbSerialDriver driver = availableDrivers.get(0);
                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tv, getContext());
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
                builder.setTitle(getString(R.string.delete_sav_dialog));
                builder.setMessage(getString(R.string.sure_delete_sav) + latestFile.getName() + "?");

                builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Try to delete the file
                        if (latestFile.delete()) {
                            Methods.toast(getContext(), getString(R.string.toast_sav_deleted));
                        } else {
                            System.out.println(getString(R.string.toast_couldnt_delete_sav));
                        }
                        btnAddImages.setVisibility(View.GONE);
                        btnDelSav.setVisibility(View.GONE);
                        tv.setText(getString(R.string.deleted_sav) + latestFile.getName());
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción a realizar cuando se presiona el botón "Cancelar"
                    }
                });
                //Show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    numImagesAdded = 0;
                    List<GbcImage> newGbcImages = new ArrayList<>();
                    List<ImageData> newImageDatas = new ArrayList<>();
                    for (int i = 0; i < extractedImagesList.size(); i++) {
                        GbcImage gbcImage = extractedImagesList.get(i);
                        boolean alreadyAdded = false;
                        //If the image already exists (by the hash) it doesn't add it. Same if it's already added
                        for (GbcImage image : Methods.gbcImagesList) {
                            if (image.getHashCode().equals(gbcImage.getHashCode())) {
                                alreadyAdded = true;
                                break;
                            }
                        }
                        if (!alreadyAdded) {
                            GbcImage.numImages++;
                            numImagesAdded++;
                            ImageData imageData = new ImageData();
                            imageData.setImageId(gbcImage.getHashCode());
                            imageData.setData(gbcImage.getImageBytes());
                            newImageDatas.add(imageData);
                            newGbcImages.add(gbcImage);
                            Methods.gbcImagesList.add(gbcImage);
                            Methods.imageBitmapCache.put(gbcImage.getHashCode(), extractedImagesBitmaps.get(i));
                        }
                    }
                    if (newGbcImages.size() > 0) {
                        new SaveImageAsyncTask(newGbcImages, newImageDatas).execute();
                    } else {
                        tv.setText(getString(R.string.no_new_images));
                        Methods.toast(getContext(), getString(R.string.no_new_images));
                    }
                } catch (Exception e) {
                    tv.setText("Error en btn add\n" + e.toString());
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTv();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("");
            }
        });

        btnDecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Method to decode the textview data
                extractedImagesBitmaps.clear();
                extractedImagesList.clear();
                try {
                    extractHexImages(tv.getText().toString());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                GalleryFragment.CustomGridViewAdapterImage customGridViewAdapterImage = new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, extractedImagesList, extractedImagesBitmaps, true, true);
                gridView.setAdapter(customGridViewAdapterImage);
                tv.append(extractedImagesList.size() + " images.");
                btnAddImages.setVisibility(View.VISIBLE);
            }
        });
        btnFullRom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAddImages.setVisibility(View.GONE);
                extractedImagesList.clear();
                extractedImagesBitmaps.clear();
                fullRomDump();
            }
        });
//        btnRomImages.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                readRomSavs();
//            }
//        });
        btnPrintImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataCreate.setLength(0);
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

                    printOverArduino.sendThreadDelay(connection, driver.getDevice(), tv, getContext());
                } catch (Exception e) {
                    tv.append(e.toString());
                    Toast toast = Toast.makeText(getContext(), getString(R.string.error_print_image) + e.toString(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
        btnReadRom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeReadRomName();
            }
        });

        btnReadRam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAddImages.setVisibility(View.GONE);
                extractedImagesList.clear();
                extractedImagesBitmaps.clear();
                completeRamDump();
            }
        });

        rbApe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arduinoPrinterMode();
            }
        });
        rbPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printOverArduinoMode();
            }
        });
        rbGbx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gbxMode();
            }
        });
        return view;
    }

    private class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcImage> gbcImagesList;
        List<ImageData> imageDataList;

        public SaveImageAsyncTask(List<GbcImage> gbcImagesList, List<ImageData> imageDataList) {
            this.gbcImagesList = gbcImagesList;
            this.imageDataList = imageDataList;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            ImageDao imageDao = MainActivity.db.imageDao();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            //Need to insert first the gbcImage because of the Foreign Key
            try {

                for (GbcImage gbcImage : gbcImagesList) {
                    imageDao.insert(gbcImage);
                }
            } catch (Exception e) {
                tv.setText("Error en gbcImage\n" + e.toString());

            }
            try {
                for (ImageData imageData : imageDataList) {
                    imageDataDao.insert(imageData);
                }
            } catch (Exception e) {
                tv.setText("Error en ImageData\n" + e.toString());

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tv.append("\n" + numImagesAdded + getString(R.string.done_adding_images));
            Methods.toast(getContext(), getString(R.string.images_added) + numImagesAdded);
        }
    }

    public static void printOnGallery() throws IOException {

        //PRINT IMAGE
        try {
            PrintOverArduino printOverArduino = new PrintOverArduino();
            Connecter.connect(tv.getContext(), tv);
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                return;
            }
            UsbSerialDriver driver = availableDrivers.get(0);

            printOverArduino.sendThread(connection, driver.getDevice(), tv);
        } catch (Exception e) {
            tv.append(e.toString());

        }
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
            spSleepTime.setVisibility(View.GONE);
            tvSleepTime.setVisibility(View.GONE);
            btnPrintBanner.setVisibility(View.GONE);

            connect();
            usbIoManager.start();
            tv.append(getString(R.string.tv_connected));
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_arduino) + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void printOverArduinoMode() {
        try {
            gbxMode = false;
            tvMode.setVisibility(View.VISIBLE);
            tvMode.setText(getString(R.string.print_mode));
            rbGroup.setVisibility(View.GONE);
            btnPrintBanner.setVisibility(View.VISIBLE);
            btnPrintImage.setVisibility(View.VISIBLE);
            spSleepTime.setVisibility(View.VISIBLE);
            tvSleepTime.setVisibility(View.VISIBLE);
            connect();
            usbIoManager.start();
//            usbIoManager.stop();
            tv.append(getString(R.string.tv_connected));
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), getString(R.string.error_arduino) + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void gbxMode() {
        gbxMode = true;
        tvMode.setText(getString(R.string.gbxcart_mode));
        tvMode.setVisibility(View.VISIBLE);
        rbGroup.setVisibility(View.GONE);
        btnReadRam.setVisibility(View.VISIBLE);
        btnReadRom.setVisibility(View.VISIBLE);

        try {
            connect();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error in CONNECT\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            usbIoManager.stop();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error in usbio STOP\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            port.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error in gbx\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        completeReadRomName();
    }

    private void readSav(File file) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        LocalDateTime now = LocalDateTime.now();
        //I get the last file from the directory, which I just dumped
        try {
            if (file.length() / 1024 == 128) {
                List<byte[]> listExtractedImageBytes = new ArrayList<>();

                listExtractedImageBytes = extractor.extractBytes(file);
                tv.append(getString(R.string.images_list) + listExtractedImageBytes.size());
                int nameIndex = 1;

                for (byte[] imageBytes : listExtractedImageBytes) {
                    GbcImage gbcImage = new GbcImage();
                    gbcImage.setName(nameIndex++ + "-" + file.getName());
                    gbcImage.setFrameIndex(0);
                    gbcImage.setPaletteIndex(0);
                    byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                    String hashHex = Methods.bytesToHex(hash);
                    gbcImage.setHashCode(hashHex);
                    ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 128, 112);
                    Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), imageBytes);
                    if (image.getHeight() == 112 && image.getWidth() == 128) {
                        //I need to use copy because if not it's inmutable bitmap
                        Bitmap framed = Methods.framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(framed);
                        canvas.drawBitmap(image, 16, 16, null);
                        image = framed;
                        imageBytes = Methods.encodeImage(image);
                    }
                    gbcImage.setImageBytes(imageBytes);
                    extractedImagesBitmaps.add(image);
                    extractedImagesList.add(gbcImage);
                }
                GalleryFragment.CustomGridViewAdapterImage customGridViewAdapterImage = new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, extractedImagesList, extractedImagesBitmaps, true, true);
                gridView.setAdapter(customGridViewAdapterImage);
            } else {
                tv.append(getString(R.string.no_good_dump));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        btnAddImages.setVisibility(View.VISIBLE);
        btnDelSav.setVisibility(View.VISIBLE);
    }

    private void readRomSavs() {
        extractedImagesBitmaps.clear();
        tv.append(getString(R.string.sav_parts) + fullRomFileList.size());
        try {
            for (File file : fullRomFileList) {
                readSav(file);
            }
            GalleryFragment.CustomGridViewAdapterImage customGridViewAdapterImage = new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, extractedImagesList, extractedImagesBitmaps, true, true);
            gridView.setAdapter(customGridViewAdapterImage);
//            } else {
//                tv.append("\nNOT A GOOD SAVE DUMP.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            Methods.toast(getContext(), "Error: " + e.toString());
        }
        btnAddImages.setVisibility(View.VISIBLE);
    }


    private void completeReadRomName() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOff(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.setCartType(port, getContext(), tv);
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOn(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                romName = PythonToJava.ReadRom(port, getContext(), tv);
                tv.setText(getString(R.string.rom_name) + romName);
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOff(port, getContext());
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (romName.startsWith("PHOTO")) {
                    btnFullRom.setVisibility(View.VISIBLE);
                } else btnFullRom.setVisibility(View.GONE);
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
                PythonToJava.powerOff(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.setCartType(port, getContext(), tv);
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOn(port, getContext());
            }
        }, 100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fullRomFileList = PythonToJava.ReadFullRom(port, getContext(), tv);
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOff(port, getContext());
            }
        }, 200);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                readRomSavs();
            }
        }, 200);
//        btnRomImages.setVisibility(View.VISIBLE);
    }

    private void completeRamDump() {
        tv.setText("");
        tv.append(getString(R.string.dumping_ram_wait));
        Handler handlerRam = new Handler();
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOff(port, getContext());
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.setCartType(port, getContext(), tv);
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOn(port, getContext());
            }
        }, 100);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.ReadRam(port, getContext(), tv);
            }
        }, 200);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                PythonToJava.powerOff(port, getContext());
            }
        }, 200);
        handlerRam.postDelayed(new Runnable() {
            @Override
            public void run() {
                //To get the extracted file, as the latest one in the directory
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                latestFile = null;
                File folder = new File(directory + "/GBxCamera Dumps");
                //To get the last created file
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return Long.compare(f2.lastModified(), f1.lastModified());
                        }
                    });
                    latestFile = files[0];
                    tv.append(getString(R.string.last_sav_name) + latestFile.getName() + ".\n" +
                            getString(R.string.size) + latestFile.length() / 1024 + "KB");
                }
                readSav(latestFile);
            }
        }, 200);

    }

//    private void showDeviceInfo() {
//        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//        while(deviceIterator.hasNext()){
//            UsbDevice device = deviceIterator.next();
//            String deviceInfo = "Device ID: " + device.getDeviceId() + "\n" +
//                    "Device Name: " + device.getDeviceName() + "\n" +
//                    "Vendor ID: " + device.getVendorId() + "\n" +
//                    "Product ID: " + device.getProductId() + "\n" +
//                    "Class: " + device.getDeviceClass() + "\n" +
//                    "Subclass: " + device.getDeviceSubclass() + "\n" +
//                    "Protocol: " + device.getDeviceProtocol() + "\n";
//            tv.append(deviceInfo + "\n");
//        }
//    }
//    private void sendTv(){
//        String textoACompartir = tv.getText().toString();
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_SEND);
//        intent.putExtra(Intent.EXTRA_TEXT, textoACompartir);
//        intent.setType("text/plain");
//        startActivity(Intent.createChooser(intent, "Compartir texto con"));
//    }

    private void saveTv() {
        String texto = tv.getText().toString();
        LocalDateTime now = null;
        DateTimeFormatter dtf = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            now = LocalDateTime.now();
        }
        String fileName = "hex_";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fileName += dtf.format(now) + ".txt";
        } else
            fileName += ".txt";
        File file = new File(directory + "/SaveTestHex", fileName);
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
            port.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//            tv.append("Puerto abierto y parametros puestos");

        } catch (Exception e) {
            System.out.println(e.toString());
            tv.append(e.toString());
            Toast.makeText(getContext(), "Error in connect." + e.toString(), Toast.LENGTH_SHORT).show();

        }

        //USE IN ARDUINO MODE ONLY
        usbIoManager = new SerialInputOutputManager(port, this);
//        usbIoManager.start();
//        tv.append("Conectado");
    }

    //For the Arduino Printer Emulator
    @Override
    public void onNewData(byte[] data) {
//        ArrayDeque<byte[]> datas = new ArrayDeque<>();
//        datas.add(data);
//        SpannableStringBuilder spn = new SpannableStringBuilder();
        String msg;

        msg = new String(data);
//            spn.append(msg);

        getActivity().runOnUiThread(() -> {
            tv.append(msg);
        });

    }

    //For de arduino printing function, try using the other
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
        List<String> dataList = HexToTileData.separateData(fileContent);
        String data = "";
        int index = 1;
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = ImportFragment.convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(bytes);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            String hashHex = Methods.bytesToHex(hash);
            gbcImage.setHashCode(hashHex);
            ImageData imageData = new ImageData();
            imageData.setImageId(hashHex);
            imageData.setData(bytes);
//            importedImageDatas.add(imageData);
            gbcImage.setName(index++ + "-" + " arduino");
            gbcImage.setFrameIndex(9999);
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), gbcImage.getImageBytes());
            extractedImagesBitmaps.add(image);
            extractedImagesList.add(gbcImage);
        }
    }

}
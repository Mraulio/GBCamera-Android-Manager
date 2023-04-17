package com.mraulio.gbcameramanager.ui.usbserial;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
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
import android.widget.BaseAdapter;
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
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.gbxcart.PythonToJava;
import com.mraulio.gbcameramanager.model.GbcImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    static UsbDeviceConnection connection;
    static UsbManager manager = MainActivity.manager;
    SerialInputOutputManager usbIoManager;
    String romName = "";

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
    public static Button btnReadSav, boton, btnSave, btnShare, btnShowInfo, btnReadRom, btnPowerOff, btnSCT, btnPowerOn, btnReadRam, btnFullRom,  btnPrintImage, btnPrintBanner, btnAddImages;
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
                    Toast toast = Toast.makeText(getContext(), "Error en PRINT IMAGE\n" + e.toString(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GbcImage.numImages += extractedImagesList.size();
                Methods.completeBitmapList.addAll(extractedImagesBitmaps);
                Methods.gbcImagesList.addAll(extractedImagesList);
                Methods.toast(getContext(), "Images added.");
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTv();
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
                    Toast toast = Toast.makeText(getContext(), "Error en PRINT IMAGE\n" + e.toString(), Toast.LENGTH_LONG);
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
//        btnReadSav.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                readSav();
//            }
//        });

        rbApe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                connect();
                arduinoPrinterMode();
            }
        });
        rbPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                connect();
                printOverArduinoMode();
            }
        });
        rbGbx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                connect();
                gbxMode();
            }
        });
        return view;
    }

    public static void printOnGallery() throws IOException {

        //PRINT IMAGE
//                printOverArduino.sendImage(port, tv);
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
//            Toast toast = Toast.makeText(getContext(), "Error en PRINT IMAGE\n" + e.toString(), Toast.LENGTH_LONG);
//            toast.show();
        }
    }

    public void arduinoPrinterMode() {
        try {
            gbxMode = false;
            tvMode.setVisibility(View.VISIBLE);
            tvMode.setText("Arduino Printer Emulator MODE");
            rbGroup.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            spSleepTime.setVisibility(View.GONE);
            tvSleepTime.setVisibility(View.GONE);
            btnPrintBanner.setVisibility(View.GONE);

            connect();
            usbIoManager.start();
            tv.append("//Connected\n");
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error en arduino\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void printOverArduinoMode() {
        try {
            gbxMode = false;
            tvMode.setVisibility(View.VISIBLE);
            tvMode.setText("PRINT");
            rbGroup.setVisibility(View.GONE);
            btnPrintBanner.setVisibility(View.VISIBLE);
            btnPrintImage.setVisibility(View.VISIBLE);
            spSleepTime.setVisibility(View.VISIBLE);
            tvSleepTime.setVisibility(View.VISIBLE);
            connect();
            usbIoManager.start();
//            usbIoManager.stop();
            tv.append("//Connected\n");
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error en arduino\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void gbxMode() {
        gbxMode = true;
        tvMode.setText("GBxCart Reader MODE");
        tvMode.setVisibility(View.VISIBLE);
        rbGroup.setVisibility(View.GONE);
        btnReadRam.setVisibility(View.VISIBLE);
        btnReadRom.setVisibility(View.VISIBLE);

        try {
            connect();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error en CONNECT\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            usbIoManager.stop();
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error en usbio STOP\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        try {
            port.setParameters(1000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error en gbx\n" + e.toString(), Toast.LENGTH_LONG);
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
                tv.append("\nThe image list has: " + listExtractedImageBytes.size() + " images.");
                int nameIndex = 1;

                for (byte[] imageBytes : listExtractedImageBytes) {
                    GbcImage gbcImage = new GbcImage();
                    gbcImage.setName(nameIndex++ + "-" + file.getName());
                    gbcImage.setFrameIndex(0);
                    gbcImage.setPaletteIndex(0);
                    ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 128, 112);
                    Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), imageBytes);
                    if (image.getHeight() == 112 && image.getWidth() == 128) {
//                        System.out.println("***********ENTERING ADDING FRAME*************");
                        //I need to use copy because if not it's inmutable bitmap
                        Bitmap framed = Methods.framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(framed);
                        canvas.drawBitmap(image, 16, 16, null);
                        image = framed;
                        imageBytes = Methods.encodeImage(image);
//                        System.out.println("***********" + image.getHeight() + " " + image.getWidth() + "*************");
                    }
                    gbcImage.setImageBytes(imageBytes);
                    extractedImagesBitmaps.add(image);
                    extractedImagesList.add(gbcImage);
                }
                ImageAdapter imageAdapter = new ImageAdapter(getContext(), extractedImagesBitmaps, extractedImagesBitmaps.size());
                gridView.setAdapter(imageAdapter);
            } else {
                tv.append("\nNOT A GOOD SAVE DUMP.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        btnAddImages.setVisibility(View.VISIBLE);
    }

    private void readRomSavs() {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        extractedImagesBitmaps.clear();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
        }
        tv.append("\nThere are: " + fullRomFileList.size() + " sav parts.");
        try {
            for (File file : fullRomFileList) {
                readSav(file);
            }
            ImageAdapter imageAdapter = new ImageAdapter(getContext(), extractedImagesBitmaps, extractedImagesBitmaps.size());
            gridView.setAdapter(imageAdapter);
//            } else {
//                tv.append("\nNOT A GOOD SAVE DUMP.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            Methods.toast(getContext(), "Error: " + e.toString());
        }
        btnAddImages.setVisibility(View.VISIBLE);
    }


    public class ImageAdapter extends BaseAdapter {
        private List<Bitmap> images;
        private Context context;
        public int itemsPage;

        public ImageAdapter(Context context, List<Bitmap> images, int itemsPage) {
            this.context = context;
            this.images = images;
            this.itemsPage = itemsPage;
        }

        public int getCount() {
            return images.size();
        }
//        public int getCount() {
//            return itemsPerPage;
//        }

        public Object getItem(int position) {
            return images.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // Si la vista aún no ha sido creada, inflar el layout del elemento de la lista
                convertView = LayoutInflater.from(context).inflate(R.layout.row_items_usb, parent, false);
                // Crear una nueva vista de imagen
                imageView = convertView.findViewById(R.id.imageView);
                // Establecer la vista de imagen como la vista del elemento de la lista
                convertView.setTag(imageView);
            } else {
                // Si la vista ya existe, obtener la vista de imagen del tag
                imageView = (ImageView) convertView.getTag();
            }
            //Obtener la imagen de la lista
            Bitmap image = images.get(position);

            imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
            return convertView;
        }
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
                tv.setText("\nThe ROM name is: " + romName);
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
        tv.append("\nDumping 1MB ROM, please wait about 40 seconds...\n...");
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
        tv.append("\nDumping RAM, please wait...\n...");
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
                File latestFile = null;
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
                    tv.append("\nThe name of the last SAV file is: " + latestFile.getName() + ".\n" +
                            "Size: " + latestFile.length() / 1024 + "KB");
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
            dtf = DateTimeFormatter.ofPattern("HH-mm-ss_dd-MM-yyyy");
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
            Toast toast = Toast.makeText(getContext(), "Error making file: " + e.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }
        //I create the new directory if it doesn't exists
        try (FileOutputStream outputStream = new FileOutputStream(file); OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);) {


            outputStreamWriter.write(texto);
            Toast toast = Toast.makeText(getContext(), "Saved to file", Toast.LENGTH_SHORT);
            toast.show();

        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "Error making file: " + e.toString(), Toast.LENGTH_SHORT);
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
            Toast.makeText(getContext(), "Error en el connect." + e.toString(), Toast.LENGTH_SHORT).show();

        }

        //USE IN ARDUINO MODE ONLY
        usbIoManager = new SerialInputOutputManager(port, this);
//        usbIoManager.start();
//        tv.append("Conectado");
    }

    @Override
    public void onNewData(byte[] data) {
        //USE ON ARDUINO MODE ONLY
        for (byte b : data) {
            if (!String.format("%02X ", b).equals("00 ")) {
                dataCreate.append(String.format("%02X ", b));
            }
        }
        getActivity().runOnUiThread(() -> {
            tv.append(dataCreate.toString());
        });
    }

    //    @Override
//    public void onNewData(byte[] data) {
//        //USE ON ARDUINO MODE ONLY
//        getActivity().runOnUiThread(() -> {
////            tv.append(new String(data));
//            for (byte b : data) {
////                tv.append(String.format("%02X ", b));
//                PrintOverArduino.count++;
//            }
////            tv.setText("Sending image..." + df.format((PrintOverArduino.count / PrintOverArduino.percentage) * 100) + "%.");
//            if (freeTv)
//                tv.append("PRINTING");
//        });
//    }
    @Override
    public void onRunError(Exception e) {

    }

}
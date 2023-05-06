package com.mraulio.gbcameramanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.databinding.ActivityMainBinding;
import com.mraulio.gbcameramanager.db.AppDatabase;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.ui.importFile.ImportFragment;
import com.mraulio.gbcameramanager.ui.palettes.PalettesFragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.spec.ECField;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    public static int printIndex = 0;//If there are no images there will be a crash when trying to print
    private AppBarConfiguration mAppBarConfiguration;
    boolean anyImage = false;
    private ActivityMainBinding binding;
    public static boolean pressBack = true;
    public static boolean doneLoading = false;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static String printedResponseBytes;
    public static boolean exportPng = true;
    public static UsbManager manager;
    public static int deletedCount = 0;
    public static AppDatabase db;
    public static boolean printingEnabled = false;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbDevice device;
    private UsbManager usbManager;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                        }
                    } else {
                        Log.d("TAG", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    SerialInputOutputManager usbIoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "Database").build();
        System.out.println("Done loading: " + doneLoading);
        System.out.println("Any image: " + anyImage);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);


// Obtener información del Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && type != null && type.equals("application/octet-stream") && uri != null && uri.toString().endsWith(".sav")) {
            // Si el Intent contiene la acción ACTION_VIEW y la categoría CATEGORY_DEFAULT y
            // el tipo es "application/octet-stream" y el URI del Intent termina en ".sav", realizar la acción deseada
            // Por ejemplo, puedes abrir el archivo en tu aplicación:
            Methods.toast(this, "Opened from file");
        }
        if (!doneLoading) {
            new ReadDataAsyncTask().execute();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_gallery, R.id.nav_settings, R.id.nav_palettes)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        /**
         * I ask for storage permissions
         */
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
    }

    private class ReadDataAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            PaletteDao paletteDao = db.paletteDao();
            FrameDao frameDao = db.frameDao();
            ImageDao imageDao = db.imageDao();

            List<GbcPalette> palettes = paletteDao.getAll();
            List<GbcFrame> frames = frameDao.getAll();
            List<GbcImage> imagesFromDao = imageDao.getAll();

            if (palettes.size() > 0) {
                Methods.gbcPalettesList.addAll(palettes);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int resourceId = R.raw.palettes;
                try {
                    InputStream inputStream = getResources().openRawResource(resourceId);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    // Crear un ByteArrayOutputStream para copiar el contenido del archivo
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        stringBuilder.append(line).append('\n');
                        line = bufferedReader.readLine();
                    }
                    bufferedReader.close();
                    inputStream.close();
                } catch (Exception e) {
                    System.out.println(e.toString());

                }
                String fileContent = stringBuilder.toString();
                List<GbcPalette> receivedList = (List<GbcPalette>) JsonReader.jsonCheck(fileContent);

                Methods.gbcPalettesList.addAll(receivedList);

                for (GbcPalette gbcPalette : Methods.gbcPalettesList) {
                    paletteDao.insert(gbcPalette);
                }
            }

            if (frames.size() > 0) {
                Methods.framesList.addAll(frames);
            } else {
                StartCreation.addFrames(getBaseContext());
                for (GbcFrame gbcFrame : Methods.framesList) {
                    frameDao.insert(gbcFrame);
                }
            }
            //Now that I have palettes, I can add images:
            if (imagesFromDao.size() > 0) {
                try {
                    System.out.println("Length" + imagesFromDao.get(0).getImageBytes().length);

                } catch (Exception e) {
                    System.out.println("Length en gbcimage es null");
                }
                anyImage = true;
                //I need to add them to the gbcImagesList(GbcImage) and completeBitmapList(Bitmap)
                Methods.gbcImagesList.addAll(imagesFromDao);
                GbcImage.numImages += Methods.gbcImagesList.size();
                System.out.println("Added");
//                int index = 0;
//                for (GbcImage gbcImage : Methods.gbcImagesList) {
//                    byte[] bytesFromNewDao = imageDataDao.getDataByImageId(gbcImage.getHashCode());
//                    System.out.println("hashcode "+gbcImage.getHashCode());
//                    System.out.println("Bytes new dao"+bytesFromNewDao);
//                    System.out.println(index);
////                    gbcImage.setImageBytes(imageDataDao.getDataByImageId(gbcImage.getHashCode()));
//                    GbcImage.numImages++;
//
//                    index++;
//                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Notifica al Adapter que los datos han cambiado
            GalleryFragment gf = new GalleryFragment();
            doneLoading = true;
            if (anyImage) {
                gf.updateFromMain();
            } else {
                GalleryFragment.tv.setText("No images in the gallery.\nGo to Import tab.");
            }
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//
//
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            System.out.println("PERMISION GRANTED*********************");            //resume tasks needing this permission
            Toast toast = Toast.makeText(this, "Granted permissions.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    //This makes the app not get closed when pressing back
//    @Override
//    public void onBackPressed() {
//        if (pressBack) {
//            super.onBackPressed();
//            //additional code
//            moveTaskToBack(true);
//        }
//    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
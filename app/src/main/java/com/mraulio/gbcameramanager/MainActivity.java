package com.mraulio.gbcameramanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

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
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static int printIndex = 0;
    private AppBarConfiguration mAppBarConfiguration;
    boolean anyImage = false;
    private ActivityMainBinding binding;
    public static boolean pressBack = true;
    public static boolean doneLoading = false;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static UsbManager manager;
    public static AppDatabase db;
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

//        Methods.extractHexImages();
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
            System.out.println("Entering readASync");
            PaletteDao paletteDao = db.paletteDao();
            FrameDao frameDao = db.frameDao();
            ImageDao imageDao = db.imageDao();

            List<GbcPalette> palettes = paletteDao.getAll();
            List<GbcFrame> frames = frameDao.getAll();
            List<GbcImage> imagesFromDao = imageDao.getAll();
            System.out.println(palettes.size() + "/////PALETTES SIZE");
            System.out.println(frames.size() + "/////FRAMES SIZE");
            System.out.println(imagesFromDao.size() + "/////IMAGES SIZE");

            if (palettes.size() > 0) {
                Methods.gbcPalettesList.addAll(palettes);
            } else {
                StartCreation.addPalettes();
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
                anyImage = true;
                //I need to add them to the gbcImagesList(GbcImage) and completeBitmapList(Bitmap)
                Methods.gbcImagesList.addAll(imagesFromDao);
                int index = 0;
                for (GbcImage gbcImage : Methods.gbcImagesList) {
                    int height = (gbcImage.getImageBytes().length + 1) / 40;//To get the real height of the image
                    ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt()), 160, height);
                    GbcImage.numImages++;
                    Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), gbcImage.getImageBytes());

//                    Methods.completeBitmapList.add(image);
                    Methods.imageBitmapCache.put(gbcImage.getHashCode(),image);

                    if (gbcImage.isLockFrame()) {
                        System.out.println("Entering lockFrame");
                        try {
                            image = GalleryFragment.frameChange(index, Methods.gbcImagesList.get(index).getFrameIndex(), true);
//                            Methods.completeBitmapList.set(index, image);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Methods.imageBitmapCache.put(gbcImage.getHashCode(),image);
                    Methods.imageBytesCache.put(gbcImage.getHashCode(),gbcImage.getImageBytes());
                    index++;
                }
//                for (GbcImage gbcImage : Methods.gbcImagesList) {
//                    imageDao.insert(gbcImage);
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
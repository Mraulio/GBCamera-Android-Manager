package com.mraulio.gbcameramanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.mraulio.gbcameramanager.methods.Methods;
import com.mraulio.gbcameramanager.methods.StartCreation;
import com.mraulio.gbcameramanager.databinding.ActivityMainBinding;
import com.mraulio.gbcameramanager.db.AppDatabase;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.ui.importFile.JsonReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    public static int printIndex = 0;//If there are no images there will be a crash when trying to print
    private AppBarConfiguration mAppBarConfiguration;
    boolean anyImage = false;
    private ActivityMainBinding binding;
    public static boolean pressBack = true;
    public static boolean doneLoading = false;

    public static SharedPreferences sharedPreferences;
    //Store in the shared preferences
    public static boolean exportPng = true;
    public static boolean printingEnabled = false;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static String languageCode = "en";

    private boolean openedSav = false;
    public static UsbManager manager;
    public static int deletedCount = 0;
    public static AppDatabase db;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Keep only the Light Theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        sharedPreferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        exportSize = sharedPreferences.getInt("export_size", 4);
        imagesPage = sharedPreferences.getInt("images_per_page", 12);
        exportPng = sharedPreferences.getBoolean("export_as_png", true);
        languageCode = sharedPreferences.getString("language", "en");
        printingEnabled = sharedPreferences.getBoolean("print_enabled", false);

        // Change language config
        if (!languageCode.equals("en")) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Resources resources = getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "Database").build();
        System.out.println("Done loading: " + doneLoading);
        System.out.println("Any image: " + anyImage);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        // Obtain Intent information
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && type != null && type.equals("application/octet-stream") && uri != null && uri.toString().endsWith(".sav")) {
            // Si el Intent contiene la acción ACTION_VIEW y la categoría CATEGORY_DEFAULT y
            // el tipo es "application/octet-stream" y el URI del Intent termina en ".sav", realizar la acción deseada
            Methods.toast(this, "Opened from file");
            openedSav = true;
        }
        if (!doneLoading) {
            new ReadDataAsyncTask().execute();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        if (openedSav) navigationView.setCheckedItem(R.id.nav_import);

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
                for (GbcPalette gbcPalette : palettes) {
                    Methods.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                Methods.gbcPalettesList.addAll(palettes);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                int resourcePalettes = R.raw.palettes;
                try {
                    InputStream inputStream = getResources().openRawResource(resourcePalettes);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    String line = bufferedReader.readLine();
                    while (line != null) {
                        stringBuilder.append(line).append('\n');
                        line = bufferedReader.readLine();
                    }
                    bufferedReader.close();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String fileContent = stringBuilder.toString();
                List<GbcPalette> receivedList = (List<GbcPalette>) JsonReader.jsonCheck(fileContent);
                Methods.gbcPalettesList.addAll(receivedList);
                for (GbcPalette gbcPalette : receivedList) {
                    Methods.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                for (GbcPalette gbcPalette : Methods.gbcPalettesList) {
                    paletteDao.insert(gbcPalette);
                }
            }

            if (frames.size() > 0) {
                for (GbcFrame gbcFrame : frames) {
                    Methods.hashFrames.put(gbcFrame.getFrameName(), gbcFrame);
                }
                Methods.framesList.addAll(frames);
            } else {
                //First time add it to the database
                StartCreation.addFrames(getBaseContext());
                for (Map.Entry<String, GbcFrame> entry : Methods.hashFrames.entrySet()) {
                    GbcFrame value = entry.getValue();
                    frameDao.insert(value);
                }
            }
            //Now that I have palettes and frames, I can add images:
            if (imagesFromDao.size() > 0) {
                anyImage = true;
                //I need to add them to the gbcImagesList(GbcImage)
                Methods.gbcImagesList.addAll(imagesFromDao);
                GbcImage.numImages += Methods.gbcImagesList.size();
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
                GalleryFragment.tv.setText(GalleryFragment.tv.getContext().getString(R.string.no_images));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //resume tasks needing this permission
            Toast toast = Toast.makeText(this, "Granted permissions.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
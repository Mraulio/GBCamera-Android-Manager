package com.mraulio.gbcameramanager;

import static com.mraulio.gbcameramanager.utils.DiskCache.CACHE_DIR_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.mraulio.gbcameramanager.utils.UncaughtExceptionHandler;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.utils.StartCreation;
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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    boolean anyImage = true;
    private ActivityMainBinding binding;
    public static boolean pressBack = true;
    public static boolean doneLoading = false;

    public static enum CURRENT_FRAGMENT {
        GALLERY,
        PALETTES,
        FRAMES,
        IMPORT,
        USB_SERIAL,
        SAVE_MANAGER,
        SETTINGS
    }

    public static CURRENT_FRAGMENT current_fragment;

    public static FloatingActionButton fab;

    public static SharedPreferences sharedPreferences;
    //Store in the shared preferences
    public static boolean exportPng = true;
    public static boolean printingEnabled = false;
    public static boolean showPaperizeButton = false;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static String languageCode;
    public static boolean magicCheck;
    public static boolean showRotationButton;
    public static int customColorPaper;
    public static int lastSeenGalleryImage = 0;
    public static boolean exportSquare = false;

    private boolean openedSav = false;
    public static UsbManager manager;
    public static int[] deletedCount = new int[7];

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
        //Unhandled Exception Manager
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        sharedPreferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        exportSize = sharedPreferences.getInt("export_size", 4);
        imagesPage = sharedPreferences.getInt("images_per_page", 12);
        exportPng = sharedPreferences.getBoolean("export_as_png", true);
        showPaperizeButton = sharedPreferences.getBoolean("show_paperize_button", false);
        printingEnabled = sharedPreferences.getBoolean("print_enabled", false);
        magicCheck = sharedPreferences.getBoolean("magic_check", true);
        showRotationButton = sharedPreferences.getBoolean("rotation_button", true);
        customColorPaper = sharedPreferences.getInt("custom_paper_color", Color.WHITE);
        exportSquare = sharedPreferences.getBoolean("export_square", false);

        String previousVersion = sharedPreferences.getString("previous_version", "0");
        GalleryFragment.currentPage = sharedPreferences.getInt("current_page", 0);

        //To get the locale on the first startup and set the def value
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        LocaleList locales = null;
        Locale currentLocale = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locales = configuration.getLocales();
            currentLocale = locales.get(0);
        } else {
            //For SDK 23 or lower
            currentLocale = configuration.locale;
        }

        String currentVersion = BuildConfig.VERSION_NAME;
        if (Float.valueOf(currentVersion) > Float.valueOf(previousVersion)) {
            //App has been updated, do something if necessary
            deleteImageCache();
            // Update version name for future comparisons
            editor.putString("previous_version", currentVersion);
            editor.apply();

        }
        if (!currentLocale.getLanguage().equals("es") && !currentLocale.getLanguage().equals("en")
                && !currentLocale.getLanguage().equals("fr") && !currentLocale.getLanguage().equals("de") && !currentLocale.getLanguage().equals("pt")) {
            languageCode = "en";
        } else {
            languageCode = currentLocale.getLanguage();
        }

        languageCode = sharedPreferences.getString("language", languageCode);
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        fab = findViewById(R.id.fab);

        Utils.makeDirs();
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "Database").build();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        // Obtain Intent information
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && type != null && type.equals("application/octet-stream") && uri != null && uri.toString().endsWith(".sav")) {
            // IF the Intent contains the action ACTION_VIEW and the category CATEGORY_DEFAULT and
            // the type is "application/octet-stream" and the URI of the Intent ends in ".sav", make the desired action
            Utils.toast(this, "Opened from file");
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
        fab = binding.appBarMain.fab;
        fab.hide();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_gallery)
                .setOpenableLayout(drawer)
                .build();


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                invalidateOptionsMenu();
            }

        });
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

    public void restartApplication() {
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
    private void deleteImageCache() {
        //Deleting cache for the next version only
        File cacheDir = new File(getApplicationContext().getCacheDir(), CACHE_DIR_NAME);
        // Borra todos los archivos dentro del directorio de caché
        if (cacheDir != null && cacheDir.isDirectory()) {
            File[] cacheFiles = cacheDir.listFiles();
            if (cacheFiles != null) {
                for (File cacheFile : cacheFiles) {
                    cacheFile.delete();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (current_fragment) {
            case GALLERY:
                menu.clear(); // Cleans the current menu
                getMenuInflater().inflate(R.menu.gallery_menu, menu); // Inflates the menu
                break;

            case PALETTES:
            case SETTINGS:
            case SAVE_MANAGER:
            case USB_SERIAL:
            case IMPORT:
            case FRAMES:
                menu.clear(); // Cleans the current menu
                fab.hide();
                menu.close();
                break;

        }
        return super.onPrepareOptionsMenu(menu);
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
                    Utils.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                Utils.gbcPalettesList.addAll(palettes);
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
                Utils.gbcPalettesList.addAll(receivedList);
                for (GbcPalette gbcPalette : receivedList) {
                    Utils.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                for (GbcPalette gbcPalette : Utils.gbcPalettesList) {
                    paletteDao.insert(gbcPalette);
                }
            }

            if (frames.size() > 0) {
                for (GbcFrame gbcFrame : frames) {
                    Utils.hashFrames.put(gbcFrame.getFrameName(), gbcFrame);
                }
                Utils.framesList.addAll(frames);
            } else {
                //First time add it to the database
                StartCreation.addFrames(getBaseContext());
                for (Map.Entry<String, GbcFrame> entry : Utils.hashFrames.entrySet()) {
                    GbcFrame value = entry.getValue();
                    frameDao.insert(value);
                }
            }
            //Now that I have palettes and frames, I can add images:
            if (imagesFromDao.size() > 0) {
                //I need to add them to the gbcImagesList(GbcImage)
                Utils.gbcImagesList.addAll(imagesFromDao);
                GbcImage.numImages += Utils.gbcImagesList.size();
            } else anyImage = false;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            GalleryFragment gf = new GalleryFragment();
            doneLoading = true;
            gf.updateFromMain();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //resume tasks needing this permission
            Toast toast = Toast.makeText(this, getString(R.string.permissions_toast), Toast.LENGTH_LONG);
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
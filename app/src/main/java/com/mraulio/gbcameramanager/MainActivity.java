package com.mraulio.gbcameramanager;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.hideSelectionOptions;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectionMode;
import static com.mraulio.gbcameramanager.utils.Utils.createNotificationChannel;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupSorting;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.sortPalettes;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import static android.os.Build.VERSION.SDK_INT;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    Uri mUri;
    NavController mNavController;
    boolean mAnyImage = true;
    boolean mOpenedFromUsb = false;
    LoadingDialog mLoadDialog;
    public static boolean pressBack = true;
    public static boolean doneLoading = false;
    public static UsbManager manager;
    public static boolean openedFromFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Unhandled Exception Manager
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        StaticValues.sharedPreferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = StaticValues.sharedPreferences.edit();

        StaticValues.exportSize = StaticValues.sharedPreferences.getInt("export_size", 4);
        StaticValues.imagesPage = StaticValues.sharedPreferences.getInt("images_per_page", 12);
        StaticValues.exportPng = StaticValues.sharedPreferences.getBoolean("export_as_png", true);
        StaticValues.showPaperizeButton = StaticValues.sharedPreferences.getBoolean("show_paperize_button", false);
        StaticValues.printingEnabled = StaticValues.sharedPreferences.getBoolean("print_enabled", false);
        StaticValues.magicCheck = StaticValues.sharedPreferences.getBoolean("magic_check", true);
        StaticValues.showRotationButton = StaticValues.sharedPreferences.getBoolean("rotation_button", true);
        StaticValues.customColorPaper = StaticValues.sharedPreferences.getInt("custom_paper_color", Color.WHITE);
        StaticValues.exportSquare = StaticValues.sharedPreferences.getBoolean("export_square", false);
        StaticValues.sortMode = StaticValues.sharedPreferences.getString("sort_by_date", StaticValues.SORT_MODE.CREATION_DATE.name());
        StaticValues.defaultPaletteId = StaticValues.sharedPreferences.getString("default_palette_id","bw");
        StaticValues.defaultFrameId = StaticValues.sharedPreferences.getString("default_frame_id","gbcam01");
        StaticValues.dateLocale = StaticValues.sharedPreferences.getString("date_locale", "yyyy-MM-dd");
        StaticValues.exportMetadata = StaticValues.sharedPreferences.getBoolean("export_metadata", false);
        StaticValues.alwaysDefaultFrame = StaticValues.sharedPreferences.getBoolean("always_default_frame", false);

        StaticValues.filterMonth = StaticValues.sharedPreferences.getBoolean("date_filter_month", false);
        StaticValues.filterYear = StaticValues.sharedPreferences.getBoolean("date_filter_year", false);
        StaticValues.filterByDate = StaticValues.sharedPreferences.getBoolean("date_filter_by_date", false);
        StaticValues.dateFilter = StaticValues.sharedPreferences.getLong("date_filter", System.currentTimeMillis());

        if (StaticValues.sortMode != null) {
            StaticValues.sortModeEnum = StaticValues.SORT_MODE.valueOf(StaticValues.sortMode);
            StaticValues.sortModeEnum = StaticValues.SORT_MODE.valueOf(StaticValues.sortMode);
        }
        StaticValues.sortDescending = StaticValues.sharedPreferences.getBoolean("sort_descending", false);
        StaticValues.selectedTags = StaticValues.sharedPreferences.getString("selected_tags", "");
        StaticValues.hiddenTags = StaticValues.sharedPreferences.getString("hidden_tags", "");

        String previousVersion = StaticValues.sharedPreferences.getString("previous_version", "0");
        GalleryFragment.currentPage = StaticValues.sharedPreferences.getInt("current_page", 0);
        //To get the locale on the first startup and set the def value
        Resources resources = getResources();
        LocaleList locales = null;
        Locale currentLocale = null;

        Configuration configuration = resources.getConfiguration();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locales = configuration.getLocales();
            currentLocale = locales.get(0);
        } else {
            //For SDK 23 or lower
            currentLocale = configuration.locale;
        }

        if (!currentLocale.getLanguage().equals("es") && !currentLocale.getLanguage().equals("en")
                && !currentLocale.getLanguage().equals("fr") && !currentLocale.getLanguage().equals("de") && !currentLocale.getLanguage().equals("pt") && !currentLocale.getLanguage().equals("ca")) {
            StaticValues.languageCode = "en";
        } else {
            StaticValues.languageCode = currentLocale.getLanguage();
        }

        String currentVersion = BuildConfig.VERSION_NAME;

        if (Float.valueOf(currentVersion) > Float.valueOf(previousVersion)) {
            //App has been updated, do something if necessary
//            deleteImageCache(getBaseContext());
            // Update version name for future comparisons
            editor.putString("previous_version", currentVersion);
            editor.apply();
        }

        StaticValues.languageCode = StaticValues.sharedPreferences.getString("language", StaticValues.languageCode);
        Locale locale = new Locale(StaticValues.languageCode);
        Locale.setDefault(locale);

        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        StaticValues.db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "Database").build();

        // Obtain Intent information
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        mUri = intent.getData();
        if (mUri == null){
            mUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);// For the SEND action
        }



        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View headerView = navigationView.getHeaderView(0);
        TextView tvGit = headerView.findViewById(R.id.tvGit);
        TextView tvWiki = headerView.findViewById(R.id.tvWiki);

        tvGit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wikiUrl = "https://github.com/Mraulio/GBCamera-Android-Manager";

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wikiUrl));

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        tvWiki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String gitUrl = "https://github.com/Mraulio/GBCamera-Android-Manager/wiki";

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(gitUrl));

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        StaticValues.fab = binding.appBarMain.fab;
        StaticValues.fab.hide();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_gallery)
                .setOpenableLayout(drawer)
                .build();

        if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) && type != null) {

            // IF the Intent contains the action ACTION_VIEW and the category CATEGORY_DEFAULT and
            openedFromFile = true;
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            mOpenedFromUsb = true;
        }

        if (!doneLoading) {
            mLoadDialog = new LoadingDialog(this, null);
            mLoadDialog.setLoadingDialogText(getString(R.string.loading));
            mLoadDialog.showDialog();
            new ReadDataAsyncTask().execute();
        }
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, mNavController);
        mNavController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                invalidateOptionsMenu();
            }

        });

        requestPermissions();

        Utils.makeDirs();//If permissions granted, create the folders(Keep this for the updated versions with already permissions, to create the frame json folder)
        createNotificationChannel(getBaseContext());
    }

    private void requestPermissions(){
        if ( SDK_INT <= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
        if (SDK_INT == Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1);
        }
    }
    private void openingFromIntent(NavController navController) {

        if (openedFromFile) {
            Bundle bundle = new Bundle();
            bundle.putString("fileUri", mUri.toString());
            navController.navigate(R.id.nav_import, bundle);
        } else if (mOpenedFromUsb) {
            navController.navigate(R.id.nav_usbserial);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        switch (StaticValues.currentFragment) {
            case GALLERY:
                menu.clear(); // Cleans the current menu
                getMenuInflater().inflate(R.menu.gallery_menu, menu); // Inflates the menu
                if (StaticValues.showEditMenuButton) {
                    menu.getItem(0).setVisible(true);
                }
                if (selectionMode[0]) StaticValues.fab.show();

                if (StaticValues.fab != null && !StaticValues.fab.hasOnClickListeners()) {
                    Activity activity = this;
                    StaticValues.fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideSelectionOptions(activity);
                        }
                    });
                }
                break;

            case PALETTES:
            case SETTINGS:
            case SAVE_MANAGER:
            case USB_SERIAL:
            case IMPORT:
            case FRAMES:
                menu.clear(); // Cleans the current menu
                StaticValues.fab.hide();
                menu.close();
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }


    private class ReadDataAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            PaletteDao paletteDao = StaticValues.db.paletteDao();
            FrameDao frameDao = StaticValues.db.frameDao();
            ImageDao imageDao = StaticValues.db.imageDao();

            List<GbcPalette> palettes = paletteDao.getAll();
            List<GbcFrame> frames = frameDao.getAll();
            List<GbcImage> imagesFromDao = imageDao.getAll();

            if (palettes.size() > 0) {
                for (GbcPalette gbcPalette : palettes) {
                    Utils.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                Utils.gbcPalettesList.addAll(palettes);
                //Sort the palettes for the palette grid, showing first the favorites
                sortPalettes();
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
                //Sort the palettes for the palette grid, showing first the favorites
                sortPalettes();
                for (GbcPalette gbcPalette : receivedList) {
                    Utils.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                }
                for (GbcPalette gbcPalette : Utils.gbcPalettesList) {
                    paletteDao.insert(gbcPalette);
                }
            }

            if (frames.size() > 0) {
                for (GbcFrame gbcFrame : frames) {
                    Utils.hashFrames.put(gbcFrame.getFrameId(), gbcFrame);
                }
                Utils.frameGroupsNames = hashFrames.get("gbcam01").getFrameGroupsNames();
                Utils.framesList.addAll(frames);
                //Now sort them by group and id
                frameGroupSorting();
            } else {
                //First time add it to the database
                StartCreation.addFrames(getBaseContext());
                for (Map.Entry<String, GbcFrame> entry : Utils.hashFrames.entrySet()) {
                    GbcFrame value = entry.getValue();
                    frameDao.insert(value);//Saving frames to database
                }
            }
            //Now that I have palettes and frames, I can add images:
            if (imagesFromDao.size() > 0) {
                //I need to add them to the gbcImagesList(GbcImage)
                Utils.gbcImagesList.addAll(imagesFromDao);
                GbcImage.numImages += Utils.gbcImagesList.size();
            } else mAnyImage = false;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            GalleryFragment gf = new GalleryFragment();
            doneLoading = true;
            openingFromIntent(mNavController);
            gf.updateFromMain(MainActivity.this);
            if (mLoadDialog != null && mLoadDialog.isShowing()){
                mLoadDialog.dismissDialog();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //resume tasks needing this permission
            toast(this, getString(R.string.permissions_toast));
            Utils.makeDirs();//If permissions granted, create the folders
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    private void openFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_gallery, fragment)
                .commit();
    }

}
package com.mraulio.gbcameramanager.utils;

import android.content.SharedPreferences;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mraulio.gbcameramanager.db.AppDatabase;

public class StaticValues {

    //Store in the shared preferences
    public static boolean exportSquare = false;
    public static boolean exportPng = true;
    public static boolean printingEnabled = false;
    public static boolean showPaperizeButton = false;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static String languageCode;
    public static String defaultPaletteId;
    public static String defaultFrameId;
    public static boolean magicCheck;
    public static boolean showRotationButton;
    public static int customColorPaper;
    public static int lastSeenGalleryImage = 0;
    public static String sortMode = "";
    public static String dateLocale = "";
    public static String selectedTags = "";
    public static String hiddenTags = "";
    public static boolean sortDescending = false;
    public static long dateFilter;
    public static boolean filterMonth;
    public static boolean filterYear;
    public static boolean filterByDate;
    public static FloatingActionButton fab;
    public static AppDatabase db;
    public static CURRENT_FRAGMENT currentFragment;
    public static SharedPreferences sharedPreferences;
    public static SORT_MODE sortModeEnum = SORT_MODE.CREATION_DATE;
    public static int[] deletedCount = new int[7];
    public static boolean showEditMenuButton = false;
    public static boolean exportMetadata = false;
    public static boolean alwaysDefaultFrame = false;

    public enum CURRENT_FRAGMENT {
        GALLERY,
        PALETTES,
        FRAMES,
        IMPORT,
        USB_SERIAL,
        SAVE_MANAGER,
        SETTINGS
    }

    public enum SORT_MODE {
        CREATION_DATE,
        IMPORT_DATE,
        TITLE
    }

    public static final String FILTER_FAVOURITE = "__filter:favourite__";
    public static final String FILTER_SUPER_FAVOURITE = "__filter:superfav__";
    public static final String FILTER_DUPLICATED = "__filter:duplicated__";
    public static final String FILTER_TRANSFORMED = "__filter:transformed__";

    public static final String TAG_FAVOURITE = "Favourite \u2764\ufe0f";
    public static final String TAG_SUPER_FAVOURITE = "Superfav \u2B50\ufe0f";
    public static final String TAG_DUPLICATED = "Duplicated \uD83D\uDC11";
    public static final String TAG_TRANSFORMED = "Transformed \uD83D\uDD04";


}

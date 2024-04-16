package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.hiddenTags;
import static com.mraulio.gbcameramanager.utils.StaticValues.selectedTags;
import static com.mraulio.gbcameramanager.utils.StaticValues.sharedPreferences;
import static com.mraulio.gbcameramanager.utils.DiskCache.CACHE_DIR_NAME;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.Codec;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class with puclic static variables and methods that are shared alongside the app
 */
public class Utils {
    static File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    static File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

    public static int notificationId = 0;
    public static final File MAIN_FOLDER = new File(downloadDirectory, "GBCamera Manager");
    public static final File SAVE_FOLDER = new File(MAIN_FOLDER, "Save dumps");
    public static final File IMAGES_FOLDER = new File(picturesDirectory, "GBCamera Manager");
    public static final File IMAGES_JSON = new File(MAIN_FOLDER, "Images json");
    public static final File TXT_FOLDER = new File(MAIN_FOLDER, "Hex images");
    public static final File PALETTES_FOLDER = new File(MAIN_FOLDER, "Palettes json");
    public static final File FRAMES_FOLDER = new File(MAIN_FOLDER, "Frames json");
    public static final File ARDUINO_HEX_FOLDER = new File(MAIN_FOLDER, "Arduino Printer Hex");
    public static final File PHOTO_DUMPS_FOLDER = new File(MAIN_FOLDER, "PHOTO Rom Dumps");
    public static final File DB_BACKUP_FOLDER = new File(MAIN_FOLDER, "DB Backup");
    public static final String CHANNEL_ID = "gbcam_channel";
    public static final String CHANNEL_NAME = "GBCAM Channel";

    private static final String DB_NAME = "Database";
    private static final String DB_NAME_SHM = "Database-shm";
    private static final String DB_NAME_WAL = "Database-wal";

    public static LinkedHashMap<String, String> frameGroupsNames = new LinkedHashMap<>();

    public static final int[] ROTATION_VALUES = {0, 90, 180, 270};
    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static ArrayList<GbcPalette> sortedPalettes = new ArrayList<>();
    public static List<GbcFrame> framesList = new ArrayList<>();
    public static HashMap<String, Bitmap> imageBitmapCache = new HashMap<>();
    public static LinkedHashMap<String, GbcFrame> hashFrames = new LinkedHashMap<>();
    public static HashMap<String, GbcPalette> hashPalettes = new HashMap<>();

    public static LinkedHashSet<String> tagsHash = new LinkedHashSet<>();

    public static enum SAVE_TYPE_INT_JP_HK {
        INT,
        JP,
        HK
    }

    public static HashMap<String, String> saveTypeNames = new HashMap<String, String>() {{
        put("International", "INT");
        put("Japanese", "JP");
        put("Hello Kitty", "HK");
    }};

    //Auxiliary method to convert byte[] to hexadecimal String
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] encodeImage(Bitmap bitmap, String paletteId) throws IOException {
        Codec decoder = new ImageCodec(160, bitmap.getHeight());
        return decoder.encodeInternal(bitmap, paletteId);
    }

    public static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));
        }
        return bytes;
    }

    public static void makeDirs() {
        List<File> listFiles = new ArrayList<>();
        listFiles.add(MAIN_FOLDER);
        listFiles.add(SAVE_FOLDER);
        listFiles.add(IMAGES_FOLDER);
        listFiles.add(IMAGES_JSON);
        listFiles.add(TXT_FOLDER);
        listFiles.add(PALETTES_FOLDER);
        listFiles.add(FRAMES_FOLDER);
        listFiles.add(ARDUINO_HEX_FOLDER);
        listFiles.add(PHOTO_DUMPS_FOLDER);

        for (File file : listFiles) {
            try {
                file.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static LinkedHashSet<String> retrieveTags(List<GbcImage> gbcImages) {
        tagsHash.clear();
        for (GbcImage gbcImage : gbcImages) {
            for (String tag : gbcImage.getTags()) {
                tagsHash.add(tag);
            }
        }
        return tagsHash;
    }

    public static void saveTagsSet(HashSet<String> tagsList, boolean saveAsHiddenTag) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(tagsList);
        if (!saveAsHiddenTag) {
            editor.putString("selected_tags", json);
        } else {
            editor.putString("hidden_tags", json);
        }
        editor.apply();
    }

    public static HashSet<String> getSelectedTags() {
        try {
            HashSet<String> tagList;

            if (!selectedTags.isEmpty()) {
                tagList = new Gson().fromJson(selectedTags, new TypeToken<HashSet<String>>() {
                }.getType());
            } else {
                tagList = new HashSet<>();
            }
            return tagList;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public static HashSet<String> getHiddenTags() {
        try {
            HashSet<String> hiddenTagList;

            if (!hiddenTags.isEmpty()) {
                hiddenTagList = new Gson().fromJson(hiddenTags, new TypeToken<HashSet<String>>() {
                }.getType());
            } else {
                hiddenTagList = new HashSet<>();
            }
            return hiddenTagList;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static Bitmap rotateBitmap(Bitmap originalBitmap, GbcImage gbcImage) {
        Matrix matrix = new Matrix();
        matrix.postRotate(ROTATION_VALUES[gbcImage.getRotation()]);
        Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
        return rotatedBitmap;
    }

    public static Bitmap transparentBitmap(Bitmap bitmap, GbcFrame gbcFrame) {
        HashSet<int[]> transparencyHS = null;
        if (gbcFrame.getTransparentPixelPositions().size() == 0) {
            transparencyHS = transparencyHashSet(gbcFrame.getFrameBitmap());
            if (transparencyHS.size() == 0) {
                transparencyHS = generateDefaultTransparentPixelPositions(gbcFrame.getFrameBitmap());
            }
            gbcFrame.setTransparentPixelPositions(transparencyHS);
        } else {
            transparencyHS = gbcFrame.getTransparentPixelPositions();
        }

        int transparentPixel = Color.argb(0, 0, 0, 0);
        for (int[] position : transparencyHS) {
            bitmap.setPixel(position[0], position[1], transparentPixel);
        }
        return bitmap;
    }

    public static HashSet<int[]> transparencyHashSet(Bitmap bitmap) {
        HashSet<int[]> transparentPixelPositions = new HashSet<>();
        // Iterate through the bitmap pixels
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) == 0) {
                    int[] pos = {x, y};
                    transparentPixelPositions.add(pos);
                }
            }
        }
        return transparentPixelPositions;
    }

    public static HashSet<int[]> generateDefaultTransparentPixelPositions(Bitmap bitmap) {
        HashSet<int[]> transparentPixelPositions = new HashSet<>();

        int bitmapHeight = bitmap.getHeight();
        int innerBitmapWidth = 128;
        int innerBitmapHeight = 112;
        int startX = 16;
        int startY = 16;
        if (bitmapHeight == 224) startY = 40;

        for (int y = startY; y < startY + innerBitmapHeight; y++) {
            for (int x = startX; x < startX + innerBitmapWidth; x++) {
                int[] pos = {x, y};
                transparentPixelPositions.add(pos);
            }
        }
        return transparentPixelPositions;
    }

    public static String generateHashFromBytes(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        String hashHex = Utils.bytesToHex(hash);
        return hashHex;
    }

    public static String removeNumbersFromEnd(String input) {
        return input.replaceAll("\\d+$", "");
    }

    public static void backupDatabase(Context context) {
        try {
            //Get the database version first.
            int databaseVersion = StaticValues.db.getOpenHelper().getReadableDatabase().getVersion();

            SimpleDateFormat sdf = new SimpleDateFormat(dateLocale+"_HH-mm-ss", Locale.getDefault());
            Date date = new Date();
            File backupDir = new File(DB_BACKUP_FOLDER + "/" + sdf.format(date) + "_v" + databaseVersion);

            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            File dataDir = Environment.getDataDirectory();
            File currentDB = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME);
            File backupDB = new File(backupDir, DB_NAME);
            File currentDB_shm = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME_SHM);
            File backupDB_shm = new File(backupDir, DB_NAME_SHM);
            File currentDB_wal = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME_WAL);
            File backupDB_wal = new File(backupDir, DB_NAME_WAL);

            FileChannel src = new FileInputStream(currentDB).getChannel();
            FileChannel dst = new FileOutputStream(backupDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            src = new FileInputStream(currentDB_shm).getChannel();
            dst = new FileOutputStream(backupDB_shm).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            src = new FileInputStream(currentDB_wal).getChannel();
            dst = new FileOutputStream(backupDB_wal).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            toast(context, context.getString(R.string.toast_backup_db) + "\nVersion: " + databaseVersion);
        } catch (IOException e) {
            e.printStackTrace();
            toast(context, "Error creating DB backup");
        }
    }

    public static void showDbBackups(Context context, Activity activity) {

        //Show only the database backups with version equal to actual version
        int databaseVersion = StaticValues.db.getOpenHelper().getReadableDatabase().getVersion();

        File directory = new File(DB_BACKUP_FOLDER.toURI());

        final List<File> directories = new ArrayList<>();
        File[] files = directory.listFiles();
        final List<String> directoriesNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    //If the directory name matches the database version, show it

                    Pattern pattern = Pattern.compile("\\d+$");
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.find()) {
                        int version = Integer.parseInt(matcher.group());
                        if (version == databaseVersion) {
                            directories.add(file);
                            directoriesNames.add(file.getName());
                        }
                    }
                }
            }
        }

        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.restore_db_dialog);

        final File[] selectedDirectory = {null};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, directoriesNames);
        ListView listView = dialog.findViewById(R.id.listViewRestoreDb);
        listView.setAdapter(adapter);
        Button btnCancelRestoreDb = dialog.findViewById(R.id.btnCancelRestoreDb);
        btnCancelRestoreDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        Button btnOkRestoreDb = dialog.findViewById(R.id.btnOkRestoreDb);
        btnOkRestoreDb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDirectory[0] == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getString(R.string.dialog_confirm_restore_db))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                restoreDatabase(context, selectedDirectory[0], activity);
                            }
                        })
                        .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });

        dialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDirectory[0] = directories.get(position);
            }
        });
    }

    public static void restoreDatabase(Context context, File backupDir, Activity activity) {
        try {
            File dataDir = Environment.getDataDirectory();
            File currentDB = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME);
            File backupDB = new File(backupDir, DB_NAME);
            File currentDB_shm = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME_SHM);
            File backupDB_shm = new File(backupDir, DB_NAME_SHM);
            File currentDB_wal = new File(dataDir, "/data/" + context.getPackageName() + "/databases/" + DB_NAME_WAL);
            File backupDB_wal = new File(backupDir, DB_NAME_WAL);

            FileChannel src = new FileInputStream(backupDB).getChannel();
            FileChannel dst = new FileOutputStream(currentDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            src = new FileInputStream(backupDB_shm).getChannel();
            dst = new FileOutputStream(currentDB_shm).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            src = new FileInputStream(backupDB_wal).getChannel();
            dst = new FileOutputStream(currentDB_wal).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();

            //Clear shared preferences and cache
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            deleteImageCache(context);
            toast(context, context.getString(R.string.toast_restore_db));
            restartApplication(context);
        } catch (IOException e) {
            e.printStackTrace();
            toast(context, "Error restoring DB backup");
        }
    }

    public static void deleteImageCache(Context context) {
        //Deleting cache for the next version only
        File cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        // Delete all files within the cache directory
        if (cacheDir != null && cacheDir.isDirectory()) {
            File[] cacheFiles = cacheDir.listFiles();
            if (cacheFiles != null) {
                for (File cacheFile : cacheFiles) {
                    cacheFile.delete();
                }
            }
        }
    }

    public static void restartApplication(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        // Required for API 34 and later
        // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
        mainIntent.setPackage(context.getPackageName());
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void showNotification(Context context, File downloadedFile) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", downloadedFile);
        intent.setDataAndType(fileUri, context.getContentResolver().getType(fileUri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(context.getResources().getString(R.string.notification_download_complete))
                .setContentText(downloadedFile.getName())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId++, builder.build());
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void frameGroupSorting() {
        List<GbcFrame> sortedFrameList = new ArrayList<>();

        List<GbcFrame> sortedFrameGroup = new ArrayList<>();

        // Sort each frame group by id and add it to the final list
        for (String key : frameGroupsNames.keySet()) {
            sortedFrameGroup.clear();
            for (GbcFrame gbcFrame : Utils.framesList) {
                String gbcFrameGroup = gbcFrame.getFrameId().replaceAll("^(\\D+).*", "$1");//To remove the numbers at the end
                if (gbcFrameGroup.equals(key)) {
                    sortedFrameGroup.add(gbcFrame);
                }
            }
            //Now sort the group by id
            Comparator<GbcFrame> comparator = new Comparator<GbcFrame>() {
                @Override
                public int compare(GbcFrame frame1, GbcFrame frame2) {
                    int titleComparison = frame1.getFrameId().compareTo(frame2.getFrameId());

                    return titleComparison;
                }
            };
            Collections.sort(sortedFrameGroup, comparator);
            sortedFrameList.addAll(sortedFrameGroup);
        }
        Utils.framesList = sortedFrameList;
    }

    public static void sortPalettes() {
        sortedPalettes.clear();
        //First add the favorites on top
        for (GbcPalette palette : gbcPalettesList) {
            if (palette.isFavorite()) {
                sortedPalettes.add(palette);
            }
        }
        //Then add the rest of the palettes
        for (GbcPalette palette : gbcPalettesList) {
            if (!palette.isFavorite()) {
                sortedPalettes.add(palette);
            }
        }
    }
}


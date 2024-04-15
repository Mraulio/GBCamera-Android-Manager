package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.Utils.MAIN_FOLDER;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context context;

    public UncaughtExceptionHandler(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String stackTrace = Log.getStackTraceString(ex);
        Log.e("GBCamera Manager", "Unhandled Exception in thread " + thread.getName() + ": " + stackTrace);
        logError(stackTrace);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showErrorDialog(context, "There was an unexpected error.\nA log was saved to /Download/GBCamera Manager.\n\nApp will close now.");
            }
        });
    }

    private void showErrorDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                System.exit(2);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    public static void logError(String message) {
        try {
            Date nowDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat(dateLocale+" HH:mm:ss", Locale.getDefault());
            String date = sdf.format(nowDate);

            BufferedWriter writer = new BufferedWriter(new FileWriter(MAIN_FOLDER + "/error_log.txt", true));
            String deviceInfo = "********************************\n";
            deviceInfo += "Date: " + date + "\n";
            deviceInfo += "OS version: " + System.getProperty("os.version") + "\n";
            deviceInfo += "API level: " + Build.VERSION.SDK_INT + "\n";
            deviceInfo += "Manufacturer: " + Build.MANUFACTURER + "\n";
            deviceInfo += "Device: " + Build.DEVICE + "\n";
            deviceInfo += "Model: " + Build.MODEL + "\n";
            deviceInfo += "Product: " + Build.PRODUCT + "\n";
            writer.write(deviceInfo);
            writer.newLine();
            writer.write(message);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
package com.mraulio.gbcameramanager.utils;
import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiskCache {

    private static final String CACHE_DIR_NAME = "images_cache";

    private File cacheDir;

    public DiskCache(Context context) {
        cacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void put(String key, byte[] data) {
        File file = new File(cacheDir, key);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] get(String key) {
        File file = new File(cacheDir, key);
        if (!file.exists()) {
            return null;
        }

        byte[] data = new byte[(int) file.length()];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            inputStream.read(data);
        } catch (IOException e) {
            e.printStackTrace();
            data = null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return data;
    }
}

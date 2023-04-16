package com.mraulio.gbcameramanager.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeConverters {
    @TypeConverter
    public static byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @TypeConverter
    public static Bitmap toBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    @TypeConverter
    public static String fromList(List<String> tags) {
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            sb.append(tag);
            sb.append(",");
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<String> toList(String data) {
        String[] tags = data.split(",");
        return new ArrayList<>(Arrays.asList(tags));
    }


}
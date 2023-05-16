package com.mraulio.gbcameramanager.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TypeConverters {
    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

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

    @TypeConverter
    public static Date toDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(FORMAT);
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    @TypeConverter
    public static String fromDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(FORMAT);
        return format.format(date);
    }
}
package com.mraulio.gbcameramanager.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    public static ArrayList<String> toList(String value) {
        try {
            return new Gson().fromJson(value, new TypeToken<ArrayList<String>>() {}.getType());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
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

    @TypeConverter
    public static LinkedHashMap<String, String> fromString(String value) {
        if (value == null) {
            return null;
        }
        return new Gson().fromJson(value, LinkedHashMap.class);
    }

    @TypeConverter
    public static String fromMap(LinkedHashMap<String, String> map) {
        if (map == null) {
            return null;
        }
        return new Gson().toJson(map);
    }

    @TypeConverter
    public static HashSet<String> tagsFromString(String value) {
        if (value == null) {
            return null;
        }
        return new Gson().fromJson(value, HashSet.class);
    }

    @TypeConverter
    public static String fromHashSet(HashSet<String> set) {
        if (set == null) {
            return null;
        }
        return new Gson().toJson(set);
    }
}
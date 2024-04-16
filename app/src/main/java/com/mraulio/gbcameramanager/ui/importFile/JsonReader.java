package com.mraulio.gbcameramanager.ui.importFile;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.frameChange;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.importedFrameGroupIdNames;
import static com.mraulio.gbcameramanager.utils.Utils.generateHashFromBytes;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class JsonReader {

    static final String INTERNATIONAL_FRAMES = "International (Game Boy Camera) Frames";
    static final String JAPANESE_FRAMES = "Japanese (Pocket Camera) Frames";
    static final String HELLO_KITTY_FRAMES = "Hello Kitty Frames";

    public static List<?> jsonCheck(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            if (!jsonObject.has("state")) {
                return null;
            }
            JSONObject stateObject = jsonObject.getJSONObject("state");

            if (stateObject.has("palettes")) {
                JSONArray palettesArray = stateObject.getJSONArray("palettes");
                if (palettesArray.length() == 0) {
                    return null;
                }
                JSONObject paletteObject = palettesArray.getJSONObject(0);

                // Verify that the keys and values exist in the palette
                if (paletteObject.has("shortName") && paletteObject.has("name") && paletteObject.has("palette")) {
                    JSONArray paletteArray = paletteObject.getJSONArray("palette");

                    if (paletteArray.length() != 4) {
                        return null;
                    }
                    return readerPalettes(palettesArray);
                } else {

                    return null;
                }

            } else if (stateObject.has("frames")) {
                return readerFrames(jsonObject, stateObject);

            } else if (stateObject.has("images")) {
                //Images json
                JSONArray imagesArray = stateObject.getJSONArray("images");
                if (imagesArray.length() == 0) {
                    return null;
                }
                JSONObject imageObject = imagesArray.getJSONObject(0);
                if (imageObject.has("hash") && imageObject.has("created") && imageObject.has("title") /*&& imageObject.has("lines")*/ && imageObject.has("tags")) {
                    //There are some more values to check, but not all images have those
                    return readerImages(jsonObject);
                } else return null;
            } else {
                return null;
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> readerImages(JSONObject jsonObject) throws IOException, JSONException {
        List<String> stringValues = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        // Access JSON values
        JSONArray images = jsonObject.getJSONObject("state").getJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject imageJson = images.getJSONObject(i);
            try {
                if (!imageJson.has("isRGBN") || (imageJson.has("isRGBN") && imageJson.get("isRGBN").equals(false))) {
                    String hash = imageJson.getString("hash");
                    stringValues.add(hash);
                    String data = jsonObject.getString(hash);
                    String decodedData = decodeDataImage(data);
                    byte[] bytes = Utils.convertToByteArray(decodedData);
                    GbcImage gbcImage = new GbcImage();
                    gbcImage.setHashCode(hash);
                    gbcImage.setImageBytes(bytes);
                    gbcImage.setName(imageJson.getString("title"));
                    JSONArray tagsArray = imageJson.getJSONArray("tags");
                    if (tagsArray.length() > 0) {
                        HashSet<String> tagsStrings = new HashSet<>();
                        for (int j = 0; j < tagsArray.length(); j++) {
                            String str = tagsArray.getString(j);
                            tagsStrings.add(str);
                        }
                        gbcImage.setTags(tagsStrings);
                    }
                    if (imageJson.has("palette")) {
                        if (!Utils.hashPalettes.containsKey(imageJson.getString("palette"))) {
                            gbcImage.setPaletteId("bw");
                        } else
                            gbcImage.setPaletteId(imageJson.getString("palette").toLowerCase());//To get the palette from the json
                    }

                    if (imageJson.has("framePalette")) {
                        if (!Utils.hashPalettes.containsKey(imageJson.getString("framePalette"))) {
                            gbcImage.setFramePaletteId("bw");
                        } else
                            gbcImage.setFramePaletteId(imageJson.getString("framePalette").toLowerCase());//To get the palette from the json
                    }
                    if (imageJson.has("rotation")) {
                        try {
                            Integer rot = imageJson.isNull("rotation") ? 0 : imageJson.getInt("rotation");
                            gbcImage.setRotation(rot);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (imageJson.has("frame")) {
                        String frameId = imageJson.getString("frame");

                        gbcImage.setFrameId(frameId.toLowerCase());
                        if (frameId.equals("null")) {
                            gbcImage.setFrameId(null);
                        } else {
                            gbcImage.setFrameId(frameId.toLowerCase());
                            if (!Utils.hashFrames.containsKey(frameId)) {
                                gbcImage.setFrameId(StaticValues.defaultFrameId);
                            }
                        }
                    }

                    if (imageJson.has("lockFrame"))
                        gbcImage.setLockFrame((Boolean) imageJson.get("lockFrame"));
                    else gbcImage.setLockFrame(false);

                    if (imageJson.has("invertPalette"))
                        gbcImage.setInvertPalette((Boolean) imageJson.get("invertPalette"));
                    else gbcImage.setInvertPalette(false);

                    if (imageJson.has("invertFramePalette"))
                        gbcImage.setInvertFramePalette((Boolean) imageJson.get("invertFramePalette"));
                    else gbcImage.setInvertFramePalette(false);

                    //To set the Date
                    String dateFormat = "yyyy-MM-dd HH:mm:ss:SSS";
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    try {
                        Date creationDate = sdf.parse((String) imageJson.get("created"));
                        gbcImage.setCreationDate(creationDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Bitmap imageBitmap = frameChange(gbcImage, gbcImage.getFrameId(), gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), null);

                    ImportFragment.importedImagesList.add(gbcImage);
                    ImportFragment.importedImagesBitmaps.add(imageBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
        return finalValues;
    }

    public static List<GbcPalette> readerPalettes(JSONArray palettesArr) {
        List<GbcPalette> paletteList = new ArrayList<>();
        for (int i = 0; i < palettesArr.length(); i++) {
            JSONObject paletteObj;
            try {
                GbcPalette gbcPalette = new GbcPalette();
                paletteObj = palettesArr.getJSONObject(i);
                String paletteId = paletteObj.getString("shortName");
                String paletteName = paletteObj.getString("name");
                if (paletteObj.has("favorite")){
                    boolean isFavorite = paletteObj.getBoolean("favorite");
                    gbcPalette.setFavorite(isFavorite);
                }
                JSONArray paletteArr = paletteObj.getJSONArray("palette");
                int[] paletteIntArray = new int[paletteArr.length()];
                for (int j = 0; j < paletteArr.length(); j++) {
                    String color = paletteArr.getString(j);
                    paletteIntArray[j] = Color.parseColor(color);
                }
                gbcPalette.setPaletteId(paletteId);
                gbcPalette.setPaletteName(paletteName);
                gbcPalette.setPaletteColors(paletteIntArray);
                paletteList.add(gbcPalette);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.PALETTES;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return paletteList;
    }

    public static List<GbcFrame> readerFrames(JSONObject jsonObject, JSONObject stateObject) throws JSONException {
        if (frameJsonType(stateObject)) {
            return readerFramesGBCAM(jsonObject, stateObject);
        } else {
            return readerFramesWebApp(jsonObject, stateObject);
        }
    }

    /**
     * Checks if the json was created in this app or not
     *
     * @param stateObject
     * @return
     * @throws JSONException
     */
    public static boolean frameJsonType(JSONObject stateObject) throws JSONException {
        JSONArray framesArray = stateObject.getJSONArray("frames");
        JSONObject frameObj = framesArray.getJSONObject(0);
        if (frameObj.has("isWildFrame")) {
            return true;
        } else return false;
    }

    /**
     * To extract the frames from a json created with this app
     *
     * @param stateObject
     * @return
     * @throws JSONException
     */
    public static List<GbcFrame> readerFramesGBCAM(JSONObject jsonObject, JSONObject stateObject) throws JSONException {
        //Entering frame json. There are 2 types, old with id, new with hash
        JSONArray framesArray = stateObject.getJSONArray("frames");
        if (framesArray.length() == 0) {
            return null;
        }
        List<GbcFrame> frameList = new ArrayList<>();

        for (int i = 0; i < framesArray.length(); i++) {
            JSONObject frameObj = null;
            try {
                frameObj = framesArray.getJSONObject(i);
                //Verify what type of frames.json it is
                String frameId = frameObj.getString("id");
                GbcFrame gbcFrame = new GbcFrame();
                gbcFrame.setFrameId(frameId);
                String frameName = frameObj.getString("name");
                gbcFrame.setFrameName(frameName);
                boolean isWildFrame = frameObj.getBoolean("isWildFrame");
                String frameHash = frameObj.getString("hash");
                gbcFrame.setFrameHash(frameHash);
                gbcFrame.setWildFrame(isWildFrame);
                String hash = jsonObject.getString("frame-" + frameHash);
                String decompHash = decodeDataImage(hash);//Change this for a full image
                byte[] bytes = Utils.convertToByteArray(decompHash);
                int height = (decompHash.length() + 1) / 120;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height);
                Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get("bw").getPaletteColorsInt(), bytes, false);//False for now, need to add the wild frame to the json

                gbcFrame.setFrameBitmap(image);
                gbcFrame.setFrameBytes(bytes);

                //Recovering the transparency data
                String hashTransp = jsonObject.getString("frame-transparency-" + frameHash);

                String data = decodeDataTransparency(hashTransp);
                HashSet<int[]> hashSet = stringToHashSet(data);
                gbcFrame.setTransparentPixelPositions(hashSet);

                image = transparentBitmap(image, gbcFrame);
                gbcFrame.setFrameBitmap(image);

                frameList.add(gbcFrame);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.FRAMES;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            JSONArray framesGroupNamesArray = stateObject.getJSONArray("frameGroupNames");
            for (int i = 0; i < framesGroupNamesArray.length(); i++) {
                JSONObject frameNameObj = framesGroupNamesArray.getJSONObject(i);
                String frameGroupId = frameNameObj.getString("id");
                String frameGroupName = frameNameObj.getString("name");
                importedFrameGroupIdNames.put(frameGroupId, frameGroupName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return frameList;
    }


    public static List<GbcFrame> readerFramesWebApp(JSONObject jsonObject, JSONObject stateObject) throws
            JSONException {
        //Entering frame json. There are 2 types, old with id, new with hash
        JSONArray framesArray = stateObject.getJSONArray("frames");
        if (framesArray.length() == 0) {
            return null;
        }
        List<GbcFrame> frameList = new ArrayList<>();
        LinkedHashSet<String> newGroupIds = new LinkedHashSet<>();//To add the group id of each image, in case there is no group name for it

        for (int i = 0; i < framesArray.length(); i++) {
            JSONObject frameObj = null;
            try {
                frameObj = framesArray.getJSONObject(i);
                //Verify what type of frames.json it is
                String name;
                String id;
                if (frameObj.has("id") && frameObj.has("name") && !frameObj.has("hash") && !frameObj.has("tempId")) {
                    //Old type
                    name = frameObj.getString("name");
                    id = frameObj.getString("id");

                } else if (frameObj.has("id") && frameObj.has("name") && frameObj.has("hash")) {//tempId sometimes is not present
                    //New type with hash
                    name = frameObj.getString("name");
                    id = frameObj.getString("id");
                } else {
                    return null;
                }

                //If an id is not valid I take the json is not valid
                if (!checkValidId(id)) {
                    return null;
                }
                Pattern pattern = Pattern.compile("^(\\D+)(\\d+)$");//Getting only the chars for the group id
                Matcher matcher = pattern.matcher(id);

                String groupId;
                if (matcher.matches()) {
                    groupId = matcher.group(1);
                    newGroupIds.add(groupId);
                } else {
                    return null;
                }

                GbcFrame gbcFrame = new GbcFrame();

                gbcFrame.setFrameName(name);
                gbcFrame.setFrameId(id);

                boolean hasHash = false;
                String frameHash = "";
                if (frameObj.has("hash")) {
                    frameHash = frameObj.getString("hash");
                    gbcFrame.setFrameHash(frameHash);
                    hasHash = true;
                }

                String data = "";
                if (hasHash) {
                    data = jsonObject.getString("frame-" + frameHash);
                } else {
                    data = jsonObject.getString("frame-" + id);
                }
                String decompHash = recreateWebAppFrame(data);
                byte[] bytes = Utils.convertToByteArray(decompHash);
                int height = (decompHash.length() + 1) / 120;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height);
                Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get("bw").getPaletteColorsInt(), bytes, false);//False for now, need to add the wild frame to the json
                gbcFrame.setFrameBitmap(image);
                gbcFrame.setFrameBytes(bytes);
                Bitmap bitmap = transparentBitmap(image, gbcFrame);
                gbcFrame.setFrameBitmap(bitmap);
                frameList.add(gbcFrame);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.FRAMES;
                if (!hasHash) {
                    //If json has not hash, generate it
                    try {
                        byte[] gbFrameBytes = Utils.encodeImage(image, "bw");
                        gbcFrame.setFrameBytes(gbFrameBytes);
                        String gbFrameHash = generateHashFromBytes(gbFrameBytes);
                        gbcFrame.setFrameHash(gbFrameHash);
                        frameHash = gbFrameHash;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (frameHash.isEmpty()) return null;

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (stateObject.has("frameGroupNames")) {
                JSONArray framesGroupNamesArray = stateObject.getJSONArray("frameGroupNames");
                for (int i = 0; i < framesGroupNamesArray.length(); i++) {
                    JSONObject frameNameObj = framesGroupNamesArray.getJSONObject(i);
                    String frameGroupId = frameNameObj.getString("id");
                    String frameGroupName = frameNameObj.getString("name");
                    importedFrameGroupIdNames.put(frameGroupId, frameGroupName);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //Now add the frames from the newGroupIds in case they are not in the frameGroupNames
        for (String st : newGroupIds) {
            if (!importedFrameGroupIdNames.containsKey(st)) {
                String groupName = "";
                if (st.equals("int")) {
                    groupName = INTERNATIONAL_FRAMES;
                } else if (st.equals("jp")) {
                    groupName = JAPANESE_FRAMES;
                } else if (st.equals("hk")) {
                    groupName = HELLO_KITTY_FRAMES;
                }
                importedFrameGroupIdNames.put(st, groupName);
            }
        }
        return frameList;
    }

    private static boolean checkValidId(String frameId) {

        //Pattern for lowercase chars with 2 numbers at the end
        String idPattern = "^[a-z]+\\d{2}$";

        Pattern pattern = Pattern.compile(idPattern);

        Matcher matcher = pattern.matcher(frameId);

        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static HashSet<int[]> stringToHashSet(String data) {
        Type type = new TypeToken<HashSet<int[]>>() {
        }.getType();
        return new Gson().fromJson(data, type);
    }

    public static String decodeDataImage(String value) {
        byte[] compressedBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] outputBytes = new byte[compressedBytes.length * 300];
        int length = 0;
        try {
            length = inflater.inflate(outputBytes);
            inflater.end();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputString.length(); i += 2) {
            try {
                sb.append(outputString.substring(i, i + 2));
                sb.append(" ");
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }

    //The web app frame json only has the actual frame, top, bottom and sides, so I create a String of the hex data of an image with the frame filled with white color
    public static String recreateWebAppFrame(String value) {
        byte[] compressedBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] outputBytes = new byte[compressedBytes.length * 300];//*300 to make sure the array is big enough
        int length = 0;
        try {
            length = inflater.inflate(outputBytes);
            inflater.end();
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < outputString.length(); i += 2) {
            try {
                sb.append(outputString.substring(i, i + 2));
                sb.append(" ");
            } catch (Exception e) {
            }
        }
        String firstPart = outputString.substring(0, 1280);
        String central = outputString.substring(1280, 3072);//Info of the frame sides. Split it in 14 parts, 128 characters each
        String[] parts = new String[14];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(firstPart);
        for (int i = 0; i < 14; i++) {
            String part = central.substring(i * 128, (i + 1) * 128);  //Obtain the 128 characters part
            StringBuilder builder = new StringBuilder(part);
            for (int j = 0; j < 512; j++) {
                builder.insert(64, '0');  //Insert '0' 512 times, for 16 tiles of white, starting in position 64 (after second frame tile)
            }
            parts[i] = builder.toString();  // Save the part with the added '0' into the array
            stringBuilder.append(parts[i]);
        }
        String lastPart = outputString.substring(outputString.length() - 1280);
        stringBuilder.append(lastPart);

        String finished = stringBuilder.toString();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < finished.length(); i += 2) {
            if (i > 0) {
                result.append(" "); // Add a space each 2 characters
            }
            result.append(finished.substring(i, i + 2));
        }
        return result.toString();
    }

    public static String decodeDataTransparency(String compressedString) {
        byte[] compressedBytes = compressedString.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        } finally {
            inflater.end();
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

}

package com.mraulio.gbcameramanager.ui.importFile;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;
import static com.mraulio.gbcameramanager.utils.Utils.generateDefaultTransparentPixelPositions;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.transparencyHashSet;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class JsonReader {

    public static List<?> jsonCheck(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            if (!jsonObject.has("state")) {
                System.out.println("Not a valid json: 'state'.");
                return null;
            }
            JSONObject stateObject = jsonObject.getJSONObject("state");

            if (stateObject.has("palettes")) {
                JSONArray palettesArray = stateObject.getJSONArray("palettes");
                if (palettesArray.length() == 0) {
                    System.out.println("No palettes.");
                    return null;
                }
                JSONObject paletteObject = palettesArray.getJSONObject(0);

                // Verify that the keys and values exist in the palette
                if (paletteObject.has("shortName") && paletteObject.has("name") && paletteObject.has("palette") && paletteObject.has("origin")) {
                    JSONArray paletteArray = paletteObject.getJSONArray("palette");

                    // Verificar que la matriz de paletas tenga cuatro elementos
                    if (paletteArray.length() == 4) {
                        System.out.println("JSON has the expected format.");
                    } else {
                        System.out.println("Not a 4 element palette.");
                        return null;
                    }
                    return readerPalettes(palettesArray);
                } else {
                    System.out.println("The json doesn't have expected keys.");
                }

            } else if (stateObject.has("frames")) {
                return readerFrames(jsonObject,stateObject);

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

        // Acces JSON values
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
//                    if (!imageJson.getString("title").equals("")) {
                    gbcImage.setName(imageJson.getString("title"));
//                    } else gbcImage.setName("*No title*");
                    JSONArray tagsArray = imageJson.getJSONArray("tags");
                    if (tagsArray.length() > 0) {
                        List<String> tagsStrings = new ArrayList<>();
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
                            gbcImage.setPaletteId(imageJson.getString("palette"));//To get the palette from the json
                    }

                    if (imageJson.has("framePalette")) {
                        if (!Utils.hashPalettes.containsKey(imageJson.getString("framePalette"))) {
                            gbcImage.setFramePaletteId("bw");
                        } else
                            gbcImage.setFramePaletteId(imageJson.getString("framePalette"));//To get the palette from the json
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
                        String frameName = imageJson.getString("frame");
                        if (frameName.equals("null"))
                            gbcImage.setFrameId("");
                        else gbcImage.setFrameId(frameName);
                        if (!Utils.hashFrames.containsKey(gbcImage.getFrameId())) {
                            gbcImage.setFrameId("Nintendo_Frame");
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
                paletteObj = palettesArr.getJSONObject(i);
                String shortName = paletteObj.getString("shortName");
                JSONArray paletteArr = paletteObj.getJSONArray("palette");
                int[] paletteIntArray = new int[paletteArr.length()];
                for (int j = 0; j < paletteArr.length(); j++) {
                    String color = paletteArr.getString(j);
                    paletteIntArray[j] = Color.parseColor(color);
                }
                GbcPalette gbcPalette = new GbcPalette();
                gbcPalette.setPaletteId(shortName);
                gbcPalette.setPaletteColors(paletteIntArray);
                paletteList.add(gbcPalette);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.PALETTES;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return paletteList;
    }

    public static List<GbcFrame> readerFrames(JSONObject jsonObject,JSONObject stateObject) throws JSONException {
        if (frameJsonType(stateObject)) {
            return readerFramesGBCAM(jsonObject,stateObject);
        } else {
            return readerFramesWebApp(jsonObject,stateObject);
        }
    }

    /**
     * Checks if the json was created in this app or not
     * @param stateObject
     * @return
     * @throws JSONException
     */
    public static boolean frameJsonType(JSONObject stateObject) throws JSONException {
        JSONArray framesArray = stateObject.getJSONArray("frames");
        JSONObject frameObj = framesArray.getJSONObject(0);
        if (frameObj.has("isWildFrame")){
            return true;
        }else return false;
    }

    /**
     * To extract the frames from a json created with this app
     * @param stateObject
     * @return
     * @throws JSONException
     */
    public static List<GbcFrame> readerFramesGBCAM(JSONObject jsonObject,JSONObject stateObject) throws JSONException {
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
                String name = frameObj.getString("id");
                GbcFrame gbcFrame = new GbcFrame();
                gbcFrame.setFrameName(name);
                boolean isWildFrame = frameObj.getBoolean("isWildFrame");
                gbcFrame.setWildFrame(isWildFrame);
                String hash = jsonObject.getString("frame-" + name);
                String decompHash = decodeDataImage(hash);//Change this for a full image
                byte[] bytes = Utils.convertToByteArray(decompHash);
                int height = (decompHash.length() + 1) / 120;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height, false);
                Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get("bw").getPaletteColorsInt(), Utils.hashPalettes.get("bw").getPaletteColorsInt(), bytes, false, false, isWildFrame);//False for now, need to add the wild frame to the json

                gbcFrame.setFrameBitmap(image);
                gbcFrame.setFrameBytes(bytes);

                //Recovering the transparency data
                String hashTransp = jsonObject.getString("frame-transparency-" + name);

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
        return frameList;

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
    public static List<GbcFrame> readerFramesWebApp(JSONObject jsonObject,JSONObject stateObject) throws
            JSONException {
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
                String name;
                String id;
                if (frameObj.has("id") && frameObj.has("name") && !frameObj.has("hash") && !frameObj.has("tempId")) {
                    //Old type
                    name = frameObj.getString("id");
                    id = frameObj.getString("id");

                } else if (frameObj.has("id") && frameObj.has("name") && frameObj.has("hash")) {//tempId sometimes is not present
                    //New type with hash
                    name = frameObj.getString("id");
                    id = frameObj.getString("hash");
                } else {
                    return null;
                }
                GbcFrame gbcFrame = new GbcFrame();
                gbcFrame.setFrameName(name);
                String data = jsonObject.getString("frame-" + id);
                String decompHash = recreateFrame(data);
                byte[] bytes = Utils.convertToByteArray(decompHash);
                int height = (decompHash.length() + 1) / 120;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(160, height, false);
                Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get("bw").getPaletteColorsInt(), Utils.hashPalettes.get("bw").getPaletteColorsInt(), bytes, false, false, false);//False for now, need to add the wild frame to the json
                gbcFrame.setFrameBitmap(image);
                gbcFrame.setFrameBytes(bytes);
                HashSet<int[]> transparencyHS = transparencyHashSet(gbcFrame.getFrameBitmap());
                if (transparencyHS.size() == 0) {
                    transparencyHS = generateDefaultTransparentPixelPositions(gbcFrame.getFrameBitmap());
                }
                gbcFrame.setTransparentPixelPositions(transparencyHS);
                frameList.add(gbcFrame);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.FRAMES;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return frameList;
    }

    public static HashSet<int[]> stringToHashSet(String data) {
        HashSet<int[]> hashSet = new HashSet<>();
        //Delete start and end brackets
        data = data.substring(1, data.length() - 1);
        String[] arrays = data.split("\\],\\[");
        for (String arrayStr : arrays) {
            String[] nums = arrayStr.split(",");
            int[] array = new int[nums.length];
            for (int i = 0; i < nums.length; i++) {
                array[i] = Integer.parseInt(nums[i].trim());
            }
            hashSet.add(array);
        }
        return hashSet;
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

    //The frame json only has the actual frame, top, bottom and sides, so I create a String of the hex data of an image with the frame filled with white color
    public static String recreateFrame(String value) {
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

}

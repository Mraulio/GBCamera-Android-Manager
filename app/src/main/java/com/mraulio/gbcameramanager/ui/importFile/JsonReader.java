package com.mraulio.gbcameramanager.ui.importFile;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
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

        // Acces JSON values
        JSONArray images = jsonObject.getJSONObject("state").getJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject imageJson = images.getJSONObject(i);
            try {
                if (!imageJson.has("isRGBN") || (imageJson.has("isRGBN") && imageJson.get("isRGBN").equals(false))) {
                    String hash = imageJson.getString("hash");
                    stringValues.add(hash);
                    String data = jsonObject.getString(hash);
                    String decodedData = decodeData(data);
                    System.out.println("Decoded " + decodedData.length() + "  " + decodedData);
                    byte[] bytes = Utils.convertToByteArray(decodedData);
                    GbcImage gbcImage = new GbcImage();
                    gbcImage.setHashCode(hash);
                    gbcImage.setImageBytes(bytes);
                    if (!imageJson.getString("title").equals("")) {
                        gbcImage.setName(imageJson.getString("title"));
                    } else gbcImage.setName("*No title*");
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
                    //To set the Date
                    String dateFormat = "yyyy-MM-dd HH:mm:ss:SSS";
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    try {
                        Date creationDate = sdf.parse((String) imageJson.get("created"));
                        gbcImage.setCreationDate(creationDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Bitmap imageBitmap = GalleryFragment.paletteChanger(gbcImage.getPaletteId(), bytes, gbcImage);
                    if (imageBitmap.getHeight() == 144) {
                        imageBitmap = GalleryFragment.frameChange(gbcImage, imageBitmap, gbcImage.getFrameId(), gbcImage.isLockFrame());//Need to change the frame to use the one in the json
                    }
                    ImportFragment.importedImagesList.add(gbcImage);
                    ImportFragment.importedImagesBitmaps.add(imageBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error. Can't be added.\n" + e.toString());//For RGB pics
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

    public static List<GbcFrame> readerFrames(JSONObject jsonObject, JSONObject stateObject) throws
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
                String hash = jsonObject.getString("frame-" + id);
                String decompHash = recreateFrame(hash);
                byte[] bytes = Utils.convertToByteArray(decompHash);
                int height = (decompHash.length() + 1) / 120;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Utils.gbcPalettesList.get(0).getPaletteColorsInt()), 160, height);
                Bitmap image = imageCodec.decodeWithPalette(Utils.gbcPalettesList.get(0).getPaletteColorsInt(), bytes);
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

    public static String decodeData(String value) {
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
        System.out.println(uncompressedData + "  UNCOMPRESSED DATA");
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputString.length(); i += 2) {
            try {
                sb.append(outputString.substring(i, i + 2));
                sb.append(" ");
            } catch (Exception e) {
            }
        }
        System.out.println(sb.toString());
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

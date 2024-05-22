package com.mraulio.gbcameramanager.gameboycameralib.saveExtractor;

import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.*;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.paletteChanger;
import static com.mraulio.gbcameramanager.utils.StaticValues.alwaysDefaultFrame;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;


import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.FileMetaParser;
import com.mraulio.gbcameramanager.utils.HomebrewRomsMetaParser;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modified from https://github.com/KodeMunkie/gameboycameralib
 */
public class SaveImageExtractor implements Extractor {

    private static final String PNG_FORMAT = "png";
    private static final int EMPTY_IMAGE_CHECKSUM = 0;
    private final ImageCodec imageCodec;
    private final ImageCodec smallImageCodec;

    public SaveImageExtractor(IndexedPalette palette) {
        this.imageCodec = new ImageCodec(IMAGE_WIDTH, IMAGE_HEIGHT);
        this.smallImageCodec = new ImageCodec(SMALL_IMAGE_WIDTH, SMALL_IMAGE_HEIGHT);
    }

    @Override
    public List<Bitmap> extract(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return extract(Files.readAllBytes(file.toPath()));//Modified
        } else {
            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fis.read(fileBytes);
            fis.close();
            return extract(fileBytes);
        }
    }

    @Override
    public List<Bitmap> extract(byte[] rawData) {
        List<Bitmap> images = new ArrayList<>(30);
        try {
            for (int i = IMAGE_START_LOCATION; i < rawData.length; i += NEXT_IMAGE_START_OFFSET) {

                // The full size images
                byte[] image = new byte[IMAGE_LENGTH];
                System.arraycopy(rawData, i, image, 0, IMAGE_LENGTH);
                if (i != 0 && isEmptyImage(image)) {
                    continue;
                }
                images.add(imageCodec.decode(image));
            }

        } catch (Exception e) {
            // Just print the error and continue to return what images we have
            e.printStackTrace();
        }
        return images;
    }

    @Override
    public List<byte[]> extractBytes(byte[] rawData, int saveBank) {
        return null;
    }


    @Override
    public List<byte[]> extractBytes(File file, int saveBank) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return extractBytes(Files.readAllBytes(file.toPath()), saveBank);//Modified
        } else {
            FileInputStream fis = new FileInputStream(file);
            byte[] fileBytes = new byte[(int) file.length()];
            fis.read(fileBytes);
            fis.close();
            return extractBytes(fileBytes, saveBank);
        }
    }

    private GbcImage getGbcImage(GbcImage gbcImage, byte[] imageBytes, String gbcImageName, HashMap<GbcImage, Bitmap> hashImageBitmap, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        try {
            gbcImage.setName(gbcImageName);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
            String hashHex = Utils.bytesToHex(hash);
            gbcImage.setHashCode(hashHex);
            Bitmap imageBitmap = gbcImageBitmap(gbcImage, imageBytes, saveTypeIntJpHk);
            hashImageBitmap.put(gbcImage, imageBitmap);
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        return gbcImage;
    }

    private Bitmap gbcImageBitmap(GbcImage gbcImage, byte[] imageBytes, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        try {
            ImageCodec imageCodec = new ImageCodec(128, 112);
            Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get("bw").getPaletteColorsInt(), imageBytes, false);
            if (image.getHeight() == 112 && image.getWidth() == 128) {
                //Get the frame id, according to the frame index coded in the bytes, if it exists in the app
                LinkedHashMap metadata = gbcImage.getImageMetadata();
                int frameNumber = 0;
                String frameId = StaticValues.defaultFrameId;
                if (metadata != null) {
                    Object frameNumberObj = gbcImage.getImageMetadata().get("frameIndex");
                    if (frameNumberObj != null) {
                        frameNumber = Integer.parseInt((String) frameNumberObj) + 1; //+1 Because the Ids begin with 1 and not 0
                    }
                }

                if (!alwaysDefaultFrame) {
                    String jpId = "jp";
                    String intId = "int";
                    String hkId = "hk";

                    switch (saveTypeIntJpHk) {
                        case JP:
                            if (frameGroupsNames.containsKey(jpId)) {
                                //Use the 4 different jp frames, 01, 02, 07 and 09
                                //Rest of the frames are from the int group, if it exists
                                if (frameNumber == 1 || frameNumber == 2 || frameNumber == 7 || frameNumber == 9) {
                                    frameId = jpId + String.format("%02d", frameNumber);
                                } else {
                                    frameId = intId + String.format("%02d", frameNumber);
                                }
                                if (!hashFrames.containsKey(frameId)) {
                                    frameId = StaticValues.defaultFrameId;//If the group exists but the frame doesn't
                                }
                            }
                            break;
                        case INT:
                            //Use the int frame group
                            if (frameGroupsNames.containsKey(intId)) {
                                frameId = intId + String.format("%02d", frameNumber);
                            }
                            if (!hashFrames.containsKey(frameId)) {
                                frameId = StaticValues.defaultFrameId;//If the group exists but the frame doesn't
                            }
                            break;
                        case HK:
                            if (frameGroupsNames.containsKey(hkId)) {

                                frameId = hkId + String.format("%02d", frameNumber);
                                if (!hashFrames.containsKey(frameId)) {
                                    frameId = StaticValues.defaultFrameId;//If the group exists but the frame doesn't
                                }
                            }
                            break;
                    }
                }
                gbcImage.setFrameId(frameId);
                GbcFrame gbcFrame = hashFrames.get(frameId);
                //I need to use copy because if not it's inmutable bitmap
                Bitmap framed = gbcFrame.getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(framed);
                canvas.drawBitmap(image, 16, gbcFrame.isWildFrame() ? 40 : 16, null);
                image = framed;
                imageBytes = Utils.encodeImage(image, "bw");
                image = paletteChanger(gbcImage.getPaletteId(), imageBytes, gbcImage.isInvertPalette());
            }
            gbcImage.setImageBytes(imageBytes);
            return image;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Added by Mraulio
    @Override
    public LinkedHashMap<GbcImage, Bitmap> extractGbcImages(byte[] rawData, String fileName, int saveBank, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        LinkedHashMap<GbcImage, Bitmap> allImagesGB = new LinkedHashMap<>(31);
        LinkedHashMap<GbcImage, Bitmap> deletedImagesGB = new LinkedHashMap<>();
        LinkedHashMap<GbcImage, Bitmap> lastSeenImageGB = new LinkedHashMap<>();

        HashMap<GbcImage, Bitmap> hashImageBitmap = new HashMap<>();

        final int PHOTOS_LOCATION = 0x11B2;
        final int PHOTOS_READ_COUNT = 0x1E;
        StaticValues.deletedCount[saveBank] = 0;
        byte[] photosPositions = new byte[PHOTOS_READ_COUNT];
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        bais.skip(PHOTOS_LOCATION);
        bais.read(photosPositions, 0, PHOTOS_READ_COUNT);

        //For the development rom, in which the vector doesn't exist and has repeated bytes.
        photosPositions = checkDuplicates(photosPositions);
        List<byte[]> deletedImages = new ArrayList<>();
        int activePhotos = 0;
        StringBuilder sb = new StringBuilder();
        for (byte x : photosPositions) {
            sb.append(String.format("%02x", x));
            sb.append(" ");
            if (x != (byte) 0xFF) {
                activePhotos++;
            }
        }
        sb.setLength(0);
        int newSize = 0;
        int index = 0;
        for (byte b : photosPositions) {
            if (b != (byte) 0xFF) {
                newSize++;
            }
        }
        byte[] noFFarray = new byte[newSize];

        for (byte b : photosPositions) {
            if (b != (byte) 0xFF) {
                sb.append(String.format("%02x", b));
                sb.append(" ");
                noFFarray[index] = b;
                index++;
            }
        }
        ArrayList<GbcImage> allImages = new ArrayList<>(31);//31 to get the last seen, which will be the first at i = 0

        for (int i = 0; i < activePhotos; i++) {
            allImages.add(null);//Fill it with null so I can later use the "put" method
        }
        //Sort the noFFarray
        Arrays.sort(noFFarray);
        sb.setLength(0);
        for (byte b : noFFarray) {
            if (b != (byte) 0xFF) {
                sb.append(String.format("%02x", b));
                sb.append(" ");
            }
        }
        try {
            int j = 0;
            for (int i = 0; i < rawData.length; i += NEXT_IMAGE_START_OFFSET) {//i=0 to get the last seen. Next image will be at IMAGE_START_LOCATION
                // The full size images
                byte[] image = new byte[IMAGE_LENGTH];
                System.arraycopy(rawData, i, image, 0, IMAGE_LENGTH);

                byte[] thumbImage = new byte[SMALL_IMAGE_LENGTH];
                System.arraycopy(rawData, i + SMALL_IMAGE_START_OFFSET, thumbImage, 0, SMALL_IMAGE_LENGTH);

                byte[] imageMetadataBytes = new byte[IMAGE_METADATA_LENGTH];
                System.arraycopy(rawData, i + IMAGE_METADATA_OFFSET, imageMetadataBytes, 0, IMAGE_METADATA_LENGTH);

                //The last seen image
                if (i == 0) {
                    i = NEXT_IMAGE_START_OFFSET;//0 means it's the last seen, then we need to continue on 0x2000(2 * NEXT_IMAGE_START_OFFSET)
                    String lastSeenGbcImageName = fileName + " [last seen]";
                    GbcImage lastSeenGbcImage = new GbcImage();
                    lastSeenGbcImage = getGbcImage(lastSeenGbcImage, image, lastSeenGbcImageName, hashImageBitmap, saveTypeIntJpHk);
                    Bitmap lastSeenBitmap = gbcImageBitmap(lastSeenGbcImage, image, saveTypeIntJpHk);
                    lastSeenImageGB.put(lastSeenGbcImage, lastSeenBitmap);

                } else {
                    //If it's a deleted photo
                    if (photosPositions[j] == (byte) 0xFF) {
                        if (!isEmptyImage(image)) {//In case the image is not FF, because of the isEmptyImage method
                            deletedImages.add(image);//Can't order it, all are -1 (0xFF)
                            String deletedGbcImageName = fileName + " [deleted]";
                            GbcImage deletedGbcImage = new GbcImage();
                            deletedGbcImage = getGbcImage(deletedGbcImage, image, deletedGbcImageName, hashImageBitmap, saveTypeIntJpHk);


                            deletedGbcImage.setImageMetadata(getMetadata(imageMetadataBytes, thumbImage, saveTypeIntJpHk));
                            Bitmap deletedBitmap = gbcImageBitmap(deletedGbcImage, image, saveTypeIntJpHk);
                            deletedImagesGB.put(deletedGbcImage, deletedBitmap);
                            StaticValues.deletedCount[saveBank]++;
                        }
                    } else {//If not a deleted photo
                        //I get the index in the sorted array where the "real" index from the vector is stored
                        for (int b = 0; b < noFFarray.length; b++) {
                            if (noFFarray[b] == photosPositions[j]) {
                                String formattedIndex = String.format("%02d", (b + 1));
                                String gbcImageName = fileName + " " + formattedIndex;
                                GbcImage gbcImage = new GbcImage();

                                gbcImage.setImageMetadata(getMetadata(imageMetadataBytes, thumbImage, saveTypeIntJpHk));
                                gbcImage = getGbcImage(gbcImage, image, gbcImageName, hashImageBitmap, saveTypeIntJpHk);

                                allImages.set(b, gbcImage);
                            }
                        }
                    }
                    j++;
                }
            }

            //To add them in order
            for (GbcImage gbcImage : allImages) {
                allImagesGB.put(gbcImage, hashImageBitmap.get(gbcImage));
            }

            //Append the last seen image after the active images
            allImagesGB.putAll(lastSeenImageGB);
            //Append the deleted images at the end
            allImagesGB.putAll(deletedImagesGB);

            //To update the images creation date, now that they are in order, so they show properly when sorting by creation date
            long timeMs = System.currentTimeMillis();
            long plusMs = 0;
            for (GbcImage gbcImage : allImagesGB.keySet()) {
                gbcImage.setCreationDate(new Date(timeMs + plusMs++));
            }
        } catch (Exception e) {
            // Just print the error and continue to return what images we have
            e.printStackTrace();
        }
        return allImagesGB;
    }

    private LinkedHashMap<String, String> getMetadata(byte[] imageMetadata, byte[] thumbImage, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        FileMetaParser fileMetaParser = new FileMetaParser();
        LinkedHashMap<String, String> metadataOriginal;
        metadataOriginal = fileMetaParser.getFileMeta(imageMetadata, saveTypeIntJpHk);

        LinkedHashMap<String, String> metadataHomebrew;
        HomebrewRomsMetaParser homebrewRomsMetaParser = new HomebrewRomsMetaParser();
        metadataHomebrew = homebrewRomsMetaParser.parseHomebrewRomMetadata(thumbImage);

        LinkedHashMap<String, String> allMetadatas = new LinkedHashMap<>();
        for (LinkedHashMap.Entry<String, String> entry : metadataOriginal.entrySet()) {
            allMetadatas.put(entry.getKey(), entry.getValue());
        }
        for (LinkedHashMap.Entry<String, String> entry : metadataHomebrew.entrySet()) {
            allMetadatas.put(entry.getKey(), entry.getValue());
        }

        return allMetadatas;
    }

    private byte[] checkDuplicates(byte[] array) {
        byte[] basicArray = new byte[30];
        Set<Byte> uniqueBytes = new HashSet<>();
        boolean hasDuplicates = false;
        for (byte b : array) {
            //If it is repeated or it's not FF like in the dev rom
            if (uniqueBytes.contains(b) && (int) b != -1) {
                hasDuplicates = true;
                break;
            } else {
                uniqueBytes.add(b);
            }
        }
        if (hasDuplicates) {
            for (int i = 0; i < basicArray.length; i++) {
                basicArray[i] = (byte) i;
            }
            array = basicArray;
        }
        return array;
    }

    @Override
    public List<byte[]> extractAsPng(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return extract(file).stream().map(this::imageToBytes).collect(Collectors.toList());
        } else return null;
    }

    @Override
    public List<byte[]> extractAsPng(byte[] rawData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return extract(rawData).stream().map(this::imageToBytes).collect(Collectors.toList());
        } else return null;

    }

    //Checks if an image is all the same color. Should change it so it checks if it's all white,
    //as images from an erased camera will be all 00
    private boolean isEmptyImage(byte[] bytes) {
        int checksum = 0;
        for (byte b : bytes) {
            checksum ^= b;
        }
        return (byte) checksum == EMPTY_IMAGE_CHECKSUM;
    }

    private byte[] imageToBytes(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}

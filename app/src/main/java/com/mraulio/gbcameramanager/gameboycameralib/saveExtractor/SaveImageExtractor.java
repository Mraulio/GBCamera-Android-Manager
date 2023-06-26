package com.mraulio.gbcameramanager.gameboycameralib.saveExtractor;


import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.*;

import android.graphics.Bitmap;


import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SaveImageExtractor implements Extractor {

    private static final String PNG_FORMAT = "png";
    private static final int EMPTY_IMAGE_CHECKSUM = 0;
    private final ImageCodec imageCodec;
    private final ImageCodec smallImageCodec;

    public SaveImageExtractor(IndexedPalette palette) {
        this.imageCodec = new ImageCodec(palette, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.smallImageCodec = new ImageCodec(palette, SMALL_IMAGE_WIDTH, SMALL_IMAGE_HEIGHT);
    }

    @Override
    public List<Bitmap> extract(File file) throws IOException {
        return extract(Files.readAllBytes(file.toPath()));//Modificado
    }

    @Override
    public List<Bitmap> extract(byte[] rawData) {
        List<Bitmap> images = new ArrayList<>(30);//31 to get the last seen, but needs tweak
        try {
            for (int i = IMAGE_START_LOCATION; i < rawData.length; i += NEXT_IMAGE_START_OFFSET) {

                // The full size images
                byte[] image = new byte[IMAGE_LENGTH];
                System.arraycopy(rawData, i, image, 0, IMAGE_LENGTH);
                if (i != 0 && isEmptyImage(image)) {//FOR THE DELETED IMAGES, CHECK THIS
                    continue;
                }
//                if (i == 0){
//                    i += NEXT_IMAGE_START_OFFSET;//0 means it's the last seen, then we need to continue on 0x2000
//                }
                images.add(imageCodec.decode(image));

                // The thumbs
//                byte[] thumbImage = new byte[SMALL_IMAGE_LENGTH];
//                System.arraycopy(rawData, i + SMALL_IMAGE_START_OFFSET, thumbImage, 0, SMALL_IMAGE_LENGTH);
//                images.add(smallImageCodec.decode(thumbImage));
            }

        } catch (Exception e) {
            // Just print the error and continue to return what images we have
            e.printStackTrace();
        }
        return images;
    }


    @Override
    public List<byte[]> extractBytes(File file) throws IOException {
        return extractBytes(Files.readAllBytes(file.toPath()));//Modificado
    }

    //Added by Mraulio
    @Override
    public List<byte[]> extractBytes(byte[] rawData) {
        final int PHOTOS_LOCATION = 0x11B2;
        final int PHOTOS_READ_COUNT = 0x1E;
        MainActivity.deletedCount = 0;
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
        ArrayList<byte[]> allImages = new ArrayList<>(31);//31 to get the last seen, which will be the first at i = 0
        for (int i = 0; i < activePhotos; i++) {
            allImages.add(null);//Fill it with null so I can later use the "put" method
        }
        byte[] lastSeenImage = new byte[0];

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

                //The last seen image
                if (i == 0) {
                    i = NEXT_IMAGE_START_OFFSET;//0 means it's the last seen, then we need to continue on 0x2000(2 * NEXT_IMAGE_START_OFFSET)
                    lastSeenImage = image;
                } else {
                    //If it's a deleted photo
                    if (photosPositions[j] == (byte) 0xFF) {
                        if (!isEmptyImage(image)) {//In case the image is not FF, because of the isEmptyImage method
                            deletedImages.add(image);//Can't order it, all are -1 (0xFF)
                            MainActivity.deletedCount++;
                        }
                    } else {//If not a deleted photo
                        //I get the index in the sorted array where the "real" index from the vector is stored
                        for (int b = 0; b < noFFarray.length; b++) {
                            if (noFFarray[b] == photosPositions[j]) {
                                allImages.set(b, image);
                            }
                        }
                    }
                    j++;
                }
            }
            //Append the last seen image after the active images
            allImages.add(lastSeenImage);
            //Append the deleted images at the end
            allImages.addAll(deletedImages);

        } catch (Exception e) {
            // Just print the error and continue to return what images we have
            e.printStackTrace();
        }
        return allImages;
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
        return extract(file).stream().map(this::imageToBytes).collect(Collectors.toList());
    }

    @Override
    public List<byte[]> extractAsPng(byte[] rawData) {
        return extract(rawData).stream().map(this::imageToBytes).collect(Collectors.toList());
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

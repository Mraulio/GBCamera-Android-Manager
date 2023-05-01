package com.mraulio.gbcameramanager.gameboycameralib.saveExtractor;


import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.*;

import android.graphics.Bitmap;


import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SaveImageExtractor implements Extractor {

    private static final String PNG_FORMAT = "png";
    private static final int EMPTY_IMAGE_CHECKSUM = 0;
    private final ImageCodec imageCodec;
    private final ImageCodec smallImageCodec;
    public static int deletedCount = 0;

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

    //Added by me
    @Override
    public List<byte[]> extractBytes(byte[] rawData) {
        final int ACTIVE_PHOTOS_LOCATION = 0x11B2;
        final int ACTIVE_PHOTOS_READ_COUNT = 0x1E;
//        final int IMAGE_READ_COUNT = 3584;
        deletedCount = 0;
        byte[] activePhotos = new byte[ACTIVE_PHOTOS_READ_COUNT];
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
        bais.skip(ACTIVE_PHOTOS_LOCATION);
        bais.read(activePhotos, 0, ACTIVE_PHOTOS_READ_COUNT);
        List<byte[]> deletedImages = new ArrayList<>();
        int howManyActivePhotos = 0;
        for (byte x : activePhotos) {
            if (x != (byte) 0xFF) {
                howManyActivePhotos++;
            }
        }
        ArrayList<byte[]> allImages = new ArrayList<>(31);//31 to get the last seen, which will be the first at i = 0
        for (int i = 0; i < howManyActivePhotos; i++) {
            allImages.add(null);//Fill it with null so I can later use the "put" method
        }
        byte[] lastSeenImage = new byte[0];

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
                    if (activePhotos[j] == (byte) 0xFF) {
                        if (!isEmptyImage(image)) {//In case the image is not FF, because of the isEmptyImage method
                            deletedImages.add(image);//Can't order it, all are -1 (0xFF)
                            deletedCount++;
                        }
                    } else {//If not a deleted photo
                        allImages.set(activePhotos[j], image);//To set the image in the position as read in activePhotos, from 0 to 29
                    }
                    j++;
                }

            }
            //Append the deleted images after the active images
            System.out.println(allImages.size() + "////////////////////allImages");

            System.out.println(deletedImages.size() + "////////////////////deleted");
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

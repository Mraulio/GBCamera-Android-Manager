package com.mraulio.gbcameramanager.gameboycameralib.saveExtractor;


import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.*;

import android.graphics.Bitmap;


import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
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
////                if (i!=0 && isEmptyImage(image)) {//FOR THE DELETED IMAGES, CHECK THIS
////                    continue;
////                }
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
        List<byte[]> images = new ArrayList<>(30);//31 to get the last seen, but needs tweak
        try {
            for (int i = IMAGE_START_LOCATION; i < rawData.length; i += NEXT_IMAGE_START_OFFSET) {

                // The full size images
                byte[] image = new byte[IMAGE_LENGTH];
                System.arraycopy(rawData, i, image, 0, IMAGE_LENGTH);
////                if (i!=0 && isEmptyImage(image)) {//FOR THE DELETED IMAGES, CHECK THIS
////                    continue;
////                }
//                if (i == 0){
//                    i += NEXT_IMAGE_START_OFFSET;//0 means it's the last seen, then we need to continue on 0x2000
//                }
                images.add(image);

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
    public List<byte[]> extractAsPng(File file) throws IOException {
        return extract(file).stream().map(this::imageToBytes).collect(Collectors.toList());
    }

    @Override
    public List<byte[]> extractAsPng(byte[] rawData) {
        return extract(rawData).stream().map(this::imageToBytes).collect(Collectors.toList());
    }

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

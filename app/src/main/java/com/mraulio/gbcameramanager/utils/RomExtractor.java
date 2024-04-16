package com.mraulio.gbcameramanager.utils;

import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.magicIsReal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class RomExtractor {

    List<Bitmap> extractedImagesBitmaps = new ArrayList<>();
    List<GbcImage> extractedImagesList = new ArrayList<>();
    List<List<GbcImage>> listActiveImages = new ArrayList<>();
    List<List<GbcImage>> listDeletedImages = new ArrayList<>();
    List<List<Bitmap>> listDeletedBitmaps = new ArrayList<>();
    List<List<Bitmap>> listDeletedBitmapsRedStroke = new ArrayList<>();
    List<GbcImage> finalListImages = new ArrayList<>();
    List<List<Bitmap>> listActiveBitmaps = new ArrayList<>();
    List<Bitmap> finalListBitmaps = new ArrayList<>();
    List<GbcImage> lastSeenImage = new ArrayList<>();
    List<Bitmap> lastSeenBitmap = new ArrayList<>();
    int totalImages;
    byte[] fileBytes;
    List<byte[]> romByteList = new ArrayList<>();
    List<String> filePartNames = new ArrayList<>();
    String fileName;
    Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk;

    public RomExtractor(byte[] fileBytes, String fileName) {
        this.fileBytes = fileBytes;
        this.fileName = fileName;
    }

    public void romExtract(Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        romSplitter();
        readRomSavs(saveTypeIntJpHk);
    }

    public void romSplitter() {
        int fileSize = fileBytes.length;
        int partSize = fileSize / 8; // Divides the file in 8 parts

        for (int i = 0; i < 8; i++) {
            String extension = ".sav";
            if (i != 0) {//Because 0 is the actual rom

                int startIndex = i * partSize;
                int endIndex = (i == 8 - 1) ? fileBytes.length : (i + 1) * partSize;

                byte[] filePartBytes = Arrays.copyOfRange(fileBytes, startIndex, endIndex);

                if (magicIsReal(filePartBytes)) {
                    romByteList.add(filePartBytes);
                    filePartNames.add(fileName + ".part_" + i + extension);
                }
            }
        }
    }

    public void readRomSavs(Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        try {
            for (int i = 0; i < romByteList.size(); i++) {
                readSavBytes(romByteList.get(i), i, filePartNames.get(i), saveTypeIntJpHk);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readSavBytes(byte[] fileBytes, int saveBank, String filePartName, Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        extractedImagesList.clear();
        extractedImagesBitmaps.clear();
        try {
            if (fileBytes.length == 131072) {
                LinkedHashMap<GbcImage, Bitmap> importedImagesHash = extractor.extractGbcImages(fileBytes, filePartName, saveBank, saveTypeIntJpHk);

                for (HashMap.Entry<GbcImage, Bitmap> entry : importedImagesHash.entrySet()) {
                    GbcImage gbcImage = entry.getKey();
                    Bitmap imageBitmap = entry.getValue();
                    ImageData imageData = new ImageData();
                    imageData.setImageId(gbcImage.getHashCode());
                    imageData.setData(gbcImage.getImageBytes());
                    extractedImagesBitmaps.add(imageBitmap);
                    extractedImagesList.add(gbcImage);
                    totalImages++;
                }

                listActiveImages.add(new ArrayList<>(extractedImagesList.subList(0, extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1)));
                listActiveBitmaps.add(new ArrayList<>(extractedImagesBitmaps.subList(0, extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1)));
                lastSeenImage.add(extractedImagesList.get(extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1));
                lastSeenBitmap.add(extractedImagesBitmaps.get(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1));
                listDeletedImages.add(new ArrayList<>(extractedImagesList.subList(extractedImagesList.size() - StaticValues.deletedCount[saveBank], extractedImagesList.size())));

                listDeletedBitmaps.add(new ArrayList<>(extractedImagesBitmaps.subList(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank], extractedImagesBitmaps.size())));

                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(2);
                int startX = 160;
                int startY = 0;
                int endX = 0;
                int endY = 144;
                listDeletedBitmapsRedStroke.add(new ArrayList<>());
                for (Bitmap bitmap : listDeletedBitmaps.get(saveBank)) {
                    Bitmap copiedBitmap = bitmap.copy(bitmap.getConfig(), true);//Need to get a copy of the original bitmap, or else I'll paint on it
                    Canvas canvas = new Canvas(copiedBitmap);
                    canvas.drawLine(startX, startY, endX, endY, paint);
                    listDeletedBitmapsRedStroke.get(saveBank).add(copiedBitmap);
                }
            } else {
//                tv.append(gridView.getContext().getString(R.string.no_good_dump));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public List<byte[]> getRomByteList() {
        return romByteList;
    }

    public void setRomByteList(List<byte[]> romByteList) {
        this.romByteList = romByteList;
    }

    public List<String> getFilePartNames() {
        return filePartNames;
    }

    public void setFilePartNames(List<String> filePartNames) {
        this.filePartNames = filePartNames;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Bitmap> getExtractedImagesBitmaps() {
        return extractedImagesBitmaps;
    }

    public void setExtractedImagesBitmaps(List<Bitmap> extractedImagesBitmaps) {
        this.extractedImagesBitmaps = extractedImagesBitmaps;
    }

    public List<GbcImage> getExtractedImagesList() {
        return extractedImagesList;
    }

    public void setExtractedImagesList(List<GbcImage> extractedImagesList) {
        this.extractedImagesList = extractedImagesList;
    }

    public List<List<GbcImage>> getListActiveImages() {
        return listActiveImages;
    }

    public void setListActiveImages(List<List<GbcImage>> listActiveImages) {
        this.listActiveImages = listActiveImages;
    }

    public List<List<GbcImage>> getListDeletedImages() {
        return listDeletedImages;
    }

    public void setListDeletedImages(List<List<GbcImage>> listDeletedImages) {
        this.listDeletedImages = listDeletedImages;
    }

    public List<List<Bitmap>> getListDeletedBitmaps() {
        return listDeletedBitmaps;
    }

    public void setListDeletedBitmaps(List<List<Bitmap>> listDeletedBitmaps) {
        this.listDeletedBitmaps = listDeletedBitmaps;
    }

    public List<List<Bitmap>> getListDeletedBitmapsRedStroke() {
        return listDeletedBitmapsRedStroke;
    }

    public void setListDeletedBitmapsRedStroke(List<List<Bitmap>> listDeletedBitmapsRedStroke) {
        this.listDeletedBitmapsRedStroke = listDeletedBitmapsRedStroke;
    }

    public List<GbcImage> getFinalListImages() {
        return finalListImages;
    }

    public void setFinalListImages(List<GbcImage> finalListImages) {
        this.finalListImages = finalListImages;
    }

    public List<List<Bitmap>> getListActiveBitmaps() {
        return listActiveBitmaps;
    }

    public void setListActiveBitmaps(List<List<Bitmap>> listActiveBitmaps) {
        this.listActiveBitmaps = listActiveBitmaps;
    }

    public List<Bitmap> getFinalListBitmaps() {
        return finalListBitmaps;
    }

    public void setFinalListBitmaps(List<Bitmap> finalListBitmaps) {
        this.finalListBitmaps = finalListBitmaps;
    }

    public List<GbcImage> getLastSeenImage() {
        return lastSeenImage;
    }

    public void setLastSeenImage(List<GbcImage> lastSeenImage) {
        this.lastSeenImage = lastSeenImage;
    }

    public List<Bitmap> getLastSeenBitmap() {
        return lastSeenBitmap;
    }

    public void setLastSeenBitmap(List<Bitmap> lastSeenBitmap) {
        this.lastSeenBitmap = lastSeenBitmap;
    }


}

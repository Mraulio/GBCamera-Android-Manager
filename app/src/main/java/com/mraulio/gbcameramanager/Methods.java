package com.mraulio.gbcameramanager;

import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_HEIGHT;
import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_WIDTH;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.gameboycameralib.codecs.Codec;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Methods {

    public static List<Bitmap> completeImageList = new ArrayList<>();
    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static List<byte[]> listImageBytes = new ArrayList<>();
    public static List<GbcFrame> framesList = new ArrayList<>();

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
    public static void extractSavImages(Context context) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColors()));
        LocalDateTime now = LocalDateTime.now();
        File latestFile = null;
        try {
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File savFile = new File(downloadsDirectory + "/gbc.sav");
            //To extract last dumped file
//            File[] files = folder.listFiles();
//            if (files != null && files.length > 0) {
//                Arrays.sort(files, new Comparator<File>() {
//                    public int compare(File f1, File f2) {
//                        return Long.compare(f2.lastModified(), f1.lastModified());
//                    }
//                });
//                latestFile = files[0];
////                tv.append("\nThe name of the last SAV file is: " + latestFile.getName() + ".\n" +
////                        "Size: " + latestFile.length() / 1024 + "KB");
//            }
//            if (savFile.length() / 1024 == 128) {

            //Extract the images
            listImageBytes = extractor.extractBytes(savFile);
//            imageList = extractor.extract(savFile);

            for (byte[] imageBytes : listImageBytes) {
                GbcImage gbcImage = new GbcImage();
                GbcImage.numImages++;
                gbcImage.setName("Image " + (GbcImage.numImages));
                gbcImage.setFrameIndex(0);
                gbcImage.setPaletteIndex(0);
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 128, 112);
                Bitmap image = imageCodec.decodeWithPalette(gbcImage.getPaletteIndex(), imageBytes);
                if (image.getHeight() == 112 && image.getWidth() == 128) {
                    //I need to use copy because if not it's inmutable bitmap
                    Bitmap framed = framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(framed);
                    canvas.drawBitmap(image, 16, 16, null);
                    image = framed;
                    imageBytes= encodeImage(image, gbcImage);
                }
                gbcImage.setImageBytes(imageBytes);
                completeImageList.add(image);
                gbcImagesList.add(gbcImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static byte[] encodeImage(Bitmap bitmap, GbcImage gbcImage) throws IOException {
        Codec decoder = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, bitmap.getHeight());
        return decoder.encodeInternal(bitmap, gbcImage);
    }
    public static void extractHexImages(){
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //*******PARA LEER EL FICHERO HEXDATA
        File ficheroHex = new File(directory, "pano2cabo.txt");
        StringBuilder stringBuilder = new StringBuilder();

        try {
            FileInputStream inputStream = new FileInputStream(ficheroHex);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            inputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileContent = stringBuilder.toString();

        List<byte[]> listaBytes= new ArrayList<>();
        //******FIN DE LEER EL FICHERO
        List<String> dataList = RawToTileData.separateData(fileContent);
        String data = "";
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            GbcImage.numImages++;
            gbcImage.setImageBytes(bytes);
            gbcImage.setName("Image " + (GbcImage.numImages));
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(gbcImage.getPaletteIndex(), gbcImage.getImageBytes());
            completeImageList.add(image);
            gbcImagesList.add(gbcImage);
        }
    }

    private static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        System.out.println(data.length());
        for (int i = 0; i < byteStrings.length; i++) {
            try{
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));

            }catch (Exception e){
            }
        }
        System.out.println(bytes.length);
        return bytes;
    }

}

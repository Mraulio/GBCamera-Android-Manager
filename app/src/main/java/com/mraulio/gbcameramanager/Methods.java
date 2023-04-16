package com.mraulio.gbcameramanager;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Methods {

    public static List<Bitmap> completeBitmapList = new ArrayList<>();
    public static List<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();
    public static List<GbcFrame> framesList = new ArrayList<>();

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
    public static void extractSavImages(Context context) {
        try {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt()));
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File savFile = new File(downloadsDirectory + "/gbc.sav");

            //Extract the images
            List<byte[]> listImageBytes = new ArrayList<>();
            listImageBytes = extractor.extractBytes(savFile);

            for (byte[] imageBytes : listImageBytes) {
                GbcImage gbcImage = new GbcImage();
                GbcImage.numImages++;
                gbcImage.setName("Image " + (GbcImage.numImages));
                gbcImage.setFrameIndex(0);
                gbcImage.setPaletteIndex(0);
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 128, 112);
                Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), imageBytes);
                if (image.getHeight() == 112 && image.getWidth() == 128) {
                    //I need to use copy because if not it's inmutable bitmap
                    Bitmap framed = framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(framed);
                    canvas.drawBitmap(image, 16, 16, null);
                    image = framed;
                    imageBytes = encodeImage(image);
                }
                gbcImage.setImageBytes(imageBytes);
                completeBitmapList.add(image);
                gbcImagesList.add(gbcImage);
            }
        } catch (IOException e) {
            System.out.println("Error aqui");
            e.printStackTrace();
        }catch (Exception e){
            System.out.println("Error aqui");
            e.printStackTrace();
        }
    }

    public static byte[] encodeImage(Bitmap bitmap) throws IOException {
        Codec decoder = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt()), 160, bitmap.getHeight());
        return decoder.encodeInternal(bitmap);
    }

    public static void extractHexImages() throws NoSuchAlgorithmException {
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

        List<byte[]> listaBytes = new ArrayList<>();
        //******FIN DE LEER EL FICHERO
        List<String> dataList = RawToTileData.separateData(fileContent);
        String data = "";
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            GbcImage.numImages++;
            gbcImage.setImageBytes(bytes);

            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            System.out.println("HASH CODE: "+new String(hash));

            gbcImage.setName("Image " + (GbcImage.numImages));
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), gbcImage.getImageBytes());
            completeBitmapList.add(image);
            gbcImagesList.add(gbcImage);
        }
    }

    public static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        System.out.println(data.length());
        for (int i = 0; i < byteStrings.length; i++) {
            try {
                bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                        + Character.digit(byteStrings[i].charAt(1), 16));

            } catch (Exception e) {
            }
        }
        System.out.println(bytes.length);
        return bytes;
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

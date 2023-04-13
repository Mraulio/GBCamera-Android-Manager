package com.mraulio.gbcameramanager;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;

import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.importFile.ImportFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
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

                // Verificar que las claves y valores existan en el objeto de paleta
                if (paletteObject.has("shortName") && paletteObject.has("name") && paletteObject.has("palette") && paletteObject.has("origin")) {
//                    String shortName = paletteObject.getString("shortName");
//                    String name = paletteObject.getString("name");
                    JSONArray paletteArray = paletteObject.getJSONArray("palette");
//                    String origin = paletteObject.getString("origin");

                    // Verificar que la matriz de paletas tenga cuatro elementos
                    if (paletteArray.length() == 4) {
                        System.out.println("El JSON tiene el formato esperado.");
                    } else {
                        System.out.println("Not a 4 element palette.");
                        return null;
                    }
                    return readerPalettes(palettesArray);
                } else {
                    System.out.println("The json doesn't have expected keys.");
                }

            } else if (stateObject.has("frames")) {
                //Entering frame json. There are 2 types, old with id, new with hash
                JSONArray framesArray = stateObject.getJSONArray("frames");
                if (framesArray.length() == 0) {
                    System.out.println("No frames.");
                    return null;
                }
                JSONObject frameObject = framesArray.getJSONObject(0);

                //Verify what type of frames.json it is
                if (frameObject.has("id") && frameObject.has("name") && !frameObject.has("hash") && !frameObject.has("tempId")) {
                    //Old type
                    return readerFrames(jsonObject, false);

                } else if (frameObject.has("id") && frameObject.has("name") && frameObject.has("hash") && frameObject.has("tempId")) {
                    //New type with hash
                    return readerFrames(jsonObject, true);
                } else {
                    System.out.println("Not a valid frames json");
                }
            } else if (stateObject.has("images")) {
                //Images json
                //Entering images json.
                JSONArray imagesArray = stateObject.getJSONArray("images");
                if (imagesArray.length() == 0) {
                    System.out.println("No images.");
                    return null;
                }
                JSONObject imageObject = imagesArray.getJSONObject(0);
                System.out.println(imageObject);
                if (imageObject.has("hash") && imageObject.has("created") && imageObject.has("title") && imageObject.has("lines") && imageObject.has("tags")) {
                    //There are some more values to check, but not all images have those
                    return readerImages(jsonObject);

                } else return null;


            } else {
                System.out.println("Not a valid json...");
                return null;
            }


        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> readerFrames(JSONObject jsonObject, boolean newType) throws IOException, JSONException {

        List<String> stringValues = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        // Acceder a los valores del JSON
        JSONArray frames = jsonObject.getJSONObject("state").getJSONArray("frames");

        if (newType) {//If new type I get the data from the frame-hash
            for (int i = 0; i < frames.length(); i++) {
                JSONObject image = frames.getJSONObject(i);
                String hash = image.getString("hash");
                stringValues.add(hash);
            }
        } else {//If old type I get the data from the frame-id
            for (int i = 0; i < frames.length(); i++) {
                JSONObject image = frames.getJSONObject(i);
                String hash = image.getString("id");
                stringValues.add(hash);
            }
        }
        for (String value : stringValues) {
            String hash = jsonObject.getString("frame-" + value);
            finalValues.add(eachFrame(hash));
        }
        ImportFragment.addEnum = ImportFragment.ADD_WHAT.FRAMES;

        return finalValues;
    }

    public static List<String> readerImages(JSONObject jsonObject) throws IOException, JSONException {
        List<String> stringValues = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        // Acceder a los valores del JSON
        JSONArray images = jsonObject.getJSONObject("state").getJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            JSONObject imageJson = images.getJSONObject(i);
            String hash = imageJson.getString("hash");
            stringValues.add(hash);
            String data = jsonObject.getString(hash);
            String decodedData = eachImage(data);
            byte[] bytes = Methods.convertToByteArray(decodedData);
            GbcImage gbcImage = new GbcImage();
            if (!imageJson.getString("title").equals("")) {
                gbcImage.setName(imageJson.getString("title"));
            } else gbcImage.setName("*No title*");
            int height = ((decodedData).length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColors()), 160, height);
            try {
                Bitmap imageBitmap = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(0).getPaletteColors(), bytes);
                System.out.println(imageBitmap.getHeight());
                try {
                    gbcImage.setImageBytes(Methods.encodeImage(imageBitmap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                gbcImage.setFrameIndex(0);
                gbcImage.setPaletteIndex(0);
                ImportFragment.importedImagesList.add(gbcImage);
                ImportFragment.importedImagesBitmaps.add(imageBitmap);
//                ImportFragment.listImportedImageBytes.add(bytes);

            } catch (Exception e) {
                System.out.println("No se puede añadir");//Para las RGB
            }

        }
        ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
        return finalValues;
    }

    public static List<GbcPalette> readerPalettes(JSONArray palettesArr) {
        List<GbcPalette> paletteList = new ArrayList<>();
        // Recorre los elementos de palettes y recupera los datos que necesitas
        for (int i = 0; i < palettesArr.length(); i++) {
            JSONObject paletteObj = null;
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
                gbcPalette.setName(shortName);
                gbcPalette.setPaletteColors(paletteIntArray);
                paletteList.add(gbcPalette);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.PALETTES;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return paletteList;
    }

    public static String reader() throws IOException, JSONException {
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String jsonString = new String(Files.readAllBytes(Paths.get(downloadsDirectory + "/frames.json")), StandardCharsets.UTF_8);
        boolean frame = true;

        // Crear un objeto JSONObject a partir del String JSON
        JSONObject jsonObject = new JSONObject(jsonString);
        String value = "";
        // Acceder a los valores del JSON
        if (!frame) {
            String hash = jsonObject.getJSONObject("state").getJSONArray("images").getJSONObject(0).getString("hash");
            value = jsonObject.getString(hash);

        } else {
            String hash = "frame-" + jsonObject.getJSONObject("state").getJSONArray("frames").getJSONObject(22).getString("hash");
            value = jsonObject.getString(hash);

        }
        System.out.println(value + "*******************");
        byte[] compressedBytes = value.getBytes(StandardCharsets.ISO_8859_1);

        System.out.println(value);
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);

        byte[] outputBytes = new byte[compressedBytes.length * 200];//Because the compressed is much smaller
        int length = 0;
        try {
            length = inflater.inflate(outputBytes);
            inflater.end();
        } catch (DataFormatException e) {
            System.out.println("Error al descomprimir los datos: " + e.getMessage());
        }

        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
        System.out.println(uncompressedData);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputString.length(); i += 2) {
            sb.append(outputString.substring(i, i + 2));
            sb.append(" ");
        }
        System.out.println(outputString);
        String primeraParte = outputString.substring(0, 1280);
        String central = outputString.substring(1280, 3072);//esta parte tiene la informacion de los lados del marco, lo divido en 14, cada una de 128 caracteres
        String[] partes = new String[14];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(primeraParte);
        for (int i = 0; i < 14; i++) {
            String parte = central.substring(i * 128, (i + 1) * 128);  // Obtener la parte de 128 caracteres
            StringBuilder builder = new StringBuilder(parte);  // Crear un StringBuilder a partir de la parte
            for (int j = 0; j < 512; j++) {
                builder.insert(64, '0');  // Insertar el caracter '0' en la posicion 64
            }
            partes[i] = builder.toString();  // Guardar la parte con los '0' agregados en el array
            stringBuilder.append(partes[i]);
            System.out.println(partes[i].length());
        }

        String ultimaParte = outputString.substring(outputString.length() - 1280);
        stringBuilder.append(ultimaParte);
        System.out.println(primeraParte);
        System.out.println(central);
        System.out.println(ultimaParte);
        String terminada = stringBuilder.toString();
        System.out.println(terminada.length() + ":  " + terminada);
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < terminada.length(); i += 2) {
            if (i > 0) {
                resultado.append(" "); // añade un espacio cada dos caracteres
            }
            resultado.append(terminada.substring(i, i + 2));
        }

        System.out.println("Resultado devuelto: " + resultado.toString());
        return resultado.toString();
    }

    public static String eachImage(String value) {
        byte[] compressedBytes = value.getBytes(StandardCharsets.ISO_8859_1);
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);
        byte[] outputBytes = new byte[compressedBytes.length * 300];
        int length = 0;
        try {
            length = inflater.inflate(outputBytes);
            inflater.end();
        } catch (DataFormatException e) {
            System.out.println("Error al descomprimir los datos: " + e.getMessage());
        }
        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < outputString.length(); i += 2) {
            try {//Esto lo pongo para el marco en blanco por ejemplo que da StringIndexOutOfBoundsException, hay que arreglarlo (Creo que esta arreglado con el *300
                sb.append(outputString.substring(i, i + 2));
                sb.append(" ");
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }

    public static String eachFrame(String value) {
        byte[] compressedBytes = value.getBytes(StandardCharsets.ISO_8859_1);

//        System.out.println(value);
        Inflater inflater = new Inflater();
        inflater.setInput(compressedBytes);

        byte[] outputBytes = new byte[compressedBytes.length * 100];
        int length = 0;
        try {
            length = inflater.inflate(outputBytes);
            inflater.end();
        } catch (DataFormatException e) {
            System.out.println("Error al descomprimir los datos: " + e.getMessage());
        }

        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
//        System.out.println(uncompressedData);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < outputString.length(); i += 2) {
            try {//Esto lo pongo para el marco en blanco por ejemplo que da StringIndexOutOfBoundsException, hay que arreglarlo
                sb.append(outputString.substring(i, i + 2));
                sb.append(" ");
            } catch (Exception e) {
            }
        }

//        System.out.println(outputString);
        String primeraParte = outputString.substring(0, 1280);
        String central = outputString.substring(1280, 3072);//esta parte tiene la informacion de los lados del marco, lo divido en 14, cada una de 128 caracteres
        String[] partes = new String[14];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(primeraParte);
        for (int i = 0; i < 14; i++) {
            String parte = central.substring(i * 128, (i + 1) * 128);  // Obtener la parte de 128 caracteres
            StringBuilder builder = new StringBuilder(parte);  // Crear un StringBuilder a partir de la parte
            for (int j = 0; j < 512; j++) {
                builder.insert(64, '0');  // Insertar el caracter '0' en la posicion 64
            }
            partes[i] = builder.toString();  // Guardar la parte con los '0' agregados en el array
            stringBuilder.append(partes[i]);
//            System.out.println(partes[i].length());
        }
        String ultimaParte = outputString.substring(outputString.length() - 1280);
        stringBuilder.append(ultimaParte);

        String terminada = stringBuilder.toString();
//        System.out.println(terminada.length()+":  "+terminada);
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < terminada.length(); i += 2) {
            if (i > 0) {
                resultado.append(" "); // añade un espacio cada dos caracteres
            }
            resultado.append(terminada.substring(i, i + 2));
        }

        return resultado.toString();
    }

}

package com.mraulio.gbcameramanager;

import android.graphics.Color;
import android.os.Environment;

import com.mraulio.gbcameramanager.model.GbcPalette;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class JsonReader {

    public static List<String> readerFrames() throws IOException, JSONException {
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String jsonString = new String(Files.readAllBytes(Paths.get(downloadsDirectory + "/frames.json")), StandardCharsets.UTF_8);
        boolean frame = true;
        List<String> stringValues = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        // Crear un objeto JSONObject a partir del String JSON

        // Acceder a los valores del JSON
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray frames = jsonObject.getJSONObject("state").getJSONArray("frames");
//        System.out.println("Hay estos frames: " + frames.length());

        for (int i = 0; i < frames.length(); i++) {
            JSONObject image = frames.getJSONObject(i);
            String hash = image.getString("hash");
//            System.out.println(hash); // o haz algo más con el valor obtenido
//            System.out.println(jsonObject.getString(hash));
            stringValues.add(hash);
        }
//        System.out.println("Hay estos: "+stringValues.size());

        for (String value : stringValues) {
//            System.out.println("El valor es: " + value);

            String hash = jsonObject.getString("frame-" + value);
//            System.out.println(eachFrame(hash));

            finalValues.add(eachFrame(hash));
        }
        return finalValues;
    }

    public static List<String> readerImages(String jsonString) throws IOException, JSONException {
//        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        String jsonString = new String(Files.readAllBytes(Paths.get(downloadsDirectory + "/frames.json")), StandardCharsets.UTF_8);
        boolean frame = true;
        List<String> stringValues = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        // Crear un objeto JSONObject a partir del String JSON

        // Acceder a los valores del JSON
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONArray images = jsonObject.getJSONObject("state").getJSONArray("images");
//        System.out.println("There are images: " + images.length());

        for (int i = 0; i < images.length(); i++) {
            JSONObject image = images.getJSONObject(i);
            String hash = image.getString("hash");
//            System.out.println(hash); // o haz algo más con el valor obtenido
//            System.out.println(jsonObject.getString(hash));
            stringValues.add(hash);
        }
//        System.out.println("Hay estos: "+stringValues.size());

        for (String value : stringValues) {
//            System.out.println("El valor es: " + value);

            String hash = jsonObject.getString(value);
//            System.out.println(eachImage(hash));

            finalValues.add(eachImage(hash));
        }
        return finalValues;
    }

    public static void readerPalettes(String jsonString) throws IOException, JSONException {
//        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        String jsonString = new String(Files.readAllBytes(Paths.get(downloadsDirectory + "/palettes.json")), StandardCharsets.UTF_8);

        // Acceder a los valores del JSON
        JSONObject jsonObject = new JSONObject(jsonString);
        // Accede a los datos de palettes
        JSONArray palettesArr = jsonObject.getJSONObject("state").getJSONArray("palettes");

        // Recorre los elementos de palettes y recupera los datos que necesitas
        for (int i = 0; i < palettesArr.length(); i++) {
            JSONObject paletteObj = palettesArr.getJSONObject(i);
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
            Methods.gbcPalettesList.add(gbcPalette);
        }

//        return finalValues;
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
            String hash = jsonObject.getJSONObject("state")
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("hash");
            value = jsonObject.getString(hash);

        } else {
            String hash = "frame-" + jsonObject.getJSONObject("state")
                    .getJSONArray("frames")
                    .getJSONObject(22)
                    .getString("hash");
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
        } catch (
                DataFormatException e) {
            System.out.println("Error al descomprimir los datos: " + e.getMessage());
        }

        String uncompressedData = new String(outputBytes, 0, length, StandardCharsets.UTF_8);
        System.out.println(uncompressedData);
        String outputString = uncompressedData.replaceAll(System.lineSeparator(), "");
        StringBuilder sb = new StringBuilder();
        for (
                int i = 0; i < outputString.length(); i += 2) {
            sb.append(outputString.substring(i, i + 2));
            sb.append(" ");
        }
        System.out.println(outputString);
        String primeraParte = outputString.substring(0, 1280);
        String central = outputString.substring(1280, 3072);//esta parte tiene la informacion de los lados del marco, lo divido en 14, cada una de 128 caracteres
        String[] partes = new String[14];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(primeraParte);
        for (
                int i = 0;
                i < 14; i++) {
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

        for (
                int i = 0; i < terminada.length(); i += 2) {
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
//        System.out.println(primeraParte);
//        System.out.println(central);
//        System.out.println(ultimaParte);
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

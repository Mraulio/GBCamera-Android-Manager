package com.mraulio.gbcameramanager.ui.importFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HexToTileData {
    public static List<String> separateData(String data) {
        List<String> dataList = new ArrayList<String>();
        String tileData;
        StringBuilder outputString = new StringBuilder();
        if (data.startsWith("{\"command\":\"INIT\"}")) {//Regular data
            String dataWithoutCommands = deleteLines(data);
            dataList.add(dataWithoutCommands);
        } else {
            try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("88 33 01")) {//Raw data
                        // INIT command
                    } else if (line.startsWith("88 33 04")) {
                        // Start DATA Package
                        if (line.startsWith("88 33 04 00 00 00 04 00 00 00")) {
                            // EMPTY DATA PACKAGE??
                        } else {
                            // Data Package has TILE data
                            if (outputString.length() > 0) {
                                // If not first line
                                outputString.append("\n");
                            }
                            try {
                                // Se hace aqui para que no añada una linea de mas al final si no es una con datos
                                // Ver de hacer esto con la longitud de una empty en vez de con un try catch
                                tileData = line.substring(18, line.length() - 12);
                                for (int i = 0; i < tileData.length(); i++) {
                                    char currentChar = tileData.charAt(i);
                                    if ((i + 1) % 48 == 0) {
                                        outputString.append('\n');
                                    } else {
                                        outputString.append(currentChar);
                                    }
                                }
                            } catch (Exception e) {
                                // DIFFERENT EMPTY DATA PACKAGE??
                                e.printStackTrace();
                            }
                        }
                    } else if (line.startsWith("88 33 02")) {
                        // PRINT command
                        if (line.startsWith("88 33 02 00 04 00 01 10") || line.startsWith("88 33 02 00 04 00 01 00")) {
                            // 0 margins, I just continue adding data to the outputString after deleting last \n
                            outputString.deleteCharAt(outputString.length() - 1);//To delete the last added \n
                        } else {
                            // End of image. It's stored in the list and set to length 0 to start fresh with the next image
                            // DIFFERENT MARGIN
                            dataList.add(outputString.toString());
                            outputString.setLength(0);
                        }
                    } else if (line.startsWith("88 33 0F")) {
                        // INQUIRY command
                    } else if (line.startsWith("88 33 08")) {
                        // ABORT command, very rare
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dataList;
    }

    //To delete lines that begin with {
    public static String deleteLines(String texto) {
        String[] lineas = texto.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String linea : lineas) {
            if (!linea.startsWith("{")) {
                sb.append(linea).append(" ");
            }
        }
        return sb.toString().trim();
    }

}

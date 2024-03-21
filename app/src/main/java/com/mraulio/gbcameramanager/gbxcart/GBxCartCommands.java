package com.mraulio.gbcameramanager.gbxcart;

import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.btnAddImages;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.btnDelSav;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.cbDeleted;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.cbLastSeen;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.layoutCb;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.readSav;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment.showImages;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.magicIsReal;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Process;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GBxCartCommands {
    //Class that contains the methods to communicate with the GBxCart
    //Translated from the code from Lesserkuma

    private static final int TIMEOUT = 2000;
    private static FileOutputStream fos = null;
    private static BufferedOutputStream bos = null;

    public static void powerOff(UsbSerialPort port, Context context) {
        byte[] command = new byte[1];
        int com = GBxCartConstants.DEVICE_CMD.get("OFW_CART_PWR_OFF");
        command[0] = (byte) com; //POWER OFF
        try {
            port.write(command, TIMEOUT);//VER LO DE LOS TIMEOUTS

        } catch (Exception e) {
//            Toast.makeText(context, "Error en PowerOff\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public static void powerOn(UsbSerialPort port, Context context) {
        byte[] command = new byte[1];
        try {
            command[0] = (byte) 0x2F;//OFW_CART_PWR_ON
            port.write(command, TIMEOUT);

        } catch (Exception e) {
//            Toast.makeText(context, "Error en PowerOn\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public static void setCartType(UsbSerialPort port, Context context) {
        byte[] command = new byte[1];

        try {
            int com = GBxCartConstants.DEVICE_CMD.get("SET_MODE_DMG");
            command[0] = (byte) com; //SET_MODE_DMG
            port.write(command, TIMEOUT);

            com = GBxCartConstants.DEVICE_CMD.get("SET_VOLTAGE_5V");
            command[0] = (byte) com; //SET_VOLTAGE_5V
            port.write(command, TIMEOUT);

            setFwVariable("DMG_READ_METHOD", 1, port, context);

            setFwVariable("CART_MODE", 1, port, context);

        } catch (Exception e) {
//            Toast.makeText(context, "Error en SetCartType\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private static void setFwVariable(String key, int value, UsbSerialPort port, Context context) {
        int size = 0;
        int keyInt = 0;
        for (Map.Entry<String, int[]> entry : GBxCartConstants.DEVICE_VAR.entrySet()) {
            String k = entry.getKey();
            int[] v = entry.getValue();
            if (k.contains(key)) {
                if (v[0] == 8) {
                    size = 1;
                } else if (v[0] == 16) {
                    size = 2;
                } else if (v[0] == 32) {
                    size = 4;
                }
                keyInt = v[1];
                break;
            }
        }
        int temp = GBxCartConstants.DEVICE_CMD.get("SET_VARIABLE");
        ByteBuffer bb = ByteBuffer.allocate(13);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) temp);
        bb.put((byte) size);
        bb.putInt(keyInt);
        bb.putInt(value);
        byte[] byteArray = bb.array();
        try {
            port.write(byteArray, TIMEOUT);
        } catch (Exception e) {
//            Toast.makeText(context, "ErrorsetFwVariable" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private static byte[] CartRead_ROM(int address, int length, UsbSerialPort port, Context context, TextView tv) {
        int max_length = 64;
        int num = (int) Math.ceil((double) length / (double) max_length);
        if (length > max_length) {
            length = max_length;
        }
        byte[] buffer = new byte[num * length];
        setFwVariable("TRANSFER_SIZE", length, port, context);
        setFwVariable("ADDRESS", address, port, context);
        setFwVariable("DMG_ACCESS_MODE", 1, port, context); // MODE_ROM_READ

        byte[] commandByte = new byte[1];

        String command = "DMG_CART_READ";

        try {
            for (int i = 0; i < num; i++) {
                int x = GBxCartConstants.DEVICE_CMD.get(command);
                commandByte[0] = (byte) x;
                port.write(commandByte, TIMEOUT);
            }
        } catch (Exception e) {
//            Toast.makeText(context, "Error en cartReadRom\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
        return buffer;
    }

    public static String ReadRomName(UsbSerialPort port, Context context, TextView tv) {
        byte[] readLength = new byte[0x10];
        byte[] receivedData = new byte[0x10];

        try {
            CartRead_ROM(0x134, 0x10, port, context, tv);
            int len = port.read(readLength, TIMEOUT);//Intento leer manualmente
            receivedData = (Arrays.copyOf(readLength, len));

        } catch (Exception e) {
//            Toast.makeText(context, "Error en PowerOn\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
        return new String(receivedData);
    }

    public static void Cart_write(int address, int value, UsbSerialPort port, Context context) {

        byte[] buffer = new byte[6];
        buffer[0] = (byte) (GBxCartConstants.DEVICE_CMD.get("DMG_CART_WRITE") & 0xFF);
        byte[] addressBytes = ByteBuffer.allocate(4).putInt(address).array();
        System.arraycopy(addressBytes, 0, buffer, 1, 4);
        buffer[5] = (byte) (value & 0xFF);

        try {
            port.write(buffer, TIMEOUT);
        } catch (Exception e) {
//            Toast.makeText(context, "Error en Cart_write\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public static void CartRead_RAM(int address, int length, UsbSerialPort port, Context context) {
        int max_length = 64;
        int num = (int) Math.ceil((double) length / (double) max_length);
        if (length > max_length) {
            length = max_length;
        }
        setFwVariable("TRANSFER_SIZE", length, port, context);
        setFwVariable("ADDRESS", 0xA000 + address, port, context);
        setFwVariable("DMG_ACCESS_MODE", 3, port, context); // MODE_ROM_READ
        setFwVariable("DMG_READ_CS_PULSE", 1, port, context);

        byte[] commandByte = new byte[1];

        String command = "DMG_CART_READ";
        try {
//
            int x = GBxCartConstants.DEVICE_CMD.get(command);
            commandByte[0] = (byte) x;
            port.write(commandByte, TIMEOUT);

        } catch (Exception e) {
//            Toast.makeText(context, "Error en cartReadRom\n" + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public static class ReadPHOTORomAsyncTask extends AsyncTask<Void, Integer, Void> {
        private Context context;
        private TextView tv;
        private UsbSerialPort port;
        List<File> fullRomFileList;
        List<byte[]> fullRomFileBytes;

        public ReadPHOTORomAsyncTask(UsbSerialPort port, Context context, TextView tv, List<File> fullRomFileList, List<byte[]> fullRomFileBytes) {
            this.port = port;
            this.context = context;
            this.tv = tv;
            this.fullRomFileList = fullRomFileList;
            this.fullRomFileBytes = fullRomFileBytes;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            //DUMP 1 MB ROM file
            LocalDateTime now = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                now = LocalDateTime.now();
            }
            Date nowDate = new Date();

            String fileName = "PhotoFullRom_";

            String folderName = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                folderName = "PhotoFullRom_" + dtf.format(now);
                fileName += dtf.format(now) + "-full.gbc";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                folderName = "PhotoFullRom_" + sdf.format(nowDate);
                fileName += sdf.format(nowDate) + "-full.gbc";
            }

            UsbSerialFragment.photoFolder = new File(Utils.PHOTO_DUMPS_FOLDER, folderName);
            //I create the new directory if it doesn't exists
            try {
                if (!UsbSerialFragment.photoFolder.exists() && !UsbSerialFragment.photoFolder.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + UsbSerialFragment.photoFolder);
                }
            } catch (Exception e) {
//                Toast toast = Toast.makeText(context, "Error making directory: " + e.toString(), Toast.LENGTH_SHORT);
//                toast.show();
            }
            File file = new File(UsbSerialFragment.photoFolder, fileName);
            // create the new file inside the directory
            try {
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Couldn't create file: " + file);
                }
            } catch (Exception e) {
//                Toast toast = Toast.makeText(context, "Error making file: " + e.toString(), Toast.LENGTH_SHORT);
//                toast.show();
            }
            try {
                fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos);
                byte[] readLength = new byte[64];
                for (int i = 0; i < 64; i++) {
                    // Set ROM bank
                    Cart_write(0x2100, i, port, context);
                    // Read 16 (0x4000) KiB of SRAM
                    for (int j = 0; j < 256; j++) {
                        //If first ROM bank, read 0-0x3fff, other banks from 0x4000 0x7fff
                        if (i == 0) {
                            CartRead_ROM(j * 64, 64, port, context, tv);
                        } else {
                            CartRead_ROM((j * 64) + 0x4000, 64, port, context, tv);
                        }
                        int len = port.read(readLength, TIMEOUT);
                        byte[] receivedData = (Arrays.copyOf(readLength, len));
                        bos.write(receivedData);
                        int totalIterations = 64 * 256;
                        int currentIteration = i * 256 + j + 1;
                        int progress = currentIteration * 100 / totalIterations;
                        publishProgress(progress, 0);
                    }
                }
                tv.append("\n" + tv.getContext().getString(R.string.done_dumping_photo));
                bos.close();

            } catch (Exception e) {
                Toast.makeText(context, "Error en FullReadRom\n" + e.toString(), Toast.LENGTH_LONG).show();
            }
            publishProgress(100, 1);

            //Now I divide the 1MB file into 8. First will be the gbc rom, next 7 ram files
            int fileSize = (int) file.length();
            int partSize = fileSize / 8; // Divides the file in 8 parts
            byte[] buffer = new byte[partSize];

            try (RandomAccessFile reader = new RandomAccessFile(file, "r")) {
                for (int i = 0; i < 8; i++) {
                    String extension = "";
                    if (i == 0) {
                        extension = ".gbc";
                    } else extension = ".part_" + i + ".sav";
                    File outputFile = new File(UsbSerialFragment.photoFolder, fileName + extension);

                    try (OutputStream writer = new FileOutputStream(outputFile)) {
                        int bytesRead = 0;
                        while (bytesRead < partSize && reader.read(buffer) != -1) {
                            writer.write(buffer);
                            bytesRead += buffer.length;
                        }
                    }
                    if (i != 0) {//Because 0 is the actual rom
                        try (InputStream is = new FileInputStream(outputFile)) {
                            byte[] bufferAux = new byte[1024];
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                            int read;
                            while ((read = is.read(bufferAux)) != -1) {
                                outputStream.write(bufferAux, 0, read);
                            }
                            byte[] fileBytes = outputStream.toByteArray();

                            if (magicIsReal(fileBytes)) {
                                fullRomFileList.add(outputFile);
                                fullRomFileBytes.add(fileBytes);
                            } else {
                                outputFile.delete();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            int finishedExtracting = values[1];
            if (finishedExtracting == 0)
                tv.setText(context.getString(R.string.dumping_rom_wait) + "\n" + progress + "%");
            else if (finishedExtracting == 1) {
                tv.append("\n" + context.getString(R.string.analyzing));
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            tv.append("\n" + context.getString(R.string.done_analyzing));
            tv.append("\n" + tv.getContext().getString(R.string.done_dumping_photo));
            powerOff(port, context);

            UsbSerialFragment.readRomSavs();
        }
    }

    public static class ReadRamAsyncTask extends AsyncTask<Void, Integer, Void> {
        private Context context;
        private TextView tv;
        private UsbSerialPort port;
        File latestFile;

        public ReadRamAsyncTask(UsbSerialPort port, Context context, TextView tv, File latestFile) {
            this.port = port;
            this.context = context;
            this.tv = tv;
            this.latestFile = latestFile;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            LocalDateTime now = null;
            Date nowDate = new Date();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                now = LocalDateTime.now();
            }
            String fileName = "gbCamera_";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                fileName += dtf.format(now) + ".sav";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                fileName += sdf.format(nowDate) + ".sav";

            }

            File file = new File(Utils.SAVE_FOLDER, fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //I create the new directory if it doesn't exists
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + parent);
                }
            } catch (Exception e) {
                Toast toast = Toast.makeText(context, "Error making file: " + e.toString(), Toast.LENGTH_SHORT);
                toast.show();
            }
            //# Enable SRAM access
            Cart_write(0x6000, 0x01, port, context);
            Cart_write(0x0000, 0x0A, port, context);

            try {
                fos = new FileOutputStream(file);
                bos = new BufferedOutputStream(fos);
                for (int i = 0; i < 16; i++) {
                    // Set SRAM bank
                    Cart_write(0x4000, i, port, context);
                    // Read 8 KiB of SRAM
                    for (int j = 0; j < 128; j++) {
                        byte[] readLength = new byte[64];
                        CartRead_RAM(j * 64, 64, port, context);
                        int len = port.read(readLength, TIMEOUT);

                        try {
                            outputStream.write(Arrays.copyOf(readLength, len));
                            outputStream.flush();
                        } catch (IOException e) {
                        }

                        int totalIterations = 16 * 128;
                        int currentIteration = i * 128 + j + 1;
                        int progress = currentIteration * 100 / totalIterations;

                        publishProgress(progress);
                    }
                }
                bos.write(outputStream.toByteArray());
                bos.close();
            } catch (Exception e) {
                Utils.toast(context, "Error en READRAM\n" + e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            tv.setText(context.getString(R.string.dumping_ram_wait) + "\n" + progress + "%");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            tv.append("\n" + tv.getContext().getString(R.string.done_dumping_ram));
            powerOff(port, context);

            //To get the extracted file, as the latest one in the directory
            latestFile = null;
            //To get the last created file
            File[] files = Utils.SAVE_FOLDER.listFiles();
            if (files != null && files.length > 0) {
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.compare(f2.lastModified(), f1.lastModified());
                    }
                });
                latestFile = files[0];

            }
            byte[] fileBytes = new byte[0];
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    fileBytes = Files.readAllBytes(latestFile.toPath());
                } else {
                    FileInputStream fis = new FileInputStream(latestFile);
                    fileBytes = new byte[(int) latestFile.length()];
                    fis.read(fileBytes);
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!magicIsReal(fileBytes)) {
                tv.append(context.getString(R.string.no_valid_file));
                return;
            }

            tv.append(context.getString(R.string.last_sav_name) + latestFile.getName() + ".\n" +
                    context.getString(R.string.size) + latestFile.length() / 1024 + "KB");
            readSav(latestFile, fileBytes, 0);
            btnAddImages.setVisibility(View.VISIBLE);
            btnDelSav.setVisibility(View.VISIBLE);
            layoutCb.setVisibility(View.VISIBLE);
            showImages(cbLastSeen, cbDeleted);

        }
    }

}

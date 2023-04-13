package com.mraulio.gbcameramanager.ui.importFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.JsonReader;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.RawToTileData;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;


public class ImportFragment extends Fragment {

    public static List<GbcImage> importedImagesList = new ArrayList<>();
    public static List<Bitmap> importedImagesBitmaps = new ArrayList<>();
    public static List<byte[]> listImportedImageBytes = new ArrayList<>();
    byte[] fileBytes;
    TextView tvFileName;
    String fileName;
    boolean savFile = false;
    boolean isJson = false;
    String fileContent = "";
    List<?> receivedList;
    List<GbcFrame> gbcFramesList;
    CustomGridViewAdapterPalette customAdapterPalette;

    public enum ADD_WHAT {
        PALETTES,
        FRAMES,
        IMAGES
    }

    public static ADD_WHAT addEnum;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);
        Button btnSelectFile = view.findViewById(R.id.btnSelectFile);
        Button btnExtractFile = view.findViewById(R.id.btnExtractFile);
        Button btnAddImages = view.findViewById(R.id.btnAddImages);
        btnAddImages.setVisibility(View.GONE);
        MainActivity.pressBack = false;

        tvFileName = view.findViewById(R.id.tvFileName);
        GridView gridViewImport = view.findViewById(R.id.gridViewImport);
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });
        btnExtractFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //I clear the lists in case I choose several files without leaving
                importedImagesList.clear();
                importedImagesBitmaps.clear();
                listImportedImageBytes.clear();
                gridViewImport.setAdapter(new CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps));
                if (savFile && !isJson) {
                    extractSavImages(getContext());
                    tvFileName.setText("" + importedImagesList.size());//"" to make it work
                    gridViewImport.setAdapter(new CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps));
                    btnAddImages.setText("Add images");
                    btnAddImages.setVisibility(View.VISIBLE);
                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;

                } else if (!savFile && !isJson) {
                    extractHexImagesFromFile(fileContent);
                    tvFileName.setText("" + importedImagesList.size());//"" to make it work
                    gridViewImport.setAdapter(new CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps));
                    btnAddImages.setText("Add images");
                    btnAddImages.setVisibility(View.VISIBLE);
                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;

                } else if (!savFile && isJson) {
                    receivedList = JsonReader.jsonCheck(fileContent);
                    if (receivedList == null) {
                        Methods.toast(getContext(), "Not a valid list");
                        return;
                    }
                    switch (addEnum) {
                        case PALETTES:
                            customAdapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, (ArrayList<GbcPalette>) receivedList, true, true);
                            gridViewImport.setAdapter(customAdapterPalette);
                            btnAddImages.setText("Add palettes");
                            btnAddImages.setVisibility(View.VISIBLE);
                            break;

                        case FRAMES:
                            gbcFramesList = new ArrayList<>();
                            for (Object str : receivedList) {
                                byte[] bytes = convertToByteArray((String) str);
                                GbcFrame gbcFrame = new GbcFrame();
                                gbcFrame.setFrameName("next frame");
                                int height = (((String) str).length() + 1) / 120;//To get the real height of the image
                                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColors()), 160, height);
                                Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(0).getPaletteColors(), bytes);
                                gbcFrame.setFrameBitmap(image);
                                gbcFramesList.add(gbcFrame);
                            }
                            btnAddImages.setText("Add frames");
                            btnAddImages.setVisibility(View.VISIBLE);
                            gridViewImport.setAdapter(new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, gbcFramesList, true));
                            break;
                        case IMAGES:
                            btnAddImages.setText("Add images");
                            btnAddImages.setVisibility(View.VISIBLE);
                            gridViewImport.setAdapter(new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps));
                            break;
                    }
//                    try {
//                        List<String> listImagesString = JsonReader.readerImages(fileContent);
//                        for (String imageString : listImagesString) {
//                            byte[] imageBytes;
//                            try {
//                                imageBytes = convertToByteArray(imageString);
//                                GbcImage gbcImage = new GbcImage();
//                                GbcImage.numImages++;
//                                gbcImage.setName("Image " + (GbcImage.numImages));
//                                gbcImage.setFrameIndex(0);
//                                gbcImage.setPaletteIndex(0);
//                                int height = (imageString.length() + 1) / 120;//To get the real height of the image
//                                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, height);
//                                Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors(), imageBytes);
//                                gbcImage.setImageBytes(imageBytes);
//                                Methods.completeImageList.add(image);
//                                Methods.gbcImagesList.add(gbcImage);
//                                importedImagesBitmaps.add(image);
//                            } catch (Exception e) {
//                                System.out.println("////////////Exception in convertToByteArray:\n" + e.toString());
//                            }
//
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }

            }
        });
        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (addEnum) {
                    case PALETTES:
                        List<GbcPalette> newPalettes = new ArrayList<>();
                        for (Object palette : receivedList) {
                            boolean alreadyAdded = false;
                            GbcPalette gbcp = (GbcPalette) palette;
                            //If the palette already exists (by the name) it doesn't add it. Same if it's already added
                            for (GbcPalette objeto : Methods.gbcPalettesList) {
                                if (objeto.getName().toLowerCase(Locale.ROOT).equals(gbcp.getName())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                newPalettes.add(gbcp);
                            }
                        }
                        Methods.gbcPalettesList.addAll(newPalettes);
                        Methods.toast(getContext(), "Palettes added.");
                        customAdapterPalette.notifyDataSetChanged();
                        break;
                    case FRAMES:
                        Methods.framesList.addAll(gbcFramesList);
                        Methods.toast(getContext(), "Frames added.");
                        break;
                    case IMAGES:
                        GbcImage.numImages += importedImagesList.size();
                        Methods.gbcImagesList.addAll(importedImagesList);
                        Methods.completeImageList.addAll(importedImagesBitmaps);
                        Methods.toast(getContext(), "Images added.");
                        break;
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    public void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//Any type of file
        startActivityForResult(Intent.createChooser(intent, "Select File"), 123);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == Activity.RESULT_OK && data != null) {

            Uri uri = data.getData();
            String[] aux = uri.getPath().split("/");
            fileName = aux[aux.length - 1];
            //I check the extension of the file
            if (uri.getPath().substring(uri.getPath().length() - 3).equals("sav")) {
                ByteArrayOutputStream byteStream = null;
                savFile = true;
                isJson = false;

                try {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                    // Crear un ByteArrayOutputStream para copiar el contenido del archivo
                    byteStream = new ByteArrayOutputStream();
                    // Leer el contenido del archivo en un buffer de 1KB y copiarlo en el ByteArrayOutputStream
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteStream.write(buffer, 0, len);
                    }
                    // Cerrar el InputStream y el ByteArrayOutputStream
                    byteStream.close();
                    inputStream.close();
                } catch (Exception e) {
                }
                // Obtener los bytes del archivo como un byte[]
                fileBytes = byteStream.toByteArray();
                tvFileName.setText("" + fileBytes.length + " Name: " + fileName);
            } else if (uri.getPath().substring(uri.getPath().length() - 3).equals("txt")) {
                savFile = false;
                isJson = false;

                try {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                    // Crear un ByteArrayOutputStream para copiar el contenido del archivo
                    StringBuilder stringBuilder = new StringBuilder();
                    try {
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
                    fileContent = stringBuilder.toString();
                    fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
                    tvFileName.setText("" + fileBytes.length + " Name: " + fileName);
                } catch (Exception e) {
                }
            } else if (uri.getPath().substring(uri.getPath().length() - 4).equals("json")) {
                savFile = false;
                isJson = true;
                try {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                    // Crear un ByteArrayOutputStream para copiar el contenido del archivo
                    StringBuilder stringBuilder = new StringBuilder();
                    try {
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
                    fileContent = stringBuilder.toString();
                    fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
                    tvFileName.setText("" + fileBytes.length + " Name: " + fileName);
                } catch (Exception e) {
                }
            }
        }
    }


    public void extractSavImages(Context context) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        try {
            //Extract the images
            listImportedImageBytes = extractor.extractBytes(fileBytes);
            int nameIndex = 1;
            for (byte[] imageBytes : listImportedImageBytes) {
                GbcImage gbcImage = new GbcImage();
                gbcImage.setName(nameIndex++ + "-" + fileName);
                gbcImage.setFrameIndex(0);
                gbcImage.setPaletteIndex(0);
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 128, 112);
                Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors(), imageBytes);
                if (image.getHeight() == 112 && image.getWidth() == 128) {
                    System.out.println("***********ENTERING ADDING FRAME*************");
                    //I need to use copy because if not it's inmutable bitmap
                    Bitmap framed = Methods.framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(framed);
                    canvas.drawBitmap(image, 16, 16, null);
                    image = framed;
                    imageBytes = Methods.encodeImage(image);
                    System.out.println("***********" + image.getHeight() + " " + image.getWidth() + "*************");

                }
                gbcImage.setImageBytes(imageBytes);
                importedImagesBitmaps.add(image);
                importedImagesList.add(gbcImage);
            }
        } catch (Exception e) {
            Toast toast = Toast.makeText(context, "Error\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();
            e.printStackTrace();
        }
    }

    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
        Context context;
        int layoutResourceId;
        List<GbcImage> data = new ArrayList<GbcImage>();
        private List<Bitmap> images;

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ImportFragment.CustomGridViewAdapterImage.RecordHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new RecordHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
                holder.imageItem = (ImageView) row.findViewById(R.id.imageView);
                row.setTag(holder);
            } else {
                holder = (RecordHolder) row.getTag();
            }
            Bitmap image = images.get(position);
            String name = data.get(position).getName();
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;

        }
    }

    public void extractHexImages() {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //*******PARA LEER EL FICHERO HEXDATA
        File ficheroHex = new File(directory, "timelapse.txt");
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
        System.out.println("La longitud del fichero hex es de : " + fileContent.length());
        List<String> dataList = RawToTileData.separateData(fileContent);
        String data = "";
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            listaBytes.add(bytes);
        }
        for (byte[] imageBytes : listaBytes) {
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(imageBytes);
//                if (nameIndex%2==0)
//                    gbcImage.setImageBytes(cambiarPaleta(imageBytes,1));
//                else
//                    gbcImage.setBitmap(imageBytes);
            gbcImage.setName("Image " + (GbcImage.numImages));
//                gbcImage.setFrameIndex(0);
//                gbcImage.setPaletteIndex(0);
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors(), gbcImage.getImageBytes());
            if (image.getHeight() == 112 && image.getWidth() == 128) {
                //I need to use copy because if not it's inmutable bitmap
                Bitmap framed = Methods.framesList.get(gbcImage.getFrameIndex()).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(framed);
                canvas.drawBitmap(image, 16, 16, null);
                image = framed;
            }
            importedImagesBitmaps.add(image);
            importedImagesList.add(gbcImage);
        }

    }

    public void extractHexImagesFromFile(String fileContent) {
//        List<byte[]> listaBytes = new ArrayList<>();
        System.out.println("La longitud del fichero hex es de : " + fileContent.length());
        List<String> dataList = RawToTileData.separateData(fileContent);
        String data = "";
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(bytes);
            gbcImage.setName("Image " + (GbcImage.numImages));
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors(), gbcImage.getImageBytes());
            importedImagesBitmaps.add(image);
            importedImagesList.add(gbcImage);
        }
    }

    private static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));
        }
//        System.out.println(bytes.length);
        return bytes;
    }

}
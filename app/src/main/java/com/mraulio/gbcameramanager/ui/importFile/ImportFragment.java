package com.mraulio.gbcameramanager.ui.importFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.methods.Methods;
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ImportFragment extends Fragment {

    public static List<GbcImage> importedImagesList = new ArrayList<>();
    public static List<Bitmap> importedImagesBitmaps = new ArrayList<>();
    GbcImage lastSeenImage;
    Bitmap lastSeenBitmap;
    List<GbcImage> listActiveImages = new ArrayList<>();
    List<Bitmap> listActiveBitmaps = new ArrayList<>();
    List<GbcImage> listDeletedImages;
    List<Bitmap> listDeletedBitmaps;
    List<Bitmap> listDeletedBitmapsRedStroke;
    List<GbcImage> finalListImages;
    List<Bitmap> finalListBitmaps;
    public static List<ImageData> importedImageDatas = new ArrayList<>();
    public static List<byte[]> listImportedImageBytes = new ArrayList<>();
    byte[] fileBytes;

    TextView tvFileName;
    static String fileName;
    boolean savFile = false;
    boolean isJson = false;
    String fileContent = "";
    List<?> receivedList;
    int numImagesAdded;
    Button btnExtractFile;
    CheckBox cbLastSeen, cbDeleted;
    LinearLayout layoutCb;
    CustomGridViewAdapterPalette customAdapterPalette;
    GridView gridViewImport;

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
        btnExtractFile = view.findViewById(R.id.btnExtractFile);
        btnExtractFile.setVisibility(View.GONE);
        cbLastSeen = view.findViewById(R.id.cbLastSeen);
        cbDeleted = view.findViewById(R.id.cbDeletedImages);
        layoutCb = view.findViewById(R.id.layout_cb);
        Button btnAddImages = view.findViewById(R.id.btnAddImages);
        btnAddImages.setVisibility(View.GONE);
        MainActivity.pressBack = false;

        tvFileName = view.findViewById(R.id.tvFileName);
        gridViewImport = view.findViewById(R.id.gridViewImport);
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                chooseFile();
                openFileDialog(v);
            }

        });
        btnExtractFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //I clear the lists in case I choose several files without leaving
                importedImagesList.clear();
                importedImagesBitmaps.clear();
                listImportedImageBytes.clear();
                cbLastSeen.setChecked(false);
                cbDeleted.setChecked(false);
                if (savFile && !isJson) {

                    btnAddImages.setEnabled(true);
                    extractSavImages();
                    listActiveImages = new ArrayList<>(importedImagesList.subList(0, importedImagesList.size() - MainActivity.deletedCount - 1));
                    listActiveBitmaps = new ArrayList<>(importedImagesBitmaps.subList(0, importedImagesBitmaps.size() - MainActivity.deletedCount - 1));
                    lastSeenImage = importedImagesList.get(importedImagesList.size() - MainActivity.deletedCount - 1);
                    lastSeenBitmap = importedImagesBitmaps.get(importedImagesBitmaps.size() - MainActivity.deletedCount - 1);
                    listDeletedImages = new ArrayList<>(importedImagesList.subList(importedImagesList.size() - MainActivity.deletedCount, importedImagesList.size()));

                    listDeletedBitmaps = new ArrayList<>(importedImagesBitmaps.subList(importedImagesBitmaps.size() - MainActivity.deletedCount, importedImagesBitmaps.size()));
                    listDeletedBitmapsRedStroke = new ArrayList<>();
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(2);
                    int startX = 160;
                    int startY = 0;
                    int endX = 0;
                    int endY = 144;
                    for (Bitmap bitmap : listDeletedBitmaps) {
                        Bitmap copiedBitmap = bitmap.copy(bitmap.getConfig(), true);//Need to get a copy of the original bitmap, or else I'll paint on it
                        Canvas canvas = new Canvas(copiedBitmap);
                        canvas.drawLine(startX, startY, endX, endY, paint);
                        listDeletedBitmapsRedStroke.add(copiedBitmap);
                    }
                    tvFileName.setText(importedImagesList.size() + getString(R.string.images_available));
                    tvFileName.setText(importedImagesList.size() + getString(R.string.images_available));
                    showImages(cbLastSeen, cbDeleted);
                    btnAddImages.setText(getString(R.string.btn_add_images));
                    btnAddImages.setVisibility(View.VISIBLE);
                    layoutCb.setVisibility(View.VISIBLE);
                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
                } else if (!savFile && !isJson) {
                    btnAddImages.setEnabled(true);
                    try {
                        extractHexImages(fileContent);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    tvFileName.setText(importedImagesList.size() + getString(R.string.images_available));
                    gridViewImport.setAdapter((new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps, true, true)));
                    btnAddImages.setText(getString(R.string.btn_add_images));
                    btnAddImages.setVisibility(View.VISIBLE);
                    layoutCb.setVisibility(View.GONE);
                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
                } else if (!savFile && isJson) {
                    receivedList = JsonReader.jsonCheck(fileContent);
                    if (receivedList == null) {
                        Methods.toast(getContext(), getString(R.string.no_valid_list));
                        return;
                    }
                    switch (addEnum) {
                        case PALETTES:
                            btnAddImages.setEnabled(true);
                            customAdapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, (ArrayList<GbcPalette>) receivedList, true, true);
                            gridViewImport.setAdapter(customAdapterPalette);
                            btnAddImages.setText(getString(R.string.btn_add_palettes));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            break;

                        case FRAMES:
                            btnAddImages.setEnabled(true);
                            btnAddImages.setText(getString(R.string.btn_add_frames));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            gridViewImport.setAdapter(new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, (List<GbcFrame>) receivedList, true, true));
                            break;
                        case IMAGES:
                            btnAddImages.setEnabled(true);
                            btnAddImages.setText(getString(R.string.btn_add_images));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            gridViewImport.setAdapter(new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps, true, true));
                            break;
                    }
                }
            }
        });

        cbLastSeen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
            }
        });
        cbDeleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
            }
        });

        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (addEnum) {
                    case PALETTES:
                        btnAddImages.setEnabled(false);
                        List<GbcPalette> newPalettes = new ArrayList<>();
                        for (Object palette : receivedList) {
                            boolean alreadyAdded = false;
                            GbcPalette gbcp = (GbcPalette) palette;
                            //If the palette already exists (by the name) it doesn't add it. Same if it's already added
                            for (GbcPalette objeto : Methods.gbcPalettesList) {
                                if (objeto.getPaletteId().toLowerCase(Locale.ROOT).equals(gbcp.getPaletteId())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                newPalettes.add(gbcp);
                            }
                        }
                        if (newPalettes.size() > 0) {
                            Methods.gbcPalettesList.addAll(newPalettes);
                            for (GbcPalette gbcPalette : newPalettes) {
                                Methods.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                            }
                            new SavePaletteAsyncTask(newPalettes).execute();
                        } else {
                            Methods.toast(getContext(), getString(R.string.no_new_palettes));
                            tvFileName.setText(getString(R.string.no_new_palettes));
                        }
                        customAdapterPalette.notifyDataSetChanged();
                        break;
                    case FRAMES:
                        btnAddImages.setEnabled(false);
                        List<GbcFrame> newFrames = new ArrayList<>();
                        for (Object frame : receivedList) {
                            boolean alreadyAdded = false;
                            GbcFrame gbcFrame = (GbcFrame) frame;
                            //If the palette already exists (by the name) it doesn't add it. Same if it's already added
                            for (GbcFrame objeto : Methods.framesList) {
                                if (objeto.getFrameName().toLowerCase(Locale.ROOT).equals(gbcFrame.getFrameName())) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                newFrames.add(gbcFrame);
                            }
                        }
                        if (newFrames.size() > 0) {
                            Methods.framesList.addAll(newFrames);
                            for (GbcFrame gbcFrame : newFrames) {
                                Methods.hashFrames.put(gbcFrame.getFrameName(), gbcFrame);
                            }
                            new SaveFrameAsyncTask(newFrames).execute();//TEST THIS
                        } else {
                            Methods.toast(getContext(), getString(R.string.no_new_frames));
                            tvFileName.setText(getString(R.string.no_new_frames));
                        }
                        break;

                    case IMAGES:
                        btnAddImages.setEnabled(false);
                        numImagesAdded = 0;
                        List<GbcImage> newGbcImages = new ArrayList<>();
                        List<ImageData> newImageDatas = new ArrayList<>();
                        if (!savFile) {
                            for (int i = 0; i < importedImagesList.size(); i++) {
                                GbcImage gbcImage = importedImagesList.get(i);
                                boolean alreadyAdded = false;
                                //If the palette already exists (by the hash) it doesn't add it. Same if it's already added
                                for (GbcImage image : Methods.gbcImagesList) {
                                    if (image.getHashCode().toLowerCase(Locale.ROOT).equals(gbcImage.getHashCode())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                }
                                if (!alreadyAdded) {
                                    GbcImage.numImages++;
                                    numImagesAdded++;
                                    ImageData imageData = new ImageData();
                                    imageData.setImageId(gbcImage.getHashCode());
                                    imageData.setData(gbcImage.getImageBytes());
                                    newImageDatas.add(imageData);
                                    Methods.gbcImagesList.add(gbcImage);
                                    newGbcImages.add(gbcImage);
                                    Methods.imageBitmapCache.put(gbcImage.getHashCode(), importedImagesBitmaps.get(i));
                                }
                            }
                        } else {
                            for (int i = 0; i < finalListImages.size(); i++) {
                                GbcImage gbcImage = finalListImages.get(i);
                                boolean alreadyAdded = false;
                                //If the palette already exists (by the hash) it doesn't add it. Same if it's already added
                                for (GbcImage image : Methods.gbcImagesList) {
                                    if (image.getHashCode().toLowerCase(Locale.ROOT).equals(gbcImage.getHashCode())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                }
                                if (!alreadyAdded) {
                                    GbcImage.numImages++;
                                    numImagesAdded++;
                                    ImageData imageData = new ImageData();
                                    imageData.setImageId(gbcImage.getHashCode());
                                    imageData.setData(gbcImage.getImageBytes());
                                    newImageDatas.add(imageData);
                                    Methods.gbcImagesList.add(gbcImage);
                                    newGbcImages.add(gbcImage);
                                    Methods.imageBitmapCache.put(gbcImage.getHashCode(), finalListBitmaps.get(i));
                                }
                            }
                        }
                        if (newGbcImages.size() > 0) {
                            new SaveImageAsyncTask(newGbcImages, newImageDatas).execute();
                        } else {
                            Methods.toast(getContext(), getString(R.string.no_new_images));
                            tvFileName.setText(getString(R.string.no_new_images));
                        }
                        break;
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    //Refactor, also on UsbSerialFragment
    private void showImages(CheckBox showLastSeen, CheckBox showDeleted) {
        List<Bitmap> bitmapsAdapterList = null;
        if (!showLastSeen.isChecked() && !showDeleted.isChecked()) {
            finalListImages = new ArrayList<>(listActiveImages);
            finalListBitmaps = new ArrayList<>(listActiveBitmaps);
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);
        } else if (showLastSeen.isChecked() && !showDeleted.isChecked()) {
            finalListImages = new ArrayList<>(listActiveImages);
            finalListBitmaps = new ArrayList<>(listActiveBitmaps);
            finalListImages.add(lastSeenImage);
            finalListBitmaps.add(lastSeenBitmap);
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);

        } else if (!showLastSeen.isChecked() && showDeleted.isChecked()) {
            finalListImages = new ArrayList<>(listActiveImages);
            finalListBitmaps = new ArrayList<>(listActiveBitmaps);
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);
            finalListImages.addAll(listDeletedImages);
            finalListBitmaps.addAll(listDeletedBitmaps);
            bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke);
        } else if (showLastSeen.isChecked() && showDeleted.isChecked()) {
            finalListImages = new ArrayList<>(listActiveImages);
            finalListBitmaps = new ArrayList<>(listActiveBitmaps);
            finalListImages.add(lastSeenImage);
            finalListImages.addAll(listDeletedImages);
            finalListBitmaps.add(lastSeenBitmap);
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);
            finalListBitmaps.addAll(listDeletedBitmaps);
            bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke);
        }
        gridViewImport.setAdapter((new GalleryFragment.CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, true)));
    }

    private class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcImage> gbcImagesList;
        List<ImageData> imageDataList;

        public SaveImageAsyncTask(List<GbcImage> gbcImagesList, List<ImageData> imageDataList) {
            this.gbcImagesList = gbcImagesList;
            this.imageDataList = imageDataList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ImageDao imageDao = MainActivity.db.imageDao();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            //Need to insert first the gbcImage because of the Foreign Key
            for (GbcImage gbcImage : gbcImagesList) {
                imageDao.insert(gbcImage);
            }
            for (ImageData imageData : imageDataList) {
                imageDataDao.insert(imageData);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvFileName.setText(numImagesAdded + getString(R.string.done_adding_images));
            Methods.toast(getContext(), getString(R.string.images_added) + numImagesAdded);
        }
    }

    private class SavePaletteAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcPalette> gbcPalettesList;

        public SavePaletteAsyncTask(List<GbcPalette> gbcPalettesList) {
            this.gbcPalettesList = gbcPalettesList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PaletteDao paletteDao = MainActivity.db.paletteDao();
            for (GbcPalette gbcPalette : gbcPalettesList) {
                paletteDao.insert(gbcPalette);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvFileName.setText(getString(R.string.done_adding_palettes));
            Methods.toast(getContext(), getString(R.string.palette_added));
        }
    }

    private class SaveFrameAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcFrame> gbcFramesList;

        public SaveFrameAsyncTask(List<GbcFrame> gbcFramesList) {
            this.gbcFramesList = gbcFramesList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FrameDao frameDao = MainActivity.db.frameDao();
            for (GbcFrame gbcFrame : gbcFramesList) {
                frameDao.insert(gbcFrame);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvFileName.setText(getString(R.string.done_adding_frames));
            Methods.toast(getContext(), getString(R.string.frames_added));
        }
    }

    public void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//Any type of file
        startActivityForResult(Intent.createChooser(intent, getString(R.string.btn_select_file)), 123);
    }

    /**
     * //https://www.youtube.com/watch?v=4EKlAvjY74U&t=0s
     *
     * @param view
     */
    public void openFileDialog(View view) {
        Intent data = new Intent(Intent.ACTION_GET_CONTENT);
        data.setType("*/*");
        data = Intent.createChooser(data, "Choose a file");
        sActivityResultLauncher.launch(data);
    }

    //Method to get the filename, because the uri sometimes ended with a number
    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    ActivityResultLauncher<Intent> sActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        Methods.toast(getContext(), getFileName(uri));
//                        String[] aux = uri.getPath().split("/");
                        fileName = getFileName(uri);
                        //I check the extension of the file
                        if (fileName.endsWith("sav")) {
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
                            tvFileName.setText("Bytes: " + fileBytes.length + ". Name: " + fileName);
                            btnExtractFile.setVisibility(View.VISIBLE);
                        } else if (fileName.endsWith("txt")) {
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
                                tvFileName.setText("Bytes: " + fileBytes.length + ". Name: " + fileName);
                                btnExtractFile.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                            }
                        } else if (fileName.endsWith("json")) {
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
                                btnExtractFile.setVisibility(View.VISIBLE);

                            } catch (Exception e) {
                            }
                        } else {
                            btnExtractFile.setVisibility(View.GONE);

                            tvFileName.setText(getString(R.string.no_valid_file));
                        }
                    }
                }
            }
    );

    public void extractSavImages() {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        try {
            //Extract the images
            listImportedImageBytes = extractor.extractBytes(fileBytes);
            int nameIndex = 1;
            for (byte[] imageBytes : listImportedImageBytes) {
                GbcImage gbcImage = new GbcImage();
                String formattedIndex = String.format("%02d", nameIndex);
                if (nameIndex == listImportedImageBytes.size() - MainActivity.deletedCount) {//Last seen image
                    gbcImage.setName(fileName + " [last seen]");
                } else if (nameIndex > listImportedImageBytes.size() - MainActivity.deletedCount) {//Deleted images
                    gbcImage.setName(fileName + " [deleted]");
                } else {
                    gbcImage.setName(fileName + " " + formattedIndex);
                }
                nameIndex++;
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                String hashHex = Methods.bytesToHex(hash);
                gbcImage.setHashCode(hashHex);
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt()), 128, 112);
                Bitmap image = imageCodec.decodeWithPalette(Methods.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes);
                if (image.getHeight() == 112 && image.getWidth() == 128) {
                    //I need to use copy because if not it's inmutable bitmap
                    Bitmap framed = Methods.framesList.get(0).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(framed);
                    canvas.drawBitmap(image, 16, 16, null);
                    image = framed;
                    imageBytes = Methods.encodeImage(image);
                }
                ImageData imageData = new ImageData();
                imageData.setImageId(gbcImage.getHashCode());
                imageData.setData(imageBytes);
                importedImageDatas.add(imageData);
                gbcImage.setImageBytes(imageBytes);
                importedImagesBitmaps.add(image);
                importedImagesList.add(gbcImage);
            }
        } catch (Exception e) {
            Methods.toast(getContext(),"Error\n" + e.toString());
            e.printStackTrace();
        }
    }

    public static void extractHexImages(String fileContent) throws NoSuchAlgorithmException {
        List<String> dataList = HexToTileData.separateData(fileContent);
        String data = "";
        int index = 1;
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(bytes);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            String hashHex = Methods.bytesToHex(hash);
            gbcImage.setHashCode(hashHex);
            ImageData imageData = new ImageData();
            imageData.setImageId(hashHex);
            imageData.setData(bytes);
            importedImageDatas.add(imageData);
            String formattedIndex = String.format("%02d", index++);
            gbcImage.setName(fileName + " " + formattedIndex);
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt()), 160, height);
            Bitmap image = imageCodec.decodeWithPalette(Methods.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), gbcImage.getImageBytes());
            importedImagesBitmaps.add(image);
            importedImagesList.add(gbcImage);
        }
    }

    public static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        try {
            for (int i = 0; i < byteStrings.length; i++) {
                bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                        + Character.digit(byteStrings[i].charAt(1), 16));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
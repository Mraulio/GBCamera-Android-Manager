package com.mraulio.gbcameramanager.ui.importFile;

import static com.mraulio.gbcameramanager.MainActivity.openedFromFile;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.paletteChanger;

import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.checkPaletteColors;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.convertToGrayScale;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.ditherImage;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.from4toBw;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.has4Colors;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.resizeImage;
import static com.mraulio.gbcameramanager.ui.importFile.newpalette.NewPaletteDialog.paletteViewer;
import static com.mraulio.gbcameramanager.ui.importFile.newpalette.NewPaletteDialog.showNewPaletteDialog;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.magicIsReal;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.StaticValues.lastSeenGalleryImage;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupSorting;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.hashPalettes;
import static com.mraulio.gbcameramanager.utils.Utils.saveTypeNames;
import static com.mraulio.gbcameramanager.utils.Utils.transparencyHashSet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.ui.gallery.MainImageDialog;
import com.mraulio.gbcameramanager.ui.gallery.SaveImageAsyncTask;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.RomExtractor;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ImportFragment extends Fragment {

    static List<Bitmap> importedImagesBitmaps = new ArrayList<>();
    static List<GbcImage> importedImagesList = new ArrayList<>();
    static LinkedHashMap<GbcImage, Bitmap> importedImagesHash = new LinkedHashMap<>();
    public static LinkedHashMap<String, String> importedFrameGroupIdNames = new LinkedHashMap<>();
    int totalImages = 0;
    public static Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk;
    Spinner spSaveType;
    Bitmap importedBitmap;
    List<List<GbcImage>> listActiveImages = new ArrayList<>();
    List<List<GbcImage>> listDeletedImages = new ArrayList<>();
    List<List<Bitmap>> listDeletedBitmaps = new ArrayList<>();
    List<List<Bitmap>> listDeletedBitmapsRedStroke = new ArrayList<>();
    List<List<Bitmap>> listActiveBitmaps = new ArrayList<>();
    public static List<GbcImage> finalListImages = new ArrayList<>();
    public static List<Bitmap> finalListBitmaps = new ArrayList<>();
    List<GbcImage> lastSeenImage = new ArrayList<>();
    List<Bitmap> lastSeenBitmap = new ArrayList<>();
    DocumentFile selectedFile;
    public static List<byte[]> listImportedImageBytes = new ArrayList<>();

    byte[] fileBytes;
    private Adapter adapter;
    boolean isGoodSave = true;
    LoadingDialog loadingDialog;
    static TextView tvFileName;
    static String fileName;

    String fileContent = "";
    List<?> receivedList;
    int numImagesAdded;
    Button btnExtractFile, btnAddImages, btnTransform, btnAddPalette;
    LinearLayout lyNewPalette;
    ImageView ivNewPalette;
    CheckBox cbLastSeen, cbDeleted, cbAddFrame;
    LinearLayout layoutCb;
    CustomGridViewAdapterPalette customAdapterPalette;
    GridView gridViewImport;
    Uri uri;

    public enum ADD_WHAT {
        PALETTES,
        FRAMES,
        IMAGES
    }

    public static ADD_WHAT addEnum;

    public enum FILE_TYPE {
        SAV,
        PHOTO_ROM,
        JSON,
        TXT,
        IMAGE
    }

    public static FILE_TYPE fileType;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadingDialog = new LoadingDialog(getContext(), getContext().getString(R.string.load_extracting_images));

        if (getArguments() != null) {
            String fileUri = getArguments().getString("fileUri");
            uri = Uri.parse(fileUri);
            fileName = getFileName(uri);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);
        Button btnSelectFile = view.findViewById(R.id.btnSelectFile);

        lyNewPalette = view.findViewById(R.id.ly_new_palette);
        btnExtractFile = view.findViewById(R.id.btnExtractFile);
        ivNewPalette = view.findViewById(R.id.iv_new_palette);

        btnExtractFile.setVisibility(View.GONE);

        btnAddPalette = view.findViewById(R.id.btn_add_new_palette);

        btnTransform = view.findViewById(R.id.btn_transform_image);

        spSaveType = view.findViewById(R.id.sp_save_type_import);
        lyNewPalette.setVisibility(View.GONE);

        List saveTypes = new ArrayList();
        saveTypes.add("International");
        saveTypes.add("Japanese");
        saveTypes.add("Hello Kitty");

        ArrayAdapter<String> adapterSaveType = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, saveTypes);
        adapterSaveType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSaveType.setAdapter(adapterSaveType);

        saveTypeIntJpHk = Utils.SAVE_TYPE_INT_JP_HK.INT;
        spSaveType.setSelection(0);
        spSaveType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveTypeIntJpHk = Utils.SAVE_TYPE_INT_JP_HK.valueOf(saveTypeNames.get(saveTypes.get(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spSaveType.setVisibility(View.GONE);
        cbLastSeen = view.findViewById(R.id.cbLastSeen);
        cbDeleted = view.findViewById(R.id.cbDeletedImages);
        cbAddFrame = view.findViewById(R.id.cbAddFrame);
        layoutCb = view.findViewById(R.id.layout_cb);
        btnAddImages = view.findViewById(R.id.btnAddImages);
        btnAddImages.setVisibility(View.GONE);
        MainActivity.pressBack = false;
        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.IMPORT;

        tvFileName = view.findViewById(R.id.tvFileName);
        gridViewImport = view.findViewById(R.id.gridViewImport);

        //Showing the image big
        gridViewImport.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bitmap image = Bitmap.createScaledBitmap(finalListBitmaps.get(position), finalListBitmaps.get(position).getWidth() * 6, finalListBitmaps.get(position).getHeight() * 6, false);
                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(image);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(imageView);

                AlertDialog dialog = builder.create();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.show();
            }
        });

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileDialog(v);
            }

        });
        btnExtractFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingDialog.showDialog();
                new loadDataTask().execute();
            }
        });

        cbLastSeen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
                gridViewImport.setAdapter((ListAdapter) adapter);
            }
        });
        cbDeleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImages(cbLastSeen, cbDeleted);
                gridViewImport.setAdapter((ListAdapter) adapter);
            }
        });

        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (addEnum) {
                    case PALETTES:
                        btnAddImages.setEnabled(false);
                        btnTransform.setVisibility(View.GONE);
                        List<GbcPalette> newPalettes = new ArrayList<>();
                        for (Object palette : receivedList) {
                            boolean alreadyAdded = false;
                            GbcPalette gbcp = (GbcPalette) palette;
                            //If the palette already exists (by the name) it doesn't add it. Same if it's already added
                            for (GbcPalette objeto : Utils.gbcPalettesList) {
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
                            Utils.gbcPalettesList.addAll(newPalettes);
                            for (GbcPalette gbcPalette : newPalettes) {
                                Utils.hashPalettes.put(gbcPalette.getPaletteId(), gbcPalette);
                            }
                            new SavePaletteAsyncTask(newPalettes).execute();
                        } else {
                            Utils.toast(getContext(), getString(R.string.no_new_palettes));
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
                            //If the frame already exists (by the name) it doesn't add it. Same if it's already added
                            for (GbcFrame objeto : Utils.framesList) {
                                if (objeto.getFrameId().toLowerCase(Locale.ROOT).equals(gbcFrame.getFrameId().toLowerCase(Locale.ROOT))) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                newFrames.add(gbcFrame);
                            }
                        }
                        if (newFrames.size() > 0) {
                            Utils.framesList.addAll(newFrames);
                            for (GbcFrame gbcFrame : newFrames) {
                                Utils.hashFrames.put(gbcFrame.getFrameId(), gbcFrame);
                            }
                            new SaveFrameAsyncTask(newFrames, getContext()).execute();
                        } else {
                            Utils.toast(getContext(), getString(R.string.no_new_frames));
                            tvFileName.setText(getString(R.string.no_new_frames));
                        }
                        break;

                    case IMAGES:
                        btnAddImages.setEnabled(false);
                        numImagesAdded = 0;
                        List<GbcImage> newGbcImages = new ArrayList<>();
                        List<Bitmap> listNewBitmaps = new ArrayList<>();
                        List<String> checkDuplicatedImport = new ArrayList<>();
                        switch (fileType) {
                            case TXT:
                            case JSON: {
                                for (int i = 0; i < importedImagesList.size(); i++) {
                                    GbcImage gbcImage = importedImagesList.get(i);
                                    boolean alreadyAdded = false;
                                    //If the image already exists (by the hash) it doesn't add it. Same if it's already added
                                    for (GbcImage image : Utils.gbcImagesList) {
                                        if (image.getHashCode().equals(gbcImage.getHashCode())) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }
                                    //Now I need to check if the image is already added to the new Images (duplicated import)
                                    for (String hashDup : checkDuplicatedImport) {
                                        if (hashDup.equals(gbcImage.getHashCode())) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }
                                    if (!alreadyAdded) {
                                        newGbcImages.add(gbcImage);
                                        listNewBitmaps.add(importedImagesBitmaps.get(i));
                                        checkDuplicatedImport.add(gbcImage.getHashCode());
                                    }
                                }
                                if (newGbcImages.size() > 0) {
                                    LoadingDialog saveDialog = new LoadingDialog(getContext(), getString(R.string.load_saving_images));
                                    saveDialog.showDialog();
                                    SaveImageAsyncTask saveImageAsyncTask = new SaveImageAsyncTask(newGbcImages, listNewBitmaps, getContext(), tvFileName, numImagesAdded, null, saveDialog);
                                    saveImageAsyncTask.execute();

                                } else {
                                    Utils.toast(getContext(), getString(R.string.no_new_images));
                                    tvFileName.setText(getString(R.string.no_new_images));
                                }
                                break;
                            }
                            case IMAGE:
                            case PHOTO_ROM:
                            case SAV: {
                                if (!cbAddFrame.isChecked()) {
                                    boolean anyImageHasTransparency = false;
                                    for (int i = 0; i < finalListBitmaps.size(); i++) {
                                        HashSet transparencyHS = transparencyHashSet(finalListBitmaps.get(i));
                                        if (transparencyHS.size() > 0) {
                                            anyImageHasTransparency = true;
                                        }
                                    }
                                    if (anyImageHasTransparency) {
                                        tvFileName.setText((getString(R.string.invalid_transparent_image)));
                                    } else {
                                        for (int i = 0; i < finalListImages.size(); i++) {
                                            GbcImage gbcImage = finalListImages.get(i);
                                            boolean alreadyAdded = false;
                                            //If the image already exists (by the hash) it doesn't add it. Same if it's already added
                                            for (GbcImage image : Utils.gbcImagesList) {
                                                if (image.getHashCode().toLowerCase(Locale.ROOT).equals(gbcImage.getHashCode())) {
                                                    alreadyAdded = true;
                                                    break;
                                                }
                                            }
                                            //Now I need to check if the image is already added to the new Images (duplicated import)
                                            for (String hashDup : checkDuplicatedImport) {
                                                if (hashDup.equals(gbcImage.getHashCode())) {
                                                    alreadyAdded = true;
                                                    break;
                                                }
                                            }
                                            if (!alreadyAdded) {
                                                newGbcImages.add(gbcImage);
                                                listNewBitmaps.add(finalListBitmaps.get(i));
                                                checkDuplicatedImport.add(gbcImage.getHashCode());
                                            }
                                        }
                                        if (newGbcImages.size() > 0) {
                                            ImagesImportDialog imagesImportDialog = new ImagesImportDialog(newGbcImages, listNewBitmaps, selectedFile, getContext(), getActivity(), tvFileName, numImagesAdded);
                                            imagesImportDialog.createImagesImportDialog();

                                        } else {
                                            Utils.toast(getContext(), getString(R.string.no_new_images));
                                            tvFileName.setText(getString(R.string.no_new_images));
                                        }
                                    }

                                } else if (cbAddFrame.isChecked()) {

                                    if (importedBitmap.getHeight() != 144 && importedBitmap.getHeight() != 224) {
                                        Utils.toast(getContext(), getString(R.string.cant_add_frame));
                                        btnAddImages.setEnabled(true);
                                    } else {
                                        //Make the frame bw
                                        if (!checkPaletteColors(importedBitmap)){
                                            if (has4Colors(importedBitmap)){
                                                importedBitmap = from4toBw(importedBitmap);
                                            }else{
                                                importedBitmap = convertToGrayScale(importedBitmap);
                                                importedBitmap = ditherImage(importedBitmap);

                                            }
                                        }
                                        FrameImportDialogClass frameImportDialogClass = new FrameImportDialogClass(importedBitmap, getContext(), null, false);
                                        frameImportDialogClass.frameImportDialog();
                                    }
                                }
                                break;
                            }
                        }
                }
            }
        });

        //To directly extract the file if opening the app from a file
        if (openedFromFile) {
            selectedFile = DocumentFile.fromSingleUri(getContext(), uri);
            readFileData(uri);
            loadingDialog.showDialog();
            new loadDataTask().execute();
            openedFromFile = false;//So this doesn't execute again when entering the fragment later
        }

        // Inflate the layout for this fragment
        return view;
    }

    private void extractFile() {
        //I clear the lists in case I choose several files without leaving
        importedImagesList.clear();
        importedImagesBitmaps.clear();
        listImportedImageBytes.clear();
        listActiveImages.clear();
        listActiveBitmaps.clear();
        lastSeenImage.clear();
        lastSeenBitmap.clear();
        listDeletedImages.clear();
        listDeletedBitmaps.clear();
        listDeletedBitmapsRedStroke.clear();
        cbLastSeen.setChecked(false);
        cbDeleted.setChecked(false);
        switch (fileType) {
            case SAV: {
                isGoodSave = extractSavImages();
                if (isGoodSave) {
                    listActiveImages.add(new ArrayList<>(importedImagesList.subList(0, importedImagesList.size() - StaticValues.deletedCount[0] - 1)));
                    listActiveBitmaps.add(new ArrayList<>(importedImagesBitmaps.subList(0, importedImagesBitmaps.size() - StaticValues.deletedCount[0] - 1)));
                    lastSeenImage.add(importedImagesList.get(importedImagesList.size() - StaticValues.deletedCount[0] - 1));
                    lastSeenBitmap.add(importedImagesBitmaps.get(importedImagesBitmaps.size() - StaticValues.deletedCount[0] - 1));
                    listDeletedImages.add(new ArrayList<>(importedImagesList.subList(importedImagesList.size() - StaticValues.deletedCount[0], importedImagesList.size())));

                    listDeletedBitmaps.add(new ArrayList<>(importedImagesBitmaps.subList(importedImagesBitmaps.size() - StaticValues.deletedCount[0], importedImagesBitmaps.size())));
                    listDeletedBitmapsRedStroke = new ArrayList<>();
                    listDeletedBitmapsRedStroke.add(new ArrayList<>());

                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(2);
                    int startX = 160;
                    int startY = 0;
                    int endX = 0;
                    int endY = 144;
                    for (Bitmap bitmap : listDeletedBitmaps.get(0)) {
                        Bitmap copiedBitmap = bitmap.copy(bitmap.getConfig(), true);//Need to get a copy of the original bitmap, or else I'll paint on it
                        Canvas canvas = new Canvas(copiedBitmap);
                        canvas.drawLine(startX, startY, endX, endY, paint);
                        listDeletedBitmapsRedStroke.get(0).add(copiedBitmap);
                    }

                    showImages(cbLastSeen, cbDeleted);
                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
                }
                break;
            }
            case PHOTO_ROM:
                RomExtractor romExtractor = new RomExtractor(fileBytes, fileName);
                romExtractor.romExtract(saveTypeIntJpHk);
                listActiveImages = romExtractor.getListActiveImages();
                listActiveBitmaps = romExtractor.getListActiveBitmaps();
                lastSeenImage = romExtractor.getLastSeenImage();
                lastSeenBitmap = romExtractor.getLastSeenBitmap();
                listDeletedImages = romExtractor.getListDeletedImages();
                listDeletedBitmaps = romExtractor.getListDeletedBitmaps();
                listDeletedBitmapsRedStroke = romExtractor.getListDeletedBitmapsRedStroke();
                totalImages = romExtractor.getTotalImages();
                showImages(cbLastSeen, cbDeleted);
                ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
            case IMAGE: {
                break;
            }
            case TXT: {
                try {
                    extractHexImages(fileContent);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                adapter = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps, true, true, false, null);

                ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
                break;
            }
            case JSON: {
                receivedList = JsonReader.jsonCheck(fileContent);
                if (receivedList == null) {
                    Utils.toast(getContext(), getString(R.string.no_valid_list));
                    return;
                }
                break;
            }
        }
    }

    private class loadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                extractFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            switch (fileType) {
                case PHOTO_ROM:
                case SAV:
                    if (!isGoodSave) {
                        tvFileName.setText(getString(R.string.no_valid_file));
                        loadingDialog.dismissDialog();
                        adapter = null;
                        gridViewImport.setAdapter((ListAdapter) adapter);
                        btnAddImages.setVisibility(View.GONE);
                        layoutCb.setVisibility(View.GONE);
                        return;
                    }
                    btnAddImages.setEnabled(true);
                    tvFileName.setText(totalImages + getString(R.string.images_available));
                    btnAddImages.setText(getString(R.string.btn_add_images));
                    btnAddImages.setVisibility(View.VISIBLE);
                    layoutCb.setVisibility(View.VISIBLE);
                    break;

                case TXT: {
                    btnAddImages.setEnabled(true);
                    tvFileName.setText(totalImages + getString(R.string.images_available));
                    btnAddImages.setText(getString(R.string.btn_add_images));
                    btnAddImages.setVisibility(View.VISIBLE);
                    layoutCb.setVisibility(View.GONE);
                    break;
                }
                case JSON: {
                    switch (addEnum) {
                        case PALETTES:
                            btnAddImages.setEnabled(true);
                            adapter = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, (ArrayList<GbcPalette>) receivedList, true, true, true);
                            customAdapterPalette = (CustomGridViewAdapterPalette) adapter;
                            btnAddImages.setText(getString(R.string.btn_add_palettes));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            break;

                        case FRAMES:
                            btnAddImages.setEnabled(true);
                            btnAddImages.setText(getString(R.string.btn_add_frames));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            adapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, (List<GbcFrame>) receivedList, true, true);
                            break;
                        case IMAGES:
                            btnAddImages.setEnabled(true);
                            btnAddImages.setText(getString(R.string.btn_add_images));
                            btnAddImages.setVisibility(View.VISIBLE);
                            layoutCb.setVisibility(View.GONE);
                            adapter = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, importedImagesList, importedImagesBitmaps, true, true, false, null);
                            break;
                    }
                    break;
                }
            }

            gridViewImport.setAdapter((ListAdapter) adapter);
            loadingDialog.dismissDialog();

        }
    }

    //Refactor this with UsbSerialFragment
    public void showImages(CheckBox showLastSeen, CheckBox showDeleted) {
        List<Bitmap> bitmapsAdapterList = new ArrayList<>();
        finalListImages.clear();
        finalListBitmaps.clear();
        if (!showLastSeen.isChecked() && !showDeleted.isChecked()) {
            for (List<GbcImage> gbcImageList : listActiveImages) {
                finalListImages.addAll(gbcImageList);
            }
            for (List<Bitmap> bitmapList : listActiveBitmaps) {
                finalListBitmaps.addAll(bitmapList);
            }
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);

        } else if (showLastSeen.isChecked() && !showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.add(lastSeenImage.get(i));
            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                finalListBitmaps.add(lastSeenBitmap.get(i));
            }
            bitmapsAdapterList = new ArrayList<>(finalListBitmaps);

        } else if (!showLastSeen.isChecked() && showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.addAll(listDeletedImages.get(i));
            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                bitmapsAdapterList.addAll(listActiveBitmaps.get(i));
                finalListBitmaps.addAll(listDeletedBitmaps.get(i));
                bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke.get(i));

            }
        } else if (showLastSeen.isChecked() && showDeleted.isChecked()) {
            for (int i = 0; i < listActiveImages.size(); i++) {
                finalListImages.addAll(listActiveImages.get(i));
                finalListImages.add(lastSeenImage.get(i));
                finalListImages.addAll(listDeletedImages.get(i));

            }
            for (int i = 0; i < listActiveBitmaps.size(); i++) {
                finalListBitmaps.addAll(listActiveBitmaps.get(i));
                bitmapsAdapterList.addAll(listActiveBitmaps.get(i));

                finalListBitmaps.add(lastSeenBitmap.get(i));
                bitmapsAdapterList.add(lastSeenBitmap.get(i));

                finalListBitmaps.addAll(listDeletedBitmaps.get(i));
                bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke.get(i));
            }
        }

        adapter = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, true, false, null);
    }


    private class SavePaletteAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcPalette> gbcPalettesList;

        public SavePaletteAsyncTask(List<GbcPalette> gbcPalettesList) {
            this.gbcPalettesList = gbcPalettesList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PaletteDao paletteDao = StaticValues.db.paletteDao();
            for (GbcPalette gbcPalette : gbcPalettesList) {
                paletteDao.insert(gbcPalette);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvFileName.setText(getString(R.string.done_adding_palettes));
            Utils.toast(getContext(), getString(R.string.palette_added));
        }
    }

    public static class SaveFrameAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcFrame> gbcFramesList;
        Context context;

        public SaveFrameAsyncTask(List<GbcFrame> gbcFramesList, Context context) {
            this.gbcFramesList = gbcFramesList;
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FrameDao frameDao = StaticValues.db.frameDao();
            for (GbcFrame gbcFrame : gbcFramesList) {
                frameDao.insert(gbcFrame);
            }
            GbcFrame frameWithFrameGroupNames = hashFrames.get("gbcam01");
            frameGroupsNames.putAll(importedFrameGroupIdNames);
            frameWithFrameGroupNames.setFrameGroupsNames(frameGroupsNames);
            frameDao.update(frameWithFrameGroupNames);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Sort the frame list by id and group
            frameGroupSorting();
            tvFileName.setText(context.getString(R.string.done_adding_frames));
            Utils.toast(context, context.getString(R.string.frames_added));
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
        data.addCategory(Intent.CATEGORY_OPENABLE);

        data.setType("*/*");
        data.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

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
                        cbAddFrame.setVisibility(View.GONE);
                        lyNewPalette.setVisibility(View.GONE);
                        btnTransform.setVisibility(View.GONE);
                        cbAddFrame.setChecked(false);
                        Intent data = result.getData();

                        if (data != null) {
                            if (data.getClipData() != null) {
                                // Multiple files were selected
                                int count = data.getClipData().getItemCount();
                                boolean allFilesValid = true;

                                for (int i = 0; i < count; i++) {
                                    Uri uri = data.getClipData().getItemAt(i).getUri();

                                    //Verify if it's a valid image
                                    if (!isImageFile(getFileName(uri))) {
                                        allFilesValid = false;
                                        break; //Leave the loop if it finds a non valid file
                                    }
                                }

                                if (allFilesValid) {
                                    List<Uri> uris = new ArrayList<>();

                                    //Order by name
                                    for (int i = 0; i < count; i++) {
                                        Uri uri = data.getClipData().getItemAt(i).getUri();
                                        uris.add(uri);
                                    }
                                    Collections.sort(uris, new Comparator<Uri>() {
                                        @Override
                                        public int compare(Uri uri1, Uri uri2) {
                                            String path1 = getFileName(uri1);
                                            String path2 = getFileName(uri2);

                                            return path1.compareTo(path2);
                                        }
                                    });

                                    finalListImages.clear();
                                    finalListBitmaps.clear();
                                    tvFileName.setText("Selected files: " + count);
                                    for (int i = 0; i < count; i++) {
                                        Uri uri = uris.get(i);

                                        fileName = getFileName(uri);
                                        fileType = FILE_TYPE.IMAGE;
                                        selectedFile = null;//Passing it as null to not get the name and modified date
                                        try {
                                            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);

                                            BitmapFactory.Options options = new BitmapFactory.Options();
                                            options.inJustDecodeBounds = true;
                                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                                            GbcImage gbcImage = new GbcImage();
                                            bitmap = resizeImage(bitmap, gbcImage);


                                            byte[] imageBytes = Utils.encodeImage(bitmap, "bw");
                                            gbcImage.setImageBytes(imageBytes);
                                            byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                                            String hashHex = Utils.bytesToHex(hash);
                                            gbcImage.setHashCode(hashHex);

                                            gbcImage.setName(fileName);
                                            finalListBitmaps.add(bitmap);
                                            finalListImages.add(gbcImage);
//
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                    btnExtractFile.setVisibility(View.GONE);
                                    spSaveType.setVisibility(View.GONE);
                                    btnAddImages.setVisibility(View.VISIBLE);
                                    btnAddImages.setEnabled(true);
                                    adapter = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, finalListBitmaps, true, true, false, null);
                                    gridViewImport.setAdapter((ListAdapter) adapter);
                                    ImportFragment.addEnum = ImportFragment.ADD_WHAT.IMAGES;
                                    gridViewImport.setAdapter((ListAdapter) adapter);

                                } else {
                                    // Show error message
                                    tvFileName.setText(getString(R.string.import_valid_images));

                                }
                            } else if (data.getData() != null) {
                                uri = data.getData();
                                selectedFile = DocumentFile.fromSingleUri(getContext(), uri);
                                fileName = getFileName(uri);
                                readFileData(uri);
                            }
                        }
                    }
                }
            }
    );

    private void readFileData(Uri uri) {

        //I check the extension of the file
        if (fileName.toLowerCase().endsWith("sav")) {
            ByteArrayOutputStream byteStream = null;
            fileType = FILE_TYPE.SAV;

            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                byteStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteStream.write(buffer, 0, len);
                }
                byteStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            fileBytes = byteStream.toByteArray();
            tvFileName.setText(getString(R.string.file_name) + fileName);
            btnExtractFile.setVisibility(View.VISIBLE);
            spSaveType.setVisibility(View.VISIBLE);

        } else if (fileName.toLowerCase().endsWith("gbc")) {
            ByteArrayOutputStream byteStream = null;
            fileType = FILE_TYPE.PHOTO_ROM;

            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                byteStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteStream.write(buffer, 0, len);
                }
                byteStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            fileBytes = byteStream.toByteArray();
            tvFileName.setText(getString(R.string.file_name) + fileName);
            btnExtractFile.setVisibility(View.VISIBLE);
            spSaveType.setVisibility(View.VISIBLE);
        } else if (fileName.toLowerCase().endsWith("txt")) {
            fileType = FILE_TYPE.TXT;
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
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
                tvFileName.setText(getString(R.string.file_name) + fileName);
                btnExtractFile.setVisibility(View.VISIBLE);
            } catch (Exception e) {
            }
        } else if (fileName.toLowerCase().endsWith("json")) {
            fileType = FILE_TYPE.JSON;

            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
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
                tvFileName.setText(getString(R.string.file_name) + fileName);
                btnExtractFile.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (isImageFile(fileName)) {
            fileType = FILE_TYPE.IMAGE;
            finalListImages.clear();
            finalListBitmaps.clear();
            try {
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);

                boolean showTransformButton = true;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                importedBitmap = BitmapFactory.decodeStream(inputStream);

                GbcImage gbcImage = new GbcImage();
                Bitmap bitmap = resizeImage(importedBitmap, gbcImage);

                byte[] imageBytes = Utils.encodeImage(bitmap, "bw");
                gbcImage.setImageBytes(imageBytes);
                int[] newPaletteColors = getImagePalette(importedBitmap, lyNewPalette, ivNewPalette);

                if (newPaletteColors != null) {
                    btnAddPalette.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Show the Add new palette dialog
                            showNewPaletteDialog(getContext(), newPaletteColors);
                        }
                    });
                }

                bitmap = paletteChanger(gbcImage.getPaletteId(), imageBytes, gbcImage.isInvertPalette());
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                String hashHex = Utils.bytesToHex(hash);
                gbcImage.setHashCode(hashHex);
                gbcImage.setName(fileName);
                finalListBitmaps.add(bitmap);
                finalListImages.add(gbcImage);
                tvFileName.setText(getString(R.string.file_name) + fileName);
                btnExtractFile.setVisibility(View.GONE);
                spSaveType.setVisibility(View.GONE);
                btnAddImages.setVisibility(View.VISIBLE);
                btnAddImages.setEnabled(true);

                adapter = new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, finalListBitmaps, true, true, false, null);
                gridViewImport.setAdapter((ListAdapter) adapter);
                addEnum = ImportFragment.ADD_WHAT.IMAGES;
                cbAddFrame.setVisibility(View.VISIBLE);

                final List<Bitmap> bitmapHolder = new ArrayList<>();
                bitmapHolder.add(bitmap);

                if (showTransformButton) {
                    btnTransform.setVisibility(View.VISIBLE);
                    btnTransform.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TransformImage transformImage = new TransformImage(importedBitmap, gbcImage, getContext(), gridViewImport);
                            transformImage.createTransformDialog();

                        }
                    });
                }

                gridViewImport.setAdapter((ListAdapter) adapter);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            btnExtractFile.setVisibility(View.GONE);
            spSaveType.setVisibility(View.GONE);
            tvFileName.setText(getString(R.string.no_valid_file));
        }
    }

    public static int[] getImagePalette(Bitmap bitmap, LinearLayout lyPalette, ImageView ivNewPalette) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        LinkedHashSet<Integer> colorSet = new LinkedHashSet<>();
        for (int pixel : pixels) {
            colorSet.add(pixel);
            if (colorSet.size() > 4) {
                return null;
            }
        }

        int[] sortedColorsIntArray = new int[4];

        if (colorSet.size() == 4) {
            List<Integer> sortedColors = new ArrayList<>(colorSet);
            Collections.sort(sortedColors, new Comparator<Integer>() {
                @Override
                public int compare(Integer color1, Integer color2) {
                    double luminance1 = calculateLuminance(color1);
                    double luminance2 = calculateLuminance(color2);
                    return Double.compare(luminance2, luminance1);
                }
            });

            for (int i = 0; i < sortedColors.size(); i++) {
                sortedColorsIntArray[i] = sortedColors.get(i);
            }

            Bitmap paletteBitmap = paletteViewer(sortedColorsIntArray);
            lyPalette.setVisibility(View.VISIBLE);
            ivNewPalette.setImageBitmap(paletteBitmap);
            return sortedColorsIntArray;
        }
        return null;
    }

    public static double calculateLuminance(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance;
    }


    /**
     * Checks if the selected file are images
     *
     * @return true if all are images
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        String[] validExtensions = {".jpg", ".jpeg", ".png", ".bmp"};
        for (String extension : validExtensions) {
            if (fileName.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public boolean extractSavImages() {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));

        //Check for Magic or FF bytes
        if (!magicIsReal(fileBytes)) {
            return false;
        }
        //Extract the images
        importedImagesHash = extractor.extractGbcImages(fileBytes, fileName, 0, saveTypeIntJpHk);

        for (HashMap.Entry<GbcImage, Bitmap> entry : importedImagesHash.entrySet()) {
            GbcImage gbcImage = entry.getKey();
            Bitmap imageBitmap = entry.getValue();
            ImageData imageData = new ImageData();
            imageData.setImageId(gbcImage.getHashCode());
            imageData.setData(gbcImage.getImageBytes());
            importedImagesBitmaps.add(imageBitmap);
            importedImagesList.add(gbcImage);
        }
        totalImages = importedImagesList.size();

        return true;
    }

    private void extractHexImages(String fileContent) throws NoSuchAlgorithmException {
        List<String> dataList = HexToTileData.separateData(fileContent);
        String data = "";
        int index = 1;
        for (String string : dataList) {
            data = string.replaceAll(System.lineSeparator(), " ");
            byte[] bytes = convertToByteArray(data);
            GbcImage gbcImage = new GbcImage();
            gbcImage.setImageBytes(bytes);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            String hashHex = Utils.bytesToHex(hash);
            gbcImage.setHashCode(hashHex);
            ImageData imageData = new ImageData();
            imageData.setImageId(hashHex);
            imageData.setData(bytes);
//            importedImageDatas.add(imageData);
            totalImages = index;
            String formattedIndex = String.format("%02d", index++);
            gbcImage.setName(fileName + " " + formattedIndex);
            int height = (data.length() + 1) / 120;//To get the real height of the image
            ImageCodec imageCodec = new ImageCodec(160, height);
            Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), gbcImage.getImageBytes(), false);
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
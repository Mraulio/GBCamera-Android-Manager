package com.mraulio.gbcameramanager.ui.savemanager;

import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.ui.usbserial.UsbSerialUtils.magicIsReal;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.saveTypeNames;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.utils.LoadingDialog;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class SaveManagerFragment extends Fragment {
    private ListView listView;
    private GridView gridviewSaves;
    private List<String> fileList;
    private String saveName;
    private Button btnDelete, btnAdd;
    Switch swModDate;
    static Utils.SAVE_TYPE_INT_JP_HK saveTypeIntJpHk;

    Spinner spSaveType;
    private File selectedFile;
    LoadingDialog loadingDialog;
    private CustomGridViewAdapterImage gridAdapter;
    private ArrayAdapter<String> listViewAdapter;
    private int numImagesAdded = 0;
    private int currentPosition = 0;
    List<GbcImage> listActiveImages = new ArrayList<>();
    List<Bitmap> listActiveBitmaps = new ArrayList<>();
    List<GbcImage> listDeletedImages = new ArrayList<>();
    List<Bitmap> listDeletedBitmaps = new ArrayList<>();
    List<Bitmap> listDeletedBitmapsRedStroke = new ArrayList<>();
    List<GbcImage> finalListImages = new ArrayList<>();
    List<Bitmap> finalListBitmaps = new ArrayList<>();
    GbcImage lastSeenImage;
    Bitmap lastSeenBitmap;
    List<Bitmap> extractedImagesBitmaps = new ArrayList<>();
    List<GbcImage> extractedImagesList = new ArrayList<>();
    List<Bitmap> bitmapsAdapterList = null;
    Date lastModDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_save_manager, container, false);
        listView = view.findViewById(R.id.listView);
        gridviewSaves = view.findViewById(R.id.gridViewSaves);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnAdd = view.findViewById(R.id.btnAdd);
        swModDate = view.findViewById(R.id.sw_mod_date);
        loadingDialog = new LoadingDialog(getContext(), getContext().getString(R.string.load_extracting_images));
        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.SAVE_MANAGER;

        spSaveType = view.findViewById(R.id.sp_save_type_manager);
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

        fileList = new ArrayList<>();
        loadFileNames();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentPosition = position;
                loadingDialog.showDialog();
                saveName = (String) parent.getItemAtPosition(position);
                selectedFile = new File(Utils.SAVE_FOLDER, saveName);
                String loc = "";
                lastModDate = new Date(selectedFile.lastModified());
                if (dateLocale.equals("yyyy-MM-dd")){
                    loc = "dd-MM-yyyy";
                }else {
                    loc = "MM-dd-yyyy";
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat(loc+" HH-mm-ss", Locale.getDefault());
                String dateString = dateFormat.format(lastModDate);
                swModDate.setText(getString(R.string.cb_use_mod_date) + ": " + dateString);
                new loadDataTask().execute();
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDeleteDialog();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<GbcImage> newGbcImages = new ArrayList<>();
//                List<ImageData> newImageDatas = new ArrayList<>();
                for (int i = 0; i < extractedImagesList.size(); i++) {
                    GbcImage gbcImage = extractedImagesList.get(i);
                    boolean alreadyAdded = false;
                    //If the image already exists (by the hash) it doesn't add it. Same if it's already added
                    for (GbcImage image : Utils.gbcImagesList) {
                        if (image.getHashCode().toLowerCase(Locale.ROOT).equals(gbcImage.getHashCode())) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {

                        newGbcImages.add(gbcImage);
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), extractedImagesBitmaps.get(i));
                    }
                }
                if (newGbcImages.size() > 0) {
                    new SaveImageAsyncTask(newGbcImages).execute();
                } else {
                    Utils.toast(getContext(), getString(R.string.no_new_images));
                }

            }
        });

        return view;
    }

    private class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
        List<GbcImage> gbcImagesList;

        public SaveImageAsyncTask(List<GbcImage> gbcImagesList) {
            this.gbcImagesList = gbcImagesList;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<GbcImage> newGbcImages = new ArrayList<>();
            List<ImageData> newImageDatas = new ArrayList<>();
            int dateIndex = 0;
            for (int i = 0; i < gbcImagesList.size(); i++) {
                GbcImage gbcImage = gbcImagesList.get(i);
                if (swModDate.isChecked()) {
                    long lastModifiedTime = selectedFile.lastModified() + dateIndex++;
                    lastModDate = new Date(lastModifiedTime);
                    gbcImage.setCreationDate(lastModDate);
                }
                ImageData imageData = new ImageData();
                imageData.setImageId(gbcImage.getHashCode());
                imageData.setData(gbcImage.getImageBytes());
                newImageDatas.add(imageData);
                Utils.gbcImagesList.add(gbcImage);
                newGbcImages.add(gbcImage);
                GbcImage.numImages++;
                numImagesAdded++;
                GalleryFragment.diskCache.put(gbcImage.getHashCode(), Utils.imageBitmapCache.get(gbcImage.getHashCode()));
            }

            ImageDao imageDao = StaticValues.db.imageDao();
            ImageDataDao imageDataDao = StaticValues.db.imageDataDao();

            //Need to insert first the gbcImage because of the Foreign Key
            imageDao.insertManyImages(newGbcImages);
            imageDataDao.insertManyDatas(newImageDatas);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            retrieveTags(gbcImagesList);
            Utils.toast(getContext(), getString(R.string.images_added) + numImagesAdded);
            numImagesAdded = 0;
        }
    }

    private void loadFileNames() {
        try {
            fileList.clear();
            File[] files = Utils.SAVE_FOLDER.listFiles();

            List<File> fileListWithDate = Arrays.asList(files);
            Collections.sort(fileListWithDate, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });

            for (File file : fileListWithDate) {
                fileList.add(file.getName());
            }

            listViewAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_1, fileList);
            listView.setAdapter(listViewAdapter);
            listView.setSelection(currentPosition);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class loadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                readSav(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            loadingDialog.dismissDialog();
            if (lastSeenImage != null) {
                showImages(gridviewSaves, gridAdapter);
                btnAdd.setEnabled(true);

            } else {
                gridviewSaves.setAdapter(null);
                btnAdd.setEnabled(false);
            }
            btnDelete.setEnabled(true);
        }
    }

    private void readSav(int saveBank) {
        extractedImagesBitmaps.clear();
        extractedImagesList.clear();
        listActiveImages.clear();
        listActiveBitmaps.clear();
        lastSeenImage = null;
        lastSeenBitmap = null;
        listDeletedImages.clear();

        listDeletedBitmaps.clear();
        listDeletedBitmapsRedStroke.clear();
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        //I get the last file from the directory, which I just dumped
        try {
            if (selectedFile.length() / 1024 == 128) {

                byte[] extractedImageBytes = new byte[0];
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    extractedImageBytes = new byte[(int) selectedFile.length()];
                    fis.read(extractedImageBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Check for Magic or FF bytes
                if (!magicIsReal(extractedImageBytes)) {
                    //
                } else {
                    String fileName = selectedFile.getName();

                    LinkedHashMap<GbcImage, Bitmap> importedImagesHash = extractor.extractGbcImages(extractedImageBytes, fileName, 0, saveTypeIntJpHk);

                    for (HashMap.Entry<GbcImage, Bitmap> entry : importedImagesHash.entrySet()) {
                        GbcImage gbcImage = entry.getKey();
                        Bitmap imageBitmap = entry.getValue();
                        ImageData imageData = new ImageData();
                        imageData.setImageId(gbcImage.getHashCode());
                        imageData.setData(gbcImage.getImageBytes());
                        extractedImagesBitmaps.add(imageBitmap);
                        extractedImagesList.add(gbcImage);
                    }

                    listActiveImages = new ArrayList<>(extractedImagesList.subList(0, extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1));
                    listActiveBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(0, extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1));
                    lastSeenImage = extractedImagesList.get(extractedImagesList.size() - StaticValues.deletedCount[saveBank] - 1);
                    lastSeenBitmap = extractedImagesBitmaps.get(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank] - 1);
                    listDeletedImages = new ArrayList<>(extractedImagesList.subList(extractedImagesList.size() - StaticValues.deletedCount[saveBank], extractedImagesList.size()));

                    listDeletedBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(extractedImagesBitmaps.size() - StaticValues.deletedCount[saveBank], extractedImagesBitmaps.size()));
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
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Refactor
    private void showImages(GridView gridView, CustomGridViewAdapterImage gridAdapterSI) {

        finalListImages = new ArrayList<>(listActiveImages);
        finalListBitmaps = new ArrayList<>(listActiveBitmaps);
        finalListImages.add(lastSeenImage);
        finalListImages.addAll(listDeletedImages);
        finalListBitmaps.add(lastSeenBitmap);
        bitmapsAdapterList = new ArrayList<>(finalListBitmaps);
        finalListBitmaps.addAll(listDeletedBitmaps);
        bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke);

        gridView.setAdapter((new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, true, false, null)));
        gridAdapterSI = (CustomGridViewAdapterImage) gridviewSaves.getAdapter();
    }

    private void createDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.delete) + " " + saveName + "?");

        GridView grid = new GridView(getContext());
        grid.setNumColumns(4);
        grid.setPadding(30, 10, 30, 10);
        if (lastSeenImage != null) {
            showImages(grid, gridAdapter);
        } else {
            grid.setAdapter(null);
        }

        builder.setView(grid);

        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Try to delete the file
                if (selectedFile.delete()) {
                    Utils.toast(getContext(), getString(R.string.toast_sav_deleted));
                    if (gridAdapter != null) {
                        gridAdapter.clear();
                        gridAdapter.notifyDataSetChanged();
                    }
                    loadFileNames();
                    listViewAdapter.notifyDataSetChanged();
                    btnDelete.setEnabled(false);
                } else {
                    Utils.toast(getContext(), getString(R.string.toast_couldnt_delete_sav));
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //No action
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
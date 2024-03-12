package com.mraulio.gbcameramanager.ui.savemanager;

import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

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
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ListView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.ImageData;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaveManagerFragment extends Fragment {
    private ListView listView;
    private GridView gridviewSaves;
    private List<String> fileList;
    private String saveName;
    private Button btnDelete, btnAdd;
    private CheckBox cbModDate;
    private AlertDialog loadingDialog;
    private File selectedFile;
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
        cbModDate =view.findViewById(R.id.cbModDate);
        loadingDialog = Utils.loadingDialog(getContext());
        MainActivity.currentFragment = MainActivity.CURRENT_FRAGMENT.SAVE_MANAGER;


        fileList = new ArrayList<>();
        loadFileNames();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentPosition = position;
                loadingDialog.show();
                saveName = (String) parent.getItemAtPosition(position);
                selectedFile = new File(Utils.SAVE_FOLDER, saveName);
                lastModDate = new Date(selectedFile.lastModified());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.getDefault());
                String dateString = dateFormat.format(lastModDate);
                cbModDate.setText(getString(R.string.cb_use_mod_date)+": "+dateString);
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
                List<ImageData> newImageDatas = new ArrayList<>();
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
                        GbcImage.numImages++;
                        numImagesAdded++;
                        ImageData imageData = new ImageData();
                        imageData.setImageId(gbcImage.getHashCode());
                        imageData.setData(gbcImage.getImageBytes());
                        newImageDatas.add(imageData);
                        Utils.gbcImagesList.add(gbcImage);
                        Utils.gbcImagesListHolder.add(gbcImage);

                        newGbcImages.add(gbcImage);
                        Utils.imageBitmapCache.put(gbcImage.getHashCode(), extractedImagesBitmaps.get(i));
                    }
                }
                if (newGbcImages.size() > 0) {
                    new SaveImageAsyncTask(newGbcImages, newImageDatas).execute();
                    retrieveTags(gbcImagesList);
                } else {
                    Utils.toast(getContext(), getString(R.string.no_new_images));
                }

            }
        });

        return view;
    }

    //Refactor this, already in Import and USB Serial Fragment!!
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
            try {
                int dateIndex = 0;
                for (GbcImage gbcImage : gbcImagesList) {
                    if (cbModDate.isChecked()){
                        long lastModifiedTime = selectedFile.lastModified()+dateIndex++;
                        lastModDate = new Date(lastModifiedTime);
                        gbcImage.setCreationDate(lastModDate);
                    }
                    imageDao.insert(gbcImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                for (ImageData imageData : imageDataList) {
                    imageDataDao.insert(imageData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Utils.toast(getContext(), getString(R.string.images_added) + numImagesAdded);
        }
    }

    private void loadFileNames() {
        try {
            fileList.clear();
            File[] files = Utils.SAVE_FOLDER.listFiles();
            for (File file : files) {
                fileList.add(file.getName());
            }

            //Sort by name and reverse to show last date first
            Collections.sort(fileList, String.CASE_INSENSITIVE_ORDER);
            Collections.reverse(fileList);

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

            loadingDialog.dismiss();
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
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        //I get the last file from the directory, which I just dumped
        try {
            if (selectedFile.length() / 1024 == 128) {
                List<byte[]> listExtractedImageBytes = new ArrayList<>();

                listExtractedImageBytes = extractor.extractBytes(selectedFile, saveBank);
                int nameIndex = 1;
                String fileName = selectedFile.getName();
                for (byte[] imageBytes : listExtractedImageBytes) {
                    GbcImage gbcImage = new GbcImage();

                    String formattedIndex = String.format("%02d", nameIndex);
                    if (nameIndex == listExtractedImageBytes.size() - MainActivity.deletedCount[saveBank]) {//Last seen image
                        gbcImage.setName(fileName + " [last seen]");
                    } else if (nameIndex > listExtractedImageBytes.size() - MainActivity.deletedCount[saveBank]) {//Deleted images
                        gbcImage.setName(fileName + " [deleted]");
                    } else {
                        gbcImage.setName(fileName + " " + formattedIndex);
                    }
                    nameIndex++;
                    byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                    String hashHex = Utils.bytesToHex(hash);
                    gbcImage.setHashCode(hashHex);
                    ImageCodec imageCodec = new ImageCodec(128, 112, false);
                    Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes, false);
                    if (image.getHeight() == 112 && image.getWidth() == 128) {
                        //I need to use copy because if not it's inmutable bitmap
                        Bitmap framed = Utils.hashFrames.get((gbcImage.getFrameId())).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(framed);
                        canvas.drawBitmap(image, 16, 16, null);
                        image = framed;
                        imageBytes = Utils.encodeImage(image, "bw");
                    }
                    gbcImage.setImageBytes(imageBytes);
                    extractedImagesBitmaps.add(image);
                    extractedImagesList.add(gbcImage);
                }
                listActiveImages = new ArrayList<>(extractedImagesList.subList(0, extractedImagesList.size() - MainActivity.deletedCount[saveBank] - 1));
                listActiveBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(0, extractedImagesBitmaps.size() - MainActivity.deletedCount[saveBank] - 1));
                lastSeenImage = extractedImagesList.get(extractedImagesList.size() - MainActivity.deletedCount[saveBank] - 1);
                lastSeenBitmap = extractedImagesBitmaps.get(extractedImagesBitmaps.size() - MainActivity.deletedCount[saveBank] - 1);
                listDeletedImages = new ArrayList<>(extractedImagesList.subList(extractedImagesList.size() - MainActivity.deletedCount[saveBank], extractedImagesList.size()));

                listDeletedBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(extractedImagesBitmaps.size() - MainActivity.deletedCount[saveBank], extractedImagesBitmaps.size()));
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
            } else {
                listActiveImages.clear();
                listActiveBitmaps.clear();
                lastSeenImage = null;
                lastSeenBitmap = null;
                listDeletedImages.clear();

                listDeletedBitmaps.clear();
                listDeletedBitmapsRedStroke.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
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
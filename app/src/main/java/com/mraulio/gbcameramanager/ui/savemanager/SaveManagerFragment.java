package com.mraulio.gbcameramanager.ui.savemanager;

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

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.gallery.CustomGridViewAdapterImage;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SaveManagerFragment extends Fragment {
    private ListView listView;
    private GridView gridviewSaves;
    private List<String> fileList;
    private String saveName;
    private Button btnDelete;
    private AlertDialog loadingDialog;
    private File selectedFile;
    private CustomGridViewAdapterImage gridAdapter;
    private ArrayAdapter<String> listViewAdapter;

    List<GbcImage> listActiveImages = new ArrayList<>();
    List<Bitmap> listActiveBitmaps = new ArrayList<>();
    List<GbcImage> listDeletedImages;
    List<Bitmap> listDeletedBitmaps;
    List<Bitmap> listDeletedBitmapsRedStroke;
    List<GbcImage> finalListImages;
    List<Bitmap> finalListBitmaps;
    GbcImage lastSeenImage;
    Bitmap lastSeenBitmap;
    List<Bitmap> extractedImagesBitmaps = new ArrayList<>();
    List<GbcImage> extractedImagesList = new ArrayList<>();
    List<Bitmap> bitmapsAdapterList = null;

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
        loadingDialog = Utils.loadingDialog(getContext());
        MainActivity.current_fragment = MainActivity.CURRENT_FRAGMENT.SAVE_MANAGER;


        fileList = new ArrayList<>();
        loadFileNames();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                loadingDialog.show();
                saveName = (String) parent.getItemAtPosition(position);

                selectedFile = new File(Utils.SAVE_FOLDER, saveName);
                new loadDataTask().execute();
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog();
            }
        });
        return view;
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

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class loadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                readSav();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            loadingDialog.dismiss();
            showImages();
            btnDelete.setEnabled(true);
        }
    }

    private void readSav() {
        extractedImagesBitmaps.clear();
        extractedImagesList.clear();
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        LocalDateTime now = LocalDateTime.now();
        //I get the last file from the directory, which I just dumped
        try {
            if (selectedFile.length() / 1024 == 128) {
                List<byte[]> listExtractedImageBytes = new ArrayList<>();

                listExtractedImageBytes = extractor.extractBytes(selectedFile);
                int nameIndex = 1;
                String fileName = selectedFile.getName();
                for (byte[] imageBytes : listExtractedImageBytes) {
                    GbcImage gbcImage = new GbcImage();
                    String formattedIndex = String.format("%02d", nameIndex);
                    if (nameIndex == listExtractedImageBytes.size() - MainActivity.deletedCount) {//Last seen image
                        gbcImage.setName(fileName + " [last seen]");
                    } else if (nameIndex > listExtractedImageBytes.size() - MainActivity.deletedCount) {//Deleted images
                        gbcImage.setName(fileName + " [deleted]");
                    } else {
                        gbcImage.setName(fileName + " " + formattedIndex);
                    }
                    nameIndex++;
                    byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageBytes);
                    String hashHex = Utils.bytesToHex(hash);
                    gbcImage.setHashCode(hashHex);
                    ImageCodec imageCodec = new ImageCodec(128, 112, false);
                    Bitmap image = imageCodec.decodeWithPalette(Utils.hashPalettes.get(gbcImage.getPaletteId()).getPaletteColorsInt(), imageBytes, false,false);
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
                listActiveImages = new ArrayList<>(extractedImagesList.subList(0, extractedImagesList.size() - MainActivity.deletedCount - 1));
                listActiveBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(0, extractedImagesBitmaps.size() - MainActivity.deletedCount - 1));
                lastSeenImage = extractedImagesList.get(extractedImagesList.size() - MainActivity.deletedCount - 1);
                lastSeenBitmap = extractedImagesBitmaps.get(extractedImagesBitmaps.size() - MainActivity.deletedCount - 1);
                listDeletedImages = new ArrayList<>(extractedImagesList.subList(extractedImagesList.size() - MainActivity.deletedCount, extractedImagesList.size()));

                listDeletedBitmaps = new ArrayList<>(extractedImagesBitmaps.subList(extractedImagesBitmaps.size() - MainActivity.deletedCount, extractedImagesBitmaps.size()));
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

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    //Refactor
    private void showImages() {

        finalListImages = new ArrayList<>(listActiveImages);
        finalListBitmaps = new ArrayList<>(listActiveBitmaps);
        finalListImages.add(lastSeenImage);
        finalListImages.addAll(listDeletedImages);
        finalListBitmaps.add(lastSeenBitmap);
        bitmapsAdapterList = new ArrayList<>(finalListBitmaps);
        finalListBitmaps.addAll(listDeletedBitmaps);
        bitmapsAdapterList.addAll(listDeletedBitmapsRedStroke);

        gridviewSaves.setAdapter((new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, true, false, null)));
        gridAdapter = (CustomGridViewAdapterImage) gridviewSaves.getAdapter();
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.delete) + " " + saveName + "?");

        GridView grid = new GridView(getContext());
        grid.setNumColumns(4);
        grid.setPadding(30, 10, 30, 10);
        grid.setAdapter(new CustomGridViewAdapterImage(getContext(), R.layout.row_items, finalListImages, bitmapsAdapterList, true, false, false, null));

        builder.setView(grid);

        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Try to delete the file
                if (selectedFile.delete()) {
                    Utils.toast(getContext(), getString(R.string.toast_sav_deleted));
                    gridAdapter.clear();
                    gridAdapter.notifyDataSetChanged();
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
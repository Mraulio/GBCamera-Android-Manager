package com.mraulio.gbcameramanager.ui.frames;


import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.frameChange;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.encodeData;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.generateDefaultTransparentPixelPositions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.ui.gallery.SaveImageAsyncTask;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;

public class FramesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_frames, container, false);
        MainActivity.current_fragment = MainActivity.CURRENT_FRAGMENT.FRAMES;
        MainActivity.fab.hide();
        GridView gridView = view.findViewById(R.id.gridViewFrames);
        MainActivity.pressBack = false;
        Button btnExportFramesJson = view.findViewById(R.id.btnExportFramesJson);

        CustomGridViewAdapterFrames customGridViewAdapterFrames = new CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Utils.framesList, true, false);
        TextView tvNumFrames = view.findViewById(R.id.tvNumFrames);

        Spinner spAvailableTags = view.findViewById(R.id.spFrameGroups);

//        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
//                android.R.layout.simple_spinner_item, availableTotalTagsSpinner);

//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spAvailableTags.setAdapter(adapter);

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 3) {
                    Utils.toast(getContext(), getString(R.string.cant_delete_base));
                }
                if (position > 3) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getString(R.string.delete_frame_dialog) + Utils.framesList.get(position).getFrameName() + "?");
                    builder.setMessage(getString(R.string.sure_dialog));

                    ImageView imageView = new ImageView(getContext());
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(30, 10, 30, 10);
                    imageView.setImageBitmap(Utils.framesList.get(position).getFrameBitmap());

                    builder.setView(imageView);

                    builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DeleteFrameAsyncTask(Utils.framesList.get(position)).execute();
                            //I change the frame index of the images that have the deleted one to 0
                            //Also need to change the bitmap on the completeImageList so it changes on the Gallery
                            //I set the first frame and keep the palette for all the image, will need to check if the image keeps frame color or not
                            for (int i = 0; i < Utils.gbcImagesList.size(); i++) {
                                if (Utils.gbcImagesList.get(i).getFrameId().equals(Utils.framesList.get(position).getFrameId())) {
                                    Utils.gbcImagesList.get(i).setFrameId("gbcam01");
                                    //If the bitmap cache already has the bitmap, change it. ONLY if it has been loaded, if not it'll crash
                                    if (GalleryFragment.diskCache.get(Utils.gbcImagesList.get(i).getHashCode()) != null) {
                                        Bitmap image = null;
                                        try {
                                            GbcImage gbcImage = Utils.gbcImagesList.get(i);
                                            image = frameChange(gbcImage, "gbcam01", gbcImage.isInvertPalette(), gbcImage.isInvertFramePalette(), gbcImage.isLockFrame(), true);
                                            Utils.imageBitmapCache.put(Utils.gbcImagesList.get(i).getHashCode(), image);
                                            GalleryFragment.diskCache.put(gbcImage.getHashCode(), image);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    new SaveImageAsyncTask(Utils.gbcImagesList.get(i)).execute();
                                }
                            }
                            Utils.hashFrames.remove(Utils.framesList.get(position).getFrameId());
                            Utils.framesList.remove(position);
                            customGridViewAdapterFrames.notifyDataSetChanged();
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
                return true;//true so the normal onItemClick doesn't show
            }
        });

        btnExportFramesJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FramesJsonCreator();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        });


        // Inflate the layout for this fragment
        gridView.setAdapter(customGridViewAdapterFrames);
        tvNumFrames.setText(getString(R.string.frames_total) + Utils.framesList.size());
        return view;
    }

    private void FramesJsonCreator() throws JSONException, IOException {
        JSONObject json = new JSONObject();
        JSONObject stateObj = new JSONObject();
        JSONArray framesArr = new JSONArray();
        for (GbcFrame gbcFrame : Utils.framesList) {
            JSONObject frameObj = new JSONObject();
            frameObj.put("id", gbcFrame.getFrameId());
            frameObj.put("name", gbcFrame.getFrameName());
            frameObj.put("hash", gbcFrame.getFrameHash());
            frameObj.put("isWildFrame", gbcFrame.isWildFrame());
            framesArr.put(frameObj);
        }
        stateObj.put("frames", framesArr);

        JSONArray frameGroupNamesArr = new JSONArray();
        for (Map.Entry<String, String> entry : frameGroupsNames.entrySet()) {
            JSONObject frameObj = new JSONObject();
            String key = entry.getKey();
            String value = entry.getValue();
            frameObj.put("id", key);
            frameObj.put("name", value);
            frameGroupNamesArr.put(frameObj);
        }

        stateObj.put("frameGroupNames", frameGroupNamesArr);

        stateObj.put("lastUpdateUTC", System.currentTimeMillis() / 1000);


        json.put("state", stateObj);

        for (int i = 0; i < Utils.framesList.size(); i++) {
            GbcFrame gbcFrame = Utils.framesList.get(i);

            String txt = Utils.bytesToHex(Utils.encodeImage(gbcFrame.getFrameBitmap(), "bw"));
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < txt.length(); j++) {
                if (j > 0 && j % 32 == 0) {
                    sb.append("\n");
                }
                sb.append(txt.charAt(j));
            }
            String tileData = sb.toString();
            String deflated = encodeData(tileData);
            json.put("frame-" + gbcFrame.getFrameHash(), deflated);

            //Now put the transparency data
            HashSet<int[]> transparencyHashSet = Utils.transparencyHashSet(gbcFrame.getFrameBitmap());
            if (transparencyHashSet.size() == 0) {
                transparencyHashSet = generateDefaultTransparentPixelPositions(gbcFrame.getFrameBitmap());
            }
            String toStringHash = hashSetToString(transparencyHashSet);

            String encodedTransparency = encodeData(toStringHash);
            json.put("frame-transparency-" + gbcFrame.getFrameHash(), encodedTransparency);

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String fileName = "frames_" + dateFormat.format(new Date()) + ".json";
        File file = new File(Utils.FRAMES_FOLDER, fileName);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(json.toString(2));
            Utils.toast(getContext(), getString(R.string.toast_frames_json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String hashSetToString(HashSet<int[]> hashSet) {
        StringBuilder sb = new StringBuilder();
//        sb.append("[");
        for (int[] array : hashSet) {
            sb.append(Arrays.toString(array)).append(",");
        }
        if (!hashSet.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1); // Eliminar la última coma
        }
//        sb.append("]");
        return sb.toString();
    }

    public static byte[] serializeHashSet(HashSet<int[]> hashSet) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int[] array : hashSet) {
            for (int num : array) {
                byteArrayOutputStream.write(num);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int length = deflater.deflate(buffer);
            outputStream.write(buffer, 0, length);
        }
        deflater.end();
        return outputStream.toByteArray();
    }

    public static String encodeDataByte(byte[] data) {
        return new String(data, StandardCharsets.ISO_8859_1);
    }


    private class DeleteFrameAsyncTask extends AsyncTask<Void, Void, Void> {

        //To add the new palette as a parameter
        private final GbcFrame gbcFrame;

        public DeleteFrameAsyncTask(GbcFrame gbcFrame) {
            this.gbcFrame = gbcFrame;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FrameDao frameDao = MainActivity.db.frameDao();
            frameDao.delete(gbcFrame);
            return null;
        }
    }

    public static class CustomGridViewAdapterFrames extends ArrayAdapter<GbcFrame> {
        Context context;
        int layoutResourceId;
        private boolean showTextView, checkDuplicate;
        List<GbcFrame> data = new ArrayList<GbcFrame>();
        int notSelectedColor = Color.parseColor("#C7D3D5");
        int selectedColor = Color.parseColor("#8C97B3");
        int lastSelectedPosition = -1; // No selected element initially


        public CustomGridViewAdapterFrames(Context context, int layoutResourceId,
                                           List<GbcFrame> data, boolean showTextView, boolean checkDuplicate) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
            this.showTextView = showTextView;
            this.checkDuplicate = checkDuplicate;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RecordHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new RecordHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.tvFrameName);
                holder.imageItem = (ImageView) row.findViewById(R.id.imageViewFrameItem);
                row.setTag(holder);
            } else {
                holder = (RecordHolder) row.getTag();
            }
            holder.txtTitle.setBackgroundColor(notSelectedColor);
            holder.imageItem.setBackgroundColor(notSelectedColor);


            if (position == lastSelectedPosition) {
                holder.txtTitle.setBackgroundColor(selectedColor);
                holder.imageItem.setBackgroundColor(selectedColor);
            }
            if (!showTextView) {
                holder.txtTitle.setVisibility(View.GONE);
            }
            Bitmap image = data.get(position).getFrameBitmap();
            String name = data.get(position).getFrameName();
            String id = data.get(position).getFrameId();
            if (checkDuplicate) {
                for (GbcFrame objeto : Utils.framesList) {
                    if (objeto.getFrameId().equals(id)) {
                        holder.imageItem.setBackgroundColor(context.getResources().getColor(R.color.duplicated));
                    }
                }
            }
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;
        }

        // Method to update the last selected position
        public void setLastSelectedPosition(int position) {
            lastSelectedPosition = position;
        }
    }


}


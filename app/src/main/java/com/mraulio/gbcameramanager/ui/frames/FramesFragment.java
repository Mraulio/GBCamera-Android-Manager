package com.mraulio.gbcameramanager.ui.frames;


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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.ui.gallery.SaveImageAsyncTask;
import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class FramesFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_frames, container, false);
        MainActivity.fab.hide();
        GridView gridView = view.findViewById(R.id.gridViewFrames);
        MainActivity.pressBack = false;
        CustomGridViewAdapterFrames customGridViewAdapterFrames = new CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Utils.framesList, true, false);
        TextView tvNumFrames = view.findViewById(R.id.tvNumFrames);
        MainActivity.current_fragment = MainActivity.CURRENT_FRAGMENT.FRAMES;

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

                    // Crear un ImageView y establecer la imagen deseada
                    ImageView imageView = new ImageView(getContext());
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(30, 10, 30, 10);
                    imageView.setImageBitmap(Utils.framesList.get(position).getFrameBitmap());

                    // Agregar el ImageView al diseño del diálogo
                    builder.setView(imageView);

                    builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DeleteFrameAsyncTask(Utils.framesList.get(position)).execute();
                            //I change the frame index of the images that have the deleted one to 0
                            //Also need to change the bitmap on the completeImageList so it changes on the Gallery
                            //I set the first frame and keep the palette for all the image, will need to check if the image keeps frame color or not
                            for (int i = 0; i < Utils.gbcImagesList.size(); i++) {
                                if (Utils.gbcImagesList.get(i).getFrameId().equals(Utils.framesList.get(position).getFrameName())) {
                                    Utils.gbcImagesList.get(i).setFrameId("Nintendo_Frame");
                                    //If the bitmap cache already has the bitmap, change it. ONLY if it has been loaded, if not it'll crash
                                    if (Utils.imageBitmapCache.containsKey(Utils.gbcImagesList.get(i).getHashCode())) {
                                        Bitmap image = null;
                                        try {
                                            GbcImage gbcImage = Utils.gbcImagesList.get(i);
                                            image = GalleryFragment.frameChange(gbcImage, Utils.imageBitmapCache.get(gbcImage.getHashCode()), gbcImage.getFrameId(), Utils.gbcImagesList.get(i).isLockFrame());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        Utils.imageBitmapCache.put(Utils.gbcImagesList.get(i).getHashCode(), image);
                                    }
                                    new SaveImageAsyncTask(Utils.gbcImagesList.get(i)).execute();
                                }
                            }
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

        // Inflate the layout for this fragment
        gridView.setAdapter(customGridViewAdapterFrames);
        tvNumFrames.setText(getString(R.string.frames_total) + Utils.framesList.size());
        return view;
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
        int notSelectedColor = Color.parseColor("#FFFFFF");
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

            if (checkDuplicate) {
                for (GbcFrame objeto : Utils.framesList) {
                    if (objeto.getFrameName().equals(name)) {
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


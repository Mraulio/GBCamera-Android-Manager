package com.mraulio.gbcameramanager.ui.frames;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.TextView;

import com.mraulio.gbcameramanager.JsonReader;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.ui.palettes.PalettesFragment;

import org.json.JSONException;
import org.w3c.dom.Text;

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
        GridView gridView = view.findViewById(R.id.gridViewFrames);
        MainActivity.pressBack = false;
        CustomGridViewAdapterFrames customGridViewAdapterFrames = new CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Methods.framesList, true,false);
        TextView tvNumFrames = view.findViewById(R.id.tvNumFrames);

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 2) {
                    Methods.toast(getContext(), "Can't delete a base frame");
                }
                if (position > 2) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Delete frame " + Methods.framesList.get(position).getFrameName() + "?");
                    builder.setMessage("Are you sure?");

                    // Crear un ImageView y establecer la imagen deseada
                    ImageView imageView = new ImageView(getContext());
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(30, 10, 30, 10);
                    imageView.setImageBitmap(Methods.framesList.get(position).getFrameBitmap());

                    // Agregar el ImageView al diseño del diálogo
                    builder.setView(imageView);

                    builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Acción a realizar cuando se presiona el botón "Aceptar"

                            //I change the frame index of the images that have the deleted one to 0
                            //Also need to change the bitmap on the completeImageList so it changes on the Gallery
                            //I set the first frame and keep the palette for all the image, will need to check if the image keeps frame color or not
                            for (int i = 0; i < Methods.gbcImagesList.size(); i++) {
                                if (Methods.gbcImagesList.get(i).getFrameIndex() == position) {
                                    Methods.gbcImagesList.get(i).setFrameIndex(0);
                                    Bitmap image = null;
                                    try {
                                        image = GalleryFragment.frameChange(i, 0, false);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Methods.completeImageList.set(i, image);
                                }
                            }
                            Methods.framesList.remove(position);
                            customGridViewAdapterFrames.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Acción a realizar cuando se presiona el botón "Cancelar"
                        }
                    });
                    // Mostrar el diálogo
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                return true;//true so the normal onItemClick doesn't show
            }

        });

        // Inflate the layout for this fragment
        gridView.setAdapter(customGridViewAdapterFrames);
        tvNumFrames.setText("There are " + Methods.framesList.size() + " frames.");
        return view;
    }

    public static class CustomGridViewAdapterFrames extends ArrayAdapter<GbcFrame> {
        Context context;
        int layoutResourceId;
        private boolean showTextView, checkDuplicate;
        List<GbcFrame> data = new ArrayList<GbcFrame>();
        int notSelectedColor = Color.parseColor("#FFFFFF");
        int selectedColor = Color.parseColor("#8C97B3");
        int lastSelectedPosition = -1; // Inicialmente no hay ningún elemento seleccionado
        int duplicatedColor = Color.parseColor("#FF0000");


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
                for (GbcFrame objeto : Methods.framesList) {
                    // Comparar el valor de la propiedad "nombre" de cada objeto con el valor del nuevo objeto
                    if (objeto.getFrameName().equals(name)) {
                        // Si el valor es igual, significa que el nombre ya existe en otro objeto de la lista
                        holder.imageItem.setBackgroundColor(duplicatedColor);
                    }
                }
            }
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
//            if (image != null && !image.isRecycled()) {
//                image.recycle();
//            }
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;

        }

        // Método para actualizar la última posición seleccionada
        public void setLastSelectedPosition(int position) {
            lastSelectedPosition = position;
        }
    }


}


package com.mraulio.gbcameramanager.ui.frames;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class FramesFragment extends Fragment {
    Button btnImportFrames;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_frames, container, false);
        btnImportFrames = view.findViewById(R.id.btnImportFrames);
        GridView gridView = view.findViewById(R.id.gridViewFrames);
        MainActivity.pressBack = false;
        CustomGridViewAdapterFrames customGridViewAdapterFrames = new CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Methods.framesList);
        btnImportFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> listFramesString = new ArrayList<>();
                try {
                    listFramesString = JsonReader.readerFrames();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (String str : listFramesString) {

                    byte[] bytes = convertToByteArray(str);
                    GbcFrame gbcFrame = new GbcFrame();
                    gbcFrame.setFrameName("next frame");
                    int height = (str.length() + 1) / 120;//To get the real height of the image
                    ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColors()), 160, height);
                    Bitmap image = imageCodec.decodeWithPalette(0, bytes);
                    gbcFrame.setFrameBitmap(image);
                    Methods.framesList.add(gbcFrame);
                }
                customGridViewAdapterFrames.notifyDataSetChanged();

            }
        });


        // Inflate the layout for this fragment
        gridView.setAdapter(customGridViewAdapterFrames);
        return view;
    }

    //Refactor this on a class
    private static byte[] convertToByteArray(String data) {
        String[] byteStrings = data.split(" ");
        byte[] bytes = new byte[byteStrings.length];
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = (byte) ((Character.digit(byteStrings[i].charAt(0), 16) << 4)
                    + Character.digit(byteStrings[i].charAt(1), 16));
        }
        System.out.println(bytes.length);
        return bytes;
    }

    public static class CustomGridViewAdapterFrames extends ArrayAdapter<GbcFrame> {
        Context context;
        int layoutResourceId;
        List<GbcFrame> data = new ArrayList<GbcFrame>();
        int notSelectedColor = Color.parseColor("#FFFFFF");
        int selectedColor = Color.parseColor("#8C97B3");
        int lastSelectedPosition = -1; // Inicialmente no hay ningún elemento seleccionado


        public CustomGridViewAdapterFrames(Context context, int layoutResourceId,
                                           List<GbcFrame> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
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
            Bitmap image = data.get(position).getFrameBitmap();
            String name = data.get(position).getFrameName();
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


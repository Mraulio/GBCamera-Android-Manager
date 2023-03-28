package com.mraulio.gbcameramanager.ui.frames;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcPalette;

import org.w3c.dom.Text;

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
        // Inflate the layout for this fragment
        gridView.setAdapter(new CustomGridViewAdapterFrames(getContext(),R.layout.frames_row_items, Methods.framesList));
        return view;
    }


    public class CustomGridViewAdapterFrames extends ArrayAdapter<GbcFrame> {
        Context context;
        int layoutResourceId;
        List<GbcFrame> data = new ArrayList<GbcFrame>();

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

            Bitmap image = data.get(position).getFrameBitmap();
            String name = data.get(position).getFrameName();
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
            return row;

        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;

        }
    }
}


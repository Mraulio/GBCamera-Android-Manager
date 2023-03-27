package com.mraulio.gbcameramanager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.ArrayList;

/**
 * Other way to show images on the GridView, with the Text
 */
public class CustomGridViewAdapterPalette extends ArrayAdapter<GbcPalette> {
    Context context;
    int layoutResourceId;
    ArrayList<GbcPalette> data = new ArrayList<GbcPalette>();

    public CustomGridViewAdapterPalette(Context context, int layoutResourceId,
                                        ArrayList<GbcPalette> data) {
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
            holder.txtTitle = (TextView) row.findViewById(R.id.tvPaletteName);
            holder.imageItem = (ImageView) row.findViewById(R.id.imageView);
            row.setTag(holder);
        } else {
            holder = (RecordHolder) row.getTag();
        }

        Bitmap image = data.get(position).paletteViewer();
        String name = data.get(position).getName();
        holder.txtTitle.setText(name);
        holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
        return row;

    }

    private class RecordHolder {
        TextView txtTitle;
        ImageView imageItem;

    }
}


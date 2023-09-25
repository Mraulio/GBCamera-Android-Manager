package com.mraulio.gbcameramanager.ui.palettes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.mraulio.gbcameramanager.utils.Utils;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.ArrayList;

/**
 * Other way to show images on the GridView, with the Text
 */
public class CustomGridViewAdapterPalette extends ArrayAdapter<GbcPalette> {
    Context context;
    int layoutResourceId;
    ArrayList<GbcPalette> data = new ArrayList<GbcPalette>();
    private boolean showTextView, checkDuplicate;
    int lastSelectedImagePosition = -1; //No image palette selected initially
    int lastSelectedFramePosition = -1; //No frame palette selected initially

    public CustomGridViewAdapterPalette(Context context, int layoutResourceId,
                                        ArrayList<GbcPalette> data, boolean showTextView, boolean checkDuplicate) {
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
        int notSelectedColor = Color.parseColor("#FFFFFF");
        int selectedImageColor = Color.parseColor("#8C97B3");
        int selectedFrameColor = Color.parseColor("#CEBB8D");
        int sameSelectedPalette = Color.parseColor("#AE4F4F");
        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RecordHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.tvPaletteName);
            holder.imageItem = (ImageView) row.findViewById(R.id.imageView);
            holder.cardView = (CardView) row.findViewById(R.id.cardViewPalette);
            row.setTag(holder);
        } else {
            holder = (RecordHolder) row.getTag();
        }
        holder.cardView.setBackgroundColor(notSelectedColor);
        holder.imageItem.setBackgroundColor(notSelectedColor);

        if (position == lastSelectedImagePosition) {
            holder.cardView.setBackgroundColor(selectedImageColor);
            holder.imageItem.setBackgroundColor(selectedImageColor);
        }
        if (position == lastSelectedFramePosition) {
            holder.cardView.setBackgroundColor(selectedFrameColor);
            holder.imageItem.setBackgroundColor(selectedFrameColor);
        }
        if (position == lastSelectedImagePosition && position ==  lastSelectedFramePosition) {
            holder.cardView.setBackgroundColor(sameSelectedPalette);
            holder.imageItem.setBackgroundColor(sameSelectedPalette);
        }
        if (!showTextView) {
            holder.txtTitle.setVisibility(View.GONE);
        }
        Bitmap image = data.get(position).paletteViewer();
        String name = data.get(position).getPaletteId();
        if (checkDuplicate) {
            for (GbcPalette objeto : Utils.gbcPalettesList) {
                if (objeto.getPaletteId().equals(name)) {
                    holder.imageItem.setBackgroundColor(context.getResources().getColor(R.color.duplicated));
                }
            }
        }
        holder.txtTitle.setText(name);
        holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
        if (image != null && !image.isRecycled()) {
            image.recycle();
        }
        return row;
    }

    private class RecordHolder {
        TextView txtTitle;
        CardView cardView;
        ImageView imageItem;
    }

    // Method to update the last palette used for the image
    public void setLastSelectedImagePosition(int position) {
        lastSelectedImagePosition = position;
    }
    // Method to update the last palette used for the image
    public void setLastSelectedFramePosition(int position) {
        lastSelectedFramePosition = position;
    }
}
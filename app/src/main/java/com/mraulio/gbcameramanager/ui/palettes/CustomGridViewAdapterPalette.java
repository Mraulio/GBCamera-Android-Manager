package com.mraulio.gbcameramanager.ui.palettes;

import android.annotation.SuppressLint;
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

import com.mraulio.gbcameramanager.Methods;
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
    int lastSelectedPosition = -1; // Inicialmente no hay ningún elemento seleccionado

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
        int selectedColor = Color.parseColor("#8C97B3");

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

        if (position == lastSelectedPosition) {
            holder.cardView.setBackgroundColor(selectedColor);
            holder.imageItem.setBackgroundColor(selectedColor);
        }
        if (!showTextView) {
            holder.txtTitle.setVisibility(View.GONE);
        }
        Bitmap image = data.get(position).paletteViewer();
        String name = data.get(position).getName();
        if (checkDuplicate) {
            for (GbcPalette objeto : Methods.gbcPalettesList) {
                // Comparar el valor de la propiedad "nombre" de cada objeto con el valor del nuevo objeto
                if (objeto.getName().equals(name)) {
                    // Si el valor es igual, significa que el nombre ya existe en otro objeto de la lista
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

    // Método para actualizar la última posición seleccionada
    public void setLastSelectedPosition(int position) {
        lastSelectedPosition = position;
    }
}
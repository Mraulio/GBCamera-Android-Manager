package com.mraulio.gbcameramanager.ui.palettes;

import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.mraulio.gbcameramanager.utils.StaticValues;
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
    boolean showFavorite;
    CustomGridViewAdapterPalette customGridViewAdapterPalette;

    public CustomGridViewAdapterPalette(Context context, int layoutResourceId,
                                        ArrayList<GbcPalette> data, boolean showTextView, boolean checkDuplicate, boolean showFavorite) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.showTextView = showTextView;
        this.showFavorite = showFavorite;
        this.checkDuplicate = checkDuplicate;
    }

    public void setCustomGridViewAdapterPalette(CustomGridViewAdapterPalette customGridViewAdapterPalette) {
        this.customGridViewAdapterPalette = customGridViewAdapterPalette;
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
            holder.imageItem = (ImageView) row.findViewById(R.id.image_view_palette);
            holder.starItem = (ImageView) row.findViewById(R.id.iv_star);
            holder.cardView = (CardView) row.findViewById(R.id.cardViewPalette);
            holder.btnMenu = (TextView) row.findViewById(R.id.btn_menu_palette);
            holder.rlTvs = (RelativeLayout) row.findViewById(R.id.ly_tvs);
            row.setTag(holder);
        } else {
            holder = (RecordHolder) row.getTag();
        }

        if (showFavorite && data.get(position).getPaletteId().equals(StaticValues.defaultPaletteId)) {
            holder.starItem.setVisibility(View.VISIBLE);
        } else {
            holder.starItem.setVisibility(View.GONE);
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
        if (position == lastSelectedImagePosition && position == lastSelectedFramePosition) {
            holder.cardView.setBackgroundColor(sameSelectedPalette);
            holder.imageItem.setBackgroundColor(sameSelectedPalette);
        }
        if (!showTextView) {
            holder.txtTitle.setVisibility(View.GONE);
            holder.btnMenu.setVisibility(View.GONE);
        } else {
            RecordHolder finalHolder = holder;
            holder.rlTvs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMenu(context, finalHolder, data.get(position).getPaletteId(), customGridViewAdapterPalette);

                }
            });

        }

        Bitmap image = data.get(position).paletteViewer();
        String id = data.get(position).getPaletteId();
        String paletteName = data.get(position).getPaletteName();
        String showingName = (paletteName != null ? paletteName : "") + " (" + id + ")";

        if (showFavorite && data.get(position).isFavorite()) {
            holder.imageItem.setBackgroundColor(context.getResources().getColor(R.color.favorite));
        }

        if (checkDuplicate) {
            for (GbcPalette objeto : Utils.gbcPalettesList) {
                if (objeto.getPaletteId().equals(id)) {
                    holder.imageItem.setBackgroundColor(context.getResources().getColor(R.color.duplicated));
                }
            }
        }

        holder.txtTitle.setText(showingName);
        holder.imageItem.setImageBitmap(image.copy(image.getConfig(),false));
        if (image != null && !image.isRecycled()) {
            image.recycle();
        }
        return row;
    }

    private class RecordHolder {
        TextView txtTitle, btnMenu;
        CardView cardView;
        ImageView imageItem, starItem;
        RelativeLayout rlTvs;
    }

    // Method to update the last palette used for the image
    public void setLastSelectedImagePosition(int position) {
        lastSelectedImagePosition = position;
    }

    // Method to update the last palette used for the image
    public void setLastSelectedFramePosition(int position) {
        lastSelectedFramePosition = position;
    }

    public static void showMenu(Context context, RecordHolder finalHolder, String paletteId, CustomGridViewAdapterPalette customGridViewAdapterPalette) {
        PopupMenu popupMenu = new PopupMenu(context, finalHolder.btnMenu);
        popupMenu.getMenuInflater().inflate(R.menu.menu_default_pal_fram, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_default:
                        toast(context, "Default: " + paletteId);
                        SharedPreferences.Editor editor = StaticValues.sharedPreferences.edit();
                        editor.putString("default_palette_id", paletteId);
                        editor.apply();
                        StaticValues.defaultPaletteId = paletteId;
                        if (customGridViewAdapterPalette != null) {
                            customGridViewAdapterPalette.notifyDataSetChanged();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
}
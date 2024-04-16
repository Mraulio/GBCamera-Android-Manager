package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;

import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
    Context context;
    int layoutResourceId;
    List<GbcImage> data = new ArrayList<GbcImage>();
    private List<Bitmap> images;
    private boolean checkDuplicate;
    private boolean showName, multiSelect;
    private List<Integer> selectedImages;

    public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                      List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate,
                                      boolean showName, boolean multiSelect, List<Integer> selectedImages) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.images = images;
        this.data = data;
        this.checkDuplicate = checkDuplicate;
        this.showName = showName;
        this.multiSelect = multiSelect;
        this.selectedImages = selectedImages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        RecordHolder holder = null;
        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new RecordHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
            holder.imageItem = (ImageView) row.findViewById(R.id.imageView);

            row.setTag(holder);
        } else {
            holder = (RecordHolder) row.getTag();
        }
        Bitmap image = images.get(position);
        image = rotateBitmap(image, data.get(position));
        String name = data.get(position).getName();
        String hash = data.get(position).getHashCode();
        List<String> hashToCheck = new ArrayList<>();
        if (multiSelect && selectedImages != null && !selectedImages.isEmpty()) {
            for (int i : selectedImages) {
                hashToCheck.add(GalleryFragment.filteredGbcImages.get(i).getHashCode());
            }
            row.setBackgroundColor(hashToCheck.contains(hash) ? context.getColor(R.color.teal_700) : Color.WHITE);
        }

        Boolean fav = data.get(position).getTags().contains(FILTER_FAVOURITE);
        Boolean superFav = data.get(position).getTags().contains(FILTER_SUPER_FAVOURITE);
        if (superFav) {
            holder.imageItem.setBackgroundColor(context.getColor(R.color.star_color));
        } else if (fav) {
            holder.imageItem.setBackgroundColor(context.getColor(R.color.favorite));
        }else {
            holder.imageItem.setBackgroundColor(context.getColor(R.color.imageview_bg));
        }
        Boolean dup = false;
        if (checkDuplicate) {
            for (GbcImage gbcImage : Utils.gbcImagesList) {
                //Compare the hash value with the value of the new image hash
                if (gbcImage.getHashCode().equals(hash)) {
                    //If hash is equals means the image already exists
                    dup = true;
                }
            }
        }
        if (showName) {
            holder.txtTitle.setTextColor(dup ? context.getResources().getColor(R.color.duplicated) : Color.BLACK);
            if (name.equals("")) {
                holder.txtTitle.setText("*No title*");
            } else
                holder.txtTitle.setText(name);
        } else {
            holder.txtTitle.setVisibility(GONE);
        }
        holder.imageItem.setImageBitmap(image);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        int desiredHeight = (int) (screenWidth * 0.255);//Factor to work on every screen aprox
        holder.imageItem.getLayoutParams().height = desiredHeight;
        holder.imageItem.requestLayout();
        RecordHolder finalHolder = holder;

        return row;
    }

    private class RecordHolder {
        TextView txtTitle;
        ImageView imageItem;
    }
}
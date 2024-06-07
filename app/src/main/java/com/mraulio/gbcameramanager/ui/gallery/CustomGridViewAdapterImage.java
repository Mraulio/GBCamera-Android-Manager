package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;

import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_DUPLICATED;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_DUPLICATED;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
    Context context;
    int layoutResourceId;
    List<GbcImage> data;
    private List<Bitmap> images;
    private boolean checkDuplicate;
    private boolean showInfo, multiSelect;
    private List<Integer> selectedImages;

    public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                      List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate,
                                      boolean showInfo, boolean multiSelect, List<Integer> selectedImages) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.images = images;
        this.data = data;
        this.checkDuplicate = checkDuplicate;
        this.showInfo = showInfo;
        this.multiSelect = multiSelect;
        this.selectedImages = selectedImages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        RecordHolder holder;
        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new RecordHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
            holder.txtTitle.setSelected(true);
            holder.txtTags = (TextView) row.findViewById(R.id.tv_tags);
            holder.txtTags.setSelected(true);
            holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
            holder.txtDate = (TextView) row.findViewById(R.id.tv_date);
            holder.txtDate.setSelected(true);
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
        } else {
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
        if (showInfo) {
            holder.txtTitle.setTextColor(dup ? context.getResources().getColor(R.color.duplicated) : Color.BLACK);
            holder.txtTitle.setText(name);

            StringBuilder sb = new StringBuilder();
            for (String item : data.get(position).getTags()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                if (item.equals(FILTER_FAVOURITE)) {
                    item = TAG_FAVOURITE;
                } else if (item.equals(FILTER_SUPER_FAVOURITE)) {
                    item = TAG_SUPER_FAVOURITE;
                } else if (item.equals(FILTER_DUPLICATED)) {
                    item = TAG_DUPLICATED;
                } else if (item.equals(FILTER_TRANSFORMED)) {
                    item = TAG_TRANSFORMED;
                }
                sb.append(item);
            }
            holder.txtTags.setText(sb.toString());

            String loc;
            if (dateLocale.equals("yyyy-MM-dd")) {
                loc = "dd/MM/yyyy";
            } else {
                loc = "MM/dd/yyyy";
            }
            SimpleDateFormat sdf = new SimpleDateFormat(loc + " HH:mm:ss");
            holder.txtDate.setText(sdf.format(data.get(position).getCreationDate()));
        } else {
            holder.txtTitle.setVisibility(GONE);
            holder.txtTags.setVisibility(GONE);
            holder.txtDate.setVisibility(GONE);
        }

        holder.imageItem.setImageBitmap(image);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        int desiredHeight = (int) (screenWidth * 0.255);//Factor to work on every screen aprox
        holder.imageItem.getLayoutParams().height = desiredHeight;
        holder.imageItem.requestLayout();

        return row;
    }

    private class RecordHolder {
        TextView txtTitle, txtTags, txtDate;
        ImageView imageItem;
    }
}
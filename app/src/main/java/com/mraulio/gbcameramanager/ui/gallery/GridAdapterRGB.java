package com.mraulio.gbcameramanager.ui.gallery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.importFile.newpalette.ItemTouchHelperAdapter;

import java.util.List;

public class GridAdapterRGB extends RecyclerView.Adapter<GridAdapterRGB.ViewHolder> implements ItemTouchHelperAdapter {

    private List<Bitmap> mItems;
    private OnItemMovedListener onItemMovedListener;

    public GridAdapterRGB(List<Bitmap> items) {
        mItems = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.palette_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bitmap item = mItems.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public List<Bitmap> getItems() {
        return mItems;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Bitmap movedItem = mItems.remove(fromPosition);
        mItems.add(toPosition, movedItem);
        notifyItemMoved(fromPosition, toPosition);
        if (onItemMovedListener != null) {
            onItemMovedListener.onItemMoved(fromPosition, toPosition);
        }
    }

    public interface OnItemMovedListener {
        void onItemMoved(int fromPosition, int toPosition);
    }

    public void setOnItemMovedListener(OnItemMovedListener listener) {
        this.onItemMovedListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;
        private RelativeLayout mLy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image_view_palette);
            mLy = itemView.findViewById(R.id.ly_tvs);
        }

        public void bind(Bitmap item, int position) {
            switch (position) {
                case 0:
                    mImageView.setBackgroundColor(Color.RED);
                    break;
                case 1:
                    mImageView.setBackgroundColor(Color.GREEN);
                    break;
                case 2:
                    mImageView.setBackgroundColor(Color.BLUE);
                    break;
                case 3:
                    mImageView.setBackgroundColor(Color.BLACK);
                    break;
                default:
                    break;
            }
            mImageView.setImageBitmap(item);
            mLy.setVisibility(View.GONE);
        }
    }
}

package com.mraulio.gbcameramanager.ui.importFile.newpalette;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mraulio.gbcameramanager.R;

import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private List<Integer> mItems;
    private OnItemMovedListener onItemMovedListener;
    public GridAdapter(List<Integer> items) {
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
        Integer item = mItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
    public List<Integer> getItems() {
        return mItems;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Integer movedItem = mItems.remove(fromPosition);
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

        private ImageView mImageView;
        private RelativeLayout mLy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image_view_palette);
            mLy = itemView.findViewById(R.id.ly_tvs);
        }

        public void bind(Integer item) {
            mImageView.setBackgroundColor(item);
            mImageView.setImageBitmap(null);
            mLy.setVisibility(View.GONE);
        }
    }
}

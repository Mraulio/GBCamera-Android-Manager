package com.mraulio.gbcameramanager.ui.palettes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.util.ArrayList;
//import com.mraulio.gbcameramanager.databinding.FragmentSlideshowBinding;

public class PalettesFragment extends Fragment {

    //    private FragmentSlideshowBinding binding;
    CustomGridViewAdapterPalette imageAdapter;
    GridView gridViewPalettes;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_palettes, container, false);
        Button btnColorPicker = view.findViewById(R.id.btnColorPicker);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        int[] selectedColors = new int[4];
//        ColorPicker colorPicker = new ColorPicker(this);
//        colorPicker.setColors(new int[] {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW});

        gridViewPalettes = view.findViewById(R.id.gridViewPalettes);

        btnColorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialogBuilder
                        .with(getContext())
                        .setTitle("Choose color")
                        .initialColor(Color.rgb(155, 188, 15)
                        )
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {
                                toast("onColorSelected: 0x" + Integer.toHexString(selectedColor));
                            }
                        })
                        .setPositiveButton("ok", new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                changeBackgroundColor(selectedColor);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();
            }
        });
        gridViewPalettes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bitmap image = Methods.gbcPalettesList.get(position).paletteViewer();
// Crear el diálogo personalizado
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);
//                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(image);
//                builder.setView(imageView);
                dialog.show();
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GbcPalette customPalette = new GbcPalette( new int[]{ Color.rgb(85, 85, 85), Color.rgb(85, 85, 85), Color.rgb(85, 85, 85), Color.rgb(85, 85, 85)},"Create");
                imageAdapter.add(customPalette);
                imageAdapter.notifyDataSetChanged();
            }
        });

        imageAdapter = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList);
        gridViewPalettes.setAdapter(imageAdapter);
        return view;
//        PalettesViewModel slideshowViewModel =
//                new ViewModelProvider(this).get(PalettesViewModel.class);
//
//        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
//        View root = binding.getRoot();
//
//        final TextView textView = binding.textSlideshow;
//        slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
//        return root;

    }

    private void toast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void changeBackgroundColor(int color) {
        View view = getView();
        if (view != null) {
            view.setBackgroundColor(color);
        }
    }


    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapterPalette extends ArrayAdapter<GbcPalette> {
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


//    @Override
//    public void onDestroyView() {btnColorPicker.setOnClickListener(new View.OnClickListener() {

//        super.onDestroyView();
//        binding = null;
//    }
}
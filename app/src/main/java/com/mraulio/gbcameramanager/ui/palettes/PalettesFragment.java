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
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
    Fragment fr_createPalette;
    ImageView iv1, iv2, iv3, iv4;
    int lastPicked = Color.rgb(155, 188, 15);
    String newPaletteName = "*Set Palette Name*";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_palettes, container, false);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        int[] selectedColors = new int[4];
//        ColorPicker colorPicker = new ColorPicker(this);
//        colorPicker.setColors(new int[] {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW});

        gridViewPalettes = view.findViewById(R.id.gridViewPalettes);

        //NEEDS LOTS OF REFACTORING
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.palette_creator);
                ImageView ivPalette = dialog.findViewById(R.id.ivPalette);
                Button btnSavePalette = dialog.findViewById(R.id.btnSavePalette);
                EditText etPaletteName = dialog.findViewById(R.id.etPaletteName);
                etPaletteName.setText(newPaletteName);
                int[] palette = {
                        Color.rgb(255, 255, 255),
                        Color.rgb(170, 170, 170),
                        Color.rgb(85, 85, 85),
                        Color.rgb(0, 0, 0)
                };
//                ivPalette.setImageBitmap(paletteMaker(palette));
                ivPalette.setImageBitmap(Bitmap.createScaledBitmap(paletteMaker(palette), paletteMaker(palette).getWidth() * 3, paletteMaker(palette).getHeight() * 3, false));

                iv1 = dialog.findViewById(R.id.iv1);
                iv2 = dialog.findViewById(R.id.iv2);
                iv3 = dialog.findViewById(R.id.iv3);
                iv4 = dialog.findViewById(R.id.iv4);
                iv1.setBackgroundColor(palette[0]);
                iv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ColorPickerDialogBuilder
                                .with(getContext())
                                .setTitle("Choose color")
                                .initialColor(lastPicked)
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
                                        iv1.setBackgroundColor(selectedColor);
                                        palette[0] = selectedColor;
                                        ivPalette.setImageBitmap(paletteMaker(palette));
                                        lastPicked = selectedColor;
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
                iv2.setBackgroundColor(palette[1]);
                iv2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ColorPickerDialogBuilder
                                .with(getContext())
                                .setTitle("Choose color")
                                .initialColor(lastPicked)
                                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
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
                                        iv2.setBackgroundColor(selectedColor);
                                        palette[1] = selectedColor;
                                        ivPalette.setImageBitmap(paletteMaker(palette));
                                        lastPicked = selectedColor;
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

                iv3.setBackgroundColor(palette[2]);
                iv3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ColorPickerDialogBuilder
                                .with(getContext())
                                .setTitle("Choose color")
                                .initialColor(lastPicked)
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
                                        iv3.setBackgroundColor(selectedColor);
                                        palette[2] = selectedColor;
                                        ivPalette.setImageBitmap(paletteMaker(palette));
                                        lastPicked = selectedColor;
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

                iv4.setBackgroundColor(palette[3]);
                iv4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ColorPickerDialogBuilder
                                .with(getContext())
                                .setTitle("Choose color")
                                .initialColor(lastPicked)
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
                                        iv4.setBackgroundColor(selectedColor);
                                        palette[3] = selectedColor;
                                        ivPalette.setImageBitmap(paletteMaker(palette));
                                        lastPicked = selectedColor;
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
                btnSavePalette.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        newPaletteName = etPaletteName.getText().toString();
                        GbcPalette newPalette = new GbcPalette();
                        newPalette.setName(newPaletteName);
                        newPalette.setPaletteColors(palette);
                        Methods.gbcPalettesList.add(newPalette);
                        gridViewPalettes.setAdapter(imageAdapter);
                        dialog.hide();
                    }
                });

                dialog.show();
            }
        });

        imageAdapter = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList);
        gridViewPalettes.setAdapter(imageAdapter);
        return view;

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

    private Bitmap paletteMaker(int[] palette) {

        // Divide el ancho del ImageView por cuatro para obtener el ancho de cada sección
        int widthHeigh = 300;
        int sectionWidth = widthHeigh / 4;

        // Crea un objeto Bitmap con el tamaño del ImageView y formato ARGB_8888
        Bitmap bitmap = Bitmap.createBitmap(widthHeigh, widthHeigh, Bitmap.Config.ARGB_8888);

        // Obtén el objeto Canvas del bitmap para poder dibujar en él
        Canvas canvas = new Canvas(bitmap);

// Dibuja un rectángulo para cada sección del ImageView y establece el color correspondiente del array
        for (int i = 0; i < palette.length; i++) {
            Rect rect = new Rect(i * sectionWidth, 0, (i + 1) * sectionWidth, widthHeigh);
            Paint paint = new Paint();
            paint.setColor(palette[i]);
            canvas.drawRect(rect, paint);
        }
        return bitmap;
    }


}
package com.mraulio.gbcameramanager.ui.palettes;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.mraulio.gbcameramanager.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.JsonReader;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcPalette;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//import com.mraulio.gbcameramanager.databinding.FragmentSlideshowBinding;

//Using this color picker:https://github.com/QuadFlask/colorpicker
//Another color picker: https://github.com/yukuku/ambilwarna
public class PalettesFragment extends Fragment {

    //    private FragmentSlideshowBinding binding;
    CustomGridViewAdapterPalette imageAdapter;
    GridView gridViewPalettes;
    Fragment fr_createPalette;
    ImageView iv1, iv2, iv3, iv4;
    int lastPicked = Color.rgb(155, 188, 15);
    String newPaletteName = "*Set Palette Name*";
    Button btnImportPalettes;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_palettes, container, false);
        MainActivity.pressBack = false;

        Button btnAdd = view.findViewById(R.id.btnAdd);
        btnImportPalettes = view.findViewById(R.id.btnImportPalettes);
        int[] selectedColors = new int[4];
//        ColorPicker colorPicker = new ColorPicker(this);
//        colorPicker.setColors(new int[] {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW});

        gridViewPalettes = view.findViewById(R.id.gridViewPalettes);

        btnImportPalettes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> listFramesString = new ArrayList<>();
                try {
                    JsonReader.readerPalettes();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                for (String str : listFramesString) {
//
//                    byte[] bytes = convertToByteArray(str);
//                    GbcFrame gbcFrame = new GbcFrame();
//                    gbcFrame.setFrameName("next frame");
//                    int height = (str.length() + 1) / 120;//To get the real height of the image
//                    ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColors()), 160, height);
//                    Bitmap image = imageCodec.decodeWithPalette(0, bytes);
//                    gbcFrame.setFrameBitmap(image);
//                    Methods.framesList.add(gbcFrame);
//                }
                imageAdapter.notifyDataSetChanged();
            }
        });


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
                try {
                    ivPalette.setImageBitmap(paletteMaker(palette));
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
                                .showAlphaSlider(false)
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
                                        try {
                                            ivPalette.setImageBitmap(paletteMaker(palette));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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
                                .showAlphaSlider(false)
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
                                        try {
                                            ivPalette.setImageBitmap(paletteMaker(palette));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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
                                .showAlphaSlider(false)
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
                                        try {
                                            ivPalette.setImageBitmap(paletteMaker(palette));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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
                                .showAlphaSlider(false)
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
                                        try {
                                            ivPalette.setImageBitmap(paletteMaker(palette));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
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
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                int desiredWidth = (int) (screenWidth * 0.8);
                Window window = dialog.getWindow();
                window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private Bitmap paletteMaker(int[] palette) throws IOException {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // Divide el ancho del ImageView por cuatro para obtener el ancho de cada sección
        byte[] imageBytes = Methods.encodeImage(Methods.completeImageList.get(0));
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(palette), 160, 144);//imageBytes.length/40 to get the height of the image
        Bitmap bitmap = imageCodec.decodeWithPalette(palette, imageBytes);
        Bitmap upscaledBitmap = Bitmap.createScaledBitmap(bitmap, Methods.completeImageList.get(0).getWidth() * 6, Methods.completeImageList.get(0).getHeight() * 6, false);
        return upscaledBitmap;
    }



}
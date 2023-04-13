package com.mraulio.gbcameramanager.ui.palettes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    EditText et1, et2, et3, et4;
    String placeholderString = "";
    String newPaletteName = "";
    int[] palette;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_palettes, container, false);
        MainActivity.pressBack = false;

        Button btnAdd = view.findViewById(R.id.btnAdd);

        gridViewPalettes = view.findViewById(R.id.gridViewPalettes);

        CustomGridViewAdapterPalette customGridViewAdapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList, true, false);
        gridViewPalettes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                palette = Methods.gbcPalettesList.get(position).getPaletteColors().clone();//Clone so it doesn't overwrite other palette
                newPaletteName = Methods.gbcPalettesList.get(position).getName();
                paletteDialog(palette, newPaletteName);
            }
        });

        gridViewPalettes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position<=4){
                    Methods.toast(getContext(),"Can't delete a base palette");
                }
                if (position > 4) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Delete palette " + Methods.gbcPalettesList.get(position).getName() + "?");
                    builder.setMessage("Are you sure?");

                    // Crear un ImageView y establecer la imagen deseada
                    ImageView imageView = new ImageView(getContext());
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(30, 10, 30, 10);
                    imageView.setImageBitmap(Methods.gbcPalettesList.get(position).paletteViewer());

                    // Agregar el ImageView al diseño del diálogo
                    builder.setView(imageView);

                    builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Acción a realizar cuando se presiona el botón "Aceptar"
                            Methods.gbcPalettesList.remove(position);
                            imageAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Acción a realizar cuando se presiona el botón "Cancelar"
                        }
                    });
                    // Mostrar el diálogo
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                return true;//true so the normal onItemClick doesn't show
            }

        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPaletteName = "*Set Palette Name*";
                palette = Methods.gbcPalettesList.get(0).getPaletteColors();
                paletteDialog(palette, newPaletteName);
            }
        });
        imageAdapter = customGridViewAdapterPalette;
        gridViewPalettes.setAdapter(imageAdapter);
        return view;
    }

    private void paletteDialog(int[] palette, String paletteName) {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.palette_creator);
        ImageView ivPalette = dialog.findViewById(R.id.ivPalette);
        Button btnSavePalette = dialog.findViewById(R.id.btnSavePalette);
        EditText etPaletteName = dialog.findViewById(R.id.etPaletteName);
        et1 = dialog.findViewById(R.id.et1);
        et2 = dialog.findViewById(R.id.et2);
        et3 = dialog.findViewById(R.id.et3);
        et4 = dialog.findViewById(R.id.et4);
        etPaletteName.setImeOptions(EditorInfo.IME_ACTION_DONE);//WHen pressing enter
        et1.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et2.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et3.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et4.setImeOptions(EditorInfo.IME_ACTION_DONE);

        etPaletteName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    placeholderString = etPaletteName.getText().toString();
                    // El usuario ha confirmado la escritura.
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etPaletteName.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });

        etPaletteName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Este método se llama antes de que el texto cambie.
                placeholderString = etPaletteName.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Este método se llama cuando el texto cambia.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Este método se llama después de que el texto cambie.
            }
        });

        etPaletteName.setText(paletteName);
        et1.setText("#" + Integer.toHexString(palette[0]).substring(2).toUpperCase());
        et2.setText("#" + Integer.toHexString(palette[1]).substring(2).toUpperCase());
        et3.setText("#" + Integer.toHexString(palette[2]).substring(2).toUpperCase());
        et4.setText("#" + Integer.toHexString(palette[3]).substring(2).toUpperCase());

        et1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    placeholderString = et1.getText().toString();
                    // El usuario ha confirmado la escritura.
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etPaletteName.getWindowToken(), 0);
//                            etPaletteName.setText("Esto se ha cerrado");

                    return true;
                }
                return false;
            }
        });
        et1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Este método se llama antes de que el texto cambie.
                placeholderString = et1.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Este método se llama cuando el texto cambia.
                try {
                    iv1.setBackgroundColor(parseColor(et1.getText().toString()));
                    palette[0] = parseColor(et1.getText().toString());
                    ivPalette.setImageBitmap(paletteMaker(palette));

                } catch (NumberFormatException e) {
                    System.out.println("Number format exception");
                } catch (Exception e) {
                    System.out.println("Other exception" + e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Este método se llama después de que el texto cambie.
            }
        });
        et2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Este método se llama antes de que el texto cambie.
                placeholderString = et2.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Este método se llama cuando el texto cambia.
                try {
                    iv2.setBackgroundColor(parseColor(et2.getText().toString()));
                    palette[1] = parseColor(et2.getText().toString());
                    ivPalette.setImageBitmap(paletteMaker(palette));

                } catch (NumberFormatException e) {
                    System.out.println("Number format exception");
                } catch (Exception e) {
                    System.out.println("Other exception" + e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Este método se llama después de que el texto cambie.
            }
        });
        et3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Este método se llama antes de que el texto cambie.
                placeholderString = et3.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Este método se llama cuando el texto cambia.
                try {
                    iv3.setBackgroundColor(parseColor(et3.getText().toString()));
                    palette[2] = parseColor(et3.getText().toString());
                    ivPalette.setImageBitmap(paletteMaker(palette));

                } catch (NumberFormatException e) {
                    System.out.println("Number format exception");
                } catch (Exception e) {
                    System.out.println("Other exception" + e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Este método se llama después de que el texto cambie.
            }
        });
        et4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Este método se llama antes de que el texto cambie.
                placeholderString = et4.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Este método se llama cuando el texto cambia.
                try {
                    iv4.setBackgroundColor(parseColor(et4.getText().toString()));
                    palette[3] = parseColor(et4.getText().toString());
                    ivPalette.setImageBitmap(paletteMaker(palette));

                } catch (NumberFormatException e) {
                    System.out.println("Number format exception");
                } catch (Exception e) {
                    System.out.println("Other exception" + e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Este método se llama después de que el texto cambie.
            }
        });
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
                                Methods.toast(getContext(), "Selected Color: #" + Integer.toHexString(selectedColor).substring(2).toUpperCase());
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
                                et1.setText("#" + Integer.toHexString(palette[0]).substring(2).toUpperCase());
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
                                Methods.toast(getContext(), "Selected Color: #" + Integer.toHexString(selectedColor).substring(2).toUpperCase());
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
                                et2.setText("#" + Integer.toHexString(palette[1]).substring(2).toUpperCase());

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
                                Methods.toast(getContext(), "Selected Color: #" + Integer.toHexString(selectedColor).substring(2).toUpperCase());
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
                                et3.setText("#" + Integer.toHexString(palette[2]).substring(2).toUpperCase());

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
                                Methods.toast(getContext(), "Selected Color: #" + Integer.toHexString(selectedColor).substring(2).toUpperCase());
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
                                et4.setText("#" + Integer.toHexString(palette[3]).substring(2).toUpperCase());

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
                boolean alreadyExists = false;
                newPaletteName = etPaletteName.getText().toString();
                for (GbcPalette paleta : Methods.gbcPalettesList) {
                    if (paleta.getName().toLowerCase(Locale.ROOT).equals(newPaletteName.toLowerCase(Locale.ROOT))) {
                        alreadyExists = true;
                        etPaletteName.setBackgroundColor(Color.parseColor("#FF0000"));
                        Methods.toast(getContext(), "2 palettes can't have the same name.");
                        break;
                    }
                }
                if (!alreadyExists) {
                    GbcPalette newPalette = new GbcPalette();
                    newPalette.setName(newPaletteName.toLowerCase(Locale.ROOT));//To lower case to be compatible with web app
                    newPalette.setPaletteColors(palette);
                    Methods.gbcPalettesList.add(newPalette);
                    gridViewPalettes.setAdapter(imageAdapter);
                    Methods.toast(getContext(), "Palette added");
                    dialog.hide();
                }
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

    private Bitmap paletteMaker(int[] palette) throws IOException {
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(palette), 160, 144);//imageBytes.length/40 to get the height of the image
        Bitmap bitmap;
        Bitmap upscaledBitmap;
        byte[] imageBytes;
        if (Methods.gbcImagesList.size() == 0 || (Methods.gbcImagesList.get(0).getImageBytes().length / 40 != 144)) {//If there are no images, or they are not 144 height
            imageBytes = Methods.encodeImage(Methods.framesList.get(0).getFrameBitmap());
            bitmap = imageCodec.decodeWithPalette(palette, imageBytes);
            upscaledBitmap = Bitmap.createScaledBitmap(bitmap, Methods.framesList.get(0).getFrameBitmap().getWidth() * 6, Methods.framesList.get(0).getFrameBitmap().getHeight() * 6, false);
        } else {
            // Divide el ancho del ImageView por cuatro para obtener el ancho de cada sección
            imageBytes = Methods.gbcImagesList.get(0).getImageBytes();
            bitmap = imageCodec.decodeWithPalette(palette, imageBytes);
            upscaledBitmap = Bitmap.createScaledBitmap(bitmap, Methods.completeImageList.get(0).getWidth() * 6, Methods.completeImageList.get(0).getHeight() * 6, false);
        }

        return upscaledBitmap;
    }

    public static int parseColor(String colorString) {
        if (colorString.charAt(0) != '#') {
            colorString = "#" + colorString;
        }
        return Color.parseColor(colorString);
    }
}
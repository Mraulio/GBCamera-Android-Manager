package com.mraulio.gbcameramanager.ui.palettes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.db.PaletteDao;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        Button btnExportPaletteJson = view.findViewById(R.id.btnExportPaletteJson);

        gridViewPalettes = view.findViewById(R.id.gridViewPalettes);

        CustomGridViewAdapterPalette customGridViewAdapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList, true, false);
        gridViewPalettes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                palette = Methods.gbcPalettesList.get(position).getPaletteColorsInt().clone();//Clone so it doesn't overwrite base palette colors.
                newPaletteName = Methods.gbcPalettesList.get(position).getName();
                paletteDialog(palette, newPaletteName);
            }
        });

        gridViewPalettes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 5) {
                    Methods.toast(getContext(), "Can't delete a base palette");
                }
                if (position > 5) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Delete palette " + Methods.gbcPalettesList.get(position).getName() + "?");
                    builder.setMessage("Are you sure? \nDoing a Json export is recommended before continuing.");

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
                            new SavePaletteAsyncTask(Methods.gbcPalettesList.get(position), false).execute();
                            Methods.gbcPalettesList.remove(position);

                            //I change the palette index of the images that have the deleted one to 0
                            //Also need to change the bitmap on the completeImageList so it changes on the Gallery
                            for (int i = 0; i < Methods.gbcImagesList.size(); i++) {
                                if (Methods.gbcImagesList.get(i).getPaletteIndex() == position) {
                                    Methods.gbcImagesList.get(i).setPaletteIndex(0);
                                    //If the bitmap cache already has the bitmap, change it.
                                    if (Methods.imageBitmapCache.containsKey(Methods.gbcImagesList.get(i).getHashCode())) {
                                        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt()), 160, Methods.gbcImagesList.get(i).getImageBytes().length / 40);
                                        Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt(), Methods.gbcImagesList.get(i).getImageBytes());
                                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(i).getHashCode(), image);
                                    }
                                    new GalleryFragment.SaveImageAsyncTask(Methods.gbcImagesList.get(i)).execute();
                                }
                                //Also need to change the palette index of the images with a superior index to the deleted one to current index -1
                                else if (Methods.gbcImagesList.get(i).getPaletteIndex() > position) {
                                    Methods.gbcImagesList.get(i).setPaletteIndex(Methods.gbcImagesList.get(i).getPaletteIndex() - 1);
                                    if (Methods.imageBitmapCache.containsKey(Methods.gbcImagesList.get(i).getHashCode())) {
                                        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(Methods.gbcImagesList.get(i).getPaletteIndex()).getPaletteColorsInt()), 160, Methods.gbcImagesList.get(i).getImageBytes().length / 40);
                                        Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(Methods.gbcImagesList.get(i).getPaletteIndex()).getPaletteColorsInt(), Methods.gbcImagesList.get(i).getImageBytes());
                                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(i).getHashCode(), image);
                                    }
                                    new GalleryFragment.SaveImageAsyncTask(Methods.gbcImagesList.get(i)).execute();
                                }
                            }
                            imageAdapter.notifyDataSetChanged();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //No action
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
                palette = Methods.gbcPalettesList.get(0).getPaletteColorsInt().clone();//Clone so it doesn't overwrite base palette colors.
                paletteDialog(palette, newPaletteName);
            }
        });

        btnExportPaletteJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    jsonCreator();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        imageAdapter = customGridViewAdapterPalette;
        gridViewPalettes.setAdapter(imageAdapter);
        return view;
    }

    private class SavePaletteAsyncTask extends AsyncTask<Void, Void, Void> {

        //To add the new palette as a parameter
        private final GbcPalette gbcPalette;
        private final boolean save;

        public SavePaletteAsyncTask(GbcPalette gbcPalette, boolean save) {
            this.gbcPalette = gbcPalette;
            this.save = save;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PaletteDao paletteDao = MainActivity.db.paletteDao();
            if (save) {
                paletteDao.insert(gbcPalette);
            } else {
                paletteDao.delete(gbcPalette);
            }
            return null;
        }
    }


    private void jsonCreator() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject stateObj = new JSONObject();
        JSONArray palettesArr = new JSONArray();
        for (GbcPalette palette : Methods.gbcPalettesList) {
            JSONObject paletteObj = new JSONObject();
            paletteObj.put("shortName", palette.getName());
            paletteObj.put("name", palette.getName());
            JSONArray paletteArr = new JSONArray();
            for (int color : palette.getPaletteColorsInt()) {
                String hexColor = "#" + Integer.toHexString(color).substring(2);
                paletteArr.put(hexColor);
            }
            paletteObj.put("palette", paletteArr);
            paletteObj.put("origin", "GbCamera Android Manager");
            palettesArr.put(paletteObj);
        }
        stateObj.put("palettes", palettesArr);
        json.put("state", stateObj);
        System.out.println(json.toString(2));

        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy", Locale.getDefault());
        String fileName = "palettes_" + dateFormat.format(new Date()) + ".json";
        File file = new File(directory, fileName);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(json.toString(2));
            System.out.println("Saved.");
            Methods.toast(getContext(), "Palettes Json saved to Download folder.");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    //To add it to the database
                    new SavePaletteAsyncTask(newPalette, true).execute();//Adding the new palette to the database

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
            upscaledBitmap = Bitmap.createScaledBitmap(bitmap, 160 * 6, 144 * 6, false);
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
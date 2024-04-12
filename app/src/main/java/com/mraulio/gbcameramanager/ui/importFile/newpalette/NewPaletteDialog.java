package com.mraulio.gbcameramanager.ui.importFile.newpalette;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.palettes.PalettesFragment;
import com.mraulio.gbcameramanager.ui.palettes.SavePaletteAsyncTask;
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class NewPaletteDialog {

    public static void showNewPaletteDialog(Context context, int[] newPaletteColors) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_palette_import, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_palettes);
        EditText etPaletteId = dialogView.findViewById(R.id.et_palette_id_import);
        EditText etPaletteName = dialogView.findViewById(R.id.et_palette_name_import);
        Button btnSaveNewPalette = dialogView.findViewById(R.id.btn_save_new_palette);
        final String[] placeholderString = {""};
        boolean validId[] = new boolean[1];
        final String PALETTE_ID_REGEX = "^[a-z0-9]{2,}$";
        int[] changedOrderColors = newPaletteColors;

        etPaletteId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    placeholderString[0] = etPaletteId.getText().toString();

                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etPaletteId.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });

        etPaletteId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placeholderString[0] = etPaletteId.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeholderString[0] = etPaletteId.getText().toString();

                if (!placeholderString[0].equals("")) {

                    //Check if the ID is valid
                    if (placeholderString[0].matches(PALETTE_ID_REGEX)) {
                        validId[0] = true;
                        btnSaveNewPalette.setEnabled(validId[0]);

                        etPaletteId.setError(null);

                        for (GbcPalette palette : Utils.gbcPalettesList) {
                            if (palette.getPaletteId().equals(placeholderString[0])) {
                                etPaletteId.setError(context.getString(R.string.toast_palettes_error));
                                validId[0] = false;
                                btnSaveNewPalette.setEnabled(validId[0]);
                                break;
                            } else {
                                validId[0] = true;
                                btnSaveNewPalette.setEnabled(validId[0]);
                                etPaletteId.setError(null);
                            }
                        }
                    } else {
                        etPaletteId.setError(context.getString(R.string.et_palette_id_error));
                        validId[0] = false;
                        btnSaveNewPalette.setEnabled(validId[0]);
                    }

                } else {
                    etPaletteId.setHint(context.getString(R.string.et_palette_id));
                    validId[0] = false;
                    btnSaveNewPalette.setEnabled(validId[0]);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etPaletteName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    placeholderString[0] = etPaletteName.getText().toString();

                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etPaletteId.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });

        final List<Integer>[] colors = new List[]{new ArrayList<>()};
        for (int color : newPaletteColors) {
            colors[0].add(color);
        }

        final Bitmap[] paletteBitmap = {paletteViewer(newPaletteColors)};
        recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
        GridAdapter gridAdapter = new GridAdapter(colors[0]);
        recyclerView.setAdapter(gridAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(gridAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        gridAdapter.setOnItemMovedListener(new GridAdapter.OnItemMovedListener() {
            @Override
            public void onItemMoved(int fromPosition, int toPosition) {
                colors[0] = gridAdapter.getItems();
                for (int i = 0; i < colors[0].size(); i++) {
                    changedOrderColors[i] = colors[0].get(i);
                }
                paletteBitmap[0] = paletteViewer(changedOrderColors);
            }
        });

        AlertDialog dialog = builder.create();

        btnSaveNewPalette.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean alreadyExistingPaletteId = false;
                String newPaletteId = etPaletteId.getText().toString().trim();
                String newPaletteName = etPaletteName.getText().toString().trim();

                if (!newPaletteId.equals("")) {
                    for (GbcPalette paleta : Utils.gbcPalettesList) {
                        if (paleta.getPaletteId().equals(newPaletteId)) {
                            alreadyExistingPaletteId = true;
                            Utils.toast(context, context.getString(R.string.toast_palettes_error));
                            break;
                        }
                    }
                    if (!alreadyExistingPaletteId) {
                        GbcPalette newPalette = new GbcPalette();

                        newPalette.setPaletteId(newPaletteId);
                        newPalette.setPaletteName(newPaletteName);

                        newPalette.setPaletteColors(changedOrderColors);
                        Utils.gbcPalettesList.add(newPalette);
                        Utils.hashPalettes.put(newPalette.getPaletteId(), newPalette);
                        Utils.toast(context, context.getString(R.string.palette_added));
                        dialog.dismiss();
                        //To add it to the database
                        new SavePaletteAsyncTask(newPalette, true).execute();//Adding the new palette to the database

                    }
                }
            }
        });
        dialog.show();
    }

    public static Bitmap paletteViewer(int[] paletteColors) {

        int widthHeight = 100;
        int sectionWidth = widthHeight / 4;

        Bitmap bitmap = Bitmap.createBitmap(widthHeight, widthHeight / 2, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        for (int i = 0; i < paletteColors.length; i++) {
            Rect rect = new Rect(i * sectionWidth, 0, (i + 1) * sectionWidth, widthHeight);
            Paint paint = new Paint();
            paint.setColor(paletteColors[i]);
            canvas.drawRect(rect, paint);
        }
        return bitmap;
    }
}

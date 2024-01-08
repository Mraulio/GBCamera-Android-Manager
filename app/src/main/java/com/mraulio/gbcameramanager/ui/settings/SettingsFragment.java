package com.mraulio.gbcameramanager.ui.settings;

import static com.mraulio.gbcameramanager.MainActivity.exportSquare;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SettingsFragment extends Fragment {
    SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
    private boolean userSelect = false;
    private boolean userSelectPage = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Spinner spinnerExport = view.findViewById(R.id.spExportSize);
        Spinner spinnerImages = view.findViewById(R.id.spImagesPage);
        Spinner spinnerLanguage = view.findViewById(R.id.spLanguage);
        RadioButton rbPng = view.findViewById(R.id.rbPng);
        RadioButton rbTxt = view.findViewById(R.id.rbTxt);
        CheckBox cbPrint = view.findViewById(R.id.cbPrint);
        CheckBox cbPaperize = view.findViewById(R.id.cbPaperize);
        CheckBox cbMagicCheck = view.findViewById(R.id.cbMagic);
        CheckBox cbRotation = view.findViewById(R.id.cbRotation);
        CheckBox cbSquare = view.findViewById(R.id.cbSquare);

        MainActivity.current_fragment = MainActivity.CURRENT_FRAGMENT.SETTINGS;

        cbPrint.setChecked(MainActivity.printingEnabled);
        cbPrint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("print_enabled", true);
                    MainActivity.printingEnabled = true;
                } else {
                    editor.putBoolean("print_enabled", false);
                    MainActivity.printingEnabled = false;
                }
                editor.apply();
            }
        });

        cbPaperize.setChecked(MainActivity.showPaperizeButton);
        cbPaperize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("show_paperize_button", true);
                    MainActivity.showPaperizeButton = true;
                } else {
                    editor.putBoolean("show_paperize_button", false);
                    MainActivity.showPaperizeButton = false;
                }
                editor.apply();
            }
        });
        cbSquare.setChecked(exportSquare);
        cbSquare.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("export_square", true);
                    exportSquare = true;
                } else {
                    editor.putBoolean("export_square", false);
                    exportSquare = false;
                }
                editor.apply();
            }
        });

        if (MainActivity.exportPng) {
            rbPng.setChecked(true);
            spinnerExport.setEnabled(true);
        } else {
            rbTxt.setChecked(true);
            spinnerExport.setEnabled(false);
        }

        cbMagicCheck.setChecked(MainActivity.magicCheck);
        cbMagicCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    MainActivity.magicCheck = true;
                    editor.putBoolean("magic_check", true);
                } else {
                    MainActivity.magicCheck = false;
                    editor.putBoolean("magic_check", false);
                }
                editor.apply();
            }
        });

        rbPng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.exportPng = true;
                editor.putBoolean("export_as_png", true);
                editor.apply();
                spinnerExport.setEnabled(true);
            }
        });
        rbTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.exportPng = false;
                editor.putBoolean("export_as_png", false);
                editor.apply();
                spinnerExport.setEnabled(false);
            }
        });

        List<Integer> sizesInteger = new ArrayList<>();
        sizesInteger.add(1);
        sizesInteger.add(2);
        sizesInteger.add(4);
        sizesInteger.add(5);
        sizesInteger.add(6);
        sizesInteger.add(8);
        sizesInteger.add(10);

        List<String> sizes = new ArrayList<>();
        sizes.add("1x");
        sizes.add("2x");
        sizes.add("4x");
        sizes.add("5x");
        sizes.add("6x");
        sizes.add("8x");
        sizes.add("10x");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, sizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExport.setAdapter(adapter);
        spinnerExport.setSelection(sizesInteger.indexOf(MainActivity.exportSize));

        spinnerExport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // I set the export size on the Main activity int as the selected one
                MainActivity.exportSize = sizesInteger.get(position);
                editor.putInt("export_size", sizesInteger.get(position));
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        List<Integer> sizesIntegerImages = new ArrayList<>();
        sizesIntegerImages.add(6);
        sizesIntegerImages.add(9);
        sizesIntegerImages.add(12);
        sizesIntegerImages.add(15);
        sizesIntegerImages.add(18);
        sizesIntegerImages.add(30);

        List<String> sizesImages = new ArrayList<>();
        sizesImages.add("6");
        sizesImages.add("9");
        sizesImages.add("12");
        sizesImages.add("15");
        sizesImages.add("18");
        sizesImages.add("30");

        ArrayAdapter<String> adapterImages = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, sizesImages);
        adapterImages.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImages.setAdapter(adapterImages);
        spinnerImages.setSelection(sizesIntegerImages.indexOf(MainActivity.imagesPage));
        spinnerImages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelectPage) {
                    // I set the export size on the Main activity int as the selected one
                    MainActivity.imagesPage = sizesIntegerImages.get(position);
                    editor.putInt("images_per_page", sizesIntegerImages.get(position));
                    GalleryFragment.currentPage = 0;
                    editor.putInt("current_page", 0);
                    editor.apply();
                } else {
                    userSelectPage = true; // Because the spinner executes an item selection on startup
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        List<String> langs = new ArrayList<>();
        langs.add("en");
        langs.add("es");
        langs.add("de");
        langs.add("fr");
        langs.add("pt");//Need to change this for the Brazilian Region. Works for now as it's the only Portuguse

        List<String> languages = new ArrayList<>();
        languages.add("English (default)");
        languages.add("Español");
        languages.add("Deutsch");
        languages.add("Français");
        languages.add("Português Brasileiro");


        ArrayAdapter<String> adapterLanguage = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, languages);
        adapterImages.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerLanguage.setAdapter(adapterLanguage);
        spinnerLanguage.setSelection(langs.indexOf(MainActivity.languageCode));
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelect) {
                    // I set the export size on the Main activity int as the selected one
                    MainActivity.languageCode = langs.get(position);
                    ChangeLanguage(langs.get(position));
                } else {
                    userSelect = true; // Because the spinner executes an item selection on startup
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        cbRotation.setChecked(MainActivity.showRotationButton);
        cbRotation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("rotation_button", true);
                    MainActivity.showRotationButton = true;
                } else {
                    editor.putBoolean("rotation_button", false);
                    MainActivity.showRotationButton = false;
                }
                editor.apply();
            }
        });


        return view;
    }

    private void ChangeLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        Resources resources = getResources();
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        editor.putString("language", languageCode);
        editor.apply();
    }
}
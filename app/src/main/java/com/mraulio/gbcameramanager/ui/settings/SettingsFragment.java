package com.mraulio.gbcameramanager.ui.settings;

import static com.mraulio.gbcameramanager.utils.StaticValues.exportSquare;
import static com.mraulio.gbcameramanager.utils.Utils.backupDatabase;
import static com.mraulio.gbcameramanager.utils.Utils.restartApplication;
import static com.mraulio.gbcameramanager.utils.Utils.showDbBackups;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;
import com.mraulio.gbcameramanager.utils.StaticValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SettingsFragment extends Fragment {
    SharedPreferences.Editor editor = StaticValues.sharedPreferences.edit();
    private boolean userSelect = false;
    private boolean userSelectPage = false;
    private boolean userSelectLocale = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        Spinner spinnerExport = view.findViewById(R.id.spExportSize);
        Spinner spinnerImages = view.findViewById(R.id.spImagesPage);
        Spinner spinnerLanguage = view.findViewById(R.id.spLanguage);
        Spinner spinnerLocale = view.findViewById(R.id.sp_locale_date);
        RadioButton rbPng = view.findViewById(R.id.rbPng);
        RadioButton rbTxt = view.findViewById(R.id.rbTxt);
        CheckBox cbPrint = view.findViewById(R.id.cbPrint);
        CheckBox cbPaperize = view.findViewById(R.id.cbPaperize);
        CheckBox cbMagicCheck = view.findViewById(R.id.cbMagic);
        CheckBox cbExportMetadata = view.findViewById(R.id.cb_export_metadata);
        CheckBox cbRotation = view.findViewById(R.id.cbRotation);
        CheckBox cbAlwaysDefaultFrame = view.findViewById(R.id.cb_always_default_frame);
        CheckBox cbSquare = view.findViewById(R.id.cbSquare);
        Button btnExportDB = view.findViewById(R.id.btnExportDB);
        Button btnRestoreDB = view.findViewById(R.id.btnRestoreDB);

        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.SETTINGS;

        cbExportMetadata.setChecked(StaticValues.exportMetadata);
        cbExportMetadata.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("export_metadata", true);
                    StaticValues.exportMetadata = true;
                } else {
                    editor.putBoolean("export_metadata", false);
                    StaticValues.exportMetadata = false;
                }
                editor.apply();
            }
        });
        cbPrint.setChecked(StaticValues.printingEnabled);
        cbPrint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("print_enabled", true);
                    StaticValues.printingEnabled = true;
                } else {
                    editor.putBoolean("print_enabled", false);
                    StaticValues.printingEnabled = false;
                }
                editor.apply();
            }
        });

        cbPaperize.setChecked(StaticValues.showPaperizeButton);
        cbPaperize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("show_paperize_button", true);
                    StaticValues.showPaperizeButton = true;
                } else {
                    editor.putBoolean("show_paperize_button", false);
                    StaticValues.showPaperizeButton = false;
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

        if (StaticValues.exportPng) {
            rbPng.setChecked(true);
            spinnerExport.setEnabled(true);
        } else {
            rbTxt.setChecked(true);
            spinnerExport.setEnabled(false);
        }

        cbMagicCheck.setChecked(StaticValues.magicCheck);
        cbMagicCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    StaticValues.magicCheck = true;
                    editor.putBoolean("magic_check", true);
                } else {
                    StaticValues.magicCheck = false;
                    editor.putBoolean("magic_check", false);
                }
                editor.apply();
            }
        });

        rbPng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticValues.exportPng = true;
                editor.putBoolean("export_as_png", true);
                editor.apply();
                spinnerExport.setEnabled(true);
            }
        });
        rbTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticValues.exportPng = false;
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
        spinnerExport.setSelection(sizesInteger.indexOf(StaticValues.exportSize));

        spinnerExport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // I set the export size on the Main activity int as the selected one
                StaticValues.exportSize = sizesInteger.get(position);
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
        spinnerImages.setSelection(sizesIntegerImages.indexOf(StaticValues.imagesPage));
        spinnerImages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelectPage) {
                    // I set the export size on the Main activity int as the selected one
                    StaticValues.imagesPage = sizesIntegerImages.get(position);
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
        langs.add("ca");

        List<String> languages = new ArrayList<>();
        languages.add("English (default)");
        languages.add("Español");
        languages.add("Deutsch");
        languages.add("Français");
        languages.add("Português Brasileiro");
        languages.add("Català");

        ArrayAdapter<String> adapterLanguage = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, languages);
        adapterImages.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerLanguage.setAdapter(adapterLanguage);
        spinnerLanguage.setSelection(langs.indexOf(StaticValues.languageCode));
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelect) {
                    // I set the export size on the Main activity int as the selected one
                    StaticValues.languageCode = langs.get(position);
                    changeLanguage(langs.get(position));
                } else {
                    userSelect = true; // Because the spinner executes an item selection on startup
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        List<String> locales = new ArrayList<>();
        locales.add("yyyy-MM-dd");
        locales.add("yyyy-dd-MM");

        List<String> localeSpinner = new ArrayList<>();
        localeSpinner.add("D-M-Y  Ej: 27-04-2024");
        localeSpinner.add("M-D-Y  Ej: 04-27-2024");

        ArrayAdapter<String> adapterLocale = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, localeSpinner);
        adapterImages.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerLocale.setAdapter(adapterLocale);
        spinnerLocale.setSelection(locales.indexOf(StaticValues.dateLocale));
        spinnerLocale.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelectLocale) {
                    // I set the export size on the Main activity int as the selected one
                    StaticValues.dateLocale = locales.get(position);
                    editor.putString("date_locale", locales.get(position));
                    editor.apply();
                } else {
                    userSelectLocale = true; // Because the spinner executes an item selection on startup
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        cbRotation.setChecked(StaticValues.showRotationButton);
        cbRotation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("rotation_button", true);
                    StaticValues.showRotationButton = true;
                } else {
                    editor.putBoolean("rotation_button", false);
                    StaticValues.showRotationButton = false;
                }
                editor.apply();
            }
        });

        cbAlwaysDefaultFrame.setChecked(StaticValues.alwaysDefaultFrame);
        cbAlwaysDefaultFrame.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editor.putBoolean("always_default_frame", true);
                    StaticValues.alwaysDefaultFrame = true;
                } else {
                    editor.putBoolean("always_default_frame", false);
                    StaticValues.alwaysDefaultFrame = false;
                }
                editor.apply();
            }
        });


        btnExportDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backupDatabase(getContext());
            }
        });
        btnRestoreDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDbBackups(getContext(), getActivity());
            }
        });
        return view;
    }

    private void changeLanguage(String languageCode) {
        editor.putString("language", languageCode);
        editor.commit();
        restartApplication(getContext());
    }


}
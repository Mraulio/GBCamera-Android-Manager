package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.hiddenFilterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectionMode;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkFilterPass;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_DUPLICATED;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.FILTER_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_DUPLICATED;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_SUPER_FAVOURITE;
import static com.mraulio.gbcameramanager.utils.StaticValues.TAG_TRANSFORMED;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.hiddenTags;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.selectedFilterTags;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updateGridView;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.checkSorting;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.compareTags;
import static com.mraulio.gbcameramanager.ui.gallery.MetadataValues.metadataTexts;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.retrieveTags;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.tagsHash;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.TouchImageView;
import com.mraulio.gbcameramanager.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class BigImageDialog {


    List<GbcImage> filteredGbcImages;
    Context context;
    Activity activity;
    boolean hideAllMultipleImage;
    HashSet<String> removedTags = new HashSet<>();

    public BigImageDialog(List<GbcImage> filteredGbcImages, Context context, Activity activity) {
        this.filteredGbcImages = filteredGbcImages;
        this.context = context;
        this.activity = activity;
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    public void showBigImageDialogSingleImage(int globalImageIndex, ImageView previousImageView, Dialog previousDialog) {
        Bitmap bitmap = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode());
        bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.big_image_dialog);

        final List<String>[] originalTags = new List[]{new ArrayList<>(filteredGbcImages.get(globalImageIndex).getTags())};
        List<String> tempTags = new ArrayList<>(filteredGbcImages.get(globalImageIndex).getTags());

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        TouchImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setMaxZoom(5);
        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false));
        Button btnOkWriteTag = dialog.findViewById(R.id.btnOkWriteTag);
        RadioButton rbEditTags = dialog.findViewById(R.id.rbEditTags);
        RadioButton rbMisc = dialog.findViewById(R.id.rbMisc);
        Button btnUpdateImage = dialog.findViewById(R.id.btnUpdateImage);

        //EditText Image Name
        EditText etImageName = dialog.findViewById(R.id.etImageName);
        etImageName.setText(filteredGbcImages.get(globalImageIndex).getName());
        String originalName = new String(filteredGbcImages.get(globalImageIndex).getName());
        final String[] newName = {""};
        final boolean[] editingName = {false};
        boolean[] editingTags = {false};

        etImageName.addTextChangedListener(new TextWatcher() {
            String placeholderString = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placeholderString = etImageName.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeholderString = etImageName.getText().toString().trim();

                if (!originalName.equals(placeholderString.trim().toLowerCase(Locale.ROOT))) {
                    etImageName.setBackgroundColor(context.getColor(R.color.update_image_color));
                    editingName[0] = true;
                    newName[0] = new String(placeholderString);
                } else {
                    etImageName.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    editingName[0] = false;
                }
                if (editingTags[0] || editingName[0]) {
                    btnUpdateImage.setEnabled(true);
                } else btnUpdateImage.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //Autocomplete text view Text Write tag
        AutoCompleteTextView autoCAddTag = dialog.findViewById(R.id.etWriteTag);
        List<String> availableTotalTags = new ArrayList<>(tagsHash);
        List<String> availableTotalTagsSpinner = new ArrayList<>();
        List<String> availableTotalTagsAutoComplete = new ArrayList<>();

        for (String tag : availableTotalTags) {
            if (tag.equals(FILTER_FAVOURITE)) {
                tag = TAG_FAVOURITE;
            } else if (tag.equals(FILTER_SUPER_FAVOURITE)) {
                tag = TAG_SUPER_FAVOURITE;
                ; // Not adding this tags, as they are non removable
            } else if (tag.equals(FILTER_DUPLICATED)) {
                continue; // Not adding this tags, as they are non removable
            } else if (tag.equals(FILTER_TRANSFORMED)) {
                continue;
            }
            availableTotalTagsAutoComplete.add(tag);
        }
        availableTotalTagsSpinner.add("~ " + context.getString(R.string.tags_dialog_title) + " ~");
        availableTotalTagsSpinner.addAll(availableTotalTagsAutoComplete);

        ArrayAdapter<String> adapterAutoComplete = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, availableTotalTagsAutoComplete);

        autoCAddTag.setAdapter(adapterAutoComplete);
        autoCAddTag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(autoCAddTag.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        TextView tvCreationDate = dialog.findViewById(R.id.tvMetadata);
        StringBuilder stringBuilder = new StringBuilder();
        String loc;
        if (dateLocale.equals("yyyy-MM-dd")) {
            loc = "dd/MM/yyyy";
        } else {
            loc = "MM/dd/yyyy";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(loc + " HH:mm:ss");

        LinkedHashMap lhm = filteredGbcImages.get(globalImageIndex).getImageMetadata();
        stringBuilder.append(sdf.format(filteredGbcImages.get(globalImageIndex).getCreationDate()).toString() + "\n");

        if (lhm != null) { //Last seen images don't have metadata
            for (Object key : lhm.keySet()) {
                if (key.equals("frameIndex")) continue;
                String metadata = metadataTexts.get(key);
                String value = (String) lhm.get(key);
                if (metadata == null) {
                    metadata = (String) key;
                }
                stringBuilder.append(metadata).append(": ").append(value).append("\n");
                if (key.equals("isCopy")) stringBuilder.append("\n");
            }
        }

        tvCreationDate.setText(stringBuilder.toString());

        LinearLayout editTagsLayout = dialog.findViewById(R.id.editTagsLayout);
        LinearLayout miscLayout = dialog.findViewById(R.id.miscLayout);


        final boolean[] rbEditWasSelected = {false};
        final boolean[] rbMiscWasSelected = {false};

        rbEditTags.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (!rbEditWasSelected[0]) {
                    rbEditWasSelected[0] = true;
                    rbMiscWasSelected[0] = false;
                    editTagsLayout.setVisibility(VISIBLE);
                    miscLayout.setVisibility(GONE);
                } else {
                    editTagsLayout.setVisibility(GONE);
                    rbEditTags.setChecked(false);
                    rbEditWasSelected[0] = false;

                }
            }
        });

        rbMisc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!rbMiscWasSelected[0]) {
                    rbMiscWasSelected[0] = true;
                    rbEditWasSelected[0] = false;

                    editTagsLayout.setVisibility(GONE);
                    miscLayout.setVisibility(VISIBLE);
                } else {
                    rbMisc.setChecked(false);
                    miscLayout.setVisibility(GONE);
                    rbMiscWasSelected[0] = false;
                }
            }
        });

        LinearLayout tagsLayout = dialog.findViewById(R.id.tagsCheckBoxes);

        Spinner spAvailableTags = dialog.findViewById(R.id.spAvailableTags);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, availableTotalTagsSpinner);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAvailableTags.setAdapter(adapter);
        final boolean[] isSpinnerTouched = {false};

        btnOkWriteTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newTag = autoCAddTag.getText().toString().trim();
                if (newTag.isEmpty())
                    return;
                if (newTag.equals(TAG_FAVOURITE)) {
                    newTag = FILTER_FAVOURITE;//Reverse the tag
                } else if (newTag.equals(TAG_SUPER_FAVOURITE)) {
                    newTag = FILTER_SUPER_FAVOURITE;
                } else if (newTag.equals(TAG_DUPLICATED)) {
                    newTag = FILTER_DUPLICATED;
                } else if (newTag.equals(TAG_TRANSFORMED)) {
                    newTag = FILTER_TRANSFORMED;
                }
                if (!tempTags.contains(newTag)) {
                    //Generate dynamically new checkboxes
                    createTagCheckBoxSingleImage(newTag, tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);

                    tempTags.add(newTag);
                    editingTags[0] = compareTags(originalTags[0], tempTags);
                    if (editingTags[0]) {
                        tagsLayout.setBackgroundColor(context.getColor(R.color.update_image_color));
                    } else {
                        tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                    }
                    if (editingTags[0] || editingName[0]) {
                        btnUpdateImage.setEnabled(true);
                    } else btnUpdateImage.setEnabled(false);
                }
            }
        });

        spAvailableTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerTouched[0]) {
                    isSpinnerTouched[0] = true;
                    return;
                }
                if (position == 0) {
                    return;
                }
                String selectedTag = adapter.getItem(position);
                if (selectedTag.equals(TAG_FAVOURITE)) {
                    selectedTag = FILTER_FAVOURITE;//Reverse the tag
                } else if (selectedTag.equals(TAG_SUPER_FAVOURITE)) {
                    selectedTag = FILTER_SUPER_FAVOURITE;
                } else if (selectedTag.equals(TAG_DUPLICATED)) {
                    selectedTag = FILTER_DUPLICATED;
                } else if (selectedTag.equals(TAG_TRANSFORMED)) {
                    selectedTag = FILTER_TRANSFORMED;
                }
                if (!tempTags.contains(selectedTag)) {
                    //Generate dynamically new checkboxes
                    createTagCheckBoxSingleImage(adapter.getItem(position), tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);
                    tempTags.add(selectedTag);
                    editingTags[0] = compareTags(originalTags[0], tempTags);
                    if (editingTags[0]) {
                        tagsLayout.setBackgroundColor(context.getColor(R.color.update_image_color));
                    } else {
                        tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                    }
                    if (editingTags[0] || editingName[0]) {
                        btnUpdateImage.setEnabled(true);
                    } else btnUpdateImage.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        for (String tag : filteredGbcImages.get(globalImageIndex).getTags()) {
            createTagCheckBoxSingleImage(tag, tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);

        }
        btnUpdateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GbcImage gbcImageToUpdate = filteredGbcImages.get(globalImageIndex);
                HashSet<String> tagsToSave = null;
                if (editingName[0]) {
                    gbcImageToUpdate.setName(newName[0]);
                }
                if (editingTags[0]) {
                    if (tempTags.contains(FILTER_FAVOURITE)) {
                        previousImageView.setBackgroundColor(context.getColor(R.color.favorite));
                    } else {
                        previousImageView.setBackgroundColor(context.getColor(R.color.imageview_bg));
                    }
                    tagsToSave = new HashSet<>(tempTags);//So it doesn't follow the temptags if I select another
                    gbcImageToUpdate.setTags(tagsToSave);
                }

                new UpdateImageAsyncTask(gbcImageToUpdate).execute();
                retrieveTags(gbcImagesList);
                originalTags[0] = new ArrayList<>(tempTags);

                tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                etImageName.setBackgroundColor(context.getColor(R.color.white));

                if (!checkFilterPass(gbcImageToUpdate)) { // If image is being removed from the filtered images, dismiss the dialogs
                    if (previousDialog != null && previousDialog.isShowing()) {
                        dialog.dismiss();
                        previousDialog.dismiss();
                    }
                }

                checkSorting(context);
                updateGridView();
                btnUpdateImage.setEnabled(false);
            }
        });

        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    public void showBigImageDialogMultipleImages(List<Integer> selectedImages, ImageView previousImageView, Dialog previousDialog) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.big_image_dialog);

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setVisibility(GONE);//Remove the single image imageview and add them dynamically
        List<ImageView> imageViewList = new ArrayList<>();

        LinearLayout layoutSelected = dialog.findViewById(R.id.lyMultipleImages);

        for (int i = 0; i < selectedImages.size(); i++) {
            GbcImage gbcImage = filteredGbcImages.get(selectedImages.get(i));

            ImageView imageViewMini = new ImageView(context);
            imageViewMini.setId(i);
            imageViewMini.setPadding(5, 5, 5, 5);
            Bitmap image = Utils.imageBitmapCache.get(gbcImage.getHashCode());
            imageViewMini.setImageBitmap(rotateBitmap(image, gbcImage));

            imageViewList.add(imageViewMini);
            layoutSelected.addView(imageViewMini);

        }

        final HashSet<String>[] originalTags = new HashSet[]{new HashSet<>()};
        for (Integer imageIndex : selectedImages) {
            GbcImage gbcImage = filteredGbcImages.get(imageIndex);
            HashSet<String> tags = gbcImage.getTags();
            originalTags[0].addAll(tags);
        }

        HashSet<String> tempTags = new HashSet<>();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;


        Button btnOkWriteTag = dialog.findViewById(R.id.btnOkWriteTag);
        RadioButton rbEditTags = dialog.findViewById(R.id.rbEditTags);
        RadioButton rbMisc = dialog.findViewById(R.id.rbMisc);
        rbMisc.setVisibility(GONE);
        Button btnUpdateImage = dialog.findViewById(R.id.btnUpdateImage);

        //EditText Image Name
        EditText etImageName = dialog.findViewById(R.id.etImageName);
        String originalName = new String(filteredGbcImages.get(selectedImages.get(0)).getName());

        etImageName.setText(originalName);

        final String[] newName = {""};

        final boolean[] editingName = {false};
        boolean[] editingTags = {false};

        etImageName.addTextChangedListener(new TextWatcher() {
            String placeholderString = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                placeholderString = etImageName.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeholderString = etImageName.getText().toString().trim();

                if (!originalName.equals(placeholderString.trim().toLowerCase(Locale.ROOT))) {
                    etImageName.setBackgroundColor(context.getColor(R.color.update_image_color));
                    etImageName.setTextColor(context.getColor(R.color.black));
                    editingName[0] = true;
                    newName[0] = new String(placeholderString);
                }

                btnUpdateImage.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


//        //Autocomplete text view Text Write tag
        AutoCompleteTextView autoCAddTag = dialog.findViewById(R.id.etWriteTag);
        List<String> availableTotalTags = new ArrayList<>(tagsHash);
        List<String> availableTotalTagsSpinner = new ArrayList<>();
        List<String> availableTotalTagsAutoComplete = new ArrayList<>();
        List<String> showingTags = new ArrayList<>(filteredGbcImages.get(selectedImages.get(0)).getTags());
        for (String tag : availableTotalTags) {
            if (tag.equals(FILTER_FAVOURITE)) {
                tag = TAG_FAVOURITE;
            } else if (tag.equals(FILTER_SUPER_FAVOURITE)) {
                tag = TAG_SUPER_FAVOURITE;
                ; // Not adding this tags, as they are non removable
            } else if (tag.equals(FILTER_DUPLICATED)) {
                continue; // Not adding this tags, as they are non removable
            } else if (tag.equals(FILTER_TRANSFORMED)) {
                continue;
            }
            availableTotalTagsAutoComplete.add(tag);
        }
        availableTotalTagsSpinner.add("~ " + context.getString(R.string.tags_dialog_title) + " ~");
        availableTotalTagsSpinner.addAll(availableTotalTagsAutoComplete);

        ArrayAdapter<String> adapterAutoComplete = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, availableTotalTagsAutoComplete);

        autoCAddTag.setAdapter(adapterAutoComplete);
        autoCAddTag.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(autoCAddTag.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        LinearLayout editTagsLayout = dialog.findViewById(R.id.editTagsLayout);
        editTagsLayout.setVisibility(VISIBLE);

        rbEditTags.setChecked(true);

        LinearLayout tagsLayout = dialog.findViewById(R.id.tagsCheckBoxes);

        Spinner spAvailableTags = dialog.findViewById(R.id.spAvailableTags);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, availableTotalTagsSpinner);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAvailableTags.setAdapter(adapter);
        final boolean[] isSpinnerTouched = {false};

        btnOkWriteTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newTag = autoCAddTag.getText().toString().trim();
                if (newTag.isEmpty())
                    return;
                if (newTag.equals(TAG_FAVOURITE)) {
                    newTag = FILTER_FAVOURITE;//Reverse the tag
                } else if (newTag.equals(TAG_SUPER_FAVOURITE)) {
                    newTag = FILTER_SUPER_FAVOURITE;
                } else if (newTag.equals(FILTER_DUPLICATED)) {
                    newTag = TAG_DUPLICATED;
                } else if (newTag.equals(FILTER_TRANSFORMED)) {
                    newTag = TAG_TRANSFORMED;
                }
                if (!tempTags.contains(newTag)) {
                    //Generate dynamically new checkboxes
                    createTagCheckBoxMultipleImages(newTag, tagsLayout, tempTags, editingTags, btnUpdateImage, true);

                    tempTags.add(newTag);

                    editingTags[0] = true;

                    btnUpdateImage.setEnabled(true);
                }
            }
        });

        spAvailableTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerTouched[0]) {
                    isSpinnerTouched[0] = true;
                    return;
                }
                if (position == 0) {
                    return;
                }
                String selectedTag = adapter.getItem(position);
                if (selectedTag.equals(FILTER_FAVOURITE)) {
                    selectedTag = TAG_FAVOURITE;
                } else if (selectedTag.equals(FILTER_SUPER_FAVOURITE)) {
                    selectedTag = TAG_SUPER_FAVOURITE;
                } else if (selectedTag.equals(FILTER_DUPLICATED)) {
                    selectedTag = TAG_DUPLICATED;
                } else if (selectedTag.equals(FILTER_TRANSFORMED)) {
                    selectedTag = TAG_TRANSFORMED;
                }
                if (!showingTags.contains(selectedTag)) {
                    if (!tempTags.contains(selectedTag)) {
                        //Generate dynamically new checkboxes
                        createTagCheckBoxMultipleImages(adapter.getItem(position), tagsLayout, tempTags, editingTags, btnUpdateImage, true);
                        tempTags.add(selectedTag);

                    }
                }
                editingTags[0] = true;
                btnUpdateImage.setEnabled(true);
//                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        for (String tag : originalTags[0]) {
            createTagCheckBoxMultipleImages(tag, tagsLayout, tempTags, editingTags, btnUpdateImage, false);
        }

        btnUpdateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nameIndex = 1;
                int maxIndex = selectedImages.size();
                int numDigits = String.valueOf(maxIndex).length();

                String formatString = "%0" + numDigits + "d";
                HashSet<String> tagsToSave = null;//So it doesn't follow the temptags if I select another
                for (Integer imageIndex : selectedImages) {
                    String formattedIndex = String.format(formatString, nameIndex);
                    tagsToSave = new HashSet<>();
                    boolean saveImage = false;
                    GbcImage gbcImageToUpdate = filteredGbcImages.get(imageIndex);
                    if (editingName[0]) {
                        saveImage = true;
                        gbcImageToUpdate.setName(newName[0] + "_" + formattedIndex);
                        nameIndex++;
                    }
                    if (editingTags[0]) {
                        saveImage = true;
                        if (tempTags.contains(FILTER_SUPER_FAVOURITE)) {
                            previousImageView.setBackgroundColor(context.getColor(R.color.star_color));
                        } else if (tempTags.contains(FILTER_FAVOURITE)) {
                            previousImageView.setBackgroundColor(context.getColor(R.color.favorite));
                        } else {
                            previousImageView.setBackgroundColor(context.getColor(R.color.imageview_bg));
                        }
                        tagsToSave.addAll(gbcImageToUpdate.getTags());//Add all previous tags from the image
                        tagsToSave.addAll(tempTags);//Add all the new tags
                        for (String st : removedTags) {
                            if (tagsToSave.contains(st)) {
                                tagsToSave.remove(st);//Remove the deselected tags
                            }
                        }
                        gbcImageToUpdate.setTags(tagsToSave);
                    }

                    if (saveImage) {
                        new UpdateImageAsyncTask(gbcImageToUpdate).execute();
                        retrieveTags(gbcImagesList);
                        originalTags[0] = new HashSet<>(tempTags);
                    }
                }

                tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                etImageName.setBackgroundColor(context.getColor(R.color.white));

                //If one of the tags removed from the image is in the tags filtered, clear selected images, hide fab, hide dialog...

                if (checkIfTagsHide(removedTags, tagsToSave)) {
                    selectedImages.clear();
                    showEditMenuButton = false;
                    StaticValues.fab.hide();
                    selectionMode[0] = false;
                    activity.invalidateOptionsMenu();
                    if (previousDialog != null && previousDialog.isShowing()) {
                        previousDialog.dismiss();
                    }
                }

                updateGridView();
                dialog.dismiss();
            }
        });

        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGridView();
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }


    /**
     * For the tags checkboxes for editing only one image
     *
     * @param tag
     */
    private void createTagCheckBoxSingleImage(String tag, LinearLayout tagsLayout, List<String> tempTags, int imageIndex, boolean[] editingTags, boolean editingName, Button btnUpdateImages) {
        CheckBox tagCb = new CheckBox(context);
        String cbText;
        boolean removableTag = true;
        if (tag.equals(FILTER_FAVOURITE)) {
            cbText = TAG_FAVOURITE;
        } else if (tag.equals(FILTER_SUPER_FAVOURITE)) {
            cbText = TAG_SUPER_FAVOURITE;
        } else if (tag.equals(FILTER_DUPLICATED)) {
            cbText = TAG_DUPLICATED;
            removableTag = false;
        } else if (tag.equals(FILTER_TRANSFORMED)) {
            cbText = TAG_TRANSFORMED;
            removableTag = false;
        } else cbText = tag;

        tagCb.setText(cbText);
        tagCb.setChecked(true);
        tagCb.setEnabled(removableTag);
        String finalTag = tag;
        tagCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> originalTags = new ArrayList<>(filteredGbcImages.get(imageIndex).getTags());
                if (tagCb.isChecked()) {
                    if (!tempTags.contains(finalTag))
                        tempTags.add(finalTag);
                } else {
                    if (tempTags.contains(finalTag))
                        tempTags.remove(finalTag);
                }

                editingTags[0] = compareTags(originalTags, tempTags);
                if (editingTags[0]) {
                    tagsLayout.setBackgroundColor(context.getColor(R.color.update_image_color));
                } else {
                    tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                }
                if (editingTags[0] || editingName) {
                    btnUpdateImages.setEnabled(true);
                } else btnUpdateImages.setEnabled(false);
            }
        });

        tagsLayout.addView(tagCb);
    }

    /**
     * For the tags checkboxes for editing multiple images
     *
     * @param tag
     */
    private void createTagCheckBoxMultipleImages(String tag, LinearLayout tagsLayout, HashSet<String> tempTags, boolean[] editingTags, Button btnUpdateImages, boolean autoCheck) {
        CheckBox tagCb = new CheckBox(context);
        if (autoCheck) {
            tagCb.setButtonDrawable(android.R.drawable.checkbox_on_background);
            tagCb.setBackgroundColor(context.getColor(R.color.save_color));

        } else {
            tagCb.setButtonDrawable(android.R.drawable.checkbox_off_background);
        }
        String cbText;
        boolean removableTag = true;

        if (tag.equals(FILTER_FAVOURITE)) {
            cbText = TAG_FAVOURITE;
        } else if (tag.equals(FILTER_SUPER_FAVOURITE)) {
            cbText = TAG_SUPER_FAVOURITE;
        } else if (tag.equals(FILTER_DUPLICATED)) {
            cbText = TAG_DUPLICATED;
            removableTag = false;
        } else if (tag.equals(FILTER_TRANSFORMED)) {
            cbText = TAG_TRANSFORMED;
            removableTag = false;
        } else cbText = tag;

        tagCb.setText(cbText);
        tagCb.setChecked(true);
        tagCb.setEnabled(removableTag);

        String finalTag = tag;
        tagCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tempTags.contains(finalTag)) {
                    tempTags.add(finalTag);
                    tagCb.setBackgroundColor(context.getColor(R.color.save_color));
                    tagCb.setButtonDrawable(android.R.drawable.checkbox_on_background);
                    if (removedTags.contains(finalTag)) {
                        removedTags.remove(finalTag);
                    }
                } else if (tempTags.contains(finalTag)) {
                    hideAllMultipleImage = true;
                    tempTags.remove(finalTag);
                    removedTags.add(finalTag);
                    tagCb.setBackgroundColor(context.getColor(R.color.listview_selected));
                    tagCb.setButtonDrawable(android.R.drawable.checkbox_off_background);
                }
                editingTags[0] = true;

                btnUpdateImages.setEnabled(true);
            }
        });
        tagsLayout.addView(tagCb);
    }

    private boolean checkIfTagsHide(HashSet<String> removedTags, HashSet<String> tagsToSave) {
        for (String tag : removedTags) {
            if (selectedFilterTags.contains(tag))
                return true;
        }
        //If I'm adding a new tag that's included in the hidden tags
        for (String tag : tagsToSave) {
            if (hiddenFilterTags.contains(tag))
                return true;
        }
        return false;
    }

}

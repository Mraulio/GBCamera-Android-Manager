package com.mraulio.gbcameramanager.ui.gallery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.updateGridView;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.compareTags;
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
import android.view.MotionEvent;
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
import com.mraulio.gbcameramanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BigImageDialog {


    List<GbcImage> filteredGbcImages;
    Context context;
    int currentPage;
    Activity activity;


    public BigImageDialog(List<GbcImage> filteredGbcImages, Context context, int currentPage, Activity activity) {
        this.filteredGbcImages = filteredGbcImages;
        this.context = context;
        this.currentPage = currentPage;
        this.activity = activity;
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    public void showBigImageDialog(int globalImageIndex, ImageView previousImageView) {
        Bitmap bitmap = Utils.imageBitmapCache.get(filteredGbcImages.get(globalImageIndex).getHashCode());
        bitmap = rotateBitmap(bitmap, filteredGbcImages.get(globalImageIndex));
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.single_image_dialog);

        final List<String>[] originalTags = new List[]{new ArrayList<>(filteredGbcImages.get(globalImageIndex).getTags())};
        List<String> tempTags = new ArrayList<>(filteredGbcImages.get(globalImageIndex).getTags());

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false));
        Button btnOkWriteTag = dialog.findViewById(R.id.btnOkWriteTag);
        RadioButton rbEditTags = dialog.findViewById(R.id.rbEditTags);
        RadioButton rbMisc = dialog.findViewById(R.id.rbMisc);
        Button btnUpdateImage = dialog.findViewById(R.id.btnSaveTags);

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
        List<String> availableTotalTagsFav = new ArrayList<>();
        for (String tag : availableTotalTags) {
            if (tag.equals("__filter:favourite__")) {
                tag = "Favourite \u2764\ufe0f";
            }
            availableTotalTagsFav.add(tag);
        }

        ArrayAdapter<String> adapterAutoComplete = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, availableTotalTagsFav);

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


        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });


        rbEditTags.setChecked(true);
        TextView tvCreationDate = dialog.findViewById(R.id.tvCreationDate);

        tvCreationDate.setText(filteredGbcImages.get(globalImageIndex).getCreationDate().toString());
        LinearLayout editTagsLayout = dialog.findViewById(R.id.editTagsLayout);
        LinearLayout miscLayout = dialog.findViewById(R.id.miscLayout);

        rbEditTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTagsLayout.setVisibility(VISIBLE);
                miscLayout.setVisibility(GONE);

            }
        });
        rbMisc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editTagsLayout.setVisibility(GONE);
                miscLayout.setVisibility(VISIBLE);

            }
        });

        LinearLayout tagsLayout = dialog.findViewById(R.id.tagsCheckBoxes);

        Spinner spAvailableTags = dialog.findViewById(R.id.spAvailableTags);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, availableTotalTagsFav);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAvailableTags.setSelection(3);
        spAvailableTags.setAdapter(adapter);
        final boolean[] isSpinnerTouched = {false};

        btnOkWriteTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newTag = autoCAddTag.getText().toString().trim();
                if (newTag.isEmpty())
                    return;
                if (newTag.equals("Favourite \u2764\ufe0f")) {
                    newTag = "__filter:favourite__";//Reverse the tag
                }
                if (!tempTags.contains(newTag)) {
                    //Generate dynamically new checkboxes
                    createTagCheckBox(newTag, tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);

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

        spAvailableTags.setOnTouchListener(new View.OnTouchListener() {
            //So the first item is not added on spinner startup
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isSpinnerTouched[0] = true;
                return false;
            }
        });
        spAvailableTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerTouched[0]) return;
                String selectedTag = adapter.getItem(position);
                if (selectedTag.equals("Favourite \u2764\ufe0f")) {
                    selectedTag = "__filter:favourite__";//Reverse the tag
                }
                if (!tempTags.contains(selectedTag)) {
                    //Generate dynamically new checkboxes
                    createTagCheckBox(adapter.getItem(position), tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);
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
            createTagCheckBox(tag, tagsLayout, tempTags, globalImageIndex, editingTags, editingName[0], btnUpdateImage);

        }
        btnUpdateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GbcImage gbcImageToUpdate = filteredGbcImages.get(globalImageIndex);
                if (editingName[0]) {
                    gbcImageToUpdate.setName(newName[0]);
                }
                if (editingTags[0]) {
                    if (tempTags.contains("__filter:favourite__")) {
                        previousImageView.setBackgroundColor(context.getColor(R.color.favorite));
                    } else {
                        previousImageView.setBackgroundColor(context.getColor(R.color.imageview_bg));
                    }
                    List<String> tagsToSave = new ArrayList<>(tempTags);//So it doesn't follow the temptags if I select another
                    gbcImageToUpdate.setTags(tagsToSave);
                }

                new SaveImageAsyncTask(gbcImageToUpdate).execute();
                retrieveTags(gbcImagesList);
                originalTags[0] = new ArrayList<>(tempTags);

                tagsLayout.setBackgroundColor(context.getColor(R.color.white));
                etImageName.setBackgroundColor(context.getColor(R.color.white));

                btnUpdateImage.setEnabled(false);
            }
        });

        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateGridView(currentPage);
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }

    /**
     * For the tags checkboxes
     *
     * @param tag
     */
    private void createTagCheckBox(String tag, LinearLayout tagsLayout, List<String> tempTags, int imageIndex, boolean[] editingTags, boolean editingName, Button btnUpdateImages) {
        CheckBox tagCb = new CheckBox(context);
        String cbText;
        if (tag.equals("__filter:favourite__")) {
            cbText = "Favourite \u2764\ufe0f";
        } else cbText = tag;

        tagCb.setText(cbText);
        tagCb.setChecked(true);

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
}

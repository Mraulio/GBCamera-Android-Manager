package com.mraulio.gbcameramanager.ui.importFile;

import static com.mraulio.gbcameramanager.utils.StaticValues.db;
import static com.mraulio.gbcameramanager.ui.frames.FramesFragment.customGridViewAdapterFrames;
import static com.mraulio.gbcameramanager.ui.frames.FramesFragment.frameGroupIds;
import static com.mraulio.gbcameramanager.ui.frames.FramesFragment.reloadFrameGroupsSpinner;
import static com.mraulio.gbcameramanager.ui.frames.FramesFragment.spFrameGroups;
import static com.mraulio.gbcameramanager.ui.importFile.ImportFragment.importedFrameGroupIdNames;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupSorting;
import static com.mraulio.gbcameramanager.utils.Utils.frameGroupsNames;
import static com.mraulio.gbcameramanager.utils.Utils.framesList;
import static com.mraulio.gbcameramanager.utils.Utils.gbcImagesList;
import static com.mraulio.gbcameramanager.utils.Utils.generateHashFromBytes;
import static com.mraulio.gbcameramanager.utils.Utils.hashFrames;
import static com.mraulio.gbcameramanager.utils.Utils.transparentBitmap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.db.FrameDao;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.UpdateFrameAsyncTask;
import com.mraulio.gbcameramanager.ui.gallery.UpdateImageAsyncTask;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class FrameImportDialogClass {

    Bitmap frameBitmap;
    Context context;
    GbcFrame gbcFrame;
    boolean editingFrame;
    String oldFrameGroupId;
    String oldFrameId;
    boolean putSpToCero;


    public FrameImportDialogClass(Bitmap frameBitmap, Context context, GbcFrame gbcFrame, boolean editingFrame) {
        this.frameBitmap = frameBitmap;
        this.context = context;
        this.gbcFrame = gbcFrame;
        this.editingFrame = editingFrame;
    }

    public void frameImportDialog() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.frame_import_dialog, null);
        ImageView ivFrame = view.findViewById(R.id.ivFrame);
        if (gbcFrame == null) {
            gbcFrame = new GbcFrame();
            gbcFrame.setFrameBitmap(frameBitmap);

        } else{
            oldFrameGroupId = new String(gbcFrame.getFrameId().replaceAll("^(\\D+).*", "$1"));
            oldFrameId = new String(gbcFrame.getFrameId());
        }
        Button btnDecrement = view.findViewById(R.id.decrementButton);
        Button btnIncrement = view.findViewById(R.id.incrementButton);
        EditText etFrameIndex = view.findViewById(R.id.numberEditText);
        AutoCompleteTextView autoCompNewId = view.findViewById(R.id.etFrameId);
        EditText etFrameName = view.findViewById(R.id.etFrameName);
        EditText etFrameGroupName = view.findViewById(R.id.etFrameGroupName);
        Spinner spFrameGroupDialog = view.findViewById(R.id.spFrameGroupsDialog);

        List<String> frameGroupsNamesList = new ArrayList<>();
        List<String> frameGroupsIdsList = new ArrayList<>();

        for (String key : frameGroupsNames.keySet()) {
            frameGroupsIdsList.add(key);
            frameGroupsNamesList.add(frameGroupsNames.get(key));
        }

        List<String> spFrameGroupNamesList = new ArrayList<>();
        spFrameGroupNamesList.add(context.getString(R.string.sp_new_frame_group));
        spFrameGroupNamesList.addAll(frameGroupsNamesList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, spFrameGroupNamesList);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spFrameGroupDialog.setAdapter(adapter);

        final String[] frameName = {""};
        final String[] frameId = {""};
        final String[] frameGroupId = {""};


        final boolean[] validId = {false};
        final int MIN_INDEX = 1;
        final int MAX_INDEX = 99;

        final int[] index = {1};

        if (editingFrame) {
            index[0] = Integer.parseInt(gbcFrame.getFrameId().replaceAll("^.*?(\\d+)$", "$1")); //To get the index numbers from the end of the id
            etFrameIndex.setText(String.valueOf(index[0]));

            frameName[0] = gbcFrame.getFrameName();
            etFrameName.setText(frameName[0]);

            frameGroupId[0] = gbcFrame.getFrameId().replaceAll("^(\\D+).*", "$1");
            autoCompNewId.setText(frameGroupId[0]);
            validId[0] = true;

            frameId[0] = gbcFrame.getFrameId();
            spFrameGroupDialog.setSelection(frameGroupIds.indexOf(frameGroupId[0])+1);//+1 because it has the New Frame group
        }

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (index[0] > MIN_INDEX) {
                    index[0]--;
                    etFrameIndex.setText(String.valueOf(index[0]));
                    frameId[0] = generateFrameId(autoCompNewId.getText().toString().trim(), index[0]);
                    if (!editingFrame) {
                        validId[0] = checkExistingIdIndex(frameId[0]);
                    } else {
                        if (frameId[0].equals(gbcFrame.getFrameId())) {
                            validId[0] = true;//If it's the same frame Id as the one it already has, it's a valid id
                        } else {
                            validId[0] = checkExistingIdIndex(frameId[0]);
                        }
                    }
                    if (!validId[0]) {
                        etFrameIndex.setError(context.getString(R.string.et_frame_id_error));
                    } else {
                        etFrameIndex.setError(null);
                    }
                }
            }
        });

        etFrameIndex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    index[0] = Integer.valueOf(text);
                }
                frameId[0] = generateFrameId(autoCompNewId.getText().toString().trim(), index[0]);
                if (!editingFrame) {
                    validId[0] = checkExistingIdIndex(frameId[0]);
                } else {
                    if (frameId[0].equals(gbcFrame.getFrameId())) {
                        validId[0] = true;//If it's the same frame Id as the one it already has, it's a valid id
                    } else {
                        validId[0] = checkExistingIdIndex(frameId[0]);
                    }
                }
                if (!validId[0]) {
                    etFrameIndex.setError(context.getString(R.string.et_frame_id_error));
                }
            }
        });

        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (index[0] < MAX_INDEX) {
                    index[0]++;
                    etFrameIndex.setText(String.valueOf(index[0]));
                    frameId[0] = generateFrameId(autoCompNewId.getText().toString().trim(), index[0]);
                    if (!editingFrame) {
                        validId[0] = checkExistingIdIndex(frameId[0]);
                    } else {
                        if (frameId[0].equals(gbcFrame.getFrameId())) {
                            validId[0] = true;//If it's the same frame Id as the one it already has, it's a valid id
                        } else {
                            validId[0] = checkExistingIdIndex(frameId[0]);
                        }
                    }
                    if (!validId[0]) {
                        etFrameIndex.setError(context.getString(R.string.et_frame_id_error));
                    } else {
                        etFrameIndex.setError(null);
                    }
                }
            }
        });

        Bitmap bitmapCopy = frameBitmap.copy(frameBitmap.getConfig(), true);
        Bitmap bitmap = transparentBitmap(bitmapCopy, gbcFrame);
        gbcFrame.setFrameBitmap(bitmap);

        autoCompNewId.dismissDropDown();//Disabled at the beginning

        ArrayAdapter<String> adapterAutoComplete = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, frameGroupsIdsList);

        autoCompNewId.setAdapter(adapterAutoComplete);
        autoCompNewId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Activity activity = (Activity) context;
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(autoCompNewId.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        final String[] newFrameGroupPlaceholder = {""};
        etFrameGroupName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    newFrameGroupPlaceholder[0] = etFrameGroupName.getText().toString().trim();
                }
            }
        });
        etFrameGroupName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                newFrameGroupPlaceholder[0] = etFrameGroupName.getText().toString().trim();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

        });

        autoCompNewId.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String placeHolder = autoCompNewId.getText().toString().trim();
                if (!placeHolder.isEmpty()) {
                    if (!placeHolder.matches("^[a-z]{2,}$")) {
                        autoCompNewId.setError(context.getString(R.string.et_frame_group_id_error));
                        validId[0] = false;
                    } else {
                        frameGroupId[0] = placeHolder;
                        autoCompNewId.setError(null);
                        if (frameGroupsIdsList.contains(placeHolder)) {
                            String groupName = frameGroupsNames.get(autoCompNewId.getText().toString().trim());
                            etFrameGroupName.setText(groupName);
                            etFrameGroupName.setEnabled(false);//An existing frame group
                            spFrameGroupDialog.setSelection(frameGroupIds.indexOf(frameGroupId[0])+1);

                        } else {
                            etFrameGroupName.setText(newFrameGroupPlaceholder[0]);
                            etFrameGroupName.setEnabled(true);
                            spFrameGroupDialog.setSelection(0);
                        }
                        frameId[0] = generateFrameId(autoCompNewId.getText().toString().trim(), index[0]);
                        if (!editingFrame) {
                            validId[0] = checkExistingIdIndex(frameId[0]);
                        } else {
                            if (frameId[0].equals(gbcFrame.getFrameId())) {
                                validId[0] = true;//If it's the same frame Id as the one it already has, it's a valid id
                            } else {
                                validId[0] = checkExistingIdIndex(frameId[0]);
                            }
                        }
                        if (!validId[0]) {
                            etFrameIndex.setError(context.getString(R.string.et_frame_id_error));
                        } else {
                            etFrameIndex.setError(null);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        autoCompNewId.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    autoCompNewId.showDropDown();
                } else {
                    autoCompNewId.dismissDropDown();
                }
            }
        });

        spFrameGroupDialog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    etFrameGroupName.setEnabled(true);
                    autoCompNewId.setEnabled(true);
                    etFrameGroupName.setText(newFrameGroupPlaceholder[0]);
                } else {
                    autoCompNewId.setText(frameGroupsIdsList.get(position-1));
                    autoCompNewId.setSelection(autoCompNewId.getText().length());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        try {
            byte[] gbFrameBytes = Utils.encodeImage(bitmap, "bw");
            gbcFrame.setFrameBytes(gbFrameBytes);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            String gbFrameHash = generateHashFromBytes(byteArray);//Getting the hash from the transparent bitmap, and not the encoded bitmap because it can be all white when encoding, causing false duplicated hashes
            gbcFrame.setFrameHash(gbFrameHash);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ivFrame.setImageBitmap(bitmap);

        etFrameName.requestFocus();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        AlertDialog alertdialog = builder.create();

        etFrameName.setImeOptions(EditorInfo.IME_ACTION_DONE);//When pressing enter

        etFrameName.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                frameName[0] = etFrameName.getText().toString().trim();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        Button btnSaveFrame = view.findViewById(R.id.btnSaveFrame);
        if (editingFrame) {
            btnSaveFrame.setText(R.string.btn_edit_frame);
        }
        btnSaveFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editingFrame) {
                    if (frameBitmap.getHeight() == 224) {
                        gbcFrame.setWildFrame(true);
                    }
                    try {
                        gbcFrame.setFrameBytes(Utils.encodeImage(gbcFrame.getFrameBitmap(), "bw"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<GbcFrame> newFrameImages = new ArrayList<>();
                    //Add here the dialog for the frame name
                    if (frameGroupId[0].equals("")) {
                        autoCompNewId.setError(context.getString(R.string.no_empty_frame_name));
                    } else {
                        gbcFrame.setFrameId(frameId[0]);
                        gbcFrame.setFrameName(frameName[0]);
                        boolean alreadyAdded = false;
                        //If the frame already exists (by the id) it doesn't add it. Same if it's already added
                        if (!validId[0]) {
                            alreadyAdded = true;
                        }
                        if (!alreadyAdded) {
                            newFrameImages.add(gbcFrame);
                        }
                        if (newFrameImages.size() > 0) {
                            Utils.framesList.addAll(newFrameImages);
                            for (GbcFrame frame : newFrameImages) {
                                Utils.hashFrames.put(frame.getFrameId(), frame);
                            }
                            if (!importedFrameGroupIdNames.containsKey(frameGroupId[0])) {
                                importedFrameGroupIdNames.put(frameGroupId[0], newFrameGroupPlaceholder[0]);
                            }

                            new ImportFragment.SaveFrameAsyncTask(newFrameImages, context).execute();
                            alertdialog.dismiss();
                        }
                    }
                } else {
                    //Editing the frame
                    if (validId[0]) {
                        if (!frameGroupsNames.containsKey(frameGroupId[0])) {//This means it's a new group created
                            frameGroupsNames.put(frameGroupId[0], newFrameGroupPlaceholder[0]);

                            GbcFrame frameWithFrameGroupNames = hashFrames.get("gbcam01");
                            //If it was the last frame from the group and was edited to be in a new group, remove old group and also update gbcam01
                            int numberOfFramesInId = 0;

                            for (GbcFrame allGbcFrames : framesList) {
                                String frameName = allGbcFrames.getFrameId().replaceAll("^(\\D+).*", "$1");
                                if (frameName.equals(oldFrameGroupId)) {
                                    numberOfFramesInId++;
                                }
                            }
                            //If it's the last frame from the group, delete the group too and update the frame holding the group names
                            if (numberOfFramesInId < 2) {
                                putSpToCero = true;
                                for (Iterator<String> iterator = frameGroupsNamesList.iterator(); iterator.hasNext(); ) {
                                    String currentGroup = iterator.next();
                                    String deletedGroupName = frameGroupsNames.get(oldFrameGroupId);
                                    if (currentGroup.equals(deletedGroupName)) {
                                        iterator.remove();
                                        break;
                                    }
                                }
                                frameGroupsNames.remove(oldFrameGroupId);
                            }

                            frameWithFrameGroupNames.setFrameGroupsNames(frameGroupsNames);
                            new UpdateFrameAsyncTask(frameWithFrameGroupNames).execute();
                        }

                        //Change also in this hashFrames there new frameId of the frame if it was changed
                        if (!oldFrameId.equals(frameId[0])) {
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    FrameDao frameDao = db.frameDao();
                                    frameDao.updateFrameWithPrimaryKeyMod(gbcFrame, frameId[0], frameName[0]);
                                }
                            });
                            thread.start();
//                            try {
//                                thread.join(); // Wait for the dao to finish
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                            for (LinkedHashMap.Entry<String, GbcFrame> entry : hashFrames.entrySet()) {
                                if (entry.getValue() == gbcFrame) {
                                    String clave = entry.getKey();
                                    hashFrames.remove(clave);
                                    hashFrames.put(frameId[0], gbcFrame);
                                    break;
                                }
                            }

                            //Change the frameId of the images that were using that frame to keep using the same one
                            for (GbcImage gbcImage : gbcImagesList) {
                                String gbcImageFrameId = gbcImage.getFrameId();
                                if (gbcImageFrameId != null && gbcImageFrameId.equals(oldFrameId)) {
                                    gbcImage.setFrameId(frameId[0]);
                                }
                                new UpdateImageAsyncTask(gbcImage).execute();
                            }
                        } else {//Not changing frame id
                            gbcFrame.setFrameName(frameName[0]);
                            new UpdateFrameAsyncTask(gbcFrame).execute();
                        }

                        //Sort the frame list by id and group
                        frameGroupSorting();

                        if (putSpToCero) {
                            spFrameGroups.setSelection(0);//Select all frames list
                        }

                        customGridViewAdapterFrames[0].notifyDataSetChanged();
                        reloadFrameGroupsSpinner(context);
                        adapter.notifyDataSetChanged();
                        alertdialog.dismiss();
                    }
                }
            }
        });

        alertdialog.show();
    }

    private String generateFrameId(String groupId, int frameIndex) {
        String frameId = new String(groupId);
        if (frameIndex < 10) {
            frameId += "0" + frameIndex;
        } else {
            frameId += String.valueOf(frameIndex);
        }
        return frameId;
    }

    private boolean checkExistingIdIndex(String frameId) {
        for (HashMap.Entry<String, GbcFrame> entry : hashFrames.entrySet()) {
            if (entry.getValue().getFrameId().equals(frameId)) {
                return false;
            }
        }
        return true;
    }
}

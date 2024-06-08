package com.mraulio.gbcameramanager.ui.extraGallery;

import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.averageImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.exportSize;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.utils.Utils.IMAGES_FOLDER;
import static com.mraulio.gbcameramanager.utils.Utils.imageBitmapCache;
import static com.mraulio.gbcameramanager.utils.Utils.rotateBitmap;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.mraulio.gbcameramanager.R;

import com.mraulio.gbcameramanager.ui.gallery.RgbUtils;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ExtraGalleryFragment extends Fragment implements RgbUtils.OnDialogDismissListener{
    public static ExtraGalleryFragment egf;
    List<File> fileList;
    private RecyclerView recyclerView;
    Switch swHdr, swRgb, swCollage;
    public static boolean selectionModeExtra = false;
    public static HashSet<Integer> selectedFilesIndex = new HashSet<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        egf = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StaticValues.currentFragment = StaticValues.CURRENT_FRAGMENT.EXTRA_GALLERY;
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_extra_gallery, container, false);

        swHdr = view.findViewById(R.id.sw_hdr);
        swRgb = view.findViewById(R.id.sw_rgb);
        swCollage = view.findViewById(R.id.sw_collage);

        fileList = loadFilesFromDirectory(IMAGES_FOLDER);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(getContext(), recyclerView, new RecyclerViewItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (selectionModeExtra) {

                    if (selectedFilesIndex.contains(position)) {
                        selectedFilesIndex.remove(position);
                    } else {
                        selectedFilesIndex.add(position);
                    }
                    if (selectedFilesIndex.size() == 0) {
                        hideSelectionOptionsExtra(getActivity());
                    }
                    loadAndDisplayImages();

                } else {
                    if (position != RecyclerView.NO_POSITION) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        View dialogView = inflater.inflate(R.layout.dialog_extra, null);
                        builder.setView(dialogView);

                        ImageView imageView = dialogView.findViewById(R.id.imageView);
                        imageView.setImageBitmap(getBitmapFromFile(fileList.get(position)));

                        Button btnClose = dialogView.findViewById(R.id.btn_close_extra);
                        Button btnShare = dialogView.findViewById(R.id.btn_share_extra);
                        btnShare.setVisibility(View.VISIBLE);

                        AlertDialog dialog = builder.create();

                        btnClose.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        btnShare.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ArrayList<Uri> imageUris = new ArrayList<>();

                                File file = fileList.get(position);
                                Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
                                imageUris.add(uri);
                                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                intent.setType("image/png");
                                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                getContext().startActivity(Intent.createChooser(intent, "Share"));
                            }
                        });

                        dialog.show();
                    }
                }

            }
        }, new RecyclerViewItemClickListener.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int position) {

                if (!selectionModeExtra) {
                    selectionModeExtra = true;
                    StaticValues.fab.show();
                    if (selectedFilesIndex.contains(position)) {
                        selectedFilesIndex.remove(position);
                    } else {
                        selectedFilesIndex.add(position);
                    }
                } else {
                    int firstImage = Collections.min(selectedFilesIndex);
                    int lastImage =  Collections.max(selectedFilesIndex);

                    selectedFilesIndex.clear();
                    selectedFilesIndex.add(position);

                    if (firstImage < position) {
                        selectedFilesIndex.clear();
                        for (int i = firstImage; i < position; i++) {
                            if (!selectedFilesIndex.contains(i)) {
                                selectedFilesIndex.add(i);
                            }
                        }
                        selectedFilesIndex.add(position);
                    } else if (firstImage > position) {
                        for (int i = lastImage; i > position; i--) {
                            if (!selectedFilesIndex.contains(i)) {
                                selectedFilesIndex.add(i);
                            }
                        }
                    }
                }

                loadAndDisplayImages();

                if (selectedFilesIndex.size() == 0) {
                    hideSelectionOptionsExtra(getActivity());
                } else {
                    getActivity().invalidateOptionsMenu();
                }

                return true;
            }
        }));

        loadAndDisplayImages();

        setupSwitchListeners();

        return view;
    }

    public void hideSelectionOptionsExtra(Activity activity) {
        showEditMenuButton = false;
        selectedFilesIndex.clear();
        selectionModeExtra = false;
        loadAndDisplayImages();
        StaticValues.fab.hide();
        activity.invalidateOptionsMenu();
    }

    private void loadAndDisplayImages() {
        fileList = loadFilesFromDirectory(IMAGES_FOLDER);

        ImageAdapter imageAdapter = new ImageAdapter(fileList, selectedFilesIndex, selectionModeExtra);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupSwitchListeners() {
        swHdr.setOnClickListener(v ->
                hideSelectionOptionsExtra(getActivity())
        );

        swRgb.setOnClickListener(v ->
                hideSelectionOptionsExtra(getActivity())
        );

        swCollage.setOnClickListener(v -> hideSelectionOptionsExtra(getActivity())
        );
    }

    private List<File> loadFilesFromDirectory(File directory) {
        List<File> fileList = new ArrayList<>();

        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".png")) {
                    boolean addFile = false;
                    if (swHdr.isChecked() && file.getName().startsWith("HDR")) {
                        addFile = true;
                    } else if (swRgb.isChecked() && file.getName().startsWith("RGB_")) {
                        addFile = true;
                    } else if (swCollage.isChecked() && file.getName().startsWith("Collage_")) {
                        addFile = true;
                    }
                    if (addFile) {
                        fileList.add(file);
                    }
                }
            }
        }
        return fileList;
    }

    private Bitmap getBitmapFromFile(File file) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public void onDialogDismiss() {
        loadAndDisplayImages();
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

        private List<File> fileList;
        private HashSet<Integer> selectedFilesIndexes;
        private boolean selectionModeExtra;

        public ImageAdapter(List<File> fileList, HashSet<Integer> selectedFilesIndexes, boolean selectionModeExtra) {
            this.fileList = fileList;
            this.selectedFilesIndexes = selectedFilesIndexes;
            this.selectionModeExtra = selectionModeExtra;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.imageView);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = fileList.get(position);

            Bitmap bitmap = getBitmapFromFile(file);

            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
            }

            if (selectionModeExtra && selectedFilesIndexes != null && !selectedFilesIndexes.isEmpty()) {
                holder.imageView.setBackgroundColor(selectedFilesIndexes.contains(position) ? getContext().getColor(R.color.teal_700) : Color.TRANSPARENT);

            }
        }

        @Override
        public int getItemCount() {
            return fileList.size();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_average_extra:
                if (!selectedFilesIndex.isEmpty()) {
                    List<Bitmap> bitmapListToAverage = new ArrayList<>();
                    for (int i : selectedFilesIndex) {
                        File file = fileList.get(i);

                        Bitmap bitmap = getBitmapFromFile(file);
                        if (bitmap != null) {
                            bitmapListToAverage.add(bitmap);
                        }
                    }
                    try {
                        Bitmap averagedBitmap = averageImages(bitmapListToAverage);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        View dialogView = inflater.inflate(R.layout.dialog_extra, null);
                        builder.setView(dialogView);

                        ImageView imageView = dialogView.findViewById(R.id.imageView);
                        imageView.setImageBitmap(averagedBitmap);

                        Button btnClose = dialogView.findViewById(R.id.btn_close_extra);
                        Button btnSave = dialogView.findViewById(R.id.btn_save_extra);
                        btnSave.setVisibility(View.VISIBLE);

                        AlertDialog dialog = builder.create();

                        btnClose.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        btnSave.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                LocalDateTime now = null;
                                Date nowDate = new Date();
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    now = LocalDateTime.now();
                                }
                                File file = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateLocale + "_HH-mm-ss");

                                    file = new File(Utils.IMAGES_FOLDER, "HDR_extra_" + dtf.format(now) + ".png");
                                } else {
                                    SimpleDateFormat sdf = new SimpleDateFormat(dateLocale + "_HH-mm-ss", Locale.getDefault());
                                    file = new File(Utils.IMAGES_FOLDER, "HDR_extra_" + sdf.format(nowDate) + ".png");

                                }
                                try (FileOutputStream out = new FileOutputStream(file)) {
                                    averagedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    Toast toast = Toast.makeText(getContext(), getString(R.string.toast_saved) + " HDR!", Toast.LENGTH_LONG);
                                    toast.show();
                                    mediaScanner(file, getContext());
                                    showNotification(getContext(), file);
                                    loadAndDisplayImages();
                                    dialog.dismiss();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        dialog.show();
                    } catch (IllegalArgumentException e) {
                        Utils.toast(getContext(), getString(R.string.hdr_exception));
                    }


                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;

            case R.id.action_rgb_extra:
                if (!selectedFilesIndex.isEmpty()) {
                    if (selectedFilesIndex.size() != 3 && selectedFilesIndex.size() != 4) {
                        Utils.toast(getContext(), getString(R.string.select_rgb));
                    } else {
                        List<Bitmap> bitmapList = new ArrayList<>();

                        for (int i : selectedFilesIndex) {
                            File file = fileList.get(i);

                            Bitmap bitmap = getBitmapFromFile(file);

                            if (bitmap != null) {
                                bitmapList.add(bitmap);
                            }
                        }
                        int width = bitmapList.get(0).getWidth();
                        int height = bitmapList.get(0).getHeight();
                        boolean sameSize = true;
                        for (Bitmap bitmap : bitmapList) {
                            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                                sameSize = false;
                            }
                        }
                        if (sameSize) {
                            RgbUtils rgbUtils = new RgbUtils(getContext(), bitmapList, true);
                            rgbUtils.showRgbDialog(this);
                        } else {
                            Utils.toast(getContext(), getString(R.string.hdr_exception));
                        }
                    }
                } else
                    Utils.toast(getContext(), getString(R.string.no_selected));
                return true;
            case R.id.action_gif_extra:

                return true;
            case R.id.action_collage_extra:

                return true;
            default:
                break;
        }
        return false;

    }

}

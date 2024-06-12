package com.mraulio.gbcameramanager.ui.extraGallery;

import static android.view.View.GONE;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.averageImages;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;
import static com.mraulio.gbcameramanager.utils.StaticValues.dateLocale;
import static com.mraulio.gbcameramanager.utils.StaticValues.imagesPage;
import static com.mraulio.gbcameramanager.utils.StaticValues.showEditMenuButton;
import static com.mraulio.gbcameramanager.utils.Utils.IMAGES_FOLDER;
import static com.mraulio.gbcameramanager.utils.Utils.showNotification;
import static com.mraulio.gbcameramanager.utils.Utils.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.R;

import com.mraulio.gbcameramanager.ui.gallery.RgbUtils;
import com.mraulio.gbcameramanager.utils.StaticValues;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import pl.droidsonroids.gif.GifDrawable;

public class ExtraGalleryFragment extends Fragment implements RgbUtils.OnRgbSaved {
    public static ExtraGalleryFragment egf;
    List<File> fileList;
    private RecyclerView recyclerView;
    Switch swHdr, swRgb, swGif, swCollage;
    Button btnFirstPage, btnLastPage, btnPrevPage, btnNextPage;
    TextView tvPage;
    public static boolean showInfoExtra;

    public static boolean selectionModeExtra = false;
    public static HashSet<Integer> selectedFilesIndex = new HashSet<>();
    private int page = 0, lastPage, globalImageIndex;
    private int itemsPage = imagesPage;

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

        btnFirstPage = view.findViewById(R.id.btnFirstPage);
        btnLastPage = view.findViewById(R.id.btnLastPage);
        btnPrevPage = view.findViewById(R.id.btnPrevPage);
        btnNextPage = view.findViewById(R.id.btnNextPage);
        tvPage = view.findViewById(R.id.tv_page);

        swHdr = view.findViewById(R.id.sw_hdr);
        swRgb = view.findViewById(R.id.sw_rgb);
        swGif = view.findViewById(R.id.sw_gif);
        swCollage = view.findViewById(R.id.sw_collage);

        fileList = loadFilesFromDirectory(IMAGES_FOLDER);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(getContext(), recyclerView, new RecyclerViewItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (page != lastPage) {
                    globalImageIndex = position + (page * itemsPage);
                } else {
                    globalImageIndex = fileList.size() - (itemsPage - position);
                }
                if (selectionModeExtra) {

                    if (selectedFilesIndex.contains(globalImageIndex)) {
                        selectedFilesIndex.remove(globalImageIndex);
                    } else {
                        selectedFilesIndex.add(globalImageIndex);
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

                        if (isGif(fileList.get(globalImageIndex))) {
                            try {
                                InputStream inputStream = new FileInputStream(fileList.get(globalImageIndex));

                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, length);
                                }
                                byte[] byteArray = outputStream.toByteArray();

                                inputStream.close();
                                outputStream.close();

                                GifDrawable gifDrawable = new GifDrawable(byteArray);

                                imageView.setImageDrawable(gifDrawable);

                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            imageView.setImageBitmap(getBitmapFromFile(fileList.get(globalImageIndex)));

                        }

                        Button btnClose = dialogView.findViewById(R.id.btn_close_extra);
                        Button btnShare = dialogView.findViewById(R.id.btn_share_extra);
                        Button btnDelete = dialogView.findViewById(R.id.btn_delete_extra);
                        btnShare.setVisibility(View.VISIBLE);
                        btnDelete.setVisibility(View.VISIBLE);

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

                                File file = fileList.get(globalImageIndex);
                                if (isGif(file)) {
                                    Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);

                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("image/gif");
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    getContext().startActivity(Intent.createChooser(intent, "Share"));

                                } else {
                                    Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
                                    imageUris.add(uri);
                                    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                    intent.setType("image/png");
                                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    getContext().startActivity(Intent.createChooser(intent, "Share"));
                                }
                            }
                        });

                        btnDelete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                File file = fileList.get(globalImageIndex);
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle(getString(R.string.sure_delete_sav) + " " + file.getName())
                                        .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface deleteDialog, int id) {

                                                if (file.delete()) {
                                                    toast(getContext(), getString(R.string.deleted_sav) + file.getName());
                                                } else {
                                                    toast(getContext(), getString(R.string.toast_couldnt_delete_sav));
                                                }

                                                deleteDialog.dismiss();
                                                dialog.dismiss();
                                                hideSelectionOptionsExtra(getActivity());
                                            }
                                        })

                                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface deleteDialog, int id) {
                                                deleteDialog.dismiss();
                                            }
                                        });

                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();

                            }
                        });

                        dialog.show();
                    }
                }

            }
        }, new RecyclerViewItemClickListener.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int position) {
                if (page != lastPage) {
                    globalImageIndex = position + (page * itemsPage);
                } else {
                    globalImageIndex = fileList.size() - (itemsPage - position);
                }
                if (!selectionModeExtra) {
                    selectionModeExtra = true;
                    StaticValues.fab.show();

                    if (selectedFilesIndex.contains(globalImageIndex)) {
                        selectedFilesIndex.remove(globalImageIndex);
                    } else {
                        selectedFilesIndex.add(globalImageIndex);
                    }
                } else {
                    int firstImage = Collections.min(selectedFilesIndex);
                    int lastImage = Collections.max(selectedFilesIndex);

                    selectedFilesIndex.clear();
                    selectedFilesIndex.add(globalImageIndex);

                    if (firstImage < globalImageIndex) {
                        selectedFilesIndex.clear();
                        for (int i = firstImage; i < globalImageIndex; i++) {
                            if (!selectedFilesIndex.contains(i)) {
                                selectedFilesIndex.add(i);
                            }
                        }
                        selectedFilesIndex.add(globalImageIndex);
                    } else if (firstImage > globalImageIndex) {
                        for (int i = lastImage; i > globalImageIndex; i--) {
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

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPage();
            }
        });
        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevPage();
            }
        });

        btnFirstPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page > 0) {
                    page = 0;
                    loadAndDisplayImages();
                }
            }
        });

        btnLastPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page < lastPage) {
                    page = lastPage;
                    loadAndDisplayImages();
                }
            }
        });
        return view;
    }

    public void prevPage() {
        if (page > 0) {
            page--;
            loadAndDisplayImages();
        }
    }

    public void nextPage() {
        if (page < lastPage) {
            page++;
            loadAndDisplayImages();
        }
    }

    public void hideSelectionOptionsExtra(Activity activity) {
        showEditMenuButton = false;
        selectedFilesIndex.clear();
        selectionModeExtra = false;
        loadAndDisplayImages();
        StaticValues.fab.hide();
        activity.invalidateOptionsMenu();
    }

    public void loadAndDisplayImages() {
        fileList = loadFilesFromDirectory(IMAGES_FOLDER);
        itemsPage = StaticValues.imagesPage;
        lastPage = (fileList.size() - 1) / itemsPage;
        tvPage.setText((page + 1) + " / " + (lastPage + 1));
        int startIndex, endIndex;

        //In case the last page is not complete
        if (page == lastPage && (fileList.size() % itemsPage) != 0) {
            itemsPage = fileList.size() % itemsPage;
            startIndex = fileList.size() - itemsPage;
            endIndex = fileList.size();

        } else {
            startIndex = page * itemsPage;
            endIndex = Math.min(startIndex + itemsPage, fileList.size());
        }
        List<File> fileListPage = fileList.subList(startIndex, endIndex);

        ImageAdapter imageAdapter = new ImageAdapter(fileListPage, fileList, selectedFilesIndex, selectionModeExtra);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupSwitchListeners() {
        swHdr.setOnClickListener(v -> {
            page = 0;
            hideSelectionOptionsExtra(getActivity());
        });
        swRgb.setOnClickListener(v -> {
            page = 0;
            hideSelectionOptionsExtra(getActivity());
        });
        swGif.setOnClickListener(v -> {
            page = 0;
            hideSelectionOptionsExtra(getActivity());
        });
        swCollage.setOnClickListener(v -> {
            page = 0;
            hideSelectionOptionsExtra(getActivity());
        });
    }

    private List<File> loadFilesFromDirectory(File directory) {
        List<File> fileList = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && (file.getName().toLowerCase().endsWith(".png") || file.getName().toLowerCase().endsWith(".gif"))) {
                    boolean addFile = false;
                    if (swHdr.isChecked() && file.getName().startsWith("HDR")) {
                        addFile = true;
                    } else if (swRgb.isChecked() && file.getName().startsWith("RGB_")) {
                        addFile = true;
                    } else if (swCollage.isChecked() && file.getName().startsWith("Collage_")) {
                        addFile = true;
                    } else if (swGif.isChecked() && file.getName().startsWith("GIF_")) {
                        addFile = true;
                    }
                    if (addFile) {
                        fileList.add(file);
                    }
                }
            }

            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });
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
    public void onButtonRgbSaved() {
        hideSelectionOptionsExtra(getActivity());
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

        private List<File> fileListPage;
        private List<File> fileListTotal;
        private HashSet<Integer> selectedFilesIndexes;
        private boolean selectionModeExtra;

        public ImageAdapter(List<File> fileListPage, List<File> fileListTotal, HashSet<Integer> selectedFilesIndexes, boolean selectionModeExtra) {
            this.fileListPage = fileListPage;
            this.fileListTotal = fileListTotal;
            this.selectedFilesIndexes = selectedFilesIndexes;
            this.selectionModeExtra = selectionModeExtra;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView tvImageType, tvImageSize;

            public ViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.imageView);
                tvImageType = view.findViewById(R.id.tv_image_type);
                tvImageSize = view.findViewById(R.id.tv_image_size);
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
            View itemView = holder.itemView;
            File file = fileListPage.get(position);
            int width = 0;
            int height = 0;
            if (isGif(file)) {
                try {
                    InputStream inputStream = new FileInputStream(file);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                    byte[] byteArray = outputStream.toByteArray();

                    inputStream.close();
                    outputStream.close();

                    GifDrawable gifDrawable = new GifDrawable(byteArray);
                    if (showInfoExtra) {
                        height = gifDrawable.getIntrinsicHeight();
                        width = gifDrawable.getIntrinsicWidth();
                    }
                    holder.imageView.setImageDrawable(gifDrawable);

                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Bitmap bitmap = getBitmapFromFile(file);
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    if (showInfoExtra) {
                        height = bitmap.getHeight();
                        width = bitmap.getWidth();
                    }
                }
            }

            boolean shouldCheck = false;
            if (selectionModeExtra && selectedFilesIndexes != null && !selectedFilesIndexes.isEmpty()) {
                int actualIndex;
                if (page != lastPage) {
                    actualIndex = position + (page * itemsPage);
                } else {
                    actualIndex = fileListTotal.size() - (itemsPage - position);
                }
                if (selectedFilesIndexes.contains(actualIndex)) {
                    shouldCheck = true;
                }
                if (!showInfoExtra) {
                    itemView.setBackgroundColor(shouldCheck ? getContext().getColor(R.color.teal_700) : Color.TRANSPARENT);
                }
            }

            if (showInfoExtra) {
                GradientDrawable drawable = (GradientDrawable) getResources().getDrawable(R.drawable.border_layout);
                drawable.setColor(shouldCheck ? getResources().getColor(R.color.teal_700) : Color.TRANSPARENT);
                itemView.setBackground(drawable);
                String type = "";
                if (file.getName().startsWith("HDR")) {
                    type = "HDR";
                } else if (file.getName().startsWith("GIF")) {
                    type = "GIF";
                } else if (file.getName().startsWith("RGB")) {
                    type = "RGB";
                } else if (file.getName().startsWith("Collage")) {
                    type = "Collage";
                }

                holder.tvImageType.setText(type);
                holder.tvImageSize.setText(width + "x" + height);
            } else {
                holder.tvImageType.setVisibility(GONE);
                holder.tvImageSize.setVisibility(GONE);
            }
        }

        @Override
        public int getItemCount() {
            return fileListPage.size();
        }

    }

    private boolean isGif(File file) {
        return file.getName().toLowerCase().endsWith(".gif");
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
                                    hideSelectionOptionsExtra(getActivity());
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
//            case R.id.action_gif_extra:
//                toast(getContext(), "Nothing yet");
//                return true;
//            case R.id.action_collage_extra:
//                toast(getContext(), "Nothing yet");
//                return true;
            case R.id.action_toggle_info:
                if (showInfoExtra) {
                    showInfoExtra = false;
                } else {
                    showInfoExtra = true;
                }
                loadAndDisplayImages();
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                break;
        }
        return false;

    }

}

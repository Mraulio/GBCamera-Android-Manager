package com.mraulio.gbcameramanager.ui.gallery;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.ui.palettes.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.ui.frames.FramesFragment;
import com.mraulio.gbcameramanager.ui.usbserial.UsbSerialFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryFragment extends Fragment {

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss_dd-MM-yyyy");

    public static GridView gridView;
    private static int itemsPerPage = MainActivity.imagesPage;
    static int startIndex = 0;
    static int endIndex = 0;
    public static int currentPage = 0;
    static int lastPage = 0;
    boolean crop = false;
    boolean showPalettes = true;
    static TextView tv_page;
    boolean keepFrame = false;
    //    Resources.Theme theme = getActivity().getTheme();//I get the theme
    public static CustomGridViewAdapterImage customGridViewAdapterImage;
    static List<Bitmap> imagesForPage;
    static List<GbcImage> gbcImagesForPage;
    public static TextView tv;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);

        customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, gbcImagesForPage, imagesForPage, false);
        Button btnPrevPage = (Button) view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = (Button) view.findViewById(R.id.btnNextPage);
        tv_page = (TextView) view.findViewById(R.id.tv_page);
        view.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                nextPage();
            }

            @Override
            public void onSwipeRight() {
                prevPage();
            }
        });

        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevPage();
            }
        });

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage();
            }
        });

        //To swipe over the gridview. Not working properly, selects the first image of the row
//        gridView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
//            @Override
//            public void onSwipeLeft() {
//                nextPage();
//            }
//            @Override
//            public void onSwipeRight() {
//                prevPage();
//            }
//        });
        /**
         * Dialog when clicking an image
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedPosition = 0;
                crop = false;
                keepFrame = false;
                // Obtener la imagen seleccionada
                if (currentPage != lastPage) {
                    selectedPosition = position + (currentPage * itemsPerPage);
                } else {
                    selectedPosition = Methods.completeBitmapList.size() - (itemsPerPage - position);
                }
                final Bitmap[] selectedImage = {Methods.completeBitmapList.get(selectedPosition)};
                byte[] selectedImageBytes = Methods.gbcImagesList.get(selectedPosition).getImageBytes();
                // Crear el diálogo personalizado
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);
                dialog.setCancelable(true);//So it closes when clicking outside or back button

                // Configurar la vista de imagen del diálogo
                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));
                int maxHeight = displayMetrics.heightPixels / 2;//To set the imageview max height as the 50% of the screen, for large images
                imageView.setMaxHeight(maxHeight);


                // Configurar el botón de cierre del diálogo
                Button printButton = dialog.findViewById(R.id.print_button);
                Button shareButton = dialog.findViewById(R.id.share_button);

                Button saveButton = dialog.findViewById(R.id.save_button);
//                Button cropButton = dialog.findViewById(R.id.crop_save_button);
//                Button paletteButton = dialog.findViewById(R.id.btn_palette);
                Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                CheckBox cbCrop = dialog.findViewById(R.id.cbCrop);

                showPalettes = true;

                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = Methods.completeBitmapList.size() - (itemsPerPage - position);
                }
                if (Methods.gbcImagesList.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                    imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                }

                imageView.setOnClickListener(new View.OnClickListener() {
                    private int clickCount = 0;
                    private final Handler handler = new Handler();
                    private final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            // Acción a realizar cuando se detecta un clic simple
                            // después de que expire el temporizador
//                            palette = Methods.gbcPalettesList.get(clickedPosition).getPaletteColorsInt().clone();//Clone so it doesn't overwrite base palette colors.
//                            newPaletteName = Methods.gbcPalettesList.get(clickedPosition).getName();
//                            paletteDialog(palette, newPaletteName);
                            Methods.toast(getContext(), "Single tap" + globalImageIndex);
                        }
                    };

                    @Override
                    public void onClick(View v) {
                        clickCount++;
                        if (clickCount == 1) {
                            // Iniciar el temporizador para detectar el doble toque
                            handler.postDelayed(runnable, 300);
                        } else if (clickCount == 2) {
                            // Detener el temporizador y realizar la acción para el doble toque
                            handler.removeCallbacks(runnable);
                            // Acción a realizar cuando se detecta un doble toque
                            Methods.gbcImagesList.get(globalImageIndex).addTag("__filter:favourite__");
                            Methods.toast(getContext(), "Set as favorite" + globalImageIndex);
                            clickCount = 0;
                            System.out.println(Methods.gbcImagesList.get(globalImageIndex).getTags());
                            imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                            updateGridView(currentPage, gridView);
                        }
                    }

                });

                paletteFrameSelButton.setText("Show frames.");
                FramesFragment.CustomGridViewAdapterFrames frameAdapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Methods.framesList, false, false);
                frameAdapter.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getFrameIndex());
                gridViewFrames.setAdapter(frameAdapter);

                //If Image is not 144 pixels high (regular camera image), like panoramas, I remove the frames selector
                if (selectedImage[0].getHeight() != 144) {
                    cbFrameKeep.setVisibility(View.GONE);
                    paletteFrameSelButton.setVisibility(View.GONE);
                }

                cbFrameKeep.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (keepFrame) keepFrame = false;
                        else keepFrame = true;
                    }
                });
                cbCrop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!crop) {
                            crop = true;
                        } else {
                            crop = false;
                        }
                    }
                });
                gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                        //Action when clicking a frame inside the Dialog
                        Bitmap framed = null;
                        try {
                            framed = frameChange(globalImageIndex, selectedFrameIndex, keepFrame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(framed, framed.getWidth() * 6, framed.getHeight() * 6, false));
//                        Methods.gbcImagesList.get(globalImageIndex).setImageBytes(imageBytes);
                        selectedImage[0] = framed;
                        Methods.completeBitmapList.set(globalImageIndex, framed);
                        Methods.gbcImagesList.get(globalImageIndex).setFrameIndex(selectedFrameIndex);
                        frameAdapter.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getFrameIndex());
                        frameAdapter.notifyDataSetChanged();
                        updateGridView(currentPage, gridView);
                    }
                });
                CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList, false, false);

                adapterPalette.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex());
                gridViewPalette.setAdapter(adapterPalette);
                gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position2, long id) {
                        //Action when clicking a palette inside the Dialog
                        Bitmap changedImage;
                        if (!keepFrame) {
                            Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(0);//Need to set this to the palette 0 to then change it with the frame
                            changedImage = paletteChanger(position2, Methods.gbcImagesList.get(globalImageIndex).getImageBytes(), Methods.gbcImagesList.get(globalImageIndex));

                        } else {
                            changedImage = paletteChanger(position2, Methods.gbcImagesList.get(globalImageIndex).getImageBytes(), Methods.gbcImagesList.get(globalImageIndex));
                        }
                        Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(position2);

                        Methods.completeBitmapList.set(globalImageIndex, changedImage);
                        if (keepFrame) {
                            try {
                                changedImage = frameChange(globalImageIndex, Methods.gbcImagesList.get(globalImageIndex).getFrameIndex(), keepFrame);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(position2);
                        adapterPalette.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex());
                        adapterPalette.notifyDataSetChanged();
                        selectedImage[0] = changedImage;//Needed to save the image with the palette changed without leaving the Dialog
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(changedImage, changedImage.getWidth() * 6, changedImage.getHeight() * 6, false));
                        updateGridView(currentPage, gridView);
                    }
                });
                paletteFrameSelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (showPalettes) {
                            showPalettes = false;
                            paletteFrameSelButton.setText("Show palettes");
                            gridViewPalette.setVisibility(View.GONE);
                            gridViewFrames.setVisibility(View.VISIBLE);

                        } else {
                            showPalettes = true;
                            paletteFrameSelButton.setText("Show frames");
                            gridViewFrames.setVisibility(View.GONE);
                            gridViewPalette.setVisibility(View.VISIBLE);
                        }
                    }
                });
//                UsbSerialFragment.rbPrint.callOnClick();//Clicking on this on startup to set the printing mode on gallery without entering the other fragment.
                printButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            MainActivity.printIndex = globalImageIndex;
                            UsbSerialFragment.btnPrintImage.callOnClick();//This works.
                            Toast toast = Toast.makeText(getContext(), "Printing, please wait...", Toast.LENGTH_LONG);
                            toast.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap sharedBitmap = Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * MainActivity.exportSize, selectedImage[0].getHeight() * MainActivity.exportSize, false);
                        shareImage(sharedBitmap);
                    }
                });
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        crop = false;
                        LocalDateTime now = LocalDateTime.now();
                        String fileName = "image_";
                        fileName += dtf.format(now) + ".png";
                        saveImage(selectedImage[0], fileName);
                    }
                });
//                cropButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
////                        crop = true;
//                        LocalDateTime now = LocalDateTime.now();
//                        String fileName = "image_";
//                        fileName += dtf.format(now) + ".png";
//                        saveImage(selectedImage[0], fileName);
//                    }
//                });


// Configurar el diálogo para que ocupe el 80% de  la pantalla

                int screenWidth = displayMetrics.widthPixels;
                int desiredWidth = (int) (screenWidth * 0.8);
                Window window = dialog.getWindow();
                window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

                //To only dismiss it instead of cancelling when clicking outside it
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // Acción al presionar fuera del diálogo o el botón de retroceso
                        dialog.dismiss();
                    }
                });
                //Show Dialog
                dialog.show();
            }
        });
        if (Methods.gbcImagesList.size() > 0 && MainActivity.doneLoading) {//This because if not updateGridView will use sublists on the same list that the MainAcvitity is creating
            updateGridView(currentPage, gridView);
            tv.setText("Total of images: " + GbcImage.numImages);

        } else {
            tv.setText("Loading...");
        }
        tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));

        return view;
    }

    public static Bitmap frameChange(int globalImageIndex, int selectedFrameIndex, boolean keepFrame) throws IOException {
        // Obtener la imagen seleccionada
        Bitmap framed = null;
        Bitmap framedAux;
        if (Methods.completeBitmapList.get(globalImageIndex).getHeight() == 144 && Methods.completeBitmapList.get(globalImageIndex).getWidth() == 160) {
            //I need to use copy because if not it's inmutable bitmap
            framed = Methods.framesList.get(selectedFrameIndex).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
            framedAux = framed.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvasAux = new Canvas(framedAux);
            Bitmap setToPalette = paletteChanger(0, Methods.gbcImagesList.get(globalImageIndex).getImageBytes(), Methods.gbcImagesList.get(globalImageIndex));
            Bitmap croppedBitmapAux = Bitmap.createBitmap(setToPalette, 16, 16, 128, 112);//Need to put this to palette 0
            canvasAux.drawBitmap(croppedBitmapAux, 16, 16, null);
            if (!keepFrame) {
                framed = paletteChanger(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex(), Methods.encodeImage(framed), Methods.gbcImagesList.get(globalImageIndex));
                framed = framed.copy(Bitmap.Config.ARGB_8888, true);//To make it mutable
            }
            Canvas canvas = new Canvas(framed);
            Bitmap croppedBitmap = Bitmap.createBitmap(Methods.completeBitmapList.get(globalImageIndex), 16, 16, 128, 112);
            canvas.drawBitmap(croppedBitmap, 16, 16, null);
            Methods.completeBitmapList.set(globalImageIndex, framed);
            try {
                Methods.gbcImagesList.get(globalImageIndex).setImageBytes(Methods.encodeImage(framedAux));//Use the framedAux because it doesn't a different palette to encode
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return framed;
    }

    //Cambiar paleta
    public static Bitmap paletteChanger(int index, byte[] imageBytes, GbcImage gbcImage) {
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 160, imageBytes.length / 40);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(index).getPaletteColorsInt(), imageBytes);
        //If the image is 128x112 (extracted from sav) I apply the frame
        if ((imageBytes.length / 40) == 112) {
            ImageCodec imageCodec2 = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 160, 144);
            //I need to use copy because if not it's inmutable bitmap
            Bitmap framed = Methods.framesList.get(1).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(framed);
            canvas.drawBitmap(image, 16, 16, null);
            image = framed;
        }
        return image;
    }

    //This one changes pixel by pixel of the bitmap, but works better with the frames
    public Bitmap paletteChanger2(int newPaletteIndex, Bitmap bitmap, int imageIndex) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int[] oldColors = Methods.gbcPalettesList.get(Methods.gbcImagesList.get(imageIndex).getPaletteIndex()).getPaletteColorsInt();
        int[] newColors = Methods.gbcPalettesList.get(newPaletteIndex).getPaletteColorsInt();

        Map<Integer, Integer> colorIndexMap = new HashMap<>();
        for (int i = 0; i < oldColors.length; i++) {
            colorIndexMap.put(oldColors[i], i);
        }

        for (int i = 0; i < pixels.length; i++) {
            int pixelColor = pixels[i];
            if (colorIndexMap.containsKey(pixelColor)) {
                int oldIndex = colorIndexMap.get(pixelColor);
                pixels[i] = newColors[oldIndex];
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(pixels, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        return newBitmap;
    }

    //Previous page
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGridView(currentPage, gridView);
            tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
        }
    }

    private void nextPage() {
        if (currentPage < lastPage) {
            currentPage++;
            updateGridView(currentPage, gridView);
            tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
        }
    }

    private void shareImage(Bitmap bitmap) {
        if ((bitmap.getHeight() / MainActivity.exportSize) == 144 && (bitmap.getWidth() / MainActivity.exportSize) == 160 && crop) {
            bitmap = Bitmap.createBitmap(bitmap, 16 * MainActivity.exportSize, 16 * MainActivity.exportSize, 128 * MainActivity.exportSize, 112 * MainActivity.exportSize);
        }
        String bitmapPath = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "image", "share image");
        Uri bitmapUri = Uri.parse(bitmapPath);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
        startActivity(Intent.createChooser(intent, "Share"));
    }


    private void saveImage(Bitmap image, String fileName) {
        if (image.getHeight() == 144 && image.getWidth() == 160 && crop) {
            image = Bitmap.createBitmap(image, 16, 16, 128, 112);
        }
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(directory, fileName);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * MainActivity.exportSize, image.getHeight() * MainActivity.exportSize, false);

            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast toast = Toast.makeText(getContext(), "SAVED x" + MainActivity.exportSize, Toast.LENGTH_LONG);
            toast.show();
            // PNG is a lossless format, the compression factor (100) is ignored

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateFromMain() {
        if (Methods.gbcImagesList.size() > 0) {
            updateGridView(currentPage, gridView);
            tv.setText("Total of images: " + GbcImage.numImages);

        } else {
            tv.setText("No images in the gallery. Go to Import tab.");
        }
        tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
        System.out.println("Called updateFromMain");
        updateGridView(currentPage, gridView);
    }

    public static void updateGridView(int page, GridView gridView) {
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        itemsPerPage = MainActivity.imagesPage;
        if (Methods.completeBitmapList.size() < itemsPerPage) {
            itemsPerPage = Methods.completeBitmapList.size();
        }
        lastPage = (Methods.completeBitmapList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage && (Methods.completeBitmapList.size() % itemsPerPage) != 0) {
            itemsPerPage = Methods.completeBitmapList.size() % itemsPerPage;
            startIndex = Methods.completeBitmapList.size() - itemsPerPage;
            endIndex = Methods.completeBitmapList.size();

        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Methods.completeBitmapList.size());
        }
        //There will be a better way to do this, but works
        imagesForPage = Methods.completeBitmapList.subList(startIndex, endIndex);
        gbcImagesForPage = Methods.gbcImagesList.subList(startIndex, endIndex);
        customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, gbcImagesForPage, imagesForPage, false);
        gridView.setAdapter(customGridViewAdapterImage);
    }


    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapterImage extends ArrayAdapter<GbcImage> {
        Context context;
        int layoutResourceId;
        List<GbcImage> data = new ArrayList<GbcImage>();
        private List<Bitmap> images;
        private boolean checkDuplicate;

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
            this.checkDuplicate = checkDuplicate;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            RecordHolder holder = null;
            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new RecordHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.tvName);
                holder.imageItem = (ImageView) row.findViewById(R.id.imageView);


                row.setTag(holder);
            } else {
                holder = (RecordHolder) row.getTag();
            }

            Bitmap image = images.get(position);
            String name = data.get(position).getName();
            String hash = data.get(position).getHashCode();
            Boolean fav = data.get(position).getTags().contains("__filter:favourite__");
            holder.imageItem.setBackgroundColor(fav ? context.getColor(R.color.favorite) : Color.WHITE);
            Boolean dup = false;
            if (checkDuplicate) {
                for (GbcImage gbcImage : Methods.gbcImagesList) {
                    // Comparar el valor de la propiedad "nombre" de cada objeto con el valor del nuevo objeto
                    if (gbcImage.getHashCode().equals(hash)) {
                        // Si el valor es igual, significa que el nombre ya existe en otro objeto de la lista
                        dup = true;
                    }
                }
            }
            holder.txtTitle.setTextColor(dup ? context.getResources().getColor(R.color.duplicated) : Color.BLACK);
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), false));
            return row;
        }

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;

        }
    }


    /**
     * Detects left and right swipes across a view.
     * https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }

}
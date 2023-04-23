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
import android.os.AsyncTask;
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
import android.view.WindowManager;
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

import com.mraulio.gbcameramanager.db.ImageDao;
import com.mraulio.gbcameramanager.db.ImageDataDao;
import com.mraulio.gbcameramanager.model.ImageData;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GalleryFragment extends Fragment {

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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
    static boolean finishedUpdating = false;

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


        Button btnPrevPage = (Button) view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = (Button) view.findViewById(R.id.btnNextPage);
        Button btnFirstPage = (Button) view.findViewById(R.id.btnFirstPage);
        Button btnLastPage = (Button) view.findViewById(R.id.btnLastPage);

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
        btnFirstPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_page.setTextColor(getContext().getResources().getColor(R.color.duplicated));
                if (currentPage > 0) {
                    currentPage = 0;
                    updateGridView(currentPage);
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                }
            }

        });
        btnLastPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv_page.setTextColor(getContext().getResources().getColor(R.color.duplicated));
                if (currentPage < lastPage) {
                    currentPage = lastPage;
                    updateGridView(currentPage);
                    tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
                }
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
                    selectedPosition = Methods.gbcImagesList.size() - (itemsPerPage - position);
                }
                final Bitmap[] selectedImage = {Methods.imageBitmapCache.get(Methods.gbcImagesList.get(selectedPosition).getHashCode())};
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
                    globalImageIndex = Methods.gbcImagesList.size() - (itemsPerPage - position);
                }
                if (Methods.gbcImagesList.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                    imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                }
                if (Methods.gbcImagesList.get(globalImageIndex).isLockFrame()) {
                    System.out.println("is lock frame true");
                    keepFrame = true;
                    cbFrameKeep.setChecked(true);
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
                            showCustomDialog(Methods.imageBitmapCache.get(Methods.gbcImagesList.get(globalImageIndex).getHashCode()));
                            clickCount = 0;
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
                            if (Methods.gbcImagesList.get(globalImageIndex).getTags().contains("__filter:favourite__")) {
                                List<String> tags = Methods.gbcImagesList.get(globalImageIndex).getTags();
                                for (Iterator<String> iter = tags.iterator(); iter.hasNext(); ) {
                                    String nombre = iter.next();
                                    if (nombre.equals("__filter:favourite__")) {
                                        iter.remove();
                                    }
                                    Methods.gbcImagesList.get(globalImageIndex).setTags(tags);
                                    imageView.setBackgroundColor(getContext().getColor(R.color.white));
                                }
                                Methods.toast(getContext(), "Removed as favorite" + globalImageIndex);
                            } else {
                                Methods.gbcImagesList.get(globalImageIndex).addTag("__filter:favourite__");
                                Methods.toast(getContext(), "Set as favorite" + globalImageIndex);
                                imageView.setBackgroundColor(getContext().getColor(R.color.favorite));
                            }
                            clickCount = 0;
                            System.out.println(Methods.gbcImagesList.get(globalImageIndex).getTags());
                            updateGridView(currentPage);
                        }
                        //To save the image with the favorite tag to the database
                        new SaveImageAsyncTask(Methods.gbcImagesList.get(globalImageIndex)).execute();
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
                        Bitmap bitmap = null;
                        try {
                            bitmap = frameChange(globalImageIndex, Methods.gbcImagesList.get(globalImageIndex).getFrameIndex(), keepFrame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        GbcImage gbcImage = Methods.gbcImagesList.get(globalImageIndex);
                        gbcImage.setLockFrame(keepFrame);
                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(globalImageIndex).getHashCode(), bitmap);
//                        Bitmap image = Methods.imageBitmapCache.get(Methods.gbcImagesList.get(globalImageIndex).getHashCode());
                        System.out.println(Methods.gbcImagesList.get(globalImageIndex).getHashCode());
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 6, bitmap.getHeight() * 6, false));
                        new SaveImageAsyncTask(gbcImage).execute();
                        updateGridView(currentPage);

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
                        Methods.gbcImagesList.get(globalImageIndex).setFrameIndex(selectedFrameIndex);//Need to set the frame index before changing it because if not it's not added to db

                        try {
                            framed = frameChange(globalImageIndex, selectedFrameIndex, keepFrame);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(framed, framed.getWidth() * 6, framed.getHeight() * 6, false));
//                        Methods.gbcImagesList.get(globalImageIndex).setImageBytes(imageBytes);
                        selectedImage[0] = framed;
                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(globalImageIndex).getHashCode(), framed);
//                        Methods.completeBitmapList.set(globalImageIndex, framed);
                        frameAdapter.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getFrameIndex());
                        frameAdapter.notifyDataSetChanged();
                        updateGridView(currentPage);
                    }
                });
                CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList, false, false);

                adapterPalette.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex());
                gridViewPalette.setAdapter(adapterPalette);
                gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int palettePosition, long id) {
                        //Action when clicking a palette inside the Dialog
                        Bitmap changedImage;
                        if (!keepFrame) {
                            Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(0);//Need to set this to the palette 0 to then change it with the frame
                        }
                        changedImage = paletteChanger(palettePosition, Methods.gbcImagesList.get(globalImageIndex).getImageBytes(), Methods.gbcImagesList.get(globalImageIndex));
                        Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(palettePosition);

                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(globalImageIndex).getHashCode(), changedImage);
//                        Methods.completeBitmapList.set(globalImageIndex, changedImage);
                        if (keepFrame) {
                            try {
                                changedImage = frameChange(globalImageIndex, Methods.gbcImagesList.get(globalImageIndex).getFrameIndex(), keepFrame);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(palettePosition);
//                        new SaveImageAsyncTask(Methods.gbcImagesList.get(globalImageIndex)).execute();
                        adapterPalette.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex());
                        adapterPalette.notifyDataSetChanged();
                        Methods.imageBitmapCache.put(Methods.gbcImagesList.get(globalImageIndex).getHashCode(), changedImage);
                        selectedImage[0] = changedImage;//Needed to save the image with the palette changed without leaving the Dialog
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(changedImage, changedImage.getWidth() * 6, changedImage.getHeight() * 6, false));
                        updateGridView(currentPage);
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
            updateGridView(currentPage);
            tv.setText("Total of images: " + GbcImage.numImages);

        } else {
            tv.setText("Loading...");
        }
        tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));

        return view;
    }


    public static Bitmap frameChange(int globalImageIndex, int selectedFrameIndex, boolean keepFrame) throws IOException {
        // Obtener la imagen seleccionada
        Bitmap framed = null;
        Bitmap framedAux;
        if ((Methods.gbcImagesList.get(globalImageIndex).getImageBytes().length / 40) == 144) {
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
            Bitmap croppedBitmap = Bitmap.createBitmap(Methods.imageBitmapCache.get(Methods.gbcImagesList.get(globalImageIndex).getHashCode()), 16, 16, 128, 112);
            canvas.drawBitmap(croppedBitmap, 16, 16, null);
//            Methods.imageBitmapCache.put(Methods.gbcImagesList.get(globalImageIndex).getHashCode(), framed);
//            Methods.completeBitmapList.set(globalImageIndex, framed);
            try {
                byte[] imageBytes = Methods.encodeImage(framedAux);
                Methods.gbcImagesList.get(globalImageIndex).setImageBytes(imageBytes);//Use the framedAux because it doesn't a different palette to encode
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new SaveImageAsyncTask(Methods.gbcImagesList.get(globalImageIndex)).execute();
        return framed;
    }

    //Cambiar paleta
    public static Bitmap paletteChanger(int index, byte[] imageBytes, GbcImage gbcImage) {
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt()), 160, imageBytes.length / 40);//imageBytes.length/40 to get the height of the image
        Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(index).getPaletteColorsInt(), imageBytes);
        new SaveImageAsyncTask(gbcImage).execute();

        return image;
    }

    //To show the "big" Image dialog when doing a simple tap on the image
    private void showCustomDialog(Bitmap bitmap) {
        // Crear el diálogo personalizado
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.single_image_dialog);

        // Configurar el tamaño del diálogo
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // Obtener el ImageView y configurarlo como desplazable
        ImageView imageView = dialog.findViewById(R.id.imageView);
        imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false));

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        // Obtener el botón de cerrar y configurar su acción
        Button closeButton = dialog.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Mostrar el diálogo personalizado
        dialog.show();
        dialog.getWindow().setAttributes(lp);
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

    private void prevPage() {
        tv_page.setTextColor(getContext().getResources().getColor(R.color.duplicated));

        if (currentPage > 0) {
            currentPage--;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        }
    }

    private void nextPage() {
        tv_page.setTextColor(getContext().getResources().getColor(R.color.duplicated));

        if (currentPage < lastPage) {
            currentPage++;
            updateGridView(currentPage);
            tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        }
    }

    //The shared image is stored in Pictures and compressed, then shared. This needs to change.
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
            updateGridView(currentPage);
            tv.setText("Total of images: " + GbcImage.numImages);

        } else {
            tv.setText("No images in the gallery. Go to Import tab.");
        }
        tv_page.setText((currentPage + 1) + " / " + (lastPage + 1));
        System.out.println("Called updateFromMain");
        updateGridView(currentPage);
    }

    //Method to update the gallery gridview
    public static void updateGridView(int page) {
        //Bitmap list to store current page bitmaps
        imagesForPage = new ArrayList<>();

        itemsPerPage = MainActivity.imagesPage;
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        if (Methods.gbcImagesList.size() < itemsPerPage) {
            itemsPerPage = Methods.gbcImagesList.size();
        }
        lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage && (Methods.gbcImagesList.size() % itemsPerPage) != 0) {
            itemsPerPage = Methods.gbcImagesList.size() % itemsPerPage;
            startIndex = Methods.gbcImagesList.size() - itemsPerPage;
            endIndex = Methods.gbcImagesList.size();

        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Methods.gbcImagesList.size());
        }
        boolean doAsync = false;

        //The bitmaps come from the BitmapCache map, using the gbcimage hashcode
        for (GbcImage gbcImage : Methods.gbcImagesList.subList(startIndex, endIndex)) {
//            imagesForPage.add(Methods.imageBitmapCache.get(gbcImage.getHashCode()));
            if (!Methods.imageBitmapCache.containsKey(gbcImage.getHashCode())) {
                doAsync = true;
            }
        }
        if (doAsync) {
            new UpdateGridViewAsyncTask().execute();
        } else {
            List<Bitmap> bitmapList = new ArrayList<>();
            for (GbcImage gbcImage : Methods.gbcImagesList.subList(startIndex, endIndex)) {
                bitmapList.add(Methods.imageBitmapCache.get(gbcImage.getHashCode()));
            }
            tv_page.setTextColor(gridView.getContext().getResources().getColor(R.color.black));
            customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, Methods.gbcImagesList.subList(startIndex, endIndex), bitmapList, false, false);
            gridView.setAdapter(customGridViewAdapterImage);

        }
//            imagesForPage = Methods.completeBitmapList.subList(startIndex, endIndex);
//        gbcImagesForPage = Methods.gbcImagesList.subList(startIndex, endIndex);
    }

    private static class UpdateGridViewAsyncTask extends AsyncTask<Void, Void, Void> {

        //I could add a isCancelled flag
        @Override
        protected Void doInBackground(Void... voids) {
            //Store the indexes because if I call it again, it will use startIndex with a different value and crash
            int newStartIndex = startIndex;
            int newEndIndex = endIndex;

            List<String> currentPageHashes = new ArrayList<>();
            ImageDataDao imageDataDao = MainActivity.db.imageDataDao();
            //foreach index
            int index = 0;
            //Loop for each gbcImage in a sublist in all gbcImages objects for the current page
            for (GbcImage gbcImage : Methods.gbcImagesList.subList(newStartIndex, newEndIndex)) {
                //Add the hashcode to the list of current hashes
                currentPageHashes.add(gbcImage.getHashCode());
                byte[] imageBytes;
//                if (Methods.imageBytesCache.containsKey(gbcImage.getHashCode())) {
//                    System.out.println("Entrando contains"+ index);
//                    imageBytes = Methods.imageBytesCache.get(gbcImage.getHashCode());
//                } else {
//                    System.out.println("Entrando dao"+index);
                //Get the image bytes from the database for the current gbcImage
                imageBytes = imageDataDao.getDataByImageId(gbcImage.getHashCode());
//                }
                //Set the image bytes to the object
                gbcImage.setImageBytes(imageBytes);
//                Methods.gbcImagesList.get(newStartIndex + index).setImageBytes(imageBytes);
                //Also put the image bytes in the cache map
//                Methods.imageBytesCache.put(gbcImage.getHashCode(), imageBytes);
                //Create the image bitmap
                int height = (imageBytes.length + 1) / 40;//To get the real height of the image
                ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(0).getPaletteColorsInt()), 160, height);
                Bitmap image = imageCodec.decodeWithPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColorsInt(), imageBytes);
                //Add the bitmap to the cache
                Methods.imageBitmapCache.put(gbcImage.getHashCode(), image);

                //Do a frameChange to create the Bitmap of the image
                try {
                    //Only do frameChange if the image is 144 height
                    if (image.getHeight() == 144)
                        image = frameChange(newStartIndex + index, Methods.gbcImagesList.get(newStartIndex + index).getFrameIndex(), Methods.gbcImagesList.get(newStartIndex + index).isLockFrame());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Methods.imageBitmapCache.put(gbcImage.getHashCode(), image);
                index++;
            }
            gbcImagesForPage = Methods.gbcImagesList.subList(newStartIndex, newEndIndex);
            //Create a list of bitmaps to use in the adapter, getting the bitmaps from the cache map for each image in the current page
            List<Bitmap> bitmapList = new ArrayList<>();
            for (GbcImage gbcImage : gbcImagesForPage) {
                bitmapList.add(Methods.imageBitmapCache.get(gbcImage.getHashCode()));
            }
            customGridViewAdapterImage = new CustomGridViewAdapterImage(gridView.getContext(), R.layout.row_items, Methods.gbcImagesList.subList(newStartIndex, newEndIndex), bitmapList, false, false);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Notifies the adapter
            gridView.setAdapter(customGridViewAdapterImage);
            tv_page.setTextColor(gridView.getContext().getResources().getColor(R.color.black));

        }
    }

    //Method to update an image to the database in the background
    public static class SaveImageAsyncTask extends AsyncTask<Void, Void, Void> {
        private GbcImage gbcImage;

        //Stores the image passes as parameter in the constructor
        public SaveImageAsyncTask(GbcImage gbcImage) {
            this.gbcImage = gbcImage;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ImageDao imageDao = MainActivity.db.imageDao();
            imageDao.update(gbcImage);
            return null;
        }
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
        private boolean showName;

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images, boolean checkDuplicate,
                                          boolean showName) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
            this.checkDuplicate = checkDuplicate;
            this.showName = showName;
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
            if (showName) {
                holder.txtTitle.setTextColor(dup ? context.getResources().getColor(R.color.duplicated) : Color.BLACK);
                holder.txtTitle.setText(name);
            } else {
                holder.txtTitle.setVisibility(View.GONE);
            }

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
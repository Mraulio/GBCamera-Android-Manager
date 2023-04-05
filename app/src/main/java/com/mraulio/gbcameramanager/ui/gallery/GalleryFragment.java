package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_HEIGHT;
import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_WIDTH;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.CustomGridViewAdapterPalette;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.StartCreation;
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
import java.util.List;

public class GalleryFragment extends Fragment {

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss_dd-MM-yyyy");

    public static GridView gridView;
    private static int itemsPerPage = 15;
    static int startIndex = 0;
    static int endIndex = 0;
    static int currentPage = 0;
    static int lastPage = 0;
    boolean crop = false;
    boolean showPalettes = true;
    TextView tv_page;
    boolean keepFrame = false;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        MainActivity.pressBack = true;
        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);

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
                Toast toast = Toast.makeText(getContext(), "Size: " + Methods.listImageBytes, Toast.LENGTH_LONG);

                keepFrame = false;
                // Obtener la imagen seleccionada
                if (currentPage != lastPage) {
                    selectedPosition = position + (currentPage * itemsPerPage);
                } else {
                    selectedPosition = Methods.completeImageList.size() - (itemsPerPage - position);
                }
                final Bitmap[] selectedImage = {Methods.completeImageList.get(selectedPosition)};
                byte[] selectedImageBytes = Methods.gbcImagesList.get(selectedPosition).getImageBytes();
                // Crear el diálogo personalizado
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);

                // Configurar la vista de imagen del diálogo
                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage[0], selectedImage[0].getWidth() * 6, selectedImage[0].getHeight() * 6, false));

                // Configurar el botón de cierre del diálogo
                Button printButton = dialog.findViewById(R.id.print_button);

                Button saveButton = dialog.findViewById(R.id.save_button);
                Button cropButton = dialog.findViewById(R.id.crop_save_button);
//                Button paletteButton = dialog.findViewById(R.id.btn_palette);
                Button paletteFrameSelButton = dialog.findViewById(R.id.btnPaletteFrame);
                GridView gridViewPalette = dialog.findViewById(R.id.gridViewPal);
                GridView gridViewFrames = dialog.findViewById(R.id.gridViewFra);
                CheckBox cbFrameKeep = dialog.findViewById(R.id.cbFrameKeep);
                showPalettes = true;

                int globalImageIndex;
                if (currentPage != lastPage) {
                    globalImageIndex = position + (currentPage * itemsPerPage);
                } else {
                    globalImageIndex = Methods.completeImageList.size() - (itemsPerPage - position);
                }

                paletteFrameSelButton.setText("Show frames.");
                FramesFragment.CustomGridViewAdapterFrames frameAdapter = new FramesFragment.CustomGridViewAdapterFrames(getContext(), R.layout.frames_row_items, Methods.framesList);
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
                gridViewFrames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int selectedFrameIndex, long id) {
                        //Action when clicking a frame inside the Dialog
                        Bitmap framed = frameChange(globalImageIndex, selectedFrameIndex, keepFrame);

                        imageView.setImageBitmap(Bitmap.createScaledBitmap(framed, framed.getWidth() * 6, framed.getHeight() * 6, false));
//                        Methods.gbcImagesList.get(globalImageIndex).setImageBytes(imageBytes);
                        selectedImage[0] = framed;
                        Methods.completeImageList.set(globalImageIndex, framed);
                        Methods.gbcImagesList.get(globalImageIndex).setFrameIndex(selectedFrameIndex);
                        frameAdapter.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getFrameIndex());
                        frameAdapter.notifyDataSetChanged();
                        updateGridView(currentPage, gridView);
                    }
                });
                CustomGridViewAdapterPalette adapterPalette = new CustomGridViewAdapterPalette(getContext(), R.layout.palette_grid_item, Methods.gbcPalettesList);
                adapterPalette.setLastSelectedPosition(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex());
                gridViewPalette.setAdapter(adapterPalette);
                gridViewPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position2, long id) {
                        //Action when clicking a palette inside the Dialog
                        Bitmap changedImage;

                        if (!keepFrame) {
                            changedImage = paletteChanger2(0, selectedImage[0], globalImageIndex);
                            Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(0);//Need to set this to the palette 0 to then change it with the frame
                            changedImage = paletteChanger2(position2, changedImage, globalImageIndex);

                        } else
                            changedImage = paletteChanger2(position2, selectedImage[0], globalImageIndex);
                        Methods.gbcImagesList.get(globalImageIndex).setPaletteIndex(position2);
                        Methods.completeImageList.set(globalImageIndex, changedImage);
                        if (keepFrame) {
                            changedImage = frameChange(globalImageIndex, Methods.gbcImagesList.get(globalImageIndex).getFrameIndex(), keepFrame);
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


                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        crop = false;
                        LocalDateTime now = LocalDateTime.now();
                        String fileName = "image_";
                        fileName += dtf.format(now) + ".png";
                        saveImage(selectedImage[0], fileName);
                    }
                });
                cropButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        crop = true;
                        LocalDateTime now = LocalDateTime.now();
                        String fileName = "image_";
                        fileName += dtf.format(now) + ".png";
                        saveImage(selectedImage[0], fileName);
                    }
                });


// Configurar el diálogo para que ocupe toda la pantalla
//                Window window = dialog.getWindow();
//                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                //Show Dialog
                dialog.show();
            }
        });
        if (Methods.gbcImagesList.size() > 0) {
            updateGridView(currentPage, gridView);
            tv.setText("Total of images: " + GbcImage.numImages);

        } else {
            tv.setText("No images in the gallery. Go to Import tab.");
        }
        tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));

        return view;
    }
    private Bitmap frameChange(int globalImageIndex, int selectedFrameIndex, boolean keepFrame) {
        // Obtener la imagen seleccionada
        Bitmap framed = null;
        Bitmap framedAux = null;
        if (Methods.completeImageList.get(globalImageIndex).getHeight() == 144 && Methods.completeImageList.get(globalImageIndex).getWidth() == 160) {
            //I need to use copy because if not it's inmutable bitmap
            framed = Methods.framesList.get(selectedFrameIndex).getFrameBitmap().copy(Bitmap.Config.ARGB_8888, true);
            framedAux = framed.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvasAux = new Canvas(framedAux);
            Bitmap setToPalette = paletteChanger2(0,Methods.completeImageList.get(globalImageIndex),globalImageIndex);
            Bitmap croppedBitmapAux = Bitmap.createBitmap(setToPalette, 16, 16, 128, 112);//Need to put this to palette 0
            canvasAux.drawBitmap(croppedBitmapAux, 16, 16, null);
            if (!keepFrame) {
                framed = paletteChanger2(Methods.gbcImagesList.get(globalImageIndex).getPaletteIndex(), framed, 0);
                framed = framed.copy(Bitmap.Config.ARGB_8888, true);//To make it mutable
            }
            Canvas canvas = new Canvas(framed);
            Bitmap croppedBitmap = Bitmap.createBitmap(Methods.completeImageList.get(globalImageIndex), 16, 16, 128, 112);
            canvas.drawBitmap(croppedBitmap, 16, 16, null);
            Methods.completeImageList.set(globalImageIndex, framed);
            try {
                Methods.gbcImagesList.get(globalImageIndex).setImageBytes(Methods.encodeImage(framedAux, Methods.gbcImagesList.get(globalImageIndex)));//Use the framedAux because it doesn't a different palette to encode
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return framed;
    }
    //Cambiar paleta
    public Bitmap paletteChanger(int index, byte[] imageBytes, GbcImage gbcImage) {
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, imageBytes.length / 40);//imageBytes.length/40 to get the height of the image
        System.out.println("************************HEIGHT" + (imageBytes.length / 40) + "++++++++++++++++++++++++++++++");
        Bitmap image = imageCodec.decodeWithPalette(index, imageBytes);

        //If the image is 128x112 (extracted from sav) I apply the frame
        if ((imageBytes.length / 40) == 112) {
            ImageCodec imageCodec2 = new ImageCodec(new IndexedPalette(Methods.gbcPalettesList.get(gbcImage.getPaletteIndex()).getPaletteColors()), 160, 144);
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
        int[] oldColors = Methods.gbcPalettesList.get(Methods.gbcImagesList.get(imageIndex).getPaletteIndex()).getPaletteColors();
        int[] newColors = Methods.gbcPalettesList.get(newPaletteIndex).getPaletteColors();
        for (int i = 0; i < pixels.length; i++) {
            int pixelColor = pixels[i];
            if (pixelColor == oldColors[0]) {
                pixels[i] = newColors[0];
            } else if (pixelColor == oldColors[1]) {
                pixels[i] = newColors[1];
            } else if (pixelColor == oldColors[2]) {
                pixels[i] = newColors[2];
            } else if (pixelColor == oldColors[3]) {
                pixels[i] = newColors[3];
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

    private void updateGridView(int page, GridView gridView) {
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        itemsPerPage = 15;

        if (Methods.completeImageList.size() < itemsPerPage) {
            itemsPerPage = Methods.completeImageList.size();
        }
        lastPage = (Methods.completeImageList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage && (Methods.completeImageList.size() % itemsPerPage) != 0) {
            itemsPerPage = Methods.completeImageList.size() % itemsPerPage;
            startIndex = Methods.completeImageList.size() - itemsPerPage;
            endIndex = Methods.completeImageList.size();

        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Methods.completeImageList.size());
        }

        //There will be a better way to do this, but works
        List<Bitmap> imagesForPage = Methods.completeImageList.subList(startIndex, endIndex);
        List<GbcImage> gbcImagesForPage = Methods.gbcImagesList.subList(startIndex, endIndex);
        gridView.setAdapter(new CustomGridViewAdapterImage(getContext(), R.layout.row_items, gbcImagesForPage, imagesForPage));
    }


    public static class CustomGridViewAdapter extends BaseAdapter {
        private List<Bitmap> images;
        private Context context;


        public CustomGridViewAdapter(Context context, List<Bitmap> images) {
            this.context = context;
            this.images = images;
        }

        public int getCount() {
            return itemsPerPage;
        }

        public Object getItem(int position) {
            return images.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // Si la vista aún no ha sido creada, inflar el layout del elemento de la lista
                convertView = LayoutInflater.from(context).inflate(R.layout.row_items, parent, false);
                // Crear una nueva vista de imagen
                imageView = convertView.findViewById(R.id.imageView);
                // Establecer la vista de imagen como la vista del elemento de la lista
                convertView.setTag(imageView);
            } else {
                // Si la vista ya existe, obtener la vista de imagen del tag
                imageView = (ImageView) convertView.getTag();
            }
            //Obtener la imagen de la lista

            Bitmap image = images.get(position);

            // Establecer la imagen en la vista de imagen
//            imageView.setImageBitmap(image);

            imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 4, image.getHeight() * 4, false));
            return convertView;
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

        public CustomGridViewAdapterImage(Context context, int layoutResourceId,
                                          List<GbcImage> data, List<Bitmap> images) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.images = images;
            this.data = data;
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
            holder.txtTitle.setText(name);
            holder.imageItem.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
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
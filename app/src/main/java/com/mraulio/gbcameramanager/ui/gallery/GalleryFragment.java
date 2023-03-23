package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_HEIGHT;
import static com.mraulio.gbcameramanager.gameboycameralib.constants.SaveImageConstants.IMAGE_WIDTH;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.gameboycameralib.codecs.ImageCodec;
import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH-mm-ss_dd-MM-yyyy");

    //    List<Bitmap> imageList;
    public static GridView gridView;

    private int pageNumber = 0;
    private static int itemsPerPage = 9;
    static int startIndex = 0;
    static int endIndex = 0;
    static int currentPage = 0;
    static int lastPage = 0;
//    List<Bitmap> listBitmaps = new ArrayList<>();
    TextView tv_page;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);

        Button btnPrevPage = (Button) view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = (Button) view.findViewById(R.id.btnNextPage);
        tv_page = (TextView) view.findViewById(R.id.tv_page);

        view.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                // Whatever
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

//        Button btnLoadImages = (Button) view.findViewById(R.id.btnLoadImages);

//        btnLoadImages.setOnClickListener(v -> loadImages());

        /**
         * Dialog when clicking an image
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedPosition = 0;

                // Obtener la imagen seleccionada
                if (currentPage != lastPage) {
                    selectedPosition = position + (currentPage * itemsPerPage);
                } else {
                    selectedPosition = Methods.completeImageList.size() - (itemsPerPage - position);
                }
                Bitmap selectedImage = Methods.completeImageList.get(selectedPosition);

                // Crear el diálogo personalizado
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);

                // Configurar la vista de imagen del diálogo
                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, selectedImage.getWidth() * 6, selectedImage.getHeight() * 6, false));

                // Configurar el botón de cierre del diálogo
                Button saveButton = dialog.findViewById(R.id.save_button);
                Button paletteButton = dialog.findViewById(R.id.btn_palette);


                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LocalDateTime now = LocalDateTime.now();
                        String fileName = "image_";
                        fileName += dtf.format(now) + ".png";
                        saveImage(selectedImage, fileName);
                    }
                });

//                paletteButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        cambiarPaleta(selectedImage);
//                        int selectedPosition2=0;
//
//                        // Obtener la imagen seleccionada
//                        if (currentPage != lastPage) {
//                            selectedPosition2 = position + (currentPage * itemsPerPage);
//                        }else{
//                            selectedPosition2 = imageList.size()-(itemsPerPage-position);
//                        }
//                        imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, selectedImage.getWidth() * 6, selectedImage.getHeight() * 6, false));
//                        imageList.set(selectedPosition2, selectedImage);
//                        updateGridView(currentPage, gridView);
//                    }
//                });

// Configurar el diálogo para que ocupe toda la pantalla
//                Window window = dialog.getWindow();
//                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//
//// Configurar el diálogo para que sea transparente
//                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                // Mostrar el diálogo
                dialog.show();
            }
        });


        /**
         * I call the extractImagesSav method and load the images on the GalleryFragment gridview
         */
//        Methods.ImageAdapter imageAdapter = new Methods.ImageAdapter(this, Methods.gbcImagesList, Methods.gbcImagesList.size());
        ImageCodec imageCodec = new ImageCodec(new IndexedPalette(IndexedPalette.GAMEBOY_LCD_PALETTE), IMAGE_WIDTH, IMAGE_HEIGHT);

        //TOO SLOW, BETTER TO DECODE BEFORE THE IMAGES AND THEN GRAB THE BITMAP LIST FROM THE DECODED IMAGES HERE
//        for (Bitmap image : Methods.completeImageList) {
////            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(IndexedPalette.GAMEBOY_LCD_PALETTE), IMAGE_WIDTH, IMAGE_HEIGHT);
//
//            listBitmaps.add(image);
//        }
        gridView.setAdapter(new CustomGridViewAdapter(getActivity(), Methods.completeImageList, itemsPerPage));
        lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;
        tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));

//        CustomGridViewAdapter imageAdapter = new CustomGridViewAdapter(getContext(), Methods.gbcImagesList,itemsPerPage);
//        loadImages(imageAdapter);

        tv.setText("Total of images: " + GbcImage.numImages);

        return view;


    }


    //Previous page
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGridView(currentPage, gridView);
//                    tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
        }
    }

    private void nextPage(){
        int lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;

        if (currentPage < lastPage) {

            currentPage++;
            System.out.println("***************last page " + lastPage);
            System.out.println("***************current page " + currentPage);
            updateGridView(currentPage, gridView);
            tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
        }
    }

    //    public void loadImages(CustomGridViewAdapter imageAdapter) {
//        int startIndex = pageNumber * itemsPerPage;
//        int endIndex = startIndex + itemsPerPage;
//        gridView.setAdapter(new CustomGridViewAdapter(getActivity(), R.layout.row_items, Methods.gbcImagesList.subList(startIndex, endIndex)));
//    }
    private void saveImage(Bitmap image, String fileName) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(directory, fileName);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }
//            if (!file.exists()) { // Si no existe, crea el archivo.
//                file.createNewFile();
//            }
        } catch (Exception e) {
//        Toast toast = Toast.makeText(get, "Error al crear el fichero" + e.toString(), Toast.LENGTH_LONG);
//        toast.show();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            Bitmap scaled = Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false);

            scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast toast = Toast.makeText(getContext(), "SAVED", Toast.LENGTH_LONG);
            toast.show();
            // PNG is a lossless format, the compression factor (100) is ignored

        } catch (IOException e) {
            e.printStackTrace();
//        Toast toast = Toast.makeText(this, "Error al crear el fos" + e.toString(), Toast.LENGTH_LONG);
//        toast.show();
        }
    }

    private void updateGridView(int page, GridView gridView) {
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        itemsPerPage = 9;

        if (Methods.completeImageList.size() < itemsPerPage) {
            itemsPerPage = Methods.completeImageList.size();
        }
        int lastPage = (Methods.completeImageList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage) {
            itemsPerPage = Methods.completeImageList.size() % itemsPerPage;
            startIndex = Methods.completeImageList.size() - itemsPerPage;
            endIndex = Methods.completeImageList.size();
        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Methods.completeImageList.size());
        }
//        List<Bitmap> listBitmaps = new ArrayList<>();
//        for (GbcImage image : Methods.gbcImagesList) {
//            ImageCodec imageCodec = new ImageCodec(new IndexedPalette(IndexedPalette.GAMEBOY_LCD_PALETTE), IMAGE_WIDTH, IMAGE_HEIGHT);
////            listBitmaps.add(imageCodec.decode(image.getImageBytes()));
//        }
        List<Bitmap> imagesForPage = Methods.completeImageList.subList(startIndex, endIndex);
        gridView.setAdapter(new CustomGridViewAdapter(getContext(), imagesForPage, itemsPerPage));
    }


    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapter extends BaseAdapter {
        private List<Bitmap> images;
        private Context context;
        public int itemsPage;


        public CustomGridViewAdapter(Context context, List<Bitmap> images, int itemsPage) {
            this.context = context;
            this.images = images;
            this.itemsPage = itemsPage;
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

        private class RecordHolder {
            TextView txtTitle;
            ImageView imageItem;

        }
    }

    //https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
    /**
     * Detects left and right swipes across a view.
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
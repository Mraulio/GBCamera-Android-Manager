package com.mraulio.gbcameramanager.ui.gallery;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {


    //    List<Bitmap> imageList;
    public static GridView gridView;

    private int pageNumber = 0;
    private static int itemsPerPage = 9;
    static int startIndex = 0;
    static int endIndex = 0;
    static int currentPage = 0;
    static int lastPage = 0;
    List<Bitmap> listBitmaps = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);

        Button btnPrevPage = (Button) view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = (Button) view.findViewById(R.id.btnNextPage);
        TextView tv_page = (TextView) view.findViewById(R.id.tv_page);


        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage > 0) {
                    currentPage--;
                    updateGridView(currentPage, gridView);
//                    tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));

                }
            }
        });

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;

                if (currentPage < lastPage) {

                    currentPage++;
                    System.out.println("***************last page " + lastPage);
                    System.out.println("***************current page " + currentPage);
                    updateGridView(currentPage, gridView);
                    tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));
                }
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
                int selectedPosition=0;

                // Obtener la imagen seleccionada
                if (currentPage != lastPage) {
                    selectedPosition = position + (currentPage * itemsPerPage);
                }else{
                    selectedPosition = listBitmaps.size()-(itemsPerPage-position);
                }
                Bitmap selectedImage = listBitmaps.get(selectedPosition);

                // Crear el diálogo personalizado
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.custom_dialog);

                // Configurar la vista de imagen del diálogo
                ImageView imageView = dialog.findViewById(R.id.image_view);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, selectedImage.getWidth() * 6, selectedImage.getHeight() * 6, false));

                // Configurar el botón de cierre del diálogo
                Button saveButton = dialog.findViewById(R.id.save_button);
                Button paletteButton = dialog.findViewById(R.id.btn_palette);


//                saveButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        LocalDateTime now = LocalDateTime.now();
//                        String fileName = "image_";
//                        fileName += dtf.format(now) + ".png";
//                        saveImage(selectedImage, fileName);
//                    }
//                });

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
        for (GbcImage image : Methods.gbcImagesList) {
            listBitmaps.add(image.getBitmap());
        }
        gridView.setAdapter(new CustomGridViewAdapter(getActivity(), listBitmaps, itemsPerPage));
        lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;
        tv_page.setText("Page " + (currentPage + 1) + " of " + (lastPage + 1));

//        CustomGridViewAdapter imageAdapter = new CustomGridViewAdapter(getContext(), Methods.gbcImagesList,itemsPerPage);
//        loadImages(imageAdapter);

        tv.setText("Total of images: " + GbcImage.numImages);

        return view;


    }

//    public void loadImages(CustomGridViewAdapter imageAdapter) {
//        int startIndex = pageNumber * itemsPerPage;
//        int endIndex = startIndex + itemsPerPage;
//        gridView.setAdapter(new CustomGridViewAdapter(getActivity(), R.layout.row_items, Methods.gbcImagesList.subList(startIndex, endIndex)));
//    }


    private void updateGridView(int page, GridView gridView) {
        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
        itemsPerPage = 9;

        if (Methods.gbcImagesList.size() < itemsPerPage) {
            itemsPerPage = Methods.gbcImagesList.size();
        }
        int lastPage = (Methods.gbcImagesList.size() - 1) / itemsPerPage;

        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
        if (currentPage == lastPage) {
            itemsPerPage = Methods.gbcImagesList.size() % itemsPerPage;
            startIndex = Methods.gbcImagesList.size() - itemsPerPage;
            endIndex = Methods.gbcImagesList.size();
        } else {
            startIndex = page * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, Methods.gbcImagesList.size());
        }
        List<Bitmap> listBitmaps = new ArrayList<>();
        for (GbcImage image : Methods.gbcImagesList) {
            listBitmaps.add(image.getBitmap());
        }
        List<Bitmap> imagesForPage = listBitmaps.subList(startIndex, endIndex);
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

}
package com.mraulio.gbcameramanager.ui.gallery;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
//import com.mraulio.gbcameramanager.databinding.FragmentGalleryBinding;

import java.time.LocalDateTime;
import java.util.List;

public class GalleryFragment extends Fragment {

//    private FragmentGalleryBinding binding;

    List<Bitmap> imageList;
    public static GridView gridView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);
//        Button btnLoadImages = (Button) view.findViewById(R.id.btnLoadImages);
        tv.setText("Probando texto tv");

//        btnLoadImages.setOnClickListener(v -> loadImages());

        /**
         * Dialog when clicking an image
         */
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedPosition=0;
                System.out.println("***************boton pulsado dialog***********************");

                // Obtener la imagen seleccionada
//                if (currentPage != lastPage) {
//                    selectedPosition = position + (currentPage * itemsPerPage);
//                }else{
//                    selectedPosition = imageList.size()-(itemsPerPage-position);
//                }
                Bitmap selectedImage = imageList.get(selectedPosition);

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

                paletteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Methods.cambiarPaleta(selectedImage,1);
                        int selectedPosition2=0;

                        // Obtener la imagen seleccionada
//                        if (currentPage != lastPage) {
//                            selectedPosition2 = position + (currentPage * itemsPerPage);
//                        }else{
//                            selectedPosition2 = imageList.size()-(itemsPerPage-position);
//                        }
                        imageView.setImageBitmap(Bitmap.createScaledBitmap(selectedImage, selectedImage.getWidth() * 6, selectedImage.getHeight() * 6, false));
                        imageList.set(selectedPosition2, selectedImage);
                        gridView.setAdapter(new Methods.CustomGridViewAdapter(getActivity(), R.layout.row_items, Methods.gbcImagesList));
                    }
                });

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



        return view;


        /**
         * Using MVC
         *
         */
        //        GalleryViewModel galleryViewModel =
//                new ViewModelProvider(this).get(GalleryViewModel.class);
//
//        binding = FragmentGalleryBinding.inflate(inflater, container, false);
//        View root = binding.getRoot();
//
//        final TextView textView = binding.textGallery;
//        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
//        return root;
        /**
         *
         *
         */
    }

    public static void loadImages(Methods.CustomGridViewAdapter imageAdapter) {
        gridView.setAdapter(imageAdapter);
    }




//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
}
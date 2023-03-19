package com.mraulio.gbcameramanager.ui.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.databinding.FragmentGalleryBinding;

import java.util.List;

public class GalleryFragment extends Fragment {

//    private FragmentGalleryBinding binding;

    List<Bitmap> imageList;
    GridView gridView;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);
        Button btnLoadImages = (Button) view.findViewById(R.id.btnLoadImages);
        tv.setText("Probando texto tv");

        btnLoadImages.setOnClickListener(v -> loadImages());


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

    private void loadImages(){
        imageList = MainActivity.imageList;
        ImageAdapter imageAdapter = new ImageAdapter(getContext(), imageList, imageList.size());
        gridView.setAdapter(imageAdapter);
    }

    public class ImageAdapter extends BaseAdapter {
        private List<Bitmap> images;
        private Context context;
        public int itemsPage;

        public ImageAdapter(Context context, List<Bitmap> images, int itemsPage) {
            this.context = context;
            this.images = images;
            this.itemsPage = itemsPage;
        }

        public int getCount() {
            return images.size();
        }
//        public int getCount() {
//            return itemsPerPage;
//        }

        public Object getItem(int position) {
            return images.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

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


            imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
            return convertView;
        }

    }


//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
}
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
import com.mraulio.gbcameramanager.Methods;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.databinding.FragmentGalleryBinding;

import java.util.List;

public class GalleryFragment extends Fragment {

//    private FragmentGalleryBinding binding;

    List<Bitmap> imageList;
    static GridView gridView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        TextView tv = (TextView) view.findViewById(R.id.text_gallery);
        gridView = (GridView) view.findViewById(R.id.gridView);
//        Button btnLoadImages = (Button) view.findViewById(R.id.btnLoadImages);
        tv.setText("Probando texto tv");

//        btnLoadImages.setOnClickListener(v -> loadImages());

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

    public static void loadImages(Methods.ImageAdapter imageAdapter) {
        gridView.setAdapter(imageAdapter);
    }




//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
}
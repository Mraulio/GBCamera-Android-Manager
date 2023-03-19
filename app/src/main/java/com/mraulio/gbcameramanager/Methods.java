package com.mraulio.gbcameramanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Methods {

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
    protected static List<Bitmap> imageList100 = null;

    public static void extractSavImages(Context context) {
        Extractor extractor = new SaveImageExtractor(new IndexedPalette(IndexedPalette.EVEN_DIST_PALETTE));
        LocalDateTime now = LocalDateTime.now();
        File latestFile = null;
        try {
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File savFile = new File(downloadsDirectory + "/PHOTO_2023-01-14_17-32-04_Ramen_Gansos.sav");
//            File[] files = folder.listFiles();
//            if (files != null && files.length > 0) {
//                Arrays.sort(files, new Comparator<File>() {
//                    public int compare(File f1, File f2) {
//                        return Long.compare(f2.lastModified(), f1.lastModified());
//                    }
//                });
//                latestFile = files[0];
////                tv.append("\nThe name of the last SAV file is: " + latestFile.getName() + ".\n" +
////                        "Size: " + latestFile.length() / 1024 + "KB");
//            }
//            if (savFile.length() / 1024 == 128) {

                //Extract the images

            //Testing 3k images to see performance
                imageList100 = extractor.extract(savFile);
            for (Bitmap elemento : imageList100) {
                // Bucle for que repite 100 veces y agrega cada elemento a la lista vacía
                for (int i = 0; i < 100; i++) {
                    MainActivity.imageList.add(elemento);
                }
            }
//            Toast toast = Toast.makeText(context, MainActivity.imageList.size(), Toast.LENGTH_LONG);
//            toast.show();
//                tv.append("\nThe image list has: " + imageList.size() + " images.");

//            for (int i = 0; i < imageList.size(); i++) {
//                String fileName = "image_";
//                fileName += dtf.format(now) + "_" + i + ".png";
////                        saveImage(imageList.get(i), "/Images_" + dtf.format(now) + "/" + fileName);
//            }
//            if (imageList.size() < itemsPerPage) {
//                itemsPerPage = imageList.size();
//            }
//                ImageAdapter imageAdapter = new ImageAdapter(getApplicationContext(), imageList, imageList.size());
//                gridView.setAdapter(imageAdapter);
//            } else {
////                tv.append("\nNOT A GOOD SAVE DUMP.");
//            }
//            Toast toast = Toast.makeText(context, MainActivity.imageList.size(), Toast.LENGTH_LONG);
//            toast.show();

        } catch (IOException e) {
            Toast toast = Toast.makeText(context, "Error\n"+e.toString(), Toast.LENGTH_LONG);
            toast.show();

            e.printStackTrace();
        }
//        return imageList;
    }


    public static class ImageAdapter extends BaseAdapter {
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


}

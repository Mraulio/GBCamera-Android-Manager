package com.mraulio.gbcameramanager;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mraulio.gbcameramanager.gameboycameralib.constants.IndexedPalette;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.Extractor;
import com.mraulio.gbcameramanager.gameboycameralib.saveExtractor.SaveImageExtractor;
import com.mraulio.gbcameramanager.model.GbcImage;
import com.mraulio.gbcameramanager.model.GbcPalette;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Methods {

    public static List<Bitmap> completeImageList;
    public static List<Bitmap> imageList = new ArrayList<>();
    public static ArrayList<GbcImage> gbcImagesList = new ArrayList<>();
    public static ArrayList<GbcPalette> gbcPalettesList = new ArrayList<>();

    /**
     * *******************************************************************
     * TO READ THE SAV IMAGES
     */
    protected static List<GbcImage> imageList100 = null;

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
            imageList = extractor.extract(savFile);
            
            int nameIndex = 1;
            //Create gbcImage objects for each image
            for (Bitmap image: imageList){
                GbcImage gbcImage = new GbcImage();
                if (nameIndex%2==0)
                gbcImage.setBitmap(cambiarPaleta(image,1));
                else
                    gbcImage.setBitmap(image);
                gbcImage.setName("Image "+nameIndex);
                gbcImagesList.add(gbcImage);
                nameIndex++;
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
            Toast toast = Toast.makeText(context, "Error\n" + e.toString(), Toast.LENGTH_LONG);
            toast.show();

            e.printStackTrace();
        }
//        return imageList;
    }
    //Change Palette
    public static Bitmap cambiarPaleta(Bitmap image, int index) {

        int[] allpixels = new int[image.getHeight() * image.getWidth()];
        image.getPixels(allpixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        GbcPalette palette = gbcPalettesList.get(index);
        for (int i = 0; i < allpixels.length; i++) {
            if (allpixels[i] == gbcPalettesList.get(0).getPaletteColors()[0]) {
                allpixels[i] = palette.getPaletteColors()[0];
            } else if (allpixels[i] == gbcPalettesList.get(0).getPaletteColors()[1]) {
                allpixels[i] = palette.getPaletteColors()[1];
            } else if (allpixels[i] == gbcPalettesList.get(0).getPaletteColors()[2]) {
                allpixels[i] = palette.getPaletteColors()[2];
            } else if (allpixels[i] == gbcPalettesList.get(0).getPaletteColors()[3]) {
                allpixels[i] = palette.getPaletteColors()[3];
            }
        }

        image.setPixels(allpixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        return image;
    }
//
//    public static void updateGridView(int page, GridView gridView) {
//        //Por si la lista de imagenes es mas corta que el tamaño de paginacion
////        itemsPerPage = 12;
//
////        if (imageList.size() < itemsPerPage) {
////            itemsPerPage = imageList.size();
////        }
////        int lastPage = (imageList.size() - 1) / itemsPerPage;
//
//        //Para que si la pagina final no está completa (no tiene tantos items como itemsPerPage)
////        if (currentPage == lastPage) {
////            itemsPerPage = imageList.size() % itemsPerPage;
////            startIndex = imageList.size() - itemsPerPage;
////            endIndex = imageList.size();
////        } else {
////            startIndex = page * itemsPerPage;
////            endIndex = Math.min(startIndex + itemsPerPage, imageList.size());
////        }
//        List<Bitmap> imagesForPage = imageList;
//        gridView.setAdapter(new ImageAdapter(gridView.getContext(), imagesForPage,imageList.size()));
//    }

    public static class ImageAdapter extends BaseAdapter {
        private List<GbcImage> gbcImages;
        private Context context;
        public int itemsPage;

        public ImageAdapter(Context context, List<GbcImage> gbcImages, int itemsPage) {
            this.context = context;
            this.gbcImages = gbcImages;
            this.itemsPage = itemsPage;
        }

        public int getCount() {
            return gbcImages.size();
        }
//        public int getCount() {
//            return itemsPerPage;
//        }

        public Object getItem(int position) {
            return gbcImages.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            TextView textView = null;
            if (convertView == null) {
                // Si la vista aún no ha sido creada, inflar el layout del elemento de la lista
                convertView = LayoutInflater.from(context).inflate(R.layout.row_items, parent, false);
                // Crear una nueva vista de imagen
                imageView = convertView.findViewById(R.id.imageView);
                textView = convertView.findViewById(R.id.tvName);

                // Establecer la vista de imagen como la vista del elemento de la lista
                convertView.setTag(imageView);
//                convertView.setTag(textView);
            } else {
                // Si la vista ya existe, obtener la vista de imagen del tag
                imageView = (ImageView) convertView.getTag();
//                textView = (TextView) convertView.getTag();
            }
            //Obtener la imagen de la lista

            Bitmap image = gbcImages.get(position).getBitmap();
            String name = gbcImages.get(position).getName();

            imageView.setImageBitmap(Bitmap.createScaledBitmap(image, image.getWidth() * 6, image.getHeight() * 6, false));
//            textView.setText(name);
            return convertView;
        }
    }

    /**
     * Other way to show images on the GridView, with the Text
     */
    public static class CustomGridViewAdapter extends ArrayAdapter<GbcImage> {
        Context context;
        int layoutResourceId;
        ArrayList<GbcImage> data = new ArrayList<GbcImage>();

        public CustomGridViewAdapter(Context context, int layoutResourceId,
                                     ArrayList<GbcImage> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
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

            Bitmap image = data.get(position).getBitmap();
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


}

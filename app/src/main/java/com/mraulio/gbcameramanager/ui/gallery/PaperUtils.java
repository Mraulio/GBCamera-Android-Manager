package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.MainActivity.customColorPaper;
import static com.mraulio.gbcameramanager.MainActivity.exportSquare;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.crop;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryFragment.loadingDialog;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.makeSquareImage;
import static com.mraulio.gbcameramanager.ui.gallery.GalleryUtils.mediaScanner;
import static com.mraulio.gbcameramanager.ui.importFile.ImageConversionUtils.rotateBitmapImport;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PaperUtils {
    static SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();

    public static Bitmap paperize(Bitmap inputBitmap, int paperColor, boolean onlyImage, Context context) {
        int mul = 20;
        int overlapping = 4;

        int numBorders = 12;
        int num1 = (int) (Math.random() * numBorders) + 1;
        int num2;
        do {
            num2 = (int) (Math.random() * numBorders) + 1;
        } while (num1 == num2);
        String topBorderName = "border_" + num1;
        String bottomBorderName = "border_" + num2;

        int topResourceId = context.getResources().getIdentifier(topBorderName, "drawable", context.getPackageName());
        int bottomnResourceId = context.getResources().getIdentifier(bottomBorderName, "drawable", context.getPackageName());
        Bitmap topBorder = BitmapFactory.decodeResource(context.getResources(), topResourceId);
        topBorder = rotateBitmapImport(topBorder, 180);
        Bitmap bottomBorder = BitmapFactory.decodeResource(context.getResources(), bottomnResourceId);


        //Calculate paperized image size
        int speckleHeight = inputBitmap.getHeight() * (mul - overlapping) + overlapping;
        int speckleWidth = inputBitmap.getWidth() * (mul - overlapping) + overlapping;
        Bitmap paperizedImage = Bitmap.createBitmap(speckleWidth, speckleHeight, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(paperizedImage);
        can.drawColor(Color.WHITE);//Fill the canvas with white. If not it won't work (fills everything black)
        Random random = new Random();
        //Size of the region to copy from the pixel_sample (20x20 is a pixel)
        Bitmap pixelSampleBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.pixel_sample);
        int regionSize = 20;


        for (int y = 0; y < inputBitmap.getHeight(); y++) {
            for (int x = 0; x < inputBitmap.getWidth(); x++) {
                int a = y * (mul - overlapping);
                int c = x * (mul - overlapping);
                int color = inputBitmap.getPixel(x, y);
                int randomRegionX = random.nextInt(50) * regionSize;
                List<Integer> possibleRotations = Arrays.asList(0, 90, 180, 270);
                int degrees = possibleRotations.get(random.nextInt(possibleRotations.size()));
                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);
                //If color is #FFFFFF do nothing, it will be just paper color
                if (color == Color.parseColor("#AAAAAA")) {
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 2 * regionSize, regionSize, regionSize, matrix, false);

                    Bitmap baseBitmap = Bitmap.createBitmap(paperizedImage, c, a, 20, 20);
                    Bitmap overlapped = overlap(baseBitmap, regionBitmap);
                    Canvas canvas = new Canvas(paperizedImage);
                    canvas.drawBitmap(overlapped, c, a, null);
                } else if (color == Color.parseColor("#555555")) {
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 1 * regionSize, regionSize, regionSize, matrix, false);

                    Bitmap baseBitmap = Bitmap.createBitmap(paperizedImage, c, a, 20, 20);
                    Bitmap overlapped = overlap(baseBitmap, regionBitmap);
                    Canvas canvas = new Canvas(paperizedImage);
                    canvas.drawBitmap(overlapped, c, a, null);
                } else if (color == Color.parseColor("#000000")) {
                    Bitmap regionBitmap = Bitmap.createBitmap(pixelSampleBitmap, randomRegionX, 0 * regionSize, regionSize, regionSize, matrix, false);

                    Bitmap baseBitmap = Bitmap.createBitmap(paperizedImage, c, a, 20, 20);
                    Bitmap overlapped = overlap(baseBitmap, regionBitmap);
                    Canvas canvas = new Canvas(paperizedImage);
                    canvas.drawBitmap(overlapped, c, a, null);
                }
            }
        }
        if (onlyImage) {
            Bitmap noPaperImage;
            if (paperColor != Color.WHITE) {
                noPaperImage = changeColorPaper(paperizedImage, paperColor);
            } else noPaperImage = paperizedImage;
            Bitmap.createScaledBitmap(noPaperImage, (int) (noPaperImage.getWidth() * 0.7), (int) (noPaperImage.getHeight() * 0.7), true);
            return noPaperImage;
        }
        int borderWidth = 3492;//Border images width
        int internalImageWidth = 2494;//3492/1.4, being 1.4 the factor of paper/image 38/27.1mm
        int paperWidth = borderWidth;
        int imageMargins = 998;//3492-2494, same vertical and horizontal margins
        float internalImageHeightFactor = (float) internalImageWidth / speckleWidth;
        int internalImageHeight = (int) (speckleHeight * internalImageHeightFactor);
        int paperHeight = internalImageHeight + imageMargins + topBorder.getHeight() + bottomBorder.getHeight();
        paperizedImage = Bitmap.createScaledBitmap(paperizedImage, internalImageWidth, internalImageHeight, false);
        Bitmap paperImage = Bitmap.createBitmap(paperWidth, paperHeight, inputBitmap.getConfig());
        int left = (paperWidth - internalImageWidth) / 2;
        int top = (paperHeight - internalImageHeight) / 2;
        Canvas canvas = new Canvas(paperImage);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, topBorder.getHeight(), paperImage.getWidth(), paperImage.getHeight() - bottomBorder.getHeight(), paint);

        float alpha = 0.85f; // Transparency value (0.0f - 1-0f)
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha((int) (alpha * 255)); // Converts transparency value to 0-255 range
        canvas.drawBitmap(topBorder, 0, 0, null);
        canvas.drawBitmap(paperizedImage, left, top, alphaPaint);
        canvas.drawBitmap(bottomBorder, 0, paperHeight - bottomBorder.getHeight(), null);
        if (paperColor != Color.WHITE) {
            paperImage = changeColorPaper(paperImage, paperColor);
        }
        paperImage = Bitmap.createScaledBitmap(paperImage, (int) (paperImage.getWidth() * 0.7), (int) (paperImage.getHeight() * 0.7), true);
        return paperImage;
    }

    public static Bitmap overlap(Bitmap baseBitmap, Bitmap topBitmap) {
        Bitmap resultBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                int basePixel = baseBitmap.getPixel(x, y);
                int topPixel = topBitmap.getPixel(x, y);

                // Extract Black intensity of every pixel (lowest value) In a BW image all components are equal
                int baseIntensity = Color.red(basePixel);

                int topIntensity = Color.red(topPixel);

                // Compare the intensities and pick the lowest(darkest) as a final color
                int finalIntensity = Math.min(topIntensity, baseIntensity);

                //Create the new pixel with the intensity value as rgb
                int finalPixel = Color.rgb(finalIntensity, finalIntensity, finalIntensity);

                // Place the pixel in the result image
                resultBitmap.setPixel(x, y, finalPixel);
            }
        }
        return resultBitmap;
    }

    public static Bitmap changeColorPaper(Bitmap bitmap, int paperColor) {
        //Obtain the color components of the paper color
        int adjustedRed = Color.red(paperColor);
        int adjustedGreen = Color.green(paperColor);
        int adjustedBlue = Color.blue(paperColor);

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int[] pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //Adjust RGB channels
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> 16) & 0xFF; // Extract RED component
            int green = (pixel >> 8) & 0xFF; // Extract GREEN component
            int blue = pixel & 0xFF; // Extract BLUE component

            // Adjust intensity of the rgb channels
            red = (int) (red * (adjustedRed / 255.0));
            green = (int) (green * (adjustedGreen / 255.0));
            blue = (int) (blue * (adjustedBlue / 255.0));

            // Create new pixels with adjusted channels
            pixels[i] = (pixel & 0xFF000000) | (red << 16) | (green << 8) | blue;
        }

        Bitmap modifiedImage = Bitmap.createBitmap(pixels, width, height, bitmap.getConfig());
        return modifiedImage;
    }

    public static void paperDialog(List<Integer> indexToPaperize, Context context) {

        //Do a dialog class extending DialogFragment with onDismiss for recycles
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.paperize_dialog);
        Button btnProcessPaper = dialog.findViewById(R.id.btnProcessPaper);
        Button btnSavePaper = dialog.findViewById(R.id.btnSavePaper);
        CheckBox cbOnlyImagePaper = dialog.findViewById(R.id.cbOnlyImagePaper);
        final int[] paperColor = {context.getColor(R.color.white)};
        ImageView ivPaperized = dialog.findViewById(R.id.ivPaperized);
        LinearLayout lyWhitePaperColor = dialog.findViewById(R.id.lyWhitePaperColor);
        lyWhitePaperColor.setBackgroundColor(context.getColor(R.color.teal_200));
        LinearLayout lyYellowPaperColor = dialog.findViewById(R.id.lyYellowPaperColor);
        LinearLayout lyBluePaperColor = dialog.findViewById(R.id.lyBluePaperColor);
        LinearLayout lyGreenPaperColor = dialog.findViewById(R.id.lyGreenPaperColor);
        LinearLayout lyPinkPaperColor = dialog.findViewById(R.id.lyPinkPaperColor);
        LinearLayout lyPurplePaperColor = dialog.findViewById(R.id.lyPurplePaperColor);
        LinearLayout lyCustomPaperColor = dialog.findViewById(R.id.lyCustomPaperColor);

        final boolean[] isCustomSelected = {false};
        ImageView ivCustomPaperColor = dialog.findViewById(R.id.ivCustomPaperColor);
        ivCustomPaperColor.setBackgroundColor(customColorPaper);

        Map<LinearLayout, Integer> colorMap = new HashMap<>();
        colorMap.put(lyWhitePaperColor, context.getColor(R.color.white));
        colorMap.put(lyYellowPaperColor, context.getColor(R.color.paper_color_yellow));
        colorMap.put(lyBluePaperColor, context.getColor(R.color.paper_color_blue));
        colorMap.put(lyGreenPaperColor, context.getColor(R.color.paper_color_green));
        colorMap.put(lyPinkPaperColor, context.getColor(R.color.paper_color_pink));
        colorMap.put(lyPurplePaperColor, context.getColor(R.color.paper_color_purple));
        colorMap.put(lyCustomPaperColor, customColorPaper);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == lyCustomPaperColor || v == ivCustomPaperColor) {
                    if (isCustomSelected[0]) {
                        ColorPickerDialogBuilder
                                .with(context)
                                .setTitle(context.getString(R.string.choose_color))
                                .initialColor(colorMap.get(lyCustomPaperColor))
                                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                                .density(12)
                                .showAlphaSlider(false)
                                .setOnColorSelectedListener(new OnColorSelectedListener() {
                                    @Override
                                    public void onColorSelected(int selectedColor) {
                                        Utils.toast(context, context.getString(R.string.selected_color) + Integer.toHexString(selectedColor).substring(2).toUpperCase());
                                    }
                                })
                                .setPositiveButton("ok", new ColorPickerClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                        ivCustomPaperColor.setBackgroundColor(selectedColor);
                                        paperColor[0] = selectedColor;
                                        customColorPaper = selectedColor;
                                        colorMap.put(lyCustomPaperColor, selectedColor);
                                        editor.putInt("custom_paper_color", selectedColor);
                                        editor.apply();
                                    }
                                })
                                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .build()
                                .show();
                    } else isCustomSelected[0] = true;
                } else isCustomSelected[0] = false;

                //Reset the layout color to WHITE
                lyWhitePaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyYellowPaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyBluePaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyGreenPaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyPinkPaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyPurplePaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                lyCustomPaperColor.setBackgroundColor(context.getColor(android.R.color.white));
                //Set the clicked layout bg
                v.setBackgroundColor(context.getColor(R.color.teal_200));
                paperColor[0] = colorMap.get(v);
            }
        };

        // Assign OnClickListener to every LinearLayout
        lyWhitePaperColor.setOnClickListener(onClickListener);
        lyYellowPaperColor.setOnClickListener(onClickListener);
        lyBluePaperColor.setOnClickListener(onClickListener);
        lyGreenPaperColor.setOnClickListener(onClickListener);
        lyPinkPaperColor.setOnClickListener(onClickListener);
        lyPurplePaperColor.setOnClickListener(onClickListener);
        lyCustomPaperColor.setOnClickListener(onClickListener);

        List<Integer> gbcImagesList = new ArrayList<>();
        List<Bitmap> paperizedBitmaps = new ArrayList<>();

        btnProcessPaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSavePaper.setEnabled(true);
                paperizedBitmaps.clear();
                if (!loadingDialog.isShowing()) {
                    loadingDialog.show();
                }
                new PaperizeAsyncTask(indexToPaperize, paperColor[0], paperizedBitmaps, ivPaperized, cbOnlyImagePaper.isChecked(), context).execute();
            }
        });
        btnSavePaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDateTime now = null;
                Date nowDate = new Date();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    now = LocalDateTime.now();
                }
                String date = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                    date = dtf.format(now);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                    date = sdf.format(nowDate);
                }
                int index = 1;
                for (Bitmap paperized : paperizedBitmaps) {
                    File file = null;
                    if (gbcImagesList.size() > 1)
                        file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + "_" + (index) + ".png");
                    else {
                        file = new File(Utils.IMAGES_FOLDER, "paperized_" + date + ".png");
                    }

                    if (paperized.getHeight() == 144 && paperized.getWidth() == 160 && crop) {
                        paperized = Bitmap.createBitmap(paperized, 16, 16, 128, 112);
                    }
                    //If make square selected in settings
                    if (exportSquare) {
                        paperized = makeSquareImage(paperized);
                    }
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        paperized.compress(Bitmap.CompressFormat.PNG, 100, out);
                        mediaScanner(file, context);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    index++;
                }
                Utils.toast(context, context.getString(R.string.saved_paperized_toast));

            }
        });

        dialog.show();
    }

}

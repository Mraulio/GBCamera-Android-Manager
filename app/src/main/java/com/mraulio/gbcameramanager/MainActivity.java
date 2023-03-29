package com.mraulio.gbcameramanager;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.mraulio.gbcameramanager.databinding.ActivityMainBinding;
import com.mraulio.gbcameramanager.model.GbcFrame;
import com.mraulio.gbcameramanager.model.GbcPalette;
import com.mraulio.gbcameramanager.ui.gallery.GalleryFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPalettes();//Before loading gallery fragment
        addFrames();
        Methods.extractHexImages();
        Methods.extractSavImages(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_gallery, R.id.nav_settings, R.id.nav_palettes)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        /**
         * I ask for storage permissions
         */
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
    }

    private void addFrames() {
        int width = 160;
        int height = 144;
        int[] pixels = new int[width * height];

        //Nintendo frame from drawable-nodpi resource (so it is not automatically scaled to the dpi)
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.nintendo_frame);
        GbcFrame nintendoframe = new GbcFrame();
        nintendoframe.setFrameName("Nintendo Frame");
        nintendoframe.setFrameBitmap(bitmap);
        Methods.framesList.add(nintendoframe);

        Arrays.fill(pixels, Color.BLACK);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame blackFrame = new GbcFrame();
        blackFrame.setFrameName("Black Frame");
        blackFrame.setFrameBitmap(bitmap);
        Methods.framesList.add(blackFrame);

        //White frame
        Arrays.fill(pixels, Color.WHITE);
        bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        GbcFrame blueFrame = new GbcFrame();
        blueFrame.setFrameName("White frame");
        blueFrame.setFrameBitmap(bitmap);
        Methods.framesList.add(blueFrame);
    }

    private void addPalettes() {
        //Palette GAMEBOY_LCD_PALETTE
        int[] GAMEBOY_LCD_PALETTE = {
                Color.rgb(155, 188, 15),
                Color.rgb(139, 172, 15),
                Color.rgb(48, 98, 48),
                Color.rgb(15, 56, 15)
        };
        int[] EVEN_DIST_PALETTE = {
                Color.rgb(255, 255, 255),
                Color.rgb(170, 170, 170),
                Color.rgb(85, 85, 85),
                Color.rgb(0, 0, 0)
        };
        GbcPalette gbcPalette1 = new GbcPalette();
        gbcPalette1.setPaletteColors(EVEN_DIST_PALETTE);
        gbcPalette1.setName("Greyscale");
        Methods.gbcPalettesList.add(gbcPalette1);
        GbcPalette gbcPalette2 = new GbcPalette();
        gbcPalette2.setPaletteColors(GAMEBOY_LCD_PALETTE);
        gbcPalette2.setName("DMG");
        Methods.gbcPalettesList.add(gbcPalette2);

        //Adding palettes from here https://www.npmjs.com/package/gb-palettes
        int[] cmyk_palette = {
                Color.parseColor("#ffff00"),
                Color.parseColor("#0be8fd"),
                Color.parseColor("#fb00fa"),
                Color.parseColor("#373737")
        };
        GbcPalette gbcPalette3 = new GbcPalette();
        gbcPalette3.setPaletteColors(cmyk_palette);
        gbcPalette3.setName("CMYK");
        Methods.gbcPalettesList.add(gbcPalette3);

        int[] tram_palette = {
                Color.parseColor("#f3c677"),
                Color.parseColor("#e64a4e"),
                Color.parseColor("#912978"),
                Color.parseColor("#0c0a3e")
        };
        GbcPalette gbcPalette5 = new GbcPalette();
        gbcPalette5.setPaletteColors(tram_palette);
        gbcPalette5.setName("Tramonto al Parco");
        Methods.gbcPalettesList.add(gbcPalette5);
        //My won palettes
        int[] cute_palette = {
                Color.parseColor("#ffc36d"),
                Color.parseColor("#fe6f9b"),
                Color.parseColor("#c64ab3"),
                Color.parseColor("#7b50b9")
        };
        GbcPalette gbcPalette4 = new GbcPalette();
        gbcPalette4.setPaletteColors(cute_palette);
        gbcPalette4.setName("Cute");
        Methods.gbcPalettesList.add(gbcPalette4);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            System.out.println("PERMISION GRANTED*********************");            //resume tasks needing this permission
            Toast toast = Toast.makeText(this, "Permisos otorgados", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
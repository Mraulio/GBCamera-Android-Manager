package com.mraulio.gbcameramanager;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.mraulio.gbcameramanager.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static int printIndex = 0;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    public static boolean pressBack = true;
    public static int exportSize = 4;
    public static int imagesPage = 12;
    public static UsbManager manager;

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private UsbDevice device;
    private UsbManager usbManager;
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                        }
                    } else {
                        Log.d("TAG", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    SerialInputOutputManager usbIoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        if (Methods.gbcPalettesList.size() == 0) {
            StartCreation.addPalettes();//Before loading gallery fragment
        }

        if (Methods.framesList.size() == 0) {
            StartCreation.addFrames(this.getBaseContext());

        }

        if (Methods.gbcImagesList.size() == 0) {
            Methods.extractSavImages(this);
        }
//        Methods.extractHexImages();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            System.out.println("PERMISION GRANTED*********************");            //resume tasks needing this permission
            Toast toast = Toast.makeText(this, "Granted permissions.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    //This makes the app not get closed when pressing back
//    @Override
//    public void onBackPressed() {
//        if (pressBack) {
//            super.onBackPressed();
//            //additional code
//            moveTaskToBack(true);
//        }
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
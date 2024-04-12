package com.mraulio.gbcameramanager.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;

public class LoadingDialog {
    private TextView textView;
    private AlertDialog dialog;
    private Context context;
    private String text;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public LoadingDialog(Context context, String text) {
        this.context = context;
        this.text = text;
    }

    public void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(context);
        layout.addView(progressBar);

        textView = new TextView(context);
        textView.setTextSize(20);
        textView.setPadding(0, 20, 0, 0);
        textView.setTextColor(context.getResources().getColor(R.color.imageview_bg));

        if (text != null) {
            textView.setText(text);
        }
        textView.setGravity(Gravity.CENTER);
        layout.addView(textView);

        builder.setView(layout);
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void showDialog() {
        if (dialog != null) {
            dialog.show();
        } else {
            createDialog();
            dialog.show();
        }
    }


    public void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void setLoadingDialogText(final String newText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (textView != null) {
                            textView.setText(newText);
                        }
                    }
                });
            }
        }).start();
    }


    public boolean isShowing() {
        if (dialog != null && dialog.isShowing()) {
            return true;
        } else return false;
    }
}



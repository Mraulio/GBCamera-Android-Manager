package com.mraulio.gbcameramanager.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mraulio.gbcameramanager.R;

public class LoadingDialog {
    TextView textView;
    Context context;
    String text;


    public LoadingDialog(Context context, String text) {
        this.context = context;
        this.text = text;
    }

    public AlertDialog showDialog() {
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

        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    public void setLoadingDialogText(String newText) {

        if (textView != null) {
            textView.setText(newText);
        }

    }
}

package com.mraulio.gbcameramanager.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.mraulio.gbcameramanager.R;
import com.mraulio.gbcameramanager.ui.gallery.OnThumbMoveListener;

import java.util.ArrayList;
import java.util.List;

public class FourThumbSeekBar extends View {

    private Paint paint;
    private Paint thumbPaint;
    private Paint textPaint;
    private List<RectF> thumbs;
    private float thumbRadius = 30;
    private int[] thumbValues = {0, 85, 170, 255};
    private int selectedThumbIndex = -1;
    private float maxThumbValue = 255;
    private float minThumbValue = 0;
    private float minGap = 1;
    private OnThumbMoveListener onThumbMoveListener;
    private PopupWindow popupWindow;
    private TextView popupTextView;

    public FourThumbSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Initialize Paints
        paint = new Paint();
        thumbPaint = new Paint();
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Reed custom attr
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FourThumbSeekBar, 0, 0);
        try {
            int lineColor = a.getColor(R.styleable.FourThumbSeekBar_lineColor, Color.BLUE);
            int thumbColor = a.getColor(R.styleable.FourThumbSeekBar_thumbColor, Color.BLUE);
            float lineThickness = a.getDimension(R.styleable.FourThumbSeekBar_lineThickness, 2);

            paint.setColor(lineColor);
            paint.setStrokeWidth(lineThickness);
            thumbPaint.setColor(thumbColor);
        } finally {
            a.recycle();
        }

        thumbs = new ArrayList<>();
        for (int i = 0; i < thumbValues.length; i++) {
            thumbs.add(new RectF());
        }

        // Initialize PopupWindow
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.thumb_popup, null);
        popupTextView = popupView.findViewById(R.id.popup_text);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate initial thum position
        for (int i = 0; i < thumbValues.length; i++) {
            float thumbX = getWidth() * thumbValues[i] / maxThumbValue;
            thumbs.get(i).set(thumbX - thumbRadius, 0, thumbX + thumbRadius, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the line with a gradient from black
        for (int i = 0; i < thumbs.size(); i++) {
            RectF thumb = thumbs.get(i);

            // Define the gradient
            int[] colors = {Color.BLACK, thumbPaint.getColor()};
            float[] positions = {0.0f, 1.0f};
            LinearGradient gradient = new LinearGradient(0, getHeight() / 2, getWidth(), getHeight() / 2, colors, positions, Shader.TileMode.CLAMP);

            // Apply the gradient to the Paint
            paint.setShader(gradient);

            // Draw the line
            canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, paint);

            // Restore the Paint
            paint.setShader(null);
        }

        // Draw the thumbs and numbers
        for (int i = 0; i < thumbs.size(); i++) {
            RectF thumb = thumbs.get(i);
            canvas.drawRoundRect(thumb, thumbRadius, thumbRadius, thumbPaint);
            canvas.drawText(String.valueOf(i + 1), thumb.centerX(), thumb.centerY() + textPaint.getTextSize() / 3, textPaint);
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float downX = event.getX();
                for (int i = 0; i < thumbs.size(); i++) {
                    if (Math.abs(thumbs.get(i).centerX() - downX) <= thumbRadius) {
                        selectedThumbIndex = i;

                        // Show popup
                        if (popupWindow != null && !popupWindow.isShowing()) {
                            updatePopup();
                            popupWindow.showAsDropDown(this, (int) downX, -getHeight());
                        }
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (selectedThumbIndex != -1) {
                    float moveX = event.getX();
                    RectF selectedThumb = thumbs.get(selectedThumbIndex);

                    // Limit the movement between the borders
                    float leftLimit = selectedThumbIndex == 0 ? minThumbValue : thumbValues[selectedThumbIndex - 1] + minGap;
                    float rightLimit = selectedThumbIndex == thumbs.size() - 1 ? maxThumbValue : thumbValues[selectedThumbIndex + 1] - minGap;

                    moveX = Math.max(moveX, getWidth() * leftLimit / maxThumbValue);
                    moveX = Math.min(moveX, getWidth() * rightLimit / maxThumbValue);

                    // Update the thumb position and its value
                    selectedThumb.offsetTo(moveX - thumbRadius, selectedThumb.top);
                    thumbValues[selectedThumbIndex] = Math.round(maxThumbValue * moveX / getWidth());

                    // Update the popup
                    if (popupWindow != null && popupWindow.isShowing()) {
                        updatePopup();
                    }

                    // Notify the listener
                    if (onThumbMoveListener != null) {
                        onThumbMoveListener.onThumbMove(thumbValues);
                    }

                    invalidate(); // Draw the view again
                    // Eliminate any pending Runnable execution
                    handler.removeCallbacks(dismissPopupRunnable);
                    // Close the Popup after 2 secs
                    handler.postDelayed(dismissPopupRunnable, 2000);
                }
                break;
            case MotionEvent.ACTION_UP:
                selectedThumbIndex = -1;

                // hide the popup
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
                break;
        }
        return true;
    }

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable dismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
        }
    };

    public void resetThumbs() {
        thumbValues = new int[]{0, 85, 170, 255};
        for (int i = 0; i < thumbValues.length; i++) {
            float thumbX = getWidth() * thumbValues[i] / maxThumbValue;
            thumbs.get(i).set(thumbX - thumbRadius, 0, thumbX + thumbRadius, getHeight());
        }
        invalidate(); // Redraw the view
    }

    private void updatePopup() {
        if (selectedThumbIndex != -1 && popupTextView != null) {
            String popupText = (selectedThumbIndex + 1) + ": " + thumbValues[selectedThumbIndex];
            popupTextView.setText(popupText);

            // Update the popup position
            float thumbX = thumbs.get(selectedThumbIndex).centerX();
            int xOffset = (int) thumbX - popupWindow.getContentView().getWidth() / 2;
            int yOffset = -getHeight() - 120;
            popupWindow.update(this, xOffset, yOffset, -1, -1);
        }
    }

    /**
     * Method to stablish the listener
     */
    public void setOnThumbMoveListener(OnThumbMoveListener listener) {
        this.onThumbMoveListener = listener;
    }

    /**
     * Method to get the current thumbs values
     * @return
     */
    public int[] getThumbValues() {
        return thumbValues;
    }
}

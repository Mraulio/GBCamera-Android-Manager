package com.mraulio.gbcameramanager.ui.gallery;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.android.material.datepicker.DayViewDecorator;
import com.mraulio.gbcameramanager.R;

import java.util.List;

/**
 * Based on this https://github.com/material-components/material-components-android/blob/master/catalog/java/io/material/catalog/datepicker/CircleIndicatorDecorator.java
 */
class CalendarIndicator extends DayViewDecorator {

    private Context context;
    private final List<Calendar> indicatorDays;
    private final Drawable dotDrawable;

    public CalendarIndicator(Context context, List<Calendar> indicatorDays) {
        this.context = context;
        this.indicatorDays = indicatorDays;
        dotDrawable = createDotDrawable();
    }

    @Override
    public Drawable getCompoundDrawableBottom(
            @NonNull Context context, int year, int month, int day, boolean valid, boolean selected) {
        if (!showIndicatorDot(year, month, day)) {
            return null;
        }
        return createDotDrawable();
    }

    private Drawable createDotDrawable() {
        int dotColor = context.getColor(R.color.star_color);
        int dotRadius = 15;
        int dotMarginBottom = 7;
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(dotColor);
        dotDrawable.setCornerRadius(dotRadius);

        InsetDrawable insetDrawable = new InsetDrawable(dotDrawable, 0, 0, 0, dotMarginBottom);
        insetDrawable.setBounds(0, 0, dotRadius, dotRadius + dotMarginBottom);
        return insetDrawable;
    }

    private boolean showIndicatorDot(int year, int month, int day) {
        for (Calendar calendar : indicatorDays) {
            if (calendar.get(Calendar.YEAR) == year
                    && calendar.get(Calendar.MONTH) == month
                    && calendar.get(Calendar.DAY_OF_MONTH) == day) {
                return true;
            }
        }
        return false;
    }

    public static final Parcelable.Creator<CalendarIndicator> CREATOR =
            new Parcelable.Creator<CalendarIndicator>() {
                @NonNull
                @Override
                public CalendarIndicator createFromParcel(@NonNull Parcel source) {
                    return new CalendarIndicator(null, null);
                }

                @NonNull
                @Override
                public CalendarIndicator[] newArray(int size) {
                    return new CalendarIndicator[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
    }
}


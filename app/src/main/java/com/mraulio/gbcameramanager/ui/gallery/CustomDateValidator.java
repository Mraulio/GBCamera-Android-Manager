package com.mraulio.gbcameramanager.ui.gallery;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.material.datepicker.CalendarConstraints;

import java.util.Calendar;
import java.util.List;

public class CustomDateValidator implements CalendarConstraints.DateValidator {

    private final List<Calendar> validDates;

    public CustomDateValidator(List<Calendar> validDates) {
        this.validDates = validDates;
    }

    @Override
    public boolean isValid(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (Calendar validDate : validDates) {
            if (calendar.get(Calendar.YEAR) == validDate.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == validDate.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == validDate.get(Calendar.DAY_OF_MONTH)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Parcelable.Creator<CustomDateValidator> CREATOR =
            new Parcelable.Creator<CustomDateValidator>() {

                @Override
                public CustomDateValidator createFromParcel(Parcel source) {
                    return null;
                }

                @Override
                public CustomDateValidator[] newArray(int size) {
                    return new CustomDateValidator[size];
                }
            };
}

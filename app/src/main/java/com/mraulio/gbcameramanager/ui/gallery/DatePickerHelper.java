package com.mraulio.gbcameramanager.ui.gallery;

import static com.mraulio.gbcameramanager.MainActivity.dateEndFilter;
import static com.mraulio.gbcameramanager.MainActivity.dateLocale;
import static com.mraulio.gbcameramanager.MainActivity.dateStartFilter;
import static com.mraulio.gbcameramanager.MainActivity.filterMonth;
import static com.mraulio.gbcameramanager.MainActivity.filterRange;
import static com.mraulio.gbcameramanager.MainActivity.filterYear;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Switch;
import android.widget.TextView;

import com.mraulio.gbcameramanager.MainActivity;
import com.mraulio.gbcameramanager.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatePickerHelper {

    public static void showDatePickerDialog(Context context, Button btnFinalDate) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.date_filter, null);

        Button btnSaveDate = dialogView.findViewById(R.id.btn_save_dates);
        Button btnDateStart = dialogView.findViewById(R.id.btn_date_start);
        Button btnDateEnd = dialogView.findViewById(R.id.btn_date_end);
        Switch swMonth = dialogView.findViewById(R.id.sw_date_month);
        Switch swYear = dialogView.findViewById(R.id.sw_date_year);
        Switch swRange = dialogView.findViewById(R.id.sw_date_range);
        Date dateStart = new Date(dateStartFilter);
        Date dateEnd = new Date(dateEndFilter);

        swMonth.setChecked(filterMonth);
        swYear.setChecked(filterYear);
        swRange.setChecked(filterRange);

        String loc;
        if (dateLocale.equals("yyyy-MM-dd")) {
            loc = "dd/MM/yyyy";
        } else {
            loc = "MM/dd/yyyy";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(loc, Locale.getDefault());

        btnDateStart.setText(dateFormat.format(dateStartFilter));
        btnDateEnd.setText(dateFormat.format(dateEndFilter));
        btnFinalDate.setText(buildDateString(filterMonth, filterYear, filterRange, dateStartFilter, dateEndFilter));

        swMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swMonth.isChecked()) {
                    swYear.setChecked(false);
                }
            }
        });
        swYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swYear.isChecked()) {
                    swMonth.setChecked(false);
                }
            }
        });
        swRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check if date2 > date1
                if (dateStart.getTime() > dateEnd.getTime()) {
                    btnSaveDate.setEnabled(false);
                    btnDateStart.setTextColor(context.getColor(R.color.duplicated));
                } else {
                    btnSaveDate.setEnabled(true);
                    btnDateEnd.setTextColor(context.getColor(R.color.save_color));
                }
            }
        });
        Dialog dialog = new Dialog(context);
        dialog.setContentView(dialogView);


        btnDateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(context, btnDateStart, dateStart, null, false, null);
            }
        });
        btnDateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(context, btnDateEnd, dateEnd, dateStart, swRange.isChecked(), btnSaveDate);
            }
        });

        btnSaveDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();

                filterMonth = swMonth.isChecked();
                filterYear = swYear.isChecked();
                filterRange = swRange.isChecked();
                dateStartFilter = dateStart.getTime();
                dateEndFilter = dateEnd.getTime();

                editor.putBoolean("date_filter_month", filterMonth);
                editor.putBoolean("date_filter_year", filterYear);
                editor.putBoolean("date_filter_range", filterRange);
                editor.putLong("date_start_filter", dateStartFilter);
                editor.putLong("date_end_filter", dateEndFilter);
                editor.apply();

                //Build the date
                btnFinalDate.setText(buildDateString(filterMonth, filterYear, filterRange, dateStartFilter, dateEndFilter));

                dialog.dismiss();
            }
        });
        Window window = dialog.getWindow();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Activity activity = (Activity) context;
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int desiredWidth = (int) (screenWidth * 0.8);
        window.setLayout(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    public static String buildDateString(boolean month, boolean year, boolean range, long dateStart, long dateEnd) {
        String loc;
        if (dateLocale.equals("yyyy-MM-dd")) {
            loc = "dd/MM/yyyy";
        } else {
            loc = "MM/dd/yyyy";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(loc, Locale.getDefault());
        if (!month && !year && !range) {
            return dateFormat.format(dateStart);
        }

        String dateStartString = dateFormat.format(dateStart);
        String dateEndString = dateFormat.format(dateEnd);

        if (month) {
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            dateStartString = monthFormat.format(dateStart);
            dateEndString = monthFormat.format(dateEnd);
        }

        if (year) {
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

            dateStartString = yearFormat.format(dateStart);
            dateEndString = yearFormat.format(dateEnd);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateStartString);

        if (range) {
            stringBuilder.append(" - ");
            stringBuilder.append(dateEndString);
        }

        return stringBuilder.toString();
    }

    public static void showDatePicker(Context context, Button btnDate, Date date, Date dateStart, boolean isEndDate, Button saveButton) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, monthOfYear);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        Date selectedDate = selectedCalendar.getTime();
                        String loc;
                        if (dateLocale.equals("yyyy-MM-dd")) {
                            loc = "dd/MM/yyyy";
                        } else {
                            loc = "MM/dd/yyyy";
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat(loc, Locale.getDefault());
                        String formattedDate = dateFormat.format(selectedDate);

                        btnDate.setText(formattedDate);

                        date.setTime(selectedDate.getTime());

                        if (isEndDate) {
                            //Check if date2 > date1
                            if (dateStart.getTime() > date.getTime()) {
                                saveButton.setEnabled(false);
                                btnDate.setTextColor(context.getColor(R.color.duplicated));
                            } else {
                                saveButton.setEnabled(true);
                                btnDate.setTextColor(context.getColor(R.color.save_color));
                            }
                        }
                    }
                }, year, month, dayOfMonth);

        if (isEndDate) {
            datePickerDialog.setTitle(context.getString(R.string.end_date_datepicker));
        } else {
            datePickerDialog.setTitle(context.getString(R.string.start_date_datepicker));
        }
        datePickerDialog.show();
    }
}


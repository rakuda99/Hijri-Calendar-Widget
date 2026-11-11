package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MonthViewFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private int mAppWidgetId;
    private List<CalendarDay> mDayItems = new ArrayList<>();
    private int mMonthOffset;
    private int mOrientation;

    public MonthViewFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        for (int i = 0; i < 42; i++)
            mDayItems.add(new CalendarDay());
    }

    @Override
    public void onDataSetChanged() {
        try {
            SharedPreferences mainPrefs = mContext.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
            mOrientation = mainPrefs.getInt("month_orientation", 0);
            int weekStart = mainPrefs.getInt("week_start", 0);

            SharedPreferences widgetPrefs = mContext.getSharedPreferences("MonthWidgetPrefs", Context.MODE_PRIVATE);
            mMonthOffset = widgetPrefs.getInt("month_offset_" + mAppWidgetId, 0);

            int hCorrection = mainPrefs.getInt("hijri_correction", 0);
            calculateMonthGrid(weekStart, hCorrection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateMonthGrid(int weekStart, int hCorrection) {
        List<CalendarDay> newList = new ArrayList<>(42);
        try {
            long adjustedTime = HijriDateUtils.getAdjustedTime(mContext);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(adjustedTime);
            cal.add(Calendar.MONTH, mMonthOffset);
            cal.set(Calendar.DAY_OF_MONTH, 1);

            int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday

            if (weekStart == 1) { // Monday
                int diff = startDayOfWeek - Calendar.MONDAY;
                if (diff < 0)
                    diff += 7;
                cal.add(Calendar.DAY_OF_MONTH, -diff);
            } else if (weekStart == 2) { // Saturday
                int diff = startDayOfWeek - Calendar.SATURDAY;
                if (diff < 0)
                    diff += 7;
                cal.add(Calendar.DAY_OF_MONTH, -diff);
            } else { // Sunday
                cal.add(Calendar.DAY_OF_MONTH, -(startDayOfWeek - 1));
            }

            for (int i = 0; i < 42; i++) {
                CalendarDay day = new CalendarDay();
                day.year = cal.get(Calendar.YEAR);
                day.month = cal.get(Calendar.MONTH);
                day.day = cal.get(Calendar.DAY_OF_MONTH);

                int[] hijri = HijriDateUtils.getHijriDateWithAdjustment(day.year, day.month + 1, day.day, hCorrection);
                day.hijriDay = hijri[2];
                day.hijriMonth = hijri[1];

                long nowMillis = HijriDateUtils.getAdjustedTime(mContext);
                Calendar today = Calendar.getInstance();
                today.setTimeInMillis(nowMillis);
                day.isToday = (day.year == today.get(Calendar.YEAR) && day.month == today.get(Calendar.MONTH)
                        && day.day == today.get(Calendar.DAY_OF_MONTH));

                newList.add(day);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Permute for Vertical
            if (mOrientation == 1) {
                List<CalendarDay> verticalList = new ArrayList<>(42);
                for (int d = 0; d < 7; d++) {
                    for (int w = 0; w < 6; w++) {
                        int srcIdx = (w * 7) + d;
                        if (srcIdx < newList.size())
                            verticalList.add(newList.get(srcIdx));
                    }
                }
                mDayItems = verticalList;
            } else {
                mDayItems = newList;
            }
        } catch (Exception e) {
            mDayItems = newList;
        }
    }

    @Override
    public int getCount() {
        return mDayItems.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= mDayItems.size())
            return null;
        CalendarDay day = mDayItems.get(position);
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.item_calendar_day);

        SharedPreferences prefs = mContext.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
        int gregColor = prefs.getInt("month_greg_color", Color.WHITE);
        int hijriColor = prefs.getInt("month_hijri_color", 0xAAFFFFFF);
        int digitStyle = prefs.getInt("digit_style", 0);
        int sizeIdx = prefs.getInt("month_day_size_index", 1);
        boolean isBold = prefs.getBoolean("month_day_bold", false);
        boolean isHollow = prefs.getBoolean("month_hollow_circle", false);

        float[] gregSizes = { 10, 12, 14, 16, 18 };
        float[] hijriSizes = { 10, 12, 14, 16, 18 };
        float gSize = gregSizes[sizeIdx % gregSizes.length];
        float hSize = hijriSizes[sizeIdx % hijriSizes.length];

        String gStr = HijriDateUtils.localizeDigits(String.valueOf(day.day), digitStyle);
        String hStr = HijriDateUtils.localizeDigits(String.valueOf(day.hijriDay), digitStyle);

        if (isBold) {
            rv.setTextViewText(R.id.day_text_greg, android.text.Html.fromHtml("<b>" + gStr + "</b>"));
            rv.setTextViewText(R.id.day_text_hijri, android.text.Html.fromHtml("<b>" + hStr + "</b>"));
        } else {
            rv.setTextViewText(R.id.day_text_greg, gStr);
            rv.setTextViewText(R.id.day_text_hijri, hStr);
        }

        rv.setTextColor(R.id.day_text_greg, gregColor);
        rv.setTextColor(R.id.day_text_hijri, hijriColor);

        rv.setTextViewTextSize(R.id.day_text_greg, android.util.TypedValue.COMPLEX_UNIT_SP, gSize);
        rv.setTextViewTextSize(R.id.day_text_hijri, android.util.TypedValue.COMPLEX_UNIT_SP, hSize);

        if (day.isToday) {
            rv.setImageViewResource(R.id.day_circle_bg, isHollow ? R.drawable.circle_hollow : R.drawable.circle_bg);
            rv.setViewVisibility(R.id.day_circle_bg, android.view.View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.day_circle_bg, android.view.View.GONE);
        }

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("year", day.year);
        fillInIntent.putExtra("month", day.month);
        fillInIntent.putExtra("day", day.day);
        rv.setOnClickFillInIntent(R.id.day_item_root, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDestroy() {
        mDayItems.clear();
    }

    private static class CalendarDay {
        int year, month, day, hijriDay, hijriMonth;
        boolean isToday;
    }
}

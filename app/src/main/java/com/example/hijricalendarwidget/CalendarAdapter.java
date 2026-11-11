package com.example.hijricalendarwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {

    private Context mContext;
    private List<CalendarDay> mDayItems = new ArrayList<>();
    private int mMonthOffset;
    private int mStartOfWeek; // 0 = Sunday, 1 = Monday
    private Set<String> mEventDays = new HashSet<>();

    public CalendarAdapter(Context context) {
        mContext = context;
        updatePreferences();
    }

    public void setMonthOffset(int offset) {
        mMonthOffset = offset;
        calculateMonthGrid();
        notifyDataSetChanged();
    }

    public void updatePreferences() {
        SharedPreferences prefs = mContext.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
        mStartOfWeek = prefs.getInt("week_start", 0);
        calculateMonthGrid();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDayItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mDayItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_calendar_day, parent, false);
        }

        CalendarDay day = mDayItems.get(position);

        TextView textGreg = convertView.findViewById(R.id.day_text_greg);
        TextView textHijri = convertView.findViewById(R.id.day_text_hijri);
        ImageView circleBg = convertView.findViewById(R.id.day_circle_bg);
        ImageView eventDot = convertView.findViewById(R.id.event_dot);

        // Load Preferences for styling
        SharedPreferences prefs = mContext.getSharedPreferences("HijriWidgetPrefs", Context.MODE_PRIVATE);
        boolean showHijri = prefs.getBoolean("month_show_hijri", true);
        boolean showGreg = prefs.getBoolean("month_show_greg", true);
        int gregColor = prefs.getInt("month_greg_color", Color.WHITE);
        int hijriColor = prefs.getInt("month_hijri_color", 0x88FFFFFF);
        int dayCircleColor = prefs.getInt("month_today_color", 0xFF0000FF);
        int eventDotColor = prefs.getInt("month_event_color", Color.GREEN);

        int sizeIndex = prefs.getInt("month_day_size_index", 1);
        boolean isBold = prefs.getBoolean("month_day_bold", false);
        boolean isHollow = prefs.getBoolean("month_hollow_circle", false);

        // Styling
        float sizeSP_Greg = 14f;
        float sizeSP_Hijri = 14f;
        switch (sizeIndex) {
            case 0:
                sizeSP_Greg = 10f;
                sizeSP_Hijri = 10f;
                break;
            case 1:
                sizeSP_Greg = 14f;
                sizeSP_Hijri = 14f;
                break;
            case 2:
                sizeSP_Greg = 18f;
                sizeSP_Hijri = 18f;
                break;
            case 3:
                sizeSP_Greg = 22f;
                sizeSP_Hijri = 22f;
                break;
            case 4:
                sizeSP_Greg = 26f;
                sizeSP_Hijri = 26f;
                break;
        }

        textGreg.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSP_Greg);
        textHijri.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSP_Hijri);

        if (isBold) {
            textGreg.setTypeface(null, android.graphics.Typeface.BOLD);
            textHijri.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            textGreg.setTypeface(null, android.graphics.Typeface.NORMAL);
            textHijri.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        int digitStyle = prefs.getInt("digit_style", 0);
        textGreg.setText(HijriDateUtils.localizeDigits(String.valueOf(day.day), digitStyle));
        textHijri.setText(HijriDateUtils.localizeDigits(String.valueOf(day.hijriDay), digitStyle));

        // Today Circle
        if (day.isToday) {
            circleBg.setVisibility(View.VISIBLE);
            if (isHollow) {
                circleBg.setImageResource(R.drawable.circle_hollow);
            } else {
                circleBg.setImageResource(R.drawable.circle_bg);
            }
            circleBg.setColorFilter(dayCircleColor);
        } else {
            circleBg.setVisibility(View.GONE);
        }

        // Visibility
        textGreg.setVisibility(showGreg ? View.VISIBLE : View.GONE);
        textHijri.setVisibility(showHijri ? View.VISIBLE : View.GONE);

        // Month Alpha
        long adjustedTime = HijriDateUtils.getAdjustedTime(mContext);
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(adjustedTime);
        targetCal.add(Calendar.MONTH, mMonthOffset);
        int targetMonth = targetCal.get(Calendar.MONTH);

        if (day.month != targetMonth) {
            int alpha = 100;
            textGreg.setTextColor(
                    Color.argb(alpha, Color.red(gregColor), Color.green(gregColor), Color.blue(gregColor)));
            textHijri.setTextColor(
                    Color.argb(alpha, Color.red(hijriColor), Color.green(hijriColor), Color.blue(hijriColor)));
        } else {
            textGreg.setTextColor(gregColor);
            textHijri.setTextColor(hijriColor);
        }

        // Event Dot (Placeholder logic from factory)
        eventDot.setVisibility(day.hasEvent ? View.VISIBLE : View.GONE);
        eventDot.setColorFilter(eventDotColor);

        return convertView;
    }

    private void calculateMonthGrid() {
        mDayItems.clear();
        long adjustedTime = HijriDateUtils.getAdjustedTime(mContext);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(adjustedTime);
        cal.add(Calendar.MONTH, mMonthOffset);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday

        if (mStartOfWeek == 1) { // Monday
            int diff = startDayOfWeek - Calendar.MONDAY;
            if (diff < 0) {
                diff += 7;
            }
            cal.add(Calendar.DAY_OF_MONTH, -diff);
        } else if (mStartOfWeek == 2) { // Saturday
            int diff = startDayOfWeek - Calendar.SATURDAY;
            if (diff < 0) {
                diff += 7;
            }
            cal.add(Calendar.DAY_OF_MONTH, -diff);
        } else { // Sunday (Default)
            cal.add(Calendar.DAY_OF_MONTH, -(startDayOfWeek - 1));
        }

        // Calculate range for query
        long startMillis = cal.getTimeInMillis();
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_MONTH, 43);
        long endMillis = endCal.getTimeInMillis();

        Set<String> eventDays = getEventDaysSimple(startMillis, endMillis);

        for (int i = 0; i < 42; i++) {
            CalendarDay dayItem = new CalendarDay();
            dayItem.year = cal.get(Calendar.YEAR);
            dayItem.month = cal.get(Calendar.MONTH);
            dayItem.day = cal.get(Calendar.DAY_OF_MONTH);

            int[] hijri = HijriDateUtils.getHijriDateFromGregorian(mContext, dayItem.year, dayItem.month + 1,
                    dayItem.day);
            dayItem.hijriDay = hijri[2];
            dayItem.hijriMonth = hijri[1];

            long adjustedIdNow = HijriDateUtils.getAdjustedTime(mContext);
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(adjustedIdNow);
            dayItem.isToday = (dayItem.year == now.get(Calendar.YEAR) &&
                    dayItem.month == now.get(Calendar.MONTH) &&
                    dayItem.day == now.get(Calendar.DAY_OF_MONTH));

            String key = dayItem.year + "-" + dayItem.month + "-" + dayItem.day;
            dayItem.hasEvent = eventDays.contains(key);

            mDayItems.add(dayItem);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private Set<String> getEventDaysSimple(long start, long end) {
        Set<String> events = new HashSet<>();
        if (mContext.checkSelfPermission(
                android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return events;
        }
        android.net.Uri.Builder builder = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon();
        android.content.ContentUris.appendId(builder, start);
        android.content.ContentUris.appendId(builder, end);
        android.database.Cursor c = null;
        try {
            c = mContext.getContentResolver().query(builder.build(),
                    new String[] { android.provider.CalendarContract.Instances.BEGIN }, null, null, null);
            if (c != null && c.moveToFirst()) {
                int beginIndex = c.getColumnIndex(android.provider.CalendarContract.Instances.BEGIN);
                Calendar eventCal = Calendar.getInstance();
                do {
                    eventCal.setTimeInMillis(c.getLong(beginIndex));
                    String key = eventCal.get(Calendar.YEAR) + "-" + eventCal.get(Calendar.MONTH) + "-"
                            + eventCal.get(Calendar.DAY_OF_MONTH);
                    events.add(key);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return events;
    }

    public static class CalendarDay {
        public int year;
        public int month;
        public int day;
        public int hijriDay;
        public int hijriMonth;
        public boolean isToday;
        public boolean hasEvent;
    }
}

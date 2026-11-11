package com.example.hijricalendarwidget;

import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;

public class BackupSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "HijriWidgetPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_backup);

        Button btnExport = findViewById(R.id.btn_export_settings);
        Button btnImport = findViewById(R.id.btn_import_settings);

        btnExport.setOnClickListener(v -> exportSettingsToClipboard());
        btnImport.setOnClickListener(v -> importSettingsFromClipboard());
    }

    private void exportSettingsToClipboard() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        JSONObject json = new JSONObject(allEntries);
        String settingsStr = json.toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("HijriWidgetSettings", settingsStr);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "تم نسخ الإعدادات بنجاح! ✅\nSettings copied to clipboard", Toast.LENGTH_LONG).show();
    }

    private void importSettingsFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(clipboard.hasPrimaryClip())) {
            Toast.makeText(this, "الحافظة فارغة! ❌\nClipboard is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        String pasteData = item.getText().toString();

        try {
            JSONObject json = new JSONObject(pasteData);
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                if (value instanceof Boolean)
                    editor.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer)
                    editor.putInt(key, (Integer) value);
                else if (value instanceof String)
                    editor.putString(key, (String) value);
                else if (value instanceof Long)
                    editor.putLong(key, (Long) value);
                else if (value instanceof Float)
                    editor.putFloat(key, ((Double) value).floatValue());
            }
            editor.apply();
            Toast.makeText(this, "تم استيراد الإعدادات بنجاح! ✅\nSettings imported. Please restart widgets.",
                    Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(this, "تنسيق غير صحيح في الحافظة! ❌\nInvalid settings data in clipboard", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}

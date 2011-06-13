/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nilriri.LunaCalendar.widget;

import java.util.ArrayList;

import org.nilriri.LunaCalendar.R;
import org.nilriri.LunaCalendar.tools.Common;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * The configuration screen for the WidgetProvider widget sample.
 */
public class WidgetConfigure extends Activity {

    private static final String PREFS_NAME = "org.nilriri.LunaCalendar";
    private static final String PREF_SIZE_KEY = "widgetsize_";
    private static final String PREF_KIND_KEY = "widgetkind_";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Spinner spin_widgetkind;
    private Spinner spin_widgetsize;
    private Button btn_save;

    public WidgetConfigure() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.e(Common.TAG, "***************onCreate**************************");
        
        setResult(RESULT_CANCELED);
        setContentView(R.layout.appwidget_configure);

        spin_widgetkind = (Spinner) findViewById(R.id.widgetkind);
        spin_widgetsize = (Spinner) findViewById(R.id.widgetsize);
        btn_save = (Button) findViewById(R.id.save_button);

        ArrayAdapter<CharSequence> adp = ArrayAdapter.createFromResource(this, R.array.entries_widgetkind, android.R.layout.simple_spinner_item);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_widgetkind.setAdapter(adp);

        ArrayAdapter<CharSequence> adp_size = ArrayAdapter.createFromResource(this, R.array.entries_widgetsize, android.R.layout.simple_spinner_item);
        adp_size.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_widgetsize.setAdapter(adp_size);

        btn_save.setOnClickListener(mOnClickListener);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = WidgetConfigure.this;

            Toast.makeText(getBaseContext(), "Coming Soon.", Toast.LENGTH_LONG).show();

            setWidgetKind(context, mAppWidgetId, spin_widgetkind.getSelectedItemId());
            setWidgetSize(context, mAppWidgetId, spin_widgetsize.getSelectedItemId());

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            WidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId, spin_widgetsize.getSelectedItemId());

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);

            setResult(RESULT_OK, resultValue);

            finish();
        }
    };

    static void setWidgetKind(Context context, int appWidgetId, Long text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_KIND_KEY + appWidgetId, text);
        prefs.commit();
    }

    static Long getWidgetKind(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        Long size = prefs.getLong(PREF_KIND_KEY + appWidgetId, new Long(-1));
        return size;
    }

    static void setWidgetSize(Context context, int appWidgetId, Long text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_SIZE_KEY + appWidgetId, text);
        prefs.commit();
    }

    static Long getWidgetSize(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        Long size = prefs.getLong(PREF_SIZE_KEY + appWidgetId, new Long(-1));
        return size;
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
    }

    static void loadAllTitlePrefs(Context context, ArrayList<Integer> appWidgetIds, ArrayList<String> texts) {
    }
}

package org.nilriri.LunaCalendar.widget;

import java.util.List;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

public class WidgetUtil {

    public static void refreshWidgets(Context context) {
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        List<AppWidgetProviderInfo> awp = awm.getInstalledProviders();

       
        
        for (int i = 0; i < awp.size(); i++) {
            AppWidgetProviderInfo af = awp.get(i);
            
            int[] appWidgetIds = awm.getAppWidgetIds(af.provider);
            if (appWidgetIds != null) {
                for (int ji = 0; ji < appWidgetIds.length; ji++) {
                    WidgetProvider.updateAppWidget(context, awm, appWidgetIds[ji]);

                }
            }
        }

    }

}

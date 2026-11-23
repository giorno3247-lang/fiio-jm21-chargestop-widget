package jp.giorno.chargestop;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;

public class ToggleWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_TOGGLE = "jp.giorno.chargestop.ACTION_TOGGLE";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            if (!ShizukuExec.isBinderAlive() || !ShizukuExec.isGranted()) {
                Toast.makeText(context, "Shizukuの準備が必要です。アプリを開いてください", Toast.LENGTH_LONG).show();
                Intent open = new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(open);
                return;
            }
            final PendingResult result = goAsync();
            final boolean next = !ChargeStop.isOn(context);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean ok = ShizukuExec.setSync(context, next);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,
                                    ok ? ("充電停止を " + (next ? "ON" : "OFF") + " にしました")
                                       : ("切り替え失敗：" + ShizukuExec.lastError),
                                    Toast.LENGTH_SHORT).show();
                            refreshAll(context);
                            result.finish();
                        }
                    });
                }
            }).start();
        }
    }

    static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, ToggleWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(name);
        for (int id : ids) {
            manager.updateAppWidget(id, buildViews(context));
        }
    }

    private static RemoteViews buildViews(Context context) {
        boolean on = ChargeStop.isOn(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setImageViewResource(R.id.widget_icon,
                on ? R.drawable.battery_on : R.drawable.battery_off);
        Intent intent = new Intent(context, ToggleWidgetProvider.class).setAction(ACTION_TOGGLE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        return views;
    }
}

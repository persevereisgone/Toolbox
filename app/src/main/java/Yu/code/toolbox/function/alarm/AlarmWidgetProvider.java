package Yu.code.toolbox.function.alarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.Locale;
import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

/**
 * 组件 A「定时提醒 · 到点响」（2×3）。
 * 数值区只放时钟时刻，绝不放时长——与组件 B 的时长格式严格区分。
 */
public class AlarmWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_ADJUST = "Yu.code.toolbox.ALARM_W_ADJUST";
    public static final String ACTION_CANCEL = "Yu.code.toolbox.ALARM_W_CANCEL";
    public static final String EXTRA_DELTA = "delta";

    @Override public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    @Override public void onReceive(Context ctx, Intent in) {
        super.onReceive(ctx, in);
        String a = in.getAction();
        AlarmStore s = new AlarmStore(ctx);
        if (ACTION_ADJUST.equals(a)) {
            int delta = in.getIntExtra(EXTRA_DELTA, 0);
            if (!s.isRunning()) {
                if (delta > 0) {
                    // 空闲时点 +：直接按 now+delta 定一个时刻，不用先进 App
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    c.add(java.util.Calendar.MINUTE, delta);
                    c.set(java.util.Calendar.SECOND, 0);
                    int h = c.get(java.util.Calendar.HOUR_OF_DAY);
                    int m = c.get(java.util.Calendar.MINUTE);
                    s.setRtc(ctx, h, m, String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m));
                } else {
                    android.widget.Toast.makeText(ctx, R.string.alarm_widget_rtc_empty, android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                s.addMinutes(ctx, delta);
            }
        } else if (ACTION_CANCEL.equals(a)) {
            s.cancel(ctx);
            AlarmReceiver.stopRing();
        } else {
            return;
        }
        refreshAll(ctx);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_alarm);
        AlarmStore s = new AlarmStore(ctx);
        boolean running = s.isRunning() && AlarmStore.MODE_RTC.equals(s.getMode());

        if (running) {
            rv.setTextViewText(R.id.tv_widget_alarm_time, s.getTargetHm());
            boolean tomorrow = ctx.getString(R.string.alarm_day_tomorrow).equals(s.getDayPrefix(ctx));
            rv.setTextViewText(R.id.tv_widget_alarm_sub,
                    ctx.getString(tomorrow ? R.string.alarm_widget_rtc_tomorrow : R.string.alarm_widget_rtc_today,
                            fmtShort(s.getRemaining())));
            rv.setViewVisibility(R.id.btn_widget_alarm_cancel, android.view.View.VISIBLE);
        } else {
            rv.setTextViewText(R.id.tv_widget_alarm_time, "--:--");
            rv.setTextViewText(R.id.tv_widget_alarm_sub, ctx.getString(R.string.alarm_widget_rtc_empty));
            rv.setViewVisibility(R.id.btn_widget_alarm_cancel, android.view.View.INVISIBLE);
        }

        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_minus15,
                broadcast(ctx, 21, ACTION_ADJUST, -15));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_plus15,
                broadcast(ctx, 22, ACTION_ADJUST, 15));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_minus5,
                broadcast(ctx, 25, ACTION_ADJUST, -5));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_plus5,
                broadcast(ctx, 26, ACTION_ADJUST, 5));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_pick,
                PendingIntent.getActivity(ctx, 23, openIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_cancel,
                broadcast(ctx, 24, ACTION_CANCEL, 0));
        mgr.updateAppWidget(id, rv);
    }

    private static PendingIntent broadcast(Context ctx, int code, String action, int delta) {
        Intent i = new Intent(ctx, AlarmWidgetProvider.class).setAction(action).putExtra(EXTRA_DELTA, delta);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static Intent openIntent(Context ctx) {
        Intent open = new Intent(ctx, MainActivity.class)
                .putExtra("open_function", R.layout.function_alarm)
                .putExtra("auto_time_sheet", true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return open;
    }

    static String fmtShort(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        if (h > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", h, (s % 3600) / 60, s % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, AlarmWidgetProvider.class))) updateOne(ctx, mgr, id);
    }
}

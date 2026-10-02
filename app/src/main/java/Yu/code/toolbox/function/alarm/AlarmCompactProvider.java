package Yu.code.toolbox.function.alarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import Yu.code.toolbox.R;

/**
 * 组件 A 紧凑版「定时提醒 · 到点响」（2×2，试验性）。
 * 与 2×3 版共用同一 AlarmStore 数据源；为塞进 2×2 只保留 ±5 微调
 * （−5 / +5 / 选时刻 / 取消），±15 请用 2×3 版。试用满意后删除 2×3 版。
 * 数值区只放时钟时刻，绝不放时长——与倒计时组件严格区分。
 */
public class AlarmCompactProvider extends AppWidgetProvider {
    public static final String ACTION_ADJUST = "Yu.code.toolbox.ALARM_RTC_C_ADJUST";
    public static final String ACTION_CANCEL = "Yu.code.toolbox.ALARM_RTC_C_CANCEL";
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
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_alarm_compact);
        AlarmStore s = new AlarmStore(ctx);
        boolean running = s.isRunning() && AlarmStore.MODE_RTC.equals(s.getMode());

        if (running) {
            rv.setTextViewText(R.id.tv_widget_alarm_time, s.getTargetHm());
            boolean tomorrow = ctx.getString(R.string.alarm_day_tomorrow).equals(s.getDayPrefix(ctx));
            rv.setTextViewText(R.id.tv_widget_alarm_sub,
                    ctx.getString(tomorrow ? R.string.alarm_widget_rtc_tomorrow : R.string.alarm_widget_rtc_today,
                            AlarmWidgetProvider.fmtShort(s.getRemaining())));
            rv.setViewVisibility(R.id.btn_widget_alarm_cancel, View.VISIBLE);
        } else {
            rv.setTextViewText(R.id.tv_widget_alarm_time, "--:--");
            rv.setTextViewText(R.id.tv_widget_alarm_sub, ctx.getString(R.string.alarm_widget_rtc_empty));
            rv.setViewVisibility(R.id.btn_widget_alarm_cancel, View.INVISIBLE);
        }

        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_minus5,
                broadcast(ctx, 51, ACTION_ADJUST, -5));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_plus5,
                broadcast(ctx, 52, ACTION_ADJUST, 5));
        Intent open = AlarmWidgetProvider.openIntent(ctx);
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_pick,
                PendingIntent.getActivity(ctx, 53, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        rv.setOnClickPendingIntent(R.id.tv_widget_alarm_time,
                PendingIntent.getActivity(ctx, 54, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        rv.setOnClickPendingIntent(R.id.btn_widget_alarm_cancel,
                broadcast(ctx, 55, ACTION_CANCEL, 0));
        mgr.updateAppWidget(id, rv);
    }

    private static PendingIntent broadcast(Context ctx, int code, String action, int delta) {
        Intent i = new Intent(ctx, AlarmCompactProvider.class).setAction(action).putExtra(EXTRA_DELTA, delta);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, AlarmCompactProvider.class))) updateOne(ctx, mgr, id);
    }
}

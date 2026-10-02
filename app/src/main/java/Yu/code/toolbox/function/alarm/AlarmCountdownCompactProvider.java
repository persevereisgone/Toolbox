package Yu.code.toolbox.function.alarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import Yu.code.toolbox.R;

/**
 * 组件 B 紧凑版「倒计时 · 剩多久响」（2×2，试验性）。
 * 与 2×3 版共用同一 AlarmStore 数据源，行为一致；为塞进 2×2 只保留 ±5 步进
 * （−5 / +5 / 开始·重定 / 清空），±1 请用 2×3 版。试用满意后删除 2×3 版。
 */
public class AlarmCountdownCompactProvider extends AppWidgetProvider {
    public static final String ACTION_ADJUST = "Yu.code.toolbox.ALARM_CDC_ADJUST";
    public static final String ACTION_START = "Yu.code.toolbox.ALARM_CDC_START";
    public static final String ACTION_CLEAR = "Yu.code.toolbox.ALARM_CDC_CLEAR";
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
            if (s.isRunning() && AlarmStore.MODE_COUNTDOWN.equals(s.getMode())) {
                long remMin = Math.round(s.getRemaining() / 60000.0);
                long next = AlarmStore.clamp((int) (remMin + delta));
                if (next != remMin + delta) {
                    Toast.makeText(ctx, R.string.alarm_widget_range_tip, Toast.LENGTH_SHORT).show();
                }
                s.setCountdownMinutes(ctx, next);
                s.setPendingMinutes((int) next);
            } else {
                int next = AlarmStore.clamp(s.getPendingMinutes() + delta);
                if (next != s.getPendingMinutes() + delta) {
                    Toast.makeText(ctx, R.string.alarm_widget_range_tip, Toast.LENGTH_SHORT).show();
                }
                s.setPendingMinutes(next);
            }
        } else if (ACTION_START.equals(a)) {
            s.setCountdownMinutes(ctx, s.getPendingMinutes());
        } else if (ACTION_CLEAR.equals(a)) {
            s.cancel(ctx);
            s.setPendingMinutes(AlarmStore.DEFAULT_PENDING_MIN);
            AlarmReceiver.stopRing();
        } else {
            return;
        }
        refreshAll(ctx);
        AlarmWidgetProvider.refreshAll(ctx);
        AlarmCountdownWidgetProvider.refreshAll(ctx);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_alarm_countdown_compact);
        AlarmStore s = new AlarmStore(ctx);
        boolean running = s.isRunning() && AlarmStore.MODE_COUNTDOWN.equals(s.getMode());

        if (running) {
            long base = SystemClock.elapsedRealtime() + s.getRemaining();
            rv.setViewVisibility(R.id.tv_widget_cd_time, View.GONE);
            rv.setViewVisibility(R.id.chrono_widget_cd, View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 24) {
                rv.setChronometerCountDown(R.id.chrono_widget_cd, true);
            }
            rv.setChronometer(R.id.chrono_widget_cd, base, null, true);
            rv.setTextViewText(R.id.tv_widget_cd_sub,
                    ctx.getString(R.string.alarm_widget_cd_running, AlarmCountdownWidgetProvider.fmtSpoken(s.getRemaining())));
            rv.setTextViewText(R.id.btn_widget_cd_start, ctx.getString(R.string.alarm_widget_btn_restart));
        } else {
            int pending = s.getPendingMinutes();
            rv.setViewVisibility(R.id.chrono_widget_cd, View.GONE);
            rv.setViewVisibility(R.id.tv_widget_cd_time, View.VISIBLE);
            rv.setChronometer(R.id.chrono_widget_cd, 0, null, false);
            rv.setTextViewText(R.id.tv_widget_cd_time,
                    String.format(java.util.Locale.getDefault(), "%02d:00", pending));
            rv.setTextViewText(R.id.tv_widget_cd_sub,
                    ctx.getString(R.string.alarm_widget_cd_pending,
                            AlarmCountdownWidgetProvider.fmtSpoken(pending * 60_000L)));
            rv.setTextViewText(R.id.btn_widget_cd_start, ctx.getString(R.string.alarm_widget_btn_start));
        }

        rv.setOnClickPendingIntent(R.id.btn_widget_cd_minus5, broadcast(ctx, 41, ACTION_ADJUST, -5));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_plus5, broadcast(ctx, 42, ACTION_ADJUST, 5));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_start, broadcast(ctx, 43, ACTION_START, 0));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_clear, broadcast(ctx, 44, ACTION_CLEAR, 0));
        Intent open = AlarmWidgetProvider.openIntent(ctx);
        rv.setOnClickPendingIntent(R.id.tv_widget_cd_time, PendingIntent.getActivity(ctx, 45, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        rv.setOnClickPendingIntent(R.id.chrono_widget_cd, PendingIntent.getActivity(ctx, 46, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        mgr.updateAppWidget(id, rv);
    }

    private static PendingIntent broadcast(Context ctx, int code, String action, int delta) {
        Intent i = new Intent(ctx, AlarmCountdownCompactProvider.class).setAction(action).putExtra(EXTRA_DELTA, delta);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, AlarmCountdownCompactProvider.class))) updateOne(ctx, mgr, id);
    }
}

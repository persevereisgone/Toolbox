package Yu.code.toolbox.function.alarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;
import Yu.code.toolbox.R;

/**
 * 组件 B「倒计时 · 剩多久响」（2×3）。
 * 数值区只放时长，绝不放时刻——与组件 A 的时钟格式严格区分。
 * RemoteViews 无法内嵌输入框，故用 ±1 分步进替代自定义输入。
 */
public class AlarmCountdownWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_ADJUST = "Yu.code.toolbox.ALARM_CD_ADJUST";
    public static final String ACTION_START = "Yu.code.toolbox.ALARM_CD_START";
    public static final String ACTION_CLEAR = "Yu.code.toolbox.ALARM_CD_CLEAR";
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
            // 清空：停掉当前闹钟，并把待定时长恢复默认 5 分钟
            s.cancel(ctx);
            s.setPendingMinutes(AlarmStore.DEFAULT_PENDING_MIN);
            AlarmReceiver.stopRing();
        } else {
            return;
        }
        refreshAll(ctx);
        AlarmWidgetProvider.refreshAll(ctx);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_alarm_countdown);
        AlarmStore s = new AlarmStore(ctx);
        boolean running = s.isRunning() && AlarmStore.MODE_COUNTDOWN.equals(s.getMode());

        if (running) {
            long rem = s.getRemaining();
            // 系统秒表倒计时：桌面进程自己走字，每秒刷新，零耗电、免保活
            long base = android.os.SystemClock.elapsedRealtime() + Math.max(0, rem);
            rv.setChronometer(R.id.chrono_widget_cd, base, null, true);
            rv.setChronometerCountDown(R.id.chrono_widget_cd, true);
            rv.setViewVisibility(R.id.chrono_widget_cd, android.view.View.VISIBLE);
            rv.setViewVisibility(R.id.tv_widget_cd_time, android.view.View.GONE);
            rv.setTextViewText(R.id.tv_widget_cd_sub, ctx.getString(R.string.alarm_widget_cd_running, fmtSpoken(rem)));
            rv.setTextViewText(R.id.btn_widget_cd_start, ctx.getString(R.string.alarm_widget_btn_restart));
        } else {
            int pending = s.getPendingMinutes();
            rv.setChronometer(R.id.chrono_widget_cd, 0, null, false);
            rv.setViewVisibility(R.id.chrono_widget_cd, android.view.View.GONE);
            rv.setViewVisibility(R.id.tv_widget_cd_time, android.view.View.VISIBLE);
            rv.setTextViewText(R.id.tv_widget_cd_time, String.format(java.util.Locale.getDefault(), "%02d:00", pending));
            rv.setTextViewText(R.id.tv_widget_cd_sub,
                    ctx.getString(R.string.alarm_widget_cd_pending, fmtSpoken(pending * 60_000L)));
            rv.setTextViewText(R.id.btn_widget_cd_start, ctx.getString(R.string.alarm_widget_btn_start));
        }

        PendingIntent open = PendingIntent.getActivity(ctx, 37, AlarmWidgetProvider.openIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.tv_widget_cd_time, open);
        rv.setOnClickPendingIntent(R.id.chrono_widget_cd, open);

        rv.setOnClickPendingIntent(R.id.btn_widget_cd_minus5, broadcast(ctx, 31, ACTION_ADJUST, -5));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_plus5, broadcast(ctx, 32, ACTION_ADJUST, 5));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_minus, broadcast(ctx, 33, ACTION_ADJUST, -1));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_plus, broadcast(ctx, 34, ACTION_ADJUST, 1));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_start, broadcast(ctx, 35, ACTION_START, 0));
        rv.setOnClickPendingIntent(R.id.btn_widget_cd_clear, broadcast(ctx, 36, ACTION_CLEAR, 0));
        mgr.updateAppWidget(id, rv);
    }

    private static PendingIntent broadcast(Context ctx, int code, String action, int delta) {
        Intent i = new Intent(ctx, AlarmCountdownWidgetProvider.class).setAction(action).putExtra(EXTRA_DELTA, delta);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void openApp(Context ctx) {
        try {
            Intent open = new Intent(ctx, Yu.code.toolbox.MainActivity.class).putExtra("open_function", R.layout.function_alarm);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ctx.startActivity(open);
        } catch (Exception ignored) {}
    }

    /** 时长数值格式：mm:ss，超 1 小时 h:mm:ss。绝不输出时钟格式语义。 */
    static String fmtDuration(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        if (h > 0) return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, (s % 3600) / 60, s % 60);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    /** 口语化时长："4 分 32 秒" / "1 小时 30 分" / "30 秒"。 */
    static String fmtSpoken(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return h + " 小时 " + m + " 分";
        if (m > 0) return sec > 0 ? m + " 分 " + sec + " 秒" : m + " 分钟";
        return sec + " 秒";
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, AlarmCountdownWidgetProvider.class))) updateOne(ctx, mgr, id);
    }
}

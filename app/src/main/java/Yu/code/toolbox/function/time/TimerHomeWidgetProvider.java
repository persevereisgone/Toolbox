package Yu.code.toolbox.function.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

public class TimerHomeWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_SET = "Yu.code.toolbox.TIMER_HOME_SET";
    public static final String ACTION_CANCEL = "Yu.code.toolbox.TIMER_HOME_CANCEL";
    public static final String EXTRA_MINUTES = "minutes";
    private static final String PREFS = "lock_prefs";
    private static final String KEY_TRIGGER = "lock_trigger_at";
    private static final String KEY_TOTAL = "lock_total_ms";
    private static final int REQ = 1001;

    @Override public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    @Override public void onReceive(Context ctx, Intent in) {
        super.onReceive(ctx, in);
        String a = in == null ? null : in.getAction();
        if (ACTION_SET.equals(a)) {
            int min = in.getIntExtra(EXTRA_MINUTES, 15);
            setTimer(ctx, min * 60L * 1000L);
            refreshAll(ctx);
        } else if (ACTION_CANCEL.equals(a)) {
            cancelTimer(ctx);
            refreshAll(ctx);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(a)) {
            refreshAll(ctx);
        }
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_timer_home);
        long remain = getRemain(ctx);
        boolean running = remain > 0;
        rv.setViewVisibility(R.id.group_preset, running ? View.GONE : View.VISIBLE);
        rv.setViewVisibility(R.id.group_running, running ? View.VISIBLE : View.GONE);
        if (running) {
            rv.setTextViewText(R.id.tv_w_remain, fmtMmSs(remain));
            rv.setProgressBar(R.id.pb_w_progress, 100, progressOf(ctx, remain), false);
            rv.setOnClickPendingIntent(R.id.btn_w_cancel, bc(ctx, 71, ACTION_CANCEL, 0));
        } else {
            rv.setOnClickPendingIntent(R.id.btn_w_preset_15, bc(ctx, 72, ACTION_SET, 15));
            rv.setOnClickPendingIntent(R.id.btn_w_preset_30, bc(ctx, 73, ACTION_SET, 30));
            rv.setOnClickPendingIntent(R.id.btn_w_preset_60, bc(ctx, 74, ACTION_SET, 60));
            Intent open2 = new Intent(ctx, MainActivity.class).putExtra("open_timer", true);
            rv.setOnClickPendingIntent(R.id.btn_w_preset_custom,
                    PendingIntent.getActivity(ctx, 76, open2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        Intent open = new Intent(ctx, MainActivity.class).putExtra("open_timer", true);
        PendingIntent pi = PendingIntent.getActivity(ctx, 75, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.tv_w_remain, pi);
        mgr.updateAppWidget(id, rv);
    }

    private static PendingIntent bc(Context ctx, int code, String action, int min) {
        Intent i = new Intent(ctx, TimerHomeWidgetProvider.class).setAction(action).putExtra(EXTRA_MINUTES, min);
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setTimer(Context ctx, long delay) {
        long at = System.currentTimeMillis() + delay;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, LockAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_TRIGGER, at).putLong(KEY_TOTAL, delay).apply();
        Intent svc = new Intent(ctx, CountdownOverlayService.class).setAction(CountdownOverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc); else ctx.startService(svc);
    }

    private static void cancelTimer(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, LockAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TRIGGER).apply();
        Intent svc = new Intent(ctx, CountdownOverlayService.class).setAction(CountdownOverlayService.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc); else ctx.startService(svc);
    }

    private static long getRemain(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long at = p.getLong(KEY_TRIGGER, -1L);
        if (at <= 0) return -1L;
        return at - System.currentTimeMillis();
    }

    private static int progressOf(Context ctx, long remain) {
        long total = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_TOTAL, -1L);
        if (total <= 0) return 100;
        long done = total - remain;
        return (int) Math.max(0, Math.min(100, done * 100 / total));
    }

    private static String fmtMmSs(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, TimerHomeWidgetProvider.class))) updateOne(ctx, mgr, id);
    }
}

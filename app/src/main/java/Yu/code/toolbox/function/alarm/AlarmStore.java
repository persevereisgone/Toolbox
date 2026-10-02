package Yu.code.toolbox.function.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;
import Yu.code.toolbox.R;

public class AlarmStore {
    private static final String PREFS = "alarm_prefs";
    private static final String KEY_TRIGGER = "trigger_at";
    private static final String KEY_LABEL = "label";
    private static final String KEY_MODE = "mode";
    private static final String KEY_PENDING_MIN = "pending_min";
    /** 进度卡片用：开始时刻/总时长 */
    private static final String KEY_START = "start_at";
    private static final String KEY_TOTAL = "total_ms";

    public static final String MODE_RTC = "rtc";
    public static final String MODE_COUNTDOWN = "countdown";
    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 12 * 60;
    public static final int DEFAULT_PENDING_MIN = 5;

    private final SharedPreferences prefs;

    public AlarmStore(Context c) { prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public void setCountdown(Context ctx, long ms, String label) {
        long now = System.currentTimeMillis();
        long at = now + ms;
        prefs.edit().putLong(KEY_TRIGGER, at).putString(KEY_LABEL, label).putString(KEY_MODE, MODE_COUNTDOWN)
                .putLong(KEY_START, now).putLong(KEY_TOTAL, ms).apply();
        schedule(ctx, at);
        refreshWidgets(ctx);
    }

    public void setCountdownMinutes(Context ctx, float minutes) {
        int m = clamp((int) Math.round(minutes));
        int sec = Math.max(6, (int) Math.round(minutes * 60));
        String label = minutes < 1
                ? ctx.getString(R.string.alarm_countdown_label_sec, sec)
                : ctx.getString(R.string.alarm_countdown_label, m);
        setCountdown(ctx, sec * 1000L, label);
    }

    public void setRtc(Context ctx, int h, int m, String label) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        long now = System.currentTimeMillis();
        long at = cal.getTimeInMillis();
        prefs.edit().putLong(KEY_TRIGGER, at).putString(KEY_LABEL, label).putString(KEY_MODE, MODE_RTC)
                .putLong(KEY_START, now).putLong(KEY_TOTAL, Math.max(1, at - now)).apply();
        schedule(ctx, at);
        refreshWidgets(ctx);
    }

    /** 组件A微调：目标时刻前后平移，落到过去则按 setRtc 同规则顺延明天。 */
    public void addMinutes(Context ctx, int delta) {
        long t = getTriggerAt();
        if (t <= 0) return;
        long at = t + delta * 60_000L;
        if (at <= System.currentTimeMillis()) at += 24 * 60 * 60 * 1000L;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(at);
        setRtc(ctx, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), getLabel(ctx));
    }

    private void schedule(Context ctx, long at) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 9001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am == null) return;
        // 用 setAlarmClock：系统按"闹钟"对待，Doze/后台限制下优先级最高，到点必触发，状态栏还会显示闹钟图标
        Intent show = new Intent(ctx, Yu.code.toolbox.MainActivity.class)
                .putExtra("open_function", Yu.code.toolbox.R.layout.function_alarm);
        PendingIntent showPi = PendingIntent.getActivity(ctx, 9002, show, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, showPi), pi);
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    public void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 9001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        prefs.edit().remove(KEY_TRIGGER).apply();
        refreshWidgets(ctx);
    }

    public void snooze5(Context ctx) {
        setCountdown(ctx, 5 * 60 * 1000L, ctx.getString(R.string.alarm_btn_snooze));
    }

    public long getTriggerAt() { return prefs.getLong(KEY_TRIGGER, -1); }
    /** 进度 0~100：已过/总时长 */
    public int getProgress() {
        long total = prefs.getLong(KEY_TOTAL, -1);
        long start = prefs.getLong(KEY_START, -1);
        long trigger = getTriggerAt();
        if (total <= 0 || start <= 0 || trigger <= 0) {
            return -1;
        }
        long done = System.currentTimeMillis() - start;
        return (int) Math.max(0, Math.min(100, done * 100 / total));
    }
    public long getRemaining() { long t = getTriggerAt(); return t <= 0 ? -1 : t - System.currentTimeMillis(); }
    public String getLabel(Context ctx) { return prefs.getString(KEY_LABEL, ctx.getString(R.string.alarm_page_title)); }
    public String getMode() { return prefs.getString(KEY_MODE, MODE_RTC); }
    public boolean isRunning() { return getTriggerAt() > System.currentTimeMillis(); }

    /** 组件B待定时长（分钟），下次进组件 B 或重定时时的默认值。 */
    public int getPendingMinutes() { return prefs.getInt(KEY_PENDING_MIN, DEFAULT_PENDING_MIN); }
    public void setPendingMinutes(int m) { prefs.edit().putInt(KEY_PENDING_MIN, clamp(m)).apply(); }

    public static int clamp(int minutes) {
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }

    public static boolean isValidMinutes(float minutes) {
        return minutes >= 0.1f && minutes <= MAX_MINUTES;
    }

    /** "今天"/"明天" 判定，用于组件A与 App hero 的副文案。 */
    public String getDayPrefix(Context ctx) {
        long t = getTriggerAt();
        if (t <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(t);
        boolean sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
        return ctx.getString(sameDay ? R.string.alarm_day_today : R.string.alarm_day_tomorrow);
    }

    public String getTargetHm() {
        long t = getTriggerAt();
        if (t <= 0) return "";
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private void refreshWidgets(Context ctx) {
        AlarmWidgetProvider.refreshAll(ctx);
        AlarmCountdownWidgetProvider.refreshAll(ctx);
        AlarmCompactProvider.refreshAll(ctx);
        AlarmCountdownCompactProvider.refreshAll(ctx);
        // 小组件刷新=闹钟状态变化,同步常驻通知(设置→启动,取消/过期→停止)
        try {
            AlarmCountdownService.sync(ctx.getApplicationContext());
        } catch (Exception ignored) {
        }
    }
}

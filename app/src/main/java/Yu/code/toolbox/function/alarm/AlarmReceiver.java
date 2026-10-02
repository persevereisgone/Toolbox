package Yu.code.toolbox.function.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import Yu.code.toolbox.R;

public class AlarmReceiver extends BroadcastReceiver {
    static Ringtone sRingtone;
    static Vibrator sVibrator;
    static final String CH = "alarm_channel";

    public static final String ACTION_DISMISS = "Yu.code.toolbox.ALARM_DISMISS";
    public static final String ACTION_SNOOZE = "Yu.code.toolbox.ALARM_SNOOZE";

    @Override public void onReceive(Context ctx, Intent intent) {
        String a = intent.getAction();
        if (ACTION_DISMISS.equals(a)) { stopRing(); cancelNotif(ctx); return; }
        if (ACTION_SNOOZE.equals(a)) { stopRing(); cancelNotif(ctx); new AlarmStore(ctx).snooze5(ctx); return; }
        startRing(ctx);
        showNotif(ctx);
        AlarmWidgetProvider.refreshAll(ctx);
        AlarmCountdownWidgetProvider.refreshAll(ctx);
        AlarmCompactProvider.refreshAll(ctx);
        AlarmCountdownCompactProvider.refreshAll(ctx);
        // 响铃=倒计时结束,停掉常驻通知(贪睡会重建)
        try {
            AlarmCountdownService.sync(ctx.getApplicationContext());
        } catch (Exception ignored) {
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> { stopRing(); cancelNotif(ctx); }, 10_000);
    }

    static void startRing(Context ctx) {
        startSound(ctx);
        startVibrate(ctx);
        Intent i = new Intent(ctx, AlarmRingActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { ctx.startActivity(i); } catch (Exception ignored) {}
    }

    private static void startSound(Context ctx) {
        try {
            if (sRingtone == null) {
                sRingtone = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                if (sRingtone == null) sRingtone = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
            if (sRingtone != null && !sRingtone.isPlaying()) sRingtone.play();
        } catch (Exception e) {
            sRingtone = null;
            try {
                sRingtone = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                if (sRingtone != null) sRingtone.play();
            } catch (Exception ignored) {}
        }
    }

    private static void startVibrate(Context ctx) {
        try {
            Vibrator v = getVibrator(ctx);
            if (v == null || !v.hasVibrator()) return;
            try { v.cancel(); } catch (Exception ignored) {}
            sVibrator = v;
            // 闹钟式三连脉冲循环，和铃声节奏完全解耦：震0.7秒→停0.15秒→震0.7秒→停0.15秒→长震1秒→停0.4秒→循环
            long[] timings = new long[]{0, 700, 150, 700, 150, 1000, 400, 1000};
            int[] amps = new int[]{0, 255, 0, 255, 0, 255, 0, 255};
            if (Build.VERSION.SDK_INT >= 26) {
                android.os.VibrationEffect effect = v.hasAmplitudeControl()
                        ? android.os.VibrationEffect.createWaveform(timings, amps, 0)
                        : android.os.VibrationEffect.createWaveform(timings, 0);
                if (Build.VERSION.SDK_INT >= 29) {
                    // 关键：声明为闹钟用途，走系统"闹钟振动"通道，不会被触感/媒体音量压住
                    android.os.VibrationAttributes attrs = new android.os.VibrationAttributes.Builder()
                            .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                            .build();
                    v.vibrate(effect, attrs);
                } else {
                    v.vibrate(effect);
                }
            } else {
                v.vibrate(timings, 0);
            }
        } catch (Exception ignored) {}
    }

    /** API 31+ 走 VibratorManager 取默认马达，兼容新机多马达/分区振动。 */
    private static Vibrator getVibrator(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager vm =
                        (android.os.VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) return vm.getDefaultVibrator();
            }
        } catch (Exception ignored) {}
        try {
            return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {
            return null;
        }
    }

    static void stopRing() {
        try { if (sRingtone != null && sRingtone.isPlaying()) sRingtone.stop(); } catch (Exception ignored) {}
        sRingtone = null;
        try { if (sVibrator != null) sVibrator.cancel(); } catch (Exception ignored) {}
        sVibrator = null;
    }

    static void showNotif(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH, "闹钟", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent dis = new Intent(ctx, AlarmReceiver.class).setAction(ACTION_DISMISS);
        Intent sno = new Intent(ctx, AlarmReceiver.class).setAction(ACTION_SNOOZE);
        // 锁屏/后台下靠全屏 intent 亮屏（Android 10+ 后台直接 startActivity 会被拦截，必须走这条路）
        Intent full = new Intent(ctx, AlarmRingActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent fullPi = PendingIntent.getActivity(ctx, 13, full, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.mipmap.toolbox)
            .setContentTitle(ctx.getString(R.string.alarm_ring_title))
            .setContentText(ctx.getString(R.string.alarm_ring_auto_close))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullPi, true)
            .addAction(0, ctx.getString(R.string.alarm_btn_close),
                    PendingIntent.getBroadcast(ctx, 11, dis, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
            .addAction(0, ctx.getString(R.string.alarm_btn_snooze),
                    PendingIntent.getBroadcast(ctx, 12, sno, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        nm.notify(9001, b.build());
    }

    static void cancelNotif(Context ctx) {
        try { ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(9001); } catch (Exception ignored) {}
    }
}

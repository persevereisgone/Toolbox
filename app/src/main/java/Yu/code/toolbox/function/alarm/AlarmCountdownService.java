package Yu.code.toolbox.function.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

/**
 * 闹钟常驻通知服务：定时/倒计时设置后在通知栏常驻显示剩余时间，锁屏可见。
 * 每 10 秒刷新一次；取消/响铃时停止。
 */
public class AlarmCountdownService extends Service {

    public static final String ACTION_STOP = "Yu.code.toolbox.alarm.COUNTDOWN_NOTIFY_STOP";
    public static final String ACTION_OPEN = "Yu.code.toolbox.alarm.COUNTDOWN_NOTIFY_OPEN";
    /** 通道升级：老通道在已安装设备上沿用旧的重要程度，切 v3 强制重建 HIGH */
    private static final String CHANNEL_ID = "alarm_countdown_v3";
    private static final int NOTIFY_ID = 9002;
    private static final long TICK_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!refresh()) {
                stopSelf();
                return;
            }
            handler.postDelayed(this, TICK_MS);
        }
    };

    /** 有闹钟待响就启动常驻通知，没有就停掉 */
    public static void sync(Context ctx) {
        AlarmStore store = new AlarmStore(ctx);
        long remaining = store.getRemaining();
        Intent i = new Intent(ctx, AlarmCountdownService.class);
        if (remaining > 0) {
            try {
                ctx.startForegroundService(i);
            } catch (Exception ignored) {
            }
        } else {
            try {
                ctx.stopService(i);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            try {
                new AlarmStore(this).cancel(this);
            } catch (Exception ignored) {
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_OPEN.equals(intent.getAction())) {
            // 点通知体：先重新断言前台存活，再进 App，保证点一次不会把通知点掉
            sync(this);
            try {
                Intent open = new Intent(this, MainActivity.class)
                        .putExtra("open_function", R.layout.function_alarm)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(open);
            } catch (Exception ignored) {
            }
            return START_STICKY;
        }
        startForeground(NOTIFY_ID, build(text()));
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, TICK_MS);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** 刷新一次通知；返回 false 表示已无待响闹钟 */
    private boolean refresh() {
        AlarmStore store = new AlarmStore(this);
        if (store.getRemaining() <= 0) {
            return false;
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFY_ID, build(text()));
        }
        return true;
    }

    private String text() {
        AlarmStore store = new AlarmStore(this);
        long remaining = store.getRemaining();
        if (remaining <= 0) {
            return getString(R.string.alarm_page_title);
        }
        // 卡片大数字只放剩余时长，标签进副行
        return format(remaining);
    }

    private String format(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", m, sec);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.alarm_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.alarm_channel_desc));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(false);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try { nm.deleteNotificationChannel("alarm_countdown_v2"); } catch (Exception ignored) {}
            try { nm.deleteNotificationChannel("alarm_countdown"); } catch (Exception ignored) {}
            nm.createNotificationChannel(channel);
        }
    }

    /** B 版两行紧凑卡片：时间居中＋副文案；取消 / 打开；无 bigView 即无折叠箭头 */
    private Notification build(String content) {
        AlarmStore store = new AlarmStore(this);
        int progress = store.getProgress();
        String sub = store.getTargetHm() + " · " + content + getString(R.string.alarm_home_after)
                + (progress >= 0 ? " · " + getString(R.string.alarm_notify_elapsed, progress) : "");

        android.widget.RemoteViews views = new android.widget.RemoteViews(
                getPackageName(), R.layout.notification_alarm_countdown);
        views.setTextViewText(R.id.tv_notify_time, content);
        views.setTextViewText(R.id.tv_notify_sub, sub);

        Intent open = new Intent(this, MainActivity.class)
                .putExtra("open_function", R.layout.function_alarm);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 9003, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, AlarmCountdownService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 9004, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openTap = new Intent(this, AlarmCountdownService.class).setAction(ACTION_OPEN);
        PendingIntent openTapPi = PendingIntent.getService(
                this, 9006, openTap, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.btn_notify_cancel, stopPi);
        views.setOnClickPendingIntent(R.id.btn_notify_open, openPi);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm_24)
                .setContentTitle(getString(R.string.alarm_page_title))
                .setContentText(sub)
                .setCustomContentView(views)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openTapPi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build();
    }
}

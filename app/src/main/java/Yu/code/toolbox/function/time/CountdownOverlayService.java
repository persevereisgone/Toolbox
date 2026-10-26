package Yu.code.toolbox.function.time;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.provider.Settings;

import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

public class CountdownOverlayService extends Service {

    public static final String ACTION_START = "Yu.code.toolbox.action.START_OVERLAY";
    public static final String ACTION_STOP = "Yu.code.toolbox.action.STOP_OVERLAY";
    private static final String CHANNEL_ID = "lock_timer_channel_v2";
    private static final int NOTIFICATION_ID = 2001;
    private static final int REQ_CODE_LOCK = 1001;

    private WindowManager windowManager;
    private View overlayView;
    private View menuView;
    private WindowManager.LayoutParams overlayParams;
    private WindowManager.LayoutParams menuParams;
    private View edgeView;
    private WindowManager.LayoutParams edgeParams;
    private Handler handler;
    private Runnable ticker;

    private boolean dragging = false;
    private float touchStartX, touchStartY;
    private int paramStartX, paramStartY;
    private Runnable longPressRunnable;
    private static final long LONG_PRESS_MS = 300;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopSelfSafely();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification(getRemainingText()));
        showOrUpdateOverlay();
        startTicker();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTicker();
        removeMenu();
        removeOverlay();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "定时返回桌面",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("到时将自动回到主屏幕");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent stopIntent = new Intent(this, CountdownOverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_black_24dp)
                .setContentTitle("定时返回桌面进行中")
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openPi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(0, "取消定时", stopPi)
                .build();
    }

    private void showOrUpdateOverlay() {
        if (overlayView == null) {
            // 若无悬浮窗权限，仅显示常驻通知，跳过悬浮窗
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                return;
            }
            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.overlay_countdown, null);

            int type;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            overlayParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            overlayParams.gravity = Gravity.TOP | Gravity.START;
            int[] pos = loadOverlayPosition();
            int margin = dp(8);
            int topInset = getStatusBarHeight();
            int[] screen = getScreenSize();
            int sw = screen[0];
            int sh = screen[1];
            // X 默认贴左侧 8dp，或使用保存值后裁剪
            int x = pos[0];
            if (x <= 0) x = margin;
            x = Math.max(margin, Math.min(x, sw - margin));
            // Y 默认放在状态栏下方 8dp，或使用保存值后裁剪
            int y = pos[1] > 0 ? pos[1] : (topInset + margin);
            int minY = topInset + margin;
            int maxY = Math.max(minY, sh - margin);
            y = Math.max(minY, Math.min(y, maxY));
            overlayParams.x = x;
            overlayParams.y = y;

            overlayView.setOnTouchListener(new View.OnTouchListener() {
                @Override public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            dragging = false;
                            touchStartX = event.getRawX();
                            touchStartY = event.getRawY();
                            paramStartX = overlayParams.x;
                            paramStartY = overlayParams.y;
                            if (longPressRunnable != null) handler.removeCallbacks(longPressRunnable);
                            longPressRunnable = new Runnable() {
                                @Override public void run() { dragging = true; }
                            };
                            handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (dragging) {
                                int dx = (int) (event.getRawX() - touchStartX);
                                int dy = (int) (event.getRawY() - touchStartY);
                                overlayParams.x = paramStartX + dx;
                                overlayParams.y = paramStartY + dy;
                                windowManager.updateViewLayout(overlayView, overlayParams);
                                return true;
                            }
                            return false;
                        case MotionEvent.ACTION_UP:
                            handler.removeCallbacks(longPressRunnable);
                            if (dragging) {
                                dragging = false;
                                snapAndSaveOverlayPosition();
                                return true;
                            } else {
                                // 作为点击：切换菜单
                                toggleMenu();
                                return true;
                            }
                        default:
                            return false;
                    }
                }
            });

            windowManager.addView(overlayView, overlayParams);
        } else {
            updateOverlayText();
        }
    }

    private void showMenu() {
        if (menuView != null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        menuView = inflater.inflate(R.layout.overlay_menu, null);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.START;
        // 计算菜单安全位置：测量尺寸并避免越界与状态栏/刘海
        menuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int mw = menuView.getMeasuredWidth();
        int mh = menuView.getMeasuredHeight();
        int[] screen = getScreenSize();
        int sw = screen[0];
        int sh = screen[1];
        int margin = dp(8);
        int topInset = getStatusBarHeight();
        int x = overlayParams.x;
        int y = overlayParams.y + dp(36);
        x = Math.max(margin, Math.min(x, sw - mw - margin));
        y = Math.max(topInset + margin, Math.min(y, sh - mh - margin));
        menuParams.x = x;
        menuParams.y = y;

        menuView.findViewById(R.id.menu_open_app).setOnClickListener(v -> {
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            open.putExtra("NAVIGATE_TO_TIMER", true);
            startActivity(open);
            removeMenu();
        });
        menuView.findViewById(R.id.menu_cancel).setOnClickListener(v -> {
            cancelScheduledLockInternal();
            removeMenu();
        });
        menuView.findViewById(R.id.menu_hide_edge).setOnClickListener(v -> {
            collapseToEdge();
        });

        windowManager.addView(menuView, menuParams);
    }

    private void removeMenu() {
        if (menuView != null) {
            windowManager.removeView(menuView);
            menuView = null;
        }
    }

    private void toggleMenu() {
        if (menuView == null) showMenu(); else removeMenu();
    }

    private void removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private void updateOverlayText() {
        String shortText = getShortRemainingText();
        if (overlayView != null) {
            TextView tv = overlayView.findViewById(R.id.tv_countdown);
            tv.setText(shortText);
        }
        if (edgeView != null) {
            TextView tv = edgeView.findViewById(R.id.tv_edge_time);
            tv.setText(shortText);
        }
    }

    private String getShortRemainingText() {
        long remaining = getRemainingMs();
        if (remaining <= 0) return "00:00";
        long totalSeconds = remaining / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return h + ":" + String.format(Locale.getDefault(), "%02d", m);
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    /** 收起到侧边:藏起主悬浮窗,只留窄边小标签,点标签恢复 */
    private void collapseToEdge() {
        removeMenu();
        if (edgeView != null) return;
        int[] screen = getScreenSize();
        int sw = screen[0];
        int sh = screen[1];
        int margin = dp(8);
        int topInset = getStatusBarHeight();
        boolean toRight = overlayParams != null && overlayParams.x + 100 > sw / 2;

        LayoutInflater inflater = LayoutInflater.from(this);
        edgeView = inflater.inflate(R.layout.overlay_edge_tab, null);
        TextView tv = edgeView.findViewById(R.id.tv_edge_time);
        tv.setText(getRemainingText());

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        edgeParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        edgeParams.gravity = Gravity.TOP | Gravity.START;
        edgeView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int ew = edgeView.getMeasuredWidth();
        int eh = edgeView.getMeasuredHeight();
        int y = overlayParams != null ? overlayParams.y : (topInset + margin);
        y = Math.max(topInset + margin, Math.min(y, Math.max(topInset + margin, sh - eh - margin)));
        edgeParams.x = toRight ? Math.max(margin, sw - ew - 2) : 2;
        edgeParams.y = y;

        edgeView.setOnClickListener(v -> expandFromEdge());
        windowManager.addView(edgeView, edgeParams);
        removeOverlay();
    }

    private void expandFromEdge() {
        removeEdgeTab();
        showOrUpdateOverlay();
        if (overlayView != null && edgeParams != null) {
            overlayParams.x = edgeParams.x;
            overlayParams.y = edgeParams.y;
            windowManager.updateViewLayout(overlayView, overlayParams);
            saveOverlayPosition(overlayParams.x, overlayParams.y);
        }
    }

    private void removeEdgeTab() {
        if (edgeView != null) {
            try { windowManager.removeView(edgeView); } catch (Exception ignored) {}
            edgeView = null;
        }
    }

    private void startTicker() {
        if (ticker != null) return;
        ticker = new Runnable() {
            @Override public void run() {
                String text = getRemainingText();
                Notification notification = buildNotification(text);
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NOTIFICATION_ID, notification);
                updateOverlayText();
                long sec = getRemainingMs() / 1000;
                if (sec >= 0 && (sec % 30) == 0) TimerHomeWidgetProvider.refreshAll(CountdownOverlayService.this);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(ticker);
    }

    private void stopTicker() {
        if (ticker != null) {
            handler.removeCallbacks(ticker);
            ticker = null;
        }
    }

    private String getRemainingText() {
        long remaining = getRemainingMs();
        if (remaining <= 0) return "无进行中的定时";
        return "剩余 " + formatDuration(remaining) + " 后回到主屏";
    }

    private long getRemainingMs() {
        return getSharedPreferences("lock_prefs", MODE_PRIVATE)
                .getLong("lock_trigger_at", -1L) - System.currentTimeMillis();
    }

    private String formatDuration(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            if (minutes > 0) return hours + "小时" + minutes + "分钟";
            return hours + "小时";
        }
        if (minutes > 0) {
            if (seconds > 0) return minutes + "分钟" + seconds + "秒";
            return minutes + "分钟";
        }
        return seconds + "秒";
    }

    private int dp(int v) {
        return (int) (getResources().getDisplayMetrics().density * v + 0.5f);
    }

    private void stopSelfSafely() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        removeMenu();
        removeEdgeTab();
        removeOverlay();
        stopSelf();
    }

    private void saveOverlayPosition(int x, int y) {
        getSharedPreferences("lock_prefs", MODE_PRIVATE)
                .edit().putInt("overlay_x", x).putInt("overlay_y", y).apply();
    }

    private int[] loadOverlayPosition() {
        int x = getSharedPreferences("lock_prefs", MODE_PRIVATE).getInt("overlay_x", 0);
        int y = getSharedPreferences("lock_prefs", MODE_PRIVATE).getInt("overlay_y", 0);
        return new int[]{x, y};
    }

    private void cancelScheduledLockInternal() {
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, LockAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                REQ_CODE_LOCK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (am != null) am.cancel(pi);
        getSharedPreferences("lock_prefs", MODE_PRIVATE).edit()
                .remove("lock_trigger_at").putBoolean("lock_restore_pending", false).apply();
        TimerHomeWidgetProvider.refreshAll(this);
        stopSelfSafely();
    }

    // 吸附与安全位置相关工具
    private void snapAndSaveOverlayPosition() {
        // 确保已测量
        int ow = overlayView.getWidth();
        int oh = overlayView.getHeight();
        if (ow == 0 || oh == 0) {
            overlayView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            ow = overlayView.getMeasuredWidth();
            oh = overlayView.getMeasuredHeight();
        }
        int[] screen = getScreenSize();
        int sw = screen[0];
        int sh = screen[1];
        int margin = dp(8);
        int topInset = getStatusBarHeight();

        // 贴左右边
        int leftX = margin;
        int rightX = Math.max(margin, sw - ow - margin);
        int targetX = (overlayParams.x + ow / 2 <= sw / 2) ? leftX : rightX;

        // 限制Y在安全区域
        int minY = topInset + margin;
        int maxY = Math.max(minY, sh - oh - margin);
        int targetY = Math.min(Math.max(overlayParams.y, minY), maxY);

        overlayParams.x = targetX;
        overlayParams.y = targetY;
        windowManager.updateViewLayout(overlayView, overlayParams);
        saveOverlayPosition(overlayParams.x, overlayParams.y);
    }

    private int[] getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(dm);
        return new int[]{dm.widthPixels, dm.heightPixels};
    }

    private int getStatusBarHeight() {
        int result = dp(24); // 合理缺省
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) result = getResources().getDimensionPixelSize(resId);
        return result;
    }
}

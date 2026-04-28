package Yu.code.toolbox.function.recorder_photo;

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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.provider.Settings;

import java.io.File;

import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

public class ScreenRecorderOverlayService extends Service {

    public static final String ACTION_START = "Yu.code.toolbox.action.START_SCREEN_RECORDER_OVERLAY";
    public static final String ACTION_STOP = "Yu.code.toolbox.action.STOP_SCREEN_RECORDER_OVERLAY";
    public static final String ACTION_PAUSE = "Yu.code.toolbox.action.PAUSE_SCREEN_RECORDER";
    public static final String ACTION_RESUME = "Yu.code.toolbox.action.RESUME_SCREEN_RECORDER";
    public static final String EXTRA_RECORDING_TIME = "Yu.code.toolbox.extra.RECORDING_TIME";
    public static final String EXTRA_START_RECORDING = "Yu.code.toolbox.extra.START_RECORDING";
    public static final String EXTRA_OUTPUT_FILE = "Yu.code.toolbox.extra.OUTPUT_FILE";
    private static final String CHANNEL_ID = "screen_recorder_channel";
    private static final int NOTIFICATION_ID = 2002;

    private WindowManager windowManager;
    private View overlayView;
    private View menuView;
    private WindowManager.LayoutParams overlayParams;
    private WindowManager.LayoutParams menuParams;
    private Handler handler;
    private Runnable ticker;

    private boolean dragging = false;
    private float touchStartX, touchStartY;
    private int paramStartX, paramStartY;
    private Runnable longPressRunnable;
    private static final long LONG_PRESS_MS = 300;

    private long recordingTime = 30000; // 默认30秒
    private long startTime = 0;
    private boolean isPaused = false;
    private long pausedTime = 0; // 暂停时累计的时间
    private File outputFile; // 当前录制的文件

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
        android.util.Log.d("ScreenRecorder", "Service onStartCommand: action=" + action);
        
        if (ACTION_STOP.equals(action)) {
            // 发送停止广播给 Manager
            Intent stopBroadcast = new Intent("Yu.code.toolbox.action.SCREEN_RECORDER_STOP");
            stopBroadcast.setPackage(getPackageName());
            sendBroadcast(stopBroadcast);
            android.util.Log.d("ScreenRecorder", "Service: 已发送停止广播");
            
            stopSelfSafely();
            return START_NOT_STICKY;
        }
        
        if (ACTION_PAUSE.equals(action)) {
            // 发送暂停广播给 Manager
            Intent pauseBroadcast = new Intent("Yu.code.toolbox.action.SCREEN_RECORDER_PAUSE");
            pauseBroadcast.setPackage(getPackageName());
            sendBroadcast(pauseBroadcast);
            android.util.Log.d("ScreenRecorder", "Service: 已发送暂停广播");
            return START_STICKY;
        }
        
        if (ACTION_RESUME.equals(action)) {
            // 发送恢复广播给 Manager
            Intent resumeBroadcast = new Intent("Yu.code.toolbox.action.SCREEN_RECORDER_PAUSE");
            resumeBroadcast.setPackage(getPackageName());
            sendBroadcast(resumeBroadcast);
            android.util.Log.d("ScreenRecorder", "Service: 已发送恢复广播");
            return START_STICKY;
        }
        
        // 处理暂停状态更新
        if ("Yu.code.toolbox.action.UPDATE_PAUSE_STATE".equals(action)) {
            if (intent != null) {
                isPaused = intent.getBooleanExtra("is_paused", false);
                android.util.Log.d("ScreenRecorder", "Service: 更新暂停状态 isPaused=" + isPaused);
                if (isPaused) {
                    // 暂停时记录当前已录制的时间
                    pausedTime = System.currentTimeMillis() - startTime;
                    android.util.Log.d("ScreenRecorder", "Service: 暂停，已录制时间=" + pausedTime + "ms");
                } else {
                    // 恢复时调整开始时间
                    if (pausedTime > 0) {
                        startTime = System.currentTimeMillis() - pausedTime;
                        android.util.Log.d("ScreenRecorder", "Service: 恢复，调整开始时间，已录制=" + pausedTime + "ms");
                    }
                }
                updateOverlayText();
            }
            return START_STICKY;
        }

        // 获取录制时间
        if (intent != null) {
            recordingTime = intent.getLongExtra(EXTRA_RECORDING_TIME, 30000);
            String filePath = intent.getStringExtra(EXTRA_OUTPUT_FILE);
            if (filePath != null) {
                outputFile = new File(filePath);
            }
        }
        startTime = System.currentTimeMillis();
        isPaused = false;
        pausedTime = 0;

        startForeground(NOTIFICATION_ID, buildNotification(getRemainingText()));
        showOrUpdateOverlay();
        startTicker();
        
        // 通知启动录屏
        if (intent != null && intent.getBooleanExtra(EXTRA_START_RECORDING, false)) {
            Intent broadcastIntent = new Intent("Yu.code.toolbox.action.SCREEN_RECORDER_SERVICE_STARTED");
            broadcastIntent.setPackage(getPackageName());
            sendBroadcast(broadcastIntent);
            android.util.Log.d("ScreenRecorder", "Service: 已发送启动录屏广播");
        }
        
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
                    "屏幕录制",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("显示屏幕录制状态和剩余时间");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent stopIntent = new Intent(this, ScreenRecorderOverlayService.class);
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
                .setContentTitle("屏幕录制进行中")
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openPi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(0, "停止录制", stopPi)
                .build();
    }

    private void showOrUpdateOverlay() {
        if (overlayView == null) {
            // 若无悬浮窗权限，仅显示常驻通知，跳过悬浮窗
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                return;
            }
            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.overlay_screen_recorder, null);

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
            overlayParams.gravity = Gravity.TOP | Gravity.END;
            int margin = dp(16);
            int topInset = getStatusBarHeight();
            overlayParams.x = -margin;
            overlayParams.y = topInset + margin;

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
                                // 对于Gravity.END，x坐标是负值，需要反转拖动方向
                                overlayParams.x = paramStartX - dx;
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

    private void removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private void showMenu() {
        if (menuView != null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        menuView = inflater.inflate(R.layout.overlay_screen_recorder_menu, null);

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
        
        // 测量菜单尺寸
        menuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int menuWidth = menuView.getMeasuredWidth();
        int menuHeight = menuView.getMeasuredHeight();
        
        // 获取屏幕尺寸
        int[] screen = getScreenSize();
        int screenWidth = screen[0];
        int screenHeight = screen[1];
        int margin = dp(8);
        int topInset = getStatusBarHeight();
        
        // 确保悬浮窗已测量
        if (overlayView.getWidth() == 0 || overlayView.getHeight() == 0) {
            overlayView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        }
        int overlayWidth = overlayView.getMeasuredWidth();
        int overlayHeight = overlayView.getMeasuredHeight();
        
        // 统一使用 TOP | END 的 gravity，简化坐标计算
        menuParams.gravity = Gravity.TOP | Gravity.END;
        
        // 计算悬浮窗在屏幕上的实际位置（从右边缘算起的距离）
        // overlayParams.x 是负值，表示距离右边缘的距离
        int overlayRightMargin = Math.abs(overlayParams.x);
        int overlayTopMargin = overlayParams.y;
        
        // 计算悬浮窗的左边缘位置（从屏幕左边缘算起）
        int overlayLeftPos = screenWidth - overlayRightMargin - overlayWidth;
        
        // 计算悬浮窗的中心位置
        int overlayCenterX = overlayLeftPos + overlayWidth / 2;
        
        // 计算菜单的中心位置，使其与悬浮窗水平居中对齐
        int menuCenterX = overlayCenterX;
        
        // 计算菜单的左边缘位置
        int menuLeftPos = menuCenterX - menuWidth / 2;
        
        // 边界检查：确保菜单不超出屏幕
        if (menuLeftPos < margin) {
            menuLeftPos = margin;
        } else if (menuLeftPos + menuWidth > screenWidth - margin) {
            menuLeftPos = screenWidth - margin - menuWidth;
        }
        
        // 转换为从右边缘算起的距离（负值）
        int menuX = -(screenWidth - menuLeftPos - menuWidth);
        
        // 计算菜单的垂直位置：显示在悬浮窗正下方
        int menuY = overlayTopMargin + overlayHeight + margin;
        
        // 垂直边界检查
        if (menuY + menuHeight > screenHeight - margin) {
            // 如果下方空间不足，显示在悬浮窗上方
            menuY = overlayTopMargin - menuHeight - margin;
        }
        // 确保不超出顶部
        menuY = Math.max(topInset + margin, menuY);
        
        menuParams.x = menuX;
        menuParams.y = menuY;

        // 更新暂停/恢复按钮文本
        TextView menuPause = menuView.findViewById(R.id.menu_pause);
        if (isPaused) {
            menuPause.setText("▶ 恢复录制");
        } else {
            menuPause.setText("⏸ 暂停录制");
        }

        menuPause.setOnClickListener(v -> {
            // 通过 Service 转发命令
            android.util.Log.d("ScreenRecorder", "菜单：点击暂停/恢复按钮，当前状态 isPaused=" + isPaused);
            Intent serviceIntent = new Intent(this, ScreenRecorderOverlayService.class);
            if (isPaused) {
                serviceIntent.setAction(ACTION_RESUME);
                android.util.Log.d("ScreenRecorder", "菜单：发送恢复命令到 Service");
            } else {
                serviceIntent.setAction(ACTION_PAUSE);
                android.util.Log.d("ScreenRecorder", "菜单：发送暂停命令到 Service");
            }
            startService(serviceIntent);
            removeMenu();
        });
        
        menuView.findViewById(R.id.menu_stop).setOnClickListener(v -> {
            // 通过 Service 转发命令
            android.util.Log.d("ScreenRecorder", "菜单：点击停止按钮");
            Intent serviceIntent = new Intent(this, ScreenRecorderOverlayService.class);
            serviceIntent.setAction(ACTION_STOP);
            startService(serviceIntent);
            android.util.Log.d("ScreenRecorder", "菜单：发送停止命令到 Service");
            removeMenu();
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

    private void updateOverlayText() {
        if (overlayView != null) {
            TextView tvStatus = overlayView.findViewById(R.id.tv_recording_status);
            TextView tvTime = overlayView.findViewById(R.id.tv_remaining_time);
            
            if (isPaused) {
                tvStatus.setText("⏸ 已暂停");
            } else {
                tvStatus.setText("🔴 录屏中");
            }
            
            // 显示已录制时长和文件大小
            String timeText = getElapsedText();
            String fileSizeText = getFileSizeText();
            tvTime.setText(timeText + " | " + fileSizeText);
        }
    }
    
    private String getElapsedText() {
        long elapsed;
        if (isPaused) {
            elapsed = pausedTime;
        } else {
            elapsed = System.currentTimeMillis() - startTime;
        }
        return formatDuration(elapsed);
    }
    
    private String getFileSizeText() {
        if (outputFile != null && outputFile.exists()) {
            long fileSize = outputFile.length();
            return formatFileSize(fileSize);
        }
        return "0 MB";
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void startTicker() {
        if (ticker != null) return;
        ticker = new Runnable() {
            @Override public void run() {
                String text = getRemainingText();
                String notificationTitle = isPaused ? "屏幕录制已暂停" : "屏幕录制进行中";
                Notification notification = buildNotification(text);
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NOTIFICATION_ID, notification);
                updateOverlayText();
                
                // 暂停时不检查录制完成，但继续更新显示
                if (!isPaused) {
                    // 检查是否录制完成
                    if (System.currentTimeMillis() - startTime >= recordingTime) {
                        stopSelfSafely();
                        return;
                    }
                }
                
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
        long elapsed;
        if (isPaused) {
            // 暂停时显示暂停时刻的剩余时间（使用已录制的时间）
            elapsed = pausedTime;
        } else {
            elapsed = System.currentTimeMillis() - startTime;
        }
        long remaining = Math.max(0, recordingTime - elapsed);
        return formatDuration(remaining);
    }

    private String formatDuration(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private int dp(int v) {
        return (int) (getResources().getDisplayMetrics().density * v + 0.5f);
    }

    private void stopSelfSafely() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        removeOverlay();
        stopSelf();
    }

    private void snapAndSaveOverlayPosition() {
        // 确保已测量
        int overlayWidth = overlayView.getWidth();
        int overlayHeight = overlayView.getHeight();
        if (overlayWidth == 0 || overlayHeight == 0) {
            overlayView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            overlayWidth = overlayView.getMeasuredWidth();
            overlayHeight = overlayView.getMeasuredHeight();
        }
        
        int[] screen = getScreenSize();
        int screenWidth = screen[0];
        int screenHeight = screen[1];
        int margin = dp(16);
        int topInset = getStatusBarHeight();

        // 使用 Gravity.END 坐标系统：
        // x 是负值，表示从右边缘向左的距离
        // x = 0 表示悬浮窗右边缘贴着屏幕右边缘
        // x = -100 表示悬浮窗右边缘距离屏幕右边缘 100px
        
        // 计算悬浮窗当前的右边缘位置（从屏幕右边缘算起的距离）
        int currentRightMargin = Math.abs(overlayParams.x);
        
        // 计算悬浮窗左边缘的位置（从屏幕左边缘算起）
        int currentLeftPos = screenWidth - currentRightMargin - overlayWidth;
        
        // 计算悬浮窗中心位置
        int currentCenterX = currentLeftPos + overlayWidth / 2;
        
        // 判断更靠近左边还是右边
        int screenCenterX = screenWidth / 2;
        
        int targetX;
        if (currentCenterX < screenCenterX) {
            // 吸附到左边
            // 左边缘位置应该是 margin
            // 右边缘位置 = margin + overlayWidth
            // 从屏幕右边缘算起的距离 = screenWidth - (margin + overlayWidth)
            targetX = -(screenWidth - margin - overlayWidth);
            android.util.Log.d("ScreenRecorder", "吸附到左边: targetX=" + targetX + 
                ", screenWidth=" + screenWidth + ", margin=" + margin + ", overlayWidth=" + overlayWidth);
        } else {
            // 吸附到右边
            // 右边缘距离屏幕右边缘 margin
            targetX = -margin;
            android.util.Log.d("ScreenRecorder", "吸附到右边: targetX=" + targetX + ", margin=" + margin);
        }

        // 限制Y在安全区域
        int minY = topInset + margin;
        int maxY = Math.max(minY, screenHeight - overlayHeight - margin);
        int targetY = Math.min(Math.max(overlayParams.y, minY), maxY);

        android.util.Log.d("ScreenRecorder", "最终位置: x=" + targetX + ", y=" + targetY + 
            ", overlayWidth=" + overlayWidth + ", overlayHeight=" + overlayHeight);

        overlayParams.x = targetX;
        overlayParams.y = targetY;
        windowManager.updateViewLayout(overlayView, overlayParams);
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
package Yu.code.toolbox.function.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;
import Yu.code.toolbox.function.time.CountdownOverlayService;
import Yu.code.toolbox.function.time.LockAlarmReceiver;

public class TimerManager {

    private static final String PREFS_NAME = "lock_prefs";
    private static final String KEY_TRIGGER_AT = "lock_trigger_at";
    private static final int REQ_CODE_LOCK = 1001;
    private static final int REQ_CODE_POST_NOTI = 3001;
    private static final int REQ_CODE_EXACT_ALARM = 3002;

    private Context context;
    private View rootView;
    private SharedPreferences prefs;
    private Handler handler;
    private Runnable updateRunnable;

    private TextView tvTimerStatus;
    private TextView tvRemainingTime;
    private EditText etCustomHours;
    private EditText etCustomMinutes;
    private Button btnCancelTimer;

    public TimerManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.handler = new Handler();

        initViews();
        setupClickListeners();
        updateTimerStatus();
        startStatusUpdater();
    }

    private void initViews() {
        tvTimerStatus = rootView.findViewById(R.id.tv_timer_status);
        tvRemainingTime = rootView.findViewById(R.id.tv_remaining_time);
        etCustomHours = rootView.findViewById(R.id.et_custom_hours);
        etCustomMinutes = rootView.findViewById(R.id.et_custom_minutes);
        btnCancelTimer = rootView.findViewById(R.id.btn_cancel_timer);
    }

    private void setupClickListeners() {
        // 预设时长按钮
        rootView.findViewById(R.id.btn_15min).setOnClickListener(v -> setTimer(15 * 60 * 1000L));
        rootView.findViewById(R.id.btn_30min).setOnClickListener(v -> setTimer(30 * 60 * 1000L));
        rootView.findViewById(R.id.btn_45min).setOnClickListener(v -> setTimer(45 * 60 * 1000L));
        rootView.findViewById(R.id.btn_60min).setOnClickListener(v -> setTimer(60 * 60 * 1000L));
        rootView.findViewById(R.id.btn_90min).setOnClickListener(v -> setTimer(90 * 60 * 1000L));
        rootView.findViewById(R.id.btn_120min).setOnClickListener(v -> setTimer(120 * 60 * 1000L));

        // 自定义时长按钮
        rootView.findViewById(R.id.btn_set_custom).setOnClickListener(v -> setCustomTimer());

        // 取消定时按钮
        btnCancelTimer.setOnClickListener(v -> cancelTimer());

        // 刷新状态按钮
        rootView.findViewById(R.id.btn_refresh_status).setOnClickListener(v -> updateTimerStatus());
    }

    private void setTimer(long delayMillis) {
        if (!ensureOverlayPermission()) return;
        ensureNotificationPermissionIfNeeded();
        if (!ensureExactAlarmPermission()) return;

        long triggerAt = System.currentTimeMillis() + delayMillis;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildLockPendingIntent();

        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }

        saveTriggerAt(triggerAt);
        startCountdownService();
        updateTimerStatus();
        Toast.makeText(context, "已设置：" + formatDuration(delayMillis) + "后回到主屏", Toast.LENGTH_LONG).show();
    }

    private void setCustomTimer() {
        String hoursStr = etCustomHours.getText().toString().trim();
        String minutesStr = etCustomMinutes.getText().toString().trim();

        int hours = 0;
        int minutes = 0;

        if (!hoursStr.isEmpty()) {
            try {
                hours = Integer.parseInt(hoursStr);
                if (hours < 0) {
                    Toast.makeText(context, "小时不能为负数", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的小时数", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!minutesStr.isEmpty()) {
            try {
                minutes = Integer.parseInt(minutesStr);
                if (minutes < 0 || minutes >= 60) {
                    Toast.makeText(context, "分钟必须在0-59之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的分钟数", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (hours == 0 && minutes == 0) {
            Toast.makeText(context, "请设置大于0的时长", Toast.LENGTH_SHORT).show();
            return;
        }

        long totalMinutes = (long) hours * 60L + minutes;
        setTimer(totalMinutes * 60L * 1000L);

        // 清空输入
        etCustomHours.setText("");
        etCustomMinutes.setText("");
    }

    private void cancelTimer() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildLockPendingIntent();
        if (am != null) {
            am.cancel(pi);
        }
        clearTriggerAt();
        stopCountdownService();
        updateTimerStatus();
        Toast.makeText(context, "已取消定时返回桌面", Toast.LENGTH_LONG).show();
    }

    private void updateTimerStatus() {
        long remaining = getRemainingMs();
        if (remaining > 0) {
            tvTimerStatus.setText("定时进行中");
            tvRemainingTime.setText("剩余：" + formatDurationWithSeconds(remaining));
            tvRemainingTime.setVisibility(View.VISIBLE);
            btnCancelTimer.setEnabled(true);
        } else {
            tvTimerStatus.setText("当前没有正在进行的定时");
            tvRemainingTime.setVisibility(View.GONE);
            btnCancelTimer.setEnabled(false);
        }
    }

    private void startStatusUpdater() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimerStatus();
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        handler.post(updateRunnable);
    }

    private void stopStatusUpdater() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private boolean ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (Settings.canDrawOverlays(context)) return true;
        Toast.makeText(context, "需要悬浮窗权限以显示倒计时", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
        return false;
    }

    private void ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 通知权限需要用户手动授予，这里只显示提示
                Toast.makeText(context, "需要通知权限以显示定时提醒", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null && am.canScheduleExactAlarms()) return true;
        Toast.makeText(context, "需要授予精准闹钟权限", Toast.LENGTH_LONG).show();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
            return false;
        }
        return true;
    }

    private PendingIntent buildLockPendingIntent() {
        Intent intent = new Intent(context, LockAlarmReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                REQ_CODE_LOCK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void startCountdownService() {
        Intent svc = new Intent(context, CountdownOverlayService.class);
        svc.setAction(CountdownOverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }

    private void stopCountdownService() {
        Intent svc = new Intent(context, CountdownOverlayService.class);
        svc.setAction(CountdownOverlayService.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }

    private void saveTriggerAt(long triggerAt) {
        prefs.edit().putLong(KEY_TRIGGER_AT, triggerAt).apply();
    }

    private void clearTriggerAt() {
        prefs.edit().remove(KEY_TRIGGER_AT).apply();
    }

    private long getRemainingMs() {
        long triggerAt = prefs.getLong(KEY_TRIGGER_AT, -1L);
        if (triggerAt <= 0) return -1L;
        return triggerAt - System.currentTimeMillis();
    }

    private String formatDuration(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            if (minutes > 0) return String.format(Locale.getDefault(), "%d小时%d分钟", hours, minutes);
            return String.format(Locale.getDefault(), "%d小时", hours);
        }
        if (minutes > 0) {
            if (seconds > 0) return String.format(Locale.getDefault(), "%d分钟%d秒", minutes, seconds);
            return String.format(Locale.getDefault(), "%d分钟", minutes);
        }
        return String.format(Locale.getDefault(), "%d秒", seconds);
    }

    private String formatDurationWithSeconds(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d小时%d分钟%d秒", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d分钟%d秒", minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%d秒", seconds);
    }

    public void onDestroy() {
        stopStatusUpdater();
    }
}

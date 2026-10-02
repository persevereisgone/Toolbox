package Yu.code.toolbox.function.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import java.util.Locale;

import Yu.code.toolbox.R;
import Yu.code.toolbox.function.time.CountdownOverlayService;
import Yu.code.toolbox.function.time.LockAlarmReceiver;

public class TimerManager {

    private static final String PREFS_NAME = "lock_prefs";
    private static final String KEY_TRIGGER_AT = "lock_trigger_at";
    private static final String KEY_AUTO_LOCK = "auto_lock_screen";
    private static final String KEY_LAST_HOURS = "last_custom_hours";
    private static final String KEY_LAST_MINUTES = "last_custom_minutes";
    private static final int REQ_CODE_LOCK = 1001;

    private Context context;
    private View rootView;
    private SharedPreferences prefs;
    private Handler handler;
    private Runnable updateRunnable;

    private TextView tvTimerStatus;
    private TextView tvRemainingTime;
    private Button btnCancelTimer;
    private androidx.appcompat.widget.SwitchCompat switchAutoLock;

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
        btnCancelTimer = rootView.findViewById(R.id.btn_cancel_timer);
        switchAutoLock = rootView.findViewById(R.id.switch_auto_lock);

        switchAutoLock.setChecked(false);
        prefs.edit().putBoolean(KEY_AUTO_LOCK, false).apply();
    }

    private void setupClickListeners() {
        rootView.findViewById(R.id.btn_15min).setOnClickListener(v -> setTimer(15 * 60 * 1000L));
        rootView.findViewById(R.id.btn_30min).setOnClickListener(v -> setTimer(30 * 60 * 1000L));
        rootView.findViewById(R.id.btn_45min).setOnClickListener(v -> setTimer(45 * 60 * 1000L));
        rootView.findViewById(R.id.btn_60min).setOnClickListener(v -> setTimer(60 * 60 * 1000L));
        rootView.findViewById(R.id.btn_90min).setOnClickListener(v -> setTimer(90 * 60 * 1000L));
        rootView.findViewById(R.id.btn_120min).setOnClickListener(v -> setTimer(120 * 60 * 1000L));

        rootView.findViewById(R.id.row_custom).setOnClickListener(v -> showCustomSheet());

        btnCancelTimer.setOnClickListener(v -> showCancelConfirm());

        // 状态每秒自动刷新,无需手动刷新键

        // 点大字查看剩余时间
        tvRemainingTime.setOnClickListener(v -> showRemainingDialog());

        switchAutoLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!isDeviceAdminActive()) {
                    Toast.makeText(context, "需要设备管理员权限才能自动锁屏", Toast.LENGTH_LONG).show();
                    requestDeviceAdminPermission();
                    buttonView.setChecked(false);
                    return;
                }
                Toast.makeText(context, "已开启：定时结束后自动锁屏", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "已关闭：定时结束后仅返回桌面", Toast.LENGTH_SHORT).show();
            }
            prefs.edit().putBoolean(KEY_AUTO_LOCK, isChecked).apply();
        });
    }

    /** 自定义时长 bottom-sheet:双滚轮 + 确定带结果 + 记忆上次 */
    private void showCustomSheet() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_sheet, null, false);
        android.widget.NumberPicker npHours = view.findViewById(R.id.np_hours);
        android.widget.NumberPicker npMinutes = view.findViewById(R.id.np_minutes);
        npHours.setMinValue(0);
        npHours.setMaxValue(12);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npHours.setValue(Math.max(0, Math.min(12, prefs.getInt(KEY_LAST_HOURS, 0))));
        npMinutes.setValue(Math.max(0, Math.min(59, prefs.getInt(KEY_LAST_MINUTES, 25))));
        npHours.setWrapSelectorWheel(true);
        npMinutes.setWrapSelectorWheel(true);

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        dialog.setContentView(view);

        Button btnOk = view.findViewById(R.id.btn_ok);
        View btnCancel = view.findViewById(R.id.btn_cancel);
        TextView tvError = view.findViewById(R.id.tv_sheet_error);
        Runnable refreshOk = () -> {
            long total = (long) npHours.getValue() * 60L + npMinutes.getValue();
            // 只保留时长,不加"确定 ·"前缀,防换行撑高按钮
            btnOk.setText(total > 0 ? formatDuration(total * 60L * 1000L) + "后" : "确定");
            tvError.setVisibility(View.GONE);
        };
        npHours.setOnValueChangedListener((p, o, n) -> refreshOk.run());
        npMinutes.setOnValueChangedListener((p, o, n) -> refreshOk.run());
        refreshOk.run();

        // 快捷预设:点即填滚轮(布局可选,不存在则跳过)
        setQuickPreset(view, R.id.btn_quick_15, npHours, npMinutes, 0, 15, refreshOk);
        setQuickPreset(view, R.id.btn_quick_25, npHours, npMinutes, 0, 25, refreshOk);
        setQuickPreset(view, R.id.btn_quick_45, npHours, npMinutes, 0, 45, refreshOk);
        setQuickPreset(view, R.id.btn_quick_60, npHours, npMinutes, 1, 0, refreshOk);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            int hours = npHours.getValue();
            int minutes = npMinutes.getValue();
            long totalMinutes = (long) hours * 60L + minutes;
            if (totalMinutes <= 0) {
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            prefs.edit().putInt(KEY_LAST_HOURS, hours).putInt(KEY_LAST_MINUTES, minutes).apply();
            dialog.dismiss();
            setTimer(totalMinutes * 60L * 1000L);
        });
        dialog.show();
    }

    /** 快捷预设:布局可选,找不到则跳过,避免崩溃 */
    private void setQuickPreset(View parent, int id,
                                android.widget.NumberPicker npHours,
                                android.widget.NumberPicker npMinutes,
                                int h, int m, Runnable refreshOk) {
        View v = parent.findViewById(id);
        if (v == null) return;
        v.setOnClickListener(x -> { npHours.setValue(h); npMinutes.setValue(m); refreshOk.run(); });
    }

    /** 取消确认:破坏性操作二次确认 */
    private void showCancelConfirm() {
        if (getRemainingMs() <= 0) {
            Toast.makeText(context, "当前没有正在进行的定时", Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("取消已设置的定时？")
                .setMessage("将取消闹钟并停止倒计时悬浮窗，此操作不可撤销。")
                .setNegativeButton("再想想", null)
                .setPositiveButton("确认取消", (d, w) -> cancelTimer())
                .create();
        dialog.setOnShowListener(d -> {
            // 破坏性确认键用 danger 色,避免与默认 positive 主色混淆
            android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) positive.setTextColor(context.getResources().getColor(R.color.app_danger));
        });
        dialog.show();
    }

    /** 点大字查看剩余时间 */
    private void showRemainingDialog() {
        long remaining = getRemainingMs();
        if (remaining <= 0) {
            Toast.makeText(context, "当前没有正在进行的定时", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("剩余时间")
                .setMessage("剩余" + formatDurationWithSeconds(remaining) + "后回到主屏")
                .setNegativeButton("取消定时", (d, w) -> showCancelConfirm())
                .setPositiveButton("知道了", null)
                .show();
    }

    private boolean isDeviceAdminActive() {
        android.app.admin.DevicePolicyManager dpm =
            (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        android.content.ComponentName adminComponent =
            new android.content.ComponentName(context, LockDeviceAdminReceiver.class);
        return dpm != null && dpm.isAdminActive(adminComponent);
    }

    private void requestDeviceAdminPermission() {
        Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        android.content.ComponentName adminComponent =
            new android.content.ComponentName(context, LockDeviceAdminReceiver.class);
        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "授予设备管理员权限后，应用可以在定时结束时自动锁定屏幕。");
        context.startActivity(intent);
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
        prefs.edit().putLong("lock_total_ms", delayMillis).apply();
        startCountdownService();
        updateTimerStatus();
        TimerHomeWidgetProvider.refreshAll(context);
        Toast.makeText(context, "已设置：" + formatDuration(delayMillis) + "后回到主屏", Toast.LENGTH_LONG).show();
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
        TimerHomeWidgetProvider.refreshAll(context);
        Toast.makeText(context, "已取消定时返回桌面", Toast.LENGTH_LONG).show();
    }

    private void updateTimerStatus() {
        long remaining = getRemainingMs();
        if (remaining > 0) {
            tvTimerStatus.setText("定时进行中");
            tvRemainingTime.setText(formatDurationWithSeconds(remaining));
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
                handler.postDelayed(this, 1000);
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

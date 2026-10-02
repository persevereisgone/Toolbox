package Yu.code.toolbox.function.alarm;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Locale;
import Yu.code.toolbox.R;

/**
 * 闹钟主页（C 版）：hero 状态条 ＋ 一键定时宫格 ＋ 更多设置卡。
 * 倒计时默认单位：分钟（支持小数，0.5 = 30 秒）。
 */
public class AlarmFunctionManager {
    private final Context ctx;
    private final View root;
    private final AlarmStore store;

    private View hero;
    private TextView tvHeroMode, tvStatus, tvHeroSub;
    private Button btnCancel;
    private View permissionHint;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable ticker;
    private boolean lastRunning = false;
    private boolean firstRefresh = true;

    public AlarmFunctionManager(Context ctx, View root) {
        this.ctx = ctx;
        this.root = root;
        this.store = new AlarmStore(ctx);

        hero = root.findViewById(R.id.hero_alarm);
        tvHeroMode = root.findViewById(R.id.tv_alarm_hero_mode);
        tvStatus = root.findViewById(R.id.tv_alarm_status);
        tvHeroSub = root.findViewById(R.id.tv_alarm_hero_sub);
        btnCancel = root.findViewById(R.id.btn_alarm_cancel);
        permissionHint = root.findViewById(R.id.tv_alarm_permission_hint);

        EditText etMin = root.findViewById(R.id.et_alarm_minutes);
        // 一键清除：部分输入法在 numberDecimal 下退格失灵，兜底保证可删
        root.findViewById(R.id.btn_alarm_clear_input).setOnClickListener(v -> {
            etMin.setText("");
            etMin.requestFocus();
        });

        bindPreset(R.id.btn_preset_1, 1f);
        bindPreset(R.id.btn_preset_5, 5f);
        bindPreset(R.id.btn_preset_10, 10f);
        bindPreset(R.id.btn_preset_30, 30f);

        root.findViewById(R.id.btn_alarm_start_countdown).setOnClickListener(v -> {
            float minutes;
            try {
                minutes = Float.parseFloat(etMin.getText().toString().trim());
            } catch (Exception e) {
                Toast.makeText(ctx, ctx.getString(R.string.alarm_input_invalid), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AlarmStore.isValidMinutes(minutes)) {
                Toast.makeText(ctx, ctx.getString(R.string.alarm_input_invalid), Toast.LENGTH_SHORT).show();
                return;
            }
            final float value = minutes;
            if (!ensureExactPermission(() -> startCountdown(value))) return;
            startCountdown(value);
        });

        root.findViewById(R.id.btn_alarm_pick_time).setOnClickListener(v -> showTimeSheet());
        btnCancel.setOnClickListener(v -> {
            store.cancel(ctx);
            Toast.makeText(ctx, R.string.alarm_cancelled, Toast.LENGTH_SHORT).show();
            refresh();
        });
        permissionHint.setOnClickListener(v -> showPermissionSheet(null));

        ticker = new Runnable() {
            @Override public void run() {
                refresh();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(ticker);
        refresh();
    }

    private void bindPreset(int btnId, final float minutes) {
        root.findViewById(btnId).setOnClickListener(v -> {
            if (!ensureExactPermission(() -> startCountdown(minutes))) return;
            startCountdown(minutes);
        });
    }

    private void startCountdown(float minutes) {
        ensureNotificationPermission();
        store.setCountdownMinutes(ctx, minutes);
        Toast.makeText(ctx, R.string.alarm_countdown_started, Toast.LENGTH_SHORT).show();
        refresh();
    }

    /** 精准闹钟权限：有则 true；无则弹 sheet 并在用户去设置后继续 pendingOp。 */
    private boolean ensureExactPermission(Runnable pendingOp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                showPermissionSheet(pendingOp);
                return false;
            }
        }
        return true;
    }

    private void showPermissionSheet(Runnable pendingOp) {
        View sheet = LayoutInflater.from(ctx).inflate(R.layout.dialog_alarm_permission_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        dialog.setContentView(sheet);
        sheet.findViewById(R.id.btn_alarm_perm_later).setOnClickListener(v -> {
            dialog.dismiss();
            refresh();
        });
        sheet.findViewById(R.id.btn_alarm_perm_settings).setOnClickListener(v -> {
            dialog.dismiss();
            // ① 精准闹钟设置页
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + ctx.getPackageName()));
                ctx.startActivity(i);
            } catch (Exception ignored) {
            }
            // ② 电池优化白名单：锁屏/后台被杀的主因，一并申请
            try {
                android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(ctx.getPackageName())) {
                    Intent b = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + ctx.getPackageName()));
                    ctx.startActivity(b);
                }
            } catch (Exception ignored) {
            }
            if (pendingOp != null) pendingOp.run();
            refresh();
        });
        dialog.setOnDismissListener(d -> refresh());
        dialog.show();
    }

    /** 供组件"选时刻"直达：打开时刻选择 sheet。 */
    public void openTimeSheet() {
        showTimeSheet();
    }

    private void showTimeSheet() {        View sheet = LayoutInflater.from(ctx).inflate(R.layout.dialog_alarm_time_sheet, null);
        TimePicker tp = sheet.findViewById(R.id.tp_alarm_sheet);
        tp.setIs24HourView(true);
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        dialog.setContentView(sheet);
        sheet.findViewById(R.id.btn_alarm_sheet_cancel).setOnClickListener(v -> dialog.dismiss());
        sheet.findViewById(R.id.btn_alarm_sheet_ok).setOnClickListener(v -> {
            int h, m;
            if (Build.VERSION.SDK_INT >= 23) { h = tp.getHour(); m = tp.getMinute(); }
            else { h = tp.getCurrentHour(); m = tp.getCurrentMinute(); }
            dialog.dismiss();
            if (!ensureExactPermission(() -> applyRtc(h, m))) return;
            applyRtc(h, m);
        });
        dialog.show();
    }

    private void applyRtc(int h, int m) {
        ensureNotificationPermission();
        store.setRtc(ctx, h, m, String.format(Locale.getDefault(), "%02d:%02d", h, m));
        Toast.makeText(ctx, R.string.alarm_set_done, Toast.LENGTH_SHORT).show();
        refresh();
    }

    /** Android 13+ 常驻通知需要通知权限，没有就提示一次（不拦设置） */
    private void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(ctx, "需要通知权限以常驻显示剩余时间", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refresh() {
        boolean running = store.isRunning();
        long rem = store.getRemaining();
        String mode = store.getMode();

        // 背景只在运行态翻转时切换，避免每秒重建 drawable 引起整页 relayout 抢输入框焦点
        if (firstRefresh || running != lastRunning) {
            hero.setBackgroundResource(running ? R.drawable.bg_alarm_hero_run : R.drawable.bg_alarm_hero_idle);
            lastRunning = running;
            firstRefresh = false;
        }

        if (!running) {
            setTextIfChanged(tvHeroMode, getStr(R.string.alarm_mode_none));
            setColorIfChanged(tvHeroMode, R.color.app_text_tertiary);
            setTextIfChanged(tvStatus, getStr(R.string.alarm_hero_idle));
            setColorIfChanged(tvStatus, R.color.app_primary_dark);
            setTextIfChanged(tvHeroSub, getStr(R.string.alarm_hero_idle_sub));
            setColorIfChanged(tvHeroSub, R.color.app_text_tertiary);
            setEnabledIfChanged(btnCancel, false);
        } else {
            boolean isRtc = AlarmStore.MODE_RTC.equals(mode);
            setTextIfChanged(tvHeroMode, getStr(isRtc ? R.string.alarm_mode_rtc : R.string.alarm_mode_countdown));
            setColorIfChanged(tvHeroMode, R.color.app_text_inverse_secondary);
            setTextIfChanged(tvStatus, isRtc ? store.getTargetHm() : fmtHms(rem));
            setColorIfChanged(tvStatus, R.color.app_text_inverse);
            String detail = isRtc
                    ? store.getDayPrefix(ctx) + " " + store.getTargetHm()
                    : store.getLabel(ctx) + " · " + ctx.getString(R.string.alarm_remaining, fmtHms(rem));
            setTextIfChanged(tvHeroSub, detail);
            setColorIfChanged(tvHeroSub, R.color.app_text_inverse_secondary);
            setEnabledIfChanged(btnCancel, true);
        }

        boolean needPerm = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            needPerm = am != null && !am.canScheduleExactAlarms();
        }
        // 电池优化没加白名单也亮提示：锁屏/后台被杀的主因
        if (!needPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.os.PowerManager pm = (android.os.PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                needPerm = pm != null && !pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
            } catch (Exception ignored) {}
        }
        setVisibleIfChanged(permissionHint, needPerm);
    }

    private String getStr(int resId) { return ctx.getString(resId); }

    /** 内容没变就不 setText，避免每秒刷新引起 relayout 抢输入框焦点。 */
    private static void setTextIfChanged(TextView tv, CharSequence text) {
        if (text == null) return;
        CharSequence cur = tv.getText();
        if (cur == null || !text.toString().contentEquals(cur)) tv.setText(text);
    }

    private void setColorIfChanged(TextView tv, int colorRes) {
        int c = ctx.getResources().getColor(colorRes);
        if (tv.getCurrentTextColor() != c) tv.setTextColor(c);
    }

    private static void setEnabledIfChanged(View v, boolean enabled) {
        if (v.isEnabled() != enabled) v.setEnabled(enabled);
    }

    private static void setVisibleIfChanged(View v, boolean visible) {
        int want = visible ? View.VISIBLE : View.GONE;
        if (v.getVisibility() != want) v.setVisibility(want);
    }

    static String fmtHms(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        if (h > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", h, (s % 3600) / 60, s % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    public void onDestroy() {
        handler.removeCallbacks(ticker);
    }
}

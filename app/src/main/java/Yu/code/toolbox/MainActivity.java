package Yu.code.toolbox;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.GridView;
import android.widget.PopupWindow;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import Yu.code.toolbox.databinding.ActivityMainBinding;
import Yu.code.toolbox.function.time.CountdownOverlayService;
import Yu.code.toolbox.function.time.LockAlarmReceiver;
import Yu.code.toolbox.ui.PanelGridAdapter;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.AdapterView;
import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import android.widget.NumberPicker;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // 悬浮面板相关变量
    private PopupWindow mPopupWindow;
    private GridView mGridView;
    private PanelGridAdapter mAdapter;
    private static final String PREFS_NAME = "lock_prefs";
    private static final String KEY_TRIGGER_AT = "lock_trigger_at";
    private static final int REQ_CODE_LOCK = 1001;
    private static final int REQ_CODE_POST_NOTI = 3001;
    private static final int REQ_CODE_EXACT_ALARM = 3002;
    private static final String KEY_LAST_HOURS = "last_custom_hours";
    private static final String KEY_LAST_MINUTES = "last_custom_minutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置ActionBar但隐藏标题
        setSupportActionBar(binding.appBarMain.toolbar);
        // 设置导航栏为白色
        binding.appBarMain.toolbar.setBackgroundColor(getResources().getColor(android.R.color.white));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // 设置导航图标颜色为黑色
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
            // 额外设置导航图标色调
            android.graphics.drawable.Drawable navigationIcon = binding.appBarMain.toolbar.getNavigationIcon();
            if (navigationIcon != null) {
                navigationIcon.setColorFilter(getResources().getColor(android.R.color.black), android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        // 设置状态栏文字颜色为黑色
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11及以上版本
            getWindow().getInsetsController().setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android 6.0到Android 10版本
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 初始化FAB点击事件
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPopupWindow != null && mPopupWindow.isShowing()) {
                    mPopupWindow.dismiss();
                } else {
                    showFloatingPanel(view);
                }
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // 如从悬浮窗菜单带参跳回，自动弹出对话框
        handleOpenLockDialogIfNeeded(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenLockDialogIfNeeded(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 若已设置定时但服务未展示，自动拉起服务（修复权限授权后未手动重设的场景）
        if (getRemainingMs() > 0) {
            ensureNotificationPermissionIfNeeded();
            Intent svc = new Intent(this, CountdownOverlayService.class);
            svc.setAction(CountdownOverlayService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }
        }
    }

    private void handleOpenLockDialogIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra("OPEN_LOCK_DIALOG", false)) {
            showLockTimerDialog();
            intent.removeExtra("OPEN_LOCK_DIALOG");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 显示悬浮面板
    private void showFloatingPanel(View anchorView) {
        // 加载悬浮面板布局
        View panelView = LayoutInflater.from(this).inflate(R.layout.floating_panel, null);

        // 初始化GridView
        mGridView = panelView.findViewById(R.id.panel_grid);
        mAdapter = new PanelGridAdapter(this, getPanelItems());
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PanelGridAdapter.PanelItem item = (PanelGridAdapter.PanelItem) mAdapter.getItem(position);
                String name = item.getName();
                if ("定时返回".equals(name) || "定时锁屏".equals(name)) {
                    showLockTimerDialog();
                }
            }
        });

        // 创建PopupWindow
        mPopupWindow = new PopupWindow(
                panelView,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                true
        );

        // 设置动画效果
        mPopupWindow.setAnimationStyle(R.style.PopupAnimation);

        // 获取屏幕宽度和高度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // 20dp转换为像素
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());

        // 图标大小和间距（从XML布局中获取）
        int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        int horizontalSpacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        int itemPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()); // panel_item.xml中的padding

        // 计算可用宽度和一行能放的图标数量
        int availableWidth = screenWidth - 2 * margin; // 左右边距各20dp
        int itemTotalWidth = iconSize + 2 * itemPadding; // 图标宽度+两侧padding（文字宽度不超过图标宽度，因为是单行显示）
        int numColumns = availableWidth / (itemTotalWidth + horizontalSpacing); // 向下取整，考虑间距

        // 确保至少有1列
        if (numColumns < 1) {
            numColumns = 1;
        }

        // 额外检查：确保最后一列不会超出可用宽度
        int totalWidthWithColumns = numColumns * itemTotalWidth + (numColumns - 1) * horizontalSpacing;
        if (totalWidthWithColumns > availableWidth) {
            numColumns--;
        }

        // 设置GridView的列数
        mGridView.setNumColumns(numColumns);

        // 强制测量panelView
        panelView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // 设置悬浮面板位于屏幕底部，距离左右和底部各20dp
        int x = margin;
        int y = screenHeight - panelView.getMeasuredHeight() - margin;

        // 设置PopupWindow的宽度
        mPopupWindow.setWidth(screenWidth - 2 * margin);

        mPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);

        // 设置点击外部关闭
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
    }

    private void showLockTimerDialog() {
        final List<String> labels = new ArrayList<>();
        final List<Long> delays = new ArrayList<>();
        // 15-120 分钟，步长 15 分钟
        for (int m = 15; m <= 120; m += 15) {
            labels.add(m + " 分钟后");
            delays.add(m * 60L * 1000L);
        }
        // 追加功能项
        labels.add("自定义时长…");
        labels.add("查看剩余时间");
        labels.add("取消已设置的定时");

        new AlertDialog.Builder(this)
                .setTitle("定时返回桌面")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which < delays.size()) {
                        if (!ensureOverlayPermission()) return;
                        ensureNotificationPermissionIfNeeded();
                        if (!ensureExactAlarmPermission()) return;
                        scheduleLock(delays.get(which));
                        if (mPopupWindow != null && mPopupWindow.isShowing()) mPopupWindow.dismiss();
                    } else {
                        int actionIndex = which - delays.size();
                        if (actionIndex == 0) {
                            // 自定义
                            promptCustomMinutes();
                        } else if (actionIndex == 1) {
                            showRemainingTime();
                        } else if (actionIndex == 2) {
                            cancelScheduledLock();
                        }
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private boolean ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (Settings.canDrawOverlays(this)) return true;
        Snackbar.make(binding.getRoot(), "需要悬浮窗权限以显示倒计时", Snackbar.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        return false;
    }

    private void ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTI);
            }
        }
    }

    private boolean ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null && am.canScheduleExactAlarms()) return true;
        Snackbar.make(binding.getRoot(), "需要授予精准闹钟权限", Snackbar.LENGTH_LONG).show();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return false;
        }
        return true;
    }

    private void scheduleLock(long delayMillis) {
        long triggerAt = System.currentTimeMillis() + delayMillis;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildLockPendingIntent();
        if (am != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
        saveTriggerAt(triggerAt);
        // 启动前台服务，显示常驻通知与悬浮窗
        Intent svc = new Intent(this, CountdownOverlayService.class);
        svc.setAction(CountdownOverlayService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        Snackbar.make(binding.getRoot(), "已设置：" + formatDuration(delayMillis) + "后回到主屏", Snackbar.LENGTH_LONG).show();
    }

    private void cancelScheduledLock() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildLockPendingIntent();
        if (am != null) {
            am.cancel(pi);
        }
        clearTriggerAt();
        // 停止前台服务
        Intent svc = new Intent(this, CountdownOverlayService.class);
        svc.setAction(CountdownOverlayService.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        Snackbar.make(binding.getRoot(), "已取消定时返回桌面", Snackbar.LENGTH_LONG).show();
    }

    private PendingIntent buildLockPendingIntent() {
        Intent intent = new Intent(this, LockAlarmReceiver.class);
        return PendingIntent.getBroadcast(
                this,
                REQ_CODE_LOCK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void promptCustomMinutes() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_time_wheel, null, false);
        NumberPicker npHours = view.findViewById(R.id.np_hours);
        NumberPicker npMinutes = view.findViewById(R.id.np_minutes);
        npHours.setMinValue(0);
        npHours.setMaxValue(12);
        int lastH = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_LAST_HOURS, 0);
        int lastM = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_LAST_MINUTES, 25);
        if (lastH < 0) lastH = 0; if (lastH > 12) lastH = 12;
        if (lastM < 0) lastM = 0; if (lastM > 59) lastM = 59;
        npHours.setValue(lastH);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npMinutes.setValue(lastM);
        npHours.setWrapSelectorWheel(true);
        npMinutes.setWrapSelectorWheel(true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("自定义时长")
                .setView(view)
                .create();

        View btnOk = view.findViewById(R.id.btn_ok);
        View btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            int hours = npHours.getValue();
            int minutes = npMinutes.getValue();
            long totalMinutes = (long) hours * 60L + minutes;
            if (totalMinutes <= 0) {
                Snackbar.make(binding.getRoot(), "请选择大于0的时长", Snackbar.LENGTH_LONG).show();
                return;
            }
            if (!ensureOverlayPermission()) return;
            ensureNotificationPermissionIfNeeded();
            if (!ensureExactAlarmPermission()) return;
            // 记忆用户上次选择
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            sp.edit().putInt(KEY_LAST_HOURS, hours).putInt(KEY_LAST_MINUTES, minutes).apply();
            scheduleLock(totalMinutes * 60L * 1000L);
            if (mPopupWindow != null && mPopupWindow.isShowing()) mPopupWindow.dismiss();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRemainingTime() {
        long remaining = getRemainingMs();
        if (remaining <= 0) {
            Snackbar.make(binding.getRoot(), "当前没有正在进行的定时", Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(binding.getRoot(), "剩余：" + formatDuration(remaining) + "后回到主屏", Snackbar.LENGTH_LONG).show();
        }
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

    private void saveTriggerAt(long triggerAt) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().putLong(KEY_TRIGGER_AT, triggerAt).apply();
    }

    private void clearTriggerAt() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().remove(KEY_TRIGGER_AT).apply();
    }

    private long getRemainingMs() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long triggerAt = sp.getLong(KEY_TRIGGER_AT, -1L);
        if (triggerAt <= 0) return -1L;
        return triggerAt - System.currentTimeMillis();
    }

    // 获取面板数据
    private List<PanelGridAdapter.PanelItem> getPanelItems() {
        List<PanelGridAdapter.PanelItem> items = new ArrayList<>();

        // 添加示例图标和名称
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "定时返回"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));

        return items;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
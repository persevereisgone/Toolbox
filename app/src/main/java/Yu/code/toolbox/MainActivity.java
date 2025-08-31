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

import android.widget.Toast;
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
import Yu.code.toolbox.ui.home.HomeFragment;
import Yu.code.toolbox.ui.function.FunctionConstants;
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
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;


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

    /**
     * Activity创建时的初始化方法
     * 设置UI界面、初始化FAB按钮、配置导航抽屉等
     */
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

    /**
     * 处理新的Intent，用于处理从悬浮窗跳转回来的情况
     * @param intent 新的Intent对象
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenLockDialogIfNeeded(intent);
    }

    /**
     * Activity恢复时检查定时服务状态
     * 如果之前设置了定时但服务未运行，则重新启动服务
     */
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



    /**
     * 检查是否需要打开锁屏对话框
     * 用于处理从悬浮窗跳转回来时自动显示定时对话框
     * @param intent 要检查的Intent对象
     */
    private void handleOpenLockDialogIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra("OPEN_LOCK_DIALOG", false)) {
            showLockTimerDialog();
            intent.removeExtra("OPEN_LOCK_DIALOG");
        }
    }

    /**
     * 创建选项菜单
     * @param menu 菜单对象
     * @return 返回true表示菜单创建成功
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * 显示悬浮功能面板
     * 创建并显示包含各种功能按钮的悬浮面板
     * @param anchorView 锚点视图，用于确定面板显示位置
     */
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
                } else if ("录音机".equals(name)) {
                    showRecorderInHome();
                } else if ("统计".equals(name)) {
                    showStatisticsInHome();
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

    /**
     * 显示定时锁屏对话框
     * 提供预设时长选项和自定义时长功能
     */
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

    /**
     * 在Home页面显示录音机功能
     * 导航到Home Fragment并显示录音机界面
     */
    private void showRecorderInHome() {
        // 关闭悬浮面板
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }

        // 导航到Home Fragment
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_home);

        // 延迟执行，确保Fragment已经创建
        new Handler().postDelayed(() -> {
            // 使用静态方法显示录音机功能
            HomeFragment.showFunctionStatic(FunctionConstants.FUNCTION_RECORDER);
        }, 300);
    }

    /**
     * 在Home页面显示统计功能
     * 导航到Home Fragment并显示统计界面
     */
    private void showStatisticsInHome() {
        // 关闭悬浮面板
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }

        // 导航到Home Fragment
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_home);

        // 延迟执行，确保Fragment已经创建
        new Handler().postDelayed(() -> {
            // 使用静态方法显示统计功能
            HomeFragment.showFunctionStatic(FunctionConstants.FUNCTION_STATISTICS);
        }, 300);
    }

    /**
     * 确保悬浮窗权限已授予
     * 如果没有权限，会引导用户到设置页面
     * @return 返回true表示有权限，false表示需要用户手动授权
     */
    private boolean ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (Settings.canDrawOverlays(this)) return true;
        Toast.makeText(this, "需要悬浮窗权限以显示倒计时", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        return false;
    }

    /**
     * 确保通知权限已授予（Android 13及以上版本需要）
     * 如果没有权限，会请求用户授权
     */
    private void ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTI);
            }
        }
    }

    /**
     * 确保精准闹钟权限已授予（Android 12及以上版本需要）
     * 如果没有权限，会引导用户到设置页面
     * @return 返回true表示有权限，false表示需要用户手动授权
     */
    private boolean ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null && am.canScheduleExactAlarms()) return true;
        Toast.makeText(this, "需要授予精准闹钟权限", Toast.LENGTH_LONG).show();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return false;
        }
        return true;
    }

    /**
     * 设置定时锁屏
     * 创建闹钟并启动倒计时服务
     * @param delayMillis 延迟时间（毫秒）
     */
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
        Toast.makeText(this, "已设置：" + formatDuration(delayMillis) + "后回到主屏", Toast.LENGTH_LONG).show();
    }

    /**
     * 取消已设置的定时锁屏
     * 取消闹钟并停止倒计时服务
     */
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
        Toast.makeText(this, "已取消定时返回桌面", Toast.LENGTH_LONG).show();
    }

    /**
     * 构建锁屏的PendingIntent
     * 用于创建定时锁屏的广播接收器
     * @return 返回配置好的PendingIntent对象
     */
    private PendingIntent buildLockPendingIntent() {
        Intent intent = new Intent(this, LockAlarmReceiver.class);
        return PendingIntent.getBroadcast(
                this,
                REQ_CODE_LOCK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * 提示用户输入自定义时长
     * 显示NumberPicker对话框让用户选择小时和分钟
     */
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
                Toast.makeText(this, "请选择大于0的时长", Toast.LENGTH_LONG).show();
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

    /**
     * 显示剩余时间
     * 显示当前定时锁屏的剩余时间
     */
    private void showRemainingTime() {
        long remaining = getRemainingMs();
        if (remaining <= 0) {
            Toast.makeText(this, "当前没有正在进行的定时", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "剩余：" + formatDuration(remaining) + "后回到主屏", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 格式化时长显示
     * 将毫秒转换为可读的时间格式（小时、分钟、秒）
     * @param ms 毫秒数
     * @return 格式化后的时间字符串
     */
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

    /**
     * 保存定时触发时间到SharedPreferences
     * @param triggerAt 触发时间戳（毫秒）
     */
    private void saveTriggerAt(long triggerAt) {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().putLong(KEY_TRIGGER_AT, triggerAt).apply();
    }

    /**
     * 清除定时触发时间
     * 从SharedPreferences中删除定时记录
     */
    private void clearTriggerAt() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sp.edit().remove(KEY_TRIGGER_AT).apply();
    }

    /**
     * 获取剩余时间
     * 计算当前时间到定时触发时间的剩余毫秒数
     * @return 剩余毫秒数，如果没有定时则返回-1
     */
    private long getRemainingMs() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long triggerAt = sp.getLong(KEY_TRIGGER_AT, -1L);
        if (triggerAt <= 0) return -1L;
        return triggerAt - System.currentTimeMillis();
    }

    /**
     * 获取悬浮面板的功能项数据
     * 定义悬浮面板中显示的功能按钮
     * @return 功能项列表
     */
    private List<PanelGridAdapter.PanelItem> getPanelItems() {
        List<PanelGridAdapter.PanelItem> items = new ArrayList<>();

        // 添加示例图标和名称
        items.add(new PanelGridAdapter.PanelItem(R.drawable.time, "定时返回"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.sound_recorder, "录音机"));
        items.add(new PanelGridAdapter.PanelItem(R.drawable.statistics, "统计"));
        return items;
    }

    /**
     * 处理导航向上按钮点击事件
     * 用于处理ActionBar的返回按钮
     * @return 返回true表示事件已处理
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
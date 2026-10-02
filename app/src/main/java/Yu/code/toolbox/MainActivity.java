package Yu.code.toolbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatActivity;

import Yu.code.toolbox.databinding.ActivityMainBinding;
import Yu.code.toolbox.function.time.CountdownOverlayService;
import Yu.code.toolbox.function.time.LockAlarmReceiver;
import Yu.code.toolbox.ui.home.HomeFragment;
import Yu.code.toolbox.ui.function.FunctionConstants;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.NumberPicker;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // 悬浮面板相关变量
    // 已移除悬浮面板相关成员
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

        // 顶栏/抽屉已移除:无 ActionBar,状态栏文字颜色跟随深浅模式
        boolean isNight = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                // 设置状态栏文字颜色:浅色模式黑字,深色模式白字
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int appearance = isNight ? 0 : android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(
                    appearance,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (isNight) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }

        // 绑定底部导航
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
            // 显式关闭图标着色与水波纹，避免被主题或库默认值覆盖
            bottomNav.setItemIconTintList(null);
            bottomNav.setItemRippleColor(null);
        }
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_slideshow, R.id.nav_settings)
                .build();

        // 如从悬浮窗菜单带参跳回，自动弹出对话框
        handleOpenLockDialogIfNeeded(getIntent());

        // 小组件点击带 open_function 跳指定功能页
        handleOpenFunctionIfNeeded(getIntent());
        
        // 处理文件分享
        handleSharedFile(getIntent());
    }

    /**
     * 处理新的Intent，用于处理从悬浮窗跳转回来的情况或文件分享
     * @param intent 新的Intent对象
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenLockDialogIfNeeded(intent);
        handleOpenFunctionIfNeeded(intent);
        handleSharedFile(intent);
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
     * 检查是否需要打开锁屏对话框或导航到定时器页面
     * 用于处理从悬浮窗跳转回来时的不同操作
     * @param intent 要检查的Intent对象
     */
    private void handleOpenLockDialogIfNeeded(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("OPEN_LOCK_DIALOG", false)) {
                // #10 并入 timer 页:悬浮窗菜单调整时长直接跳回本页,不再弹八档 dialog
                navigateToTimerPage();
                intent.removeExtra("OPEN_LOCK_DIALOG");
            } else if (intent.getBooleanExtra("NAVIGATE_TO_TIMER", false)) {
                navigateToTimerPage();
                intent.removeExtra("NAVIGATE_TO_TIMER");
            }
        }
    }

    /**
     * 处理从其他应用分享的文件
     * @param intent 包含分享文件的Intent
     */
    private void handleSharedFile(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        String type = intent.getType();
        
        // 检查是否是文件分享或查看操作
        if ((Intent.ACTION_SEND.equals(action) || Intent.ACTION_VIEW.equals(action)) && type != null) {
            // 检查是否是CSV或Excel文件
            if (type.startsWith("text/") || type.contains("csv") || type.contains("excel") || type.contains("spreadsheet")) {
                Uri fileUri = null;
                
                if (Intent.ACTION_SEND.equals(action)) {
                    fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                } else if (Intent.ACTION_VIEW.equals(action)) {
                    fileUri = intent.getData();
                }
                
                if (fileUri != null) {
                    final Uri finalUri = fileUri;
                    
                    // 显示确认对话框
                    new AlertDialog.Builder(this)
                            .setTitle("导入统计数据")
                            .setMessage("检测到 CSV/Excel 文件，是否导入到统计功能？")
                            .setPositiveButton("导入", (dialog, which) -> {
                                // 导航到统计页面并传递文件URI
                                navigateToStatisticsWithFile(finalUri);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    
                    // 清除Intent，避免重复处理
                    intent.setAction(null);
                }
            }
        }
    }

    /**
     * 导航到统计页面并导入文件
     * @param fileUri 要导入的文件URI
     */
    private void navigateToStatisticsWithFile(Uri fileUri) {
        // 先导航到统计页面
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        
        // 创建Bundle传递功能ID和文件URI
        Bundle args = new Bundle();
        args.putInt("function_id", FunctionConstants.FUNCTION_STATISTICS);
        args.putParcelable("import_file_uri", fileUri);
        
        navController.navigate(R.id.nav_function, args);
        
        Toast.makeText(this, "正在导入文件...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 导航到定时器页面
     * 用于从悬浮窗直接跳转到定时器功能页面
     */
    private void navigateToTimerPage() {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.slide_out_right)
                .build();

        // 创建Bundle传递功能ID
        android.os.Bundle args = new android.os.Bundle();
        args.putInt("function_id", Yu.code.toolbox.ui.function.FunctionConstants.FUNCTION_TIMER);

        navController.navigate(R.id.nav_function, args, navOptions);
    }

    /**
     * 小组件跳转：open_function 为 layoutId，导航到 nav_function 对应页
     */
    private void handleOpenFunctionIfNeeded(Intent intent) {
        if (intent == null || !intent.hasExtra("open_function")) return;
        int functionId = intent.getIntExtra("open_function", -1);
        intent.removeExtra("open_function");
        if (functionId == -1) return;
        try {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            Bundle args = new Bundle();
            args.putInt("function_id", functionId);
            if (intent.getBooleanExtra("auto_time_sheet", false)) args.putBoolean("auto_time_sheet", true);
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true).build();
            navController.navigate(R.id.nav_function, args, navOptions);
        } catch (Exception ignored) {}
    }

    /**
     * 创建选项菜单
     * @param menu 菜单对象
     * @return 返回true表示菜单创建成功
     */
    /**
     * 显示悬浮功能面板
     * 创建并显示包含各种功能按钮的悬浮面板
     * @param anchorView 锚点视图，用于确定面板显示位置
     */


    /**
     * 显示定时锁屏对话框
     * 提供预设时长选项和自定义时长功能
     */
    /**
     * 在Home页面显示录音机功能
     * 导航到Home Fragment并显示录音机界面
     */
    private void showRecorderInHome() {
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
    /**
     * 显示剩余时间
     * 显示当前定时锁屏的剩余时间
     */
    /**
     * 显示剩余时间:统一为对话框(与 timer 页 B-3 同语言)
     */
    private void showRemainingTime() {
        long remaining = getRemainingMs();
        if (remaining <= 0) {
            new AlertDialog.Builder(this, R.style.AppDialogStyle)
                    .setTitle("剩余时间")
                    .setMessage("当前没有正在进行的定时")
                    .setPositiveButton("知道了", null)
                    .show();
        } else {
            new AlertDialog.Builder(this, R.style.AppDialogStyle)
                    .setTitle("剩余时间")
                    .setMessage("剩余" + formatDuration(remaining) + "后回到主屏")
                    .setNegativeButton("取消定时", (d, w) -> cancelScheduledLock())
                    .setPositiveButton("知道了", null)
                    .show();
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
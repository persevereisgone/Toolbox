package Yu.code.toolbox.function.time;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class LockAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 回到主屏
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(homeIntent);

        // 检查是否需要自动锁屏
        SharedPreferences sp = context.getSharedPreferences("lock_prefs", Context.MODE_PRIVATE);
        boolean autoLock = sp.getBoolean("auto_lock_screen", false);
        
        if (autoLock) {
            // 延迟500ms后锁屏，确保先返回桌面
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                lockScreen(context);
            }, 500);
        }

        // 清除记录，避免再次自动拉起
        sp.edit().remove("lock_trigger_at").apply();

        // 直接停止倒计时前台服务，避免前台服务未及时启动的崩溃
        context.stopService(new Intent(context, CountdownOverlayService.class));
    }
    
    /**
     * 锁定屏幕
     */
    private void lockScreen(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(context, LockDeviceAdminReceiver.class);
            
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                // 有设备管理员权限，直接锁屏
                dpm.lockNow();
            } else {
                // 没有设备管理员权限，显示提示
                Toast.makeText(context, "需要设备管理员权限才能自动锁屏，请在设置中授予", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "锁屏失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
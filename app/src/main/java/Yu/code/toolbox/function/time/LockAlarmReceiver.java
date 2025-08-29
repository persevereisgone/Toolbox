package Yu.code.toolbox.function.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class LockAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 回到主屏
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(homeIntent);

        // 清除记录，避免再次自动拉起
        SharedPreferences sp = context.getSharedPreferences("lock_prefs", Context.MODE_PRIVATE);
        sp.edit().remove("lock_trigger_at").apply();

        // 直接停止倒计时前台服务，避免前台服务未及时启动的崩溃
        context.stopService(new Intent(context, CountdownOverlayService.class));
    }
}
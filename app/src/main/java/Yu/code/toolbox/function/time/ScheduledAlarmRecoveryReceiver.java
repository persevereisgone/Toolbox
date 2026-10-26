package Yu.code.toolbox.function.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import Yu.code.toolbox.function.alarm.AlarmStore;

public class ScheduledAlarmRecoveryReceiver extends BroadcastReceiver {
    private static final String ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || ACTION_EXACT_ALARM_PERMISSION_CHANGED.equals(action)) {
            if (TimerManager.restoreAfterBoot(context)) {
                Intent serviceIntent = new Intent(context, CountdownOverlayService.class)
                        .setAction(CountdownOverlayService.ACTION_START);
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (RuntimeException ignored) {
                }
            }
            AlarmStore.restoreAfterBoot(context);
        }
    }
}

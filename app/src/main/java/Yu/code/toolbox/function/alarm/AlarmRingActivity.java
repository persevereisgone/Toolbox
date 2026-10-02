package Yu.code.toolbox.function.alarm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.Locale;
import Yu.code.toolbox.R;

public class AlarmRingActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoClose = new Runnable() {
        @Override public void run() {
            AlarmReceiver.stopRing();
            AlarmReceiver.cancelNotif(AlarmRingActivity.this);
            finish();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        setContentView(R.layout.activity_alarm_ring);

        AlarmStore store = new AlarmStore(this);
        String shown = AlarmStore.MODE_RTC.equals(store.getMode())
                ? store.getTargetHm()
                : AlarmFunctionManager.fmtHms(store.getRemaining());
        if (shown == null || shown.isEmpty()) {
            shown = String.format(Locale.getDefault(), "%02d:%02d",
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE));
        }
        ((TextView) findViewById(R.id.tv_ring_time)).setText(shown);

        findViewById(R.id.btn_ring_close).setOnClickListener(v -> {
            AlarmReceiver.stopRing();
            AlarmReceiver.cancelNotif(this);
            finish();
        });
        findViewById(R.id.btn_ring_snooze).setOnClickListener(v -> {
            AlarmReceiver.stopRing();
            AlarmReceiver.cancelNotif(this);
            store.snooze5(this);
            finish();
        });

        handler.postDelayed(autoClose, 10_000);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(autoClose);
        super.onDestroy();
    }
}

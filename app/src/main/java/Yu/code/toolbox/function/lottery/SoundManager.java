package Yu.code.toolbox.function.lottery;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import Yu.code.toolbox.R;

/**
 * 音效管理器
 */
public class SoundManager {

    private static SoundManager instance;
    private SoundPool soundPool;
    private int spinSoundId;
    private int resultSoundId;
    private boolean isSoundEnabled = true;

    private SoundManager(Context context) {
        initSoundPool(context);
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    private void initSoundPool(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        }

        // 初始化音效ID为-1（静音模式）
        // 在实际项目中，您可以添加自定义音效文件到 res/raw/ 目录
        // 然后使用 soundPool.load(context, R.raw.your_sound_file, 1) 来加载
        spinSoundId = -1;
        resultSoundId = -1;

        // 示例：如果您有自定义音效文件，可以这样加载：
        // try {
        //     spinSoundId = soundPool.load(context, R.raw.spin_sound, 1);
        //     resultSoundId = soundPool.load(context, R.raw.result_sound, 1);
        // } catch (Exception e) {
        //     spinSoundId = -1;
        //     resultSoundId = -1;
        // }
    }

    public void playSpinSound() {
        if (isSoundEnabled && soundPool != null && spinSoundId != -1) {
            soundPool.play(spinSoundId, 0.5f, 0.5f, 1, 0, 1.0f);
        }
    }

    public void playResultSound() {
        if (isSoundEnabled && soundPool != null && resultSoundId != -1) {
            soundPool.play(resultSoundId, 0.8f, 0.8f, 1, 0, 1.2f);
        }
    }

    public void setSoundEnabled(boolean enabled) {
        this.isSoundEnabled = enabled;
    }

    public boolean isSoundEnabled() {
        return isSoundEnabled;
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}

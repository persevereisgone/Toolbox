package Yu.code.toolbox.ui.function;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import Yu.code.toolbox.R;

public class RecorderManager {

    private Fragment fragment;
    private View recorderView;

    // 录音机相关变量
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File outputFile;
    private Handler handler = new Handler();
    private long startMs = 0L;
    private static final int REQ_RECORD_AUDIO = 5001;

    // UI控件
    private TextView tvStatus;
    private Button btnRecord;
    private Button btnStop;
    private Button btnBack;

    public RecorderManager(Fragment fragment, View recorderView) {
        this.fragment = fragment;
        this.recorderView = recorderView;
        initUI();
    }

    private void initUI() {
        // 获取录音机界面的控件
        tvStatus = recorderView.findViewById(R.id.tv_status);
        btnRecord = recorderView.findViewById(R.id.btn_record);
        btnStop = recorderView.findViewById(R.id.btn_stop);
        btnBack = recorderView.findViewById(R.id.btn_back);

        // 设置录音按钮点击事件
        btnRecord.setOnClickListener(v -> tryStartRecording());

        // 设置停止按钮点击事件
        btnStop.setOnClickListener(v -> stopRecording());

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void tryStartRecording() {
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startRecording();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                tvStatus.setText("未授予麦克风权限");
            }
        }
    }

    private void startRecording() {
        stopTicker();
        outputFile = new File(fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), generateFileName());
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(96000);
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            startMs = System.currentTimeMillis();
            startTicker();
            updateRecorderUI();
        } catch (IOException e) {
            tvStatus.setText("录音启动失败: " + e.getMessage());
            releaseRecorder();
        }
    }

    private void stopRecording() {
        stopTicker();
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException ignore) {
                // 安全兜底
            }
            releaseRecorder();
        }
        isRecording = false;
        updateRecorderUI();
        if (outputFile != null) {
            tvStatus.setText("已保存: " + outputFile.getName());
        }
    }

    private void releaseRecorder() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.reset();
                mediaRecorder.release();
            }
        } finally {
            mediaRecorder = null;
        }
    }

    private String generateFileName() {
        String ts = DateFormat.format("yyyyMMdd_HHmmss", new Date()).toString();
        return "REC_" + ts + ".m4a";
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long el = System.currentTimeMillis() - startMs;
            long s = Math.max(0, el / 1000);
            long m = s / 60; s = s % 60;
            tvStatus.setText(String.format("录音中 %02d:%02d", m, s));
            handler.postDelayed(this, 1000);
        }
    };

    private void startTicker() {
        handler.post(ticker);
    }

    private void stopTicker() {
        handler.removeCallbacks(ticker);
    }

    private void updateRecorderUI() {
        btnRecord.setEnabled(!isRecording);
        btnStop.setEnabled(isRecording);
        if (!isRecording) {
            tvStatus.setText("准备就绪");
        }
    }

    private void onBackPressed() {
        if (isRecording) {
            stopRecording();
        }
        // 通知FunctionFragment隐藏功能界面
        if (fragment instanceof FunctionFragment) {
            ((FunctionFragment) fragment).hideFunction();
        }
    }

    public void onPause() {
        if (isRecording) {
            stopRecording();
        }
    }

    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        handler.removeCallbacksAndMessages(null);
    }
}

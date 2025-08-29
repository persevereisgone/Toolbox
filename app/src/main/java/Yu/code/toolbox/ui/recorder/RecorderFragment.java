package Yu.code.toolbox.ui.recorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class RecorderFragment extends Fragment {

    private static final int REQ_RECORD_AUDIO = 5001;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File outputFile;
    private Handler handler = new Handler();
    private long startMs = 0L;

    private TextView tvStatus;
    private Button btnRecord;
    private Button btnStop;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(Yu.code.toolbox.R.layout.fragment_recorder, container, false);
        tvStatus = root.findViewById(Yu.code.toolbox.R.id.tv_status);
        btnRecord = root.findViewById(Yu.code.toolbox.R.id.btn_record);
        btnStop = root.findViewById(Yu.code.toolbox.R.id.btn_stop);

        btnRecord.setOnClickListener(v -> tryStartRecording());
        btnStop.setOnClickListener(v -> stopRecording());

        updateUi();
        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
    }

    private void tryStartRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        outputFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), generateFileName());
        mediaRecorder = new MediaRecorder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        } else {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
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
            updateUi();
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
        updateUi();
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

    private void updateUi() {
        btnRecord.setEnabled(!isRecording);
        btnStop.setEnabled(isRecording);
        if (!isRecording) {
            tvStatus.setText("准备就绪");
        }
    }
}
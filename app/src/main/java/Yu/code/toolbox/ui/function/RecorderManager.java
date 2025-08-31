package Yu.code.toolbox.ui.function;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import Yu.code.toolbox.R;
import Yu.code.toolbox.function.recorder_photo.RecordingPhotoItem;

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
    private static final int REQ_CAMERA = 5002;
    private static final int REQ_TAKE_PHOTO = 5003;

    // UI控件
    private TextView tvStatus;
    private Button btnRecord;
    private Button btnStop;
    private Button btnBack;
    private Button btnPlayRecordings;
    private Button btnTakePhoto;

    // 拍照相关变量
    private List<RecordingPhotoItem> currentRecordingPhotos = new ArrayList<>();
    private Uri photoUri;

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
        btnPlayRecordings = recorderView.findViewById(R.id.btn_play_recordings);
        btnTakePhoto = recorderView.findViewById(R.id.btn_take_photo);

        // 设置录音按钮点击事件
        btnRecord.setOnClickListener(v -> tryStartRecording());

        // 设置停止按钮点击事件
        btnStop.setOnClickListener(v -> stopRecording());

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> onBackPressed());

        // 设置播放录音按钮点击事件
        btnPlayRecordings.setOnClickListener(v -> showPlaylist());

        // 设置拍照按钮点击事件
        btnTakePhoto.setOnClickListener(v -> tryTakePhoto());
    }

    private void tryStartRecording() {
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 检查是否应该显示权限说明
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                // 用户之前拒绝了权限，显示说明
                Toast.makeText(fragment.requireContext(), "需要麦克风权限来录音，请在设置中开启", Toast.LENGTH_LONG).show();
            } else {
                // 首次申请权限
                Toast.makeText(fragment.requireContext(), "正在申请麦克风权限...", Toast.LENGTH_SHORT).show();
            }
            fragment.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }
        startRecording();
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
            // 保存照片信息到录音文件
            savePhotoInfoToRecording();
            tvStatus.setText("已保存: " + outputFile.getName());
        }
    }

    private void savePhotoInfoToRecording() {
        if (!currentRecordingPhotos.isEmpty()) {
            try {
                // 创建照片信息文件
                String photoInfoFileName = outputFile.getName().replace(".m4a", "_photos.json");
                File photoInfoFile = new File(outputFile.getParent(), photoInfoFileName);

                // 将照片信息序列化为JSON
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\n");
                jsonBuilder.append("  \"recording_file\": \"").append(outputFile.getName()).append("\",\n");
                jsonBuilder.append("  \"photos\": [\n");

                for (int i = 0; i < currentRecordingPhotos.size(); i++) {
                    RecordingPhotoItem photo = currentRecordingPhotos.get(i);
                    jsonBuilder.append("    {\n");
                    jsonBuilder.append("      \"timestamp\": ").append(photo.getTimestamp()).append(",\n");
                    jsonBuilder.append("      \"photo_path\": \"").append(photo.getPhotoPath()).append("\",\n");
                    jsonBuilder.append("      \"formatted_time\": \"").append(photo.getFormattedTimestamp()).append("\"\n");
                    jsonBuilder.append("    }");
                    if (i < currentRecordingPhotos.size() - 1) {
                        jsonBuilder.append(",");
                    }
                    jsonBuilder.append("\n");
                }

                jsonBuilder.append("  ]\n");
                jsonBuilder.append("}\n");

                // 写入文件
                java.io.FileWriter writer = new java.io.FileWriter(photoInfoFile);
                writer.write(jsonBuilder.toString());
                writer.close();

                Log.d("RecorderManager", "照片信息已保存到: " + photoInfoFile.getAbsolutePath());
                Toast.makeText(fragment.requireContext(),
                        "录音完成，包含 " + currentRecordingPhotos.size() + " 张照片", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e("RecorderManager", "保存照片信息失败", e);
                Toast.makeText(fragment.requireContext(), "保存照片信息失败", Toast.LENGTH_SHORT).show();
            }
        }

        // 清空当前录音的照片列表
        currentRecordingPhotos.clear();
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
        btnTakePhoto.setEnabled(isRecording); // 只有在录音时才能拍照
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

    private void showPlaylist() {
        // 通知FunctionFragment显示录音播放列表
        if (fragment instanceof FunctionFragment) {
            ((FunctionFragment) fragment).showFunction(FunctionConstants.FUNCTION_RECORDER_PLAYLIST);
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

    // 拍照相关方法
    private void tryTakePhoto() {
        if (!isRecording) {
            Toast.makeText(fragment.requireContext(), "请先开始录音", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 检查是否应该显示权限说明
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // 用户之前拒绝了权限，显示说明
                Toast.makeText(fragment.requireContext(), "需要相机权限来拍照，请在设置中开启", Toast.LENGTH_LONG).show();
            } else {
                // 首次申请权限
                Toast.makeText(fragment.requireContext(), "正在申请相机权限...", Toast.LENGTH_SHORT).show();
            }
            fragment.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }

        takePhoto();
    }

    private void takePhoto() {
        try {
            Log.d("RecorderManager", "开始拍照流程");

            // 创建照片文件
            File photoDir = fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (photoDir == null) {
                Log.e("RecorderManager", "无法获取照片目录");
                Toast.makeText(fragment.requireContext(), "无法创建照片目录", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d("RecorderManager", "照片目录: " + photoDir.getAbsolutePath());

            // 确保目录存在
            if (!photoDir.exists()) {
                boolean created = photoDir.mkdirs();
                if (!created) {
                    Log.e("RecorderManager", "无法创建照片目录: " + photoDir.getAbsolutePath());
                    Toast.makeText(fragment.requireContext(), "无法创建照片目录: " + photoDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    return;
                }
                Log.d("RecorderManager", "成功创建照片目录");
            }

            String photoFileName = "PHOTO_" + DateFormat.format("yyyyMMdd_HHmmss", new Date()) + ".jpg";
            File photoFile = new File(photoDir, photoFileName);

            Log.d("RecorderManager", "照片文件路径: " + photoFile.getAbsolutePath());

            // 检查文件是否可写
            if (photoFile.exists() && !photoFile.canWrite()) {
                Log.e("RecorderManager", "照片文件不可写: " + photoFile.getAbsolutePath());
                Toast.makeText(fragment.requireContext(), "照片文件不可写: " + photoFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }

            // 使用 FileProvider 创建 URI
            photoUri = FileProvider.getUriForFile(
                    fragment.requireContext(),
                    "Yu.code.toolbox.fileprovider",
                    photoFile
            );
            Log.d("RecorderManager", "照片URI: " + photoUri.toString());

            Toast.makeText(fragment.requireContext(), "准备拍照，文件路径: " + photoFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // 启动相机
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            // 添加权限标志
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Log.d("RecorderManager", "检查相机应用是否可用");
            if (takePictureIntent.resolveActivity(fragment.requireContext().getPackageManager()) != null) {
                Log.d("RecorderManager", "启动相机应用");
                fragment.startActivityForResult(takePictureIntent, REQ_TAKE_PHOTO);
            } else {
                Log.e("RecorderManager", "无法找到相机应用");
                Toast.makeText(fragment.requireContext(), "无法启动相机，请检查设备是否支持相机功能", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("RecorderManager", "拍照过程中发生异常", e);
            String errorMsg = "拍照失败: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            Toast.makeText(fragment.requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            e.printStackTrace(); // 在日志中打印详细错误信息
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("RecorderManager", "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQ_TAKE_PHOTO) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                Log.d("RecorderManager", "拍照成功");
                // 拍照成功，记录时间戳
                try {
                    if (photoUri != null) {
                        Log.d("RecorderManager", "照片URI: " + photoUri.toString());
                        long currentTime = System.currentTimeMillis() - startMs; // 录音开始后的时间
                        RecordingPhotoItem photoItem = new RecordingPhotoItem(currentTime, photoUri.getPath());
                        currentRecordingPhotos.add(photoItem);

                        Log.d("RecorderManager", "照片已添加到列表，时间戳: " + photoItem.getFormattedTimestamp());
                        Toast.makeText(fragment.requireContext(),
                                "照片已保存: " + photoItem.getFormattedTimestamp(), Toast.LENGTH_SHORT).show();

                        // 更新状态显示，显示拍照时间点
                        updateStatusWithPhotoInfo(photoItem);
                    } else {
                        Log.e("RecorderManager", "拍照成功但照片路径为空");
                        Toast.makeText(fragment.requireContext(), "拍照成功但照片路径为空", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e("RecorderManager", "处理照片时出错", e);
                    String errorMsg = "处理照片时出错: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    Toast.makeText(fragment.requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
                Log.d("RecorderManager", "用户取消了拍照");
                Toast.makeText(fragment.requireContext(), "用户取消了拍照", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("RecorderManager", "拍照失败，返回码: " + resultCode);
                Toast.makeText(fragment.requireContext(), "拍照失败，返回码: " + resultCode, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatusWithPhotoInfo(RecordingPhotoItem photoItem) {
        // 在状态栏显示拍照时间点信息
        String photoInfo = "📸 拍照时间点: " + photoItem.getFormattedTimestamp();
        tvStatus.setText(photoInfo);

        // 3秒后恢复正常的录音时间显示
        handler.postDelayed(() -> {
            if (isRecording) {
                long el = System.currentTimeMillis() - startMs;
                long s = Math.max(0, el / 1000);
                long m = s / 60; s = s % 60;
                tvStatus.setText(String.format("录音中 %02d:%02d", m, s));
            }
        }, 3000);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                tvStatus.setText("未授予麦克风权限");
                Toast.makeText(fragment.requireContext(), "需要麦克风权限才能录音", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(fragment.requireContext(), "相机权限已授予，正在启动相机...", Toast.LENGTH_SHORT).show();
                takePhoto();
            } else {
                Toast.makeText(fragment.requireContext(), "未授予相机权限，无法拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

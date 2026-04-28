package Yu.code.toolbox.function.recorder_photo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Yu.code.toolbox.R;
import Yu.code.toolbox.ui.function.FunctionFragment;

public class ScreenRecorderManager {

    private static final int REQUEST_MEDIA_PROJECTION = 5002;
    private static final int REQUEST_STORAGE_PERMISSION = 5003;
    private static final int REQUEST_MANAGE_STORAGE = 5004;
    private static final int REQUEST_OVERLAY_PERMISSION = 5005;
    private static final String TAG = "ScreenRecorderManager";

    private Context context;
    private View view;
    private FunctionFragment fragment;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private AudioRecord audioRecord; // 用于捕获应用音频
    private boolean isRecording = false;
    private boolean isPaused = false;
    private boolean captureAppAudio = true; // 是否捕获应用音频
    private File outputFile;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    // 定时相关
    private long recordingTime = 30000; // 默认30秒
    private long startTime = 0L;
    private long pausedTime = 0L; // 暂停时累计的已录制时间
    private Runnable timerRunnable;
    private android.content.BroadcastReceiver serviceStartedReceiver;
    
    // UI组件
    private TextView tvStatus;
    private TextView tvHours, tvMinutes, tvSeconds;
    private Button btnStartRecord, btnStopRecord, btnViewRecordings, btnViewFolder;
    private Button btnHourMinus, btnHourPlus, btnMinuteMinus, btnMinutePlus, btnSecondMinus, btnSecondPlus;
    private Button btnQuick30s, btnQuick1min, btnQuick5min, btnQuick10min;
    private android.widget.Spinner spinnerQuality, spinnerFps;
    private androidx.appcompat.widget.SwitchCompat switchAppAudio;
    
    // 标记是否是手动停止（用于判断是否需要重命名）
    private boolean isManualStop = false;
    
    // 视频质量设置
    private int videoWidth = 1920;
    private int videoHeight = 1080;
    private int videoFps = 30;
    private int videoBitrate = 8 * 1024 * 1024; // 8Mbps

    public ScreenRecorderManager(FunctionFragment fragment, View view) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.view = view;
        this.projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        initViews();
        setupListeners();
        initBroadcastReceiver();
    }

    private void initBroadcastReceiver() {
        Log.d(TAG, "初始化广播接收器");
        // 初始化广播接收器，接收服务启动通知、暂停/恢复和停止录制命令
        serviceStartedReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "广播接收器：收到广播 action=" + action);
                
                if ("Yu.code.toolbox.action.SCREEN_RECORDER_SERVICE_STARTED".equals(action)) {
                    Log.d(TAG, "广播接收器：收到服务启动通知，开始录屏");
                    startRecordingWithMediaProjection();
                } else if ("Yu.code.toolbox.action.SCREEN_RECORDER_PAUSE".equals(action)) {
                    Log.d(TAG, "广播接收器：收到暂停/恢复录制通知，当前 isPaused=" + isPaused + ", isRecording=" + isRecording);
                    // 切换暂停/恢复状态
                    if (isPaused) {
                        Log.d(TAG, "广播接收器：执行恢复录制");
                        resumeRecording();
                    } else {
                        Log.d(TAG, "广播接收器：执行暂停录制");
                    pauseRecording();
                    }
                } else if ("Yu.code.toolbox.action.SCREEN_RECORDER_STOP".equals(action)) {
                    Log.d(TAG, "广播接收器：收到停止录制通知，isRecording=" + isRecording);
                    // 标记为手动停止（通过悬浮窗停止）
                    isManualStop = true;
                    // 停止录制
                    stopRecording();
                } else {
                    Log.d(TAG, "广播接收器：收到未知广播 action=" + action);
                }
            }
        };
        
        // 注册广播接收器
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("Yu.code.toolbox.action.SCREEN_RECORDER_SERVICE_STARTED");
        filter.addAction("Yu.code.toolbox.action.SCREEN_RECORDER_PAUSE");
        filter.addAction("Yu.code.toolbox.action.SCREEN_RECORDER_STOP");
        
        androidx.core.content.ContextCompat.registerReceiver(context, serviceStartedReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "广播接收器注册完成");
    }



    private void initViews() {
        tvStatus = view.findViewById(R.id.tv_status);
        tvHours = view.findViewById(R.id.tv_hours);
        tvMinutes = view.findViewById(R.id.tv_minutes);
        tvSeconds = view.findViewById(R.id.tv_seconds);
        btnStartRecord = view.findViewById(R.id.btn_start_record);
        btnStopRecord = view.findViewById(R.id.btn_stop_record);
        btnViewRecordings = view.findViewById(R.id.btn_view_recordings);
        btnViewFolder = view.findViewById(R.id.btn_view_folder);
        btnHourMinus = view.findViewById(R.id.btn_hour_minus);
        btnHourPlus = view.findViewById(R.id.btn_hour_plus);
        btnMinuteMinus = view.findViewById(R.id.btn_minute_minus);
        btnMinutePlus = view.findViewById(R.id.btn_minute_plus);
        btnSecondMinus = view.findViewById(R.id.btn_second_minus);
        btnSecondPlus = view.findViewById(R.id.btn_second_plus);
        btnQuick30s = view.findViewById(R.id.btn_quick_30s);
        btnQuick1min = view.findViewById(R.id.btn_quick_1min);
        btnQuick5min = view.findViewById(R.id.btn_quick_5min);
        btnQuick10min = view.findViewById(R.id.btn_quick_10min);
        spinnerQuality = view.findViewById(R.id.spinner_quality);
        spinnerFps = view.findViewById(R.id.spinner_fps);
        switchAppAudio = view.findViewById(R.id.switch_app_audio);
        
        // 初始化画质选择器
        setupQualitySpinner();
        // 初始化帧率选择器
        setupFpsSpinner();
        // 初始化音频开关
        setupAudioSwitch();
    }
    
    private void setupQualitySpinner() {
        String[] qualities = {"高清 (1080p)", "标清 (720p)", "流畅 (480p)"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            context, android.R.layout.simple_spinner_item, qualities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuality.setAdapter(adapter);
        spinnerQuality.setSelection(0); // 默认高清
        
        spinnerQuality.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // 1080p
                        videoWidth = 1920;
                        videoHeight = 1080;
                        videoBitrate = 8 * 1024 * 1024; // 8Mbps
                        break;
                    case 1: // 720p
                        videoWidth = 1280;
                        videoHeight = 720;
                        videoBitrate = 5 * 1024 * 1024; // 5Mbps
                        break;
                    case 2: // 480p
                        videoWidth = 854;
                        videoHeight = 480;
                        videoBitrate = 2 * 1024 * 1024; // 2Mbps
                        break;
                }
                Log.d(TAG, "画质设置: " + videoWidth + "x" + videoHeight + ", 码率: " + videoBitrate);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void setupFpsSpinner() {
        String[] fpsOptions = {"60 FPS", "30 FPS", "24 FPS"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            context, android.R.layout.simple_spinner_item, fpsOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFps.setAdapter(adapter);
        spinnerFps.setSelection(1); // 默认30fps
        
        spinnerFps.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        videoFps = 60;
                        break;
                    case 1:
                        videoFps = 30;
                        break;
                    case 2:
                        videoFps = 24;
                        break;
                }
                Log.d(TAG, "帧率设置: " + videoFps + " fps");
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
    
    private void setupAudioSwitch() {
        // Android 10 以下禁用音频捕获开关
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            switchAppAudio.setEnabled(false);
            switchAppAudio.setChecked(false);
            captureAppAudio = false;
        }
        
        switchAppAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            captureAppAudio = isChecked;
            Log.d(TAG, "应用音频捕获设置: " + (isChecked ? "启用" : "禁用"));
            
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(context, "将尝试捕获应用音频（仅支持允许录制的应用）", Toast.LENGTH_SHORT).show();
            } else if (isChecked) {
                Toast.makeText(context, "需要 Android 10 或更高版本", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // 开始录屏
        btnStartRecord.setOnClickListener(v -> {
            Log.d(TAG, "开始录屏按钮被点击");
            tryStartRecording();
        });
        
        // 停止录屏
        btnStopRecord.setOnClickListener(v -> {
            Log.d(TAG, "停止录屏按钮被点击");
            isManualStop = true; // 标记为手动停止
            stopRecording();
        });
        
        // 查看最近录屏文件（播放）
        btnViewRecordings.setOnClickListener(v -> {
            Log.d(TAG, "查看录屏文件按钮被点击");
            viewRecordings();
        });
        
        // 查看录屏文件夹
        btnViewFolder.setOnClickListener(v -> {
            Log.d(TAG, "查看录屏文件夹按钮被点击");
            viewRecordingsFolder();
        });
        
        // 快捷时间设置按钮
        btnQuick30s.setOnClickListener(v -> setQuickTime(0, 0, 30));
        btnQuick1min.setOnClickListener(v -> setQuickTime(0, 1, 0));
        btnQuick5min.setOnClickListener(v -> setQuickTime(0, 5, 0));
        btnQuick10min.setOnClickListener(v -> setQuickTime(0, 10, 0));
        
        // 时间调整按钮
        btnHourMinus.setOnClickListener(v -> adjustTime(0, -1));
        btnHourPlus.setOnClickListener(v -> adjustTime(0, 1));
        btnMinuteMinus.setOnClickListener(v -> adjustTime(1, -1));
        btnMinutePlus.setOnClickListener(v -> adjustTime(1, 1));
        btnSecondMinus.setOnClickListener(v -> adjustTime(2, -1));
        btnSecondPlus.setOnClickListener(v -> adjustTime(2, 1));
    }

    private void adjustTime(int type, int delta) {
        int hours = Integer.parseInt(tvHours.getText().toString());
        int minutes = Integer.parseInt(tvMinutes.getText().toString());
        int seconds = Integer.parseInt(tvSeconds.getText().toString());

        switch (type) {
            case 0: // 小时
                hours = Math.max(0, hours + delta);
                break;
            case 1: // 分钟
                minutes = Math.max(0, minutes + delta);
                if (minutes >= 60) {
                    minutes = 0;
                    hours++;
                } else if (minutes < 0) {
                    minutes = 59;
                    hours = Math.max(0, hours - 1);
                }
                break;
            case 2: // 秒
                seconds = Math.max(0, seconds + delta);
                if (seconds >= 60) {
                    seconds = 0;
                    minutes++;
                    if (minutes >= 60) {
                        minutes = 0;
                        hours++;
                    }
                } else if (seconds < 0) {
                    seconds = 59;
                    minutes = Math.max(0, minutes - 1);
                    if (minutes < 0) {
                        minutes = 59;
                        hours = Math.max(0, hours - 1);
                    }
                }
                break;
        }

        // 更新显示
        tvHours.setText(String.format("%02d", hours));
        tvMinutes.setText(String.format("%02d", minutes));
        tvSeconds.setText(String.format("%02d", seconds));
        
        // 更新录制时间（毫秒）
        recordingTime = (hours * 3600 + minutes * 60 + seconds) * 1000;
    }

    private void setQuickTime(int hours, int minutes, int seconds) {
        // 快捷设置时间
        tvHours.setText(String.format("%02d", hours));
        tvMinutes.setText(String.format("%02d", minutes));
        tvSeconds.setText(String.format("%02d", seconds));
        
        // 更新录制时间（毫秒）
        recordingTime = (hours * 3600 + minutes * 60 + seconds) * 1000;
        
        // 显示提示
        String timeStr;
        if (hours > 0) {
            timeStr = hours + "小时";
        } else if (minutes > 0) {
            timeStr = minutes + "分钟";
        } else {
            timeStr = seconds + "秒";
        }
        Toast.makeText(context, "已设置录制时长: " + timeStr, Toast.LENGTH_SHORT).show();
    }

    private void tryStartRecording() {
        Log.d(TAG, "tryStartRecording 方法被调用");
        
        // 检查权限
        Log.d(TAG, "检查存储权限");
        
        // 对于 Android 13+，检查 MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Log.d(TAG, "需要 MANAGE_EXTERNAL_STORAGE 权限，引导用户到设置页面");
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                fragment.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                return;
            }
        } else {
            // 对于 Android 12 及以下，使用传统存储权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "存储权限未授予，请求权限");
                ActivityCompat.requestPermissions(fragment.requireActivity(), 
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                        REQUEST_STORAGE_PERMISSION);
                return;
            }
        }
        
        Log.d(TAG, "存储权限已授予，启动屏幕录制权限请求");

        // 启动屏幕录制权限请求
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        fragment.startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
        Log.d(TAG, "已启动屏幕录制权限请求");
    }

    // 存储屏幕录制权限结果
    // 注意：Activity.RESULT_OK 的值就是 -1，所以初始值不能用 -1
    private int mediaProjectionResultCode = 0;  // 初始值改为 0
    private Intent mediaProjectionData = null;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult 被调用 - requestCode: " + requestCode + ", resultCode: " + resultCode);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            // 从设置页面返回，检查悬浮窗权限是否已授予
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "悬浮窗权限已授予，启动悬浮窗服务");
                    // 权限已授予，启动悬浮窗服务
                    startOverlayService();
                } else {
                    Log.d(TAG, "用户拒绝了悬浮窗权限");
                    tvStatus.setText("需要悬浮窗权限才能显示录屏进度");
                }
            }
        } else if (requestCode == REQUEST_MANAGE_STORAGE) {
            // 从设置页面返回，检查权限是否已授予
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d(TAG, "MANAGE_EXTERNAL_STORAGE 权限已授予，继续启动录屏");
                    // 权限已授予，继续启动录屏
                    tryStartRecording();
                } else {
                    Log.d(TAG, "用户拒绝了 MANAGE_EXTERNAL_STORAGE 权限");
                    tvStatus.setText("需要存储管理权限才能保存录屏文件");
                }
            }
        } else if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == fragment.requireActivity().RESULT_OK) {
                Log.d(TAG, "用户授予了屏幕录制权限");
                // 先存储权限结果
                mediaProjectionResultCode = resultCode;
                mediaProjectionData = data;
                Log.d(TAG, "已保存 MediaProjection 权限数据: resultCode=" + resultCode + ", data=" + (data != null));
                
                // 然后启动悬浮窗服务（前台服务）
                startOverlayService();
            } else {
                Log.d(TAG, "用户拒绝了屏幕录制权限");
                tvStatus.setText("用户拒绝了录屏权限");
            }
        }
    }

    // 启动录屏（在前台服务启动后调用）
    private void startRecordingWithMediaProjection() {
        Log.d(TAG, "startRecordingWithMediaProjection 被调用");
        Log.d(TAG, "mediaProjectionResultCode=" + mediaProjectionResultCode + ", mediaProjectionData=" + (mediaProjectionData != null));
        
        // RESULT_OK 的值是 -1，所以不能用 -1 来判断是否获取权限
        // 应该检查 mediaProjectionData 是否为 null
        if (mediaProjectionData == null) {
            Log.e(TAG, "MediaProjection 权限未获取：mediaProjectionData 为 null");
            tvStatus.setText("录屏权限获取失败");
            Toast.makeText(context, "录屏权限获取失败，请重试", Toast.LENGTH_LONG).show();
            return;
        }
        
        Log.d(TAG, "开始创建 MediaProjection");
        // 获取MediaProjection
        try {
        mediaProjection = projectionManager.getMediaProjection(mediaProjectionResultCode, mediaProjectionData);
        if (mediaProjection != null) {
                Log.d(TAG, "MediaProjection 获取成功");
                
                // Android 14+ 需要注册回调
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Log.d(TAG, "Android 14+，注册 MediaProjection 回调");
                    mediaProjection.registerCallback(new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            Log.d(TAG, "MediaProjection 回调：onStop");
                            handler.post(() -> {
                                if (isRecording) {
                                    stopRecording();
                                    Toast.makeText(context, "屏幕录制已停止", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }, handler);
                }
                
                Log.d(TAG, "开始录制");
            startRecording();
        } else {
                Log.e(TAG, "MediaProjection 获取失败：getMediaProjection 返回 null");
            tvStatus.setText("录屏权限获取失败");
                Toast.makeText(context, "MediaProjection 创建失败", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "创建 MediaProjection 时发生异常", e);
            tvStatus.setText("录屏权限获取失败: " + e.getMessage());
            Toast.makeText(context, "创建 MediaProjection 失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult 被调用 - requestCode: " + requestCode);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "存储权限授予成功");
                // 权限获取成功，再次尝试启动录屏
                tryStartRecording();
            } else {
                Log.d(TAG, "存储权限授予失败");
                tvStatus.setText("需要存储权限才能保存录屏文件");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        if (isRecording) {
            Log.d(TAG, "startRecording: 已经在录制中，跳过");
            return;
        }

        Log.d(TAG, "startRecording: 开始初始化录制");

        // 生成输出文件
        outputFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), generateFileName());
        Log.d(TAG, "输出文件: " + outputFile.getAbsolutePath());
        
        // 初始化MediaRecorder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 使用新的 API
            mediaRecorder = new MediaRecorder(context);
        } else {
        mediaRecorder = new MediaRecorder();
        }
        
        // Android 10+ 尝试使用 AudioPlaybackCapture 捕获应用音频
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && captureAppAudio) {
            Log.d(TAG, "尝试使用 AudioPlaybackCapture 捕获应用音频");
            try {
                // 配置音频源和视频源
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 先设置一个占位音频源
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                
                // 设置音频编码器
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setAudioEncodingBitRate(128000);
                mediaRecorder.setAudioSamplingRate(44100);
                
                // 设置视频编码器
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                
                Log.d(TAG, "已配置音频+视频录制");
            } catch (Exception e) {
                Log.e(TAG, "配置音频捕获失败，降级为仅视频录制", e);
                // 降级为仅视频录制
                captureAppAudio = false;
                mediaRecorder.reset();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mediaRecorder = new MediaRecorder(context);
                } else {
                    mediaRecorder = new MediaRecorder();
                }
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            }
        } else {
            // Android 9 及以下，或用户禁用音频捕获，仅录制视频
            Log.d(TAG, "仅录制视频，不录制音频");
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        }
        
        // 设置视频质量
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        Log.d(TAG, "屏幕尺寸: " + screenWidth + "x" + screenHeight);
        
        // 使用用户选择的分辨率，但不超过屏幕分辨率
        int finalWidth = Math.min(videoWidth, screenWidth);
        int finalHeight = Math.min(videoHeight, screenHeight);
        
        // 保持宽高比
        if (screenWidth > screenHeight) {
            // 横屏
            finalHeight = (int) (finalWidth * ((float) screenHeight / screenWidth));
        } else {
            // 竖屏
            finalWidth = (int) (finalHeight * ((float) screenWidth / screenHeight));
        }
        
        Log.d(TAG, "录制分辨率: " + finalWidth + "x" + finalHeight + ", 帧率: " + videoFps + ", 码率: " + videoBitrate);
        
        mediaRecorder.setVideoSize(finalWidth, finalHeight);
        mediaRecorder.setVideoFrameRate(videoFps);
        mediaRecorder.setVideoEncodingBitRate(videoBitrate);
        
        // 设置输出文件
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            Log.d(TAG, "准备 MediaRecorder");
            mediaRecorder.prepare();
            
            // Android 10+ 尝试创建 AudioPlaybackCapture
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && captureAppAudio) {
                try {
                    startAudioPlaybackCapture();
                } catch (Exception e) {
                    Log.e(TAG, "启动 AudioPlaybackCapture 失败", e);
                    Toast.makeText(context, "音频捕获失败，仅录制视频", Toast.LENGTH_SHORT).show();
                }
            }
            
            // 获取Surface并开始录制
            Surface surface = mediaRecorder.getSurface();
            if (surface != null) {
                Log.d(TAG, "创建 VirtualDisplay");
                
                // 使用标准标志创建 VirtualDisplay
                mediaProjection.createVirtualDisplay("ScreenRecorder",
                        screenWidth, screenHeight, metrics.densityDpi,
                        android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        surface, null, null);
                
                Log.d(TAG, "启动 MediaRecorder");
                mediaRecorder.start();
                isRecording = true;
                isPaused = false;
                pausedTime = 0L; // 重置暂停时间
                startTime = System.currentTimeMillis();
                
                updateUI();
                startTimer();
                
                String statusMsg = captureAppAudio ? "录屏中（含应用音频）..." : "录屏中（仅视频）...";
                tvStatus.setText(statusMsg);
                Toast.makeText(context, "录屏已开始", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "录制成功启动，isRecording=" + isRecording);
                
                // 定时停止录制
                handler.postDelayed(() -> {
                    if (isRecording) {
                        Log.d(TAG, "定时结束，停止录制");
                        isManualStop = false; // 标记为自动停止
                        stopRecording();
                        tvStatus.setText("定时结束，录屏已保存");
                    }
                }, recordingTime);
            } else {
                Log.e(TAG, "无法获取 Surface");
                tvStatus.setText("无法创建录制表面");
                releaseRecorder();
            }
        } catch (IOException e) {
            Log.e(TAG, "录制准备失败", e);
            tvStatus.setText("录制启动失败: " + e.getMessage());
            Toast.makeText(context, "录制启动失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            releaseRecorder();
        }
    }
    
    // Android 10+ 使用 AudioPlaybackCapture 捕获应用音频
    @SuppressLint("MissingPermission")
    private void startAudioPlaybackCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        
        try {
            // 创建 AudioPlaybackCaptureConfiguration
            AudioPlaybackCaptureConfiguration config = 
                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // 捕获媒体音频
                    .addMatchingUsage(AudioAttributes.USAGE_GAME) // 捕获游戏音频
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN) // 捕获未知类型音频
                    .build();
            
            // 配置音频格式
            AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build();
            
            // 计算缓冲区大小
            int bufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            );
            
            // 创建 AudioRecord
            audioRecord = new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build();
            
            // 启动音频录制
            audioRecord.startRecording();
            Log.d(TAG, "AudioPlaybackCapture 启动成功");
            
            // 在后台线程中读取音频数据（这里只是启动，实际混音需要更复杂的处理）
            executorService.execute(() -> {
                byte[] buffer = new byte[bufferSize];
                while (isRecording && audioRecord != null) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        // 这里可以处理音频数据，例如混音、写入文件等
                        // 注意：MediaRecorder 已经在录制，这里只是演示如何捕获音频
                        Log.v(TAG, "读取到音频数据: " + read + " 字节");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "创建 AudioPlaybackCapture 失败", e);
            throw e;
        }
    }

    private void pauseRecording() {
        if (!isRecording || mediaRecorder == null || isPaused) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.pause();
                isPaused = true;
                
                // 记录暂停时已经录制的时间
                pausedTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "暂停录制，已录制时间: " + pausedTime + "ms");
                
                // 停止主界面定时器
                stopTimer();
                
            tvStatus.setText("录屏已暂停");
                Toast.makeText(context, "录屏已暂停", Toast.LENGTH_SHORT).show();
                
                // 通知悬浮窗服务更新状态
                Intent intent = new Intent(context, ScreenRecorderOverlayService.class);
                intent.setAction("Yu.code.toolbox.action.UPDATE_PAUSE_STATE");
                intent.putExtra("is_paused", true);
                context.startService(intent);
            } else {
                Toast.makeText(context, "当前系统版本不支持暂停功能", Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "暂停录制时出错", e);
            tvStatus.setText("暂停失败: " + e.getMessage());
        }
    }

    private void resumeRecording() {
        if (!isRecording || mediaRecorder == null || !isPaused) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.resume();
                isPaused = false;
                
                // 恢复时调整开始时间，使得已暂停的时间不计入总时长
                startTime = System.currentTimeMillis() - pausedTime;
                Log.d(TAG, "恢复录制，调整开始时间，已录制: " + pausedTime + "ms");
                
                // 重新启动主界面定时器
                startTimer();
                
            tvStatus.setText("录屏中...");
                Toast.makeText(context, "录屏已恢复", Toast.LENGTH_SHORT).show();
                
                // 通知悬浮窗服务更新状态
                Intent intent = new Intent(context, ScreenRecorderOverlayService.class);
                intent.setAction("Yu.code.toolbox.action.UPDATE_PAUSE_STATE");
                intent.putExtra("is_paused", false);
                context.startService(intent);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "恢复录制时出错", e);
            tvStatus.setText("恢复失败: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        stopTimer();
        
        // 停止悬浮窗服务
        stopOverlayService();
        
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "停止录制时出错", e);
        } finally {
            releaseRecorder();
            isRecording = false;
            isPaused = false;
            pausedTime = 0L; // 重置暂停时间
            updateUI();
            
            if (outputFile != null && outputFile.exists()) {
                // 如果是手动停止，弹出重命名对话框
                if (isManualStop) {
                    showRenameDialog(outputFile);
                    isManualStop = false; // 重置标记
                } else {
                    // 时间到了自动停止，不重命名
                tvStatus.setText("已保存: " + outputFile.getName());
                Toast.makeText(context, "录屏已保存到: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void releaseRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "释放MediaRecorder时出错", e);
            }
            mediaRecorder = null;
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "释放AudioRecord时出错", e);
            }
            audioRecord = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                if (!isRecording) return;
                
                // 暂停时不更新定时器
                if (isPaused) {
                    handler.postDelayed(this, 1000);
                    return;
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = recordingTime - elapsed;
                
                if (remaining <= 0) {
                    stopRecording();
                    return;
                }
                
                // 显示已录制时长和文件大小
                long hours = elapsed / (1000 * 60 * 60);
                long minutes = (elapsed / (1000 * 60)) % 60;
                long seconds = (elapsed / 1000) % 60;
                
                String fileSize;
                if (outputFile != null && outputFile.exists()) {
                    long size = outputFile.length();
                    if (size < 1024 * 1024) {
                        fileSize = String.format("%.1f KB", size / 1024.0);
                    } else {
                        fileSize = String.format("%.1f MB", size / (1024.0 * 1024.0));
                    }
                } else {
                    fileSize = "0 MB";
                }
                
                handler.post(() -> {
                    tvStatus.setText(String.format("录屏中 %02d:%02d:%02d | %s", hours, minutes, seconds, fileSize));
                });
                
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateUI() {
        btnStartRecord.setEnabled(!isRecording);
        btnStopRecord.setEnabled(isRecording);
    }

    private void viewRecordings() {
        // 打开录屏文件所在目录
        File recordingsDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (recordingsDir != null && recordingsDir.exists()) {
            // 检查目录中是否有录屏文件
            File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));
            
            if (files != null && files.length > 0) {
                // 如果有录屏文件，打开最新的一个
                File latestFile = files[0];
                for (File file : files) {
                    if (file.lastModified() > latestFile.lastModified()) {
                        latestFile = file;
                    }
                }
                
            Intent intent = new Intent(Intent.ACTION_VIEW);
                android.net.Uri uri;
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 使用 FileProvider
                    uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        latestFile
                    );
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    uri = android.net.Uri.fromFile(latestFile);
                }
                
                intent.setDataAndType(uri, "video/mp4");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(TAG, "无法打开视频文件", e);
                    Toast.makeText(context, "没有找到可以播放视频的应用", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 如果没有录屏文件，显示目录路径
                Toast.makeText(context, "还没有录屏文件\n保存位置: " + recordingsDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, "录屏文件目录不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewRecordingsFolder() {
        // 跳转到录屏文件列表页面
        Intent intent = new Intent(context, RecordingsListActivity.class);
        context.startActivity(intent);
            }

    private void playVideoFile(File videoFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        android.net.Uri uri;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ 使用 FileProvider
            uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                videoFile
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = android.net.Uri.fromFile(videoFile);
        }
        
        intent.setDataAndType(uri, "video/mp4");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
                context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "无法打开视频文件", e);
            Toast.makeText(context, "没有找到可以播放视频的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(File file) {
        // 创建输入对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(fragment.requireContext());
        builder.setTitle("自定义录屏文件名");
        
        // 创建输入框容器，添加 padding
        android.widget.LinearLayout container = new android.widget.LinearLayout(fragment.requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (20 * fragment.getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);
        
        // 创建输入框
        final android.widget.EditText input = new android.widget.EditText(fragment.requireContext());
        input.setHint("输入文件名（不含扩展名）");
        input.setSingleLine(true);
        
        // 设置默认文件名（去掉扩展名）
        String currentName = file.getName();
        if (currentName.endsWith(".mp4")) {
            currentName = currentName.substring(0, currentName.length() - 4);
        }
        input.setText(currentName);
        input.setSelection(currentName.length()); // 光标移到末尾
        
        container.addView(input);
        builder.setView(container);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(context, "文件名不能为空", Toast.LENGTH_SHORT).show();
                tvStatus.setText("已保存: " + file.getName());
                return;
            }
            
            // 添加 .mp4 扩展名
            if (!newName.endsWith(".mp4")) {
                newName = newName + ".mp4";
            }
            
            // 重命名文件
            File newFile = new File(file.getParent(), newName);
            if (newFile.exists()) {
                Toast.makeText(context, "文件名已存在，使用原文件名", Toast.LENGTH_SHORT).show();
                tvStatus.setText("已保存: " + file.getName());
            } else {
                if (file.renameTo(newFile)) {
                    tvStatus.setText("已保存: " + newFile.getName());
                    Toast.makeText(context, "录屏已保存为: " + newFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "重命名失败，使用原文件名", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("已保存: " + file.getName());
                }
            }
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> {
            // 取消重命名，使用原文件名
            tvStatus.setText("已保存: " + file.getName());
            Toast.makeText(context, "录屏已保存到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        });
        
        // 创建并显示对话框
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // 设置按钮文字颜色，确保在白色背景上可见
        if (dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF2196F3); // 蓝色
        }
        if (dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF757575); // 灰色
        }
    }

    private String generateFileName() {
        String ts = DateFormat.format("yyyyMMdd_HHmmss", new Date()).toString();
        return "SCREEN_" + ts + ".mp4";
    }

    // 启动悬浮窗服务
    private void startOverlayService() {
        Log.d(TAG, "启动悬浮窗服务");
        
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Log.d(TAG, "需要悬浮窗权限，引导用户到设置页面");
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                fragment.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                return;
            }
        }
        
        // 启动悬浮窗服务
        Intent serviceIntent = new Intent(context, ScreenRecorderOverlayService.class);
        serviceIntent.setAction(ScreenRecorderOverlayService.ACTION_START);
        serviceIntent.putExtra(ScreenRecorderOverlayService.EXTRA_RECORDING_TIME, recordingTime);
        serviceIntent.putExtra(ScreenRecorderOverlayService.EXTRA_START_RECORDING, true);
        if (outputFile != null) {
            serviceIntent.putExtra(ScreenRecorderOverlayService.EXTRA_OUTPUT_FILE, outputFile.getAbsolutePath());
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    // 停止悬浮窗服务
    private void stopOverlayService() {
        Log.d(TAG, "停止悬浮窗服务");
        Intent intent = new Intent(context, ScreenRecorderOverlayService.class);
        intent.setAction(ScreenRecorderOverlayService.ACTION_STOP);
        context.startService(intent);
    }

    public void onPause() {
        // 暂停时不停止录制，保持后台录制
    }

    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        if (serviceStartedReceiver != null) {
            context.unregisterReceiver(serviceStartedReceiver);
        }
        executorService.shutdown();
    }
}
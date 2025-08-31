package Yu.code.toolbox.function.recorder_photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import Yu.code.toolbox.R;
import Yu.code.toolbox.ui.home.HomeFragment;
import Yu.code.toolbox.ui.function.FunctionConstants;
import Yu.code.toolbox.ui.function.FunctionFragment;
import Yu.code.toolbox.Utils.DialogUtils;

/**
 * 录音播放列表管理器
 */
public class RecordingPlaylistManager {

    private Fragment fragment;
    private View rootView;
    private RecordingAdapter adapter;
    private MediaPlayer mediaPlayer;
    private int currentPlayingPosition = -1;
    private Handler photoUpdateHandler = new Handler();
    private Runnable photoUpdateRunnable;

    private RecyclerView rvRecordings;
    private TextView tvEmpty;
    private ImageView ivPhoto;

    public RecordingPlaylistManager(Fragment fragment, View rootView) {
        this.fragment = fragment;
        this.rootView = rootView;
        initViews();
        setupRecyclerView();
        loadRecordings();
    }

    private void initViews() {
        rvRecordings = rootView.findViewById(R.id.rv_recordings);
        tvEmpty = rootView.findViewById(R.id.tv_empty);
        ivPhoto = rootView.findViewById(R.id.iv_photo);

        // 设置返回按钮点击事件
        rootView.findViewById(R.id.btn_back).setOnClickListener(v -> {
            stopCurrentPlayback();
            // 直接调用父Fragment的方法切换界面
            if (fragment instanceof FunctionFragment) {
                ((FunctionFragment) fragment).showFunction(FunctionConstants.FUNCTION_RECORDER);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new RecordingAdapter();
        rvRecordings.setLayoutManager(new LinearLayoutManager(fragment.getContext()));
        rvRecordings.setAdapter(adapter);

        // 设置点击监听器
        adapter.setOnItemClickListener(new RecordingAdapter.OnItemClickListener() {
            @Override
            public void onPlayPauseClick(int position) {
                handlePlayPauseClick(position);
            }

            @Override
            public void onDeleteClick(int position) {
                handleDeleteClick(position);
            }
        });
    }

    private void loadRecordings() {
        File recordingsDir = fragment.getContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (recordingsDir == null || !recordingsDir.exists()) {
            showEmptyState();
            return;
        }

        File[] files = recordingsDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".m4a") ||
                        name.toLowerCase().endsWith(".mp3") ||
                        name.toLowerCase().endsWith(".wav"));

        if (files == null || files.length == 0) {
            showEmptyState();
            return;
        }

        List<RecordingItem> recordings = new ArrayList<>();
        for (File file : files) {
            RecordingItem item = new RecordingItem(file);
            // 加载该录音对应的照片信息
            loadPhotosForRecording(item);
            recordings.add(item);
        }

        // 按创建时间倒序排列（最新的在前面）
        Collections.sort(recordings, (o1, o2) ->
                Long.compare(o2.getCreateTime().getTime(), o1.getCreateTime().getTime()));

        adapter.setRecordings(recordings);
        hideEmptyState();
    }

    private void loadPhotosForRecording(RecordingItem recordingItem) {
        try {
            // 查找对应的照片信息文件
            String photoInfoFileName = recordingItem.getFileName().replace(".m4a", "_photos.json");
            File photoInfoFile = new File(recordingItem.getFile().getParent(), photoInfoFileName);

            if (photoInfoFile.exists()) {
                // 读取JSON文件
                FileReader reader = new FileReader(photoInfoFile);
                StringBuilder jsonString = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    jsonString.append(buffer, 0, bytesRead);
                }
                reader.close();

                // 解析JSON
                JSONObject jsonObject = new JSONObject(jsonString.toString());
                JSONArray photosArray = jsonObject.getJSONArray("photos");

                for (int i = 0; i < photosArray.length(); i++) {
                    JSONObject photoObject = photosArray.getJSONObject(i);
                    long timestamp = photoObject.getLong("timestamp");
                    String photoPath = photoObject.getString("photo_path");

                    // 检查照片文件是否存在
                    File photoFile = new File(photoPath);
                    if (photoFile.exists()) {
                        RecordingPhotoItem photoItem = new RecordingPhotoItem(timestamp, photoPath);
                        recordingItem.addPhoto(photoItem);
                    }
                }

                Log.d("RecordingPlaylistManager", "为录音 " + recordingItem.getFileName() +
                        " 加载了 " + recordingItem.getPhotoCount() + " 张照片");
            }
        } catch (Exception e) {
            Log.e("RecordingPlaylistManager", "加载照片信息失败: " + recordingItem.getFileName(), e);
        }
    }

    private void handlePlayPauseClick(int position) {
        if (currentPlayingPosition == position) {
            // 当前正在播放，暂停
            pausePlayback();
        } else {
            // 播放新的录音
            playRecording(position);
        }
    }

    private void handleDeleteClick(int position) {
        // 检查 position 是否有效
        if (position < 0 || position >= adapter.getRecordings().size()) {
            Toast.makeText(fragment.getContext(), "无效的录音文件索引", Toast.LENGTH_SHORT).show();
            return;
        }

        RecordingItem item = adapter.getRecordings().get(position);

        DialogUtils.createDangerConfirmDialog(fragment.getContext(), "确认删除",
                "确定要删除录音文件 \"" + item.getFileName() + "\" 吗？",
                "删除", "取消",
                (dialog, which) -> {
                    // 再次检查 position 是否仍然有效
                    if (position < 0 || position >= adapter.getRecordings().size()) {
                        Toast.makeText(fragment.getContext(), "录音文件列表已发生变化，请重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteRecording(position);
                },
                null);
    }

    private void playRecording(int position) {
        // 检查 position 是否有效
        if (position < 0 || position >= adapter.getRecordings().size()) {
            Toast.makeText(fragment.getContext(), "无效的录音文件索引", Toast.LENGTH_SHORT).show();
            return;
        }

        stopCurrentPlayback();

        RecordingItem item = adapter.getRecordings().get(position);
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(item.getFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                stopCurrentPlayback();
            });

            currentPlayingPosition = position;
            adapter.setPlayingItem(position);

            // 启动照片更新定时器
            startPhotoUpdateTimer(item);

            Toast.makeText(fragment.getContext(), "开始播放: " + item.getFileName(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(fragment.getContext(), "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopCurrentPlayback();
        }
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            adapter.setPlayingItem(-1); // 清除播放状态
            currentPlayingPosition = -1;
        }
    }



    private void deleteRecording(int position) {
        // 最终的安全检查
        if (position < 0 || position >= adapter.getRecordings().size()) {
            Toast.makeText(fragment.getContext(), "录音文件索引无效", Toast.LENGTH_SHORT).show();
            return;
        }

        RecordingItem item = adapter.getRecordings().get(position);
        File file = item.getFile();

        if (file.exists() && file.delete()) {
            // 如果正在播放被删除的文件，停止播放
            if (currentPlayingPosition == position) {
                stopCurrentPlayback();
            }

            adapter.removeItem(position);
            Toast.makeText(fragment.getContext(), "已删除录音文件", Toast.LENGTH_SHORT).show();

            // 检查是否还有录音文件
            if (adapter.getItemCount() == 0) {
                showEmptyState();
            }
        } else {
            Toast.makeText(fragment.getContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyState() {
        rvRecordings.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        rvRecordings.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    public void onDestroy() {
        stopCurrentPlayback();
        stopPhotoUpdateTimer();
        photoUpdateHandler.removeCallbacksAndMessages(null);
    }

    public void onPause() {
        // 暂停播放
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pausePlayback();
        }
    }

    // 照片显示相关方法
    private void updatePhotoDisplay(RecordingItem item, long currentPosition) {
        if (item.hasPhotos()) {
            RecordingPhotoItem photoItem = item.getPhotoAtTime(currentPosition);
            if (photoItem != null && photoItem.isPhotoExists()) {
                showPhoto(photoItem.getPhotoPath());
                Log.d("RecordingPlaylistManager", "显示照片: " + photoItem.getFormattedTimestamp());
            } else {
                hidePhoto();
            }
        } else {
            hidePhoto();
        }
    }

    private void showPhoto(String photoPath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            if (bitmap != null) {
                ivPhoto.setImageBitmap(bitmap);
                ivPhoto.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            hidePhoto();
        }
    }

    private void hidePhoto() {
        ivPhoto.setVisibility(View.GONE);
    }

    private void startPhotoUpdateTimer(RecordingItem item) {
        // 停止之前的定时器
        stopPhotoUpdateTimer();

        photoUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    updatePhotoDisplay(item, mediaPlayer.getCurrentPosition());
                    // 每秒检查一次
                    photoUpdateHandler.postDelayed(this, 1000);
                }
            }
        };

        // 立即开始第一次检查
        photoUpdateHandler.post(photoUpdateRunnable);
    }

    private void stopPhotoUpdateTimer() {
        if (photoUpdateRunnable != null) {
            photoUpdateHandler.removeCallbacks(photoUpdateRunnable);
            photoUpdateRunnable = null;
        }
    }

    private void stopCurrentPlayback() {
        // 停止照片更新定时器
        stopPhotoUpdateTimer();

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mediaPlayer = null;
            }
        }
        currentPlayingPosition = -1;
        adapter.setPlayingItem(-1);
        hidePhoto(); // 停止播放时隐藏照片
    }
}

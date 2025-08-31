package Yu.code.toolbox.ui.function;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import Yu.code.toolbox.R;
import Yu.code.toolbox.function.statistics.StatisticsManager;
import Yu.code.toolbox.function.recorder_photo.RecordingPlaylistManager;

public class FunctionFragment extends Fragment {

    private FrameLayout container;
    private int currentLayoutId = -1;
    private RecorderManager recorderManager;
    private StatisticsManager statisticsManager;
    private RecordingPlaylistManager recordingPlaylistManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_function, container, false);
        this.container = root.findViewById(R.id.function_container);
        return root;
    }

    // 显示指定的功能布局
    public void showFunction(int layoutId) {
        if (currentLayoutId != layoutId) {
            container.removeAllViews();
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, true);
            currentLayoutId = layoutId;

            // 根据布局ID初始化相应的功能管理器
            initFunctionManager(layoutId, functionView);
        }
    }

    // 隐藏功能界面
    public void hideFunction() {
        // 清理当前功能管理器
        cleanupCurrentManager();

        container.removeAllViews();
        currentLayoutId = -1;
    }

    // 获取当前显示的功能布局ID
    public int getCurrentLayoutId() {
        return currentLayoutId;
    }

    private void initFunctionManager(int layoutId, View functionView) {
        // 清理之前的管理器
        cleanupCurrentManager();

        // 根据布局ID初始化相应的管理器
        if (layoutId == R.layout.function_recorder) {
            recorderManager = new RecorderManager(this, functionView);
        } else if (layoutId == R.layout.function_statistics) {
            statisticsManager = new StatisticsManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_recorder_playlist) {
            recordingPlaylistManager = new RecordingPlaylistManager(this, functionView);
        }
        // 在这里可以添加其他功能的管理器
    }

    private void cleanupCurrentManager() {
        if (recorderManager != null) {
            recorderManager.onDestroy();
            recorderManager = null;
        }
        if (statisticsManager != null) {
            statisticsManager.onDestroy();
            statisticsManager = null;
        }
        if (recordingPlaylistManager != null) {
            recordingPlaylistManager.onDestroy();
            recordingPlaylistManager = null;
        }
        // 在这里可以清理其他功能的管理器
    }

    @Override
    public void onPause() {
        super.onPause();
        if (recorderManager != null) {
            recorderManager.onPause();
        }
        if (recordingPlaylistManager != null) {
            recordingPlaylistManager.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupCurrentManager();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (recorderManager != null) {
            recorderManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (recorderManager != null) {
            recorderManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}

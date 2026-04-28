package Yu.code.toolbox.ui.function;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import Yu.code.toolbox.R;
import Yu.code.toolbox.function.changelog.ChangelogManager;
import Yu.code.toolbox.function.recorder_photo.ScreenRecorderManager;
import Yu.code.toolbox.function.statistics.StatisticsManager;
import Yu.code.toolbox.function.recorder_photo.RecordingPlaylistManager;
import Yu.code.toolbox.function.time.TimerManager;
import Yu.code.toolbox.function.lottery.LotteryManager;
import Yu.code.toolbox.function.lottery.LotteryManageManager;

public class FunctionFragment extends Fragment {

    private FrameLayout container;
    private int currentLayoutId = -1;
    private RecorderManager recorderManager;
    private StatisticsManager statisticsManager;
    private RecordingPlaylistManager recordingPlaylistManager;
    private TimerManager timerManager;
    private LotteryManager lotteryManager;
    private LotteryManageManager lotteryManageManager;
    private ScreenRecorderManager screenRecorderManager;
    private ChangelogManager changelogManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_function, container, false);
        this.container = root.findViewById(R.id.function_container);

        // 检查是否有传递的功能ID
        if (getArguments() != null) {
            int functionId = getArguments().getInt("function_id", -1);
            if (functionId != -1) {
                showFunction(functionId);
                
                // 检查是否有要导入的文件URI（用于统计功能）
                if (functionId == R.layout.function_statistics) {
                    android.net.Uri importFileUri = getArguments().getParcelable("import_file_uri");
                    if (importFileUri != null && statisticsManager != null) {
                        // 延迟一下，确保StatisticsManager已经初始化完成
                        new android.os.Handler().postDelayed(() -> {
                            if (statisticsManager != null) {
                                statisticsManager.importFromFileUri(importFileUri);
                            }
                        }, 300);
                    }
                }
            }
        }

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
        } else if (layoutId == R.layout.function_timer) {
            timerManager = new TimerManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_lottery) {
            lotteryManager = new LotteryManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_lottery_manage) {
            lotteryManageManager = new LotteryManageManager(requireContext(), functionView);
            // 设置数据回传监听器
            lotteryManageManager.setOnDataChangeListener(items -> {
                // 当管理页面的数据发生变化时，更新抽奖页面的数据
                if (lotteryManager != null) {
                    // 这里需要实现数据同步逻辑
                    // 暂时用Toast提示
                    Toast.makeText(requireContext(), "数据已更新", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (layoutId == R.layout.function_screen_recorder) {
            screenRecorderManager = new ScreenRecorderManager(this, functionView);
        } else if (layoutId == R.layout.function_changelog) {
            changelogManager = new ChangelogManager(requireContext(), functionView);
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
        if (timerManager != null) {
            timerManager.onDestroy();
            timerManager = null;
        }
        if (lotteryManager != null) {
            lotteryManager.onDestroy();
            lotteryManager = null;
        }
        if (lotteryManageManager != null) {
            lotteryManageManager = null;
        }
        if (screenRecorderManager != null) {
            screenRecorderManager.onDestroy();
            screenRecorderManager = null;
        }
        if (changelogManager != null) {
            changelogManager = null;
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
        if (screenRecorderManager != null) {
            screenRecorderManager.onPause();
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
        if (screenRecorderManager != null) {
            screenRecorderManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (recorderManager != null) {
            recorderManager.onActivityResult(requestCode, resultCode, data);
        }
        if (screenRecorderManager != null) {
            screenRecorderManager.onActivityResult(requestCode, resultCode, data);
        }
        if (statisticsManager != null) {
            statisticsManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}

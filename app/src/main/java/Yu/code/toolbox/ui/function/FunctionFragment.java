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
import Yu.code.toolbox.function.lottery.LotteryManageManager;
import Yu.code.toolbox.function.lottery.LotteryManager;
import Yu.code.toolbox.function.recorder_photo.RecordingPlaylistManager;
import Yu.code.toolbox.function.recorder_photo.ScreenRecorderManager;
import Yu.code.toolbox.function.statistics.StatisticsManager;
import Yu.code.toolbox.function.time.TimerManager;
import Yu.code.toolbox.function.alarm.AlarmFunctionManager;
import Yu.code.toolbox.ui.function.todo.TodoFunctionManager;
import Yu.code.toolbox.ui.function.RecorderManager;
import Yu.code.toolbox.ui.function.cookbook.CookBookFragment;

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
    private CookBookFragment cookBookFragment;
    private TodoFunctionManager todoFunctionManager;
    private AlarmFunctionManager alarmFunctionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_function, container, false);
        this.container = root.findViewById(R.id.function_container);

        if (getArguments() != null) {
            int functionId = getArguments().getInt("function_id", -1);
            if (functionId != -1) {
                showFunction(functionId);

                if (functionId == R.layout.function_alarm
                        && getArguments().getBoolean("auto_time_sheet", false)
                        && alarmFunctionManager != null) {
                    getArguments().remove("auto_time_sheet");
                    new android.os.Handler().postDelayed(() -> {
                        if (alarmFunctionManager != null) alarmFunctionManager.openTimeSheet();
                    }, 300);
                }

                if (functionId == R.layout.function_statistics) {
                    android.net.Uri importFileUri = getArguments().getParcelable("import_file_uri");
                    if (importFileUri != null && statisticsManager != null) {
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

    public void showFunction(int layoutId) {
        if (currentLayoutId == layoutId) {
            return;
        }
        container.removeAllViews();
        currentLayoutId = layoutId;
        initFunctionManager(layoutId);
    }

    public void hideFunction() {
        cleanupCurrentManager();
        container.removeAllViews();
        currentLayoutId = -1;
    }

    public int getCurrentLayoutId() {
        return currentLayoutId;
    }

    private void initFunctionManager(int layoutId) {
        cleanupCurrentManager();

        if (layoutId == R.layout.function_recorder) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            recorderManager = new RecorderManager(this, functionView);
        } else if (layoutId == R.layout.function_statistics) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            statisticsManager = new StatisticsManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_recorder_playlist) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            recordingPlaylistManager = new RecordingPlaylistManager(this, functionView);
        } else if (layoutId == R.layout.function_timer) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            timerManager = new TimerManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_lottery) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            lotteryManager = new LotteryManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_lottery_manage) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            lotteryManageManager = new LotteryManageManager(requireContext(), functionView);
            lotteryManageManager.setOnDataChangeListener(items -> {
                if (lotteryManager != null) {
                    Toast.makeText(requireContext(), "数据已更新", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (layoutId == R.layout.function_screen_recorder) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            screenRecorderManager = new ScreenRecorderManager(this, functionView);
        } else if (layoutId == R.layout.function_changelog) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            changelogManager = new ChangelogManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_todo) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            todoFunctionManager = new TodoFunctionManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_alarm) {
            View functionView = LayoutInflater.from(requireContext()).inflate(layoutId, container, false);
            container.addView(functionView);
            alarmFunctionManager = new AlarmFunctionManager(requireContext(), functionView);
        } else if (layoutId == R.layout.function_cookbook) {
            cookBookFragment = new CookBookFragment();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.function_container, cookBookFragment)
                    .commitNowAllowingStateLoss();
        }
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
        lotteryManageManager = null;
        if (screenRecorderManager != null) {
            screenRecorderManager.onDestroy();
            screenRecorderManager = null;
        }
        changelogManager = null;
        todoFunctionManager = null;
        if (alarmFunctionManager != null) {
            alarmFunctionManager.onDestroy();
            alarmFunctionManager = null;
        }
        if (cookBookFragment != null) {
            getChildFragmentManager().beginTransaction().remove(cookBookFragment).commitNowAllowingStateLoss();
            cookBookFragment = null;
        }
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

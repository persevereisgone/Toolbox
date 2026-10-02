package Yu.code.toolbox.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Yu.code.toolbox.R;
import Yu.code.toolbox.databinding.FragmentHomeBinding;
import Yu.code.toolbox.function.alarm.AlarmStore;
import Yu.code.toolbox.ui.function.FunctionConstants;
import Yu.code.toolbox.ui.function.todo.TodoItem;
import Yu.code.toolbox.ui.function.todo.TodoManager;

public class HomeFragment extends Fragment {

    private static final String PREFS_HOME_DEBUG = "home_debug_prefs";
    private static final String KEY_BETA_UNLOCKED = "beta_unlocked";
    private static final String KEY_PINNED_IDS = "pinned_ids";
    private static final String KEY_CHANGELOG_LAST_SEEN_SIGNATURE = "changelog_last_seen_signature";
    private static final int DEV_TAP_REQUIRED = 5;
    private static final long DEV_TAP_TIMEOUT_MS = 1200L;

    private FragmentHomeBinding binding;
    private RecyclerView functionList;
    private HomeFunctionAdapter functionAdapter;
    private List<HomeFunctionAdapter.Item> allFunctions = new ArrayList<>();
    private RecyclerView betaRecyclerView;
    private BetaGridAdapter betaAdapter;
    private SharedPreferences debugPreferences;
    private boolean betaUnlocked;
    private int devTapCount;
    private long lastDevTapAt;

    private static HomeFragment currentInstance;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        debugPreferences = requireContext().getSharedPreferences(PREFS_HOME_DEBUG, Context.MODE_PRIVATE);
        betaUnlocked = debugPreferences.getBoolean(KEY_BETA_UNLOCKED, false);

        setupTitle();
        setupFunctionList(root);
        setupBeta(root);
        setupDeveloperEntry();

        currentInstance = this;
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 返回首页刷新状态副标题(计时剩余/待办/闹钟)
        refreshStatuses();
    }

    private void setupTitle() {
        String full = "常用操作，一步即达";
        SpannableString ss = new SpannableString(full);
        int start = full.indexOf("一步即达");
        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.app_primary)),
                start, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.homeTitle.setText(ss);
    }

    private void setupFunctionList(View root) {
        functionList = root.findViewById(R.id.home_panel_grid);
        functionList.setLayoutManager(new LinearLayoutManager(requireContext()));
        functionAdapter = new HomeFunctionAdapter(requireContext());
        functionAdapter.setOnItemClickListener(item -> navigateToFunction(item.functionId));
        functionAdapter.setPinnedIds(getPinnedIds());
        functionAdapter.setOnPinClickListener(item -> togglePin(item.functionId));
        functionList.setAdapter(functionAdapter);

        buildFunctions();
        functionAdapter.setItems(sortedWithPins(allFunctions));

        binding.homeSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterFunctions(s.toString().trim());
            }
        });
        binding.tvHomeAll.setOnClickListener(v -> showAllFunctionsSheet());
        updateAllEntryText();
    }

    private void buildFunctions() {
        allFunctions = new ArrayList<>();
        allFunctions.add(new HomeFunctionAdapter.Item(
                R.drawable.time, "计时器", timerStatus(),
                FunctionConstants.FUNCTION_TIMER, R.color.app_primary_light, R.color.app_primary));
        allFunctions.add(new HomeFunctionAdapter.Item(
                R.drawable.recipe, "菜谱", "收藏 · 菜单",
                FunctionConstants.FUNCTION_COOKBOOK, R.color.app_danger_light, R.color.app_danger));
        allFunctions.add(new HomeFunctionAdapter.Item(
                R.drawable.ic_todo_24, "待办", todoStatus(),
                FunctionConstants.FUNCTION_TODO, R.color.app_success_light, R.color.app_success));
        allFunctions.add(new HomeFunctionAdapter.Item(
                R.drawable.ic_alarm_24, "闹钟", alarmStatus(),
                FunctionConstants.FUNCTION_ALARM, R.color.app_accent_light, R.color.app_accent));
        allFunctions.add(new HomeFunctionAdapter.Item(
                R.drawable.statistics, "统计", "用量 · 导入",
                FunctionConstants.FUNCTION_STATISTICS, R.color.app_info_light, R.color.app_info));
    }

    private void refreshStatuses() {
        if (functionAdapter == null) return;
        buildFunctions();
        updateAllEntryText();
        String keyword = binding.homeSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            functionAdapter.setItems(sortedWithPins(allFunctions));
        } else {
            filterFunctions(keyword);
        }
    }

    private java.util.Set<String> getPinnedIds() {
        return new java.util.HashSet<>(debugPreferences.getStringSet(KEY_PINNED_IDS, new java.util.HashSet<>()));
    }

    private void togglePin(int functionId) {
        java.util.Set<String> pinned = getPinnedIds();
        String key = String.valueOf(functionId);
        boolean nowPinned;
        if (pinned.contains(key)) { pinned.remove(key); nowPinned = false; }
        else { pinned.add(key); nowPinned = true; }
        debugPreferences.edit().putStringSet(KEY_PINNED_IDS, pinned).apply();
        applyPinsAndSort();
        android.widget.Toast.makeText(requireContext(), nowPinned ? "已置顶，排在最前面" : "已取消置顶", android.widget.Toast.LENGTH_SHORT).show();
    }

    private java.util.List<HomeFunctionAdapter.Item> sortedWithPins(java.util.List<HomeFunctionAdapter.Item> src) {
        java.util.Set<String> pinned = getPinnedIds();
        java.util.List<HomeFunctionAdapter.Item> top = new java.util.ArrayList<>();
        java.util.List<HomeFunctionAdapter.Item> rest = new java.util.ArrayList<>();
        for (HomeFunctionAdapter.Item it : src) {
            if (pinned.contains(String.valueOf(it.functionId))) top.add(it); else rest.add(it);
        }
        top.addAll(rest);
        return top;
    }

    private void applyPinsAndSort() {
        if (functionAdapter == null || binding == null) return;
        java.util.Set<String> pinned = getPinnedIds();
        functionAdapter.setPinnedIds(pinned);
        String keyword = binding.homeSearch.getText().toString().trim();
        if (keyword.isEmpty()) functionAdapter.setItems(sortedWithPins(allFunctions));
        else filterFunctions(keyword);
    }

    private void filterFunctions(String keyword) {
        functionAdapter.setPinnedIds(getPinnedIds());
        if (keyword.isEmpty()) {
            functionAdapter.setItems(sortedWithPins(allFunctions));
            return;
        }
        List<HomeFunctionAdapter.Item> out = new ArrayList<>();
        for (HomeFunctionAdapter.Item item : sortedWithPins(allFunctions)) {
            if (item.name.contains(keyword)) out.add(item);
        }
        functionAdapter.setItems(out);
    }

    private String timerStatus() {
        long triggerAt = requireContext().getSharedPreferences("lock_prefs", Context.MODE_PRIVATE)
                .getLong("lock_trigger_at", -1L);
        long remaining = triggerAt - System.currentTimeMillis();
        if (remaining > 0) return "剩余" + formatShort(remaining) + " · 悬浮窗";
        return "倒计时 · 悬浮窗";
    }

    private String todoStatus() {
        try {
            TodoManager m = new TodoManager(requireContext());
            int undone = 0;
            for (TodoItem item : m.getAll()) if (!item.isDone()) undone++;
            if (undone == 0) return "暂无待办";
            return undone + "项未完成";
        } catch (Exception e) {
            return "待办清单";
        }
    }

    private String alarmStatus() {
        try {
            AlarmStore store = new AlarmStore(requireContext());
            long remaining = store.getRemaining();
            if (remaining > 0) return store.getLabel(requireContext()) + " " + formatShort(remaining) + getString(R.string.alarm_home_after);
            return getString(R.string.alarm_widget_rtc_empty);
        } catch (Exception e) {
            return getString(R.string.alarm_page_title);
        }
    }

    private String formatShort(long ms) {
        long totalSeconds = Math.max(0, ms / 1000);
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format(Locale.getDefault(), "%d:%02d", h, m);
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void setupBeta(View root) {
        betaRecyclerView = root.findViewById(R.id.beta_recycler_view);
        betaRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        betaAdapter = new BetaGridAdapter(requireContext(), new ArrayList<>());
        updateBetaSectionVisibility();
    }

    private List<HomeFunctionAdapter.Item> getBetaItems() {
        List<HomeFunctionAdapter.Item> out = new ArrayList<>();
        out.add(new HomeFunctionAdapter.Item(
                R.drawable.sound_recorder, "录音机", "BETA · 录音",
                FunctionConstants.FUNCTION_RECORDER, R.color.app_info_light, R.color.app_info));
        out.add(new HomeFunctionAdapter.Item(
                R.drawable.extract, "转盘抽奖", "BETA · 抽奖",
                FunctionConstants.FUNCTION_LOTTERY, R.color.app_accent_light, R.color.app_accent));
        out.add(new HomeFunctionAdapter.Item(
                R.drawable.sound_recorder, "定时录屏", "BETA · 录屏",
                FunctionConstants.FUNCTION_SCREEN_RECORDER, R.color.app_primary_light, R.color.app_primary));
        return out;
    }

    private void updateAllEntryText() {
        int total = allFunctions.size() + (betaUnlocked ? getBetaItems().size() : 0);
        binding.tvHomeAll.setText(total + "款 · 全部功能");
    }

    private void showAllFunctionsSheet() {
        binding.homeSearch.setText("");
        List<HomeFunctionAdapter.Item> all = new ArrayList<>(allFunctions);
        boolean showBeta = betaUnlocked;
        if (showBeta) {
            all.addAll(getBetaItems());
        }
        View sheet = getLayoutInflater().inflate(R.layout.dialog_all_functions, null);
        androidx.recyclerview.widget.RecyclerView rv = sheet.findViewById(R.id.rv_all_functions);
        android.widget.TextView tvCount = sheet.findViewById(R.id.tv_all_count);
        tvCount.setText("共" + all.size() + "项");
        android.view.View betaHint = sheet.findViewById(R.id.tv_all_beta_hint);
        if (betaHint != null) {
            betaHint.setVisibility(showBeta ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        HomeFunctionAdapter adapter = new HomeFunctionAdapter(requireContext());
        adapter.setPinnedIds(getPinnedIds());
        adapter.setItems(sortedWithPins(all));
        rv.setAdapter(adapter);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(sheet);
        adapter.setOnItemClickListener(item -> {
            dialog.dismiss();
            navigateToFunction(item.functionId);
        });
        adapter.setOnPinClickListener(item -> {
            togglePin(item.functionId);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setupDeveloperEntry() {
        binding.homeDevEntry.setOnClickListener(v -> {
            long now = SystemClock.elapsedRealtime();
            if (now - lastDevTapAt > DEV_TAP_TIMEOUT_MS) {
                devTapCount = 0;
            }
            lastDevTapAt = now;
            devTapCount++;

            if (devTapCount >= DEV_TAP_REQUIRED) {
                betaUnlocked = !betaUnlocked;
                debugPreferences.edit().putBoolean(KEY_BETA_UNLOCKED, betaUnlocked).apply();
                updateBetaSectionVisibility();
                updateAllEntryText();
                Toast.makeText(requireContext(), betaUnlocked ? "测试区已开启" : "测试区已隐藏", Toast.LENGTH_SHORT).show();
                devTapCount = 0;
            }
        });

        updateChangelogBadgeVisibility();
        binding.homeChangelogEntry.setOnClickListener(v -> {
            markChangelogAsSeen();
            navigateToFunction(FunctionConstants.FUNCTION_CHANGELOG);
        });
    }

    private void updateChangelogBadgeVisibility() {
        String lastSeenSignature = debugPreferences.getString(KEY_CHANGELOG_LAST_SEEN_SIGNATURE, "");
        boolean hasUnread = !getCurrentChangelogSignature().equals(lastSeenSignature);
        binding.homeChangelogBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    private void markChangelogAsSeen() {
        debugPreferences.edit().putString(KEY_CHANGELOG_LAST_SEEN_SIGNATURE, getCurrentChangelogSignature()).apply();
        binding.homeChangelogBadge.setVisibility(View.GONE);
    }

    private String getCurrentChangelogSignature() {
        try {
            java.io.InputStream inputStream = requireContext().getResources().openRawResource(R.raw.changelog_entries);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            inputStream.close();
            return String.valueOf(builder.toString().hashCode());
        } catch (Exception e) {
            return "";
        }
    }

    private void updateBetaSectionVisibility() {
        if (binding == null) {
            return;
        }
        int visible = betaUnlocked ? View.VISIBLE : View.GONE;
        binding.betaSectionCard.setVisibility(visible);
        binding.betaSectionTitle.setVisibility(visible);
        binding.betaSectionSubtitle.setVisibility(visible);
        betaRecyclerView.setVisibility(visible);
        if (visible == View.VISIBLE) {
            HomeFunctionAdapter betaListAdapter = new HomeFunctionAdapter(requireContext());
            betaListAdapter.setPinnedIds(new java.util.HashSet<>());
            betaListAdapter.setItems(getBetaItems());
            betaListAdapter.setOnItemClickListener(item -> navigateToFunction(item.functionId));
            betaRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            betaRecyclerView.setAdapter(betaListAdapter);
        }
    }

    private void navigateToFunction(int layoutId) {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                .setEnterAnim(android.R.anim.slide_in_left)
                .setExitAnim(android.R.anim.slide_out_right)
                .build();

        Bundle args = new Bundle();
        args.putInt("function_id", layoutId);

        navController.navigate(R.id.nav_function, args, navOptions);
    }

    public void showFunction(int layoutId) {
        navigateToFunction(layoutId);
    }

    public void hideFunction() {
    }

    public static void showFunctionStatic(int layoutId) {
        if (currentInstance != null) {
            currentInstance.showFunction(layoutId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (currentInstance == this) {
            currentInstance = null;
        }
    }
}

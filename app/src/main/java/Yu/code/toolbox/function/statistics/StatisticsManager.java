package Yu.code.toolbox.function.statistics;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import Yu.code.toolbox.R;
import Yu.code.toolbox.ui.home.HomeFragment;
import Yu.code.toolbox.Utils.DialogUtils;

// 统计组数据类
class StatisticsSet {
    private String name;
    private List<StatisticsItem> items;

    public StatisticsSet(String name) {
        this.name = name;
        this.items = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<StatisticsItem> getItems() { return items; }
    public void setItems(List<StatisticsItem> items) { this.items = items; }
}

public class StatisticsManager {

    private static final String PREFS_NAME = "statistics_prefs";
    private static final String KEY_STATISTICS_SETS = "statistics_sets";
    private static final String KEY_CURRENT_SET = "current_set";
    private static final String KEY_LIST_SORT = "list_sort";
    private static final String KEY_FULLSCREEN_SORT = "fullscreen_sort";
    private static final String DEFAULT_SET_NAME = "默认统计组";
    private static final int REQUEST_CODE_IMPORT_FILE = 6001;

    private Context context;
    private View rootView;
    private StatisticsAdapter adapter;
    private SharedPreferences prefs;
    private Gson gson;

    private EditText etSearch;
    private TextView tvSort;
    private int sortMode;
    private RecyclerView rvStatistics;
    private TextView tvTotalCount;
    private LinearLayout layoutSetPills;
    private boolean speedOpen;

    private List<StatisticsSet> statisticsSets;
    private String currentSetName;

    public StatisticsManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.statisticsSets = new ArrayList<>();

        initViews();
        setupRecyclerView();
        loadStatisticsSets();
        loadCurrentSetData();
    }

    private void initViews() {
        rvStatistics = rootView.findViewById(R.id.rv_statistics);
        tvTotalCount = rootView.findViewById(R.id.tv_total_count);
        etSearch = rootView.findViewById(R.id.et_search);

        // 一键清空
        rootView.findViewById(R.id.btn_clear_all).setOnClickListener(v -> clearAllItems());

        // 搜索过滤(全屏删除后上主页)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshFilteredList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 排序:底部列表单选,文字居中,记忆上次
        sortMode = prefs.getInt(KEY_LIST_SORT, 0);
        if (sortMode < 0 || sortMode > 2) {
            sortMode = 0;
        }
        tvSort = rootView.findViewById(R.id.tv_sort);
        updateSortLabel();
        tvSort.setOnClickListener(v -> {
            java.util.List<String> options = java.util.Arrays.asList("数量高→低", "数量低→高", "名称A→Z");
            DialogUtils.showBottomListDialog(context, "排序方式", "选择物品列表的排序",
                    options, options.get(sortMode), value -> {
                        int pos = options.indexOf(value);
                        if (pos >= 0) {
                            sortMode = pos;
                            prefs.edit().putInt(KEY_LIST_SORT, pos).apply();
                            updateSortLabel();
                            refreshFilteredList();
                        }
                    });
        });

        setupSpeedDial();
    }

    /** 速拨 FAB:平时单＋,点开展开新增/导入/导出;点空白或返回收回 */
    private void setupSpeedDial() {
        View scrim = rootView.findViewById(R.id.view_speed_scrim);
        View dial = rootView.findViewById(R.id.layout_speed_dial);
        View fabMain = rootView.findViewById(R.id.fab_main);
        fabMain.setOnClickListener(v -> setSpeedOpen(!speedOpen));
        scrim.setOnClickListener(v -> setSpeedOpen(false));
        rootView.findViewById(R.id.fab_menu_add).setOnClickListener(v -> {
            setSpeedOpen(false);
            showAddSheet();
        });
        rootView.findViewById(R.id.fab_menu_import).setOnClickListener(v -> {
            setSpeedOpen(false);
            importData();
        });
        rootView.findViewById(R.id.fab_menu_export).setOnClickListener(v -> {
            setSpeedOpen(false);
            exportData();
        });
        setSpeedOpen(false);
    }

    private void setSpeedOpen(boolean open) {
        speedOpen = open;
        View scrim = rootView.findViewById(R.id.view_speed_scrim);
        View dial = rootView.findViewById(R.id.layout_speed_dial);
        scrim.setVisibility(open ? View.VISIBLE : View.GONE);
        dial.setVisibility(open ? View.VISIBLE : View.GONE);
        // 主 FAB:＋旋转 45° 变 ×,好做且天然对齐(不换图不动位)
        View fabMain = rootView.findViewById(R.id.fab_main);
        if (fabMain != null) {
            fabMain.animate().rotation(open ? 45f : 0f).setDuration(180).start();
        }
        if (open) {
            dial.setAlpha(0f);
            dial.setTranslationX(24f);
            dial.animate().alpha(1f).translationX(0f).setDuration(180).start();
        }
        refreshBackCallback();
    }

    private void setupRecyclerView() {
        adapter = new StatisticsAdapter();
        rvStatistics.setLayoutManager(new LinearLayoutManager(context));
        rvStatistics.setAdapter(adapter);
        adapter.setEmptyView(rootView.findViewById(R.id.layout_stats_empty));
        // 全屏入口已删,主页全量展示+页内滚动
        adapter.setMaxVisible(0);

        // 设置点击监听器
        adapter.setOnItemClickListener(new StatisticsAdapter.OnItemClickListener() {
            @Override
            public void onIncreaseClick(int position) {
                StatisticsItem item = adapter.getItems().get(position);
                item.increaseQuantity();
                adapter.updateItem(position, item);
                saveData();
                updateTotalCount(true);
            }

            @Override
            public void onDecreaseClick(int position) {
                StatisticsItem item = adapter.getItems().get(position);
                item.decreaseQuantity();
                adapter.updateItem(position, item);
                saveData();
                updateTotalCount(true);
            }

            @Override
            public void onDeleteClick(int position) {
                deleteItem(position);
            }

            @Override
            public void onQuantityClick(int position) {
                editQuantity(position);
            }

            @Override
            public void onSelectionChanged(int selectedCount, boolean inMultiSelect) {
                updateMultiSelectBar(selectedCount, inMultiSelect);
            }
        });
        setupMultiSelectBack();
    }

    /** 多选底部栏:已选计数 + 取消/删除 */
    private void updateMultiSelectBar(int selectedCount, boolean inMultiSelect) {
        android.view.View bar = rootView.findViewById(R.id.layout_multiselect_bar);
        if (bar == null) return;
        bar.setVisibility(inMultiSelect ? android.view.View.VISIBLE : android.view.View.GONE);
        // 多选态隐藏 FAB,避免遮挡底栏
        android.view.View fabMain = rootView.findViewById(R.id.fab_main);
        if (fabMain != null) {
            fabMain.setVisibility(inMultiSelect ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        if (inMultiSelect) {
            setSpeedOpen(false);
        }
        refreshBackCallback();
        if (!inMultiSelect) return;
        android.widget.TextView tvCount = rootView.findViewById(R.id.tv_multiselect_count);
        android.widget.Button btnDelete = rootView.findViewById(R.id.btn_multiselect_delete);
        if (tvCount != null) tvCount.setText("已选 " + selectedCount + " 项");
        if (btnDelete != null) btnDelete.setText("删除所选 (" + selectedCount + ")");
        android.view.View btnCancel = rootView.findViewById(R.id.btn_multiselect_cancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> adapter.exitMultiSelect());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> deleteSelected());
    }

    /** 系统返回:速拨展开先收,多选态先退,都不在才走默认返回 */
    private androidx.activity.OnBackPressedCallback backCallback;

    private void setupMultiSelectBack() {
        if (!(context instanceof androidx.fragment.app.FragmentActivity)) {
            return;
        }
        androidx.fragment.app.FragmentActivity activity =
                (androidx.fragment.app.FragmentActivity) context;
        backCallback = new androidx.activity.OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (speedOpen) {
                    setSpeedOpen(false);
                    return;
                }
                if (adapter != null && adapter.isMultiSelect()) {
                    adapter.exitMultiSelect();
                    return;
                }
                setEnabled(false);
                activity.getOnBackPressedDispatcher().onBackPressed();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);
        refreshBackCallback();
    }

    /** 有拦截态才接管返回键 */
    private void refreshBackCallback() {
        if (backCallback == null) {
            return;
        }
        backCallback.setEnabled(speedOpen || (adapter != null && adapter.isMultiSelect()));
    }

    private void deleteSelected() {
        java.util.List<Integer> positions = adapter.getSelectedPositions();
        if (positions.isEmpty()) {
            adapter.exitMultiSelect();
            return;
        }
        DialogUtils.createDangerConfirmDialog(context, "删除所选 " + positions.size() + " 个物品？",
                "选中物品将被删除，此操作不可撤销。",
                "确认删除", "再想想",
                (dialog, which) -> {
                    java.util.Collections.sort(positions, java.util.Collections.reverseOrder());
                    // 批量删：一次清数据+一次刷新，不逐个发通知，避免中间态闪烁
                    adapter.removeItemsDescending(positions);
                    adapter.exitMultiSelect();
                    saveData();
                    updateTotalCount();
                    android.widget.Toast.makeText(context, "已删除所选", android.widget.Toast.LENGTH_SHORT).show();
                },
                null);
    }

    /** 新增底弹(FAB→新增):名称+数量,成功后滚到新行 */
    private void showAddSheet() {
        View sheetView = LayoutInflater.from(context).inflate(R.layout.sheet_add_item, null, false);
        EditText etName = sheetView.findViewById(R.id.et_sheet_name);
        EditText etQty = sheetView.findViewById(R.id.et_sheet_quantity);
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        sheet.setContentView(sheetView);
        // 键盘弹起时整个底弹上移(名称+数量+按钮全部可见),而不是只顶当前输入框
        if (sheet.getWindow() != null) {
            sheet.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                    sheet.getBehavior();
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });
        sheetView.findViewById(R.id.btn_sheet_cancel).setOnClickListener(v -> sheet.dismiss());
        sheetView.findViewById(R.id.btn_sheet_add).setOnClickListener(v -> {
            if (addNewItem(etName.getText().toString().trim(), etQty.getText().toString().trim())) {
                sheet.dismiss();
            }
        });
        sheet.show();
    }

    /** 返回 true 表示新增/更新成功 */
    private boolean addNewItem(String itemName, String quantityStr) {

        if (TextUtils.isEmpty(itemName)) {
            Toast.makeText(context, "请输入物品名称", Toast.LENGTH_SHORT).show();
            return false;
        }

        int quantity = 1; // 默认数量为1
        if (!TextUtils.isEmpty(quantityStr)) {
            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    Toast.makeText(context, "数量不能为负数", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的数量", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 检查是否已存在相同名称的物品
        List<StatisticsItem> items = adapter.getItems();
        for (int i = 0; i < items.size(); i++) {
            StatisticsItem item = items.get(i);
            if (itemName.equals(item.getName())) {
                // 如果已存在，增加数量
                item.setQuantity(item.getQuantity() + quantity);
                adapter.updateItem(i, item);
                saveData();
                updateTotalCount();
                refreshFilteredList();
                int dupDisplay = adapter.displayPositionOf(i);
                if (dupDisplay >= 0) {
                    rvStatistics.smoothScrollToPosition(dupDisplay);
                }
                adapter.flashPosition(i);
                Toast.makeText(context, "已更新物品数量", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        // 添加新物品
        StatisticsItem newItem = new StatisticsItem(itemName, quantity);
        adapter.addItem(newItem);
        saveData();
        updateTotalCount();
        refreshFilteredList();
        // 滚到新增行(受搜索/排序影响,用显示下标)
        int displayPos = adapter.displayPositionOf(adapter.getItems().size() - 1);
        if (displayPos >= 0) {
            rvStatistics.smoothScrollToPosition(displayPos);
        }
        adapter.flashPosition(adapter.getItems().size() - 1);
        Toast.makeText(context, "已添加物品", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void deleteItem(int position) {
        deleteItem(position, null);
    }

    /** after 在确认删除成功后执行(全屏框用它刷新列表) */
    private void deleteItem(int position, Runnable after) {
        // 注意:getItemCount 受主页 maxVisible=3 限制,这里用全量 size 做越界检查
        if (position >= 0 && position < adapter.getItems().size()) {
            StatisticsItem item = adapter.getItems().get(position);
            DialogUtils.createDangerConfirmDialog(context, "确认删除",
                    "确定要删除物品 \"" + item.getName() + "\" 吗？",
                    "删除", "取消",
                    (dialog, which) -> {
                        adapter.removeItem(position);
                        saveData();
                        updateTotalCount();
                        Toast.makeText(context, "已删除物品", Toast.LENGTH_SHORT).show();
                        if (after != null) after.run();
                    },
                    null);
        }
    }

    private void clearAllItems() {
        if (adapter.getItemCount() > 0) {
            DialogUtils.createDangerConfirmDialog(context, "确认清空",
                    "确定要清空所有物品吗？此操作不可恢复。",
                    "清空", "取消",
                    (dialog, which) -> {
                        adapter.setItems(new ArrayList<>());
                        saveData();
                        updateTotalCount();
                        Toast.makeText(context, "已清空所有物品", Toast.LENGTH_SHORT).show();
                    },
                    null);
        } else {
            Toast.makeText(context, "没有物品需要清空", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @deprecated 全屏入口已删(主页全量滚动代替),此方法不再被调用,保留仅防回滚,下次清理可删
     */
    @Deprecated
    private void showFullscreenListDialog() {
        List<StatisticsItem> items = adapter.getItems();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_statistics_fullscreen, null);
        TextView tvFullscreenTotal = dialogView.findViewById(R.id.tv_fullscreen_total_count);
        EditText etFullscreenSearch = dialogView.findViewById(R.id.et_fullscreen_search);
        Spinner spinnerFullscreenSort = dialogView.findViewById(R.id.spinner_fullscreen_sort);
        androidx.recyclerview.widget.RecyclerView rvFullscreen =
                dialogView.findViewById(R.id.rv_fullscreen_statistics);

        // 全屏列表复用同一 Adapter:可改数/可删除,与主页同一行布局
        final StatisticsAdapter fsAdapter = new StatisticsAdapter();
        fsAdapter.setMaxVisible(0);
        fsAdapter.setEmptyView(dialogView.findViewById(R.id.layout_fullscreen_empty));
        rvFullscreen.setLayoutManager(new LinearLayoutManager(context));
        rvFullscreen.setAdapter(fsAdapter);
        final List<Integer> fsIndex = new ArrayList<>();

        int total = 0;
        for (StatisticsItem item : items) {
            total += item.getQuantity();
        }
        tvFullscreenTotal.setText("总计: " + total);

        String[] sortOptions = {"数量高->低", "数量低->高", "名称A->Z"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                sortOptions
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFullscreenSort.setAdapter(sortAdapter);
        // 排序记忆:上次选什么,下次还选什么
        int savedSort = prefs.getInt(KEY_FULLSCREEN_SORT, 0);
        if (savedSort >= 0 && savedSort < sortOptions.length) {
            spinnerFullscreenSort.setSelection(savedSort);
        }

        Runnable refreshList = () -> {
            String keyword = etFullscreenSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
            List<Integer> matched = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                StatisticsItem item = items.get(i);
                if (TextUtils.isEmpty(keyword) || item.getName().toLowerCase(Locale.getDefault()).contains(keyword)) {
                    matched.add(i);
                }
            }

            int sortPos = spinnerFullscreenSort.getSelectedItemPosition();
            if (sortPos == 0) {
                Collections.sort(matched, (a, b) -> Integer.compare(items.get(b).getQuantity(), items.get(a).getQuantity()));
            } else if (sortPos == 1) {
                Collections.sort(matched, (a, b) -> Integer.compare(items.get(a).getQuantity(), items.get(b).getQuantity()));
            } else {
                Collections.sort(matched, (a, b) -> items.get(a).getName().compareToIgnoreCase(items.get(b).getName()));
            }

            fsIndex.clear();
            fsIndex.addAll(matched);
            List<StatisticsItem> display = new ArrayList<>();
            for (int index : fsIndex) {
                display.add(items.get(index));
            }
            fsAdapter.setItems(display);
        };

        // 全屏行操作映射回主 adapter 下标,改完刷新全屏列表
        fsAdapter.setOnItemClickListener(new StatisticsAdapter.OnItemClickListener() {
            private int map(int fsPos) {
                return (fsPos >= 0 && fsPos < fsIndex.size()) ? fsIndex.get(fsPos) : -1;
            }

            private void afterChange() {
                List<StatisticsItem> latest = adapter.getItems();
                items.clear();
                items.addAll(latest);
                int t = 0;
                for (StatisticsItem item : latest) {
                    t += item.getQuantity();
                }
                tvFullscreenTotal.setText("总计: " + t);
                refreshList.run();
            }

            @Override
            public void onIncreaseClick(int position) {
                int mainPos = map(position);
                if (mainPos < 0) return;
                StatisticsItem item = adapter.getItems().get(mainPos);
                item.increaseQuantity();
                adapter.updateItem(mainPos, item);
                saveData();
                updateTotalCount();
                afterChange();
            }

            @Override
            public void onDecreaseClick(int position) {
                int mainPos = map(position);
                if (mainPos < 0) return;
                StatisticsItem item = adapter.getItems().get(mainPos);
                item.decreaseQuantity();
                adapter.updateItem(mainPos, item);
                saveData();
                updateTotalCount();
                afterChange();
            }

            @Override
            public void onDeleteClick(int position) {
                int mainPos = map(position);
                if (mainPos < 0) return;
                deleteItem(mainPos, this::afterChange);
            }

            @Override
            public void onQuantityClick(int position) {
                int mainPos = map(position);
                if (mainPos < 0) return;
                editQuantity(mainPos, this::afterChange);
            }

            @Override
            public void onSelectionChanged(int selectedCount, boolean inMultiSelect) {
            }
        });

        etFullscreenSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFullscreenSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_FULLSCREEN_SORT, position).apply();
                refreshList.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        refreshList.run();

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            // 底部系统导航条跟 dialog 底同色,避免黑条割裂
            android.view.Window window = dialog.getWindow();
            window.setNavigationBarColor(context.getResources().getColor(R.color.app_surface));
            boolean isNight = (context.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                int flags = window.getDecorView().getSystemUiVisibility();
                if (isNight) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
        }
    }

    private void editQuantity(int position) {
        editQuantity(position, null);
    }

    /** after 在数量成功更新后执行(全屏框用它刷新列表) */
    private void editQuantity(int position, Runnable after) {
        // 注意:getItemCount 受主页 maxVisible=3 限制,这里用全量 size 做越界检查
        if (position < 0 || position >= adapter.getItems().size()) {
            return;
        }
        StatisticsItem item = adapter.getItems().get(position);

        DialogUtils.showNumberInputDialog(context, "编辑数量", "请输入数量",
                String.valueOf(item.getQuantity()), input -> {
                    if (!TextUtils.isEmpty(input)) {
                        try {
                            int newQuantity = Integer.parseInt(input);
                            if (newQuantity >= 0) {
                                item.setQuantity(newQuantity);
                                adapter.updateItem(position, item);
                                saveData();
                                updateTotalCount();
                                Toast.makeText(context, "数量已更新", Toast.LENGTH_SHORT).show();
                                if (after != null) after.run();
                            } else {
                                Toast.makeText(context, "数量不能为负数", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "请输入数量", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateTotalCount() {
        updateTotalCount(false);
    }

    /** @param keepOrder true=加减数量:保序不重排,只单行刷新,不闪 */
    private void updateTotalCount(boolean keepOrder) {
        int total = 0;
        for (StatisticsItem item : adapter.getItems()) {
            total += item.getQuantity();
        }
        tvTotalCount.setText("总计: " + total + " · " + adapter.getItems().size() + " 件");
        // 空态 FAB 脉冲一次,提示点＋新增
        if (adapter.getItems().isEmpty()) {
            View fabMain = rootView.findViewById(R.id.fab_main);
            if (fabMain != null) {
                fabMain.animate().scaleX(1.15f).scaleY(1.15f).setDuration(160)
                        .withEndAction(() -> fabMain.animate().scaleX(1f).scaleY(1f)
                                .setDuration(160).start()).start();
            }
        }
        refreshFilteredList(keepOrder);
    }

    /** 排序按钮文案:数量↓ / 数量↑ / 名称 */
    private void updateSortLabel() {
        if (tvSort == null) {
            return;
        }
        String[] labels = {"数量 ↓", "数量 ↑", "名称"};
        tvSort.setText(labels[Math.max(0, Math.min(2, sortMode))]);
    }

    /** 搜索+排序过滤:显示下标映射,数据本身不动 */
    private void refreshFilteredList() {
        refreshFilteredList(false);
    }

    /**
     * @param keepOrder true=保序(加减数量时调):不过滤重排,只剔除已删、下标顺延,
     *                  新行缀尾,当前屏一行不动,自然不闪
     */
    private void refreshFilteredList(boolean keepOrder) {
        if (adapter == null || etSearch == null) {
            return;
        }
        // 搜索/排序变化时先退多选,避免选中下标与显示错位
        if (adapter.isMultiSelect()) {
            adapter.exitMultiSelect();
        }
        if (keepOrder) {
            // 加减数量:下标体系不变,原映射照用(无过滤时更是什么都不做),
            // 不重排不重绑,只前面 updateItem 的单行刷新生效
            if (adapter.hasFilter()) {
                adapter.setVisibleIndex(adapter.getVisibleIndex());
            }
            return;
        }
        String keyword = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<StatisticsItem> all = adapter.getItems();
        List<Integer> matched = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (TextUtils.isEmpty(keyword)
                    || all.get(i).getName().toLowerCase(Locale.getDefault()).contains(keyword)) {
                matched.add(i);
            }
        }
        int sortPos = sortMode;
        if (sortPos == 1) {
            Collections.sort(matched, (a, b) -> Integer.compare(all.get(a).getQuantity(), all.get(b).getQuantity()));
        } else if (sortPos == 2) {
            Collections.sort(matched, (a, b) -> all.get(a).getName().compareToIgnoreCase(all.get(b).getName()));
        } else {
            Collections.sort(matched, (a, b) -> Integer.compare(all.get(b).getQuantity(), all.get(a).getQuantity()));
        }
        adapter.setVisibleIndex(matched);
        // 空态文案：全空→新增引导，有数据搜不到→换关键词
        adapter.updateEmptyView(all.isEmpty(),
                "这个组还没有物品", "点右下 ＋ 新增第一件",
                "没有匹配的物品", "换个关键词试试");
    }

    /**
     * 唯一数据写入路径：adapter → 当前组 → 落盘
     */
    private void saveData() {
        StatisticsSet currentSet = getCurrentSet();
        if (currentSet != null) {
            currentSet.setItems(new ArrayList<>(adapter.getItems()));
        }
        persist();
    }

    /** 组列表 + 当前组名一次提交 */
    private void persist() {
        prefs.edit()
                .putString(KEY_STATISTICS_SETS, gson.toJson(statisticsSets))
                .putString(KEY_CURRENT_SET, currentSetName)
                .apply();
    }

    public void onDestroy() {
        // 保存数据
        saveData();
    }

    /**
     * 导出数据
     */
    private void exportData() {
        List<StatisticsItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(context, "没有数据可以导出", Toast.LENGTH_SHORT).show();
            return;
        }

        // 导出方式:分享已归并到"导出成功"框里的按钮,不再单列
        String[] options = {"导出为 CSV 文件", "复制为文本"};
        new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("选择导出方式")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportToExcel();
                    } else {
                        exportToText();
                    }
                })
                .show();
    }

    /**
     * CSV 正文唯一构造器(BOM+标题+数据+总计),导出文件与分享共用
     */
    private String buildCsvBody(List<StatisticsItem> items) {
        StringBuilder body = new StringBuilder();
        body.append('\ufeff');
        body.append("物品名称,数量\n");
        int totalQuantity = 0;
        for (StatisticsItem item : items) {
            String name = item.getName();
            if (name.contains(",") || name.contains("\"") || name.contains("\n")) {
                name = "\"" + name.replace("\"", "\"\"") + "\"";
            }
            body.append(name).append(",").append(item.getQuantity()).append("\n");
            totalQuantity += item.getQuantity();
        }
        body.append("总计,").append(totalQuantity).append("\n");
        return body.toString();
    }

    private void writeTextFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        writer.write(content);
        writer.flush();
        writer.close();
        fos.close();
    }

    /**
     * 导出为 CSV 文件（使用 CSV 格式，Excel 可以直接打开）
     */
    private void exportToExcel() {
        List<StatisticsItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(context, "没有数据可以导出", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "统计数据_" + currentSetName + "_" + timestamp + ".csv";

            // 保存文件
            File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Statistics");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File csvFile = new File(exportDir, fileName);

            // 写入 CSV 内容（带 UTF-8 BOM，Excel 才能正确识别中文）
            writeTextFile(csvFile, buildCsvBody(items));

            // 显示成功提示并询问是否分享
            showExportSuccessDialog(csvFile);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 显示导出成功对话框
     */
    private void showExportSuccessDialog(File excelFile) {
        new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("导出成功")
                .setMessage("文件已保存到:\n" + excelFile.getAbsolutePath() + "\n\n是否立即分享？")
                .setPositiveButton("分享", (dialogInterface, which) -> shareFile(excelFile))
                .setNegativeButton("完成", null)
                .show();
    }

    /**
     * 导出为文本（复制到剪贴板）
     */
    private void exportToText() {
        List<StatisticsItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(context, "没有数据可以导出", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder exportText = new StringBuilder();
        exportText.append("统计数据 - ").append(currentSetName).append("\n");
        exportText.append("导出时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
        exportText.append("物品名称\t数量\n");
        exportText.append("------------------------\n");
        
        int total = 0;
        for (StatisticsItem item : items) {
            exportText.append(item.getName()).append("\t").append(item.getQuantity()).append("\n");
            total += item.getQuantity();
        }
        
        exportText.append("------------------------\n");
        exportText.append("总计\t").append(total);

        // 复制到剪贴板
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("统计数据", exportText.toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "数据已复制到剪贴板", Toast.LENGTH_LONG).show();
    }

    /**
     * 分享文件
     */
    private void shareFile(File file) {
        try {
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            // CSV 文件可以用多种 MIME 类型，使用通用类型确保兼容性
            shareIntent.setType("text/comma-separated-values");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "统计数据 - " + currentSetName);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "分享统计数据（CSV 格式，可用 Excel 打开）");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "分享到"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "分享失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 导入数据
     */
    private void importData() {
        // 显示导入选项对话框
        String[] options = {"从 Excel/CSV 文件导入", "手动输入数据"};
        new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("选择导入方式")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            importFromFile();
                            break;
                        case 1:
                            importFromText();
                            break;
                    }
                })
                .show();
    }

    /**
     * 从文件导入
     */
    private void importFromFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // 支持的文件类型
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/vnd.ms-excel", 
                              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            ((FragmentActivity) context).startActivityForResult(
                Intent.createChooser(intent, "选择 Excel/CSV 文件"), 
                REQUEST_CODE_IMPORT_FILE
            );
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 手动输入数据导入
     */
    private void importFromText() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_import_data, null);
        EditText etImportData = dialogView.findViewById(R.id.et_import_data);

        DialogUtils.createCustomViewDialog(context, "导入数据", dialogView,
                "导入", "取消",
                (dialog, which) -> {
                    String importText = etImportData.getText().toString().trim();
                    if (TextUtils.isEmpty(importText)) {
                        Toast.makeText(context, "请输入要导入的数据", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ImportResult result = parseImportData(importText);

                    if (result.getValidItems().isEmpty()) {
                        if (result.hasErrors()) {
                            showImportErrors(result.getErrorDetails());
                        } else {
                            Toast.makeText(context, "没有有效的数据可以导入", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    // 确认导入：坏行跳过（先报数），好行覆盖当前数据
                    String warn = result.hasErrors()
                            ? "其中 " + result.getErrorDetails().size() + " 行格式错误已跳过，"
                            : "";
                    DialogUtils.createConfirmDialog(context, "确认导入",
                            "将导入 " + result.getValidItems().size() + " 个物品，" + warn + "这将覆盖当前数据。确定继续吗？",
                            "确定", "取消",
                            (confirmDialog, confirmWhich) -> {
                                adapter.setItems(result.getValidItems());
                                saveData();
                                updateTotalCount();
                                Toast.makeText(context, "成功导入 " + result.getValidItems().size() + " 个物品", Toast.LENGTH_SHORT).show();
                            },
                            null);
                },
                null);
    }

    /**
     * 处理文件选择结果
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMPORT_FILE && resultCode == android.app.Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                importFromFileUri(fileUri);
            }
        }
    }

    /**
     * 从文件 URI 导入数据（公开方法，供外部调用）
     */
    public void importFromFileUri(Uri fileUri) {
        try {
            java.io.InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            // 读取文件内容
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );
            
            StringBuilder content = new StringBuilder();
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // 跳过 UTF-8 BOM
                if (isFirstLine && line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }
                isFirstLine = false;
                
                // 跳过标题行（如果包含"物品名称"或"名称"）
                if (line.contains("物品名称") || line.contains("名称") || 
                    line.toLowerCase().contains("name") || line.toLowerCase().contains("item")) {
                    continue;
                }
                
                // 跳过总计行
                if (line.startsWith("总计") || line.toLowerCase().startsWith("total")) {
                    continue;
                }
                
                content.append(line).append("\n");
            }
            
            reader.close();
            inputStream.close();

            // 解析导入的数据
            String importText = content.toString().trim();
            if (TextUtils.isEmpty(importText)) {
                Toast.makeText(context, "文件内容为空", Toast.LENGTH_SHORT).show();
                return;
            }

            ImportResult result = parseImportData(importText);

            if (result.getValidItems().isEmpty()) {
                if (result.hasErrors()) {
                    showImportErrors(result.getErrorDetails());
                } else {
                    Toast.makeText(context, "没有有效的数据可以导入", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // 确认导入：坏行跳过（先报数），好行覆盖当前数据
            String warnFile = result.hasErrors()
                    ? "其中 " + result.getErrorDetails().size() + " 行格式错误已跳过，"
                    : "";
            DialogUtils.createConfirmDialog(context, "确认导入",
                    "将导入 " + result.getValidItems().size() + " 个物品，" + warnFile + "这将覆盖当前数据。确定继续吗？",
                    "确定", "取消",
                    (confirmDialog, confirmWhich) -> {
                        adapter.setItems(result.getValidItems());
                        saveData();
                        updateTotalCount();
                        Toast.makeText(context, "成功导入 " + result.getValidItems().size() + " 个物品", Toast.LENGTH_SHORT).show();
                    },
                    null);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 解析导入数据
     */
    private ImportResult parseImportData(String importText) {
        List<StatisticsItem> validItems = new ArrayList<>();
        List<String> errorDetails = new ArrayList<>();

        String[] lines = importText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (TextUtils.isEmpty(line)) {
                continue; // 跳过空行
            }

            int lineNumber = i + 1;
            String[] parts = parseLine(line);

            if (parts == null) {
                errorDetails.add("第" + lineNumber + "行：格式错误 - \"" + line + "\"");
                continue;
            }

            String name = parts[0].trim();
            String quantityStr = parts[1].trim();

            if (TextUtils.isEmpty(name)) {
                errorDetails.add("第" + lineNumber + "行：物品名称不能为空 - \"" + line + "\"");
                continue;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    errorDetails.add("第" + lineNumber + "行：数量不能为负数 - \"" + line + "\"");
                    continue;
                }
                validItems.add(new StatisticsItem(name, quantity));
            } catch (NumberFormatException e) {
                errorDetails.add("第" + lineNumber + "行：数量格式错误 - \"" + line + "\"");
            }
        }

        return new ImportResult(validItems, errorDetails);
    }

    /**
     * 解析单行数据，支持多种分隔符和无分隔符格式
     * 从右到左查找分隔符，避免物品名称中的分隔符干扰
     */
    private String[] parseLine(String line) {
        // 首先尝试解析 CSV 格式（处理引号包裹的字段）
        String[] csvParts = parseCsvLine(line);
        if (csvParts != null) {
            return csvParts;
        }
        
        // 支持的分隔符：逗号、空格、横杠、句号、制表符
        String[] separators = {",", "\t", " ", "-", "."};

        // 尝试使用分隔符解析
        for (String separator : separators) {
            int lastIndex = line.lastIndexOf(separator);
            if (lastIndex > 0 && lastIndex < line.length() - 1) {
                String name = line.substring(0, lastIndex).trim();
                String quantity = line.substring(lastIndex + separator.length()).trim();

                // 验证数量部分是否为数字
                if (isValidQuantity(quantity)) {
                    return new String[]{name, quantity};
                }
            }
        }

        // 如果没有找到分隔符，尝试从右到左查找数字
        return parseWithoutSeparator(line);
    }

    /**
     * 解析标准 CSV 格式的行（处理引号包裹的字段）
     */
    private String[] parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // 检查是否是转义的引号
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // 跳过下一个引号
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 字段分隔符
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // 添加最后一个字段
        fields.add(currentField.toString().trim());
        
        // CSV 格式应该有至少 2 个字段
        if (fields.size() >= 2) {
            String name = fields.get(0);
            String quantity = fields.get(1);
            
            if (!TextUtils.isEmpty(name) && isValidQuantity(quantity)) {
                return new String[]{name, quantity};
            }
        }
        
        return null;
    }

    /**
     * 解析无分隔符的格式，如"可乐15"
     */
    private String[] parseWithoutSeparator(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // 从右到左查找第一个数字的位置
        int numberStartIndex = -1;
        for (int i = line.length() - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (Character.isDigit(c)) {
                numberStartIndex = i;
            } else if (numberStartIndex != -1) {
                // 找到数字后，遇到非数字字符就停止
                break;
            }
        }

        if (numberStartIndex == -1 || numberStartIndex == 0) {
            return null; // 没有找到数字或数字在开头
        }

        String name = line.substring(0, numberStartIndex).trim();
        String quantity = line.substring(numberStartIndex).trim();

        // 验证数量部分是否为数字
        if (!TextUtils.isEmpty(name) && isValidQuantity(quantity)) {
            return new String[]{name, quantity};
        }

        return null;
    }

    /**
     * 验证数量是否为有效数字
     */
    private boolean isValidQuantity(String quantity) {
        if (TextUtils.isEmpty(quantity)) {
            return false;
        }

        try {
            int num = Integer.parseInt(quantity);
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 显示导入错误详情
     */
    private void showImportErrors(List<String> errorDetails) {
        View errorView = LayoutInflater.from(context).inflate(R.layout.dialog_import_errors, null);
        TextView tvErrorDetails = errorView.findViewById(R.id.tv_error_details);

        StringBuilder errorText = new StringBuilder();
        for (String error : errorDetails) {
            errorText.append("• ").append(error).append("\n");
        }
        tvErrorDetails.setText(errorText.toString());

        DialogUtils.createCustomViewDialog(context, "导入错误", errorView,
                "确定", null,
                null,
                null);
    }

    /**
     * 导入结果类
     */
    private static class ImportResult {
        private final List<StatisticsItem> validItems;
        private final List<String> errorDetails;

        public ImportResult(List<StatisticsItem> validItems, List<String> errorDetails) {
            this.validItems = validItems;
            this.errorDetails = errorDetails;
        }

        public List<StatisticsItem> getValidItems() {
            return validItems;
        }

        public List<String> getErrorDetails() {
            return errorDetails;
        }

        public boolean hasErrors() {
            return !errorDetails.isEmpty();
        }
    }

    // ==================== 多组统计功能 ====================

    /**
     * 加载统计组列表
     */
    private void loadStatisticsSets() {
        String setsJson = prefs.getString(KEY_STATISTICS_SETS, "");
        if (TextUtils.isEmpty(setsJson)) {
            // 如果没有数据，创建默认统计组
            statisticsSets.clear();
            statisticsSets.add(new StatisticsSet(DEFAULT_SET_NAME));
            persist();
        } else {
            Type type = new TypeToken<List<StatisticsSet>>(){}.getType();
            statisticsSets = gson.fromJson(setsJson, type);
            if (statisticsSets == null || statisticsSets.isEmpty()) {
                statisticsSets = new ArrayList<>();
                statisticsSets.add(new StatisticsSet(DEFAULT_SET_NAME));
                persist();
            }
        }

        // 加载当前选中的统计组
        currentSetName = prefs.getString(KEY_CURRENT_SET, DEFAULT_SET_NAME);
        if (!isSetExists(currentSetName)) {
            currentSetName = statisticsSets.get(0).getName();
            persist();
        }

        setupSpinner();
    }

    /**
     * 检查统计组是否存在
     */
    private boolean isSetExists(String setName) {
        for (StatisticsSet set : statisticsSets) {
            if (set.getName().equals(setName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 组 pills:横滑切换,末尾＋进管理
     */
    private void setupSpinner() {
        if (layoutSetPills == null) {
            layoutSetPills = rootView.findViewById(R.id.layout_set_pills);
        }
        layoutSetPills.removeAllViews();
        for (StatisticsSet set : statisticsSets) {
            boolean current = set.getName().equals(currentSetName);
            TextView pill = new TextView(context);
            pill.setText(set.getName());
            pill.setTextSize(12);
            pill.setTypeface(null, android.graphics.Typeface.BOLD);
            int padH = (int) (18 * context.getResources().getDisplayMetrics().density + 0.5f);
            int padV = (int) (10 * context.getResources().getDisplayMetrics().density + 0.5f);
            pill.setPadding(padH, padV, padH, padV);
            pill.setBackgroundResource(current ? R.drawable.pill_on_bg : R.drawable.pill_off_bg);
            pill.setTextColor(context.getResources().getColor(
                    current ? android.R.color.white : R.color.app_text_primary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int gap = (int) (8 * context.getResources().getDisplayMetrics().density + 0.5f);
            lp.setMarginEnd(gap);
            pill.setLayoutParams(lp);
            pill.setMinHeight((int) (44 * context.getResources().getDisplayMetrics().density + 0.5f));
            pill.setGravity(android.view.Gravity.CENTER);
            String name = set.getName();
            pill.setOnClickListener(v -> {
                if (!name.equals(currentSetName)) {
                    switchToSet(name);
                }
            });
            layoutSetPills.addView(pill);
        }
        // 末尾＋进组管理
        TextView more = new TextView(context);
        more.setText("＋");
        more.setTextSize(14);
        more.setTypeface(null, android.graphics.Typeface.BOLD);
        int padH = (int) (16 * context.getResources().getDisplayMetrics().density + 0.5f);
        int padV = (int) (10 * context.getResources().getDisplayMetrics().density + 0.5f);
        more.setPadding(padH, padV, padH, padV);
        more.setBackgroundResource(R.drawable.pill_off_bg);
        more.setTextColor(context.getResources().getColor(R.color.app_text_primary));
        more.setMinHeight((int) (44 * context.getResources().getDisplayMetrics().density + 0.5f));
        more.setGravity(android.view.Gravity.CENTER);
        more.setOnClickListener(v -> showManageSetsDialog());
        layoutSetPills.addView(more);
    }

    /**
     * 切换到指定的统计组
     */
    private void switchToSet(String setName) {
        // 落盘当前统计组数据,再切名并加载新组
        saveData();
        currentSetName = setName;
        persist();
        loadCurrentSetData();
    }

    /**
     * 加载当前统计组的数据
     */
    private void loadCurrentSetData() {
        StatisticsSet currentSet = getCurrentSet();
        if (currentSet != null) {
            adapter.setItems(new ArrayList<>(currentSet.getItems()));
            updateTotalCount();
        }
    }

    /**
     * 获取当前统计组
     */
    private StatisticsSet getCurrentSet() {
        for (StatisticsSet set : statisticsSets) {
            if (set.getName().equals(currentSetName)) {
                return set;
            }
        }
        return null;
    }

    /**
     * 显示统计组管理对话框
     */
    private void showManageSetsDialog() {
        String[] options = {"创建新统计组", "重命名当前统计组", "删除当前统计组"};

        new AlertDialog.Builder(context, R.style.AppDialogStyle)
                .setTitle("管理统计组")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCreateSetDialog();
                            break;
                        case 1:
                            showRenameSetDialog();
                            break;
                        case 2:
                            showDeleteSetDialog();
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示创建统计组对话框
     */
    private void showCreateSetDialog() {
        DialogUtils.showInputDialog(context, "创建新统计组", "请输入统计组名称", null,
                input -> {
                    if (!TextUtils.isEmpty(input)) {
                        createNewSet(input);
                    } else {
                        Toast.makeText(context, "请输入统计组名称", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 显示重命名统计组对话框
     */
    private void showRenameSetDialog() {
        DialogUtils.showInputDialog(context, "重命名统计组", "请输入新的统计组名称", currentSetName,
                input -> {
                    if (!TextUtils.isEmpty(input) && !input.equals(currentSetName)) {
                        renameCurrentSet(input);
                    } else if (input.equals(currentSetName)) {
                        Toast.makeText(context, "名称没有变化", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "请输入有效的统计组名称", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 显示删除统计组对话框
     */
    private void showDeleteSetDialog() {
        if (statisticsSets.size() <= 1) {
            Toast.makeText(context, "至少需要保留一个统计组", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogUtils.createDangerConfirmDialog(context, "删除统计组",
                "确定要删除统计组 \"" + currentSetName + "\" 吗？\n此操作不可撤销！",
                "删除", "取消",
                (dialog, which) -> deleteCurrentSet(),
                null);
    }

    /**
     * 创建新的统计组
     */
    private void createNewSet(String setName) {
        if (isSetExists(setName)) {
            Toast.makeText(context, "统计组名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        StatisticsSet newSet = new StatisticsSet(setName);
        statisticsSets.add(newSet);
        persist();

        // 更新Spinner
        setupSpinner();

        // 切换到新创建的统计组
        switchToSet(setName);

        Toast.makeText(context, "统计组 \"" + setName + "\" 创建成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 重命名当前统计组
     */
    private void renameCurrentSet(String newName) {
        if (isSetExists(newName)) {
            Toast.makeText(context, "统计组名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        StatisticsSet currentSet = getCurrentSet();
        if (currentSet != null) {
            currentSet.setName(newName);
            currentSetName = newName;
            persist();

            // 更新Spinner
            setupSpinner();

            Toast.makeText(context, "统计组重命名成功", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除当前统计组
     */
    private void deleteCurrentSet() {
        if (statisticsSets.size() <= 1) {
            Toast.makeText(context, "至少需要保留一个统计组", Toast.LENGTH_SHORT).show();
            return;
        }

        statisticsSets.removeIf(set -> set.getName().equals(currentSetName));
        currentSetName = statisticsSets.get(0).getName();
        persist();

        // 更新Spinner和加载数据
        setupSpinner();
        loadCurrentSetData();

        Toast.makeText(context, "统计组已删除", Toast.LENGTH_SHORT).show();
    }
}

package Yu.code.toolbox.function.statistics;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
    private static final String KEY_STATISTICS_DATA = "statistics_data";
    private static final String KEY_STATISTICS_SETS = "statistics_sets";
    private static final String KEY_CURRENT_SET = "current_set";
    private static final String DEFAULT_SET_NAME = "默认统计组";

    private Context context;
    private View rootView;
    private StatisticsAdapter adapter;
    private SharedPreferences prefs;
    private Gson gson;

    private EditText etItemName;
    private EditText etItemQuantity;
    private RecyclerView rvStatistics;
    private TextView tvTotalCount;
    private Spinner spinnerStatisticsSet;
    private ArrayAdapter<String> setAdapter;

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
        etItemName = rootView.findViewById(R.id.et_item_name);
        etItemQuantity = rootView.findViewById(R.id.et_item_quantity);
        rvStatistics = rootView.findViewById(R.id.rv_statistics);
        tvTotalCount = rootView.findViewById(R.id.tv_total_count);
        spinnerStatisticsSet = rootView.findViewById(R.id.spinner_statistics_set);

        // 设置添加按钮点击事件
        rootView.findViewById(R.id.btn_add_item).setOnClickListener(v -> addNewItem());

        // 设置一键清空按钮点击事件
        rootView.findViewById(R.id.btn_clear_all).setOnClickListener(v -> clearAllItems());

        // 设置导出按钮点击事件
        rootView.findViewById(R.id.btn_export).setOnClickListener(v -> exportData());

        // 设置导入按钮点击事件
        rootView.findViewById(R.id.btn_import).setOnClickListener(v -> importData());

        // 设置统计套管理按钮点击事件
        rootView.findViewById(R.id.btn_manage_sets).setOnClickListener(v -> showManageSetsDialog());

    }

    private void setupRecyclerView() {
        adapter = new StatisticsAdapter();
        rvStatistics.setLayoutManager(new LinearLayoutManager(context));
        rvStatistics.setAdapter(adapter);

        // 设置点击监听器
        adapter.setOnItemClickListener(new StatisticsAdapter.OnItemClickListener() {
            @Override
            public void onIncreaseClick(int position) {
                StatisticsItem item = adapter.getItems().get(position);
                item.increaseQuantity();
                adapter.updateItem(position, item);
                saveData();
                updateTotalCount();
            }

            @Override
            public void onDecreaseClick(int position) {
                StatisticsItem item = adapter.getItems().get(position);
                item.decreaseQuantity();
                adapter.updateItem(position, item);
                saveData();
                updateTotalCount();
            }

            @Override
            public void onDeleteClick(int position) {
                deleteItem(position);
            }

            @Override
            public void onQuantityClick(int position) {
                editQuantity(position);
            }
        });
    }

    private void addNewItem() {
        String itemName = etItemName.getText().toString().trim();
        String quantityStr = etItemQuantity.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            Toast.makeText(context, "请输入物品名称", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = 1; // 默认数量为1
        if (!TextUtils.isEmpty(quantityStr)) {
            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    Toast.makeText(context, "数量不能为负数", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "请输入有效的数量", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 检查是否已存在相同名称的物品
        List<StatisticsItem> items = adapter.getItems();
        for (StatisticsItem item : items) {
            if (itemName.equals(item.getName())) {
                // 如果已存在，增加数量
                item.setQuantity(item.getQuantity() + quantity);
                adapter.notifyDataSetChanged();
                saveData();
                updateTotalCount();
                clearInputs();
                Toast.makeText(context, "已更新物品数量", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 添加新物品
        StatisticsItem newItem = new StatisticsItem(itemName, quantity);
        adapter.addItem(newItem);
        saveData();
        updateTotalCount();
        clearInputs();
        Toast.makeText(context, "已添加物品", Toast.LENGTH_SHORT).show();
    }

    private void clearInputs() {
        etItemName.setText("");
        etItemQuantity.setText("");
        etItemName.requestFocus();
    }

    private void deleteItem(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
            StatisticsItem item = adapter.getItems().get(position);
            DialogUtils.createDangerConfirmDialog(context, "确认删除",
                    "确定要删除物品 \"" + item.getName() + "\" 吗？",
                    "删除", "取消",
                    (dialog, which) -> {
                        adapter.removeItem(position);
                        saveData();
                        updateTotalCount();
                        Toast.makeText(context, "已删除物品", Toast.LENGTH_SHORT).show();
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

    private void editQuantity(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
            StatisticsItem item = adapter.getItems().get(position);

            // 创建输入对话框
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_quantity, null);
            EditText etQuantity = dialogView.findViewById(R.id.et_quantity);
            etQuantity.setText(String.valueOf(item.getQuantity()));
            etQuantity.setSelection(etQuantity.getText().length()); // 光标移到末尾
            etQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

            DialogUtils.createCustomViewDialog(context, "编辑数量", dialogView,
                    "确定", "取消",
                    (dialog, which) -> {
                        String quantityStr = etQuantity.getText().toString().trim();
                        if (!TextUtils.isEmpty(quantityStr)) {
                            try {
                                int newQuantity = Integer.parseInt(quantityStr);
                                if (newQuantity >= 0) {
                                    item.setQuantity(newQuantity);
                                    adapter.updateItem(position, item);
                                    saveData();
                                    updateTotalCount();
                                    Toast.makeText(context, "数量已更新", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "数量不能为负数", Toast.LENGTH_SHORT).show();
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(context, "请输入有效的数字", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "请输入数量", Toast.LENGTH_SHORT).show();
                        }
                    },
                    null);
        }
    }

    private void updateTotalCount() {
        int total = 0;
        for (StatisticsItem item : adapter.getItems()) {
            total += item.getQuantity();
        }
        tvTotalCount.setText("总计: " + total);
    }

    private void loadData() {
        // 这个方法现在由loadCurrentSetData()替代
        // 保留此方法是为了向后兼容，但实际加载逻辑在loadCurrentSetData()中
        loadCurrentSetData();
    }

    private void saveData() {
        try {
            // 保存到当前选中的统计套
            saveCurrentSetData();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        StringBuilder exportText = new StringBuilder();
        for (StatisticsItem item : items) {
            exportText.append(item.getName()).append(",").append(item.getQuantity()).append("\n");
        }

        // 复制到剪贴板
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("统计数据", exportText.toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "数据已复制到剪贴板", Toast.LENGTH_LONG).show();
    }

    /**
     * 导入数据
     */
    private void importData() {
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

                    if (result.hasErrors()) {
                        showImportErrors(result.getErrorDetails());
                        return;
                    }

                    if (result.getValidItems().isEmpty()) {
                        Toast.makeText(context, "没有有效的数据可以导入", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 确认导入
                    DialogUtils.createConfirmDialog(context, "确认导入",
                            "将导入 " + result.getValidItems().size() + " 个物品，这将覆盖当前数据。确定继续吗？",
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
        // 支持的分隔符：逗号、空格、横杠、句号
        String[] separators = {",", " ", "-", "."};

        // 首先尝试使用分隔符解析
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
            saveStatisticsSets();
        } else {
            Type type = new TypeToken<List<StatisticsSet>>(){}.getType();
            statisticsSets = gson.fromJson(setsJson, type);
            if (statisticsSets == null || statisticsSets.isEmpty()) {
                statisticsSets = new ArrayList<>();
                statisticsSets.add(new StatisticsSet(DEFAULT_SET_NAME));
                saveStatisticsSets();
            }
        }

        // 加载当前选中的统计组
        currentSetName = prefs.getString(KEY_CURRENT_SET, DEFAULT_SET_NAME);
        if (!isSetExists(currentSetName)) {
            currentSetName = statisticsSets.get(0).getName();
            saveCurrentSet();
        }

        setupSpinner();
    }

    /**
     * 保存统计组列表
     */
    private void saveStatisticsSets() {
        String setsJson = gson.toJson(statisticsSets);
        prefs.edit().putString(KEY_STATISTICS_SETS, setsJson).apply();
    }

    /**
     * 保存当前选中的统计组
     */
    private void saveCurrentSet() {
        prefs.edit().putString(KEY_CURRENT_SET, currentSetName).apply();
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
     * 设置Spinner
     */
    private void setupSpinner() {
        List<String> setNames = new ArrayList<>();
        for (StatisticsSet set : statisticsSets) {
            setNames.add(set.getName());
        }

        setAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, setNames);
        setAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatisticsSet.setAdapter(setAdapter);

        // 设置当前选中的统计组
        int position = setNames.indexOf(currentSetName);
        if (position >= 0) {
            spinnerStatisticsSet.setSelection(position);
        }

        // 设置选择监听器
        spinnerStatisticsSet.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedSetName = setNames.get(position);
                if (!selectedSetName.equals(currentSetName)) {
                    switchToSet(selectedSetName);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不做处理
            }
        });
    }

    /**
     * 切换到指定的统计组
     */
    private void switchToSet(String setName) {
        // 保存当前统计组的数据
        saveCurrentSetData();

        // 切换到新的统计组
        currentSetName = setName;
        saveCurrentSet();

        // 加载新统计组的数据
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
     * 保存当前统计组的数据
     */
    private void saveCurrentSetData() {
        StatisticsSet currentSet = getCurrentSet();
        if (currentSet != null) {
            currentSet.setItems(new ArrayList<>(adapter.getItems()));
            saveStatisticsSets();
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

        new AlertDialog.Builder(context)
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
        EditText etSetName = new EditText(context);
        etSetName.setHint("请输入统计组名称");

        new AlertDialog.Builder(context)
                .setTitle("创建新统计组")
                .setView(etSetName)
                .setPositiveButton("创建", (dialog, which) -> {
                    String setName = etSetName.getText().toString().trim();
                    if (!TextUtils.isEmpty(setName)) {
                        createNewSet(setName);
                    } else {
                        Toast.makeText(context, "请输入统计组名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示重命名统计组对话框
     */
    private void showRenameSetDialog() {
        EditText etSetName = new EditText(context);
        etSetName.setText(currentSetName);
        etSetName.setHint("请输入新的统计组名称");

        new AlertDialog.Builder(context)
                .setTitle("重命名统计组")
                .setView(etSetName)
                .setPositiveButton("重命名", (dialog, which) -> {
                    String newName = etSetName.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName) && !newName.equals(currentSetName)) {
                        renameCurrentSet(newName);
                    } else if (newName.equals(currentSetName)) {
                        Toast.makeText(context, "名称没有变化", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "请输入有效的统计组名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示删除统计组对话框
     */
    private void showDeleteSetDialog() {
        if (statisticsSets.size() <= 1) {
            Toast.makeText(context, "至少需要保留一个统计组", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("删除统计组")
                .setMessage("确定要删除统计组 \"" + currentSetName + "\" 吗？\n此操作不可撤销！")
                .setPositiveButton("删除", (dialog, which) -> deleteCurrentSet())
                .setNegativeButton("取消", null)
                .show();
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
        saveStatisticsSets();

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
            saveStatisticsSets();

            // 更新当前统计套名称
            currentSetName = newName;
            saveCurrentSet();

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
        saveStatisticsSets();

        // 切换到第一个统计组
        currentSetName = statisticsSets.get(0).getName();
        saveCurrentSet();

        // 更新Spinner和加载数据
        setupSpinner();
        loadCurrentSetData();

        Toast.makeText(context, "统计组已删除", Toast.LENGTH_SHORT).show();
    }
}

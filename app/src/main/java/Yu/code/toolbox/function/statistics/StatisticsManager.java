package Yu.code.toolbox.function.statistics;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

public class StatisticsManager {

    private static final String PREFS_NAME = "statistics_prefs";
    private static final String KEY_STATISTICS_DATA = "statistics_data";

    private Context context;
    private View rootView;
    private StatisticsAdapter adapter;
    private SharedPreferences prefs;
    private Gson gson;

    private EditText etItemName;
    private EditText etItemQuantity;
    private RecyclerView rvStatistics;
    private TextView tvTotalCount;

    public StatisticsManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        initViews();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        etItemName = rootView.findViewById(R.id.et_item_name);
        etItemQuantity = rootView.findViewById(R.id.et_item_quantity);
        rvStatistics = rootView.findViewById(R.id.rv_statistics);
        tvTotalCount = rootView.findViewById(R.id.tv_total_count);

        // 设置添加按钮点击事件
        rootView.findViewById(R.id.btn_add_item).setOnClickListener(v -> addNewItem());

        // 设置一键清空按钮点击事件
        rootView.findViewById(R.id.btn_clear_all).setOnClickListener(v -> clearAllItems());

        // 设置导出按钮点击事件
        rootView.findViewById(R.id.btn_export).setOnClickListener(v -> exportData());

        // 设置导入按钮点击事件
        rootView.findViewById(R.id.btn_import).setOnClickListener(v -> importData());

        // 设置返回按钮点击事件
        rootView.findViewById(R.id.btn_back).setOnClickListener(v -> {
            // 隐藏功能界面，显示默认界面
            if (context instanceof FragmentActivity) {
                FragmentActivity activity = (FragmentActivity) context;
                Fragment fragment = activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_content_main);
                if (fragment != null && fragment.getChildFragmentManager() != null) {
                    Fragment homeFragment = fragment.getChildFragmentManager()
                            .findFragmentById(R.id.function_fragment);
                    if (homeFragment instanceof HomeFragment) {
                        ((HomeFragment) homeFragment).hideFunction();
                    }
                }
            }
        });
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
        String jsonData = prefs.getString(KEY_STATISTICS_DATA, "[]");
        try {
            Type type = new TypeToken<ArrayList<StatisticsItem>>(){}.getType();
            List<StatisticsItem> items = gson.fromJson(jsonData, type);
            if (items != null) {
                adapter.setItems(items);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果数据损坏，使用空列表
            adapter.setItems(new ArrayList<>());
        }
        updateTotalCount();
    }

    private void saveData() {
        try {
            String jsonData = gson.toJson(adapter.getItems());
            prefs.edit().putString(KEY_STATISTICS_DATA, jsonData).apply();
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

    // 其他方法将在后续添加...
}

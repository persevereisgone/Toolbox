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
import android.widget.ListView;
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
import java.util.Comparator;
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
    private static final String KEY_STATISTICS_DATA = "statistics_data";
    private static final String KEY_STATISTICS_SETS = "statistics_sets";
    private static final String KEY_CURRENT_SET = "current_set";
    private static final String DEFAULT_SET_NAME = "默认统计组";
    private static final int REQUEST_CODE_IMPORT_FILE = 6001;

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

        // 设置批量删除按钮点击事件
        rootView.findViewById(R.id.btn_batch_delete).setOnClickListener(v -> showBatchDeleteDialog());

        // 设置导出按钮点击事件
        rootView.findViewById(R.id.btn_export).setOnClickListener(v -> exportData());

        // 设置导入按钮点击事件
        rootView.findViewById(R.id.btn_import).setOnClickListener(v -> importData());

        // 设置全屏查看按钮点击事件
        rootView.findViewById(R.id.btn_fullscreen_list).setOnClickListener(v -> showFullscreenListDialog());

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

    private void showBatchDeleteDialog() {
        List<StatisticsItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(context, "没有物品可删除", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itemNames = new String[items.size()];
        boolean[] checkedItems = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            StatisticsItem item = items.get(i);
            itemNames[i] = item.getName() + " (" + item.getQuantity() + ")";
            checkedItems[i] = false;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("批量删除物品")
                .setMultiChoiceItems(itemNames, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("删除选中", (dialog, which) -> {
                    int deletedCount = 0;
                    for (int i = checkedItems.length - 1; i >= 0; i--) {
                        if (checkedItems[i]) {
                            adapter.removeItem(i);
                            deletedCount++;
                        }
                    }

                    if (deletedCount > 0) {
                        saveData();
                        updateTotalCount();
                        Toast.makeText(context, "已批量删除 " + deletedCount + " 个物品", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "请至少选择一个物品", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        alertDialog.show();

        // 显式设置按钮文字颜色，避免主题导致按钮“存在但不可见”
        if (alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_red_dark)
            );
        }
        if (alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    context.getResources().getColor(android.R.color.black)
            );
        }
    }

    private void showFullscreenListDialog() {
        List<StatisticsItem> items = adapter.getItems();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_statistics_fullscreen, null);
        TextView tvFullscreenTotal = dialogView.findViewById(R.id.tv_fullscreen_total_count);
        EditText etFullscreenSearch = dialogView.findViewById(R.id.et_fullscreen_search);
        Spinner spinnerFullscreenSort = dialogView.findViewById(R.id.spinner_fullscreen_sort);
        ListView lvFullscreenStatistics = dialogView.findViewById(R.id.lv_fullscreen_statistics);

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

        ArrayAdapter<String> fullscreenAdapter = new ArrayAdapter<String>(
                context,
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextSize(17f);
                textView.setTextColor(context.getResources().getColor(R.color.app_text_primary));
                textView.setPadding(24, 20, 24, 20);
                return view;
            }
        };
        lvFullscreenStatistics.setAdapter(fullscreenAdapter);

        Runnable refreshList = () -> {
            String keyword = etFullscreenSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
            List<StatisticsItem> filtered = new ArrayList<>();
            for (StatisticsItem item : items) {
                if (TextUtils.isEmpty(keyword) || item.getName().toLowerCase(Locale.getDefault()).contains(keyword)) {
                    filtered.add(item);
                }
            }

            int sortPos = spinnerFullscreenSort.getSelectedItemPosition();
            if (sortPos == 0) {
                Collections.sort(filtered, (a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));
            } else if (sortPos == 1) {
                Collections.sort(filtered, Comparator.comparingInt(StatisticsItem::getQuantity));
            } else {
                Collections.sort(filtered, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            }

            List<String> displayItems = new ArrayList<>();
            for (StatisticsItem item : filtered) {
                displayItems.add(item.getName() + "    数量: " + item.getQuantity());
            }
            if (displayItems.isEmpty()) {
                displayItems.add("没有匹配的物品");
            }

            fullscreenAdapter.clear();
            fullscreenAdapter.addAll(displayItems);
            fullscreenAdapter.notifyDataSetChanged();
        };

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
                refreshList.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        refreshList.run();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .create();
        dialog.show();

        // 设置按钮文字颜色，避免主题导致按钮不可见
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_blue_dark)
            );
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void editQuantity(int position) {
        if (position >= 0 && position < adapter.getItemCount()) {
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

        // 显示导出选项对话框
        String[] options = {"导出为 Excel 文件", "复制为文本", "分享到微信/QQ"};
        new AlertDialog.Builder(context)
                .setTitle("选择导出方式")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportToExcel();
                            break;
                        case 1:
                            exportToText();
                            break;
                        case 2:
                            shareData();
                            break;
                    }
                })
                .show();
    }

    /**
     * 导出为 Excel 文件（使用 CSV 格式，Excel 可以直接打开）
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
            
            // 写入 CSV 内容（使用 UTF-8 BOM 确保 Excel 正确识别中文）
            FileOutputStream fos = new FileOutputStream(csvFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            
            // 写入 UTF-8 BOM（Excel 需要这个来正确识别 UTF-8 编码）
            writer.write('\ufeff');
            
            // 写入标题行
            writer.write("物品名称,数量\n");
            
            // 写入数据行
            int totalQuantity = 0;
            for (StatisticsItem item : items) {
                // 如果物品名称包含逗号，需要用引号包裹
                String name = item.getName();
                if (name.contains(",") || name.contains("\"") || name.contains("\n")) {
                    name = "\"" + name.replace("\"", "\"\"") + "\"";
                }
                writer.write(name + "," + item.getQuantity() + "\n");
                totalQuantity += item.getQuantity();
            }
            
            // 写入总计行
            writer.write("总计," + totalQuantity + "\n");
            
            writer.flush();
            writer.close();
            fos.close();

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
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("导出成功")
                .setMessage("文件已保存到:\n" + excelFile.getAbsolutePath() + "\n\n是否立即分享？")
                .setPositiveButton("分享", (dialogInterface, which) -> shareFile(excelFile))
                .setNegativeButton("完成", null)
                .create();
        dialog.show();

        // 设置按钮文字颜色，避免主题导致按钮不可见
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_blue_dark)
            );
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    context.getResources().getColor(android.R.color.black)
            );
        }
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
     * 分享数据
     */
    private void shareData() {
        List<StatisticsItem> items = adapter.getItems();
        if (items.isEmpty()) {
            Toast.makeText(context, "没有数据可以分享", Toast.LENGTH_SHORT).show();
            return;
        }

        // 导出为 CSV 文件
        try {
            // 生成文件名
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "统计数据_" + currentSetName + "_" + timestamp + ".csv";

            // 保存文件到缓存目录
            File cacheDir = new File(context.getCacheDir(), "share");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File csvFile = new File(cacheDir, fileName);
            
            // 写入 CSV 内容
            FileOutputStream fos = new FileOutputStream(csvFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            
            // 写入 UTF-8 BOM
            writer.write('\ufeff');
            
            // 写入标题行
            writer.write("物品名称,数量\n");
            
            // 写入数据行
            int totalQuantity = 0;
            for (StatisticsItem item : items) {
                String name = item.getName();
                if (name.contains(",") || name.contains("\"") || name.contains("\n")) {
                    name = "\"" + name.replace("\"", "\"\"") + "\"";
                }
                writer.write(name + "," + item.getQuantity() + "\n");
                totalQuantity += item.getQuantity();
            }
            
            // 写入总计行
            writer.write("总计," + totalQuantity + "\n");
            
            writer.flush();
            writer.close();
            fos.close();

            // 分享文件
            shareFile(csvFile);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "分享失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        new AlertDialog.Builder(context)
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

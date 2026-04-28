package Yu.code.toolbox.function.lottery;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import Yu.code.toolbox.R;

/**
 * 抽奖内容管理页面管理器
 */
public class LotteryManageManager {

    private Context context;
    private View rootView;
    private LotteryItemAdapter adapter;
    private Gson gson;

    // UI组件
    private ImageButton btnBack;
    private Button btnQuickAdd;
    private Button btnAddItem;
    private Button btnImport;
    private Button btnExport;
    private RecyclerView rvLotteryItems;
    private TextView tvItemCount;
    private TextView tvTotalWeight;

    // 数据
    private List<LotteryItem> items;
    private OnDataChangeListener dataChangeListener;

    public interface OnDataChangeListener {
        void onDataChanged(List<LotteryItem> items);
    }

    public LotteryManageManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.gson = new Gson();
        this.items = new ArrayList<>();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = rootView.findViewById(R.id.btn_back);
        btnAddItem = rootView.findViewById(R.id.btn_add_item);
        btnImport = rootView.findViewById(R.id.btn_import);
        btnExport = rootView.findViewById(R.id.btn_export);
        rvLotteryItems = rootView.findViewById(R.id.rv_lottery_items);

        // 设置RecyclerView
        rvLotteryItems.setLayoutManager(new LinearLayoutManager(context));
        adapter = new LotteryItemAdapter(context);
        rvLotteryItems.setAdapter(adapter);

        // 设置项目操作监听器
        adapter.setOnItemActionListener(new LotteryItemAdapter.OnItemActionListener() {
            @Override
            public void onEditItem(int position, LotteryItem item) {
                showEditItemDialog(position, item);
            }

            @Override
            public void onDeleteItem(int position, LotteryItem item) {
                showDeleteItemDialog(position, item);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnQuickAdd.setOnClickListener(v -> showQuickAddDialog());
        btnAddItem.setOnClickListener(v -> showAddItemDialog());
        btnImport.setOnClickListener(v -> showImportExportDialog(true));
        btnExport.setOnClickListener(v -> showImportExportDialog(false));
    }

    public void setItems(List<LotteryItem> items) {
        this.items = new ArrayList<>(items);
        adapter.setItems(this.items);
        updateInfo();
    }

    public List<LotteryItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListener = listener;
    }

    private void onBackPressed() {
        if (dataChangeListener != null) {
            dataChangeListener.onDataChanged(items);
        }

        // 导航回抽奖页面
        try {
            NavController navController = Navigation.findNavController(rootView);
            Bundle args = new Bundle();
            args.putInt("function_id", R.layout.function_lottery);
            navController.navigate(R.id.nav_function, args);
        } catch (Exception e) {
            Toast.makeText(context, "无法返回抽奖页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateInfo() {
        tvItemCount.setText("项目总数: " + items.size());
        int totalWeight = 0;
        for (LotteryItem item : items) {
            totalWeight += item.getWeight();
        }
        tvTotalWeight.setText("总权重: " + totalWeight);
    }

    // ==================== 项目管理 ====================

    private void showQuickAddDialog() {
        String[] quickItems = {
                "是", "否", "可能", "不确定",
                "同意", "不同意", "赞成", "反对",
                "好", "不好", "可以", "不可以",
                "喜欢", "不喜欢", "想要", "不想要"
        };

        new AlertDialog.Builder(context)
                .setTitle("快速添加项目")
                .setItems(quickItems, (dialog, which) -> {
                    String itemName = quickItems[which];
                    LotteryItem item = new LotteryItem(itemName, 1, getRandomColor());
                    addItem(item);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAddItemDialog() {
        View dialogView = View.inflate(context, R.layout.dialog_lottery_item, null);
        EditText etName = dialogView.findViewById(R.id.et_item_name);
        EditText etWeight = dialogView.findViewById(R.id.et_item_weight);
        EditText etColor = dialogView.findViewById(R.id.et_item_color);

        // 设置默认值
        etWeight.setText("1");
        etColor.setText("#FF6B6B");

        new AlertDialog.Builder(context)
                .setTitle("添加抽奖项目")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String color = etColor.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(context, "请输入项目名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int weight = 1;
                    try {
                        weight = Integer.parseInt(weightStr);
                        if (weight < 1) weight = 1;
                    } catch (NumberFormatException e) {
                        weight = 1;
                    }

                    if (TextUtils.isEmpty(color)) {
                        color = "#FF6B6B";
                    }

                    LotteryItem item = new LotteryItem(name, weight, color);
                    addItem(item);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditItemDialog(int position, LotteryItem item) {
        View dialogView = View.inflate(context, R.layout.dialog_lottery_item, null);
        EditText etName = dialogView.findViewById(R.id.et_item_name);
        EditText etWeight = dialogView.findViewById(R.id.et_item_weight);
        EditText etColor = dialogView.findViewById(R.id.et_item_color);

        // 设置当前值
        etName.setText(item.getName());
        etWeight.setText(String.valueOf(item.getWeight()));
        etColor.setText(item.getColor());

        new AlertDialog.Builder(context)
                .setTitle("编辑抽奖项目")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String weightStr = etWeight.getText().toString().trim();
                    String color = etColor.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(context, "请输入项目名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int weight = 1;
                    try {
                        weight = Integer.parseInt(weightStr);
                        if (weight < 1) weight = 1;
                    } catch (NumberFormatException e) {
                        weight = 1;
                    }

                    if (TextUtils.isEmpty(color)) {
                        color = "#FF6B6B";
                    }

                    item.setName(name);
                    item.setWeight(weight);
                    item.setColor(color);

                    adapter.notifyItemChanged(position);
                    updateInfo();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteItemDialog(int position, LotteryItem item) {
        new AlertDialog.Builder(context)
                .setTitle("删除项目")
                .setMessage("确定要删除项目 \"" + item.getName() + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    adapter.removeItem(position);
                    items.remove(position);
                    updateInfo();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addItem(LotteryItem item) {
        adapter.addItem(item);
        items.add(item);
        updateInfo();
    }

    // ==================== 导入导出功能 ====================

    private void showImportExportDialog(boolean isImport) {
        View dialogView = View.inflate(context, R.layout.dialog_import_export, null);
        EditText etContent = dialogView.findViewById(R.id.et_import_export);
        Button btnClear = dialogView.findViewById(R.id.btn_clear);
        Button btnImport = dialogView.findViewById(R.id.btn_import);
        Button btnExport = dialogView.findViewById(R.id.btn_export);

        if (isImport) {
            btnExport.setVisibility(View.GONE);
            etContent.setHint("在此输入或粘贴要导入的内容...");
        } else {
            btnImport.setVisibility(View.GONE);
            // 导出当前数据
            etContent.setText(exportToText());
            etContent.setSelection(etContent.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        btnClear.setOnClickListener(v -> etContent.setText(""));
        btnImport.setOnClickListener(v -> {
            importFromText(etContent.getText().toString());
            dialog.dismiss();
        });
        btnExport.setOnClickListener(v -> {
            copyToClipboard(etContent.getText().toString());
            dialog.dismiss();
        });

        dialog.show();
    }

    private String exportToText() {
        StringBuilder sb = new StringBuilder();
        for (LotteryItem item : items) {
            sb.append(item.getName()).append("|")
                    .append(item.getWeight()).append("|")
                    .append(item.getColor()).append("\n");
        }
        return sb.toString();
    }

    private void importFromText(String text) {
        if (TextUtils.isEmpty(text.trim())) {
            Toast.makeText(context, "请输入要导入的内容", Toast.LENGTH_SHORT).show();
            return;
        }

        List<LotteryItem> importedItems = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (TextUtils.isEmpty(line)) continue;

            String[] parts = line.split("\\|");
            if (parts.length >= 1) {
                String name = parts[0].trim();
                int weight = 1;
                String color = "#FF6B6B";

                if (parts.length >= 2) {
                    try {
                        weight = Integer.parseInt(parts[1].trim());
                        if (weight < 1) weight = 1;
                    } catch (NumberFormatException e) {
                        weight = 1;
                    }
                }

                if (parts.length >= 3) {
                    color = parts[2].trim();
                    if (TextUtils.isEmpty(color)) {
                        color = "#FF6B6B";
                    }
                }

                importedItems.add(new LotteryItem(name, weight, color));
            }
        }

        if (importedItems.isEmpty()) {
            Toast.makeText(context, "没有找到有效的项目数据", Toast.LENGTH_SHORT).show();
            return;
        }

        // 询问是否替换现有数据
        new AlertDialog.Builder(context)
                .setTitle("导入确认")
                .setMessage("找到 " + importedItems.size() + " 个项目，是否替换当前数据？")
                .setPositiveButton("替换", (dialog, which) -> {
                    items.clear();
                    items.addAll(importedItems);
                    adapter.setItems(items);
                    updateInfo();
                    Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("追加", (dialog, which) -> {
                    items.addAll(importedItems);
                    adapter.setItems(items);
                    updateInfo();
                    Toast.makeText(context, "追加成功", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("取消", null)
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("抽奖数据", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private String getRandomColor() {
        String[] colors = {
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
                "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
        };
        return colors[(int) (Math.random() * colors.length)];
    }
}

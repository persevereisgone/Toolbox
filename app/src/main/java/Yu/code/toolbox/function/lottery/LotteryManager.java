package Yu.code.toolbox.function.lottery;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import Yu.code.toolbox.R;

/**
 * 抽奖功能管理器
 */
public class LotteryManager {

    private static final String PREFS_NAME = "lottery_prefs";
    private static final String KEY_LOTTERY_GROUPS = "lottery_groups";
    private static final String KEY_CURRENT_GROUP = "current_group";
    private static final String DEFAULT_GROUP_NAME = "默认抽奖组";

    private Context context;
    private View rootView;
    private LotteryItemAdapter adapter;
    private Gson gson;

    // UI组件
    private Spinner spinnerLotteryGroup;
    private Button btnManageGroups;
    private LotteryWheelView lotteryWheel;
    private LinearLayout resultArea;
    private TextView tvResult;
    private Button btnStartLottery;
    private Button btnReset;
    private Button btnManageItems;
    private TextView tvGroupInfo;

    // 数据
    private List<LotteryGroup> lotteryGroups;
    private String currentGroupName;
    private ArrayAdapter<String> groupAdapter;
    private SoundManager soundManager;

    public LotteryManager(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
        this.gson = new Gson();
        this.lotteryGroups = new ArrayList<>();
        this.soundManager = SoundManager.getInstance(context);

        initViews();
        loadLotteryGroups();
        setupListeners();
    }

    private void initViews() {
        spinnerLotteryGroup = rootView.findViewById(R.id.spinner_lottery_group);
        btnManageGroups = rootView.findViewById(R.id.btn_manage_groups);
        lotteryWheel = rootView.findViewById(R.id.lottery_wheel);
        resultArea = rootView.findViewById(R.id.result_area);
        tvResult = rootView.findViewById(R.id.tv_result);
        btnStartLottery = rootView.findViewById(R.id.btn_start_lottery);
        btnReset = rootView.findViewById(R.id.btn_reset);
        btnManageItems = rootView.findViewById(R.id.btn_manage_items);
        tvGroupInfo = rootView.findViewById(R.id.tv_group_info);

        // 设置转盘监听器
        lotteryWheel.setOnSpinCompleteListener(this::onSpinComplete);
    }

    private void setupListeners() {
        btnManageGroups.setOnClickListener(v -> showManageGroupsDialog());
        btnStartLottery.setOnClickListener(v -> startLottery());
        btnReset.setOnClickListener(v -> resetLottery());
        btnManageItems.setOnClickListener(v -> navigateToManagePage());
    }

    // ==================== 抽奖组管理 ====================

    /**
     * 加载抽奖组列表
     */
    private void loadLotteryGroups() {
        String groupsJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOTTERY_GROUPS, "");

        if (TextUtils.isEmpty(groupsJson)) {
            // 创建默认抽奖组和示例数据
            lotteryGroups.clear();
            createDefaultGroups();
            saveLotteryGroups();
        } else {
            Type type = new TypeToken<List<LotteryGroup>>(){}.getType();
            lotteryGroups = gson.fromJson(groupsJson, type);
            if (lotteryGroups == null) {
                lotteryGroups = new ArrayList<>();
                lotteryGroups.add(new LotteryGroup(DEFAULT_GROUP_NAME));
                saveLotteryGroups();
            }
        }

        // 加载当前选中的抽奖组
        currentGroupName = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_GROUP, DEFAULT_GROUP_NAME);
        if (!isGroupExists(currentGroupName)) {
            currentGroupName = lotteryGroups.get(0).getName();
            saveCurrentGroup();
        }

        setupGroupSpinner();
        loadCurrentGroupData();
    }

    /**
     * 保存抽奖组列表
     */
    private void saveLotteryGroups() {
        String groupsJson = gson.toJson(lotteryGroups);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOTTERY_GROUPS, groupsJson)
                .apply();
    }

    /**
     * 保存当前选中的抽奖组
     */
    private void saveCurrentGroup() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_GROUP, currentGroupName)
                .apply();
    }

    /**
     * 检查抽奖组是否存在
     */
    private boolean isGroupExists(String groupName) {
        for (LotteryGroup group : lotteryGroups) {
            if (group.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置抽奖组选择器
     */
    private void setupGroupSpinner() {
        List<String> groupNames = new ArrayList<>();
        for (LotteryGroup group : lotteryGroups) {
            groupNames.add(group.getName());
        }

        groupAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groupNames);
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLotteryGroup.setAdapter(groupAdapter);

        // 设置当前选中的抽奖组
        int position = groupNames.indexOf(currentGroupName);
        if (position >= 0) {
            spinnerLotteryGroup.setSelection(position);
        }

        // 设置选择监听器
        spinnerLotteryGroup.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedGroup = groupNames.get(position);
                if (!selectedGroup.equals(currentGroupName)) {
                    switchToGroup(selectedGroup);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不做处理
            }
        });
    }

    /**
     * 切换到指定的抽奖组
     */
    private void switchToGroup(String groupName) {
        // 保存当前抽奖组的数据
        saveCurrentGroupData();

        // 切换到新的抽奖组
        currentGroupName = groupName;
        saveCurrentGroup();

        // 加载新抽奖组的数据
        loadCurrentGroupData();
    }

    /**
     * 加载当前抽奖组的数据
     */
    private void loadCurrentGroupData() {
        LotteryGroup currentGroup = getCurrentGroup();
        if (currentGroup != null) {
            lotteryWheel.setItems(new ArrayList<>(currentGroup.getItems()));
            updateGroupInfo();
        }
    }

    /**
     * 更新抽奖组信息显示
     */
    private void updateGroupInfo() {
        LotteryGroup currentGroup = getCurrentGroup();
        if (currentGroup != null) {
            tvGroupInfo.setText("项目数量: " + currentGroup.getItemCount());
        }
    }

    /**
     * 导航到管理页面
     */
    private void navigateToManagePage() {
        try {
            NavController navController = Navigation.findNavController(rootView);
            Bundle args = new Bundle();
            args.putInt("function_id", R.layout.function_lottery_manage);
            navController.navigate(R.id.nav_function, args);
        } catch (Exception e) {
            Toast.makeText(context, "无法打开管理页面", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存当前抽奖组的数据
     */
    private void saveCurrentGroupData() {
        LotteryGroup currentGroup = getCurrentGroup();
        if (currentGroup != null) {
            currentGroup.setItems(new ArrayList<>(adapter.getItems()));
            saveLotteryGroups();
        }
    }

    /**
     * 获取当前抽奖组
     */
    private LotteryGroup getCurrentGroup() {
        for (LotteryGroup group : lotteryGroups) {
            if (group.getName().equals(currentGroupName)) {
                return group;
            }
        }
        return null;
    }

    // ==================== 抽奖组管理对话框 ====================

    /**
     * 显示抽奖组管理对话框
     */
    private void showManageGroupsDialog() {
        String[] options = {"创建新抽奖组", "重命名当前抽奖组", "删除当前抽奖组"};

        new AlertDialog.Builder(context)
                .setTitle("管理抽奖组")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCreateGroupDialog();
                            break;
                        case 1:
                            showRenameGroupDialog();
                            break;
                        case 2:
                            showDeleteGroupDialog();
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示创建抽奖组对话框
     */
    private void showCreateGroupDialog() {
        EditText etGroupName = new EditText(context);
        etGroupName.setHint("请输入抽奖组名称");

        new AlertDialog.Builder(context)
                .setTitle("创建新抽奖组")
                .setView(etGroupName)
                .setPositiveButton("创建", (dialog, which) -> {
                    String groupName = etGroupName.getText().toString().trim();
                    if (!TextUtils.isEmpty(groupName)) {
                        createNewGroup(groupName);
                    } else {
                        Toast.makeText(context, "请输入抽奖组名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示重命名抽奖组对话框
     */
    private void showRenameGroupDialog() {
        EditText etGroupName = new EditText(context);
        etGroupName.setText(currentGroupName);
        etGroupName.setHint("请输入新的抽奖组名称");

        new AlertDialog.Builder(context)
                .setTitle("重命名抽奖组")
                .setView(etGroupName)
                .setPositiveButton("重命名", (dialog, which) -> {
                    String newName = etGroupName.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName) && !newName.equals(currentGroupName)) {
                        renameCurrentGroup(newName);
                    } else if (newName.equals(currentGroupName)) {
                        Toast.makeText(context, "名称没有变化", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "请输入有效的抽奖组名称", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示删除抽奖组对话框
     */
    private void showDeleteGroupDialog() {
        if (lotteryGroups.size() <= 1) {
            Toast.makeText(context, "至少需要保留一个抽奖组", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("删除抽奖组")
                .setMessage("确定要删除抽奖组 \"" + currentGroupName + "\" 吗？\n此操作不可撤销！")
                .setPositiveButton("删除", (dialog, which) -> deleteCurrentGroup())
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 抽奖组操作方法 ====================

    /**
     * 创建新的抽奖组
     */
    private void createNewGroup(String groupName) {
        if (isGroupExists(groupName)) {
            Toast.makeText(context, "抽奖组名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        LotteryGroup newGroup = new LotteryGroup(groupName);
        lotteryGroups.add(newGroup);
        saveLotteryGroups();

        // 更新Spinner
        setupGroupSpinner();

        // 切换到新创建的抽奖组
        switchToGroup(groupName);

        Toast.makeText(context, "抽奖组 \"" + groupName + "\" 创建成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 重命名当前抽奖组
     */
    private void renameCurrentGroup(String newName) {
        if (isGroupExists(newName)) {
            Toast.makeText(context, "抽奖组名称已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        LotteryGroup currentGroup = getCurrentGroup();
        if (currentGroup != null) {
            currentGroup.setName(newName);
            saveLotteryGroups();

            currentGroupName = newName;
            saveCurrentGroup();

            // 更新Spinner
            setupGroupSpinner();

            Toast.makeText(context, "抽奖组重命名成功", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除当前抽奖组
     */
    private void deleteCurrentGroup() {
        if (lotteryGroups.size() <= 1) {
            Toast.makeText(context, "至少需要保留一个抽奖组", Toast.LENGTH_SHORT).show();
            return;
        }

        lotteryGroups.removeIf(group -> group.getName().equals(currentGroupName));
        saveLotteryGroups();

        // 切换到第一个抽奖组
        currentGroupName = lotteryGroups.get(0).getName();
        saveCurrentGroup();

        // 更新Spinner和加载数据
        setupGroupSpinner();
        loadCurrentGroupData();

        Toast.makeText(context, "抽奖组已删除", Toast.LENGTH_SHORT).show();
    }


    // ==================== 抽奖功能 ====================

    /**
     * 开始抽奖
     */
    private void startLottery() {
        if (adapter.getItems().isEmpty()) {
            Toast.makeText(context, "请先添加抽奖项目", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lotteryWheel.isSpinning()) {
            Toast.makeText(context, "转盘正在转动中...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 隐藏结果区域
        resultArea.setVisibility(View.GONE);

        // 开始转动
        lotteryWheel.startSpin();

        // 禁用按钮
        btnStartLottery.setEnabled(false);
        btnStartLottery.setText("转动中...");
    }

    /**
     * 重置抽奖
     */
    private void resetLottery() {
        lotteryWheel.reset();
        resultArea.setVisibility(View.GONE);
        btnStartLottery.setEnabled(true);
        btnStartLottery.setText("开始抽奖");
    }

    /**
     * 抽奖完成回调
     */
    private void onSpinComplete(LotteryItem selectedItem) {
        if (selectedItem != null) {
            tvResult.setText(selectedItem.getName());
            resultArea.setVisibility(View.VISIBLE);
        }

        // 恢复按钮
        btnStartLottery.setEnabled(true);
        btnStartLottery.setText("开始抽奖");
    }

    /**
     * 创建默认抽奖组
     */
    private void createDefaultGroups() {
        // 创建默认抽奖组
        LotteryGroup defaultGroup = new LotteryGroup(DEFAULT_GROUP_NAME);
        defaultGroup.addItem(new LotteryItem("一等奖", 1, "#FF6B6B"));
        defaultGroup.addItem(new LotteryItem("二等奖", 2, "#4ECDC4"));
        defaultGroup.addItem(new LotteryItem("三等奖", 3, "#45B7D1"));
        defaultGroup.addItem(new LotteryItem("四等奖", 4, "#96CEB4"));
        defaultGroup.addItem(new LotteryItem("五等奖", 5, "#FFEAA7"));
        defaultGroup.addItem(new LotteryItem("谢谢参与", 10, "#E0E0E0"));
        lotteryGroups.add(defaultGroup);

        // 创建美食抽奖组
        LotteryGroup foodGroup = new LotteryGroup("美食抽奖");
        foodGroup.addItem(new LotteryItem("火锅", 2, "#FF6B6B"));
        foodGroup.addItem(new LotteryItem("烧烤", 2, "#8B4513"));
        foodGroup.addItem(new LotteryItem("寿司", 1, "#FFD700"));
        foodGroup.addItem(new LotteryItem("披萨", 2, "#FFA500"));
        foodGroup.addItem(new LotteryItem("汉堡", 3, "#DEB887"));
        foodGroup.addItem(new LotteryItem("面条", 4, "#F5DEB3"));
        lotteryGroups.add(foodGroup);

        // 创建活动抽奖组
        LotteryGroup activityGroup = new LotteryGroup("活动抽奖");
        activityGroup.addItem(new LotteryItem("看电影", 2, "#9370DB"));
        activityGroup.addItem(new LotteryItem("KTV", 2, "#FF69B4"));
        activityGroup.addItem(new LotteryItem("游戏", 3, "#32CD32"));
        activityGroup.addItem(new LotteryItem("运动", 2, "#00CED1"));
        activityGroup.addItem(new LotteryItem("购物", 1, "#FF1493"));
        activityGroup.addItem(new LotteryItem("休息", 5, "#D3D3D3"));
        lotteryGroups.add(activityGroup);
    }

    /**
     * 销毁时保存数据
     */
    public void onDestroy() {
        saveCurrentGroupData();
    }
}

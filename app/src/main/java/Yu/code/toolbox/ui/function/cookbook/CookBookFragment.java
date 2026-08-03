package Yu.code.toolbox.ui.function.cookbook;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CookBookFragment extends Fragment {

    private View rootView;
    private CookBookManager cookBookManager;
    private CookBookListAdapter cookbookAdapter;
    private MenuListAdapter menuAdapter;
    private PurchaseListAdapter purchaseAdapter;
    private List<CookItem> cookbooks = new ArrayList<>();
    private CookItem pendingImageTarget;
    private ImageView pendingImagePreview;
    private String pendingImageUri;

    private LinearLayout tabCookbooks;
    private LinearLayout tabMenu;
    private LinearLayout tabPurchase;
    private LinearLayout tabManage;

    private TextView tvMenuEmptyHint;
    private RecyclerView rvMenuList;
    private TextView tvPurchaseEmptyHint;
    private RecyclerView rvPurchaseList;
    private TextView tvManageCount;
    private Button btnAddCustomCookbook;
    private Button btnBulkDeleteCookbook;
    private Button btnClearPurchased;
    private Button btnClearMenu;
    private RecyclerView rvCookbooks;
    private BottomNavigationView bottomNav;
    private ActivityResultLauncher<PickVisualMediaRequest> coverPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        coverPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    String storedPath = copyImageToInternalStorage(uri);
                    if (storedPath == null) {
                        Toast.makeText(requireContext(), "图片保存失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingImageUri = storedPath;
                    if (pendingImageTarget != null) {
                        pendingImageTarget.setImageUri(storedPath);
                    }
                    if (pendingImagePreview != null) {
                        CookbookImageLoader.load(requireContext(), storedPath, pendingImagePreview, Yu.code.toolbox.R.drawable.recipe);
                    }
                    Toast.makeText(requireContext(), "封面已选择", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(Yu.code.toolbox.R.layout.function_cookbook, container, false);
        initViews();
        initData();
        initListeners();
        return rootView;
    }

    private void initViews() {
        bottomNav = rootView.findViewById(Yu.code.toolbox.R.id.bottom_nav);
        tabCookbooks = rootView.findViewById(Yu.code.toolbox.R.id.tab_cookbooks);
        tabMenu = rootView.findViewById(Yu.code.toolbox.R.id.tab_menu);
        tabPurchase = rootView.findViewById(Yu.code.toolbox.R.id.tab_purchase);
        tabManage = rootView.findViewById(Yu.code.toolbox.R.id.tab_manage);
        rvCookbooks = rootView.findViewById(Yu.code.toolbox.R.id.cookbook_grid);
        tvMenuEmptyHint = rootView.findViewById(Yu.code.toolbox.R.id.tv_menu_empty_hint);
        rvMenuList = rootView.findViewById(Yu.code.toolbox.R.id.rv_menu_list);
        tvPurchaseEmptyHint = rootView.findViewById(Yu.code.toolbox.R.id.tv_purchase_empty_hint);
        rvPurchaseList = rootView.findViewById(Yu.code.toolbox.R.id.rv_purchase_list);
        tvManageCount = rootView.findViewById(Yu.code.toolbox.R.id.tv_manage_count);
        btnAddCustomCookbook = rootView.findViewById(Yu.code.toolbox.R.id.btn_add_custom_cookbook);
        btnBulkDeleteCookbook = rootView.findViewById(Yu.code.toolbox.R.id.btn_bulk_delete_cookbook);
        btnClearPurchased = rootView.findViewById(Yu.code.toolbox.R.id.btn_clear_purchased);
        btnClearMenu = rootView.findViewById(Yu.code.toolbox.R.id.btn_clear_menu);
    }

    private void initData() {
        cookBookManager = new CookBookManager(requireContext());
        cookbooks = cookBookManager.getAllCookbooks();
        cookbookAdapter = new CookBookListAdapter(requireContext(),
                this::showCookbookDetailDialog,
                this::addToMenu,
                this::showDeleteConfirmDialog);
        menuAdapter = new MenuListAdapter(new MenuListAdapter.OnMenuItemActionListener() {
            @Override
            public void onRemoveFromMenu(int cookbookId) {
                removeMenuItem(cookbookId);
            }

            @Override
            public void onItemClick(CookItem item) {
                showCookbookDetailDialog(item);
            }
        });
        purchaseAdapter = new PurchaseListAdapter((name, checked) -> {
            cookBookManager.setPurchased(name, checked);
            refreshPurchaseList();
        });

        rvCookbooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCookbooks.setAdapter(cookbookAdapter);
        rvMenuList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMenuList.setAdapter(menuAdapter);
        rvPurchaseList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPurchaseList.setAdapter(purchaseAdapter);

        refreshCookbooks();
        updateManagePanel();
        switchTab(Yu.code.toolbox.R.id.nav_cookbooks);
    }

    private void initListeners() {
        bottomNav.setOnItemSelectedListener(item -> {
            switchTab(item.getItemId());
            return true;
        });
        bottomNav.setSelectedItemId(Yu.code.toolbox.R.id.nav_cookbooks);
        if (btnAddCustomCookbook != null) {
            btnAddCustomCookbook.setOnClickListener(v -> showAddOrEditDialog(null, false));
        }
        if (btnBulkDeleteCookbook != null) {
            btnBulkDeleteCookbook.setOnClickListener(v -> showBulkDeleteCookbookDialog());
        }
        if (btnClearPurchased != null) {
            btnClearPurchased.setOnClickListener(v -> {
                cookBookManager.clearPurchased();
                refreshPurchaseList();
            });
        }
        if (btnClearMenu != null) {
            btnClearMenu.setOnClickListener(v -> {
                cookBookManager.clearMenu();
                refreshMenu();
                refreshPurchaseList();
            });
        }
    }

    private void switchTab(int itemId) {
        tabCookbooks.setVisibility(View.GONE);
        tabMenu.setVisibility(View.GONE);
        tabPurchase.setVisibility(View.GONE);
        tabManage.setVisibility(View.GONE);

        if (itemId == Yu.code.toolbox.R.id.nav_menu) {
            tabMenu.setVisibility(View.VISIBLE);
            refreshMenu();
        } else if (itemId == Yu.code.toolbox.R.id.nav_purchase) {
            tabPurchase.setVisibility(View.VISIBLE);
            refreshPurchaseList();
        } else if (itemId == Yu.code.toolbox.R.id.nav_manage) {
            tabManage.setVisibility(View.VISIBLE);
            updateManagePanel();
        } else {
            tabCookbooks.setVisibility(View.VISIBLE);
        }
    }

    private void refreshCookbooks() {
        cookbooks = cookBookManager.getAllCookbooks();
        cookbookAdapter.setData(cookbooks);
    }

    private void refreshMenu() {
        List<CookItem> menuItems = cookBookManager.getMenuItems();
        menuAdapter.setData(menuItems);
        tvMenuEmptyHint.setVisibility(menuItems.isEmpty() ? View.VISIBLE : View.GONE);
        rvMenuList.setVisibility(menuItems.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void refreshPurchaseList() {
        Map<String, Integer> purchaseMap = cookBookManager.generatePurchaseList();
        Set<String> purchased = cookBookManager.getPurchasedItems();
        List<PurchaseListAdapter.PurchaseItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : purchaseMap.entrySet()) {
            PurchaseListAdapter.PurchaseItem purchaseItem = new PurchaseListAdapter.PurchaseItem(entry.getKey(), entry.getValue());
            purchaseItem.purchased = purchased.contains(entry.getKey());
            items.add(purchaseItem);
        }
        items.sort((a, b) -> {
            if (a.purchased != b.purchased) {
                return a.purchased ? 1 : -1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });
        purchaseAdapter.setData(items);
        tvPurchaseEmptyHint.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        rvPurchaseList.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateManagePanel() {
        tvManageCount.setText(String.format(Locale.getDefault(), "%d", cookBookManager.getAllCookbooks().size()));
    }

    private void showCookbookDetailDialog(CookItem item) {
        View view = LayoutInflater.from(requireContext()).inflate(Yu.code.toolbox.R.layout.dialog_cookbook_detail, null, false);
        ImageView ivCover = view.findViewById(Yu.code.toolbox.R.id.iv_detail_cover);
        TextView tvTitle = view.findViewById(Yu.code.toolbox.R.id.tv_dialog_title);
        TextView tvCategory = view.findViewById(Yu.code.toolbox.R.id.tv_category);
        TextView tvIngredients = view.findViewById(Yu.code.toolbox.R.id.tv_ingredients);
        TextView tvSeasonings = view.findViewById(Yu.code.toolbox.R.id.tv_seasonings);
        TextView tvSteps = view.findViewById(Yu.code.toolbox.R.id.tv_steps);
        Button btnBack = view.findViewById(Yu.code.toolbox.R.id.btn_back);
        tvTitle.setText(item.getName());
        tvCategory.setText(item.getCategory());
        tvIngredients.setText(item.getIngredients());
        tvSeasonings.setText(item.getSeasonings());
        tvSteps.setText(item.getSteps());
        CookbookImageLoader.load(requireContext(), item.getImageUri(), ivCover, Yu.code.toolbox.R.drawable.recipe);

        Button btnEdit = view.findViewById(Yu.code.toolbox.R.id.btn_edit_detail);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnBack.setOnClickListener(v -> dialog.dismiss());
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showAddOrEditDialog(item, true);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
            );
        }
    }

    private void showAddOrEditDialog(CookItem editingItem, boolean isEditing) {
        View view = LayoutInflater.from(requireContext()).inflate(Yu.code.toolbox.R.layout.dialog_add_edit_cookbook, null, false);
        TextView tvDialogTitle = view.findViewById(Yu.code.toolbox.R.id.tv_dialog_title);
        ImageView ivCoverPreview = view.findViewById(Yu.code.toolbox.R.id.iv_cover_preview);
        EditText etName = view.findViewById(Yu.code.toolbox.R.id.et_cookbook_name);
        EditText etIngredients = view.findViewById(Yu.code.toolbox.R.id.et_ingredients);
        EditText etSeasonings = view.findViewById(Yu.code.toolbox.R.id.et_seasonings);
        EditText etSteps = view.findViewById(Yu.code.toolbox.R.id.et_steps);
        Spinner etCategory = view.findViewById(Yu.code.toolbox.R.id.et_category);
        Button btnCancel = view.findViewById(Yu.code.toolbox.R.id.btn_cancel);
        Button btnSave = view.findViewById(Yu.code.toolbox.R.id.btn_save);
        Button btnPickCover = view.findViewById(Yu.code.toolbox.R.id.btn_pick_cover);
        Button btnOnlineCover = view.findViewById(Yu.code.toolbox.R.id.btn_online_cover);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                new String[]{CookBookManager.CATEGORY_MAIN, CookBookManager.CATEGORY_SOUP, CookBookManager.CATEGORY_COLD,
                        CookBookManager.CATEGORY_DESSERT, CookBookManager.CATEGORY_SIDE});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etCategory.setAdapter(categoryAdapter);

        pendingImageTarget = editingItem;
        pendingImagePreview = ivCoverPreview;
        pendingImageUri = editingItem != null ? editingItem.getImageUri() : null;
        tvDialogTitle.setText(isEditing ? "编辑菜谱" : "新建菜谱");
        if (editingItem != null) {
            etName.setText(editingItem.getName());
            etIngredients.setText(editingItem.getIngredients());
            etSeasonings.setText(editingItem.getSeasonings());
            etSteps.setText(editingItem.getSteps());
            etCategory.setSelection(findCategoryIndex(editingItem.getCategory()));
            CookbookImageLoader.load(requireContext(), editingItem.getImageUri(), ivCoverPreview, Yu.code.toolbox.R.drawable.recipe);
        } else {
            ivCoverPreview.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        }

        btnPickCover.setOnClickListener(v -> pickCoverImage());
        btnOnlineCover.setOnClickListener(v -> Toast.makeText(requireContext(), "在线找图功能稍后接入图片源", Toast.LENGTH_SHORT).show());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View.OnClickListener saveAction = v -> {
            CookItem target = editingItem == null ? new CookItem() : editingItem;
            target.setName(etName.getText().toString().trim());
            target.setIngredients(etIngredients.getText().toString().trim());
            target.setSeasonings(etSeasonings.getText().toString().trim());
            target.setSteps(etSteps.getText().toString().trim());
            target.setCategory((String) etCategory.getSelectedItem());
            target.setImageUri(pendingImageUri);
            if (isEditing) {
                cookBookManager.updateCookbook(target);
            } else {
                cookBookManager.addCookbook(target);
            }
            refreshCookbooks();
            updateManagePanel();
            dialog.dismiss();
        };
        btnSave.setOnClickListener(saveAction);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
            );
        }
    }

    private void pickCoverImage() {
        coverPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void showDeleteConfirmDialog(CookItem item) {
        Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(requireContext(),
                "确认删除",
                "确定要删除菜谱“" + item.getName() + "”吗？",
                "删除",
                "取消",
                (dialog, which) -> {
                    cookBookManager.deleteCookbook(item.getId());
                    refreshCookbooks();
                    updateManagePanel();
                    Toast.makeText(requireContext(), "菜谱已删除", Toast.LENGTH_SHORT).show();
                },
                null);
    }

    private void addToMenu(CookItem item) {
        cookBookManager.addToMenu(item);
        Toast.makeText(requireContext(), "已加入今日菜单", Toast.LENGTH_SHORT).show();
        refreshMenu();
        refreshPurchaseList();
    }

    private void showBulkDeleteCookbookDialog() {
        List<CookItem> allCookbooks = cookBookManager.getAllCookbooks();
        if (allCookbooks.isEmpty()) {
            Toast.makeText(requireContext(), "没有可删除的菜谱", Toast.LENGTH_SHORT).show();
            return;
        }
        View view = LayoutInflater.from(requireContext()).inflate(Yu.code.toolbox.R.layout.dialog_bulk_delete_cookbook, null, false);
        TextView tvTitle = view.findViewById(Yu.code.toolbox.R.id.tv_bulk_delete_title);
        TextView tvSelectedCount = view.findViewById(Yu.code.toolbox.R.id.tv_bulk_delete_selected_count);
        EditText etSearch = view.findViewById(Yu.code.toolbox.R.id.et_bulk_delete_search);
        Button btnSelectAll = view.findViewById(Yu.code.toolbox.R.id.btn_bulk_delete_select_all);
        Button btnInvert = view.findViewById(Yu.code.toolbox.R.id.btn_bulk_delete_invert);
        android.widget.ListView listView = view.findViewById(Yu.code.toolbox.R.id.lv_bulk_delete_cookbooks);
        Button btnCancel = view.findViewById(Yu.code.toolbox.R.id.btn_bulk_delete_cancel);
        Button btnConfirm = view.findViewById(Yu.code.toolbox.R.id.btn_bulk_delete_confirm);

        tvTitle.setText("批量删除菜谱");
        List<CookItem> filtered = new ArrayList<>(allCookbooks);
        boolean[] checked = new boolean[allCookbooks.size()];

        java.util.function.Consumer<Integer> updateCount = count -> tvSelectedCount.setText("已选择 " + count + " 项");
        java.util.function.Consumer<List<CookItem>> refreshList = items -> {
            List<String> names = new ArrayList<>();
            for (CookItem item : items) {
                names.add(item.getName());
            }
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_multiple_choice, names);
            listView.setAdapter(adapter);
            listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_MULTIPLE);
            for (int i = 0; i < items.size(); i++) {
                CookItem item = items.get(i);
                int originalIndex = allCookbooks.indexOf(item);
                listView.setItemChecked(i, originalIndex >= 0 && checked[originalIndex]);
            }
        };

        java.util.function.Consumer<Void> refreshSelected = unused -> {
            int count = 0;
            for (boolean b : checked) {
                if (b) count++;
            }
            updateCount.accept(count);
        };

        refreshList.accept(filtered);
        refreshSelected.accept(null);

        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            CookItem current = filtered.get(position);
            int originalIndex = allCookbooks.indexOf(current);
            if (originalIndex >= 0) {
                checked[originalIndex] = listView.isItemChecked(position);
                refreshSelected.accept(null);
            }
        });

        btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < filtered.size(); i++) {
                CookItem item = filtered.get(i);
                int originalIndex = allCookbooks.indexOf(item);
                if (originalIndex >= 0) checked[originalIndex] = true;
            }
            refreshList.accept(filtered);
            refreshSelected.accept(null);
        });

        btnInvert.setOnClickListener(v -> {
            for (int i = 0; i < filtered.size(); i++) {
                CookItem item = filtered.get(i);
                int originalIndex = allCookbooks.indexOf(item);
                if (originalIndex >= 0) checked[originalIndex] = !checked[originalIndex];
            }
            refreshList.accept(filtered);
            refreshSelected.accept(null);
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim().toLowerCase();
                filtered.clear();
                for (CookItem item : allCookbooks) {
                    String name = item.getName() == null ? "" : item.getName().toLowerCase();
                    if (query.isEmpty() || name.contains(query)) {
                        filtered.add(item);
                    }
                }
                refreshList.accept(filtered);
                refreshSelected.accept(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            int selectedCount = 0;
            for (boolean itemChecked : checked) {
                if (itemChecked) selectedCount++;
            }
            if (selectedCount == 0) {
                Toast.makeText(requireContext(), "请至少选择一个菜谱", Toast.LENGTH_SHORT).show();
                return;
            }
            Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(requireContext(),
                    "再次确认删除",
                    "确定要删除已选择的 " + selectedCount + " 个菜谱吗？",
                    "确认删除",
                    "取消",
                    (confirmDialog, which) -> {
                        int deletedCount = 0;
                        for (int i = checked.length - 1; i >= 0; i--) {
                            if (checked[i]) {
                                cookBookManager.deleteCookbook(allCookbooks.get(i).getId());
                                deletedCount++;
                            }
                        }
                        refreshCookbooks();
                        updateManagePanel();
                        Toast.makeText(requireContext(), "已删除 " + deletedCount + " 个菜谱", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    },
                    null);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.85)
            );
        }
    }

    private void removeMenuItem(int cookbookId) {
        cookBookManager.removeFromMenu(cookbookId);
        refreshMenu();
        refreshPurchaseList();
    }

    private String copyImageToInternalStorage(android.net.Uri sourceUri) {
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            File dir = new File(requireContext().getFilesDir(), "cookbook_images");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String fileName = "cook_" + System.currentTimeMillis() + ".jpg";
            File targetFile = new File(dir, fileName);
            try (InputStream inputStream = resolver.openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                if (inputStream == null) {
                    return null;
                }
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
            }
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private int findCategoryIndex(String category) {
        String[] categories = new String[]{CookBookManager.CATEGORY_MAIN, CookBookManager.CATEGORY_SOUP,
                CookBookManager.CATEGORY_COLD, CookBookManager.CATEGORY_DESSERT, CookBookManager.CATEGORY_SIDE};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
        pendingImageTarget = null;
        pendingImagePreview = null;
        pendingImageUri = null;
    }
}

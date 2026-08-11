package Yu.code.toolbox.ui.function.cookbook;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CookBookFragment extends Fragment {

    private View rootView;
    private CookBookManager cookBookManager;
    private CookBookImportExportHelper importExportHelper;
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
    private EditText etCookbookSearch;
    private Button btnAddCustomCookbook;
    private Button btnClearPurchased;
    private Button btnClearMenu;
    private Button btnImportCookbook;
    private Button btnExportCookbook;
    private Button btnDownloadCookbookTemplate;
    private ImageButton btnCookbookFilter;
    private ImageButton btnCookbookSearchClear;
    private RecyclerView rvCookbooks;
    private BottomNavigationView bottomNav;
    private ActivityResultLauncher<PickVisualMediaRequest> coverPickerLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> importCookbookLauncher;
    private ActivityResultLauncher<String> exportCookbookLauncher;
    private ActivityResultLauncher<String> downloadTemplateLauncher;
    private Uri pendingCameraUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraPermissionLauncher = registerForActivityResult(new RequestPermission(), granted -> {
            if (!isAdded()) return;
            if (Boolean.TRUE.equals(granted)) {
                launchCamera();
            } else {
                Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (!isAdded()) return;
            if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
                String storedPath = copyImageToInternalStorage(pendingCameraUri);
                if (storedPath == null) {
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_image_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                pendingImageUri = storedPath;
                if (pendingImageTarget != null) {
                    pendingImageTarget.setImageUri(storedPath);
                }
                if (pendingImagePreview != null) {
                    CookbookImageLoader.load(requireContext(), storedPath, pendingImagePreview, Yu.code.toolbox.R.drawable.recipe);
                }
                Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_cover_selected, Toast.LENGTH_SHORT).show();
            }
        });
        importCookbookLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (!isAdded() || uri == null || importExportHelper == null) return;
            importExportHelper.importFromUri(uri);
        });
        exportCookbookLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), uri -> {
            if (!isAdded() || uri == null || importExportHelper == null) return;
            importExportHelper.exportToUri(uri);
        });
        downloadTemplateLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"), uri -> {
            if (!isAdded() || uri == null || importExportHelper == null) return;
            try {
                importExportHelper.exportTemplateImage(uri);
            } catch (IOException e) {
                Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_template_download_failed, Toast.LENGTH_SHORT).show();
            }
        });
        coverPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (!isAdded() || uri == null) {
                        return;
                    }
                    String storedPath = copyImageToInternalStorage(uri);
                    if (storedPath == null) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_image_save_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pendingImageUri = storedPath;
                    if (pendingImageTarget != null) {
                        pendingImageTarget.setImageUri(storedPath);
                    }
                    if (pendingImagePreview != null) {
                        CookbookImageLoader.load(requireContext(), storedPath, pendingImagePreview, Yu.code.toolbox.R.drawable.recipe);
                    }
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_cover_selected, Toast.LENGTH_SHORT).show();
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
        etCookbookSearch = rootView.findViewById(Yu.code.toolbox.R.id.et_cookbook_search);
        tvMenuEmptyHint = rootView.findViewById(Yu.code.toolbox.R.id.tv_menu_empty_hint);
        rvMenuList = rootView.findViewById(Yu.code.toolbox.R.id.rv_menu_list);
        tvPurchaseEmptyHint = rootView.findViewById(Yu.code.toolbox.R.id.tv_purchase_empty_hint);
        rvPurchaseList = rootView.findViewById(Yu.code.toolbox.R.id.rv_purchase_list);
        tvManageCount = rootView.findViewById(Yu.code.toolbox.R.id.tv_manage_count);
        btnAddCustomCookbook = rootView.findViewById(Yu.code.toolbox.R.id.btn_add_custom_cookbook);
        btnClearPurchased = rootView.findViewById(Yu.code.toolbox.R.id.btn_clear_purchased);
        btnClearMenu = rootView.findViewById(Yu.code.toolbox.R.id.btn_clear_menu);
        btnImportCookbook = rootView.findViewById(Yu.code.toolbox.R.id.btn_import_cookbook);
        btnExportCookbook = rootView.findViewById(Yu.code.toolbox.R.id.btn_export_cookbook);
        btnDownloadCookbookTemplate = rootView.findViewById(Yu.code.toolbox.R.id.btn_download_cookbook_template);
        btnCookbookFilter = rootView.findViewById(Yu.code.toolbox.R.id.btn_cookbook_filter);
        btnCookbookSearchClear = rootView.findViewById(Yu.code.toolbox.R.id.btn_cookbook_search_clear);
    }

    private void initData() {
        cookBookManager = new CookBookManager(requireContext());
        importExportHelper = new CookBookImportExportHelper(requireContext(), cookBookManager,
                new CookBookImportExportHelper.UiCallback() {
                    @Override
                    public void showToast(String message) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void showResultDialog(String title, String message) {
                        if (!isAdded()) return;
                        showImportResultDialog(title, message);
                    }

                    @Override
                    public void onImportDataChanged() {
                        if (!isAdded()) return;
                        refreshCookbooks();
                        updateManagePanel();
                    }
                });
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
            btnAddCustomCookbook.setOnClickListener(v -> showCustomCookbookActions());
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
        if (btnImportCookbook != null) {
            btnImportCookbook.setOnClickListener(v -> importCookbookLauncher.launch(new String[]{"application/zip", "text/csv", "text/*"}));
        }
        if (btnExportCookbook != null) {
            btnExportCookbook.setOnClickListener(v -> exportCookbookLauncher.launch("cookbook_export.zip"));
        }
        if (btnDownloadCookbookTemplate != null) {
            btnDownloadCookbookTemplate.setOnClickListener(v -> downloadTemplateLauncher.launch("cookbook_template.png"));
        }
        if (btnCookbookFilter != null) {
            btnCookbookFilter.setOnClickListener(v -> showCookbookFilterDialog());
        }
        if (btnCookbookSearchClear != null) {
            btnCookbookSearchClear.setOnClickListener(v -> {
                if (etCookbookSearch != null) {
                    etCookbookSearch.setText("");
                }
            });
        }
        if (etCookbookSearch != null) {
            etCookbookSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentCookbookSearchQuery = s == null ? "" : s.toString();
                    if (btnCookbookSearchClear != null) {
                        btnCookbookSearchClear.setVisibility(
                                currentCookbookSearchQuery.trim().isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    refreshCookbooks();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
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

    private String currentCookbookFilter = "全部";
    private String currentCookbookSearchQuery = "";

    private void refreshCookbooks() {
        List<CookItem> allCookbooks = cookBookManager.getAllCookbooks();
        List<CookItem> filtered = new ArrayList<>();
        String search = currentCookbookSearchQuery == null ? "" : currentCookbookSearchQuery.trim().toLowerCase(Locale.ROOT);
        String allLabel = getString(Yu.code.toolbox.R.string.cookbook_category_all);
        for (CookItem item : allCookbooks) {
            if (item == null) {
                continue;
            }
            String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
            String category = item.getCategory() == null ? "" : item.getCategory();
            boolean matchesSearch = search.isEmpty() || name.contains(search);
            boolean matchesCategory = allLabel.equals(currentCookbookFilter) || currentCookbookFilter.equals(category);
            if (matchesSearch && matchesCategory) {
                filtered.add(item);
            }
        }
        cookbooks = filtered;
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
        tvManageCount.setText(getString(
                Yu.code.toolbox.R.string.cookbook_manage_count,
                cookBookManager.getAllCookbooks().size()));
    }

    private void showCookbookFilterDialog() {
        List<String> categories = cookBookManager.getFilterCategories();
        Yu.code.toolbox.Utils.DialogUtils.showBottomListDialog(
                requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_filter),
                getString(Yu.code.toolbox.R.string.cookbook_filter_current, currentCookbookFilter),
                categories,
                currentCookbookFilter,
                value -> {
                    currentCookbookFilter = value;
                    refreshCookbooks();
                    Toast.makeText(requireContext(),
                            getString(Yu.code.toolbox.R.string.cookbook_filter_applied, currentCookbookFilter),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showCustomCookbookActions() {
        List<String> actions = new ArrayList<>();
        actions.add(getString(Yu.code.toolbox.R.string.cookbook_action_new));
        actions.add(getString(Yu.code.toolbox.R.string.cookbook_action_bulk_delete));
        actions.add(getString(Yu.code.toolbox.R.string.cookbook_action_edit_categories));
        Yu.code.toolbox.Utils.DialogUtils.showBottomListDialog(
                requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_custom_actions),
                getString(Yu.code.toolbox.R.string.cookbook_action_sheet_desc),
                actions,
                null,
                value -> {
                    if (getString(Yu.code.toolbox.R.string.cookbook_action_new).equals(value)) {
                        showAddOrEditDialog(null, false);
                    } else if (getString(Yu.code.toolbox.R.string.cookbook_action_bulk_delete).equals(value)) {
                        showBulkDeleteCookbookDialog();
                    } else if (getString(Yu.code.toolbox.R.string.cookbook_action_edit_categories).equals(value)) {
                        showEditCategoriesDialog();
                    }
                }
        );
    }

    private void showEditCategoriesDialog() {
        View view = LayoutInflater.from(requireContext())
                .inflate(Yu.code.toolbox.R.layout.dialog_edit_categories, null, false);
        RecyclerView rvCategories = view.findViewById(Yu.code.toolbox.R.id.rv_edit_categories);
        Button btnAddCategory = view.findViewById(Yu.code.toolbox.R.id.btn_add_category);
        Button btnClose = view.findViewById(Yu.code.toolbox.R.id.btn_edit_categories_close);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final CategoryEditAdapter[] adapterHolder = new CategoryEditAdapter[1];
        adapterHolder[0] = new CategoryEditAdapter(new CategoryEditAdapter.Listener() {
            @Override
            public void onRename(String category) {
                promptRenameCategory(category, () -> {
                    adapterHolder[0].setData(cookBookManager.getCategories());
                    ensureFilterStillValid();
                    refreshCookbooks();
                });
            }

            @Override
            public void onDelete(String category) {
                promptDeleteCategory(category, () -> {
                    adapterHolder[0].setData(cookBookManager.getCategories());
                    ensureFilterStillValid();
                    refreshCookbooks();
                });
            }
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapterHolder[0]);
        adapterHolder[0].setData(cookBookManager.getCategories());

        btnAddCategory.setOnClickListener(v -> promptAddCategory(() -> {
            adapterHolder[0].setData(cookBookManager.getCategories());
        }));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.75)
            );
        }
    }

    private void promptAddCategory(Runnable onChanged) {
        Yu.code.toolbox.Utils.DialogUtils.showInputDialog(
                requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_add_category),
                getString(Yu.code.toolbox.R.string.cookbook_category_name_hint),
                "",
                input -> {
                    String name = input == null ? "" : input.trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!cookBookManager.addCategory(name)) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_added, Toast.LENGTH_SHORT).show();
                    if (onChanged != null) onChanged.run();
                },
                null
        );
    }

    private void promptRenameCategory(String oldName, Runnable onChanged) {
        Yu.code.toolbox.Utils.DialogUtils.showInputDialog(
                requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_action_edit_categories),
                getString(Yu.code.toolbox.R.string.cookbook_category_name_hint),
                oldName,
                input -> {
                    String name = input == null ? "" : input.trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!cookBookManager.renameCategory(oldName, name)) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (oldName.equals(currentCookbookFilter)) {
                        currentCookbookFilter = name;
                    }
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_renamed, Toast.LENGTH_SHORT).show();
                    if (onChanged != null) onChanged.run();
                },
                null
        );
    }

    private void promptDeleteCategory(String category, Runnable onChanged) {
        if (cookBookManager.getCategories().size() <= 1) {
            Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_last, Toast.LENGTH_SHORT).show();
            return;
        }
        String fallback = cookBookManager.getFallbackCategory(category);
        Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(
                requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_action_edit_categories),
                getString(Yu.code.toolbox.R.string.cookbook_category_in_use_migrate, fallback),
                getString(android.R.string.ok),
                getString(android.R.string.cancel),
                (dialog, which) -> {
                    if (!cookBookManager.deleteCategory(category)) {
                        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_last, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (category.equals(currentCookbookFilter)) {
                        currentCookbookFilter = getString(Yu.code.toolbox.R.string.cookbook_category_all);
                    }
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_category_deleted, Toast.LENGTH_SHORT).show();
                    if (onChanged != null) onChanged.run();
                },
                null
        );
    }

    private void ensureFilterStillValid() {
        List<String> filters = cookBookManager.getFilterCategories();
        if (!filters.contains(currentCookbookFilter)) {
            currentCookbookFilter = getString(Yu.code.toolbox.R.string.cookbook_category_all);
        }
    }

    private void showCookbookDetailDialog(CookItem item) {
        if (item == null) {
            return;
        }
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

        List<String> categoryList = cookBookManager.getCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etCategory.setAdapter(categoryAdapter);

        pendingImageTarget = editingItem;
        pendingImagePreview = ivCoverPreview;
        pendingImageUri = editingItem != null ? editingItem.getImageUri() : null;
        tvDialogTitle.setText(isEditing
                ? getString(Yu.code.toolbox.R.string.cookbook_edit_title)
                : getString(Yu.code.toolbox.R.string.cookbook_new_title));
        if (editingItem != null) {
            etName.setText(editingItem.getName());
            etIngredients.setText(editingItem.getIngredients());
            etSeasonings.setText(editingItem.getSeasonings());
            etSteps.setText(editingItem.getSteps());
            etCategory.setSelection(findCategoryIndex(editingItem.getCategory(), categoryList));
            CookbookImageLoader.load(requireContext(), editingItem.getImageUri(), ivCoverPreview, Yu.code.toolbox.R.drawable.recipe);
        } else {
            ivCoverPreview.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        }

        btnPickCover.setOnClickListener(v -> showCoverSourceDialog());
        btnOnlineCover.setEnabled(false);
        btnOnlineCover.setAlpha(0.5f);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View.OnClickListener saveAction = v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_name_required, Toast.LENGTH_SHORT).show();
                return;
            }
            CookItem target = editingItem == null ? new CookItem() : editingItem;
            target.setName(name);
            target.setIngredients(etIngredients.getText().toString().trim());
            target.setSeasonings(etSeasonings.getText().toString().trim());
            target.setSteps(etSteps.getText().toString().trim());
            Object selected = etCategory.getSelectedItem();
            target.setCategory(selected == null ? CookBookManager.CATEGORY_MAIN : selected.toString());
            target.setImageUri(pendingImageUri);
            if (isEditing) {
                cookBookManager.updateCookbook(target);
            } else {
                cookBookManager.addCookbook(target);
            }
            refreshCookbooks();
            refreshMenu();
            refreshPurchaseList();
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

    private void showCoverSourceDialog() {
        Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_cover_source_title),
                getString(Yu.code.toolbox.R.string.cookbook_cover_source_message),
                getString(Yu.code.toolbox.R.string.cookbook_pick_gallery),
                getString(Yu.code.toolbox.R.string.cookbook_take_photo),
                (dialog, which) -> pickCoverImage(),
                (dialog, which) -> takeCoverPhoto());
    }

    private void takeCoverPhoto() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File photoFile = new File(requireContext().getCacheDir(), "cookbook_camera_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_camera_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmDialog(CookItem item) {
        String name = item == null || item.getName() == null ? "" : item.getName();
        Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(requireContext(),
                getString(Yu.code.toolbox.R.string.cookbook_delete_confirm_title),
                getString(Yu.code.toolbox.R.string.cookbook_delete_confirm_message, name),
                getString(Yu.code.toolbox.R.string.cookbook_delete),
                getString(Yu.code.toolbox.R.string.cookbook_cancel),
                (dialog, which) -> {
                    if (item == null) return;
                    cookBookManager.deleteCookbook(item.getId());
                    refreshCookbooks();
                    refreshMenu();
                    refreshPurchaseList();
                    updateManagePanel();
                    Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_deleted, Toast.LENGTH_SHORT).show();
                },
                null);
    }

    private void addToMenu(CookItem item) {
        cookBookManager.addToMenu(item);
        Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_added_to_menu, Toast.LENGTH_SHORT).show();
        refreshMenu();
        refreshPurchaseList();
    }

    private void showBulkDeleteCookbookDialog() {
        List<CookItem> allCookbooks = cookBookManager.getAllCookbooks();
        if (allCookbooks.isEmpty()) {
            Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_no_deleteable, Toast.LENGTH_SHORT).show();
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

        tvTitle.setText(getString(Yu.code.toolbox.R.string.cookbook_bulk_delete_title));
        List<CookItem> filtered = new ArrayList<>(allCookbooks);
        boolean[] checked = new boolean[allCookbooks.size()];

        java.util.function.Consumer<Integer> updateCount = count ->
                tvSelectedCount.setText(getString(Yu.code.toolbox.R.string.cookbook_selected_count, count));
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
                Toast.makeText(requireContext(), Yu.code.toolbox.R.string.cookbook_select_at_least_one, Toast.LENGTH_SHORT).show();
                return;
            }
            Yu.code.toolbox.Utils.DialogUtils.showCommonConfirmDialog(requireContext(),
                    getString(Yu.code.toolbox.R.string.cookbook_bulk_delete_confirm_title),
                    getString(Yu.code.toolbox.R.string.cookbook_bulk_delete_confirm_message, selectedCount),
                    getString(Yu.code.toolbox.R.string.cookbook_bulk_delete_confirm),
                    getString(Yu.code.toolbox.R.string.cookbook_cancel),
                    (confirmDialog, which) -> {
                        int deletedCount = 0;
                        for (int i = checked.length - 1; i >= 0; i--) {
                            if (checked[i]) {
                                cookBookManager.deleteCookbook(allCookbooks.get(i).getId());
                                deletedCount++;
                            }
                        }
                        refreshCookbooks();
                        refreshMenu();
                        refreshPurchaseList();
                        updateManagePanel();
                        Toast.makeText(requireContext(),
                                getString(Yu.code.toolbox.R.string.cookbook_bulk_deleted, deletedCount),
                                Toast.LENGTH_SHORT).show();
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

    private void showImportResultDialog(String title, String message) {
        View view = LayoutInflater.from(requireContext()).inflate(Yu.code.toolbox.R.layout.dialog_confirm_common, null, false);
        TextView tvTitle = view.findViewById(Yu.code.toolbox.R.id.tv_confirm_title);
        TextView tvMessage = view.findViewById(Yu.code.toolbox.R.id.tv_confirm_message);
        Button btnNegative = view.findViewById(Yu.code.toolbox.R.id.btn_confirm_negative);
        Button btnPositive = view.findViewById(Yu.code.toolbox.R.id.btn_confirm_positive);
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnNegative.setVisibility(View.GONE);
        btnPositive.setText(getString(Yu.code.toolbox.R.string.cookbook_got_it));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnPositive.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

    private int findCategoryIndex(String category, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equals(category)) {
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

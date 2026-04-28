package Yu.code.toolbox.function.recorder_photo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import Yu.code.toolbox.Utils.DialogUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Yu.code.toolbox.R;

/**
 * 录屏文件列表页面
 */
public class RecordingsListActivity extends AppCompatActivity {

    private static final String TAG = "RecordingsListActivity";
    private RecyclerView recyclerView;
    private RecordingsAdapter adapter;
    private TextView tvEmpty;
    private EditText etSearch;
    private Button btnSort, btnBatchDelete;
    private List<File> recordingFiles = new ArrayList<>();
    private List<File> filteredFiles = new ArrayList<>();
    private boolean isBatchMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private int currentSortMode = 0; // 0=时间, 1=大小, 2=名称

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings_list);

        // 设置标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("录屏文件");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        loadRecordings();
        setupListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);
        etSearch = findViewById(R.id.et_search);
        btnSort = findViewById(R.id.btn_sort);
        btnBatchDelete = findViewById(R.id.btn_batch_delete);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordingsAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // 搜索功能
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFiles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 排序按钮
        btnSort.setOnClickListener(v -> showSortDialog());

        // 批量删除按钮
        btnBatchDelete.setOnClickListener(v -> toggleBatchMode());
    }

    private void filterFiles(String query) {
        filteredFiles.clear();
        if (query.isEmpty()) {
            filteredFiles.addAll(recordingFiles);
        } else {
            String lowerQuery = query.toLowerCase();
            for (File file : recordingFiles) {
                if (file.getName().toLowerCase().contains(lowerQuery)) {
                    filteredFiles.add(file);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void showSortDialog() {
        String[] options = {"按时间排序", "按大小排序", "按名称排序"};
        new AlertDialog.Builder(this)
                .setTitle("选择排序方式")
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    sortFiles();
                    dialog.dismiss();
                })
                .show();
    }

    private void sortFiles() {
        Comparator<File> comparator;
        switch (currentSortMode) {
            case 1: // 按大小
                comparator = (f1, f2) -> Long.compare(f2.length(), f1.length());
                btnSort.setText("大小↓");
                break;
            case 2: // 按名称
                comparator = Comparator.comparing(File::getName);
                btnSort.setText("名称↑");
                break;
            default: // 按时间
                comparator = (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified());
                btnSort.setText("时间↓");
                break;
        }
        
        recordingFiles.sort(comparator);
        filterFiles(etSearch.getText().toString());
    }

    private void toggleBatchMode() {
        isBatchMode = !isBatchMode;
        selectedPositions.clear();
        
        if (isBatchMode) {
            btnBatchDelete.setText("完成");
        } else {
            btnBatchDelete.setText("批量删除");
        }
        
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedFiles() {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "请选择要删除的文件", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("批量删除")
                .setMessage("确定要删除选中的 " + selectedPositions.size() + " 个文件吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    List<File> toDelete = new ArrayList<>();
                    for (int pos : selectedPositions) {
                        if (pos < filteredFiles.size()) {
                            toDelete.add(filteredFiles.get(pos));
                        }
                    }
                    
                    int deletedCount = 0;
                    for (File file : toDelete) {
                        if (file.delete()) {
                            recordingFiles.remove(file);
                            deletedCount++;
                        }
                    }
                    
                    selectedPositions.clear();
                    toggleBatchMode();
                    filterFiles(etSearch.getText().toString());
                    
                    Toast.makeText(this, "已删除 " + deletedCount + " 个文件", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadRecordings() {
        File recordingsDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (recordingsDir != null && recordingsDir.exists()) {
            File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".mp4"));

            if (files != null && files.length > 0) {
                recordingFiles.clear();
                recordingFiles.addAll(Arrays.asList(files));
                sortFiles();

                recyclerView.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyView() {
        if (filteredFiles.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(etSearch.getText().toString().isEmpty() ? "还没有录屏文件" : "没有找到匹配的文件");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * 录屏文件列表适配器
     */
    private class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recording, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = filteredFiles.get(position);
            holder.bind(file, position);
        }

        @Override
        public int getItemCount() {
            return filteredFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFileName;
            TextView tvFileInfo;
            View btnPlay;
            View btnDelete;
            CheckBox checkbox;

            ViewHolder(View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tv_file_name);
                tvFileInfo = itemView.findViewById(R.id.tv_file_info);
                btnPlay = itemView.findViewById(R.id.btn_play);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                checkbox = itemView.findViewById(R.id.checkbox);
            }

            void bind(File file, int position) {
                // 显示文件名
                tvFileName.setText(file.getName());

                // 显示文件信息（大小和时间）
                long fileSize = file.length();
                String sizeStr = formatFileSize(fileSize);
                String timeStr = DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date(file.lastModified())).toString();
                tvFileInfo.setText(sizeStr + " • " + timeStr);

                // 批量删除模式
                if (isBatchMode) {
                    checkbox.setVisibility(View.VISIBLE);
                    checkbox.setChecked(selectedPositions.contains(position));
                    btnPlay.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);
                    
                    itemView.setOnClickListener(v -> {
                        if (selectedPositions.contains(position)) {
                            selectedPositions.remove(position);
                        } else {
                            selectedPositions.add(position);
                        }
                        notifyItemChanged(position);
                    });
                } else {
                    checkbox.setVisibility(View.GONE);
                    btnPlay.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);
                    
                    // 播放按钮
                    btnPlay.setOnClickListener(v -> playVideo(file));

                    // 删除按钮
                    btnDelete.setOnClickListener(v -> showDeleteDialog(file, position));

                    // 点击整个项目也可以播放
                    itemView.setOnClickListener(v -> playVideo(file));
                    
                    // 长按重命名
                    itemView.setOnLongClickListener(v -> {
                        showRenameDialog(file, position);
                        return true;
                    });
                }
            }
        }
    }

    /**
     * 播放视频
     */
    private void playVideo(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }

        intent.setDataAndType(uri, "video/mp4");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "无法打开视频文件", e);
            Toast.makeText(this, "没有找到可以播放视频的应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(File file, int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除录屏")
                .setMessage("确定要删除 " + file.getName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (file.delete()) {
                        recordingFiles.remove(file);
                        filterFiles(etSearch.getText().toString());
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示重命名对话框
     */
    private void showRenameDialog(File file, int position) {
        String currentName = file.getName();
        if (currentName.endsWith(".mp4")) {
            currentName = currentName.substring(0, currentName.length() - 4);
        }
        
        DialogUtils.showInputDialog(this, "重命名文件", "输入新文件名（不含扩展名）", currentName, newName -> {
            if (newName.isEmpty()) {
                Toast.makeText(this, "文件名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!newName.endsWith(".mp4")) {
                newName = newName + ".mp4";
            }
            
            File newFile = new File(file.getParent(), newName);
            if (newFile.exists()) {
                Toast.makeText(this, "文件名已存在", Toast.LENGTH_SHORT).show();
            } else {
                if (file.renameTo(newFile)) {
                    int index = recordingFiles.indexOf(file);
                    if (index >= 0) {
                        recordingFiles.set(index, newFile);
                    }
                    filterFiles(etSearch.getText().toString());
                    Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isBatchMode) {
            if (!selectedPositions.isEmpty()) {
                deleteSelectedFiles();
            } else {
                toggleBatchMode();
            }
        } else {
            super.onBackPressed();
        }
    }
}


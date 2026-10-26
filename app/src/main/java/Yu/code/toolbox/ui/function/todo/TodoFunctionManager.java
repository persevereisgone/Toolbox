package Yu.code.toolbox.ui.function.todo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import Yu.code.toolbox.R;

public class TodoFunctionManager {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_UNDONE = 1;
    private static final int FILTER_DONE = 2;

    private final Context ctx;
    private final View root;
    private final TodoManager m;
    private TodoAdapter adapter;
    private int filterMode = FILTER_ALL;
    private TextView tvSummary;
    private Button chipAll;
    private Button chipUndone;
    private Button chipDone;
    private Button btnClearDone;

    public TodoFunctionManager(Context ctx, View root) {
        this.ctx = ctx;
        this.root = root;
        this.m = new TodoManager(ctx);
        RecyclerView rv = root.findViewById(R.id.rv_todo);
        rv.setLayoutManager(new LinearLayoutManager(ctx));
        adapter = new TodoAdapter();
        rv.setAdapter(adapter);
        new ItemTouchHelper(new SwipeToDelete()).attachToRecyclerView(rv);
        tvSummary = root.findViewById(R.id.tv_todo_summary);
        chipAll = root.findViewById(R.id.chip_todo_all);
        chipUndone = root.findViewById(R.id.chip_todo_undone);
        chipDone = root.findViewById(R.id.chip_todo_done);
        btnClearDone = root.findViewById(R.id.btn_todo_clear_done);
        chipAll.setOnClickListener(v -> { filterMode = FILTER_ALL; reload(); });
        chipUndone.setOnClickListener(v -> { filterMode = FILTER_UNDONE; reload(); });
        chipDone.setOnClickListener(v -> { filterMode = FILTER_DONE; reload(); });
        EditText et = root.findViewById(R.id.et_todo_input);
        root.findViewById(R.id.btn_todo_add).setOnClickListener(v -> {
            String t = et.getText().toString().trim();
            if (t.isEmpty()) { Toast.makeText(ctx, "请输入内容", Toast.LENGTH_SHORT).show(); return; }
            m.add(ctx, t);
            et.setText("");
            filterMode = FILTER_ALL;
            reload();
        });
        btnClearDone.setOnClickListener(v -> {
            List<TodoItem> removed = m.deleteDone(ctx);
            if (removed.isEmpty()) {
                Toast.makeText(ctx, "没有已完成的待办", Toast.LENGTH_SHORT).show();
                return;
            }
            reload();
            styled(com.google.android.material.snackbar.Snackbar.make(root, "已清除已完成", com.google.android.material.snackbar.Snackbar.LENGTH_LONG))
                .setAction("撤销", vv -> { m.restoreItems(ctx, removed); reload(); })
                .show();
        });
        reload();
    }

    private void reload() {
        List<TodoItem> all = m.getAll();
        int undone = 0;
        int done = 0;
        for (TodoItem i : all) if (i.isDone()) done++; else undone++;
        List<TodoItem> shown = new ArrayList<>();
        for (TodoItem i : all) {
            if (filterMode == FILTER_UNDONE && i.isDone()) continue;
            if (filterMode == FILTER_DONE && !i.isDone()) continue;
            shown.add(i);
        }
        adapter.setItems(shown);
        if (tvSummary != null) {
            tvSummary.setText(all.isEmpty() ? "暂无待办 · 去添加第一项吧" : (undone + " 项未完成 · 已完成沉底"));
        }
        if (chipAll != null) {
            chipAll.setText("全部 " + all.size());
            chipUndone.setText("未完成 " + undone);
            chipDone.setText("已完成 " + done);
            styleChip(chipAll, filterMode == FILTER_ALL);
            styleChip(chipUndone, filterMode == FILTER_UNDONE);
            styleChip(chipDone, filterMode == FILTER_DONE);
        }
        if (btnClearDone != null) {
            btnClearDone.setText("清除已完成（" + done + "）");
            btnClearDone.setEnabled(done > 0);
            btnClearDone.setAlpha(done > 0 ? 1.0f : 0.4f);
        }
        View empty = root.findViewById(R.id.layout_todo_empty);
        View rv = root.findViewById(R.id.rv_todo);
        boolean isEmpty = shown.isEmpty();
        if (empty != null) {
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            TextView emptyTitle = empty.findViewById(R.id.tv_todo_empty_title);
            TextView emptyHint = empty.findViewById(R.id.tv_todo_empty_hint);
            if (all.isEmpty()) {
                emptyTitle.setText("暂无待办");
                emptyHint.setText("在上方输入框写下第一件事吧");
            } else if (filterMode == FILTER_DONE) {
                emptyTitle.setText("还没有已完成项");
                emptyHint.setText("完成待办后会显示在这里");
            } else if (filterMode == FILTER_UNDONE) {
                emptyTitle.setText("所有待办都已完成");
                emptyHint.setText("切换“全部”查看完整清单");
            }
        }
        if (rv != null) rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void styleChip(Button chip, boolean selected) {
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ctx.getResources().getColor(selected ? R.color.app_text_primary : R.color.app_surface)));
        chip.setTextColor(ctx.getResources().getColor(
                selected ? R.color.white : R.color.app_text_secondary));
    }

    private com.google.android.material.snackbar.Snackbar styled(com.google.android.material.snackbar.Snackbar snack) {
        View v = snack.getView();
        v.setBackgroundResource(R.drawable.snackbar_rounded_bg);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        int m = (int) (12 * ctx.getResources().getDisplayMetrics().density);
        lp.setMargins(m, 0, m, m);
        v.setLayoutParams(lp);
        return snack;
    }

    private String metaOf(TodoItem it) {
        if (it.isDone()) return "已完成";
        long days = (System.currentTimeMillis() - it.getCreateAt()) / 86400000L;
        if (days <= 0) return "今天创建";
        if (days == 1) return "昨天创建";
        return days + " 天前创建";
    }

    private void showEditSheet(TodoItem it) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_todo, null, false);
        EditText et = view.findViewById(R.id.et_edit_todo);
        TextView tvError = view.findViewById(R.id.tv_edit_todo_error);
        et.setText(it.getTitle());
        et.setSelection(et.getText().length());
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(ctx);
        dialog.setContentView(view);
        view.findViewById(R.id.btn_edit_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_edit_save).setOnClickListener(v -> {
            String t = et.getText().toString().trim();
            if (t.isEmpty()) { tvError.setVisibility(View.VISIBLE); return; }
            m.updateTitle(ctx, it.getId(), t);
            dialog.dismiss();
            reload();
        });
        dialog.show();
        et.post(() -> {
            et.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(et, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private class SwipeToDelete extends ItemTouchHelper.SimpleCallback {
        SwipeToDelete() { super(0, ItemTouchHelper.LEFT); }

        @Override
        public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
            int pos = vh.getAdapterPosition();
            TodoItem removed = adapter.removeAt(pos);
            if (removed == null) { reload(); return; }
            m.deleteItem(ctx, removed.getId());
            reload();
            styled(com.google.android.material.snackbar.Snackbar.make(root,
                            "已删除“" + removed.getTitle() + "”",
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG))
                    .setAction("撤销", vv -> { m.restoreSingle(ctx, removed); reload(); })
                    .show();
        }

        @Override
        public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView rv,
                                @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                int actionState, boolean isActive) {
            View foreground = vh.itemView.findViewById(R.id.view_foreground);
            if (foreground != null) {
                getDefaultUIUtil().onDraw(c, rv, foreground, dX, dY, actionState, isActive);
            } else {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
            View foreground = vh.itemView.findViewById(R.id.view_foreground);
            if (foreground != null) getDefaultUIUtil().clearView(foreground);
            else super.clearView(rv, vh);
        }
    }

    class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.H> {
        List<TodoItem> items = new ArrayList<>();
        void setItems(List<TodoItem> l) { items = l; notifyDataSetChanged(); }

        TodoItem removeAt(int pos) {
            if (pos < 0 || pos >= items.size()) return null;
            return items.remove(pos);
        }

        @Override public H onCreateViewHolder(ViewGroup p, int t) {
            return new H(LayoutInflater.from(ctx).inflate(R.layout.item_todo, p, false));
        }

        @Override public void onBindViewHolder(H h, int pos) {
            TodoItem it = items.get(pos);
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(it.isDone());
            h.cb.setText(it.isDone() ? "\u2713" : "");
            h.title.setText(it.getTitle());
            h.title.setPaintFlags(it.isDone()
                    ? (h.title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)
                    : (h.title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            h.title.setTextColor(ctx.getResources().getColor(
                    it.isDone() ? R.color.app_text_tertiary : R.color.app_text_primary));
            h.meta.setText(metaOf(it));
            h.meta.setVisibility(View.VISIBLE);
            h.cb.setContentDescription((it.isDone() ? "标为未完成:" : "标为完成:") + it.getTitle());
            h.btnEdit.setOnClickListener(v -> showEditSheet(it));
            h.cb.setOnCheckedChangeListener((b, c) -> { m.toggleDone(ctx, it.getId()); reload(); });
            h.hit.setOnClickListener(v -> h.cb.performClick());
            int stagger = Math.min(pos, 5) * ctx.getResources().getInteger(R.integer.motion_stagger);
            Animation enter = AnimationUtils.loadAnimation(ctx, R.anim.list_item_enter);
            enter.setStartOffset(stagger);
            h.itemView.startAnimation(enter);
        }

        @Override public int getItemCount() { return items.size(); }

        class H extends RecyclerView.ViewHolder {
            CheckBox cb;
            View hit;
            TextView title;
            TextView meta;
            Button btnEdit;
            H(View v) {
                super(v);
                cb = v.findViewById(R.id.cb_todo);
            hit = v.findViewById(R.id.cb_hit);
                title = v.findViewById(R.id.tv_todo_title);
                meta = v.findViewById(R.id.tv_todo_meta);
                btnEdit = v.findViewById(R.id.btn_todo_edit);
            }
        }
    }
}

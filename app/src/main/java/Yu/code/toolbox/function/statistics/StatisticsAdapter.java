package Yu.code.toolbox.function.statistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    private List<StatisticsItem> items;
    private OnItemClickListener listener;
    private View emptyView;
    private int maxVisible = Integer.MAX_VALUE;
    private boolean multiSelect;
    private final java.util.Set<Integer> selected = new java.util.HashSet<>();
    /** 显示下标映射:null=全显;搜索/排序时只显示映射下标(存的是数据下标) */
    private List<Integer> visibleIndex;

    public interface OnItemClickListener {
        void onIncreaseClick(int position);
        void onDecreaseClick(int position);
        void onDeleteClick(int position);
        void onQuantityClick(int position);
        void onSelectionChanged(int selectedCount, boolean inMultiSelect);
    }

    public StatisticsAdapter() {
        this.items = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** 空状态 view:有数据隐藏,无数据(初次)显示；搜索无匹配由 manager 另行文案 */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        refreshEmpty();
    }

    private void refreshEmpty() {
        updateEmptyView(items == null || items.isEmpty(),
                "这个组还没有物品", "点右下 ＋ 新增第一件",
                "没有匹配的物品", "换个关键词试试");
    }

    /**
     * 空态统一出口：全空→新增引导；有数据但搜不到→换关键词引导
     */
    public void updateEmptyView(boolean allEmpty, String emptyTitle, String emptySub,
                                String noMatchTitle, String noMatchSub) {
        if (emptyView == null) {
            return;
        }
        boolean filterEmpty = !allEmpty && getItemCount() == 0;
        emptyView.setVisibility(allEmpty || filterEmpty ? View.VISIBLE : View.GONE);
        android.view.View title = emptyView.findViewById(R.id.tv_stats_empty_title);
        android.view.View sub = emptyView.findViewById(R.id.tv_stats_empty_sub);
        String t = filterEmpty ? noMatchTitle : emptyTitle;
        String s = filterEmpty ? noMatchSub : emptySub;
        if (title instanceof android.widget.TextView && t != null) {
            ((android.widget.TextView) title).setText(t);
        }
        if (sub instanceof android.widget.TextView && s != null) {
            ((android.widget.TextView) sub).setText(s);
        }
    }

    /** 新增高亮：数据下标，-1=无 */
    private int highlightPos = -1;

    /** 新增成功后调：该行描边高亮一闪（约1秒后自动熄灭） */
    public void flashPosition(int dataPos) {
        highlightPos = dataPos;
        int display = displayPositionOf(dataPos);
        if (display >= 0) {
            notifyItemChanged(display);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                highlightPos = -1;
                int d = displayPositionOf(dataPos);
                if (d >= 0) {
                    notifyItemChanged(d);
                }
            }, 900);
        }
    }

    /** 显示映射:回调收到的 position 永远是数据下标,调用方无需转换 */
    public void setVisibleIndex(List<Integer> index) {
        List<Integer> next = (index == null) ? null : new ArrayList<>(index);
        // 映射没变就不刷,避免整列重绑闪一下
        if (next == null ? visibleIndex == null : next.equals(visibleIndex)) {
            return;
        }
        this.visibleIndex = next;
        notifyDataSetChanged();
    }

    /** 当前是否有搜索/排序过滤(无则全显,加减数量时连映射都不用动) */
    public boolean hasFilter() {
        return visibleIndex != null;
    }

    /** 当前显示映射(数据下标顺序),供保序刷新用 */
    public List<Integer> getVisibleIndex() {
        if (visibleIndex != null) {
            return new ArrayList<>(visibleIndex);
        }
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            all.add(i);
        }
        return all;
    }

    /** 数据下标 → 显示下标(搜索过滤后可能不在末尾),找不到返回 -1 */
    public int displayPositionOf(int dataPos) {
        if (visibleIndex != null) {
            return visibleIndex.indexOf(dataPos);
        }
        return (dataPos >= 0 && dataPos < items.size()) ? dataPos : -1;
    }

    private int toDataPos(int displayPos) {
        if (visibleIndex != null) {
            return (displayPos >= 0 && displayPos < visibleIndex.size())
                    ? visibleIndex.get(displayPos) : RecyclerView.NO_POSITION;
        }
        return displayPos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_statistics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int dataPos = toDataPos(position);
        if (dataPos == RecyclerView.NO_POSITION || dataPos >= items.size()) {
            return;
        }
        StatisticsItem item = items.get(dataPos);
        holder.tvItemName.setText(item.getName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // 新增高亮一闪：描边强调，其他行必须复原（复用会串色）
        holder.itemView.setBackgroundResource(dataPos == highlightPos
                ? R.drawable.item_statistics_highlight_bg : R.drawable.app_card_bg);

        holder.btnIncrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseClick(dataPos);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseClick(dataPos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(dataPos);
            }
        });

        // 多选态:复选框显示,步进器隐藏,点行切换,长按进入
        boolean checked = multiSelect && selected.contains(dataPos);
        holder.ivCheck.setVisibility(multiSelect ? View.VISIBLE : View.GONE);
        holder.ivCheck.setImageResource(checked
                ? R.drawable.ic_check_on_24 : R.drawable.ic_check_off_24);
        int steppers = multiSelect ? View.GONE : View.VISIBLE;
        holder.btnIncrease.setVisibility(steppers);
        holder.btnDecrease.setVisibility(steppers);
        holder.btnDelete.setVisibility(steppers);
        holder.itemView.setOnClickListener(v -> {
            if (multiSelect) {
                int p = holder.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) toggleSelection(toDataPos(p));
            }
        });
        // 长按进入:行内 −/＋/×/数量都是可点击子 view,会吃掉触摸,
        // 长按监听必须挂到它们身上,否则按在按钮上永远到不了 itemView
        View.OnLongClickListener enterListener = v -> {
            int p = holder.getBindingAdapterPosition();
            int data = (p == RecyclerView.NO_POSITION) ? RecyclerView.NO_POSITION : toDataPos(p);
            if (data != RecyclerView.NO_POSITION && !multiSelect) {
                enterMultiSelect(data);
                return true;
            }
            return false;
        };
        holder.itemView.setOnLongClickListener(enterListener);
        holder.btnIncrease.setOnLongClickListener(enterListener);
        holder.btnDecrease.setOnLongClickListener(enterListener);
        holder.btnDelete.setOnLongClickListener(enterListener);
        holder.tvQuantity.setOnLongClickListener(enterListener);

        holder.tvQuantity.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityClick(dataPos);
            }
        });
        // 注:视觉 40dp 是用户为省空间明确定的,触摸按 48dp 验收项记例外(见落地文档)
        // 进入动画只放一次(tag 标记),重绑/多选切换不再重播,避免整列闪烁
        if (holder.itemView.getTag() == null) {
            holder.itemView.setTag("entered");
            int stagger = Math.min(Math.max(0, position), 5)
                    * holder.itemView.getResources().getInteger(R.integer.motion_stagger);
            Animation enter = AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.list_item_enter);
            enter.setStartOffset(stagger);
            holder.itemView.startAnimation(enter);
        }
    }

    @Override
    public int getItemCount() {
        int count = (visibleIndex != null) ? visibleIndex.size() : items.size();
        return Math.min(count, maxVisible);
    }

    /** 主页只展示前 N 个,其余走全屏列表 */
    public void setMaxVisible(int max) {
        this.maxVisible = max <= 0 ? Integer.MAX_VALUE : max;
        notifyDataSetChanged();
        refreshEmpty();
    }

    public void addItem(StatisticsItem item) {
        items.add(item);
        // 显示位由随后的 refreshFilteredList 重算,这里整刷保底
        notifyDataSetChanged();
        refreshEmpty();
    }

    /** 批量删：调用方保证倒序；一次清数据，不发局部通知，由随后的过滤刷新统一重绑 */
    public void removeItemsDescending(List<Integer> dataPositionsDesc) {
        for (int p : dataPositionsDesc) {
            if (p >= 0 && p < items.size()) {
                items.remove(p);
            }
        }
        refreshEmpty();
    }

    /** position 永远是数据下标,内部换算显示位 */
    public void updateItem(int position, StatisticsItem item) {
        if (position >= 0 && position < items.size()) {
            items.set(position, item);
            int display = displayPositionOf(position);
            if (display >= 0) {
                notifyItemChanged(display);
            } else {
                notifyDataSetChanged();
            }
        }
    }

    /** position 永远是数据下标,内部换算显示位 */
    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            int display = displayPositionOf(position);
            items.remove(position);
            if (display >= 0) {
                notifyItemRemoved(display);
            } else {
                notifyDataSetChanged();
            }
            refreshEmpty();
        }
    }

    public List<StatisticsItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<StatisticsItem> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
        refreshEmpty();
    }

    /** 长按触发:首选入口,进入多选并选中当前行 */
    public void enterMultiSelect(int position) {
        if (!multiSelect) {
            multiSelect = true;
        }
        if (position >= 0 && position < items.size()) {
            selected.add(position);
        }
        notifyDataSetChanged();
        notifySelection();
    }

    public void toggleSelection(int position) {
        if (!multiSelect) return;
        if (selected.contains(position)) {
            selected.remove(position);
        } else {
            selected.add(position);
        }
        if (selected.isEmpty()) {
            exitMultiSelect();
            return;
        }
        // 只刷当前行(数据下标→显示下标),不整列重绑,避免闪烁
        int display = displayPositionOf(position);
        if (display >= 0) {
            notifyItemChanged(display);
        }
        notifySelection();
    }

    public void exitMultiSelect() {
        multiSelect = false;
        selected.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public java.util.List<Integer> getSelectedPositions() {
        return new java.util.ArrayList<>(selected);
    }

    private void notifySelection() {
        if (listener != null) {
            listener.onSelectionChanged(selected.size(), multiSelect);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView ivCheck;
        TextView tvItemName;
        TextView tvQuantity;
        ImageButton btnIncrease;
        ImageButton btnDecrease;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}



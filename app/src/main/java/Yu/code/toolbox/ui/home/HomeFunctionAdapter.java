package Yu.code.toolbox.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;

public class HomeFunctionAdapter extends RecyclerView.Adapter<HomeFunctionAdapter.ViewHolder> {

    public static class Item {
        public final int iconRes;
        public final String name;
        public final String status;
        public final int functionId;
        public final int iconBgColor;
        public final int iconTint;

        public Item(int iconRes, String name, String status, int functionId, int iconBgColor, int iconTint) {
            this.iconRes = iconRes;
            this.name = name;
            this.status = status;
            this.functionId = functionId;
            this.iconBgColor = iconBgColor;
            this.iconTint = iconTint;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public interface OnPinClickListener {
        void onPinClick(Item item);
    }

    private final Context context;
    private List<Item> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnPinClickListener pinListener;
    private java.util.Set<String> pinnedIds = new java.util.HashSet<>();

    public HomeFunctionAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void setOnPinClickListener(OnPinClickListener l) {
        this.pinListener = l;
    }

    public void setPinnedIds(java.util.Set<String> pinned) {
        this.pinnedIds = pinned == null ? new java.util.HashSet<>() : pinned;
    }

    public void setItems(List<Item> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_home_function, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.ivIcon.setImageResource(item.iconRes);
        holder.ivIcon.setColorFilter(context.getResources().getColor(item.iconTint));
        holder.iconBox.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(context.getResources().getColor(item.iconBgColor)));
        holder.tvName.setText(item.name);
        holder.tvStatus.setText(item.status);
        holder.tvStatus.setVisibility(item.status == null || item.status.isEmpty() ? View.GONE : View.VISIBLE);
        boolean isPinned = pinnedIds.contains(String.valueOf(item.functionId));
        holder.itemView.setContentDescription(item.name + (item.status == null ? "" : "，" + item.status)
                + (isPinned ? "，已置顶" : ""));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
        holder.btnPin.setContentDescription(isPinned ? ("取消置顶" + item.name) : ("置顶" + item.name));
        holder.btnPin.setColorFilter(context.getResources().getColor(
                isPinned ? R.color.app_primary : R.color.app_text_tertiary));
        holder.btnPin.setAlpha(isPinned ? 1.0f : 0.45f);
        holder.btnPin.setOnClickListener(v -> {
            if (pinListener != null) pinListener.onPinClick(item);
        });
        int stagger = Math.min(position, 5) * context.getResources().getInteger(R.integer.motion_stagger);
        Animation enter = AnimationUtils.loadAnimation(context, R.anim.list_item_enter);
        enter.setStartOffset(stagger);
        holder.itemView.startAnimation(enter);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconBox;
        ImageView ivIcon;
        TextView tvName;
        TextView tvStatus;
        ImageView btnPin;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // 图标底
            iconBox = itemView.findViewById(R.id.home_icon_box);
            ivIcon = itemView.findViewById(R.id.iv_home_icon);
            tvName = itemView.findViewById(R.id.tv_home_name);
            tvStatus = itemView.findViewById(R.id.tv_home_status);
            btnPin = itemView.findViewById(R.id.btn_pin);
        }
    }
}

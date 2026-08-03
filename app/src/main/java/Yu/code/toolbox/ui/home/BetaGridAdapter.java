package Yu.code.toolbox.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import Yu.code.toolbox.R;

/**
 * Beta 功能区网格适配器 - 用于 RecyclerView 显示测试区功能项
 */
public class BetaGridAdapter extends RecyclerView.Adapter<BetaGridAdapter.ViewHolder> {

    private final Context context;
    private final List<Item> items;

    public static class Item {
        public int iconRes;
        public String name;

        public Item(int iconRes, String name) {
            this.iconRes = iconRes;
            this.name = name;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivIcon;
        public TextView tvName;

        public ViewHolder(View view) {
            super(view);
            ivIcon = view.findViewById(R.id.iv_icon);
            tvName = view.findViewById(R.id.tv_name);
        }
    }

    public BetaGridAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
    }

    // 点击监听器接口
    public interface OnItemClickListener {
        void onItemClick(int position, Item item);
    }

    private OnItemClickListener clickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.beta_item_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.ivIcon.setImageResource(item.iconRes);
        holder.tvName.setText(item.name);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
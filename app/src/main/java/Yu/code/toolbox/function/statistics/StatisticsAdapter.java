package Yu.code.toolbox.function.statistics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    private List<StatisticsItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onIncreaseClick(int position);
        void onDecreaseClick(int position);
        void onDeleteClick(int position);
        void onQuantityClick(int position);
    }

    public StatisticsAdapter() {
        this.items = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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
        StatisticsItem item = items.get(position);
        holder.tvItemName.setText(item.getName());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        holder.btnIncrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseClick(position);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseClick(position);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(position);
            }
        });

        holder.tvQuantity.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(StatisticsItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void updateItem(int position, StatisticsItem item) {
        if (position >= 0 && position < items.size()) {
            items.set(position, item);
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<StatisticsItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<StatisticsItem> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        TextView tvQuantity;
        Button btnIncrease;
        Button btnDecrease;
        Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}



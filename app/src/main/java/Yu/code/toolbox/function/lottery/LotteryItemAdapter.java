package Yu.code.toolbox.function.lottery;

import android.content.Context;
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

/**
 * 抽奖项目适配器
 */
public class LotteryItemAdapter extends RecyclerView.Adapter<LotteryItemAdapter.ViewHolder> {

    private Context context;
    private List<LotteryItem> items;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onEditItem(int position, LotteryItem item);
        void onDeleteItem(int position, LotteryItem item);
    }

    public LotteryItemAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<LotteryItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<LotteryItem> getItems() {
        return new ArrayList<>(items);
    }

    public void addItem(LotteryItem item) {
        if (item != null) {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lottery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LotteryItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvWeight;
        private Button btnEdit;
        private Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvWeight = itemView.findViewById(R.id.tv_item_weight);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(LotteryItem item, int position) {
            tvName.setText(item.getName());
            tvWeight.setText("权重: " + item.getWeight());

            // 设置权重颜色
            if (item.getWeight() <= 1) {
                tvWeight.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else if (item.getWeight() <= 3) {
                tvWeight.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvWeight.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditItem(position, item);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteItem(position, item);
                }
            });
        }
    }
}

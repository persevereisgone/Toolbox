package Yu.code.toolbox.ui.function.cookbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PurchaseListAdapter extends RecyclerView.Adapter<PurchaseListAdapter.PurchaseViewHolder> {

    public interface OnPurchaseItemCheckedListener {
        void onPurchaseCheckedChanged(String name, boolean checked);
    }

    private List<PurchaseItem> items = new ArrayList<>();
    private final OnPurchaseItemCheckedListener listener;

    public PurchaseListAdapter(OnPurchaseItemCheckedListener listener) {
        this.listener = listener;
    }

    public void setData(List<PurchaseItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    public List<PurchaseItem> getData() {
        return items;
    }

    @NonNull
    @Override
    public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(Yu.code.toolbox.R.layout.item_purchase_list, parent, false);
        return new PurchaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
        PurchaseItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvQty.setText(String.valueOf(item.quantity));
        holder.checkPurchased.setText("已买");
        holder.checkPurchased.setOnCheckedChangeListener(null);
        holder.checkPurchased.setChecked(item.purchased);
        holder.itemView.setAlpha(item.purchased ? 0.58f : 1f);
        holder.tvName.setPaintFlags(item.purchased ? holder.tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG : holder.tvName.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        holder.tvQty.setPaintFlags(item.purchased ? holder.tvQty.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG : holder.tvQty.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        holder.checkPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            PurchaseItem current = items.get(pos);
            if (current != null) {
                current.purchased = isChecked;
                if (listener != null) {
                    listener.onPurchaseCheckedChanged(current.name, isChecked);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvQty;
        CheckBox checkPurchased;

        PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_purchase_item);
            tvQty = itemView.findViewById(Yu.code.toolbox.R.id.tv_purchase_qty);
            checkPurchased = itemView.findViewById(Yu.code.toolbox.R.id.cb_purchase_done);
        }
    }

    public static class PurchaseItem {
        public final String name;
        public final int quantity;
        public boolean purchased;

        public PurchaseItem(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }
}

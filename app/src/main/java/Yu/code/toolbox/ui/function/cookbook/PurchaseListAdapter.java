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
        holder.tvQty.setText(holder.itemView.getContext().getString(
                Yu.code.toolbox.R.string.cookbook_purchase_recipe_count, item.recipeCount));
        holder.checkPurchased.setOnCheckedChangeListener(null);
        holder.checkPurchased.setChecked(item.purchased);
        applyPurchasedStyle(holder, item.purchased);
        holder.checkPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            PurchaseItem current = items.get(pos);
            if (current != null) {
                current.purchased = isChecked;
                applyPurchasedStyle(holder, isChecked);
                if (listener != null) {
                    listener.onPurchaseCheckedChanged(current.name, isChecked);
                }
            }
        });
    }

    private void applyPurchasedStyle(PurchaseViewHolder holder, boolean purchased) {
        holder.itemView.setAlpha(purchased ? 0.72f : 1f);
        holder.strikeLine.setVisibility(purchased ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class PurchaseViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvQty;
        CheckBox checkPurchased;
        View strikeLine;

        PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_purchase_item);
            tvQty = itemView.findViewById(Yu.code.toolbox.R.id.tv_purchase_qty);
            checkPurchased = itemView.findViewById(Yu.code.toolbox.R.id.cb_purchase_done);
            strikeLine = itemView.findViewById(Yu.code.toolbox.R.id.view_purchased_strike);
        }
    }

    public static class PurchaseItem {
        public final String name;
        public final int recipeCount;
        public boolean purchased;

        public PurchaseItem(String name, int recipeCount) {
            this.name = name;
            this.recipeCount = recipeCount;
        }
    }
}

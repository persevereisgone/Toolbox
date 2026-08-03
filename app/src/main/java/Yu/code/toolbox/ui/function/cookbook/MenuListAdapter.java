package Yu.code.toolbox.ui.function.cookbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MenuListAdapter extends RecyclerView.Adapter<MenuListAdapter.MenuViewHolder> {

    public interface OnMenuItemActionListener {
        void onRemoveFromMenu(int cookbookId);
        void onItemClick(CookItem item);
    }

    private List<CookItem> menuItems = new ArrayList<>();
    private final OnMenuItemActionListener listener;

    public MenuListAdapter(OnMenuItemActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<CookItem> items) {
        this.menuItems = items == null ? new ArrayList<>() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                Yu.code.toolbox.R.layout.item_menu_list, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        CookItem item = menuItems.get(position);
        if (item == null) {
            return;
        }
        holder.ivIcon.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        CookbookImageLoader.load(holder.itemView.getContext(), item.getImageUri(), holder.ivIcon, Yu.code.toolbox.R.drawable.recipe);
        holder.tvName.setText(item.getName());
        holder.tvCategory.setText(item.getCategory());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || listener == null) {
                return;
            }
            CookItem current = menuItems.get(pos);
            if (current != null) {
                listener.onRemoveFromMenu(current.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems == null ? 0 : menuItems.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvCategory;
        Button btnRemove;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(Yu.code.toolbox.R.id.iv_menu_icon);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_menu_name);
            tvCategory = itemView.findViewById(Yu.code.toolbox.R.id.tv_menu_category);
            btnRemove = itemView.findViewById(Yu.code.toolbox.R.id.btn_remove_menu);
        }
    }
}

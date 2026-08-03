package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
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

public class CookBookListAdapter extends RecyclerView.Adapter<CookBookListAdapter.ViewHolder> {

    public interface OnCookBookItemClickListener {
        void onItemClickListener(CookItem item);
    }

    public interface OnAddToMenuListener {
        void onAddToMenu(CookItem item);
    }

    public interface OnDeleteCookbookListener {
        void onDeleteCookbook(CookItem item);
    }

    private final Context context;
    private List<CookItem> cookbooks = new ArrayList<>();
    private final OnCookBookItemClickListener itemClickListener;
    private final OnAddToMenuListener addToMenuListener;
    private final OnDeleteCookbookListener deleteCookbookListener;

    public CookBookListAdapter(Context context, OnCookBookItemClickListener itemClickListener,
                               OnAddToMenuListener addToMenuListener, OnDeleteCookbookListener deleteCookbookListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.addToMenuListener = addToMenuListener;
        this.deleteCookbookListener = deleteCookbookListener;
    }

    public void setData(List<CookItem> cookbooks) {
        this.cookbooks = cookbooks == null ? new ArrayList<>() : cookbooks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(Yu.code.toolbox.R.layout.item_cookbook_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookItem item = cookbooks.get(position);
        if (item == null) {
            return;
        }
        holder.ivIcon.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        CookbookImageLoader.load(context, item.getImageUri(), holder.ivIcon, Yu.code.toolbox.R.drawable.recipe);
        holder.tvName.setText(item.getName());
        holder.tvCategory.setText(item.getCategory());
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClickListener(item);
            }
        });
        holder.btnAddToMenu.setOnClickListener(v -> {
            if (addToMenuListener != null) {
                addToMenuListener.onAddToMenu(item);
            }
        });
        holder.btnDeleteCookbook.setOnClickListener(v -> {
            if (deleteCookbookListener != null) {
                deleteCookbookListener.onDeleteCookbook(item);
            }
        });
        holder.btnAddToMenu.setFocusable(false);
        holder.btnAddToMenu.setClickable(true);
        holder.btnDeleteCookbook.setFocusable(false);
        holder.btnDeleteCookbook.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return cookbooks == null ? 0 : cookbooks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvCategory;
        Button btnAddToMenu;
        Button btnDeleteCookbook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(Yu.code.toolbox.R.id.iv_cookbook_icon);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_cookbook_name);
            tvCategory = itemView.findViewById(Yu.code.toolbox.R.id.tv_category);
            btnAddToMenu = itemView.findViewById(Yu.code.toolbox.R.id.btn_add_to_menu);
            btnDeleteCookbook = itemView.findViewById(Yu.code.toolbox.R.id.btn_delete_cookbook);
        }
    }
}

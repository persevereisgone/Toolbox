package Yu.code.toolbox.ui.function.cookbook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CategoryEditAdapter extends RecyclerView.Adapter<CategoryEditAdapter.ViewHolder> {

    public interface Listener {
        void onRename(String category);
        void onDelete(String category);
    }

    private final List<String> categories = new ArrayList<>();
    private final Listener listener;

    public CategoryEditAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<String> data) {
        categories.clear();
        if (data != null) {
            categories.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(Yu.code.toolbox.R.layout.item_edit_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvName.setText(category);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRename(category);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final Button btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_category_name);
            btnDelete = itemView.findViewById(Yu.code.toolbox.R.id.btn_delete_category);
        }
    }
}

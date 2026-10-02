package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
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

    public interface OnFavoriteListener {
        void onToggleFavorite(CookItem item);
    }

    private final Context context;
    private List<CookItem> cookbooks = new ArrayList<>();
    private final OnCookBookItemClickListener itemClickListener;
    private final OnAddToMenuListener addToMenuListener;
    private final OnDeleteCookbookListener deleteCookbookListener;
    private final OnFavoriteListener favoriteListener;

    public CookBookListAdapter(Context context, OnCookBookItemClickListener itemClickListener,
                               OnAddToMenuListener addToMenuListener, OnDeleteCookbookListener deleteCookbookListener,
                               OnFavoriteListener favoriteListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.addToMenuListener = addToMenuListener;
        this.deleteCookbookListener = deleteCookbookListener;
        this.favoriteListener = favoriteListener;
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
        boolean hasCustomImage = item.getImageUri() != null && !item.getImageUri().isEmpty();
        holder.ivIcon.setScaleType(hasCustomImage ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        holder.ivIcon.setPadding(
                hasCustomImage ? 0 : context.getResources().getDimensionPixelSize(Yu.code.toolbox.R.dimen.space_xs),
                hasCustomImage ? 0 : context.getResources().getDimensionPixelSize(Yu.code.toolbox.R.dimen.space_xs),
                hasCustomImage ? 0 : context.getResources().getDimensionPixelSize(Yu.code.toolbox.R.dimen.space_xs),
                hasCustomImage ? 0 : context.getResources().getDimensionPixelSize(Yu.code.toolbox.R.dimen.space_xs)
        );
        holder.ivIcon.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        CookbookImageLoader.load(context, item.getImageUri(), holder.ivIcon, Yu.code.toolbox.R.drawable.recipe);
        holder.tvName.setText(item.getName());
        holder.tvCategory.setText(item.getCategory());
        // P0/P3: 收藏用图标 + tint 区分状态,不用文字符号
        holder.btnFavorite.setImageResource(Yu.code.toolbox.R.drawable.ic_favorite_24);
        holder.btnFavorite.setColorFilter(item.isFavorite()
                ? context.getResources().getColor(Yu.code.toolbox.R.color.app_primary)
                : context.getResources().getColor(Yu.code.toolbox.R.color.app_nav_unchecked));
        holder.btnFavorite.setContentDescription(item.isFavorite()
                ? context.getString(Yu.code.toolbox.R.string.cookbook_unfavorite)
                : context.getString(Yu.code.toolbox.R.string.cookbook_favorite));
        // P3: 列表 stagger 进入(30ms 递增,封顶 5 项),仅 alpha + 位移
        int stagger = Math.min(position, 5) * context.getResources().getInteger(Yu.code.toolbox.R.integer.motion_stagger);
        Animation enter = AnimationUtils.loadAnimation(context, Yu.code.toolbox.R.anim.list_item_enter);
        enter.setStartOffset(stagger);
        holder.itemView.startAnimation(enter);
        // P3: 按压缩放 0.98,仅 transform
        holder.itemView.setOnTouchListener((v, e) -> {
            int d = context.getResources().getInteger(Yu.code.toolbox.R.integer.motion_press_scale_duration);
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(d).start();
            } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(d).start();
            }
            return false;
        });
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClickListener(item);
            }
        });
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onToggleFavorite(item);
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
        holder.btnFavorite.setFocusable(false);
        holder.btnFavorite.setClickable(true);
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
        ImageButton btnFavorite;
        ImageButton btnAddToMenu;
        ImageButton btnDeleteCookbook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(Yu.code.toolbox.R.id.iv_cookbook_icon);
            tvName = itemView.findViewById(Yu.code.toolbox.R.id.tv_cookbook_name);
            tvCategory = itemView.findViewById(Yu.code.toolbox.R.id.tv_category);
            btnFavorite = itemView.findViewById(Yu.code.toolbox.R.id.btn_favorite_cookbook);
            btnAddToMenu = itemView.findViewById(Yu.code.toolbox.R.id.btn_add_to_menu);
            btnDeleteCookbook = itemView.findViewById(Yu.code.toolbox.R.id.btn_delete_cookbook);
        }
    }
}

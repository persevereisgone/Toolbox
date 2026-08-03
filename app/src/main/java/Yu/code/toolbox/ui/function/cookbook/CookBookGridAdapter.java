package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CookBookGridAdapter extends BaseAdapter {
    private final Context context;
    private List<CookItem> cookbooks;

    public CookBookGridAdapter(Context context, List<CookItem> cookbooks) {
        this.context = context;
        this.cookbooks = cookbooks == null ? new ArrayList<>() : cookbooks;
    }

    @Override
    public int getCount() {
        return cookbooks == null ? 0 : cookbooks.size();
    }

    @Override
    public Object getItem(int position) {
        return cookbooks.get(position);
    }

    @Override
    public long getItemId(int position) {
        CookItem item = cookbooks.get(position);
        return item == null ? position : item.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    Yu.code.toolbox.R.layout.item_cookbook_grid, parent, false);
        }

        CookItem item = cookbooks.get(position);
        if (item == null) {
            return convertView;
        }

        ImageView ivIcon = convertView.findViewById(Yu.code.toolbox.R.id.iv_cookbook_icon);
        TextView tvName = convertView.findViewById(Yu.code.toolbox.R.id.tv_cookbook_name);
        TextView tvCategory = convertView.findViewById(Yu.code.toolbox.R.id.tv_category);

        ivIcon.setImageResource(Yu.code.toolbox.R.drawable.recipe);
        tvName.setText(item.getName());
        tvCategory.setText(item.getCategory());

        return convertView;
    }

    public void setData(List<CookItem> cookbooks) {
        this.cookbooks = cookbooks == null ? new ArrayList<>() : cookbooks;
        notifyDataSetChanged();
    }

    public List<CookItem> getCookbooks() {
        return cookbooks;
    }
}

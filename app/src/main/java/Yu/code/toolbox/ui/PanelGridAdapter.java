package Yu.code.toolbox.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import Yu.code.toolbox.R;

public class PanelGridAdapter extends BaseAdapter {

    private Context mContext;
    private List<PanelItem> mItems;

    public PanelGridAdapter(Context context, List<PanelItem> items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.panel_item, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.item_icon);
            holder.name = convertView.findViewById(R.id.item_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PanelItem item = mItems.get(position);
        holder.icon.setImageResource(item.getIconRes());
        holder.name.setText(item.getName());

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
    }

    public static class PanelItem {
        private int mIconRes;
        private String mName;

        public PanelItem(int iconRes, String name) {
            mIconRes = iconRes;
            mName = name;
        }

        public int getIconRes() {
            return mIconRes;
        }

        public String getName() {
            return mName;
        }
    }
}
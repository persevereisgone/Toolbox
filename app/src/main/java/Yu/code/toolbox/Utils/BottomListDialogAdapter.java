package Yu.code.toolbox.Utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;

public class BottomListDialogAdapter extends RecyclerView.Adapter<BottomListDialogAdapter.OptionViewHolder> {
    public interface OnOptionClickListener {
        void onOptionClick(String value);
    }

    private final LayoutInflater inflater;
    private final OnOptionClickListener listener;
    private final List<String> items = new ArrayList<>();
    private String selectedValue;

    public BottomListDialogAdapter(Context context, OnOptionClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setData(List<String> data, String selectedValue) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        this.selectedValue = selectedValue;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_bottom_list_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String value = items.get(position);
        holder.tvText.setText(value);
        holder.tvMark.setVisibility(value != null && value.equals(selectedValue) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionClick(value);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OptionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvText;
        final TextView tvMark;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_option_text);
            tvMark = itemView.findViewById(R.id.tv_option_mark);
        }
    }
}
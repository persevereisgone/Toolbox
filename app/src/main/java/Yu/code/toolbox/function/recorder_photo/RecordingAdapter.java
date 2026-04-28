package Yu.code.toolbox.function.recorder_photo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import Yu.code.toolbox.R;

/**
 * 录音文件列表适配器
 */
public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    private List<RecordingItem> recordings = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPlayPauseClick(int position);
        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordingItem item = recordings.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public void setRecordings(List<RecordingItem> recordings) {
        this.recordings = recordings;
        notifyDataSetChanged();
    }

    public List<RecordingItem> getRecordings() {
        return recordings;
    }

    public void updateItem(int position, RecordingItem item) {
        if (position >= 0 && position < recordings.size()) {
            recordings.set(position, item);
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < recordings.size()) {
            recordings.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void setPlayingItem(int position) {
        // 重置所有项的播放状态
        for (int i = 0; i < recordings.size(); i++) {
            recordings.get(i).setPlaying(i == position);
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageButton btnPlayPause;
        private TextView tvFileName;
        private TextView tvFileInfo;
        private ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnPlayPause = itemView.findViewById(R.id.btn_play);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileInfo = itemView.findViewById(R.id.tv_file_info);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(RecordingItem item, int position) {
            tvFileName.setText(item.getFileName());
            tvFileInfo.setText(item.getDisplayInfo());

            // 设置播放/暂停按钮图标
            if (item.isPlaying()) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }

            // 设置点击事件
            btnPlayPause.setOnClickListener(v -> {
                if (listener != null) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        listener.onPlayPauseClick(currentPosition);
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(currentPosition);
                    }
                }
            });
        }
    }
}

package Yu.code.toolbox.function.recorder_photo;

import java.io.File;

/**
 * 录音中的照片项
 */
public class RecordingPhotoItem {
    private long timestamp; // 照片在录音中的时间戳（毫秒）
    private String photoPath; // 照片文件路径
    private File photoFile; // 照片文件对象
    private String description; // 照片描述（可选）

    public RecordingPhotoItem(long timestamp, String photoPath) {
        this.timestamp = timestamp;
        this.photoPath = photoPath;
        this.photoFile = new File(photoPath);
    }

    public RecordingPhotoItem(long timestamp, String photoPath, String description) {
        this.timestamp = timestamp;
        this.photoPath = photoPath;
        this.photoFile = new File(photoPath);
        this.description = description;
    }

    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
        this.photoFile = new File(photoPath);
    }

    public File getPhotoFile() {
        return photoFile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取格式化的时间戳字符串
     */
    public String getFormattedTimestamp() {
        long seconds = timestamp / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 检查照片文件是否存在
     */
    public boolean isPhotoExists() {
        return photoFile != null && photoFile.exists();
    }
}

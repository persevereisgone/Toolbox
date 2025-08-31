package Yu.code.toolbox.function.recorder_photo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 录音文件数据模型
 */
public class RecordingItem {
    private File file;
    private String fileName;
    private String filePath;
    private long fileSize;
    private long duration;
    private Date createTime;
    private boolean isPlaying = false;
    private List<RecordingPhotoItem> photos = new ArrayList<>(); // 录音中的照片列表

    public RecordingItem(File file) {
        this.file = file;
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.createTime = new Date(file.lastModified());
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    /**
     * 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", fileSize / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * 获取格式化的创建时间
     */
    public String getFormattedCreateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(createTime);
    }

    /**
     * 获取格式化的时长
     */
    public String getFormattedDuration() {
        if (duration <= 0) {
            return "未知";
        }
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 获取显示的文件信息
     */
    public String getDisplayInfo() {
        String baseInfo = getFormattedCreateTime() + " | " + getFormattedFileSize() + " | " + getFormattedDuration();
        if (hasPhotos()) {
            return baseInfo + " | 📸 " + getPhotoCount() + "张照片";
        }
        return baseInfo;
    }

    // 照片相关方法
    public List<RecordingPhotoItem> getPhotos() {
        return photos;
    }

    public void setPhotos(List<RecordingPhotoItem> photos) {
        this.photos = photos;
    }

    public void addPhoto(RecordingPhotoItem photo) {
        this.photos.add(photo);
    }

    public void removePhoto(RecordingPhotoItem photo) {
        this.photos.remove(photo);
    }

    /**
     * 根据时间戳获取照片
     */
    public RecordingPhotoItem getPhotoAtTime(long timestamp) {
        for (RecordingPhotoItem photo : photos) {
            if (Math.abs(photo.getTimestamp() - timestamp) < 1000) { // 1秒内的误差
                return photo;
            }
        }
        return null;
    }

    /**
     * 获取照片数量
     */
    public int getPhotoCount() {
        return photos.size();
    }

    /**
     * 检查是否有照片
     */
    public boolean hasPhotos() {
        return !photos.isEmpty();
    }
}

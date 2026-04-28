package Yu.code.toolbox.function.lottery;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽奖组数据模型
 */
public class LotteryGroup {
    private String name;
    private List<LotteryItem> items;
    private long createTime;

    public LotteryGroup() {
        this.items = new ArrayList<>();
        this.createTime = System.currentTimeMillis();
    }

    public LotteryGroup(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LotteryItem> getItems() {
        return items;
    }

    public void setItems(List<LotteryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    /**
     * 添加抽奖项目
     */
    public void addItem(LotteryItem item) {
        if (item != null && !items.contains(item)) {
            items.add(item);
        }
    }

    /**
     * 移除抽奖项目
     */
    public boolean removeItem(LotteryItem item) {
        return items.remove(item);
    }

    /**
     * 根据名称移除项目
     */
    public boolean removeItemByName(String name) {
        for (LotteryItem item : items) {
            if (item.getName().equals(name)) {
                return items.remove(item);
            }
        }
        return false;
    }

    /**
     * 获取总权重
     */
    public int getTotalWeight() {
        int total = 0;
        for (LotteryItem item : items) {
            total += item.getWeight();
        }
        return total;
    }

    /**
     * 检查是否有项目
     */
    public boolean hasItems() {
        return !items.isEmpty();
    }

    /**
     * 获取项目数量
     */
    public int getItemCount() {
        return items.size();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LotteryGroup that = (LotteryGroup) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

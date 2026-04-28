package Yu.code.toolbox.function.lottery;

/**
 * 抽奖项目数据模型
 */
public class LotteryItem {
    private String name;
    private int weight; // 权重，用于控制抽中概率
    private String color; // 颜色，用于转盘显示

    public LotteryItem() {
        this.weight = 1; // 默认权重为1
        this.color = "#FF6B6B"; // 默认颜色
    }

    public LotteryItem(String name) {
        this();
        this.name = name;
    }

    public LotteryItem(String name, int weight) {
        this(name);
        this.weight = weight;
    }

    public LotteryItem(String name, int weight, String color) {
        this(name, weight);
        this.color = color;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight); // 权重至少为1
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LotteryItem that = (LotteryItem) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

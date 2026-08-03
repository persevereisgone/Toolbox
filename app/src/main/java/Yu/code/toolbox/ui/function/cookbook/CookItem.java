package Yu.code.toolbox.ui.function.cookbook;

/**
 * 菜品实体类
 */
public class CookItem {
    private int id;
    private String name;
    private String ingredients; // 食材，逗号分隔
    private String seasonings; // 配料，逗号分隔
    private String steps; // 制作步骤
    private String category; // 分类（主菜、汤品、凉菜等）
    private String imageUri; // 本地选择的封面图片 URI

    public CookItem() {
    }

    public CookItem(int id, String name, String ingredients, String seasonings, String steps, String category) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.seasonings = seasonings;
        this.steps = steps;
        this.category = category;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getSeasonings() { return seasonings; }
    public void setSeasonings(String seasonings) { this.seasonings = seasonings; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String[] getIngredientArray() {
        if (ingredients == null || ingredients.isEmpty()) {
            return new String[0];
        }
        return ingredients.split("[,，]+");
    }

    public String[] getSeasoningArray() {
        if (seasonings == null || seasonings.isEmpty()) {
            return new String[0];
        }
        return seasonings.split("[,，]+");
    }

    @Override
    public String toString() {
        return "CookItem{name='" + name + "', category='" + category + "'}";
    }
}

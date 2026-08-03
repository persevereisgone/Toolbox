package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 菜谱管理类：负责持久化存储和 CRUD 操作
 */
public class CookBookManager {
    private static final String PREFS_NAME = "cookbook_prefs";
    private static final String KEY_COOKBOOKS = "cookbooks_list";
    private static final String KEY_MENU_ITEMS = "menu_items_list";
    private static final String KEY_PURCHASED_ITEMS = "purchased_items_list";

    private final SharedPreferences preferences;
    private final Gson gson;
    private int nextId;

    public static final String CATEGORY_MAIN = "主菜";
    public static final String CATEGORY_SOUP = "汤品";
    public static final String CATEGORY_COLD = "凉菜";
    public static final String CATEGORY_DESSERT = "甜品";
    public static final String CATEGORY_SIDE = "配菜";

    public CookBookManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadNextId();
    }

    public List<CookItem> getAllCookbooks() {
        Type listType = new TypeToken<List<CookItem>>(){}.getType();
        String json = preferences.getString(KEY_COOKBOOKS, "");
        if (json.isEmpty()) {
            List<CookItem> defaults = getDefaultCookbooks();
            syncNextId(defaults);
            return defaults;
        }
        List<CookItem> cookbooks = gson.fromJson(json, listType);
        if (cookbooks == null) {
            return new ArrayList<>();
        }
        syncNextId(cookbooks);
        return cookbooks;
    }

    public void saveCookbooks(List<CookItem> cookbooks) {
        String json = gson.toJson(cookbooks);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_COOKBOOKS, json);
        editor.putInt("next_id", nextId);
        editor.apply();
    }

    public int addCookbook(CookItem cookbook) {
        List<CookItem> cookbooks = getAllCookbooks();
        cookbook.setId(nextId++);
        cookbooks.add(cookbook);
        saveCookbooks(cookbooks);
        return cookbook.getId();
    }

    public boolean updateCookbook(CookItem updatedCookbook) {
        List<CookItem> cookbooks = getAllCookbooks();
        for (CookItem item : cookbooks) {
            if (item.getId() == updatedCookbook.getId()) {
                item.setName(updatedCookbook.getName());
                item.setIngredients(updatedCookbook.getIngredients());
                item.setSeasonings(updatedCookbook.getSeasonings());
                item.setSteps(updatedCookbook.getSteps());
                item.setCategory(updatedCookbook.getCategory());
                item.setImageUri(updatedCookbook.getImageUri());
                saveCookbooks(cookbooks);
                return true;
            }
        }
        return false;
    }

    public boolean deleteCookbook(int id) {
        List<CookItem> cookbooks = getAllCookbooks();
        cookbooks.removeIf(item -> item.getId() == id);
        saveCookbooks(cookbooks);
        return true;
    }

    public CookItem getCookbookById(int id) {
        List<CookItem> cookbooks = getAllCookbooks();
        for (CookItem item : cookbooks) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public List<CookItem> getMenuItems() {
        Type listType = new TypeToken<List<CookItem>>(){}.getType();
        String json = preferences.getString(KEY_MENU_ITEMS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        List<CookItem> menuItems = gson.fromJson(json, listType);
        return menuItems == null ? new ArrayList<>() : menuItems;
    }

    public void addToMenu(CookItem cookbook) {
        List<CookItem> menuItems = getMenuItems();
        for (CookItem item : menuItems) {
            if (item.getId() == cookbook.getId()) {
                return;
            }
        }
        menuItems.add(cookbook);
        saveMenuItems(menuItems);
    }

    public void removeFromMenu(int id) {
        List<CookItem> menuItems = getMenuItems();
        menuItems.removeIf(item -> item.getId() == id);
        saveMenuItems(menuItems);
    }

    public void clearMenu() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_MENU_ITEMS);
        editor.apply();
    }

    private void saveMenuItems(List<CookItem> menuItems) {
        String json = gson.toJson(menuItems);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_MENU_ITEMS, json);
        editor.apply();
    }

    public boolean isMenuEmpty() {
        return getMenuItems().isEmpty();
    }

    public int getMenuItemCount(int cookbookId) {
        List<CookItem> menuItems = getMenuItems();
        int count = 0;
        for (CookItem item : menuItems) {
            if (item.getId() == cookbookId) {
                count++;
            }
        }
        return count;
    }

    public Map<String, Integer> generatePurchaseList() {
        List<CookItem> menuItems = getMenuItems();
        Map<String, Integer> purchaseMap = new HashMap<>();

        for (CookItem item : menuItems) {
            String[] ingredients = item.getIngredientArray();
            for (String ing : ingredients) {
                String key = ing.trim();
                if (!key.isEmpty()) {
                    purchaseMap.put(key, purchaseMap.getOrDefault(key, 0) + 1);
                }
            }

            String[] seasonings = item.getSeasoningArray();
            for (String sea : seasonings) {
                String key = sea.trim();
                if (!key.isEmpty()) {
                    purchaseMap.put(key, purchaseMap.getOrDefault(key, 0) + 1);
                }
            }
        }

        return purchaseMap;
    }

    public Map<String, List<String>> generateGroupedPurchaseList() {
        Map<String, List<String>> grouped = new HashMap<>();
        Map<String, Integer> purchaseMap = generatePurchaseList();

        for (String ingredient : purchaseMap.keySet()) {
            String category = classifyIngredient(ingredient);
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(ingredient);
        }

        return grouped;
    }

    private String classifyIngredient(String ingredient) {
        ingredient = ingredient.toLowerCase();
        if (ingredient.contains("肉") || ingredient.contains("鸡") || ingredient.contains("猪") ||
            ingredient.contains("牛") || ingredient.contains("羊") || ingredient.contains("鱼")) {
            return "肉类";
        } else if (ingredient.contains("菜") || ingredient.contains("瓜") || ingredient.contains("豆") ||
                   ingredient.contains("叶") || ingredient.contains("椒")) {
            return "蔬菜";
        } else if (ingredient.contains("油") || ingredient.contains("盐") || ingredient.contains("糖") ||
                   ingredient.contains("酱油") || ingredient.contains("醋") || ingredient.contains("粉") ||
                   ingredient.contains("酱")) {
            return "调味品";
        } else {
            return "其他";
        }
    }

    public Set<String> getPurchasedItems() {
        String json = preferences.getString(KEY_PURCHASED_ITEMS, "");
        if (json.isEmpty()) {
            return new HashSet<>();
        }
        Type type = new TypeToken<Set<String>>(){}.getType();
        Set<String> set = gson.fromJson(json, type);
        return set == null ? new HashSet<>() : set;
    }

    public void setPurchased(String itemName, boolean purchased) {
        Set<String> set = getPurchasedItems();
        if (purchased) {
            set.add(itemName);
        } else {
            set.remove(itemName);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PURCHASED_ITEMS, gson.toJson(set));
        editor.apply();
    }

    public void clearPurchased() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_PURCHASED_ITEMS);
        editor.apply();
    }

    private void loadNextId() {
        nextId = preferences.getInt("next_id", 1);
    }

    private void syncNextId(List<CookItem> cookbooks) {
        int maxId = 0;
        for (CookItem item : cookbooks) {
            if (item != null && item.getId() > maxId) {
                maxId = item.getId();
            }
        }
        if (nextId <= maxId) {
            nextId = maxId + 1;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("next_id", nextId);
            editor.apply();
        }
    }

    private List<CookItem> getDefaultCookbooks() {
        List<CookItem> defaultCookbooks = new ArrayList<>();
        defaultCookbooks.add(new CookItem(1, "红烧肉", "五花肉,冰糖,葱", "姜,八角,桂皮,料酒",
            "1.五花肉切块焯水；2.炒糖色；3.加入肉块翻炒上色；4.加入调料加水炖煮1小时", "主菜"));
        defaultCookbooks.add(new CookItem(2, "番茄鸡蛋面", "鸡蛋,番茄,面条", "盐,葱,香菜",
            "1.鸡蛋打散炒熟盛出；2.番茄炒出汁；3.加水煮开下面条；4.加入鸡蛋调味；5.撒葱花出锅", "主菜"));
        defaultCookbooks.add(new CookItem(3, "冬瓜排骨汤", "排骨,冬瓜", "姜片,盐,胡椒粉",
            "1.排骨焯水；2.冬瓜去皮切块；3.所有材料放入锅中加水煮沸转小火炖1小时；4.调味即可", "汤品"));
        defaultCookbooks.add(new CookItem(4, "凉拌黄瓜", "黄瓜", "蒜末,生抽,醋,香油,辣椒油",
            "1.黄瓜拍切；2.加入蒜末、生抽、醋、香油拌匀；3.喜辣可加辣椒油", "凉菜"));
        defaultCookbooks.add(new CookItem(5, "清炒时蔬", "菠菜或油菜", "蒜,盐适量",
            "1.蒜切片；2.热油爆香蒜片；3.放入时蔬快速翻炒；4.加盐调味出锅", "配菜"));
        return defaultCookbooks;
    }
}

package Yu.code.toolbox.ui.function.cookbook;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜谱管理类：负责持久化存储和 CRUD 操作
 */
public class CookBookManager {
    private static final String PREFS_NAME = "cookbook_prefs";
    private static final String KEY_COOKBOOKS = "cookbooks_list";
    private static final String KEY_MENU_ITEMS = "menu_items_list";
    private static final String KEY_PURCHASED_ITEMS = "purchased_items_list";
    private static final String KEY_CATEGORIES = "categories_list";

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
            saveCookbooks(defaults);
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
        boolean found = false;
        for (CookItem item : cookbooks) {
            if (item.getId() == updatedCookbook.getId()) {
                copyCookbookFields(updatedCookbook, item);
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
        saveCookbooks(cookbooks);
        syncMenuItem(updatedCookbook);
        return true;
    }

    public boolean deleteCookbook(int id) {
        List<CookItem> cookbooks = getAllCookbooks();
        cookbooks.removeIf(item -> item.getId() == id);
        saveCookbooks(cookbooks);
        removeFromMenu(id);
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

    public List<String> getCategories() {
        Type listType = new TypeToken<List<String>>(){}.getType();
        String json = preferences.getString(KEY_CATEGORIES, "");
        if (json.isEmpty()) {
            List<String> defaults = getDefaultCategories();
            saveCategories(defaults);
            return defaults;
        }
        List<String> categories = gson.fromJson(json, listType);
        if (categories == null || categories.isEmpty()) {
            List<String> defaults = getDefaultCategories();
            saveCategories(defaults);
            return defaults;
        }
        return categories;
    }

    public List<String> getFilterCategories() {
        List<String> result = new ArrayList<>();
        result.add("全部");
        result.addAll(getCategories());
        return result;
    }

    public boolean addCategory(String name) {
        String normalized = normalizeCategoryName(name);
        if (normalized.isEmpty()) {
            return false;
        }
        List<String> categories = getCategories();
        for (String existing : categories) {
            if (existing.equalsIgnoreCase(normalized)) {
                return false;
            }
        }
        categories.add(normalized);
        saveCategories(categories);
        return true;
    }

    public boolean renameCategory(String oldName, String newName) {
        if (TextUtils.isEmpty(oldName)) {
            return false;
        }
        String normalized = normalizeCategoryName(newName);
        if (normalized.isEmpty()) {
            return false;
        }
        List<String> categories = getCategories();
        int index = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (oldName.equals(categories.get(i))) {
                index = i;
            } else if (normalized.equalsIgnoreCase(categories.get(i))) {
                return false;
            }
        }
        if (index < 0) {
            return false;
        }
        categories.set(index, normalized);
        saveCategories(categories);
        migrateCategoryName(oldName, normalized);
        return true;
    }

    public boolean deleteCategory(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        List<String> categories = getCategories();
        if (categories.size() <= 1) {
            return false;
        }
        boolean removed = categories.removeIf(item -> name.equals(item));
        if (!removed) {
            return false;
        }
        String fallback = categories.get(0);
        saveCategories(categories);
        migrateCategoryName(name, fallback);
        return true;
    }

    public String getFallbackCategory(String excluding) {
        List<String> categories = getCategories();
        for (String category : categories) {
            if (!category.equals(excluding)) {
                return category;
            }
        }
        return CATEGORY_MAIN;
    }

    private void migrateCategoryName(String from, String to) {
        List<CookItem> cookbooks = getAllCookbooks();
        boolean changed = false;
        for (CookItem item : cookbooks) {
            if (item != null && from.equals(item.getCategory())) {
                item.setCategory(to);
                changed = true;
            }
        }
        if (changed) {
            saveCookbooks(cookbooks);
        }

        List<CookItem> menuItems = getMenuItems();
        boolean menuChanged = false;
        for (CookItem item : menuItems) {
            if (item != null && from.equals(item.getCategory())) {
                item.setCategory(to);
                menuChanged = true;
            }
        }
        if (menuChanged) {
            saveMenuItems(menuItems);
        }
    }

    private void saveCategories(List<String> categories) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CATEGORIES, gson.toJson(categories));
        editor.apply();
    }

    private List<String> getDefaultCategories() {
        List<String> defaults = new ArrayList<>();
        defaults.add(CATEGORY_MAIN);
        defaults.add(CATEGORY_SOUP);
        defaults.add(CATEGORY_COLD);
        defaults.add(CATEGORY_DESSERT);
        defaults.add(CATEGORY_SIDE);
        return defaults;
    }

    private String normalizeCategoryName(String name) {
        return name == null ? "" : name.trim();
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
        editor.remove(KEY_PURCHASED_ITEMS);
        editor.apply();
    }

    private void saveMenuItems(List<CookItem> menuItems) {
        String json = gson.toJson(menuItems);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_MENU_ITEMS, json);
        editor.apply();
    }

    private void syncMenuItem(CookItem updatedCookbook) {
        List<CookItem> menuItems = getMenuItems();
        boolean changed = false;
        for (CookItem item : menuItems) {
            if (item.getId() == updatedCookbook.getId()) {
                copyCookbookFields(updatedCookbook, item);
                changed = true;
                break;
            }
        }
        if (changed) {
            saveMenuItems(menuItems);
        }
    }

    private void copyCookbookFields(CookItem source, CookItem target) {
        target.setName(source.getName());
        target.setIngredients(source.getIngredients());
        target.setSeasonings(source.getSeasonings());
        target.setSteps(source.getSteps());
        target.setCategory(source.getCategory());
        target.setImageUri(source.getImageUri());
    }

    public boolean isMenuEmpty() {
        return getMenuItems().isEmpty();
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

package Yu.code.toolbox.ui.function.cookbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CookBookManagerCategoryFilterTest {
    private Context context;
    private CookBookManager manager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("cookbook_prefs", Context.MODE_PRIVATE).edit().clear().commit();
        manager = new CookBookManager(context);
    }

    @Test
    public void allIsReservedAndLegacyCategoryIsMigrated() {
        CookItem legacyRecipe = new CookItem(1, "旧菜谱", "", "", "", CookBookManager.CATEGORY_ALL);
        context.getSharedPreferences("cookbook_prefs", Context.MODE_PRIVATE).edit()
                .putString("categories_list", new Gson().toJson(Arrays.asList("全部", "主菜")))
                .putString("cookbooks_list", new Gson().toJson(Arrays.asList(legacyRecipe)))
                .commit();

        List<String> filters = manager.getFilterCategories();
        List<String> categories = manager.getCategories();

        assertEquals(1, filters.stream().filter(CookBookManager.CATEGORY_ALL::equals).count());
        assertFalse(categories.contains(CookBookManager.CATEGORY_ALL));
        assertEquals("全部菜谱", manager.getAllCookbooks().get(0).getCategory());
        assertFalse(manager.addCategory(CookBookManager.CATEGORY_ALL));
    }
}

package Yu.code.toolbox.ui.function.cookbook;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class CookBookManagerPurchaseListTest {

    @Before
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
                .getSharedPreferences("cookbook_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void purchaseListCountsEachIngredientOncePerRecipe() {
        CookBookManager manager = new CookBookManager(RuntimeEnvironment.getApplication());
        CookItem first = new CookItem(0, "姜菜", "姜5片,盐", "姜片", "", "主菜");
        CookItem second = new CookItem(0, "葱菜", "姜", "", "", "主菜");

        manager.addCookbook(first);
        manager.addCookbook(second);
        manager.addToMenu(first);
        manager.addToMenu(second);

        Map<String, Integer> purchaseList = manager.generatePurchaseList();

        assertEquals(Integer.valueOf(2), purchaseList.get("姜"));
        assertEquals(Integer.valueOf(1), purchaseList.get("盐"));
    }
}

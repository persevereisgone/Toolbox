package Yu.code.toolbox.ui.function.cookbook;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CookBookManagerIngredientNormalizationTest {

    @Test
    public void normalizeIngredientKeyRemovesLeadingAndTrailingQuantities() {
        assertEquals("鸡蛋", CookBookManager.normalizeIngredientKey("鸡蛋2个"));
        assertEquals("鸡蛋", CookBookManager.normalizeIngredientKey("2个鸡蛋"));
        assertEquals("姜", CookBookManager.normalizeIngredientKey("姜5片"));
        assertEquals("五花肉", CookBookManager.normalizeIngredientKey("500g五花肉"));
        assertEquals("五花肉", CookBookManager.normalizeIngredientKey("五花肉500克"));
    }
}

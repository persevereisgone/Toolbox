import static org.junit.Assert.*;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Method;

import Yu.code.toolbox.R;
import Yu.code.toolbox.function.statistics.StatisticsAdapter;
import Yu.code.toolbox.function.statistics.StatisticsManager;

@RunWith(RobolectricTestRunner.class)
public class StatisticsManagerRobolectricTest {

    private static class Harness {
        final FragmentActivity activity;
        final View rootView;
        final StatisticsManager manager;

        final Button btnClearAll;
        final TextView tvTotal;
        final RecyclerView rv;
        final View fabMain;

        Harness() {
            ActivityController<FragmentActivity> controller =
                    Robolectric.buildActivity(FragmentActivity.class).setup();
            activity = controller.get();
            rootView = LayoutInflater.from(activity).inflate(R.layout.function_statistics, null, false);
            manager = new StatisticsManager(activity, rootView);

            btnClearAll = rootView.findViewById(R.id.btn_clear_all);
            tvTotal = rootView.findViewById(R.id.tv_total_count);
            rv = rootView.findViewById(R.id.rv_statistics);
            fabMain = rootView.findViewById(R.id.fab_main);
        }

        StatisticsAdapter adapter() {
            return (StatisticsAdapter) rv.getAdapter();
        }

        /** 经新增底弹加一件(走真链路:showAddSheet→填→添加) */
        void addViaSheet(String name, String qty) {
            fabMain.performClick();
            View speedAdd = rootView.findViewById(R.id.fab_menu_add);
            assertNotNull(speedAdd);
            speedAdd.performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

            androidx.appcompat.app.AlertDialog dialog =
                    (androidx.appcompat.app.AlertDialog) ShadowAlertDialog.getLatestDialog();
            // BottomSheetDialog 不进 ShadowAlertDialog,改走反射调 addNewItem 真逻辑
            if (dialog == null) {
                invokeAddItem(manager, name, qty);
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            }
        }

        void layoutRecycler() {
            rv.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
            );
            rv.layout(0, 0, 1080, 1920);
        }
    }

    @Test
    public void initialState_shouldHaveDefaultSet_andTotalZero() {
        Harness h = new Harness();

        assertEquals("总计: 0 · 0 件", h.tvTotal.getText().toString());
        assertNotNull(h.rv.getAdapter());
        assertEquals(0, h.adapter().getItemCount());
        // FAB 主键存在,速拨默认收起
        assertNotNull(h.fabMain);
        assertEquals(View.GONE, h.rootView.findViewById(R.id.layout_speed_dial).getVisibility());
    }

    @Test
    public void uiDisplay_shouldShowCoreTextsHintsAndButtons() {
        Harness h = new Harness();

        assertNotNull(findTextViewByExactText(h.rootView, "物品统计"));
        assertNotNull(findTextViewByExactText(h.rootView, "长摁多选 · 加减改数 · 导入导出"));

        EditText etSearch = h.rootView.findViewById(R.id.et_search);
        assertNotNull(etSearch);
        assertEquals("搜索物品名", etSearch.getHint().toString());
        TextView tvSort = h.rootView.findViewById(R.id.tv_sort);
        assertNotNull(tvSort);
        assertEquals("数量 ↓", tvSort.getText().toString());
        assertEquals("一键清空当前组", h.btnClearAll.getText().toString());
        assertEquals("总计: 0 · 0 件", h.tvTotal.getText().toString());
    }

    @Test
    public void createSetDialog_buttonsShouldBeVisibleAndReadable() {
        Harness h = new Harness();

        invokePrivateNoArg(h.manager, "showCreateSetDialog");
        android.app.Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);

        android.widget.Button positive = getDialogButton(dialog, android.content.DialogInterface.BUTTON_POSITIVE);
        android.widget.Button negative = getDialogButton(dialog, android.content.DialogInterface.BUTTON_NEGATIVE);

        assertNotNull(positive);
        assertNotNull(negative);
        assertEquals(View.VISIBLE, positive.getVisibility());
        assertEquals(View.VISIBLE, negative.getVisibility());
        assertTrue(positive.getText().length() > 0);
        assertTrue(negative.getText().length() > 0);
    }

    @Test
    public void speedDial_shouldExpandAndCollapse() {
        Harness h = new Harness();
        View dial = h.rootView.findViewById(R.id.layout_speed_dial);
        View scrim = h.rootView.findViewById(R.id.view_speed_scrim);

        h.fabMain.performClick();
        assertEquals(View.VISIBLE, dial.getVisibility());
        assertEquals(View.VISIBLE, scrim.getVisibility());

        scrim.performClick();
        assertEquals(View.GONE, dial.getVisibility());
        assertEquals(View.GONE, scrim.getVisibility());
    }

    private TextView findTextViewByExactText(View root, String text) {
        if (root instanceof TextView) {
            CharSequence value = ((TextView) root).getText();
            if (value != null && text.equals(value.toString())) {
                return (TextView) root;
            }
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView matched = findTextViewByExactText(vg.getChildAt(i), text);
                if (matched != null) {
                    return matched;
                }
            }
        }
        return null;
    }

    private android.widget.Button getDialogButton(android.app.Dialog dialog, int whichButton) {
        if (dialog instanceof android.app.AlertDialog) {
            return ((android.app.AlertDialog) dialog).getButton(whichButton);
        }
        if (dialog instanceof androidx.appcompat.app.AlertDialog) {
            return ((androidx.appcompat.app.AlertDialog) dialog).getButton(whichButton);
        }
        return null;
    }

    private void invokePrivateNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke method: " + methodName, e);
        }
    }

    private static void invokeAddItem(Object target, String name, String qty) {
        try {
            Method method = target.getClass().getDeclaredMethod("addNewItem", String.class, String.class);
            method.setAccessible(true);
            method.invoke(target, name, qty);
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke addNewItem", e);
        }
    }

    @Test
    public void addItem_shouldUpdateRecycler_andTotal() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "2");

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 2 · 1 件", h.tvTotal.getText().toString());
        assertEquals("已添加物品", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addSameName_shouldMergeQuantity_andKeepSingleRow() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "2");
        h.addViaSheet("可乐", "3");

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 5 · 1 件", h.tvTotal.getText().toString());
        assertEquals("已更新物品数量", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addItem_emptyName_shouldToast_andDoNothing() {
        Harness h = new Harness();
        h.addViaSheet("", "1");

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("请输入物品名称", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addItem_negativeQuantity_shouldToast_andDoNothing() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "-1");

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("数量不能为负数", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void increaseDecreaseButtons_shouldUpdateTotalAndRow() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "1");
        assertEquals("总计: 1 · 1 件", h.tvTotal.getText().toString());

        h.layoutRecycler();
        RecyclerView.ViewHolder vh0 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh0);

        android.widget.ImageButton btnIncrease = vh0.itemView.findViewById(R.id.btn_increase);
        android.widget.ImageButton btnDecrease = vh0.itemView.findViewById(R.id.btn_decrease);

        btnIncrease.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        h.layoutRecycler();
        RecyclerView.ViewHolder vh1 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh1);
        TextView tvQty1 = vh1.itemView.findViewById(R.id.tv_quantity);
        assertEquals("2", tvQty1.getText().toString());
        assertEquals("总计: 2 · 1 件", h.tvTotal.getText().toString());

        btnDecrease.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        h.layoutRecycler();
        RecyclerView.ViewHolder vh2 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh2);
        TextView tvQty2 = vh2.itemView.findViewById(R.id.tv_quantity);
        assertEquals("1", tvQty2.getText().toString());
        assertEquals("总计: 1 · 1 件", h.tvTotal.getText().toString());
    }

    @Test
    public void clearAll_shouldConfirmThenClearAndTotalZero() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "2");
        assertEquals(1, h.adapter().getItemCount());

        h.btnClearAll.performClick();
        android.app.Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);

        android.widget.Button positive = getDialogButton(dialog, android.content.DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positive);
        positive.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("总计: 0 · 0 件", h.tvTotal.getText().toString());
        assertEquals("已清空所有物品", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void multiSelectDelete_shouldDeleteSelectedItems_andUpdateTotal() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "2");
        h.addViaSheet("薯片", "3");
        h.addViaSheet("面包", "4");

        assertEquals(3, h.adapter().getItemCount());
        assertEquals("总计: 9 · 3 件", h.tvTotal.getText().toString());

        // 长摁多选:直接调 adapter 真链路(enterMultiSelect+toggleSelection)
        h.adapter().enterMultiSelect(0);
        h.adapter().toggleSelection(1);
        assertEquals(View.VISIBLE, h.rootView.findViewById(R.id.layout_multiselect_bar).getVisibility());
        // 多选态隐藏 FAB
        assertEquals(View.GONE, h.fabMain.getVisibility());

        Button btnDelete = h.rootView.findViewById(R.id.btn_multiselect_delete);
        btnDelete.performClick();
        android.app.Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);

        android.widget.Button positive = getDialogButton(dialog, android.content.DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positive);
        positive.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 4 · 1 件", h.tvTotal.getText().toString());
        assertEquals("已删除所选", ShadowToast.getTextOfLatestToast());
        // 删完自动退多选,FAB 恢复
        assertEquals(View.GONE, h.rootView.findViewById(R.id.layout_multiselect_bar).getVisibility());
        assertEquals(View.VISIBLE, h.fabMain.getVisibility());
    }

    @Test
    public void search_shouldFilterVisibleRows_andKeepTotal() {
        Harness h = new Harness();
        h.addViaSheet("可乐", "2");
        h.addViaSheet("薯片", "3");

        EditText etSearch = h.rootView.findViewById(R.id.et_search);
        etSearch.setText("可乐");
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(1, h.adapter().getItemCount());
        // 总计不受搜索影响
        assertEquals("总计: 5 · 2 件", h.tvTotal.getText().toString());
    }

    @Test
    public void persistence_shouldReloadItemsInNewManagerInstance() {
        Harness h1 = new Harness();
        h1.addViaSheet("可乐", "2");
        h1.manager.onDestroy();

        Harness h2 = new Harness();
        assertEquals(1, h2.adapter().getItemCount());
        assertEquals("总计: 2 · 1 件", h2.tvTotal.getText().toString());
    }
}

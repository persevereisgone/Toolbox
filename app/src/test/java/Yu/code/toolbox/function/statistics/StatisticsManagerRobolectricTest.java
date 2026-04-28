package Yu.code.toolbox.function.statistics;

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

@RunWith(RobolectricTestRunner.class)
public class StatisticsManagerRobolectricTest {

    private static class Harness {
        final FragmentActivity activity;
        final View rootView;
        final StatisticsManager manager;

        final EditText etName;
        final EditText etQty;
        final Button btnAdd;
        final Button btnClearAll;
        final Button btnBatchDelete;
        final Button btnExport;
        final Button btnImport;
        final Button btnFullscreenList;
        final Button btnManageSets;
        final TextView tvTotal;
        final Spinner spinnerSets;
        final RecyclerView rv;

        Harness() {
            ActivityController<FragmentActivity> controller =
                    Robolectric.buildActivity(FragmentActivity.class).setup();
            activity = controller.get();
            rootView = LayoutInflater.from(activity).inflate(R.layout.function_statistics, null, false);
            manager = new StatisticsManager(activity, rootView);

            etName = rootView.findViewById(R.id.et_item_name);
            etQty = rootView.findViewById(R.id.et_item_quantity);
            btnAdd = rootView.findViewById(R.id.btn_add_item);
            btnClearAll = rootView.findViewById(R.id.btn_clear_all);
            btnBatchDelete = rootView.findViewById(R.id.btn_batch_delete);
            btnExport = rootView.findViewById(R.id.btn_export);
            btnImport = rootView.findViewById(R.id.btn_import);
            btnFullscreenList = rootView.findViewById(R.id.btn_fullscreen_list);
            btnManageSets = rootView.findViewById(R.id.btn_manage_sets);
            tvTotal = rootView.findViewById(R.id.tv_total_count);
            spinnerSets = rootView.findViewById(R.id.spinner_statistics_set);
            rv = rootView.findViewById(R.id.rv_statistics);
        }

        StatisticsAdapter adapter() {
            return (StatisticsAdapter) rv.getAdapter();
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

        assertNotNull(h.spinnerSets.getAdapter());
        assertTrue(h.spinnerSets.getAdapter().getCount() >= 1);
        assertEquals("默认统计组", h.spinnerSets.getAdapter().getItem(0));

        assertEquals("总计: 0", h.tvTotal.getText().toString());
        assertNotNull(h.rv.getAdapter());
        assertEquals(0, h.adapter().getItemCount());
    }

    @Test
    public void uiDisplay_shouldShowCoreTextsHintsAndButtons() {
        Harness h = new Harness();

        assertNotNull(findTextViewByExactText(h.rootView, "物品统计"));
        assertNotNull(findTextViewByExactText(h.rootView, "快速维护统计组、录入数量并导入导出数据。"));

        assertEquals("物品名称", h.etName.getHint().toString());
        assertEquals("数量", h.etQty.getHint().toString());
        assertEquals("添加", h.btnAdd.getText().toString());
        assertEquals("一键清空", h.btnClearAll.getText().toString());
        assertEquals("批量删除", h.btnBatchDelete.getText().toString());
        assertEquals("导出数据", h.btnExport.getText().toString());
        assertEquals("导入数据", h.btnImport.getText().toString());
        assertEquals("全屏查看物品列表", h.btnFullscreenList.getText().toString());
        assertEquals("管理", h.btnManageSets.getText().toString());
        assertEquals("总计: 0", h.tvTotal.getText().toString());
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

        int expectedPositiveColor = h.activity.getResources().getColor(android.R.color.holo_blue_dark);
        int expectedNegativeColor = h.activity.getResources().getColor(android.R.color.black);
        assertEquals(expectedPositiveColor, positive.getCurrentTextColor());
        assertEquals(expectedNegativeColor, negative.getCurrentTextColor());
    }

    @Test
    public void batchDelete_whenNoItems_shouldToast() {
        Harness h = new Harness();
        h.btnBatchDelete.performClick();
        assertEquals("没有物品可删除", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void batchDeleteDialog_buttonsShouldBeVisibleAndReadable() {
        Harness h = new Harness();
        h.etName.setText("可乐");
        h.etQty.setText("1");
        h.btnAdd.performClick();

        h.btnBatchDelete.performClick();
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

        int expectedPositiveColor = h.activity.getResources().getColor(android.R.color.holo_red_dark);
        int expectedNegativeColor = h.activity.getResources().getColor(android.R.color.black);
        assertEquals(expectedPositiveColor, positive.getCurrentTextColor());
        assertEquals(expectedNegativeColor, negative.getCurrentTextColor());
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

    private android.widget.ListView getDialogListView(android.app.Dialog dialog) {
        if (dialog instanceof android.app.AlertDialog) {
            return ((android.app.AlertDialog) dialog).getListView();
        }
        if (dialog instanceof androidx.appcompat.app.AlertDialog) {
            return ((androidx.appcompat.app.AlertDialog) dialog).getListView();
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

    @Test
    public void addItem_shouldUpdateRecycler_andTotal() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("2");
        h.btnAdd.performClick();

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 2", h.tvTotal.getText().toString());
        assertEquals("已添加物品", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addSameName_shouldMergeQuantity_andKeepSingleRow() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("2");
        h.btnAdd.performClick();

        h.etName.setText("可乐");
        h.etQty.setText("3");
        h.btnAdd.performClick();

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 5", h.tvTotal.getText().toString());
        assertEquals("已更新物品数量", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addItem_emptyName_shouldToast_andDoNothing() {
        Harness h = new Harness();

        h.etName.setText("");
        h.etQty.setText("1");
        h.btnAdd.performClick();

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("请输入物品名称", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void addItem_negativeQuantity_shouldToast_andDoNothing() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("-1");
        h.btnAdd.performClick();

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("数量不能为负数", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void increaseDecreaseButtons_shouldUpdateTotalAndRow() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("1");
        h.btnAdd.performClick();
        assertEquals("总计: 1", h.tvTotal.getText().toString());

        h.layoutRecycler();
        RecyclerView.ViewHolder vh0 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh0);

        Button btnIncrease = vh0.itemView.findViewById(R.id.btn_increase);
        Button btnDecrease = vh0.itemView.findViewById(R.id.btn_decrease);

        btnIncrease.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        h.layoutRecycler();
        RecyclerView.ViewHolder vh1 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh1);
        TextView tvQty1 = vh1.itemView.findViewById(R.id.tv_quantity);
        assertEquals("2", tvQty1.getText().toString());
        assertEquals("总计: 2", h.tvTotal.getText().toString());

        btnDecrease.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        h.layoutRecycler();
        RecyclerView.ViewHolder vh2 = h.rv.findViewHolderForAdapterPosition(0);
        assertNotNull(vh2);
        TextView tvQty2 = vh2.itemView.findViewById(R.id.tv_quantity);
        assertEquals("1", tvQty2.getText().toString());
        assertEquals("总计: 1", h.tvTotal.getText().toString());
    }

    @Test
    public void clearAll_shouldConfirmThenClearAndTotalZero() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("2");
        h.btnAdd.performClick();
        assertEquals(1, h.adapter().getItemCount());

        h.btnClearAll.performClick();
        android.app.Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);

        android.widget.Button positive = getDialogButton(dialog, android.content.DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positive);
        positive.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(0, h.adapter().getItemCount());
        assertEquals("总计: 0", h.tvTotal.getText().toString());
        assertEquals("已清空所有物品", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void batchDelete_shouldDeleteSelectedItems_andUpdateTotal() {
        Harness h = new Harness();

        h.etName.setText("可乐");
        h.etQty.setText("2");
        h.btnAdd.performClick();
        h.etName.setText("薯片");
        h.etQty.setText("3");
        h.btnAdd.performClick();
        h.etName.setText("面包");
        h.etQty.setText("4");
        h.btnAdd.performClick();

        assertEquals(3, h.adapter().getItemCount());
        assertEquals("总计: 9", h.tvTotal.getText().toString());

        h.btnBatchDelete.performClick();
        android.app.Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
        android.widget.ListView listView = getDialogListView(dialog);
        assertNotNull(listView);

        View first = listView.getAdapter().getView(0, null, listView);
        View second = listView.getAdapter().getView(1, null, listView);
        listView.performItemClick(first, 0, listView.getAdapter().getItemId(0));
        listView.performItemClick(second, 1, listView.getAdapter().getItemId(1));

        android.widget.Button positive = getDialogButton(dialog, android.content.DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positive);
        positive.performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        assertEquals(1, h.adapter().getItemCount());
        assertEquals("总计: 4", h.tvTotal.getText().toString());
        assertEquals("已批量删除 2 个物品", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void persistence_shouldReloadItemsInNewManagerInstance() {
        Harness h1 = new Harness();
        h1.etName.setText("可乐");
        h1.etQty.setText("2");
        h1.btnAdd.performClick();
        h1.manager.onDestroy();

        Harness h2 = new Harness();
        assertEquals(1, h2.adapter().getItemCount());
        assertEquals("总计: 2", h2.tvTotal.getText().toString());
    }
}


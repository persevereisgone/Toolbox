package Yu.code.toolbox.ui.function.cookbook;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.List;
import java.util.Random;
import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

public class CookbookWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_NEXT = "Yu.code.toolbox.COOK_NEXT";

    @Override public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    @Override public void onReceive(Context ctx, Intent in) {
        super.onReceive(ctx, in);
        if (ACTION_NEXT.equals(in.getAction())) refreshAll(ctx);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_cookbook);
        CookBookManager m = new CookBookManager(ctx);
        List<CookItem> menu = m.getMenuItems();
        List<CookItem> all = m.getAllCookbooks();
        CookItem pick = null;
        if (!menu.isEmpty()) pick = menu.get(new Random().nextInt(menu.size()));
        else if (!all.isEmpty()) pick = all.get(new Random().nextInt(all.size()));
        rv.setTextViewText(R.id.tv_widget_cook_title, pick == null ? "今日菜谱" : pick.getName());
        rv.setTextViewText(R.id.tv_widget_cook_sub, pick == null ? "去App添加菜单" : String.valueOf(pick.getIngredients()));
        rv.setOnClickPendingIntent(R.id.btn_widget_cook_next,
            PendingIntent.getBroadcast(ctx, 31, new Intent(ctx, CookbookWidgetProvider.class).setAction(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent open = new Intent(ctx, MainActivity.class).putExtra("open_function", R.layout.function_cookbook);
        rv.setOnClickPendingIntent(R.id.btn_widget_cook_open,
            PendingIntent.getActivity(ctx, 32, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        mgr.updateAppWidget(id, rv);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        for (int i : mgr.getAppWidgetIds(new ComponentName(ctx, CookbookWidgetProvider.class))) updateOne(ctx, mgr, i);
    }
}

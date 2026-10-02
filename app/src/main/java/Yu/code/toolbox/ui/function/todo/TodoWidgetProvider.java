package Yu.code.toolbox.ui.function.todo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import Yu.code.toolbox.MainActivity;
import Yu.code.toolbox.R;

public class TodoWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_TOGGLE = "Yu.code.toolbox.TODO_TOGGLE";
    public static final String ACTION_REFRESH = "Yu.code.toolbox.TODO_REFRESH";
    public static final String EXTRA_ID = "todo_id";

    @Override public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    @Override public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (ACTION_TOGGLE.equals(a)) {
            new TodoManager(ctx).toggleDone(ctx, intent.getIntExtra(EXTRA_ID, -1));
        } else if (ACTION_REFRESH.equals(a)) {
            refreshAll(ctx);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(a)) {
            refreshAll(ctx);
        }
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int appWidgetId) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_todo);
        TodoManager m = new TodoManager(ctx);
        rv.setTextViewText(R.id.tv_widget_todo_title, "待办 (" + m.getTopUndone(99).size() + " 未完成)");
        Intent svc = new Intent(ctx, TodoWidgetService.class);
        svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.lv_widget_todo, svc);
        rv.setEmptyView(R.id.lv_widget_todo, R.id.tv_widget_todo_empty);
        Intent toggle = new Intent(ctx, TodoWidgetProvider.class).setAction(ACTION_TOGGLE);
        rv.setPendingIntentTemplate(R.id.lv_widget_todo,
            PendingIntent.getBroadcast(ctx, 0, toggle, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent open = new Intent(ctx, MainActivity.class).putExtra("open_function", R.layout.function_todo);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.btn_widget_todo_add,
            PendingIntent.getActivity(ctx, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent refresh = new Intent(ctx, TodoWidgetProvider.class).setAction(ACTION_REFRESH);
        rv.setOnClickPendingIntent(R.id.btn_widget_todo_refresh,
            PendingIntent.getBroadcast(ctx, 2, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        mgr.updateAppWidget(appWidgetId, rv);
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_widget_todo);
    }

    public static void refreshAll(Context ctx) {
        if (ctx == null) return;
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, TodoWidgetProvider.class);
        for (int id : mgr.getAppWidgetIds(cn)) updateOne(ctx, mgr, id);
    }
}

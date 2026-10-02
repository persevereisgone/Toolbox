package Yu.code.toolbox.ui.function.todo;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.List;
import Yu.code.toolbox.R;

public class TodoWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            List<TodoItem> items;
            public void onCreate() {}
            public void onDataSetChanged() { items = new TodoManager(getApplicationContext()).getTopUndone(12); }
            public void onDestroy() {}
            public int getCount() { return items == null ? 0 : items.size(); }
            public RemoteViews getViewAt(int p) {
                TodoItem it = items.get(p);
                RemoteViews rv = new RemoteViews(getPackageName(), R.layout.widget_todo_item);
                rv.setTextViewText(R.id.tv_item_state, it.isDone() ? "✓" : "○");
                rv.setTextViewText(R.id.tv_item_title, it.getTitle());
                rv.setContentDescription(R.id.root_item, (it.isDone() ? "已完成：" : "未完成：") + it.getTitle());
                Intent fill = new Intent().putExtra(TodoWidgetProvider.EXTRA_ID, it.getId());
                rv.setOnClickFillInIntent(R.id.root_item, fill);
                return rv;
            }
            public RemoteViews getLoadingView() { return null; }
            public int getViewTypeCount() { return 1; }
            public long getItemId(int p) { return items.get(p).getId(); }
            public boolean hasStableIds() { return true; }
        };
    }
}

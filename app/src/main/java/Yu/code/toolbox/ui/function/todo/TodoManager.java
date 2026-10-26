package Yu.code.toolbox.ui.function.todo;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TodoManager {
    private static final String PREFS = "todo_prefs";
    private static final String KEY = "todos_list";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private int nextId;

    public TodoManager(Context c) {
        prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        nextId = prefs.getInt("next_id", 1);
    }

    public List<TodoItem> getAll() {
        Type t = new TypeToken<List<TodoItem>>(){}.getType();
        String json = prefs.getString(KEY, "");
        if (json.isEmpty()) return new ArrayList<>();
        List<TodoItem> l = gson.fromJson(json, t);
        return l == null ? new ArrayList<>() : l;
    }

    private void save(List<TodoItem> l) {
        prefs.edit().putString(KEY, gson.toJson(l)).putInt("next_id", nextId).apply();
    }

    public void saveAndRefresh(Context ctx, List<TodoItem> l) {
        prefs.edit().putString(KEY, gson.toJson(l)).putInt("next_id", nextId).apply();
        TodoWidgetProvider.refreshAll(ctx);
    }

    public void add(Context ctx, String title) {
        List<TodoItem> l = getAll();
        l.add(0, new TodoItem(nextId++, title));
        saveAndRefresh(ctx, l);
    }

    public void toggleDone(Context ctx, int id) {
        List<TodoItem> l = getAll();
        for (TodoItem i : l) if (i.getId() == id) {
            i.setDone(!i.isDone());
            i.setDoneAt(i.isDone() ? System.currentTimeMillis() : 0);
        }
        l.sort((a, b) -> Boolean.compare(a.isDone(), b.isDone()));
        saveAndRefresh(ctx, l);
    }

    public void updateTitle(Context ctx, int id, String title) {
        List<TodoItem> l = getAll();
        for (TodoItem i : l) if (i.getId() == id) i.setTitle(title);
        saveAndRefresh(ctx, l);
    }

    public TodoItem deleteItem(Context ctx, int id) {
        List<TodoItem> l = getAll();
        TodoItem removed = null;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).getId() == id) { removed = l.remove(i); break; }
        }
        if (removed != null) saveAndRefresh(ctx, l);
        return removed;
    }

    public void restoreSingle(Context ctx, TodoItem item) {
        if (item == null) return;
        List<TodoItem> l = getAll();
        boolean exists = false;
        for (TodoItem i : l) if (i.getId() == item.getId()) { exists = true; break; }
        if (!exists) l.add(0, item);
        saveAndRefresh(ctx, l);
    }

    public List<TodoItem> deleteDone(Context ctx) {
        List<TodoItem> current = getAll();
        List<TodoItem> removed = new ArrayList<>();
        List<TodoItem> remaining = new ArrayList<>();
        for (TodoItem item : current) {
            if (item.isDone()) removed.add(item); else remaining.add(item);
        }
        saveAndRefresh(ctx, remaining);
        return removed;
    }

    /** P1: 撤销"清除已完成"时把快照拼回原列表. */
    public void restoreItems(Context ctx, List<TodoItem> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) return;
        List<TodoItem> current = getAll();
        java.util.Set<Integer> existingIds = new java.util.HashSet<>();
        for (TodoItem item : current) existingIds.add(item.getId());
        for (TodoItem item : snapshot) {
            if (item != null && existingIds.add(item.getId())) current.add(item);
        }
        current.sort((a, b) -> Boolean.compare(a.isDone(), b.isDone()));
        saveAndRefresh(ctx, current);
    }

    public List<TodoItem> getTopUndone(int n) {
        List<TodoItem> out = new ArrayList<>();
        for (TodoItem i : getAll()) if (!i.isDone()) { out.add(i); if (out.size() >= n) break; }
        return out;
    }
}

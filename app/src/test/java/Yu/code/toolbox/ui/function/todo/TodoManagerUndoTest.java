package Yu.code.toolbox.ui.function.todo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TodoManagerUndoTest {
    private Context context;
    private TodoManager manager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE).edit().clear().commit();
        manager = new TodoManager(context);
    }

    @Test
    public void restoreDeletedDoneItemsKeepsChangesMadeAfterClear() {
        manager.add(context, "Keep this item");
        manager.add(context, "Restore this item");
        List<TodoItem> beforeClear = manager.getAll();
        int doneId = beforeClear.get(0).getId();
        int editableId = beforeClear.get(1).getId();
        manager.toggleDone(context, doneId);

        List<TodoItem> deleted = manager.deleteDone(context);
        manager.add(context, "New item after clear");
        manager.updateTitle(context, editableId, "Edited after clear");
        manager.restoreItems(context, deleted);

        List<TodoItem> restored = manager.getAll();
        assertEquals(3, restored.size());
        assertTrue(restored.stream().anyMatch(item -> "New item after clear".equals(item.getTitle())));
        assertTrue(restored.stream().anyMatch(item -> "Edited after clear".equals(item.getTitle())));
        assertTrue(restored.stream().anyMatch(item -> item.getId() == doneId && item.isDone()));
    }
}

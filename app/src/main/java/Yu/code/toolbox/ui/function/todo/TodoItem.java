package Yu.code.toolbox.ui.function.todo;

public class TodoItem {
    private int id;
    private String title;
    private boolean done;
    private long createAt;
    private long doneAt;

    public TodoItem(int id, String title) {
        this.id = id;
        this.title = title;
        this.createAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public boolean isDone() { return done; }
    public void setDone(boolean d) { this.done = d; }
    public long getCreateAt() { return createAt; }
    public long getDoneAt() { return doneAt; }
    public void setDoneAt(long t) { this.doneAt = t; }
}

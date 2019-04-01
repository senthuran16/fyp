package kubernetes.manager.models;

/**
 * Contains details of a child Siddhi app
 */
public class ChildSiddhiAppInfo {
    private String name;
    private String content;
    private int initialParallelism;
    private boolean isStateful;
    private boolean isReceiver;

    public ChildSiddhiAppInfo(String name, String content, int initialParallelism, boolean isStateful,
                              boolean isReceiver) {
        this.name = name;
        this.content = content;
        this.initialParallelism = initialParallelism;
        this.isStateful = isStateful;
        this.isReceiver = isReceiver;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public int getInitialParallelism() {
        return initialParallelism;
    }

    public boolean isStateful() {
        return isStateful;
    }

    public boolean isReceiver() {
        return isReceiver;
    }
}

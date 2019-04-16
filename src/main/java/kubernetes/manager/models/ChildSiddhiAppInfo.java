package kubernetes.manager.models;

import java.util.List;

/**
 * Contains details of a child Siddhi app
 */
public class ChildSiddhiAppInfo {
    private String name;
    private String content;
    private List<ResourceRequirement> resourceRequirements;
    private int initialParallelism;
    private boolean isStateful;
    private boolean isReceiver;

    public ChildSiddhiAppInfo(String name,
                              String content,
                              List<ResourceRequirement> resourceRequirements,
                              int initialParallelism,
                              boolean isStateful,
                              boolean isReceiver) {
        this.name = name;
        this.content = content;
        this.resourceRequirements = resourceRequirements;
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

    public List<ResourceRequirement> getResourceRequirements() {
        return resourceRequirements;
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

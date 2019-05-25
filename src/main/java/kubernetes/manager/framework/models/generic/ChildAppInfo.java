package kubernetes.manager.framework.models.generic;

import kubernetes.manager.framework.models.concrete.ResourceRequirement;

import java.util.List;
import java.util.Objects;

public abstract class ChildAppInfo {
    private String name;
    private String content;
    private List<ResourceRequirement> resourceRequirements;
    private int initialParallelism;
    private boolean isStateful;
    private boolean isReceiver;

    public ChildAppInfo(String name,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChildAppInfo that = (ChildAppInfo) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

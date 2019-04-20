package kubernetes.manager.framework.models.concrete;

import java.util.Map;

/**
 * Contains information about a resource requirement of a child Siddhi app
 */
public class ResourceRequirement {
    Map<String, String> labels;

    public ResourceRequirement(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}

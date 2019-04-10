package kubernetes.manager.models;

import java.util.Map;

/**
 * Contains information about a resource requirement for a child Siddhi application (eg: Database)
 */
public class ResourceRequirementInfo {
    Map<String, String> resourceProperties;

    public ResourceRequirementInfo(Map<String, String> resourceProperties) {
        this.resourceProperties = resourceProperties;
    }

    public Map<String, String> getResourceProperties() {
        return resourceProperties;
    }
}

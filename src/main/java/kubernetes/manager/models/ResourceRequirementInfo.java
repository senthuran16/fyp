package kubernetes.manager.models;

import java.util.Map;
import java.util.Objects;

/**
 * Contains information about a resource requirement for a child Siddhi application (eg: Database)
 */
public class ResourceRequirementInfo {
    ResourceType resourceType;
    Map<String, String> resourceProperties;

    public ResourceRequirementInfo(ResourceType resourceType, Map<String, String> resourceProperties) {
        this.resourceType = resourceType;
        this.resourceProperties = resourceProperties;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Map<String, String> getResourceProperties() {
        return resourceProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceRequirementInfo that = (ResourceRequirementInfo) o;
        return resourceType == that.resourceType &&
                resourceProperties.equals(that.resourceProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceProperties);
    }
}

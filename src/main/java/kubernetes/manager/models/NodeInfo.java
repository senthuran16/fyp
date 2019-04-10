package kubernetes.manager.models;

import java.util.List;

/**
 * Contains information about a Node in the Kubernetes cluster
 */
public class NodeInfo {
    String internalIPAddress;
    List<ResourceRequirementInfo> resourceRequirementInfos;

    public NodeInfo(String internalIPAddress, List<ResourceRequirementInfo> resourceRequirementInfos) {
        this.internalIPAddress = internalIPAddress;
        this.resourceRequirementInfos = resourceRequirementInfos;
    }

    public String getInternalIPAddress() {
        return internalIPAddress;
    }

    public List<ResourceRequirementInfo> getResourceRequirementInfos() {
        return resourceRequirementInfos;
    }
}

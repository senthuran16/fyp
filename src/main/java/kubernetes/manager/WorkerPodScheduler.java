package kubernetes.manager;

import kubernetes.manager.models.NodeInfo;
import kubernetes.manager.models.ResourceRequirementInfo;

import java.util.List;

/**
 * Schedules a worker pod based on special resource requirements
 */
public class WorkerPodScheduler {
    public static NodeInfo getSchedulableNode(ResourceRequirementInfo requiredResource, List<NodeInfo> availableNodes) {
        for (NodeInfo node : availableNodes) {
            if (node.getResourceRequirementInfos().contains(requiredResource)) {
                return node;
            }
        }
        // No scheduling required
        return null;
    }
}

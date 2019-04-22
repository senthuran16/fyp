package kubernetes.manager.framework.components.kubernetes.manager.generic.helpers;

import kubernetes.manager.framework.models.generic.ChildAppInfo;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodMetrics;

import java.io.IOException;
import java.util.List;

public interface ManagerHTTPClientInterface <T extends ChildAppInfo> {
    List<DeploymentInfo> updateDeployments(ManagerServiceInfo managerServiceInfo, List<DeploymentInfo> deployments)
        throws IOException;

    List<WorkerPodMetrics> getWorkerPodMetrics(ManagerServiceInfo managerServiceInfo, List<WorkerPodInfo> workerPods)
        throws IOException;

    List<T> getChildAppInfos(ManagerServiceInfo managerServiceInfo, String userDefinedApp) throws IOException;
}

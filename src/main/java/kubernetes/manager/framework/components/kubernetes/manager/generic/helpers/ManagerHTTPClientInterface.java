package kubernetes.manager.framework.components.kubernetes.manager.generic.helpers;

import kubernetes.manager.framework.models.generic.ChildAppInfo;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodMetrics;

import java.io.IOException;
import java.util.List;

/**
 * Communicates with the JobManager
 * @param <T> Type of the streaming application of your Stream Processor
 */
public interface ManagerHTTPClientInterface <T extends ChildAppInfo> {
    /**
     * Updates all the deployments in the given list, by communicating to the Manager
     * @param managerServiceInfo
     * @param deployments
     * @return
     * @throws IOException
     */
    List<DeploymentInfo> updateDeployments(ManagerServiceInfo managerServiceInfo, List<DeploymentInfo> deployments)
        throws IOException;

    /**
     * Gets metrics of all given worker pods
     * @param managerServiceInfo
     * @param workerPods
     * @return
     * @throws IOException
     */
    List<WorkerPodMetrics> getWorkerPodMetrics(ManagerServiceInfo managerServiceInfo, List<WorkerPodInfo> workerPods)
        throws IOException;

    /**
     * Gets child applications from the Manager, with the given user defined application
     * @param managerServiceInfo
     * @param userDefinedApp
     * @return
     * @throws IOException
     */
    List<T> getChildAppInfos(ManagerServiceInfo managerServiceInfo, String userDefinedApp) throws IOException;
}

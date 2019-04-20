package kubernetes.manager.framework.models.concrete;

import kubernetes.manager.framework.models.generic.ChildAppInfo;

/**
 * Contains details of a Deployment, which is responsible for containing a child Siddhi app
 */
public class DeploymentInfo {
    private WorkerPodInfo workerPodInfo;
    private ChildAppInfo childAppInfo;

    public DeploymentInfo(WorkerPodInfo workerPodInfo, ChildAppInfo childAppInfo) {
        this.workerPodInfo = workerPodInfo;
        this.childAppInfo = childAppInfo;
    }

    public WorkerPodInfo getWorkerPodInfo() {
        return workerPodInfo;
    }

    public ChildAppInfo getChildAppInfo() {
        return childAppInfo;
    }
}

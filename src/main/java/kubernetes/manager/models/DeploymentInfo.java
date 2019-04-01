package kubernetes.manager.models;

/**
 * Contains details of a Deployment, which is responsible for containing a child Siddhi app
 */
public class DeploymentInfo {
    private WorkerPodInfo workerPodInfo;
    private ChildSiddhiAppInfo childSiddhiAppInfo;

    public DeploymentInfo(WorkerPodInfo workerPodInfo, ChildSiddhiAppInfo childSiddhiAppInfo) {
        this.workerPodInfo = workerPodInfo;
        this.childSiddhiAppInfo = childSiddhiAppInfo;
    }

    public WorkerPodInfo getWorkerPodInfo() {
        return workerPodInfo;
    }

    public ChildSiddhiAppInfo getChildSiddhiAppInfo() {
        return childSiddhiAppInfo;
    }
}

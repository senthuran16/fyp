package kubernetes.manager.framework.models.concrete;

import kubernetes.manager.framework.models.generic.ChildAppInfo;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeploymentInfo that = (DeploymentInfo) o;
        return workerPodInfo.equals(that.workerPodInfo) &&
                childAppInfo.equals(that.childAppInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerPodInfo, childAppInfo);
    }
}

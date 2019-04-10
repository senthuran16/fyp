package kubernetes.manager.models;

/**
 * Contains information of metrics of a Worker Pod
 */
public class WorkerPodMetrics {
    private WorkerPodInfo workerPodInfo;
    private double value;
    private long time;

    public WorkerPodMetrics(WorkerPodInfo workerPodInfo, double value, long time) {
        this.workerPodInfo = workerPodInfo;
        this.value = value;
        this.time = time;
    }

    public WorkerPodInfo getWorkerPodInfo() {
        return workerPodInfo;
    }

    public double getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }
}

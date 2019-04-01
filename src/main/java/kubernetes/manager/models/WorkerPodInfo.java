package kubernetes.manager.models;

import java.util.Objects;

/**
 * Contains details of a Worker Pod, which is contained by a Deployment, in order to run a child Siddhi app
 */
public class WorkerPodInfo {
    private String name;
    private String ip;
    private String childSiddhiAppName;

    public WorkerPodInfo(String name, String ip, String childSiddhiAppName) {
        this.name = name;
        this.ip = ip;
        this.childSiddhiAppName = childSiddhiAppName;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getChildSiddhiAppName() {
        return childSiddhiAppName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerPodInfo that = (WorkerPodInfo) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

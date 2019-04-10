package kubernetes.manager.models;

import java.util.Objects;

/**
 * Contains details of a Worker Pod, which is contained by a Deployment, in order to run a child Siddhi app
 */
public class WorkerPodInfo {
    private String name;
    private String ip;
    private String childSiddhiAppName;
    private String uid;

    public WorkerPodInfo(String name, String ip, String childSiddhiAppName, String uid) {
        this.name = name;
        this.ip = ip;
        this.childSiddhiAppName = childSiddhiAppName;
        this.uid = uid;
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

    public String getUid() { return uid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerPodInfo that = (WorkerPodInfo) o;
        return name.equals(that.name) &&
                ip.equals(that.ip) &&
                childSiddhiAppName.equals(that.childSiddhiAppName) &&
                uid.equals(that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ip, childSiddhiAppName, uid);
    }
}

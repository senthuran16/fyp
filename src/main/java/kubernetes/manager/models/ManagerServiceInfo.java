package kubernetes.manager.models;

/**
 * Contains information about the Manager Service, which is exposed as a Load Balancer
 */
public class ManagerServiceInfo {
    private String ip;
    private String port;
    private String protocol;

    public ManagerServiceInfo(String ip) {
        this.ip = ip;
        this.port = "9190";
        this.protocol = "http";
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }
}

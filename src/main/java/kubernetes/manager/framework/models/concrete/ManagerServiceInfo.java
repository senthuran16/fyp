package kubernetes.manager.framework.models.concrete;

import kubernetes.manager.constants.ProjectConstants;

/**
 * Contains information about the Manager Service, which is exposed as a Load Balancer
 */
public class ManagerServiceInfo {
    private String ip;
    private String port;
    private String protocol;

    public ManagerServiceInfo(String ip) {
        this.ip = ip;
        this.port = ProjectConstants.MANAGER_SERVICE_PORT;
        this.protocol = ProjectConstants.MANAGER_SERVICE_PROTOCOL;
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

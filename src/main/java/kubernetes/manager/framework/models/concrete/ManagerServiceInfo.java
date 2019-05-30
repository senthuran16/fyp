package kubernetes.manager.framework.models.concrete;

/**
 * Contains information about the Manager Service
 */
public class ManagerServiceInfo {
    private String ip;
    private String port;
    private String protocol;
    private String kafkaIp;
    private String kafkaPort;

    public ManagerServiceInfo(String ip, String port, String protocol) {
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
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

    public String getKafkaIp() {
        return kafkaIp;
    }

    public String getKafkaPort() {
        return kafkaPort;
    }

    public void setKafkaIp(String kafkaIp) {
        this.kafkaIp = kafkaIp;
    }

    public void setKafkaPort(String kafkaPort) {
        this.kafkaPort = kafkaPort;
    }
}

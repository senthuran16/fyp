package kubernetes.manager.constants.mapper;

import java.util.Map;

public class KubernetesConfig {
    public String defaultNamespace;
    public Map<String, String> manager;
    public KubernetesWorkerConfig worker;
    public Map<String, String> qos;
}

package kubernetes.manager.impl.components;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.framework.components.kubernetes.manager.generic.AbstractDeploymentManager;
import kubernetes.manager.impl.components.helpers.ChildSiddhiAppsHandler;
import kubernetes.manager.impl.components.helpers.SiddhiManagerHTTPClient;
import kubernetes.manager.impl.models.ChildSiddhiAppInfo;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains methods to communicate with the Kubernetes cluster, and performs scheduling, spawning and scaling for pods
 */
public class SiddhiDeploymentManager extends AbstractDeploymentManager<ChildSiddhiAppInfo> {
    private static final String SIDDHI_APP_LABEL_KEY = "siddhi-app";
    private static final String APP_LABEL_KEY = "app";
    private static final String NODE_LABEL_KEY = "node";
    private static final String PATTERN_LABEL_KEY = "pattern";
    private static final String PATTERN_LABEL_VALUE = "wso2sp-pattern-distributed";

    public SiddhiDeploymentManager(KubernetesClient kubernetesClient) {
        super(kubernetesClient);
    }

    @Override
    public List<ChildSiddhiAppInfo> getChildAppInfos(ManagerServiceInfo managerServiceInfo,
                                                     String userDefinedSiddhiApp) {
        return new ChildSiddhiAppsHandler().getChildAppInfos(managerServiceInfo, userDefinedSiddhiApp);
    }

    @Override
    public List<DeploymentInfo> updateAppDeployments(ManagerServiceInfo managerServiceInfo,
                                                     List<DeploymentInfo> siddhiAppDeployments)
            throws IOException {
        return new SiddhiManagerHTTPClient().updateDeployments(managerServiceInfo, siddhiAppDeployments);
    }

    @Override
    protected Map<String, String> constructLabels(String childSiddhiAppName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(SIDDHI_APP_LABEL_KEY, childSiddhiAppName);
        labels.put(APP_LABEL_KEY, childSiddhiAppName);
        labels.put(NODE_LABEL_KEY, childSiddhiAppName);
        labels.put(PATTERN_LABEL_KEY, PATTERN_LABEL_VALUE);
        return labels;
    }

    @Override
    protected List<ServicePort> buildWorkerServicePorts() {
        List<ServicePort> servicePorts = new ArrayList<>();
        servicePorts.add(
                new ServicePortBuilder()
                        .withName("http-port-1")
                        .withPort(9090)
                        .withProtocol("TCP")
                        .build());
        servicePorts.add(
                new ServicePortBuilder()
                        .withName("https-port-1")
                        .withPort(9443)
                        .withProtocol("TCP")
                        .build());
        servicePorts.add(
                new ServicePortBuilder()
                        .withName("https-port-2")
                        .withPort(9544)
                        .withProtocol("TCP")
                        .build());
        servicePorts.add(
                new ServicePortBuilder()
                        .withName("https-port-3")
                        .withPort(7070)
                        .withProtocol("TCP")
                        .build());
        servicePorts.add(
                new ServicePortBuilder()
                        .withName("https-port-4")
                        .withPort(7443)
                        .withProtocol("TCP")
                        .build());
        return servicePorts;
    }

    @Override
    protected Container buildWorkerDeploymentContainer(String childSiddhiAppName) {
        return new ContainerBuilder()
                .withImage("senthuran16/wso2sp-worker:4.3.0")
                .withName(childSiddhiAppName)
                .withImagePullPolicy("Always")
                .withCommand("sh", "-c", "sleep 40 && /home/wso2carbon/init.sh")
                .withEnv(
                        new EnvVarBuilder().withName("WSO2_SERVER_PROFILE").withValue("worker").build(),
                        new EnvVarBuilder().withName("OFFSET").withValue("0").build(),
                        new EnvVarBuilder().withName("RECEIVER_NODE").withValue("false").build(),
                        new EnvVarBuilder().withName("NODE_PORT").withValue("9443").build(),
                        new EnvVarBuilder()
                                .withName("NODE_IP")
                                .withNewValueFrom()
                                .withNewFieldRef()
                                .withFieldPath("status.podIP")
                                .endFieldRef()
                                .endValueFrom()
                                .build())
                .withPorts(
                        new ContainerPortBuilder().withContainerPort(9090).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(9443).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(9543).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(9544).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(9711).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(9611).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(7711).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(7611).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(7070).withProtocol("TCP").build(),
                        new ContainerPortBuilder().withContainerPort(7443).withProtocol("TCP").build())
                .withLivenessProbe(
                        new ProbeBuilder()
                                .withNewTcpSocket()
                                .withNewPort(9090)
                                .endTcpSocket()
                                .withInitialDelaySeconds(300)
                                .withPeriodSeconds(20)
                                .build())
                .build();
    }
}

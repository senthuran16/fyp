package kubernetes.manager.components;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.components.helpers.ChildSiddhiAppsHandler;
import kubernetes.manager.components.helpers.SiddhiAppDeployer;
import kubernetes.manager.constants.ProjectConstants;
import kubernetes.manager.models.ChildSiddhiAppInfo;
import kubernetes.manager.models.DeploymentInfo;
import kubernetes.manager.models.ManagerServiceInfo;
import kubernetes.manager.models.ResourceRequirement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains methods to communicate with the Kubernetes cluster, and performs scheduling, spawning and scaling for pods
 */
public class DeploymentManager {
    private static final String SIDDHI_APP_LABEL_KEY = "siddhi-app";
    private static final String APP_LABEL_KEY = "app";
    private static final String NODE_LABEL_KEY = "node";
    private static final String PATTERN_LABEL_KEY = "pattern";
    private static final String PATTERN_LABEL_VALUE = "wso2sp-pattern-distributed";

    private KubernetesClient kubernetesClient;

    public DeploymentManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public List<ChildSiddhiAppInfo> getChildSiddhiAppInfos(String userDefinedSiddhiApp) {
        return ChildSiddhiAppsHandler.getChildSiddhiAppInfos(userDefinedSiddhiApp);
    }

    public void createChildSiddhiAppDeployments(List<ChildSiddhiAppInfo> childSiddhiAppInfos) throws IOException {
        for (ChildSiddhiAppInfo childSiddhiAppInfo : childSiddhiAppInfos) {
            createScalableWorkerDeployment(childSiddhiAppInfo, ProjectConstants.DEFAULT_NAMESPACE);
        }
    }

    private void createScalableWorkerDeployment(ChildSiddhiAppInfo childSiddhiAppInfo, String namespace) {
        System.out.println("Creating scalable worker deployment for child Siddhi app: " + childSiddhiAppInfo.getName());
        // Create Service
        Service service = kubernetesClient.services()
                .inNamespace(namespace)
                .create(buildWorkerService(childSiddhiAppInfo.getName()));
        System.out.println("\tCreated Kubernetes Service"); // TODO log

        // Create Deployment
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .create(buildWorkerDeployment(childSiddhiAppInfo));
        System.out.println("\tCreated Kubernetes Deployment"); // TODO log

        // TODO calculate min,max replicas and targetAverageValue

        // Create Horizontal Pod Autoscaler
        HorizontalPodAutoscaler horizontalPodAutoscaler = kubernetesClient.autoscaling().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .create(buildHorizontalPodAutoscaler(childSiddhiAppInfo.getName(), namespace, 1, 10, "20"));
        System.out.println("\tCreated Horizontal Pod Autoscaler"); //TODO log

        // TODO log
        System.out.println("Created scalable worker deployment for child Siddhi app: " + childSiddhiAppInfo.getName());
    }

    public List<DeploymentInfo> updateSiddhiAppDeployments(ManagerServiceInfo managerServiceInfo,
                                                           List<DeploymentInfo> siddhiAppDeployments)
            throws IOException {
        // TODO implementation in progress
        return SiddhiAppDeployer.updateDeployments(managerServiceInfo, siddhiAppDeployments);
    }

    private Map<String, String> constructLabels(String childSiddhiAppName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(SIDDHI_APP_LABEL_KEY, childSiddhiAppName);
        labels.put(APP_LABEL_KEY, childSiddhiAppName);
        labels.put(NODE_LABEL_KEY, childSiddhiAppName);
        labels.put(PATTERN_LABEL_KEY, PATTERN_LABEL_VALUE);
        return labels;
    }

    private Service buildWorkerService(String childSiddhiAppName) {
        return new ServiceBuilder()
                .withApiVersion("v1")
                .withKind("Service")

                // metadata [BEGIN]
                .withNewMetadata()
                .withName(childSiddhiAppName)
                .withLabels(constructLabels(childSiddhiAppName))
                .endMetadata()
                // metadata [END]

                // spec [BEGIN]
                .withNewSpec()
                // type
                .withType("LoadBalancer")
                // ports
                .withPorts(buildWorkerServicePorts())
                // selector
                .withSelector(constructLabels(childSiddhiAppName))
                .endSpec()
                // spec [END]

                .build();
    }

    private List<ServicePort> buildWorkerServicePorts() {
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

    private Deployment buildWorkerDeployment(ChildSiddhiAppInfo childSiddhiAppInfo) {
        return new DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")

                // metadata [BEGIN]
                .withNewMetadata()
                .withName(childSiddhiAppInfo.getName())
                .withLabels(constructLabels(childSiddhiAppInfo.getName()))
                .endMetadata()
                // metadata [END]

                // spec [BEGIN]
                .withNewSpec()
                // selector
                .withNewSelector()
                .withMatchLabels(constructLabels(childSiddhiAppInfo.getName()))
                .endSelector()

                // strategy
                .withNewStrategy()
                .withType("Recreate")
                .endStrategy()

                // replicas
                .withReplicas(1)

                // template [BEGIN]
                .withNewTemplate()
                // metadata
                .withNewMetadata()
                .withLabels(constructLabels(childSiddhiAppInfo.getName()))
                .endMetadata()
                // spec
                .withSpec(buildPodSpec(childSiddhiAppInfo))
//                .withNewSpec() TODO remove these
//                .withContainers(buildWorkerDeploymentContainer(childSiddhiAppName))
//                .endSpec()
                .endTemplate()
                // template [END]

                .endSpec()
                // spec [END]
                .build();
    }

    private PodSpec buildPodSpec(ChildSiddhiAppInfo childSiddhiAppInfo) {
        if (childSiddhiAppInfo.getResourceRequirements().isEmpty()) {
            return new PodSpecBuilder()
                    .withContainers(buildWorkerDeploymentContainer(childSiddhiAppInfo.getName()))
                    .build();
        } else {
            return new PodSpecBuilder()
                    .withNewAffinity()
                    .withPodAffinity(buildPodAffinity(childSiddhiAppInfo.getResourceRequirements()))
                    .endAffinity()
                    .withContainers(buildWorkerDeploymentContainer(childSiddhiAppInfo.getName()))
                    .build();
        }
    }

    private PodAffinity buildPodAffinity(List<ResourceRequirement> resourceRequirements) {
        return new PodAffinityBuilder()
                .withRequiredDuringSchedulingIgnoredDuringExecution(
                        new PodAffinityTermBuilder()
                                .withLabelSelector(
                                        new LabelSelectorBuilder()
                                                .withMatchLabels(
                                                        getResourceRequirementLabelSelectors(resourceRequirements))
                                                .build())
                                .build())
                .build();
    }

    private Map<String, String> getResourceRequirementLabelSelectors(List<ResourceRequirement> resourceRequirements) {
        Map<String, String> labelSelectors = new HashMap<>();
        for (ResourceRequirement resourceRequirement : resourceRequirements) {
            labelSelectors.putAll(resourceRequirement.getLabels());
        }
        return labelSelectors;
    }

    private Container buildWorkerDeploymentContainer(String childSiddhiAppName) {
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

    private HorizontalPodAutoscaler buildHorizontalPodAutoscaler(
            String deploymentName,
            String deploymentNamespace,
            int minReplicas,
            int maxReplicas,
            String targetAverageValue) {
        return new HorizontalPodAutoscalerBuilder()
                .withApiVersion("autoscaling/v2beta1")
                .withNewMetadata()
                .withName(deploymentName)
                .withNamespace(deploymentNamespace)
                .endMetadata()
                // specs [BEGIN]
                .withNewSpec()
                .withMinReplicas(minReplicas)
                .withMaxReplicas(maxReplicas)
                // scaleTargetRef
                .withNewScaleTargetRef()
                .withApiVersion("apps/v1beta1")
                .withKind("Deployment")
                .withName(deploymentName)
                .endScaleTargetRef()
                // metrics
                .withMetrics(
                        new MetricSpecBuilder()
                                .withType("Pods")
                                .withNewPods()
                                .withMetricName(deploymentName)
                                .withTargetAverageValue(new Quantity(targetAverageValue))
                                .endPods()
                                .build())
                .endSpec()
                // specs [END]
                .build();
    }

    // TODO Old below

//    private void createScalableWorkerDeployment(String childSiddhiAppName, String namespace) {
//        System.out.println("Creating scalable worker deployment for child Siddhi app: " + childSiddhiAppName);
//        // Create Service
//        Service service = kubernetesClient.services()
//                .inNamespace(namespace)
//                .create(buildWorkerService(childSiddhiAppName));
//        System.out.println("\tCreated Kubernetes Service"); // TODO log
//
//        // Create Deployment
//        Deployment deployment = kubernetesClient.apps().deployments()
//                .inNamespace(namespace)
//                .create(buildWorkerDeployment(childSiddhiAppName));
//        System.out.println("\tCreated Kubernetes Deployment"); // TODO log
//
//        // TODO calculate min,max replicas and targetAverageValue
//
//        // Create Horizontal Pod Autoscaler
//        HorizontalPodAutoscaler horizontalPodAutoscaler = kubernetesClient.autoscaling().horizontalPodAutoscalers()
//                .inNamespace(namespace)
//                .create(buildHorizontalPodAutoscaler(childSiddhiAppName, namespace, 1, 10, "20"));
//        System.out.println("\tCreated Horizontal Pod Autoscaler"); //TODO log
//
//        System.out.println("Created scalable worker deployment for child Siddhi app: " + childSiddhiAppName); //TODO log
//    }
//
//    public List<DeploymentInfo> updateSiddhiAppDeployments(ManagerServiceInfo managerServiceInfo,
//                                                           List<DeploymentInfo> siddhiAppDeployments)
//            throws IOException {
//        // TODO implementation in progress
//        return SiddhiAppDeployer.updateDeployments(managerServiceInfo, siddhiAppDeployments);
//    }
//
//    private Map<String, String> constructLabels(String childSiddhiAppName) {
//        Map<String, String> labels = new HashMap<>();
//        labels.put(SIDDHI_APP_LABEL_KEY, childSiddhiAppName);
//        labels.put(APP_LABEL_KEY, childSiddhiAppName);
//        labels.put(NODE_LABEL_KEY, childSiddhiAppName);
//        labels.put(PATTERN_LABEL_KEY, PATTERN_LABEL_VALUE);
//        return labels;
//    }
//
//    private Service buildWorkerService(String childSiddhiAppName) {
//        return new ServiceBuilder()
//                .withApiVersion("v1")
//                .withKind("Service")
//
//                // metadata [BEGIN]
//                .withNewMetadata()
//                .withName(childSiddhiAppName)
//                .withLabels(constructLabels(childSiddhiAppName))
//                .endMetadata()
//                // metadata [END]
//
//                // spec [BEGIN]
//                .withNewSpec()
//                // type
//                .withType("LoadBalancer")
//                // ports
//                .withPorts(buildWorkerServicePorts())
//                // selector
//                .withSelector(constructLabels(childSiddhiAppName))
//                .endSpec()
//                // spec [END]
//
//                .build();
//    }
//
//    private List<ServicePort> buildWorkerServicePorts() {
//        List<ServicePort> servicePorts = new ArrayList<>();
//        servicePorts.add(
//                new ServicePortBuilder()
//                        .withName("http-port-1")
//                        .withPort(9090)
//                        .withProtocol("TCP")
//                        .build());
//        servicePorts.add(
//                new ServicePortBuilder()
//                        .withName("https-port-1")
//                        .withPort(9443)
//                        .withProtocol("TCP")
//                        .build());
//        servicePorts.add(
//                new ServicePortBuilder()
//                        .withName("https-port-2")
//                        .withPort(9544)
//                        .withProtocol("TCP")
//                        .build());
//        servicePorts.add(
//                new ServicePortBuilder()
//                        .withName("https-port-3")
//                        .withPort(7070)
//                        .withProtocol("TCP")
//                        .build());
//        servicePorts.add(
//                new ServicePortBuilder()
//                        .withName("https-port-4")
//                        .withPort(7443)
//                        .withProtocol("TCP")
//                        .build());
//        return servicePorts;
//    }
//
//    private Deployment buildWorkerDeployment(String childSiddhiAppName) {
//        return new DeploymentBuilder()
//                .withApiVersion("apps/v1")
//                .withKind("Deployment")
//
//                // metadata [BEGIN]
//                .withNewMetadata()
//                .withName(childSiddhiAppName)
//                .withLabels(constructLabels(childSiddhiAppName))
//                .endMetadata()
//                // metadata [END]
//
//                // spec [BEGIN]
//                .withNewSpec()
//                // selector
//                .withNewSelector()
//                .withMatchLabels(constructLabels(childSiddhiAppName))
//                .endSelector()
//
//                // strategy
//                .withNewStrategy()
//                .withType("Recreate")
//                .endStrategy()
//
//                // replicas
//                .withReplicas(1)
//
//                // template [BEGIN]
//                .withNewTemplate()
//                // metadata
//                .withNewMetadata()
//                .withLabels(constructLabels(childSiddhiAppName))
//                .endMetadata()
//                // spec
//                .withSpec(buildPodSpec(childSiddhiAppName))
////                .withNewSpec()
////                .withContainers(buildWorkerDeploymentContainer(childSiddhiAppName))
////                .endSpec()
//                .endTemplate()
//                // template [END]
//
//                .endSpec()
//                // spec [END]
//        .build();
//    }
//
//    private PodSpec buildPodSpec(String childSiddhiAppName) {
//        return new PodSpecBuilder()
//                .withContainers(buildWorkerDeploymentContainer(childSiddhiAppName))
//                .build();
//    }
//
//    private Container buildWorkerDeploymentContainer(String childSiddhiAppName) {
//        return new ContainerBuilder()
//                .withImage("senthuran16/wso2sp-worker:4.3.0")
//                .withName(childSiddhiAppName)
//                .withImagePullPolicy("Always")
//                .withCommand("sh", "-c", "sleep 40 && /home/wso2carbon/init.sh")
//                .withEnv(
//                        new EnvVarBuilder().withName("WSO2_SERVER_PROFILE").withValue("worker").build(),
//                        new EnvVarBuilder().withName("OFFSET").withValue("0").build(),
//                        new EnvVarBuilder().withName("RECEIVER_NODE").withValue("false").build(),
//                        new EnvVarBuilder().withName("NODE_PORT").withValue("9443").build(),
//                        new EnvVarBuilder()
//                                .withName("NODE_IP")
//                                .withNewValueFrom()
//                                .withNewFieldRef()
//                                .withFieldPath("status.podIP")
//                                .endFieldRef()
//                                .endValueFrom()
//                                .build())
//                .withPorts(
//                        new ContainerPortBuilder().withContainerPort(9090).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(9443).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(9543).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(9544).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(9711).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(9611).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(7711).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(7611).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(7070).withProtocol("TCP").build(),
//                        new ContainerPortBuilder().withContainerPort(7443).withProtocol("TCP").build())
//                .withLivenessProbe(
//                        new ProbeBuilder()
//                                .withNewTcpSocket()
//                                .withNewPort(9090)
//                                .endTcpSocket()
//                                .withInitialDelaySeconds(300)
//                                .withPeriodSeconds(20)
//                                .build())
//                .build();
//    }
//
//    private HorizontalPodAutoscaler buildHorizontalPodAutoscaler(
//            String deploymentName,
//            String deploymentNamespace,
//            int minReplicas,
//            int maxReplicas,
//            String targetAverageValue) {
//        return new HorizontalPodAutoscalerBuilder()
//                .withApiVersion("autoscaling/v2beta1")
//                .withNewMetadata()
//                .withName(deploymentName)
//                .withNamespace(deploymentNamespace)
//                .endMetadata()
//                // specs [BEGIN]
//                .withNewSpec()
//                .withMinReplicas(minReplicas)
//                .withMaxReplicas(maxReplicas)
//                // scaleTargetRef
//                .withNewScaleTargetRef()
//                .withApiVersion("apps/v1beta1")
//                .withKind("Deployment")
//                .withName(deploymentName)
//                .endScaleTargetRef()
//                // metrics
//                .withMetrics(
//                        new MetricSpecBuilder()
//                                .withType("Pods")
//                                .withNewPods()
//                                .withMetricName(deploymentName)
//                                .withTargetAverageValue(new Quantity(targetAverageValue))
//                                .endPods()
//                                .build())
//                .endSpec()
//                // specs [END]
//                .build();
//    }
}

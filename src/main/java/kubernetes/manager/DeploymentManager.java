package kubernetes.manager;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.exception.KubernetesDeploymentManagerException;
import kubernetes.manager.models.ChildSiddhiAppInfo;
import kubernetes.manager.models.DeploymentInfo;
import kubernetes.manager.models.WorkerPodInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Has functions to communicate with the Kubernetes cluster, and performs scheduling, spawning and scaling for pods
 */
public class DeploymentManager {
    private static final String WORKERS_NAMESPACE = "wso2"; // TODO create this namespace programmatically first

    // Labels
    private static final String SIDDHI_APP_LABEL_KEY = "siddhi-app";
    private static final String APP_LABEL_KEY = "app";
    private static final String NODE_LABEL_KEY = "node";
    private static final String PATTERN_LABEL_KEY = "pattern";
    private static final String PATTERN_LABEL_VALUE = "wso2sp-pattern-distributed";

    private Map<String, ChildSiddhiAppInfo> childSiddhiApps;
    private List<WorkerPodInfo> updatedWorkerPods; // Worker in which, Siddhi apps have been deployed

    private KubernetesClient kubernetesClient;

    public DeploymentManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        updatedWorkerPods = new ArrayList<>();
        childSiddhiApps = new HashMap<>();
    }

    public void createChildSiddhiAppDeployments(List<ChildSiddhiAppInfo> childSiddhiAppInfos) throws IOException {
        for (ChildSiddhiAppInfo childSiddhiAppInfo : childSiddhiAppInfos) {
//            createScalableWorkerDeployment(childSiddhiAppInfo.getName(), WORKERS_NAMESPACE); // TODO uncomment
            childSiddhiApps.put(childSiddhiAppInfo.getName(), childSiddhiAppInfo);
        }
        updateDeployments();
    }

    private void createScalableWorkerDeployment(String childSiddhiAppName, String namespace)
            throws FileNotFoundException {
        // TODO be sure about namespaces
        // Create Service
        Service service = kubernetesClient.services()
                .inNamespace(namespace)
                .create(constructWorkerService(childSiddhiAppName));
        System.out.println("Created Service for child Siddhi app: " + childSiddhiAppName); // TODO log

        // Create Deployment
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .create(constructWorkerDeployment(childSiddhiAppName));
        System.out.println("Created Deployment for child Siddhi app: " + childSiddhiAppName); // TODO log

        // Create Horizontal Pod Autoscaler // TODO uncomment HPA. On hold for now
//        HorizontalPodAutoscaler horizontalPodAutoscaler = kubernetesClient.autoscaling().horizontalPodAutoscalers()
//                .inNamespace(namespace)
//                .create(
//                        constructHorizontalPodAutoscaler(
//                                constructHorizontalPodAutoscalerName(deploymentName), namespace, deploymentName));
//        System.out.println("Created Horizontal Pod Autoscaler: " + horizontalPodAutoscaler); // TODO log
    }

    public void updateDeployments() throws IOException { // TODO implement
        List<WorkerPodInfo> latestPods = getLatestPods();
        if (latestPods.size() > 0) {
            System.out.println(latestPods.size() + " new running pods were found"); // TODO log
            List<DeploymentInfo> siddhiAppDeployments = getSiddhiAppDeployments(latestPods);
            List<DeploymentInfo> failedDeployments = SiddhiAppDeployer.updateDeployments(siddhiAppDeployments);

            // Set updated worker pods with passed deployments
            siddhiAppDeployments.removeAll(failedDeployments);
            if (siddhiAppDeployments.size() > 0) {
                setUpdatedWorkerPods(siddhiAppDeployments);
                System.out.println("Successful deployments Updated"); // TODO log
            } else {
                System.out.println(
                        "No Deployments were successful and no updates were made. Retry will occur at the next cycle");
            }
        } else {
            System.out.println("No new running pods were found"); // TODO log
        }
    }

    private List<WorkerPodInfo> getLatestPods() {
        List<WorkerPodInfo> latestPods = getAllRunningWorkerPods();
        latestPods.removeAll(this.updatedWorkerPods);
        return latestPods;
    }

    private List<WorkerPodInfo> getAllRunningWorkerPods() { // TODO make this private
        List<Pod> pods = kubernetesClient.pods().list().getItems();
        List<WorkerPodInfo> workerPodInfos = new ArrayList<>();
        for (Pod pod : pods) {
            // Filter worker pods that are running
            if (pod.getStatus() != null && pod.getStatus().getPhase().equalsIgnoreCase("Running") &&
                    pod.getMetadata() != null && pod.getMetadata().getLabels() != null &&
                    pod.getMetadata().getLabels().containsKey(SIDDHI_APP_LABEL_KEY)) {
                workerPodInfos.add(
                        new WorkerPodInfo(
                                pod.getMetadata().getName(),
                                pod.getStatus().getPodIP(),
                                pod.getMetadata().getLabels().get(SIDDHI_APP_LABEL_KEY)));
            }
        }
        return workerPodInfos;
    }

    private List<DeploymentInfo> getSiddhiAppDeployments(List<WorkerPodInfo> newPods) {
        List<DeploymentInfo> deployments = new ArrayList<>(); // TODO confirm
        for(WorkerPodInfo workerPodInfo : newPods) {
            deployments.add(new DeploymentInfo(workerPodInfo, getChildSiddhiAppInfo(workerPodInfo)));
        }
        return deployments;
    }

    private ChildSiddhiAppInfo getChildSiddhiAppInfo(WorkerPodInfo workerPodInfo) {
        ChildSiddhiAppInfo childSiddhiAppInfo = childSiddhiApps.get(workerPodInfo.getChildSiddhiAppName());
        if (childSiddhiAppInfo != null) {
            return childSiddhiAppInfo;
        }
        throw new KubernetesDeploymentManagerException(
                "Unable to find child Siddhi app for worker pod: " + workerPodInfo);
    }

    private void setUpdatedWorkerPods(List<DeploymentInfo> passedDeployments) {
        List<WorkerPodInfo> updatedWorkerPodInfos = new ArrayList<>();
        for (DeploymentInfo passedDeployment : passedDeployments) {
            updatedWorkerPodInfos.add(passedDeployment.getWorkerPodInfo());
        }
        this.updatedWorkerPods = updatedWorkerPodInfos;
    }

    private String getChildSiddhiAppName(WorkerPodInfo workerPodInfo) {
        // TODO extract the Siddhi app name from Worker pod uuid
        String[] splitName = workerPodInfo.getName().split("-");
        return splitName[0] + splitName[1] + splitName[2] + splitName[3];
        // TODO might maintain parent Siddhi app name also. Otherwise impossible to extract siddhi app name from pod
    }

    private HasMetadata constructResourceFromFile(String absoluteFilePath) throws FileNotFoundException {
        List<HasMetadata> resources = kubernetesClient.load(new FileInputStream(absoluteFilePath)).get();
        return resources.get(0);
    }

    private Map<String, String> constructLabels(String childSiddhiAppName) {
        Map<String, String> labels = new HashMap<>();
        labels.put(SIDDHI_APP_LABEL_KEY, childSiddhiAppName);
        labels.put(APP_LABEL_KEY, childSiddhiAppName);
        labels.put(NODE_LABEL_KEY, childSiddhiAppName);
        labels.put(PATTERN_LABEL_KEY, PATTERN_LABEL_VALUE);
        return labels;
    }

    private Service constructWorkerService(String childSiddhiAppName) throws FileNotFoundException {
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
                .withPorts(constructWorkerServicePorts())
                // selector
                .withSelector(constructLabels(childSiddhiAppName))
                .endSpec()
                // spec [END]

                .build();
    }

    private List<ServicePort> constructWorkerServicePorts() {
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

    private Deployment constructWorkerDeployment(String childSiddhiAppName) throws FileNotFoundException {
        return new DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")

                // metadata [BEGIN]
                .withNewMetadata()
                .withName(childSiddhiAppName)
                .withLabels(constructLabels(childSiddhiAppName))
                .endMetadata()
                // metadata [END]

                // spec [BEGIN]
                .withNewSpec()
                // selector
                .withNewSelector()
                .withMatchLabels(constructLabels(childSiddhiAppName))
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
                .withLabels(constructLabels(childSiddhiAppName))
                .endMetadata()
                // spec
                .withNewSpec()
                .withContainers(constructWorkerDeploymentContainer(childSiddhiAppName))
                .endSpec()
                .endTemplate()
                // template [END]

                .endSpec()
                // spec [END]
        .build();
    }

    private Container constructWorkerDeploymentContainer(String childSiddhiAppName) {
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

    private HorizontalPodAutoscaler constructHorizontalPodAutoscaler(String name,
                                                                     String namespace,
                                                                     String deploymentName) {
        return new HorizontalPodAutoscalerBuilder()
                .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                .withNewSpec()
                .withNewScaleTargetRef()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withName(deploymentName)
                .endScaleTargetRef()
                .withMinReplicas(1)
                .withMaxReplicas(10)
                .addToMetrics(new MetricSpecBuilder()
                        .withType("Resource")
                        .withNewResource()
                        .withName("cpu")
                        .withTargetAverageUtilization(50)
                        .endResource()
                        .build())
                .endSpec()
                .build();
    }

    // TODO remove if file template is going to be used
    private Deployment getWorkerDeployment(String name) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
//                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()

                .addNewContainer()
                .withName(name)
                .withImage("wso2/wso2sp-worker")
                .withCommand("sh", "-c", "sleep 40 && /home/wso2carbon/init.sh")
                .addNewPort()
                .withContainerPort(80)
                .endPort()
                .endContainer()

                .endSpec()
                .endTemplate()
                .withNewSelector()
                .addToMatchLabels("app", name)
                .endSelector()
                .endSpec()
                .build();
    }
}
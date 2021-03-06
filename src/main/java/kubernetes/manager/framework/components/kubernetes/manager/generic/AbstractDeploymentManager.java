package kubernetes.manager.framework.components.kubernetes.manager.generic;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.constants.ProjectConstants;
import kubernetes.manager.framework.models.generic.ChildAppInfo;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.ResourceRequirement;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles creation of Workers and deployment of streaming applications in Kubernetes environment
 * @param <T> Type of the streaming application of your Stream Processor
 */
public abstract class AbstractDeploymentManager<T extends ChildAppInfo> {
    protected KubernetesClient kubernetesClient;

    public AbstractDeploymentManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    /**
     * Gets child applications for the deployment
     * @param managerServiceInfo
     * @param userDefinedApp
     * @return
     */
    public abstract List<T> getChildAppInfos(ManagerServiceInfo managerServiceInfo, String userDefinedApp);

    /**
     * Gets the next child application that is suitable for deployment.
     * This should be used in cases like different consumer groups when using Kafka.
     * Return the argument itself, if this is not applicable.
     * @param childAppInfo
     * @return
     */
    public abstract T getNextChildAppInfo(T childAppInfo);

    /**
     * Creates a Service, Deployment and HPA in Kubernetes, for all given child applications
     * @param childAppInfos
     */
    public void createChildAppDeployments(List<T> childAppInfos) {
        for (T childAppInfo : childAppInfos) {
            createScalableWorkerDeployment(childAppInfo, ProjectConstants.defaultNamespace);
        }
    }

    protected void createScalableWorkerDeployment(T childAppInfo, String namespace) {
        System.out.println("Creating scalable Worker deployment for child app: " + childAppInfo.getName());
        // Create Service
        Service service = kubernetesClient.services()
                .inNamespace(namespace)
                .create(buildWorkerService(childAppInfo.getName()));
        System.out.println("\tCreated Kubernetes Service");

        // Create Deployment
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .create(buildWorkerDeployment(childAppInfo));
        System.out.println("\tCreated Kubernetes Deployment");

        // Create Horizontal Pod Autoscaler
        HorizontalPodAutoscaler horizontalPodAutoscaler = kubernetesClient.autoscaling().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .create(
                        buildHorizontalPodAutoscaler(
                                childAppInfo.getName(),
                                namespace,
                                ProjectConstants.minReplicas,
                                ProjectConstants.maxReplicas,
                                ProjectConstants.metricsThreshold));
        System.out.println("\tCreated Kubernetes Horizontal Pod Autoscaler");
        System.out.println("Created scalable Worker deployment for child app: " + childAppInfo.getName());
    }

    /**
     * Updates deployments of child applications in Worker pods.
     * Worker pod details are carried within the appDeployments itself
     * @param managerServiceInfo
     * @param appDeployments
     * @return
     * @throws IOException
     */
    public abstract List<DeploymentInfo> updateAppDeployments(ManagerServiceInfo managerServiceInfo,
                                                              List<DeploymentInfo> appDeployments) throws IOException;

    /**
     * Constructs labels for Kubernetes artifacts related to Workers
     * @param childAppName
     * @return
     */
    protected abstract Map<String, String> constructLabels(String childAppName);

    protected Service buildWorkerService(String childAppName) {
        return new ServiceBuilder()
                .withApiVersion("v1")
                .withKind("Service")

                // metadata [BEGIN]
                .withNewMetadata()
                .withName(childAppName)
                .withLabels(constructLabels(childAppName))
                .endMetadata()
                // metadata [END]

                // spec [BEGIN]
                .withNewSpec()
                // type
                .withType("LoadBalancer")
                // ports
                .withPorts(buildWorkerServicePorts())
                // selector
                .withSelector(constructLabels(childAppName))
                .endSpec()
                // spec [END]

                .build();
    }

    protected abstract List<ServicePort> buildWorkerServicePorts();

    protected Deployment buildWorkerDeployment(T childAppInfo) {
        return new DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")

                // metadata [BEGIN]
                .withNewMetadata()
                .withName(childAppInfo.getName())
                .withLabels(constructLabels(childAppInfo.getName()))
                .endMetadata()
                // metadata [END]

                // spec [BEGIN]
                .withNewSpec()
                // selector
                .withNewSelector()
                .withMatchLabels(constructLabels(childAppInfo.getName()))
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
                .withLabels(constructLabels(childAppInfo.getName()))
                .endMetadata()
                // spec
                .withSpec(buildPodSpec(childAppInfo))
                .endTemplate()
                // template [END]

                .endSpec()
                // spec [END]
                .build();
    }

    protected PodSpec buildPodSpec(T childAppInfo) {
        if (childAppInfo.getResourceRequirements() == null || childAppInfo.getResourceRequirements().isEmpty() ||
                childAppInfo.getResourceRequirements().get(0) == null) {
            // No resource requirements - No pod affinity
            return new PodSpecBuilder()
                    .withContainers(buildWorkerDeploymentContainer(childAppInfo.getName()))
                    .build();
        } else {
            // Build with pod affinity
            return new PodSpecBuilder()
                    .withNewAffinity()
                    .withPodAffinity(buildPodAffinity(childAppInfo.getResourceRequirements()))
                    .endAffinity()
                    .withContainers(buildWorkerDeploymentContainer(childAppInfo.getName()))
                    .build();
        }
    }

    protected PodAffinity buildPodAffinity(List<ResourceRequirement> resourceRequirements) {
        return new PodAffinityBuilder()
                .withRequiredDuringSchedulingIgnoredDuringExecution(
                        new PodAffinityTermBuilder()
                                .withLabelSelector(
                                        new LabelSelectorBuilder()
                                                .withMatchLabels(
                                                        getResourceRequirementLabelSelectors(resourceRequirements))
                                                .build())
                                .withTopologyKey("kubernetes.io/hostname")
                                .build())
                .build();
    }

    protected Map<String, String> getResourceRequirementLabelSelectors(List<ResourceRequirement> resourceRequirements) {
        Map<String, String> labelSelectors = new HashMap<>();
        for (ResourceRequirement resourceRequirement : resourceRequirements) {
            if (resourceRequirement != null) {
                labelSelectors.putAll(resourceRequirement.getLabels());
            }
        }
        return labelSelectors;
    }

    /**
     * Creates a worker container of your Stream Processor
     * @param childAppName
     * @return
     */
    protected abstract Container buildWorkerDeploymentContainer(String childAppName);

    protected HorizontalPodAutoscaler buildHorizontalPodAutoscaler(
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
}

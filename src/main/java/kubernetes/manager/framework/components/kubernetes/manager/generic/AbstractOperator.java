package kubernetes.manager.framework.components.kubernetes.manager.generic;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.constants.ProjectConstants;

import kubernetes.manager.framework.components.kubernetes.manager.exception.KubernetesManagerException;
import kubernetes.manager.framework.components.kubernetes.manager.concrete.MetricsManager;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ManagerHTTPClientInterface;
import kubernetes.manager.framework.models.generic.ChildAppInfo;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractOperator<T extends ChildAppInfo> {
    private KubernetesClient kubernetesClient;
    private AbstractDeploymentManager<T> deploymentManager;
    private MetricsManager<T> metricsManager;
    private ManagerHTTPClientInterface<T> managerClient;

    private ManagerServiceInfo managerServiceInfo;
    private Map<String, T> childApps;
    private List<WorkerPodInfo> knownWorkerPods;

    public AbstractOperator(KubernetesClient kubernetesClient,
                            AbstractDeploymentManager<T> deploymentManager,
                            MetricsManager<T> metricsManager,
                            ManagerHTTPClientInterface<T> managerClient) {
        this.kubernetesClient = kubernetesClient;
        this.deploymentManager = deploymentManager;
        this.metricsManager = metricsManager;
        this.managerClient = managerClient;
        this.knownWorkerPods = new ArrayList<>(); // No active Worker pods at the beginning
    }

    public void updateManagerService() {
        ManagerServiceInfo managerServiceInfo = getManagerService();
        while (managerServiceInfo == null) {
            try {
                System.out.println("Unable to find Manager Service. Retrying in 5 seconds.");
                Thread.sleep(5000);
                managerServiceInfo = getManagerService();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.managerServiceInfo = managerServiceInfo;
        System.out.println(
                "Updated Manager Service: " +
                        managerServiceInfo.getProtocol() + "://" +
                        managerServiceInfo.getIp() + ":" +
                        managerServiceInfo.getPort());
    }

    public void initiateChildAppDeployments(String userDefinedApp) {
        // Create deployments
        List<T> childAppInfos = deploymentManager.getChildAppInfos(managerServiceInfo, userDefinedApp);
        deploymentManager.createChildAppDeployments(childAppInfos);

        // Store details about child apps
        Map<String, T> childAppInfoMap = new HashMap<>();
        for (T childAppInfo : childAppInfos) {
            childAppInfoMap.put(childAppInfo.getName(), childAppInfo);
        }
        this.childApps = childAppInfoMap;
        System.out.println("Created scalable Worker deployments for apps:");
        for (String childAppName : childApps.keySet()) {
            System.out.println("\t" + childAppName);
        }
    }

    public void updateAppDeployments() throws IOException {
        List<DeploymentInfo> appDeploymentsToBeUpdated = getDeploymentsToBeUpdated();

        // Get successful deployments and update
        List<DeploymentInfo> failedAppDeployments =
                deploymentManager.updateAppDeployments(managerServiceInfo, appDeploymentsToBeUpdated);
        // Notify about failed child app deployments
        if (!failedAppDeployments.isEmpty()) {
            System.out.println("Child app deployments failed in " + failedAppDeployments.size() + " Worker pods. " +
                    "Will be re-attempted in the next cycle.");
            for (DeploymentInfo failedDeployment : failedAppDeployments) {
                System.out.println("\tChild app: " + failedDeployment.getChildAppInfo().getName() +
                        " in Worker pod: " + failedDeployment.getWorkerPodInfo().getName());
            }
        }
        appDeploymentsToBeUpdated.removeAll(failedAppDeployments); // Only successful deployments will remain

        if (!appDeploymentsToBeUpdated.isEmpty()) {
            // Notify about successful child app deployments
            updateKnownWorkerPods(appDeploymentsToBeUpdated);
            System.out.println(
                    "Child app deployments succeeded in " + appDeploymentsToBeUpdated.size() + " Worker pods.");
            for (DeploymentInfo deployment : appDeploymentsToBeUpdated) {
                System.out.println("\tChild app: " + deployment.getChildAppInfo().getName() +
                        " in Worker pod: " + deployment.getWorkerPodInfo().getName());
            }
        }
//        // Updates as "All the above Deployments were successful" TODO remove. Just there for testing
//        updateKnownWorkerPods(appDeploymentsToBeUpdated);
    }

    public void updateWorkerPodMetrics() throws IOException, InterruptedException {
        metricsManager.updateWorkerPodMetrics(managerClient, managerServiceInfo, knownWorkerPods);
    }

    private ManagerServiceInfo getManagerService() {
        List<Service> services = kubernetesClient.services().list().getItems();
        for (Service service : services) {
            if (service.getMetadata() != null && service.getMetadata().getName() != null &&
                    service.getMetadata().getName().equals(ProjectConstants.managerMetadataName) &&
                    service.getStatus() != null &&
                    service.getStatus().getLoadBalancer().getIngress() != null &&
                    service.getStatus().getLoadBalancer().getIngress().size() == 1 &&
                    service.getStatus().getLoadBalancer().getIngress().get(0).getIp() != null) {
                return new ManagerServiceInfo(service.getStatus().getLoadBalancer().getIngress().get(0).getIp());
            }
        }
        return null;
    }

    private List<DeploymentInfo> getDeploymentsToBeUpdated() {
        return getAppDeployments(getLatestPods());
    }

    private void updateKnownWorkerPods(List<DeploymentInfo> successfulDeployments) {
        for (DeploymentInfo deployment : successfulDeployments) {
            this.knownWorkerPods.add(deployment.getWorkerPodInfo());
        }
    }

    private List<WorkerPodInfo> getLatestPods() {
        List<WorkerPodInfo> runningWorkerPods = getAllRunningWorkerPods();
        runningWorkerPods.removeAll(this.knownWorkerPods);
        if (!runningWorkerPods.isEmpty()) {
            System.out.println(runningWorkerPods.size() + " un-updated Worker pods were detected.");
            for (WorkerPodInfo newlyDetectedPod : runningWorkerPods) {
                System.out.println("\t" + newlyDetectedPod.getName());
            }
        }
        return runningWorkerPods;
    }

    private List<WorkerPodInfo> getAllRunningWorkerPods() {
        List<Pod> pods = kubernetesClient.pods().list().getItems();
        List<WorkerPodInfo> workerPodInfos = new ArrayList<>();
        for (Pod pod : pods) {
            // Filter worker pods that are running
            if (pod.getStatus() != null && pod.getStatus().getPhase().equalsIgnoreCase("Running") &&
                    pod.getMetadata() != null && pod.getMetadata().getLabels() != null &&
                    pod.getMetadata().getLabels().containsKey(ProjectConstants.appLabelKey)) {
                workerPodInfos.add(
                        new WorkerPodInfo(
                                pod.getMetadata().getName(),
                                pod.getStatus().getPodIP(),
                                pod.getMetadata().getLabels().get(ProjectConstants.appLabelKey),
                                pod.getMetadata().getUid()));
            }
        }
        return workerPodInfos;
    }

    private List<DeploymentInfo> getAppDeployments(List<WorkerPodInfo> pods) {
        List<DeploymentInfo> deployments = new ArrayList<>();
        for (WorkerPodInfo workerPodInfo : pods) {
            deployments.add(new DeploymentInfo(workerPodInfo, getChildAppInfo(workerPodInfo)));
        }
        return deployments;
    }

    private T getChildAppInfo(WorkerPodInfo workerPodInfo) {
        T childAppInfo = childApps.get(workerPodInfo.getChildAppName());
        if (childAppInfo != null) {
            return childAppInfo;
        }
        throw new KubernetesManagerException("Unable to find child Siddhi app for worker pod: " + workerPodInfo);
    }

}
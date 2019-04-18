package kubernetes.manager;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.components.DeploymentManager;
import kubernetes.manager.exception.KubernetesManagerException;
import kubernetes.manager.components.MetricsManager;
import kubernetes.manager.models.ChildSiddhiAppInfo;
import kubernetes.manager.models.DeploymentInfo;
import kubernetes.manager.models.ManagerServiceInfo;
import kubernetes.manager.models.WorkerPodInfo;

import java.io.IOException;
import java.util.*;

/**
 * Handles the deployment and dynamic scaling in a Kubernetes cluster
 */
public class Operator {
    private static final String SIDDHI_APP_LABEL_KEY = "siddhi-app";
    private static final String MANAGER_METADATA_NAME = "wso2sp-manager";

    private KubernetesClient kubernetesClient;
    private DeploymentManager deploymentManager;

    private ManagerServiceInfo managerServiceInfo;
    private Map<String, ChildSiddhiAppInfo> childSiddhiApps;
    private List<WorkerPodInfo> knownWorkerPods;

    public Operator(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.deploymentManager = new DeploymentManager(kubernetesClient);
        this.knownWorkerPods = new ArrayList<>(); // No active worker pods at the beginning
    }

    public static void main(String[] args) throws IOException {
        String userDefinedSiddhiApp =
                "@App:name('test-app')\n" +
                "@App:description('Description of the plan')\n" +
                "\n" +
                "define stream InputStreamOne (name string);\n" +
                "define stream InputStreamTwo (name string);\n" +
                "\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamOne(name string);\n" +
                "\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamTwo(name string);\n" +
                "\n" +
                "@info(name='query1')\n" +
                "@dist(execGroup='group-1')\n" +
                "from InputStreamOne\n" +
                "select *\n" +
                "insert into LogStreamOne;\n" +
                "\n" +
                "@info(name='query2')\n" +
                "@dist(execGroup='group-2' ,parallel ='2')\n" +
                "from InputStreamTwo\n" +
                "select *\n" +
                "insert into LogStreamTwo;";

        final Operator operator = new Operator(new DefaultKubernetesClient());
        operator.updateManagerService();
        operator.initiateChildSiddhiAppDeployments(userDefinedSiddhiApp); // TODO uncomment

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    operator.updateSiddhiAppDeployments();
                    operator.updateWorkerPodMetrics();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 5000, 5000);

        System.out.println("Started periodic listening"); // TODO log
    }

    public void updateManagerService() {
        ManagerServiceInfo managerServiceInfo = getManagerService();
        while (managerServiceInfo == null) {
            try {
                System.out.println("Unable to find Manager Service. Retrying in 5 seconds."); // TODO log
                Thread.sleep(5000);
                managerServiceInfo = getManagerService();
            } catch (InterruptedException e) {
                e.printStackTrace(); // TODO log
            }
        }
        this.managerServiceInfo = managerServiceInfo;
        System.out.println(
                "Updated Manager Service: " +
                        managerServiceInfo.getProtocol() + "://" +
                        managerServiceInfo.getIp() + ":" +
                        managerServiceInfo.getPort()); // TODO log
    }

    public void initiateChildSiddhiAppDeployments(String userDefinedSiddhiApp) throws IOException {
        // Create deployments
        List<ChildSiddhiAppInfo> childSiddhiAppInfos = deploymentManager.getChildSiddhiAppInfos(userDefinedSiddhiApp);
        deploymentManager.createChildSiddhiAppDeployments(childSiddhiAppInfos);

        // Store details about child Siddhi apps
        Map<String, ChildSiddhiAppInfo> childSiddhiAppInfoMap = new HashMap<>();
        for (ChildSiddhiAppInfo childSiddhiAppInfo : childSiddhiAppInfos) {
            childSiddhiAppInfoMap.put(childSiddhiAppInfo.getName(), childSiddhiAppInfo);
        }
        this.childSiddhiApps = childSiddhiAppInfoMap;
        System.out.println("Created Worker pods for Siddhi apps:"); // TODO log
        for (String childSiddhiAppName : childSiddhiApps.keySet()) {
            System.out.println("\t" + childSiddhiAppName); // TODO log
        }
    }

    public void updateSiddhiAppDeployments() throws IOException {
        List<DeploymentInfo> siddhiAppDeploymentsToBeUpdated = getDeploymentsToBeUpdated();

        // Get successful deployments and update TODO uncomment
        List<DeploymentInfo> failedSiddhiAppDeployments =
                deploymentManager.updateSiddhiAppDeployments(managerServiceInfo, siddhiAppDeploymentsToBeUpdated);
        siddhiAppDeploymentsToBeUpdated.removeAll(failedSiddhiAppDeployments);
        if (!siddhiAppDeploymentsToBeUpdated.isEmpty()) {
            updateKnownWorkerPods(siddhiAppDeploymentsToBeUpdated);
        }
        // Updates as "All the above Deployments were successful" TODO remove. Just there for testing
//        updateKnownWorkerPods(siddhiAppDeploymentsToBeUpdated);
    }

    public void updateWorkerPodMetrics() throws IOException, InterruptedException {
        MetricsManager.updateWorkerPodMetrics(managerServiceInfo, knownWorkerPods);
    }

    private ManagerServiceInfo getManagerService() {
        List<Service> services = kubernetesClient.services().list().getItems();
        for (Service service : services) {
            if (service.getMetadata() != null && service.getMetadata().getName() != null &&
                    service.getMetadata().getName().equals(MANAGER_METADATA_NAME) &&
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
        return getSiddhiAppDeployments(getLatestPods());
    }

    private void updateKnownWorkerPods(List<DeploymentInfo> successfulDeployments) {
        for (DeploymentInfo deployment : successfulDeployments) {
            this.knownWorkerPods.add(deployment.getWorkerPodInfo());
        }
    }

    private List<WorkerPodInfo> getLatestPods() {
        List<WorkerPodInfo> runningWorkerPods = getAllRunningWorkerPods();
        runningWorkerPods.removeAll(this.knownWorkerPods);
        if (runningWorkerPods.size() > 0) {
            System.out.println(runningWorkerPods.size() + " new worker pods were detected"); // TODO LOG
            for (WorkerPodInfo newlyDetectedPod : runningWorkerPods) {
                System.out.println("\t" + newlyDetectedPod.getName()); // TODO log
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
                    pod.getMetadata().getLabels().containsKey(SIDDHI_APP_LABEL_KEY)) {
                workerPodInfos.add(
                        new WorkerPodInfo(
                                pod.getMetadata().getName(),
                                pod.getStatus().getPodIP(),
                                pod.getMetadata().getLabels().get(SIDDHI_APP_LABEL_KEY),
                                pod.getMetadata().getUid()));
            }
        }
        return workerPodInfos;
    }

    private List<DeploymentInfo> getSiddhiAppDeployments(List<WorkerPodInfo> pods) {
        List<DeploymentInfo> deployments = new ArrayList<>();
        for(WorkerPodInfo workerPodInfo : pods) {
            deployments.add(new DeploymentInfo(workerPodInfo, getChildSiddhiAppInfo(workerPodInfo)));
        }
        return deployments;
    }

    private ChildSiddhiAppInfo getChildSiddhiAppInfo(WorkerPodInfo workerPodInfo) {
        ChildSiddhiAppInfo childSiddhiAppInfo = childSiddhiApps.get(workerPodInfo.getChildSiddhiAppName());
        if (childSiddhiAppInfo != null) {
            return childSiddhiAppInfo;
        }
        throw new KubernetesManagerException(
                "Unable to find child Siddhi app for worker pod: " + workerPodInfo);
    }
}

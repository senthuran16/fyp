package kubernetes.manager.impl.components;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.constants.ProjectConstants;
import kubernetes.manager.framework.components.kubernetes.manager.concrete.MetricsManager;
import kubernetes.manager.framework.components.kubernetes.manager.generic.AbstractOperator;
import kubernetes.manager.impl.components.helpers.SiddhiManagerHTTPClient;
import kubernetes.manager.impl.models.ChildSiddhiAppInfo;

import java.io.*;
import java.util.*;

/**
 * Handles the deployment and dynamic scaling related to WSO2 SP, in Kubernetes cluster
 */
public class Operator extends AbstractOperator<ChildSiddhiAppInfo> {
    public Operator(KubernetesClient kubernetesClient) {
        super(kubernetesClient,
                new SiddhiDeploymentManager(kubernetesClient),
                new MetricsManager<ChildSiddhiAppInfo>(),
                new SiddhiManagerHTTPClient());
    }

    public static void main(String[] args) throws IOException {
        ProjectConstants.loadProjectConstants();
        final Operator operator = new Operator(new DefaultKubernetesClient());
        operator.updateManagerService();
        operator.initiateChildAppDeployments(ProjectConstants.userDefinedApp);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    operator.updateAppDeployments();
                    operator.updateWorkerPodMetrics();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, ProjectConstants.interval, ProjectConstants.interval);

        System.out.println("Started periodic listening");
    }
}

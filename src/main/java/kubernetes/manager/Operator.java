package kubernetes.manager;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.models.ChildSiddhiAppInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

/**
 * Handles the deployment and dynamic scaling in a Kubernetes cluster
 */
public class Operator extends TimerTask {
    KubernetesClient kubernetesClient;
    ChildSiddhiAppsHandler childSiddhiAppsHandler;
    DeploymentManager deploymentManager;

    public Operator() {
        this.kubernetesClient = new DefaultKubernetesClient();
        this.childSiddhiAppsHandler = new ChildSiddhiAppsHandler();
        this.deploymentManager = new DeploymentManager(kubernetesClient);
    }

    public Operator(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.childSiddhiAppsHandler = new ChildSiddhiAppsHandler();
        this.deploymentManager = new DeploymentManager(kubernetesClient);
    }

    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public ChildSiddhiAppsHandler getChildSiddhiAppsHandler() {
        return childSiddhiAppsHandler;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    @Override
    public void run() {

    }

    public static void main(String[] args) throws IOException {
        Operator operator = new Operator(new DefaultKubernetesClient());

        String siddhiApp = "@App:name('test-app')\n" +
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

        List<ChildSiddhiAppInfo> childSiddhiAppInfos =
                operator.childSiddhiAppsHandler.getChildSiddhiAppInfos(siddhiApp);
        operator.deploymentManager.createChildSiddhiAppDeployments(childSiddhiAppInfos);
        operator.deploymentManager.updateDeployments();
        System.out.println("Success!");
    }
}

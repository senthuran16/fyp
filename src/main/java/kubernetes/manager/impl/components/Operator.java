package kubernetes.manager.impl.components;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.framework.components.kubernetes.manager.generic.AbstractOperator;
import kubernetes.manager.impl.models.ChildSiddhiAppInfo;

import java.io.IOException;
import java.util.*;

/**
 * Handles the deployment and dynamic scaling in a Kubernetes cluster
 */
public class Operator extends AbstractOperator<ChildSiddhiAppInfo> {
    public Operator(KubernetesClient kubernetesClient) {
        super(kubernetesClient, new SiddhiDeploymentManager(kubernetesClient));
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
        operator.initiateChildAppDeployments(userDefinedSiddhiApp); // TODO uncomment

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
        timer.schedule(timerTask, 5000, 5000);

        System.out.println("Started periodic listening"); // TODO log
    }
}

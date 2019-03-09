package executors;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Operator.class);

    private KubernetesClient kubernetesClient;
    private PodsManager podsManager;

    public Operator() {
        this.kubernetesClient = new DefaultKubernetesClient();
        this.podsManager = new PodsManager(kubernetesClient);
    }

    public static void main(String[] args) {
        Operator operator = new Operator();
        operator.listNodes();
        System.out.println("");
        System.out.println("");
        operator.listPods();
    }

    private void listNodes() {
        NodeList nodes = kubernetesClient.nodes().list();
        for (Node node : nodes.getItems()) {
            System.out.println(node.getMetadata().getName());
        }
    }

    private void listPods() {
        PodList pods = kubernetesClient.pods().list();
        for (Pod pod : pods.getItems()) {
            System.out.println(pod.getMetadata().getName());
        }
    }
}

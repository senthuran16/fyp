package executors;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Creates and manages Kubernetes pods
 */
public class PodsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PodsManager.class);

    private KubernetesClient kubernetesClient;

    public PodsManager(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public void spawnPod(String fileName, String namespace) throws FileNotFoundException {
        if (isFileValid(fileName)) {
            // Get Kubernetes client's namespace when not given
            if (namespace == null) {
                namespace = kubernetesClient.getNamespace();
            }

            List<HasMetadata> resources = kubernetesClient.load(new FileInputStream(fileName)).get();
            if (resources.isEmpty()) {
                System.err.println("No resources loaded from file: " +fileName); // TODO log
                return;
            }
            HasMetadata resource = resources.get(0);
            if (resource instanceof Pod){
                Pod pod = (Pod) resource;
                System.out.println("Creating pod in namespace " + namespace);
                NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods = kubernetesClient.pods().inNamespace(namespace);
                Pod result = pods.create(pod);
                System.out.println("Created pod " + result.getMetadata().getName());
            } else {
                System.err.println("Loaded resource is not a Pod! " + resource);
            }
        }
    }

    private boolean isFileValid(String fileName) {
        File file = new File(fileName);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist: " + fileName); // TODO log
            return false;
        }
        return true;
    }

}


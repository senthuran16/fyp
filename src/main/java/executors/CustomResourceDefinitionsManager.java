package executors;

import beans.crd.DoneableSiddhiApp;
import beans.crd.SiddhiApp;
import beans.crd.SiddhiAppList;
import beans.crd.SiddhiAppSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomResourceDefinitionsManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomResourceDefinitionsManager.class);

    public static String CRD_GROUP = "siddhi.io";
    public static String CRD_NAME = "siddhiapps." + CRD_GROUP;

    private static boolean logRootPaths = false;

    private static String resourceScope(boolean resourceNamespaced) {
        if (resourceNamespaced) {
            return "Namespaced";
        }
        return "Cluster";
    }

    /**
     * Example of Cluster and Namespaced scoped K8S Custom Resources.
     * To test Cluster scoped resource use "--cluster" as first argument.
     * To test Namespaced resource provide namespace as first argument (namespace must exists in K8S).
     *
     * @param args Either "--cluster" or namespace name.
     */
    public static void main(String[] args) {
        boolean resourceNamespaced = true;
        String namespace = null;
        if (args.length > 0) {
            if ("--cluster".equals(args[0])) { // TODO remove this and make into instantiatable methods
                resourceNamespaced = false;
            } else {
                namespace = args[0];
            }
        }

        try (final KubernetesClient client = new DefaultKubernetesClient()) {
            if (resourceNamespaced) {
                if (namespace == null) {
                    namespace = client.getNamespace();
                }
                if (namespace == null) {
                    System.err.println("No namespace specified and no default defined!");
                    return;
                }

                System.out.println("Using namespace: " + namespace);
            } else {
                System.out.println("Creating cluster scoped resource");
            }

            if (logRootPaths) {
                RootPaths rootPaths = client.rootPaths();
                if (rootPaths != null) {
                    List<String> paths = rootPaths.getPaths();
                    if (paths != null) {
                        System.out.println("Supported API Paths:");
                        for (String path : paths) {
                            System.out.println("    " + path);
                        }
                        System.out.println();
                    }
                }
            }

            CustomResourceDefinition siddhiAppCRD = getCustomResourceDefinition(client, CRD_NAME);

            if (siddhiAppCRD != null) {
                System.out.println("Found CRD: " + siddhiAppCRD.getMetadata().getSelfLink());
            } else {
                siddhiAppCRD = new CustomResourceDefinitionBuilder().
                        withApiVersion("apiextensions.k8s.io/v1beta1").
                        withNewMetadata().withName(CRD_NAME).endMetadata().
                        withNewSpec().withGroup(CRD_GROUP).withVersion("v1").withScope(resourceScope(resourceNamespaced)).
                        withNewNames().withKind("SiddhiApp").withShortNames("siddhiapp").withPlural("siddhiapps").endNames().endSpec().
                        build();

                client.customResourceDefinitions().create(siddhiAppCRD);
                System.out.println("Created CRD " + siddhiAppCRD.getMetadata().getName());
            }

            KubernetesDeserializer.registerCustomKind(CRD_GROUP + "/v1", "SiddhiApp", SiddhiApp.class);

            // lets create a client for the CRD
            NonNamespaceOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
                    Resource<SiddhiApp, DoneableSiddhiApp>> siddhiAppClient =
                    client.customResources(siddhiAppCRD, SiddhiApp.class, SiddhiAppList.class, DoneableSiddhiApp.class);
            if (resourceNamespaced) {
                siddhiAppClient =
                        ((MixedOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp, Resource<SiddhiApp, DoneableSiddhiApp>>) siddhiAppClient).inNamespace(namespace);
            }
            CustomResourceList<SiddhiApp> siddhiAppList = siddhiAppClient.list();
            List<SiddhiApp> items = siddhiAppList.getItems();
            System.out.println("  found " + items.size() + " Siddhi apps");
            for (SiddhiApp item : items) {
                System.out.println("    " + item);
            }

            // Sample Siddhi app data
            String sampleSiddhiAppName = "sample"; // Must be all simple
            String sampleSiddhiAppContent = "<Siddhi app content>";

            SiddhiApp siddhiApp = instantiateCustomResourceDefinition(sampleSiddhiAppName, sampleSiddhiAppContent);

            SiddhiApp created = siddhiAppClient.createOrReplace(siddhiApp);

            System.out.println("Created " + siddhiApp);

            // Create or Replace
//            created.getSpec().setContent("<Updated Siddhi app content>");
//            siddhiAppClient.createOrReplace(created);

            System.out.println("Watching for changes to Siddhi Apps");
            siddhiAppClient.withResourceVersion(created.getMetadata().getResourceVersion()).watch(new Watcher<SiddhiApp>() {
                @Override
                public void eventReceived(Action action, SiddhiApp resource) {
                    System.out.println("==> " + action + " for " + resource);
                    if (resource.getSpec() == null) {
                        logger.error("No Spec for resource " + resource);
                    }
                }

                @Override
                public void onClose(KubernetesClientException cause) {
                }
            });

            System.in.read();

        } catch (KubernetesClientException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void test(KubernetesClient kubernetesClient, String namespace) {
        boolean isResourceNamespaced = false;
        String namespaceScope = namespace;

        if (namespace != null) {
            // Resource is namespaced
            isResourceNamespaced = true;
        }

        if (!isResourceNamespaced) {
            // Cluster scope will be the namespace
            namespaceScope = kubernetesClient.getNamespace();
        }

        if (logRootPaths) {
            RootPaths rootPaths = kubernetesClient.rootPaths();
            if (rootPaths != null) {
                List<String> paths = rootPaths.getPaths();
                if (paths != null) {
                    System.out.println("Supported API Paths:");
                    for (String path : paths) {
                        System.out.println("    " + path);
                    }
                    System.out.println();
                }
            }
        }
    }

    private static CustomResourceDefinition getCustomResourceDefinition(KubernetesClient kubernetesClient,
                                                                     String crdName) {
        CustomResourceDefinitionList crds = kubernetesClient.customResourceDefinitions().list();
        List<CustomResourceDefinition> crdsItems = crds.getItems();
        System.out.println("Found " + crdsItems.size() + " CRD(s)");
        CustomResourceDefinition siddhiAppCRD = null;
        for (CustomResourceDefinition crd : crdsItems) {
            ObjectMeta metadata = crd.getMetadata();
            if (metadata != null) {
                String name = metadata.getName();
                System.out.println("    " + name + " => " + metadata.getSelfLink());
                if (crdName.equals(name)) {
                    return crd;
                }
            }
        }
        return null;
    }

    private static SiddhiApp instantiateCustomResourceDefinition(String name, String content) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name + ".siddhi");
        SiddhiApp siddhiApp = new SiddhiApp();
        siddhiApp.setMetadata(metadata);

        SiddhiAppSpec siddhiAppSpec = new SiddhiAppSpec();
        siddhiAppSpec.setName(name);
        siddhiAppSpec.setContent(content);

        siddhiApp.setSpec(siddhiAppSpec);

        return siddhiApp;
    }

}

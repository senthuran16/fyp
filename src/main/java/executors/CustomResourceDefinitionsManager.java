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

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomResourceDefinitionsManager.class);

    public static final String CRD_GROUP = "siddhi.io";
    public static final String CRD_NAME = "siddhiapps." + CRD_GROUP;

    private String namespace;
    private CustomResourceDefinition siddhiAppCRD;

    private KubernetesClient kubernetesClient;
    private NonNamespaceOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
            Resource<SiddhiApp, DoneableSiddhiApp>> siddhiAppCRDClient;

    public CustomResourceDefinitionsManager(KubernetesClient kubernetesClient, String namespace) {
        this.kubernetesClient = kubernetesClient;
        this.namespace = getValidNamespace(namespace);
        this.siddhiAppCRD = createOrLoadSiddhiAppCRD();
        this.siddhiAppCRDClient = createSiddhiAppCRDClient(siddhiAppCRD, namespace);
    }

    /**
     * Creates a new SiddhiApp, or updates an existing one
     * @param name      Name of the SiddhiApp, without '.siddhi' extension. Must be in lowercase
     * @param content   Body of the SiddhiApp
     */
    public void createOrUpdateSiddhiApp(String name, String content) { // TODO name must be in lowercase
        SiddhiApp siddhiApp = constructSiddhiAppCRDInstance(name, content);
        SiddhiApp createdSiddhiApp = siddhiAppCRDClient.createOrReplace(siddhiApp);
        System.out.println("Created SiddhiApp: " + createdSiddhiApp); // TODO log
    }

    /**
     * Loads existing SiddhiApps
     */
    public void listExistingSiddhiApps() {
        CustomResourceList<SiddhiApp> siddhiAppList = siddhiAppCRDClient.list();
        List<SiddhiApp> items = siddhiAppList.getItems();
        System.out.println("Found " + items.size() + " Siddhi apps"); // TODO log
        for (SiddhiApp item : items) {
            System.out.println("    " + item); // TODO log
        }
    }

    /**
     * Logs root paths of the Kubernetes client
     */
    public void logRootPaths() {
        RootPaths rootPaths = kubernetesClient.rootPaths();
        if (rootPaths != null) {
            List<String> paths = rootPaths.getPaths();
            if (paths != null) {
                System.out.println("Supported API Paths:");
                for (String path : paths) {
                    System.out.println("    " + path); // TODO log
                }
                System.out.println(); // TODO log
            }
        }
    }

    private SiddhiApp constructSiddhiAppCRDInstance(String name, String content) {
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

    private String getValidNamespace(String namespace) {
        if (namespace == null) {
            // Cluster scope
            System.out.println("Using cluster scope"); // TODO log
            return kubernetesClient.getNamespace();
        }
        System.out.println("Using namespace: " + namespace); // TODO log
        return namespace;
    }

    private CustomResourceDefinition createOrLoadSiddhiAppCRD() {
        CustomResourceDefinition siddhiAppCRD = getCustomResourceDefinition();
        if (siddhiAppCRD != null) {
            System.out.println("Found CRD: " + siddhiAppCRD.getMetadata().getSelfLink()); // TODO log
        } else {
            siddhiAppCRD = buildSiddhiAppCRD(namespace != null);
            kubernetesClient.customResourceDefinitions().create(siddhiAppCRD);
            System.out.println("Created CRD: " + siddhiAppCRD.getMetadata().getName()); // TODO log
        }
        KubernetesDeserializer.registerCustomKind(CRD_GROUP + "/v1", "SiddhiApp", SiddhiApp.class);
        return siddhiAppCRD;
    }

    private CustomResourceDefinition getCustomResourceDefinition() {
        CustomResourceDefinitionList crds = kubernetesClient.customResourceDefinitions().list();
        List<CustomResourceDefinition> crdsItems = crds.getItems();
        System.out.println("Found " + crdsItems.size() + " CRD(s)"); // TODO log
        for (CustomResourceDefinition crd : crdsItems) {
            ObjectMeta metadata = crd.getMetadata();
            if (metadata != null) {
                String name = metadata.getName();
                System.out.println("    " + name + " => " + metadata.getSelfLink());
                if (CRD_NAME.equals(name)) {
                    return crd;
                }
            }
        }
        return null;
    }

    private CustomResourceDefinition buildSiddhiAppCRD(boolean isResourceNamespaced) {
        return new CustomResourceDefinitionBuilder().
                withApiVersion("apiextensions.k8s.io/v1beta1").
                withNewMetadata().withName(CRD_NAME).endMetadata().
                withNewSpec().withGroup(CRD_GROUP).withVersion("v1").withScope(getResourceScope(isResourceNamespaced)).
                withNewNames().withKind("SiddhiApp").withShortNames("siddhiapp").withPlural("siddhiapps").endNames().endSpec().
                build();
    }

    private String getResourceScope(boolean resourceNamespaced) {
        if (resourceNamespaced) {
            return "Namespaced";
        }
        return "Cluster";
    }

    private NonNamespaceOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
            Resource<SiddhiApp, DoneableSiddhiApp>> createSiddhiAppCRDClient(CustomResourceDefinition siddhiAppCRD,
                                                                             String namespace) { // TODO simplify
        NonNamespaceOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
                Resource<SiddhiApp, DoneableSiddhiApp>> siddhiAppCRDClient =
                kubernetesClient.customResources(siddhiAppCRD, SiddhiApp.class, SiddhiAppList.class, DoneableSiddhiApp.class);
        if (namespace != null) {
            // Namespaced resource
            siddhiAppCRDClient =
                    ((MixedOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
                            Resource<SiddhiApp, DoneableSiddhiApp>>) siddhiAppCRDClient).inNamespace(namespace);
        }
        return siddhiAppCRDClient;
    }


    /**
     * Example of Cluster and Namespaced scoped K8S Custom Resources.
     * To test Cluster scoped resource use "--cluster" as first argument.
     * To test Namespaced resource provide namespace as first argument (namespace must exists in K8S).
     *
     * @param args Either "--cluster" or namespace name. TODO Refer "watch for changes"
     */
//    public static void main(String[] args) {
//        boolean resourceNamespaced = true;
//        String namespace = null;
//        if (args.length > 0) {
//            if ("--cluster".equals(args[0])) { // TODO remove this and make into instantiatable methods
//                resourceNamespaced = false;
//            } else {
//                namespace = args[0];
//            }
//        }
//
//        try (final KubernetesClient client = new DefaultKubernetesClient()) {
//            if (resourceNamespaced) {
//                if (namespace == null) {
//                    namespace = client.getNamespace();
//                }
//                if (namespace == null) {
//                    System.err.println("No namespace specified and no default defined!");
//                    return;
//                }
//
//                System.out.println("Using namespace: " + namespace);
//            } else {
//                System.out.println("Creating cluster scoped resource");
//            }
//
//            if (logRootPaths) {
//                RootPaths rootPaths = client.rootPaths();
//                if (rootPaths != null) {
//                    List<String> paths = rootPaths.getPaths();
//                    if (paths != null) {
//                        System.out.println("Supported API Paths:");
//                        for (String path : paths) {
//                            System.out.println("    " + path);
//                        }
//                        System.out.println();
//                    }
//                }
//            }
//
//            CustomResourceDefinition siddhiAppCRD = getCustomResourceDefinition(client, CRD_NAME);
//
//            if (siddhiAppCRD != null) {
//                System.out.println("Found CRD: " + siddhiAppCRD.getMetadata().getSelfLink());
//            } else {
//                siddhiAppCRD = new CustomResourceDefinitionBuilder().
//                        withApiVersion("apiextensions.k8s.io/v1beta1").
//                        withNewMetadata().withName(CRD_NAME).endMetadata().
//                        withNewSpec().withGroup(CRD_GROUP).withVersion("v1").withScope(resourceScope(resourceNamespaced)).
//                        withNewNames().withKind("SiddhiApp").withShortNames("siddhiapp").withPlural("siddhiapps").endNames().endSpec().
//                        build();
//
//                client.customResourceDefinitions().create(siddhiAppCRD);
//                System.out.println("Created CRD " + siddhiAppCRD.getMetadata().getName());
//            }
//
//            KubernetesDeserializer.registerCustomKind(CRD_GROUP + "/v1", "SiddhiApp", SiddhiApp.class);
//
//            // lets create a client for the CRD
//            NonNamespaceOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp,
//                    Resource<SiddhiApp, DoneableSiddhiApp>> siddhiAppClient =
//                    client.customResources(siddhiAppCRD, SiddhiApp.class, SiddhiAppList.class, DoneableSiddhiApp.class);
//            if (resourceNamespaced) {
//                siddhiAppClient =
//                        ((MixedOperation<SiddhiApp, SiddhiAppList, DoneableSiddhiApp, Resource<SiddhiApp, DoneableSiddhiApp>>) siddhiAppClient).inNamespace(namespace);
//            }
//
//            CustomResourceList<SiddhiApp> siddhiAppList = siddhiAppClient.list();
//            List<SiddhiApp> items = siddhiAppList.getItems();
//            System.out.println("  found " + items.size() + " Siddhi apps");
//            for (SiddhiApp item : items) {
//                System.out.println("    " + item);
//            }
//
//            // Sample Siddhi app data
//            String sampleSiddhiAppName = "sample"; // Must be all simple
//            String sampleSiddhiAppContent = "<Siddhi app content>";
//
//            SiddhiApp siddhiApp = constructSiddhiAppCRDInstance(sampleSiddhiAppName, sampleSiddhiAppContent);
//
//            SiddhiApp created = siddhiAppClient.createOrReplace(siddhiApp);
//
//            System.out.println("Created " + siddhiApp);
//
//            // Create or Replace
////            created.getSpec().setContent("<Updated Siddhi app content>");
////            siddhiAppClient.createOrReplace(created);
//
//            System.out.println("Watching for changes to Siddhi Apps");
//            siddhiAppClient.withResourceVersion(created.getMetadata().getResourceVersion()).watch(new Watcher<SiddhiApp>() {
//                @Override
//                public void eventReceived(Action action, SiddhiApp resource) {
//                    System.out.println("==> " + action + " for " + resource);
//                    if (resource.getSpec() == null) {
//                        logger.error("No Spec for resource " + resource);
//                    }
//                }
//
//                @Override
//                public void onClose(KubernetesClientException cause) {
//                }
//            });
//
//            System.in.read();
//
//        } catch (KubernetesClientException e) {
//            logger.error(e.getMessage(), e);
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }
}



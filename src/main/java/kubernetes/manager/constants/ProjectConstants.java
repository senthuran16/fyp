package kubernetes.manager.constants;

import kubernetes.manager.constants.mapper.Config;
import kubernetes.manager.constants.mapper.WorkerReceiverPort;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains constants related to the Kubernetes cluster and GKE project
 */
//public class ProjectConstants {
//    // Constants related to GCloud
//    public static final String GCLOUD_PROJECT_ID = "savvy-factor-237205";
//    public static final String GCLOUD_PROJECT_ZONE = "us-east1-b";
//    public static final String GCLOUD_PROJECT_CLUSTER_NAME = "fyp-cluster";
//
//    // Constants related to Kubernetes objects
//    public static final String DEFAULT_NAMESPACE = "default";
//
//    // Constants related to the Manager Service
//    public static final String MANAGER_SERVICE_PORT = "9190";
//    public static final String MANAGER_SERVICE_PROTOCOL = "http";
//    public static final String MANAGER_METADATA_NAME = "wso2sp-manager";
//
//    // Constants related to Worker pods
//    public static final String APP_LABEL_KEY = "siddhi-app";
//}

public class ProjectConstants {
    private static final String CONFIG_FILE_LOCATION = "/home/senthuran/CodeIIT/FYP/impl/config/config.yaml";
    private static final String USER_DEFINED_SIDDHI_APP_FOLDER = "/home/senthuran/CodeIIT/FYP/impl/config/";

    public static String userDefinedSiddhiApp;
    public static long interval;

    // Constants related to GCloud
    public static String gcloudProjectId;
    public static String gcloudProjectZone;
    public static String gcloudProjectClusterName;

    // Constants related to Kubernetes objects
    public static String defaultNamespace;

    // Constants related to the Manager Service and Kafka
    public static String managerMetadataName;
    public static String managerServicePort;
    public static String managerServiceProtocol;
    public static String kafkaMetadataName;
    public static String kafkaPort;

    // Constants related to Worker pods
    public static String appLabelKey;
    public static List<WorkerReceiverPort> receiverPorts;

    // Constants related to Quality of Service
    public static int minReplicas;
    public static int maxReplicas;
    public static String metricsThreshold;

    /**
     * Prevents Instantiation
     */
    private ProjectConstants() {}

    public static void loadProjectConstants() throws IOException {
        loadUserDefinedSiddhiApp();
        loadConfig();
    }

    private static void loadUserDefinedSiddhiApp() throws IOException {
        List<Path> result = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(USER_DEFINED_SIDDHI_APP_FOLDER), "*.siddhi");
        for (Path entry: stream) {
            result.add(entry);
        }
        File siddhiApp = result.get(0).toFile(); // Consider only the first Siddhi app
        BufferedReader bufferedReader = new BufferedReader(new FileReader(siddhiApp));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        userDefinedSiddhiApp = stringBuilder.toString();
    }

    private static void loadConfig() throws FileNotFoundException {
        Constructor constructor = new Constructor(Config.class);
        Yaml yaml = new Yaml(constructor);
        InputStream input = new FileInputStream(new File(CONFIG_FILE_LOCATION));
        Config config = yaml.loadAs(input, Config.class);

        interval = getValidatedInterval(config.interval);
        gcloudProjectId = config.project.get("gcloudProjectId");
        gcloudProjectZone = config.project.get("gcloudProjectZone");
        gcloudProjectClusterName = config.project.get("gcloudProjectClusterName");
        defaultNamespace = config.kubernetes.defaultNamespace;
        managerMetadataName = config.kubernetes.manager.get("managerMetadataName");
        managerServicePort = config.kubernetes.manager.get("managerServicePort");
        managerServiceProtocol = config.kubernetes.manager.get("managerServiceProtocol");
        kafkaMetadataName = config.kubernetes.manager.get("kafkaMetadataName");
        kafkaPort = config.kubernetes.manager.get("kafkaPort");
        appLabelKey = config.kubernetes.worker.appLabelKey;
        receiverPorts = config.kubernetes.worker.receiverPorts;
        minReplicas = Integer.valueOf(config.kubernetes.qos.get("minReplicas"));
        maxReplicas = Integer.valueOf(config.kubernetes.qos.get("maxReplicas"));
        metricsThreshold = config.kubernetes.qos.get("metricsThreshold");
    }

    private static long getValidatedInterval(long interval) {
        if (interval < 5000) {
            return 5000;
        }
        return interval;
    }
}

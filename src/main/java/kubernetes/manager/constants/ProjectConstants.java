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
 * Contains constants related to the project
 */
public class ProjectConstants {
    private static final String CONFIG_FILE_LOCATION = "/home/senthuran/CodeIIT/FYP/impl/config/config.yaml";
    private static final String USER_DEFINED_APP_FOLDER = "/home/senthuran/CodeIIT/FYP/impl/config/";

    public static String userDefinedApp;
    public static String appExtension;
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
        loadConfig();
        loadUserDefinedStreamingApp();
    }

    private static void loadConfig() throws FileNotFoundException {
        Constructor constructor = new Constructor(Config.class);
        Yaml yaml = new Yaml(constructor);
        InputStream input = new FileInputStream(new File(CONFIG_FILE_LOCATION));
        Config config = yaml.loadAs(input, Config.class);

        appExtension = config.appExtension;
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

    private static void loadUserDefinedStreamingApp() throws IOException {
        String extension = "*." + appExtension;
        List<Path> result = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(USER_DEFINED_APP_FOLDER), extension);
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
        userDefinedApp = stringBuilder.toString();
    }

    private static long getValidatedInterval(long interval) {
        if (interval < 5000) {
            return 5000;
        }
        return interval;
    }
}

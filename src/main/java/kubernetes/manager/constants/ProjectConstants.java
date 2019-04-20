package kubernetes.manager.constants;

/**
 * Contains constants related to the Kubernetes cluster and GKE project
 */
public class ProjectConstants {
    // Constants related to GCloud
    public static final String GCLOUD_PROJECT_ID = "savvy-factor-237205";
    public static final String GCLOUD_PROJECT_ZONE = "us-east1-b";
    public static final String GCLOUD_PROJECT_CLUSTER_NAME = "fyp-cluster";

    // Constants related to Kubernetes objects
    public static final String DEFAULT_NAMESPACE = "default";

    // Constants related to the Manager Service
    public static final String MANAGER_SERVICE_PORT = "9190";
    public static final String MANAGER_SERVICE_PROTOCOL = "http";
    public static final String MANAGER_METADATA_NAME = "wso2sp-manager";

    // Constants related to Worker pods
    public static final String APP_LABEL_KEY = "siddhi-app";
}

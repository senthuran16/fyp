package kubernetes.manager.framework.components.kubernetes.manager.concrete;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ManagerHTTPClientInterface;
import kubernetes.manager.framework.models.generic.ChildAppInfo;
import kubernetes.manager.constants.ProjectConstants;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodMetrics;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.*;

/**
 * Contains methods for querying and publishing worker pod metrics for auto scaling purpose
 */
public class MetricsManager<T extends ChildAppInfo> {
    private static final String METRIC_TYPE_PREFIX = "custom.googleapis.com/";
    private static final String METRIC_LABEL_KEY_SIDDHI_APP = "siddhi_app";
    private static final String MONITORED_RESOURCE_TYPE = "gke_container";

    private static final String RESOURCE_LABEL_KEY_PROJECT_ID = "project_id";
    private static final String RESOURCE_LABEL_KEY_POD_ID = "pod_id";
    private static final String RESOURCE_LABEL_KEY_CONTAINER_NAME = "container_name";
    private static final String RESOURCE_LABEL_KEY_ZONE = "zone";
    private static final String RESOURCE_LABEL_KEY_CLUSTER_NAME = "cluster_name";
    private static final String RESOURCE_LABEL_KEY_NAMESPACE_ID = "namespace_id";
    private static final String RESOURCE_LABEL_KEY_INSTANCE_ID = "instance_id";

    private static String projectId;
    private static ProjectName projectName;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static MetricServiceClient metricServiceClient;

    public MetricsManager() {
        try {
            metricServiceClient = MetricServiceClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Exists for testing purposes
    private static void deleteMetricDescriptor(String name) throws IOException {
        final MetricServiceClient client = MetricServiceClient.create();
        MetricDescriptorName metricName = MetricDescriptorName.of(projectId, METRIC_TYPE_PREFIX + name);
        client.deleteMetricDescriptor(metricName);
        System.out.println("Deleted descriptor " + METRIC_TYPE_PREFIX + name);
    }

    public static void main(String[] args) throws IOException {
        projectId = "savvy-factor-237205";
        projectName = ProjectName.of(projectId);
        deleteMetricDescriptor("test-app-group-1-1");
        deleteMetricDescriptor("test-app-group-2-1");
    }

    public void updateWorkerPodMetrics(ManagerHTTPClientInterface<T> managerClient,
                                       ManagerServiceInfo managerServiceInfo,
                                       List<WorkerPodInfo> workerPods)
            throws InterruptedException, IOException {
        projectId = ProjectConstants.GCLOUD_PROJECT_ID;
        projectName = ProjectName.of(projectId);
        List<WorkerPodMetrics> workerPodMetrics = managerClient.getWorkerPodMetrics(managerServiceInfo, workerPods);
        if (workerPodMetrics != null) {
            publishWorkerPodMetrics(workerPodMetrics);
        }
    }

    private void publishWorkerPodMetrics(List<WorkerPodMetrics> allWorkerPodMetrics)
            throws InterruptedException {
        for (WorkerPodMetrics workerPodMetrics : allWorkerPodMetrics) {
            createTimeSeries(workerPodMetrics);
            Thread.sleep(5000);
        }
    }

    private void createTimeSeries(WorkerPodMetrics workerPodMetrics) {
        metricServiceClient.createTimeSeries(
                prepareTimeSeriesRequest(
                        workerPodMetrics.getWorkerPodInfo().getUid(),
                        workerPodMetrics.getWorkerPodInfo().getChildAppName(),
                        workerPodMetrics.getValue(),
                        workerPodMetrics.getTime()));
        // TODO remove
//        System.out.println(
//                "[" + workerPodMetrics.getTime() + "] " +
//                        workerPodMetrics.getWorkerPodInfo().getChildAppName() + "   " +
//                workerPodMetrics.getWorkerPodInfo().getUid() + " - " + workerPodMetrics.getValue());
    }

    private CreateTimeSeriesRequest prepareTimeSeriesRequest(
            String podId, String childSiddhiAppName, double value, long time) {
        TimeSeries timeSeries = TimeSeries.newBuilder()
                .setMetric(prepareMetricDescriptor(childSiddhiAppName))
                .setResource(prepareMonitoredResourceDescriptor(podId))
                .addAllPoints(preparePointList(value, time)) // TODO uncomment
                .build();
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        timeSeriesList.add(timeSeries);

        return CreateTimeSeriesRequest.newBuilder()
                .setName(projectName.toString())
                .addAllTimeSeries(timeSeriesList)
                .build();
    }

    private Metric prepareMetricDescriptor(String childSiddhiAppName) {
        Map<String, String> metricLabels = new HashMap<>();
        metricLabels.put(METRIC_LABEL_KEY_SIDDHI_APP, childSiddhiAppName);
        return Metric.newBuilder()
                .setType(getMetricsType(childSiddhiAppName))
                .putAllLabels(metricLabels)
                .build();
    }

    private String getMetricsType(String childSiddhiAppName) {
        return METRIC_TYPE_PREFIX + childSiddhiAppName;
    }

    private MonitoredResource prepareMonitoredResourceDescriptor(String podId) { // TODO remove the parameters
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put(RESOURCE_LABEL_KEY_PROJECT_ID, projectId);
        resourceLabels.put(RESOURCE_LABEL_KEY_POD_ID, podId);
        resourceLabels.put(RESOURCE_LABEL_KEY_CONTAINER_NAME, "");
        resourceLabels.put(RESOURCE_LABEL_KEY_ZONE, ProjectConstants.GCLOUD_PROJECT_ZONE);
        resourceLabels.put(RESOURCE_LABEL_KEY_CLUSTER_NAME, ProjectConstants.GCLOUD_PROJECT_CLUSTER_NAME);
        resourceLabels.put(RESOURCE_LABEL_KEY_NAMESPACE_ID, "default");
        resourceLabels.put(RESOURCE_LABEL_KEY_INSTANCE_ID, "");

        return MonitoredResource.newBuilder()
                .setType(MONITORED_RESOURCE_TYPE)
                .putAllLabels(resourceLabels)
                .build();
    }

    private List<Point> preparePointList(double value, long time) {
        List<Point> points = new ArrayList<>();
        points.add(prepareDataPoint(value, time));
        return points;
    }

    private Point prepareDataPoint(double value, long currentTime) {
        TimeInterval interval = TimeInterval.newBuilder()
                .setEndTime(Timestamps.fromMillis(currentTime))
                .build();
        TypedValue typedValue = TypedValue.newBuilder()
                .setDoubleValue(value)
                .build();
        return Point.newBuilder()
                .setInterval(interval)
                .setValue(typedValue)
                .build();
    }
}

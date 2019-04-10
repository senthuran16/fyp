package kubernetes.manager.metrics;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.ManagerHTTPClient;
import kubernetes.manager.constants.ProjectConstants;
import kubernetes.manager.models.ManagerServiceInfo;
import kubernetes.manager.models.WorkerPodInfo;
import kubernetes.manager.models.WorkerPodMetrics;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains methods for querying and publishing worker pod metrics for auto scaling purposes
 */
/*
TODO Publish per container (pod), not per deployment.
See https://cloud.google.com/kubernetes-engine/docs/tutorials/custom-metrics-autoscaling
And modify
 */
public class MetricsPublisher {
    private static final String METRIC_TYPE_PREFIX = "custom.googleapis.com/";
    private static final String MONITORED_RESOURCE_TYPE = "gke_container";
    private static final String METRIC_LABEL_KEY_SIDDHI_APP = "siddhi_app";
    private static final String RESOURCE_LABEL_KEY_PROJECT_ID = "project_id";

    private static String projectId;
    private static ProjectName projectName;
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static MetricServiceClient metricServiceClient;

    static {
        try {
            metricServiceClient = MetricServiceClient.create();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createMetricDescriptor(String type) throws IOException {
        // [START monitoring_create_metric]
        // Your Google Cloud Platform project ID
        String metricType = METRIC_TYPE_PREFIX + type;

        final MetricServiceClient client = MetricServiceClient.create();
        ProjectName name = ProjectName.of(projectId);

        MetricDescriptor descriptor = MetricDescriptor.newBuilder()
                .setType(metricType)
                .setDescription("Testing the sample")
                .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
                .setValueType(MetricDescriptor.ValueType.DOUBLE)
                .build();

        CreateMetricDescriptorRequest request = CreateMetricDescriptorRequest.newBuilder()
                .setName(name.toString())
                .setMetricDescriptor(descriptor)
                .build();

        client.createMetricDescriptor(request);
    }

    public static void deleteMetricDescriptor(String name) throws IOException {
        final MetricServiceClient client = MetricServiceClient.create();
        MetricDescriptorName metricName = MetricDescriptorName.of(projectId, METRIC_TYPE_PREFIX + name);
        client.deleteMetricDescriptor(metricName);
        System.out.println("Deleted descriptor " + METRIC_TYPE_PREFIX + name);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        projectId = "savvy-factor-237205";
        projectName = ProjectName.of(projectId);
//        createMetricDescriptor("foobar");
//        deleteMetricDescriptor("test-app-group-1-1");
//        deleteMetricDescriptor("test-app-group-2-1");

        KubernetesClient kubernetesClient = new DefaultKubernetesClient();

        while (true) {
            List<Pod> pods = kubernetesClient.pods().inNamespace("default").list().getItems();
            for (Pod pod : pods) {
                if (pod.getMetadata().getLabels() != null && pod.getMetadata().getLabels().get("siddhi-app") != null) {
                    publishWorkerPodMetrics(
                            new WorkerPodMetrics(
                                    new WorkerPodInfo(
                                            pod.getMetadata().getName(),
                                            pod.getStatus().getPodIP(),
                                            pod.getMetadata().getLabels().get("siddhi-app"),
//                                            "foo",
                                            pod.getMetadata().getUid()),
                                    80,
                                    System.currentTimeMillis()));
                    Thread.sleep(5000);
                }
            }
        }
    }

    public static void publishWorkerPodMetrics(ManagerServiceInfo managerServiceInfo, List<WorkerPodInfo> workerPods)
            throws InterruptedException, IOException {
        projectId = ProjectConstants.GCLOUD_PROJECT_ID; // TODO maybe get this automatically
        projectName = ProjectName.of(projectId);
        List<WorkerPodMetrics> workerPodMetrics = ManagerHTTPClient.getWorkerPodMetrics(managerServiceInfo, workerPods);
        if (workerPodMetrics != null) {
            publishWorkerPodMetrics(workerPodMetrics);
        }
    }

    private static void publishWorkerPodMetrics(List<WorkerPodMetrics> allWorkerPodMetrics)
            throws InterruptedException {
        for (WorkerPodMetrics workerPodMetrics : allWorkerPodMetrics) {
            MetricsPublisher.publishWorkerPodMetrics(workerPodMetrics);
            Thread.sleep(5000);
        }
    }

    private static void publishWorkerPodMetrics(WorkerPodMetrics workerPodMetrics) {
        metricServiceClient.createTimeSeries(
                prepareTimeSeriesRequest(
                        workerPodMetrics.getWorkerPodInfo().getUid(),
                        workerPodMetrics.getWorkerPodInfo().getChildSiddhiAppName(),
                        workerPodMetrics.getValue(),
                        workerPodMetrics.getTime()));
        // TODO remove
        System.out.println(
                "[" + workerPodMetrics.getTime() + "] " +
                        workerPodMetrics.getWorkerPodInfo().getChildSiddhiAppName() + "   " +
                workerPodMetrics.getWorkerPodInfo().getUid() + " - " + workerPodMetrics.getValue());
    }

    private static CreateTimeSeriesRequest prepareTimeSeriesRequest(
            String podId, String childSiddhiAppName, double value, long time) {
        TimeSeries timeSeries = TimeSeries.newBuilder()
                .setMetric(prepareMetricDescriptor(childSiddhiAppName))
                .setResource(prepareMonitoredResourceDescriptor(podId))
                .addAllPoints(preparePointList(value, time))
                .build();
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        timeSeriesList.add(timeSeries);

        return CreateTimeSeriesRequest.newBuilder()
                .setName(projectName.toString())
                .addAllTimeSeries(timeSeriesList)
                .build();
    }

    private static Metric prepareMetricDescriptor(String childSiddhiAppName) {
        Map<String, String> metricLabels = new HashMap<>();
        metricLabels.put(METRIC_LABEL_KEY_SIDDHI_APP, childSiddhiAppName);
        return Metric.newBuilder()
                .setType(getMetricsType(childSiddhiAppName))
                .putAllLabels(metricLabels)
                .build();
    }

    private static String getMetricsType(String childSiddhiAppName) {
        return METRIC_TYPE_PREFIX + childSiddhiAppName;
    }

    private static MonitoredResource prepareMonitoredResourceDescriptor(String podId) { // TODO remove the parameters
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put(RESOURCE_LABEL_KEY_PROJECT_ID, projectId);
        resourceLabels.put("pod_id", podId);
        resourceLabels.put("container_name", "");
        resourceLabels.put("zone", "us-east1-b");
        resourceLabels.put("cluster_name", "fyp-cluster");
        resourceLabels.put("namespace_id", "default");
        resourceLabels.put("instance_id", "");

        return MonitoredResource.newBuilder()
                .setType(MONITORED_RESOURCE_TYPE)
                .putAllLabels(resourceLabels)
                .build();
    }

    private static List<Point> preparePointList(double value, long time) {
        List<Point> points = new ArrayList<>();
        points.add(prepareDataPoint(value, time));
        return points;
    }

    private static Point prepareDataPoint(double value, long currentTime) {
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

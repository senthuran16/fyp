package kubernetes.manager;

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains methods for querying and publishing worker pod metrics for auto scaling purposes
 */
public class MetricsPublisher {
    private static final String METRIC_TYPE_PREFIX = "custom.googleapis.com/worker_pod_metrics/";
    private static final String MONITORED_REESOURCE_TYPE = "global";
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

    public static void publishAllMetrics() {
        // TODO get complete metrics from single GET API call, and use the below methods to publish individually
    }

    public static void publishAllMetrics(Map<String, List<Double>> childAppWiseWorkerPodMetrics) {
        Map<String, Double> childAppWiseAverageMetrics = getChildAppWiseAverageMetrics(childAppWiseWorkerPodMetrics);
        for (Map.Entry<String, Double> childAppAverageMetrics : childAppWiseAverageMetrics.entrySet()) {
            publishMetrics(childAppAverageMetrics.getKey(), childAppAverageMetrics.getValue());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        projectId = "hale-post-233718";
        projectName = ProjectName.of(projectId);
        MetricsPublisher.publishMetrics("test-app1", 130); // TODO frequency prob. Send as batches?

    }

    private static void publishMetrics(String childSiddhiAppName, double value) {
        metricServiceClient.createTimeSeries(prepareTimeSeriesRequest(childSiddhiAppName, value));
    }

    private static Map<String, Double> getChildAppWiseAverageMetrics(
            Map<String, List<Double>> childAppWiseWorkerPodMetrics) {
        Map<String, Double> childAppWiseAverageMetrics = new HashMap<>();
        for (Map.Entry<String, List<Double>> childAppWorkerPodMetrics : childAppWiseWorkerPodMetrics.entrySet()) {
            childAppWiseAverageMetrics.put(
                    childAppWorkerPodMetrics.getKey(),
                    getAverageMetrics(childAppWorkerPodMetrics.getValue()));
        }
        return childAppWiseAverageMetrics;
    }

    private static double getAverageMetrics(List<Double> allPodMetrics) {
        double sum = 0;
        for (Double podMetrics : allPodMetrics) {
            sum += podMetrics;
        }
        return sum / allPodMetrics.size();
    }

    private static CreateTimeSeriesRequest prepareTimeSeriesRequest(String childSiddhiAppName, double value) {
        TimeSeries timeSeries = TimeSeries.newBuilder()
                .setMetric(prepareMetricDescriptor(childSiddhiAppName))
                .setResource(prepareMonitoredResourceDescriptor())
                .addAllPoints(preparePointList(value))
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

    private static MonitoredResource prepareMonitoredResourceDescriptor() {
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put(RESOURCE_LABEL_KEY_PROJECT_ID, projectId);
        return MonitoredResource.newBuilder()
                .setType(MONITORED_REESOURCE_TYPE)
                .putAllLabels(resourceLabels)
                .build();
    }

    private static List<Point> preparePointList(double value) {
        List<Point> points = new ArrayList<>();
        points.add(prepareDataPoint(value, System.currentTimeMillis()));
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

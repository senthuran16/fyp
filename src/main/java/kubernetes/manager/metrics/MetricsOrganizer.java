package kubernetes.manager.metrics;

import kubernetes.manager.models.WorkerPodMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Organizes Pod metrics based on their respective Siddhi applications
 */
public class MetricsOrganizer {
    public static Map<String, Double> getSiddhiAppWiseAverageMetrics(List<WorkerPodMetrics> workerPodMetrics) {
        Map<String, List<Double>> organizedWorkerPodMetrics = organizeByChildSiddhiApps(workerPodMetrics);
        return getChildAppWiseAverageMetrics(organizedWorkerPodMetrics);
    }

    private static Map<String, List<Double>> organizeByChildSiddhiApps(List<WorkerPodMetrics> workerPodMetrics) {
        Map<String, List<Double>> organizedMetrics = new HashMap<>();
        for (final WorkerPodMetrics metrics : workerPodMetrics) {
            String childSiddhiAppName = metrics.getWorkerPodInfo().getChildSiddhiAppName();
            if (organizedMetrics.keySet().contains(childSiddhiAppName)) {
                organizedMetrics.get(childSiddhiAppName).add(metrics.getValue());
            } else {
                List<Double> podMetrics = new ArrayList<>();
                podMetrics.add(metrics.getValue());
                organizedMetrics.put(childSiddhiAppName, podMetrics);
            }
        }
        return organizedMetrics;
    }

    private static Map<String, Double> getChildAppWiseAverageMetrics(
            Map<String, List<Double>> childAppWiseWorkerPodMetrics) {
        Map<String, Double> childAppWiseAverageMetrics = new HashMap<>();
        for (Map.Entry<String, List<Double>> childAppWorkerPodMetrics : childAppWiseWorkerPodMetrics.entrySet()) {
            childAppWiseAverageMetrics.put(
                    childAppWorkerPodMetrics.getKey(),
                    calculateAverageMetrics(childAppWorkerPodMetrics.getValue()));
        }
        return childAppWiseAverageMetrics;
    }

    private static double calculateAverageMetrics(List<Double> allPodMetrics) {
        double sum = 0;
        for (Double podMetrics : allPodMetrics) {
            sum += podMetrics;
        }
        return sum / allPodMetrics.size();
    }
}

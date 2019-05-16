package kubernetes.manager.impl;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodMetrics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class TestSimulator {
    public static WorkerPodMetrics simulateLoadMetrics(WorkerPodInfo workerPodInfo,
                                                long currentTimeMillis,
                                                double minValue,
                                                double maxValue) {
        double value = getRandomValue(minValue, maxValue);
        return new WorkerPodMetrics(workerPodInfo, value, currentTimeMillis);
    }

    public static double getRandomValue(double rangeMin, double rangeMax) {
        return rangeMin + (rangeMax - rangeMin) * new Random().nextDouble();
    }

    public static void appendToFile(String childApp, double totalLoadAverageToTheHPA, int noOfReplicas) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            "/home/senthuran/Desktop/jmeter-test/system-testing/load-avg-vs-replicas/case3-app-a.csv",
                            true));
            writer.newLine();
            writer.write(childApp + ", " + totalLoadAverageToTheHPA + ", " + noOfReplicas);
            writer.close();
            System.out.println(childApp + ", " + totalLoadAverageToTheHPA + ", " + noOfReplicas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

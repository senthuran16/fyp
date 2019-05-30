package kubernetes.manager.impl.components.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ManagerHTTPClientInterface;
import kubernetes.manager.framework.models.concrete.*;
import kubernetes.manager.impl.models.*;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Communicates with the API that is exposed by WSO2 SP Job Manager, for the usage of Kubernetes Manager
 */
public class SiddhiManagerHTTPClient implements ManagerHTTPClientInterface<ChildSiddhiAppInfo> {
    private static OkHttpClient client = new OkHttpClient();

    private static String getBaseUrl(ManagerServiceInfo managerServiceInfo) {
        return managerServiceInfo.getProtocol() + "://" + managerServiceInfo.getIp() + ":" +
                managerServiceInfo.getPort() + "/kubernetes-manager/";
    }

    @Override
    public List<DeploymentInfo> updateDeployments(ManagerServiceInfo managerServiceInfo,
                                                         List<DeploymentInfo> deployments) throws IOException {
        final MediaType TEXT_PLAIN = MediaType.get("text/plain");
        RequestBody body = RequestBody.create(TEXT_PLAIN, new Gson().toJson(deployments));
        Request request = new Request.Builder()
                .url(getBaseUrl(managerServiceInfo) + "worker-pods/deployments")
                .post(body)
                .addHeader("Authorization", Credentials.basic("admin", "admin"))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String failedDeploymentsJson = response.body().string();
                Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
                return new Gson().fromJson(failedDeploymentsJson, listType);
            }
            // All deployments were failed
            return deployments;
        } catch (Exception e) {
            // All deployments were failed
            return deployments;
        }
    }

    @Override
    public List<WorkerPodMetrics> getWorkerPodMetrics(ManagerServiceInfo managerServiceInfo,
                                                      List<WorkerPodInfo> workerPods) throws IOException {
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, new Gson().toJson(workerPods));
        Request request = new Request.Builder()
                .url(getBaseUrl(managerServiceInfo) + "worker-pods/metrics")
                .post(body)
                .addHeader("Authorization", Credentials.basic("admin", "admin"))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String workerPodMetricsJson = response.body().string();
                Type listType = new TypeToken<ArrayList<WorkerPodMetrics>>(){}.getType();
                return new Gson().fromJson(workerPodMetricsJson, listType);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ChildSiddhiAppInfo> getChildAppInfos(ManagerServiceInfo managerServiceInfo,
                                                     String userDefinedSiddhiApp) throws IOException {
        RequestBody body = new FormBody.Builder()
                .add("userDefinedSiddhiApp", userDefinedSiddhiApp)
                .add("kafkaIp", managerServiceInfo.getKafkaIp())
                .add("kafkaPort", managerServiceInfo.getKafkaPort())
                .build();
        Request request = new Request.Builder()
                .url(getBaseUrl(managerServiceInfo) + "siddhi-app")
                .post(body)
                .addHeader("Authorization", Credentials.basic("admin", "admin"))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String childSiddhiAppsJson = response.body().string();
                Type listType = new TypeToken<ArrayList<ChildSiddhiAppInfo>>(){}.getType();
                return new Gson().fromJson(childSiddhiAppsJson, listType);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}

package kubernetes.manager.components.helpers;

import kubernetes.manager.models.DeploymentInfo;
import kubernetes.manager.models.ManagerServiceInfo;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

/**
 * Handles Siddhi app deployment in worker pods
 */
public class SiddhiAppDeployer {
    static OkHttpClient client = new OkHttpClient();

    public static List<DeploymentInfo> updateDeployments(ManagerServiceInfo managerServiceInfo,
                                                         List<DeploymentInfo> deployments) throws IOException {
        return ManagerHTTPClient.updateDeployments(managerServiceInfo, deployments); // TODO test whether this API hits
    }

//    public static List<DeploymentInfo> updateDeployments(List<DeploymentInfo> deployments) throws IOException {
//        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
//
//        RequestBody body = RequestBody.create(JSON, new Gson().toJson(deployments));
//        Request request = new Request.Builder()
//                .url("http://localhost:9190/kubernetes-manager/worker-pods/deployments")
//                .post(body)
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            String failedDeploymentsJson = response.body().string(); // TODO not yet finalized
//            Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
//            return new Gson().fromJson(failedDeploymentsJson, listType);
//        } catch (Exception e) {
//            // TODO log
//            return deployments;
//        }
//    }

    // TODO remove old code
//    public static void updateDeployments(Map<WorkerPodInfo, ChildSiddhiAppInfo> deployments) throws IOException {
//        Request request = new Request.Builder()
//                .url("http://localhost:9190/kubernetes-manager/worker-pods/metrics") // TODO put a BASE_URL
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//
//        Call call = client.newCall(request);
//        Response response = call.execute();
//        Object o = null;
//    }
}

package kubernetes.manager.impl.components.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ManagerHTTPClientInterface;
import kubernetes.manager.framework.models.concrete.DeploymentInfo;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodInfo;
import kubernetes.manager.framework.models.concrete.WorkerPodMetrics;
import kubernetes.manager.impl.models.*;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Communicates with the API that is exposed by the Job Manager, for the usage of Kubernetes Manager
 */
public class SiddhiManagerHTTPClient implements ManagerHTTPClientInterface<ChildSiddhiAppInfo> {
    private static OkHttpClient client = new OkHttpClient();

    private static String getBaseUrl(ManagerServiceInfo managerServiceInfo) {
        return managerServiceInfo.getProtocol() + "://" + managerServiceInfo.getIp() + ":" +
                managerServiceInfo.getPort() + "/kubernetes-manager/worker-pods/";
    }

    public static void main(String[] args) throws IOException {
        String app = "@App:name('test-app')\n" +
                "@App:description('Description of the plan')\n" +
                "\n" +
                "define stream InputStreamOne (name string);\n" +
                "define stream InputStreamTwo (name string);\n" +
                "\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamOne(name string);\n" +
                "\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamTwo(name string);\n" +
                "\n" +
                "@info(name='query1')\n" +
                "@dist(execGroup='group-1')\n" +
                "from InputStreamOne\n" +
                "select *\n" +
                "insert into LogStreamOne;\n" +
                "\n" +
                "@info(name='query2')\n" +
                "@dist(execGroup='group-2' ,parallel ='2')\n" +
                "from InputStreamTwo\n" +
                "select *\n" +
                "insert into LogStreamTwo;";
        test(app);
        Object o = null;
    }

    public static List<DeploymentInfo> test(String siddhiApp) throws IOException {
        final MediaType JSON = MediaType.get("text/plain; charset=utf-8");

        String encodedString =
                new String(Base64.getEncoder().encode(siddhiApp.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);

        RequestBody body = RequestBody.create(JSON, siddhiApp);
        Request request = new Request.Builder()
//                .url("http://35.185.42.143:9190/kubernetes-manager/is-active")
                .url("http://35.231.75.50:9190/kubernetes-manager/siddhi-app")
//                .url("http://localhost:9190/kubernetes-manager/siddhi-app")
//                .url(getBaseUrl(managerServiceInfo) + "deployments")
                .post(body)
//                .get()
                .addHeader("Authorization", Credentials.basic("admin", "admin"))
                .addHeader("Connection", "keep-alive")
                .build();
        try (Response response = client.newCall(request).execute()) {
            String failedDeploymentsJson = response.body().string();
            Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
            return new Gson().fromJson(failedDeploymentsJson, listType);
        } catch (Exception e) { // TODO don't catch Exception e if there is any better method
            // TODO log
//            return null;
            System.out.println(e);
            return null;
        }




//        final MediaType JSON = MediaType.get("text/plain; charset=utf-8");
//
//        String encodedString =
//                new String(Base64.getEncoder().encode(siddhiApp.getBytes(StandardCharsets.UTF_8)),
//                        StandardCharsets.UTF_8);
//
//        RequestBody body = RequestBody.create(JSON, new Gson().toJson(encodedString));
//        Request request = new Request.Builder()
//                .url("http://localhost:9190/kubernetes-manager/siddhi-app")
////                .url(getBaseUrl(managerServiceInfo) + "deployments")
//                .post(body)
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            String failedDeploymentsJson = response.body().string();
//            Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
//            return new Gson().fromJson(failedDeploymentsJson, listType);
//        } catch (Exception e) { // TODO don't catch Exception e if there is any better method
//            // TODO log
//            return null;
//        }
    }

    @Override
    public List<DeploymentInfo> updateDeployments(ManagerServiceInfo managerServiceInfo,
                                                         List<DeploymentInfo> deployments) throws IOException {
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        // TODO implementation might not have been finalized
        RequestBody body = RequestBody.create(JSON, new Gson().toJson(deployments));
        Request request = new Request.Builder()
//                .url("http://localhost:9190/kubernetes-manager/worker-pods/deployments")
                .url(getBaseUrl(managerServiceInfo) + "deployments")
                .post(body)
                .addHeader("Authorization", Credentials.basic("admin", "admin"))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String failedDeploymentsJson = response.body().string();
            Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
            return new Gson().fromJson(failedDeploymentsJson, listType);
        } catch (Exception e) {
            // All deployments were failed
            return deployments;
        }
    }

    @Override
    public List<WorkerPodMetrics> getWorkerPodMetrics(ManagerServiceInfo managerServiceInfo,
                                                      List<WorkerPodInfo> workerPods) throws IOException {
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        List<WorkerPodMetrics> test = new ArrayList<>();
        for (WorkerPodInfo workerPod : workerPods) {
            if (workerPod.getChildAppName().equals("test-app-group-1-1")) {
                test.add(new WorkerPodMetrics(workerPod, 80, System.currentTimeMillis()));
            } else {
                test.add(new WorkerPodMetrics(workerPod, 30, System.currentTimeMillis()));
            }
        }
        return test; // TODO just test. Remove

//        RequestBody body = RequestBody.create(JSON, new Gson().toJson(workerPods));
//
//        // TODO API is not aware about WorkerPodMetric bean. Update that
//        Request request = new Request.Builder()
////                .url("http://localhost:9190/kubernetes-manager/worker-pods/metrics")
//                .url(getBaseUrl(managerServiceInfo) + "metrics")
//                .post(body)
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            String workerPodMetrics = response.body().string();
//            Type listType = new TypeToken<ArrayList<WorkerPodMetrics>>(){}.getType();
//            return new Gson().fromJson(workerPodMetrics, listType);
//        } catch (Exception e) { // TODO don't catch Exception e if there is any better method
//            // TODO hard coded testing below. Remoce
//            List<WorkerPodMetrics> test = new ArrayList<>();
//            for (WorkerPodInfo workerPod : workerPods) {
//                test.add(new WorkerPodMetrics(
//                        workerPods.get(0),
//                        80,
//                        System.currentTimeMillis()));
//            }
//            return test; // TODO just test. Remove
//        }
    }

    @Override
    public List<ChildSiddhiAppInfo> getChildAppInfos(ManagerServiceInfo managerServiceInfo,
                                                     String userDefinedSiddhiApp) throws IOException {
        return getHardCodedChildSiddhiApps(); // TODO fix below
        // TODO moved to job manager [BEGIN]
//        SiddhiTopologyCreator siddhiTopologyCreator = new SiddhiTopologyCreatorImpl();
//        SiddhiTopology siddhiTopology = siddhiTopologyCreator.createTopology(userDefinedSiddhiApp);
        // TODO moved to job manager [END]
//        List<ChildSiddhiAppInfo> childSiddhiAppInfos = new ArrayList<>();
//        for (DeployableSiddhiQueryGroup group : queryGroupList) {
//            Object o = null;
//        }
//        for (SiddhiQueryGroup siddhiQueryGroup : siddhiTopology.getQueryGroupList()) {
//            String siddhiQueryGroupName = siddhiQueryGroup.getName();
//            // TODO in existing impl, query group is adhered to user config. In mine, each member is a new Query Group
//            int queryIndex = 0;
//            for (String query : siddhiQueryGroup.getQueryList()) {
//                childSiddhiAppInfos.add(
//                        new ChildSiddhiAppInfo(
//                                siddhiQueryGroupName + "-" + queryIndex++,
//                                query,
//                                siddhiQueryGroup.getParallelism(),
//                                false, // TODO check isStateful properly
//                                siddhiQueryGroup.isReceiverQueryGroup()));
//            }
//        }
    }

    private static List<ChildSiddhiAppInfo> getHardCodedChildSiddhiApps() { // TODO remove when finalized
        List<ChildSiddhiAppInfo> childSiddhiAppInfos = new ArrayList<>();
        String hardCodedApp1 = "@App:name('test-app-group-1-1') \n" +
//                "@source(type='kafka', topic.list='test-app.InputStreamOne', group.id='test-app-group-1-0', threading.option='single.thread', bootstrap.servers='localhost:9092', @map(type='xml'))" +
                "define stream InputStreamOne (name string);\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamOne(name string);\n" +
                "@info(name='query1')\n" +
                "\n" +
                "from InputStreamOne\n" +
                "select *\n" +
                "insert into LogStreamOne;";

        String hardCodedApp2 = "@App:name('test-app-group-2-1') \n" +
//                "@source(type='kafka', topic.list='test-app.InputStreamTwo', group.id='test-app-group-2', threading.option='single.thread', bootstrap.servers='localhost:9092', @map(type='xml'))" +
                "define stream InputStreamTwo (name string);\n" +
                "@sink(type='log')\n" +
                "define stream LogStreamTwo(name string);\n" +
                "@info(name='query2')\n" +
                "\n" +
                "from InputStreamTwo\n" +
                "select *\n" +
                "insert into LogStreamTwo;";
        childSiddhiAppInfos.add(
                new ChildSiddhiAppInfo(
                        "dummy-passthrough-21746-1",
                        hardCodedApp1,
                        null,
                        1,
                        false,
                        false));
        childSiddhiAppInfos.add( // TODO TEMPORARY. UNCOMMENT THIS
                new ChildSiddhiAppInfo(
                        "dummy-group-1-1",
                        hardCodedApp1,
                        null,
                        1,
                        false,
                        false));
        childSiddhiAppInfos.add( // TODO TEMPORARY. UNCOMMENT THIS
                new ChildSiddhiAppInfo(
                        "dummy-group-2-1",
                        hardCodedApp2,
                        null,
                        1,
                        false,
                        false));

        return childSiddhiAppInfos;


//        List<ChildSiddhiAppInfo> childSiddhiAppInfos = new ArrayList<>();
//        String hardCodedApp1 = "@App:name('test-app-group-1-1') \n" +
////                "@source(type='kafka', topic.list='test-app.InputStreamOne', group.id='test-app-group-1-0', threading.option='single.thread', bootstrap.servers='localhost:9092', @map(type='xml'))" +
//                "define stream InputStreamOne (name string);\n" +
//                "@sink(type='log')\n" +
//                "define stream LogStreamOne(name string);\n" +
//                "@info(name='query1')\n" +
//                "\n" +
//                "from InputStreamOne\n" +
//                "select *\n" +
//                "insert into LogStreamOne;";
//
//        String hardCodedApp2 = "@App:name('test-app-group-2-1') \n" +
////                "@source(type='kafka', topic.list='test-app.InputStreamTwo', group.id='test-app-group-2', threading.option='single.thread', bootstrap.servers='localhost:9092', @map(type='xml'))" +
//                "define stream InputStreamTwo (name string);\n" +
//                "@sink(type='log')\n" +
//                "define stream LogStreamTwo(name string);\n" +
//                "@info(name='query2')\n" +
//                "\n" +
//                "from InputStreamTwo\n" +
//                "select *\n" +
//                "insert into LogStreamTwo;";
//        childSiddhiAppInfos.add(
//                new ChildSiddhiAppInfo(
//                        "test-app-group-1-1",
//                        hardCodedApp1,
//                        null,
//                        1,
//                        false,
//                        false));
//        childSiddhiAppInfos.add( // TODO TEMPORARY. UNCOMMENT THIS
//                new ChildSiddhiAppInfo(
//                        "test-app-group-2-1",
//                        hardCodedApp2,
//                        null,
//                        2,
//                        false,
//                        false));
//        return childSiddhiAppInfos;
    }
}

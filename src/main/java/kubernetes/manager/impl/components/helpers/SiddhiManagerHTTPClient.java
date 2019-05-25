package kubernetes.manager.impl.components.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ManagerHTTPClientInterface;
import kubernetes.manager.framework.models.concrete.*;
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
                managerServiceInfo.getPort() + "/kubernetes-manager/";
    }

//    public static void main(String[] args) throws IOException {
//        String app = "@App:name('test-app')\n" +
//                "@App:description('Description of the plan')\n" +
//                "\n" +
//                "define stream InputStreamOne (name string);\n" +
//                "define stream InputStreamTwo (name string);\n" +
//                "\n" +
//                "@sink(type='log')\n" +
//                "define stream LogStreamOne(name string);\n" +
//                "\n" +
//                "@sink(type='log')\n" +
//                "define stream LogStreamTwo(name string);\n" +
//                "\n" +
//                "@info(name='query1')\n" +
//                "@dist(execGroup='group-1')\n" +
//                "from InputStreamOne\n" +
//                "select *\n" +
//                "insert into LogStreamOne;\n" +
//                "\n" +
//                "@info(name='query2')\n" +
//                "@dist(execGroup='group-2' ,parallel ='2')\n" +
//                "from InputStreamTwo\n" +
//                "select *\n" +
//                "insert into LogStreamTwo;";
//        test(app);
//        Object o = null;
//    }

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
        } catch (Exception e) {
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
//        } catch (Exception e) {
//            return null;
//        }
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

    public static void main(String[] args) throws IOException {
        WorkerPodInfo workerPodInfo = new WorkerPodInfo(
                "test-app-group-1-1",
                "10.60.2.13",
                "test-app-group-1-1",
                "test-app-group-1-1-57bcf8cf58-g242p");

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
        ChildSiddhiAppInfo childSiddhiAppInfo = new ChildSiddhiAppInfo(
                "test-app-group-1-1",
                hardCodedApp1,
                null,
                1,
                false,
                false);
        List<DeploymentInfo> deployments = new ArrayList<>();
        deployments.add(
                new DeploymentInfo(
                        workerPodInfo,
                        childSiddhiAppInfo));

        ManagerServiceInfo managerServiceInfo = new ManagerServiceInfo("34.74.67.87");

        List<DeploymentInfo> failed = new SiddhiManagerHTTPClient().updateDeployments(managerServiceInfo, deployments);


//        final MediaType JSON = MediaType.get("text/plain");
//        RequestBody body = RequestBody.create(JSON, new Gson().toJson(deployments));
//        Request request = new Request.Builder()
//                .url("http://localhost:9190/kubernetes-manager/worker-pods/deployments")
//                .post(body)
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            String failedDeploymentsJson = response.body().string();
//            Type listType = new TypeToken<ArrayList<DeploymentInfo>>(){}.getType();
//            List<DeploymentInfo> failed = new Gson().fromJson(failedDeploymentsJson, listType);
//            Object o = null;
//        } catch (Exception e) {
//            // All deployments were failed
//            Object o = null;
//        }
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

//         TODO Remove hardcoded begin
//        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
//
//        List<WorkerPodMetrics> test = new ArrayList<>();
//        for (WorkerPodInfo workerPod : workerPods) {
//            if (workerPod.getChildAppName().equals("test-app-group-1-1")) {
//                test.add(new WorkerPodMetrics(workerPod, 80, System.currentTimeMillis()));
//            } else {
//                test.add(new WorkerPodMetrics(workerPod, 30, System.currentTimeMillis()));
//            }
//        }
//        return test;
//        TODO hardcoded end
    }

    @Override
    public List<ChildSiddhiAppInfo> getChildAppInfos(ManagerServiceInfo managerServiceInfo,
                                                     String userDefinedSiddhiApp) throws IOException {
        return getHardCodedChildSiddhiApps(); // TODO this is just hard coded. Below is the original

//        final MediaType TEXT_PLAIN = MediaType.get("text/plain");
//        RequestBody body = RequestBody.create(TEXT_PLAIN, userDefinedSiddhiApp);
//        Request request = new Request.Builder()
//                .url(getBaseUrl(managerServiceInfo) + "siddhi-app")
//                .post(body)
//                .addHeader("Authorization", Credentials.basic("admin", "admin"))
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            if (response.body() != null) {
//                String childSiddhiAppsJson = response.body().string();
//                Type listType = new TypeToken<ArrayList<ChildSiddhiAppInfo>>(){}.getType();
//                return new Gson().fromJson(childSiddhiAppsJson, listType);
//            }
//            return Collections.emptyList();
//        } catch (Exception e) {
//            return Collections.emptyList();
//        }
    }

    private static List<ChildSiddhiAppInfo> getHardCodedChildSiddhiApps() { // TODO remove when finalized
        List<ChildSiddhiAppInfo> childSiddhiAppInfos = new ArrayList<>();
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
//                        "dummy-passthrough-21746-1",
//                        hardCodedApp1,
//                        null,
//                        1,
//                        false,
//                        false));

        ///////////////////////////////////////////////////////////////

        String hardCodedApp1 = "@App:name('simple-group-1-1') \n" +
                "@Source(type='http',\n" +
                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
                "\tbasic.auth.enabled='false',\n" +
                "\t@map(type='json')) \n" +
                "define stream allStream(name string, amount double);\n" +
                "@sink(type='log')\n" +
                "define stream greaterThanFifty(name string, amount double);\n" +
                "@info(name='greaterThanFifty')\n" +
                "\n" +
                "from allStream[amount > 50]\n" +
                "select *\n" +
                "insert into greaterThanFifty;";
        String hardCodedApp2 = "@App:name('simple-group-2-1') \n" +
                "@Source(type='http',\n" +
                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
                "\tbasic.auth.enabled='false',\n" +
                "\t@map(type='json')) \n" +
                "define stream allStream(name string, amount double);\n" +
                "@store(type='rdbms',\n" +
                "\tjdbc.url='jdbc:mysql//localhost:3306/simple',\n" +
                "\tusername='root',\n" +
                "\tpassword='',\n" +
                "\tjdbc.driver.name='com.mysql.jdbc.Driver')\n" +
                "define table lessThanFifty(name string, amount double);\n" +
                "@info(name='lessThanFifty')\n" +
                "\n" +
                "from allStream[amount < 50]\n" +
                "select *\n" +
                "insert into lessThanFifty;";
        childSiddhiAppInfos.add(
                new ChildSiddhiAppInfo(
                        "simple-group-1-1",
                        hardCodedApp1,
                        null,
                        1,
                        false,
                        false));
//        childSiddhiAppInfos.add(
//                new ChildSiddhiAppInfo(
//                        "simple-group-2-1",
//                        hardCodedApp2,
//                        null,
//                        1,
//                        false,
//                        false));

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
//        childSiddhiAppInfos.add(
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

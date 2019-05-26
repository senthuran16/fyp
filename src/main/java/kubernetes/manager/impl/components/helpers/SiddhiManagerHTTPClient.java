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
        return getHardCodedChildSiddhiApps();
//        RequestBody body = new FormBody.Builder()
//                .add("userDefinedSiddhiApp", userDefinedSiddhiApp)
//                .add("kafkaIp", managerServiceInfo.getKafkaIp())
//                .add("kafkaPort", managerServiceInfo.getKafkaPort())
//                .build();
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

//        String hardCodedApp1 = "@App:name('simple-group-1-1') \n" +
//                "@Source(type='http',\n" +
//                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
//                "\tbasic.auth.enabled='false',\n" +
//                "\t@map(type='json')) \n" +
//                "define stream allStream(name string, amount double);\n" +
//                "@sink(type='log')\n" +
//                "define stream greaterThanFifty(name string, amount double);\n" +
//                "@info(name='greaterThanFifty')\n" +
//                "\n" +
//                "from allStream[amount > 50]\n" +
//                "select *\n" +
//                "insert into greaterThanFifty;";
//        String hardCodedApp2 = "@App:name('simple-group-2-1') \n" +
//                "@Source(type='http',\n" +
//                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
//                "\tbasic.auth.enabled='false',\n" +
//                "\t@map(type='json')) \n" +
//                "define stream allStream(name string, amount double);\n" +
//                "@store(type='rdbms',\n" +
//                "\tjdbc.url='jdbc:mysql//localhost:3306/simple',\n" +
//                "\tusername='root',\n" +
//                "\tpassword='',\n" +
//                "\tjdbc.driver.name='com.mysql.jdbc.Driver')\n" +
//                "define table lessThanFifty(name string, amount double);\n" +
//                "@info(name='lessThanFifty')\n" +
//                "\n" +
//                "from allStream[amount < 50]\n" +
//                "select *\n" +
//                "insert into lessThanFifty;";
//        childSiddhiAppInfos.add(
//                new ChildSiddhiAppInfo(
//                        "simple-group-1-1",
//                        hardCodedApp1,
//                        null,
//                        1,
//                        false,
//                        false));
////        childSiddhiAppInfos.add(
////                new ChildSiddhiAppInfo(
////                        "simple-group-2-1",
////                        hardCodedApp2,
////                        null,
////                        1,
////                        false,
////                        false));
//
//        return childSiddhiAppInfos;


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

        // TODO important below

//        String hardCodedApp1 = "@App:name('test-group-1-1') \n" +
//                "@Source(type='http',\n" +
//                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
//                "\tbasic.auth.enabled='false',\n" +
//                "\t@map(type='json')) \n" +
//                "define stream allStream(name string, amount double);\n" +
//                "@sink(type='kafka',\n" +
////                "      topic='sweet_productions',\n" +
//                "      topic='senthuran',\n" +
//                "      bootstrap.servers='35.243.252.98:9092',\n" +
//                "      @map(type='xml'))\n" +
//                "define stream greaterThanFifty(name string, amount double);\n" +
//                "@info(name='greaterThanFifty')\n" +
//                "\n" +
//                "from allStream[amount > 50]\n" +
//                "select *\n" +
//                "insert into greaterThanFifty;";
//
//        String hardCodedApp2 = "@App:name('test-group-1-2') \n" +
////                "@source(type='kafka', topic.list='sweet_productions', group.id='test-app-group-1-0', threading.option='single.thread', bootstrap.servers='35.243.252.98:9092', @map(type='xml'))" +
//                "@source(type='kafka', topic.list='senthuran', group.id='senthu', threading.option='single.thread', bootstrap.servers='35.243.252.98:9092', @map(type='xml'))" +
//                "define stream InputStreamOne (name string, amount double);\n" +
//                "@sink(type='log')\n" +
//                "define stream LogStreamOne(name string, amount double);\n" +
//                "@info(name='query1')\n" +
//                "\n" +
//                "from InputStreamOne\n" +
//                "select *\n" +
//                "insert into LogStreamOne;";

//        String hardCodedApp1 = "@App:name('test-group-1-1') \n" +
//                "@Source(type='http',\n" +
//                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
//                "\tbasic.auth.enabled='false',\n" +
//                "\t@map(type='json'))\n" +
//                "define stream passthroughallStream(name string, amount double);\n" +
//                "@sink(type='kafka', topic='simple.allStream' , bootstrap.servers='35.243.252.98:9092', @map(type='xml')) \n" +
//                "define stream allStream(name string, amount double);\n" +
//                "from passthroughallStream select * insert into allStream;";
//
//        String hardCodedApp2 = "@App:name('test-group-1-2') \n" +
////                "@source(type='kafka', topic.list='sweet_productions', group.id='test-app-group-1-0', threading.option='single.thread', bootstrap.servers='35.243.252.98:9092', @map(type='xml'))" +
//                "@source(type='kafka', topic.list='simple.allStream', group.id='simple-group-1-0', threading.option='single.thread', bootstrap.servers='35.243.252.98:9092', @map(type='xml')) \n" +
//                "define stream allStream(name string, amount double);\n" +
//                "@sink(type='log')\n" +
//                "define stream greaterThanFifty(name string, amount double);\n" +
//                "@info(name='greaterThanFifty')\n" +
//                "\n" +
//                "from allStream[amount > 50]\n" +
//                "select *\n" +
//                "insert into greaterThanFifty;";
//
//        childSiddhiAppInfos.add(
//                new ChildSiddhiAppInfo(
//                        "test-group-1-1",
//                        hardCodedApp1,
//                        null,
//                        1,
//                        false,
//                        false));
//        childSiddhiAppInfos.add(
//                new ChildSiddhiAppInfo(
//                        "test-group-1-2",
//                        hardCodedApp2,
//                        null,
//                        1,
//                        false,
//                        false));
//
//        return childSiddhiAppInfos;

        String hardCodedApp1 = "@App:name('simple-passthrough-34600-1') \n" +
                "@Source(type='http',\n" +
                "\treceiver.url='http://0.0.0.0:8006/productionStream',\n" +
                "\tbasic.auth.enabled='false',\n" +
                "\t@map(type='json'))\n" +
                "define stream passthroughallStream(name string, amount double);\n" +
                "@sink(type='kafka', topic='simple.allStream' , bootstrap.servers='104.196.11.24:9092', @map(type='xml')) \n" +
                "define stream allStream(name string, amount double);\n" +
                "from passthroughallStream select * insert into allStream;";

        String hardCodedApp2 = "@App:name('simple-group-1-1') \n" +
                "@source(type='kafka', topic.list='simple.allStream', group.id='simple-group-1-0', threading.option='single.thread', bootstrap.servers='104.196.11.24:9092', @map(type='xml')) \n" +
                "define stream allStream(name string, amount double);\n" +
                "@sink(type='log')\n" +
                "define stream greaterThanFifty(name string, amount double);\n" +
                "@info(name='greaterThanFifty')\n" +
                "\n" +
                "from allStream[amount > 50]\n" +
                "select *\n" +
                "insert into greaterThanFifty;";

        String hardCodedApp3 = "@App:name('simple-group-1-1') \n" +
                "@source(type='kafka', topic.list='simple.allStream', group.id='simple-group-1-0-0', threading.option='single.thread', bootstrap.servers='104.196.11.24:9092', @map(type='xml')) \n" +
                "define stream allStream(name string, amount double);\n" +
                "@sink(type='log')\n" +
                "define stream greaterThanFifty(name string, amount double);\n" +
                "@info(name='greaterThanFifty')\n" +
                "\n" +
                "from allStream[amount > 50]\n" +
                "select *\n" +
                "insert into greaterThanFifty;";

        childSiddhiAppInfos.add(
                new ChildSiddhiAppInfo(
                        "simple-passthrough-34600-1",
                        hardCodedApp1,
                        null,
                        1,
                        false,
                        false));
        childSiddhiAppInfos.add(
                new ChildSiddhiAppInfo(
                        "simple-group-1-1-pod1",
                        hardCodedApp2,
                        null,
                        1,
                        false,
                        false));
        childSiddhiAppInfos.add(
                new ChildSiddhiAppInfo(
                        "simple-group-1-1-pod2",
                        hardCodedApp3,
                        null,
                        1,
                        false,
                        false));

        return childSiddhiAppInfos;
    }
}

package kubernetes.manager.components.helpers;

import kubernetes.manager.models.ChildSiddhiAppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods for handling child Siddhi app related actions
 */
public class ChildSiddhiAppsHandler { // TODO THIS SHOULD CALL THE API
    public static List<ChildSiddhiAppInfo> getChildSiddhiAppInfos(String userDefinedSiddhiApp) {
        return getHardCodedChildSiddhiApps(); // TODO fix below
//        SiddhiTopologyCreator siddhiTopologyCreator = new SiddhiTopologyCreatorImpl();
//        SiddhiTopology siddhiTopology = siddhiTopologyCreator.createTopology(userDefinedSiddhiApp);

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
                        "test-app-group-1-1",
                        hardCodedApp1,
                        null,
                        1,
                        isChildAppStateful(hardCodedApp1),
                        false));
        childSiddhiAppInfos.add( // TODO TEMPORARY. UNCOMMENT THIS
                new ChildSiddhiAppInfo(
                        "test-app-group-2-1",
                        hardCodedApp2,
                        null,
                        2,
                        isChildAppStateful(hardCodedApp1),
                        false));
        return childSiddhiAppInfos;
    }

    private static boolean isChildAppStateful(String childApp) {
        return false;
    }
}

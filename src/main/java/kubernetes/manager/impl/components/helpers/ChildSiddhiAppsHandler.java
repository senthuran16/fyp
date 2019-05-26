package kubernetes.manager.impl.components.helpers;

import kubernetes.manager.framework.components.kubernetes.manager.exception.KubernetesManagerException;
import kubernetes.manager.framework.components.kubernetes.manager.generic.helpers.ChildAppsHandler;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.impl.models.ChildSiddhiAppInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods for handling child Siddhi app related actions
 */
public class ChildSiddhiAppsHandler implements ChildAppsHandler<ChildSiddhiAppInfo> {
    @Override
    public List<ChildSiddhiAppInfo> getChildAppInfos(ManagerServiceInfo managerServiceInfo,
                                                     String userDefinedSiddhiApp) throws KubernetesManagerException {
        try {
            return new SiddhiManagerHTTPClient().getChildAppInfos(managerServiceInfo, userDefinedSiddhiApp);
        } catch (IOException e) {
            throw new KubernetesManagerException("Failed to get child Siddhi app info.", e);
        }
    }

    @Override
    public ChildSiddhiAppInfo getNextChildAppInfo(ChildSiddhiAppInfo childAppInfo) {

        return new ChildSiddhiAppInfo(
                childAppInfo.getName(),
                replaceWithNextKafkaSourceGroupId(childAppInfo.getContent()),
                childAppInfo.getResourceRequirements(),
                childAppInfo.getInitialParallelism(),
                childAppInfo.isStateful(),
                childAppInfo.isReceiver());
    }

    private String replaceWithNextKafkaSourceGroupId(String siddhiApp) {
        if (!siddhiApp.contains("@source(type='kafka'")) {
            return siddhiApp;
        } else {
            final String GROUP_ID = "group.id=";
            final String THREADING_OPTION = ", threading.option";
            String[] split = siddhiApp.split(GROUP_ID);
            String[] split2 = split[1].split(THREADING_OPTION);

            String beforeGroupId = split[0];
            String nextGroupId = getNextGroupId(split2[0]);
            String afterGroupId = THREADING_OPTION + split2[1];

            return beforeGroupId + GROUP_ID + nextGroupId + afterGroupId;
        }
    }

    private String getNextGroupId(String currentGroupId) {
        return "'" + currentGroupId.split("'")[1] + "-rep'";
    }
}

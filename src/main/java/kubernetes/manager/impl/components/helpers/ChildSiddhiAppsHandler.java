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
}

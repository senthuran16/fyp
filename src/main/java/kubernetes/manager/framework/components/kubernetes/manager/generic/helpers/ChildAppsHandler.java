package kubernetes.manager.framework.components.kubernetes.manager.generic.helpers;

import kubernetes.manager.framework.components.kubernetes.manager.exception.KubernetesManagerException;
import kubernetes.manager.framework.models.concrete.ManagerServiceInfo;
import kubernetes.manager.framework.models.generic.ChildAppInfo;

import java.util.List;

public interface ChildAppsHandler <T extends ChildAppInfo> {
    List<T> getChildAppInfos(ManagerServiceInfo managerServiceInfo, String userDefinedApp)
            throws KubernetesManagerException;
}

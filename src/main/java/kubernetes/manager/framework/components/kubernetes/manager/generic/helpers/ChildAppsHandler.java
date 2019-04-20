package kubernetes.manager.framework.components.kubernetes.manager.generic.helpers;

import kubernetes.manager.framework.models.generic.ChildAppInfo;

import java.util.List;

public interface ChildAppsHandler <T extends ChildAppInfo> {
    List<T> getChildAppInfos(String userDefinedApp);
}

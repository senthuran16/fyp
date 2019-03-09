package beans.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

/**
 */
public class DoneableSiddhiApp extends CustomResourceDoneable<SiddhiApp> {
  public DoneableSiddhiApp(SiddhiApp resource, Function function) {
    super(resource, function);
  }
}

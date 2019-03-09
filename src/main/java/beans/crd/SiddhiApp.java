package beans.crd;

import io.fabric8.kubernetes.client.CustomResource;

/**
 */
public class SiddhiApp extends CustomResource {
    private SiddhiAppSpec spec;


    @Override
    public String toString() {
        return "SiddhiApp{" +
                "apiVersion='" + getApiVersion() + '\'' +
                ", metadata=" + getMetadata() +
                ", spec=" + spec +
                '}';
    }

    public SiddhiAppSpec getSpec() {
        return spec;
    }

    public void setSpec(SiddhiAppSpec spec) {
        this.spec = spec;
    }
}

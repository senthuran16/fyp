package beans.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

/**
 */
@JsonDeserialize(
    using = JsonDeserializer.None.class
)
public class SiddhiAppSpec implements KubernetesResource {
  private String name;
  private String content;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "SiddhiAppSpec{" +
            "name='" + name + '\'' +
            ", content='" + content + '\'' +
            '}';
  }
}

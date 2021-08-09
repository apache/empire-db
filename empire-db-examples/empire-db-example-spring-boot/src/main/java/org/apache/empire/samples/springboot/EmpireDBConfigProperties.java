package org.apache.empire.samples.springboot;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "empiredb")
public class EmpireDBConfigProperties {

  private String driverClass;
  private Map<String, String> driverProperties;

  public String getDriverClass() {
    return driverClass;
  }

  public Map<String, String> getDriverProperties() {
    return driverProperties;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  public void setDriverProperties(Map<String, String> driverProperties) {
    this.driverProperties = driverProperties;
  }
}

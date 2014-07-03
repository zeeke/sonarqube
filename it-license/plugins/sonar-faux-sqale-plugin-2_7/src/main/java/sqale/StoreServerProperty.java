package sqale;

import org.picocontainer.Startable;
import org.sonar.api.ServerExtension;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

public class StoreServerProperty implements ServerExtension, Startable {
  private final PropertiesDao dao;

  public StoreServerProperty(PropertiesDao dao) {
    this.dao = dao;
  }

  public void start() {
    dao.setProperty(new PropertyDto().setKey("printed_from_server_extension").setValue("true"));
  }

  public void stop() {
  }
}

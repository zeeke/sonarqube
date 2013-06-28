import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.GlobalPropertyChangeHandler;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

@Properties(
    @Property(key = "globalPropertyChange.received", name = "Check that extension has correctly been notified by global property change", category = "fake")
)
public final class FakeGlobalPropertyChange extends GlobalPropertyChangeHandler {

  private PropertiesDao dao;

  public FakeGlobalPropertyChange(PropertiesDao dao) {
    this.dao = dao;
  }

  @Override
  public void onChange(PropertyChange propertyChange) {
    System.out.println("Received change: " + propertyChange);
    dao.setProperty(new PropertyDto().setKey("globalPropertyChange.received").setValue(propertyChange.getNewValue()));
  }
}

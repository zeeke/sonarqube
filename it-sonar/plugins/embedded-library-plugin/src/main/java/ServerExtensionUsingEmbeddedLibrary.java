import org.apache.commons.i18n.bundles.MessageBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;

public class ServerExtensionUsingEmbeddedLibrary implements ServerExtension {

  public void start() {
    // this is a commons-i18n class
    new MessageBundle("12345");
    LoggerFactory.getLogger(getClass()).info("Embedded dependency from server extension");
  }
}

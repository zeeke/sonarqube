import org.sonar.api.config.Settings;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.SecurityRealm;

public class FakeRealm extends SecurityRealm {

  private FakeAuthenticator instance;

  public FakeRealm(Settings settings) {
    this.instance = new FakeAuthenticator(settings);
  }

  @Override
  public LoginPasswordAuthenticator getLoginPasswordAuthenticator() {
    return instance;
  }

  @Override
  public ExternalGroupsProvider getGroupsProvider() {
    return new FakeGroupsProvider(instance);
  }

  @Override
  public ExternalUsersProvider getUsersProvider() {
    return new FakeUsersProvider(instance);
  }

}

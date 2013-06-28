import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.UserDetails;

public class FakeUsersProvider extends ExternalUsersProvider {

  private final FakeAuthenticator instance;

  public FakeUsersProvider(FakeAuthenticator instance) {
    this.instance = instance;
  }

  @Override
  public UserDetails doGetUserDetails(String username) {
    return instance.doGetUserDetails(username);
  }

}

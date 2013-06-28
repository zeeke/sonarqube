import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.security.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class FakeAuthenticatorTest {

  private Settings settings;
  private FakeAuthenticator authenticator;

  @Before
  public void setUp() {
    settings = new Settings();
    authenticator = new FakeAuthenticator(settings);
    authenticator.init();
  }

  @Test
  public void shouldNeverTouchAdmin() {
    assertThat(authenticator.authenticate("admin", "admin"), is(true));
    assertThat(authenticator.doGetGroups("admin"), nullValue());
    assertThat(authenticator.doGetUserDetails("admin"), nullValue());
  }

  @Test
  public void shouldAuthenticateFakeUsers() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo");

    assertThat(authenticator.authenticate("evgeny", "foo"), is(true));
    assertThat(authenticator.authenticate("evgeny", "bar"), is(false));
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotAuthenticateNotExistingUsers() {
    authenticator.authenticate("evgeny", "foo");
  }

  @Test
  public void shouldGetUserDetails() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo\n" +
      "evgeny.name=Tester Testerovich\n" +
      "evgeny.email=evgeny@example.org");

    UserDetails details = authenticator.doGetUserDetails("evgeny");
    assertThat(details.getName(), is("Tester Testerovich"));
    assertThat(details.getEmail(), is("evgeny@example.org"));
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotReturnDetailsForNotExistingUsers() {
    authenticator.doGetUserDetails("evgeny");
  }

  @Test
  public void shouldGetGroups() {
    settings.setProperty(FakeAuthenticator.DATA_PROPERTY, "evgeny.password=foo\n" +
      "evgeny.groups=sonar-users,sonar-developers");

    assertThat(authenticator.doGetGroups("evgeny"), is((Collection) Arrays.asList("sonar-users", "sonar-developers")));
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotReturnGroupsForNotExistingUsers() {
    authenticator.doGetGroups("evgeny");
  }

  @Test
  public void shouldParseList() {
    assertThat(FakeAuthenticator.parseList(null).size(), is(0));
    assertThat(FakeAuthenticator.parseList("").size(), is(0));
    assertThat(FakeAuthenticator.parseList(",,,").size(), is(0));
    assertThat(FakeAuthenticator.parseList("a,b"), is(Arrays.asList("a", "b")));
  }

  @Test
  public void shouldParseMap() {
    Map<String, String> map = FakeAuthenticator.parse(null);
    assertThat(map.size(), is(0));

    map = FakeAuthenticator.parse("");
    assertThat(map.size(), is(0));

    map = FakeAuthenticator.parse("foo=bar");
    assertThat(map.size(), is(1));
    assertThat(map.get("foo"), is("bar"));

    map = FakeAuthenticator.parse("foo=bar\r\nbaz=qux");
    assertThat(map.size(), is(2));
    assertThat(map.get("foo"), is("bar"));
    assertThat(map.get("baz"), is("qux"));

    map = FakeAuthenticator.parse("foo=bar\nbaz=qux");
    assertThat(map.size(), is(2));
    assertThat(map.get("foo"), is("bar"));
    assertThat(map.get("baz"), is("qux"));

    map = FakeAuthenticator.parse("foo=bar\n\n\nbaz=qux");
    assertThat(map.size(), is(2));
    assertThat(map.get("foo"), is("bar"));
    assertThat(map.get("baz"), is("qux"));
  }

}

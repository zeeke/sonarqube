package org.sonar.samples;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;

public final class Profile1 extends ProfileDefinition {

  public RulesProfile createProfile(ValidationMessages messages) {
    return RulesProfile.create("Profile1", "foo");
  }
}

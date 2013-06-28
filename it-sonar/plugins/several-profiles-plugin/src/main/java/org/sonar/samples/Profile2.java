package org.sonar.samples;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;

public final class Profile2 extends ProfileDefinition {

  public RulesProfile createProfile(ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create("Profile2", "foo");
    profile.setDefaultProfile(true);
    return profile;
  }
}

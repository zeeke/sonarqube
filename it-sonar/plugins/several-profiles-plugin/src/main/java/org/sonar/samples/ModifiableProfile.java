package org.sonar.samples;

import org.sonar.api.config.Settings;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;

/**
 * Profile that can be changed by properties to update:
 * <ul>
 * <li>its name</li>
 * <li>its "default_profile" property</li>
 * </ul>
 * 
 * This is used to play with different sets of profiles for the ITs.
 */
public final class ModifiableProfile extends ProfileDefinition {

  private Settings settings;

  public ModifiableProfile(Settings settings) {
    this.settings = settings;
  }

  public RulesProfile createProfile(ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create("Profile3", "foo");

    Boolean isDefault = settings.getBoolean("sonar.modifiable_profile.default");
    if (Boolean.TRUE.equals(isDefault)) {
      profile.setDefaultProfile(true);
    }

    return profile;
  }
}

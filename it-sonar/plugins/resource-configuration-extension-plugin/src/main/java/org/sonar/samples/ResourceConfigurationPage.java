package org.sonar.samples;

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.api.web.ResourceQualifier;
import org.sonar.api.web.UserRole;

@NavigationSection(NavigationSection.RESOURCE_CONFIGURATION)
@UserRole(UserRole.ADMIN)
@ResourceQualifier({Qualifiers.PROJECT})
public class ResourceConfigurationPage implements Page {

  public String getId() {
    return "/resource_configuration_sample";
  }

  public String getTitle() {
    return "Resource Configuration";
  }

}

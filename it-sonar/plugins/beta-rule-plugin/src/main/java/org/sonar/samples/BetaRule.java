package org.sonar.samples;

import org.sonar.check.Priority;
import org.sonar.check.Rule;

@Rule(key = "beta-rule", name = "Beta rule", status = "BETA", priority = Priority.MAJOR, description = "<p>This is a beta rule.</p>")
public class BetaRule {

}

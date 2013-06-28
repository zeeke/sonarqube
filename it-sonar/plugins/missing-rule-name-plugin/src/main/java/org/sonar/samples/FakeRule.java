package org.sonar.samples;

import org.sonar.check.Priority;
import org.sonar.check.Rule;

@Rule(key = "FakeRule", priority = Priority.MAJOR, description = "<p>This is a fake rule without name specified.</p>")
public class FakeRule {

}

/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.sensor;

import org.sonar.api.BatchExtension;
import org.sonar.api.component.Perspective;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

public interface Sensor extends BatchExtension {

  class Context {

    // shortcuts to popular services in order to avoid pico injection

    public Module module() {
      return null;
    }

    public Settings settings() {
      return null;
    }

    public ModuleFileSystem fs() {
      return null;
    }

    // missing information about analysis ? dry run ? incremental ? date ?

    public Measures measures() {
      return null;
    }

    public Issues issues() {
      return null;
    }
  }

  // the method shouldExecuteOnProject(Project) has been dropped. Sensors must add conditions in
  // the method execute(Context)
  void execute(Context context);

}

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
import org.sonar.api.batch.Module;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.issue.IssueService;
import org.sonar.api.batch.measure.MeasureService;
import org.sonar.api.config.Settings;

/**
 * Replaces the deprecated org.sonar.api.batch.Sensor.
 * @since 4.2
 */
public interface Sensor extends BatchExtension {

  /**
   * Contains shortcuts to popular services in order to avoid injection by constructor.
   */
  interface Context {
    Module module();

    Settings settings();

    FileSystem fs();

    // TODO missing information about analysis ? dry run ? incremental ? date ?

    MeasureService measures();

    IssueService issues();
  }

  void execute(Context context);

}

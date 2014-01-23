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
package org.sonar.api.batch.sensor.example;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.WithinLifecycle;

public class SquidSensor implements Sensor, WithinLifecycle {

  @Override
  public void define(Lifecycle lifecycle) {
    // Must be executed before all other plugins so the dictionary Java classes <-> InputFile is
    // available.
    // Some standard barriers must be defined in core: init, scan (default), consolidation
    lifecycle.setOn("init");
  }

  @Override
  public void execute(Context context) {

  }
}

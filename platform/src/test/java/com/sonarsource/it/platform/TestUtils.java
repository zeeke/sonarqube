/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonarsource.it.platform;

import com.sonar.orchestrator.OrchestratorBuilder;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TestUtils {

  public static void addAllCompatiblePlugins(OrchestratorBuilder builder) {
    org.sonar.updatecenter.common.Version sonarVersion = org.sonar.updatecenter.common.Version.create(builder.getSonarVersion());
    for (Plugin p : findAllCompatiblePlugins(builder.getUpdateCenter(), sonarVersion)) {
      Release r = p.getLastCompatible(sonarVersion);
      builder.setOrchestratorProperty(p.getKey() + "Version", r.getVersion().toString());
      builder.addPlugin(p.getKey());
    }
  }

  public static void activateLicenses(OrchestratorBuilder builder) {
    builder
      .activateLicense("abap")
      .activateLicense("cobol")
      .activateLicense("cpp")
      .activateLicense("devcockpit")
      .activateLicense("natural")
      .activateLicense("pacbase")
      .activateLicense("pli")
      .activateLicense("plsql")
      .activateLicense("report")
      .activateLicense("rpg")
      .activateLicense("sqale")
      .activateLicense("vb")
      .activateLicense("vbnet")
      .activateLicense("views");
  }

  private static List<Plugin> findAllCompatiblePlugins(UpdateCenter center, org.sonar.updatecenter.common.Version sqVersion) {
    List<Plugin> availables = newArrayList();
    for (Plugin plugin : center.findAllCompatiblePlugins()) {
      Release release = plugin.getLastCompatible(sqVersion);
      if (release != null
        // Don't install member of an ecosystem as they will be automatically installed by Orchestrator with the parent
        && release.getParent() == null) {
        availables.add(plugin);
      }
    }
    return availables;
  }

}

/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.samples;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

public final class RequiredMeasuresWidgetsDashboard extends DashboardTemplate {

  @Override
  public String getName() {
    return "RequiredMeasuresWidgetsDashboard";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);
    dashboard.addWidget("WidgetMandatoryAndOneOfSatisfied", 1);
    dashboard.addWidget("WidgetMandatoryNotSatisfied", 1);
    dashboard.addWidget("WidgetMandatorySatisfied", 1);
    dashboard.addWidget("WidgetMandatorySatisfiedButNotOneOf", 1);
    dashboard.addWidget("WidgetNoConstraints", 1);
    dashboard.addWidget("WidgetOneOfNotSatisfied", 1);
    dashboard.addWidget("WidgetOneOfSatisfied", 1);
    dashboard.addWidget("WidgetOneOfSatisfiedButNotMandatory", 1);
    return dashboard;
  }

}

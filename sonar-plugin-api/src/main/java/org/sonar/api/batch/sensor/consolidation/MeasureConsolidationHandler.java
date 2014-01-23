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
package org.sonar.api.batch.sensor.consolidation;

import org.sonar.api.measures.Measure;

import javax.annotation.CheckForNull;
import java.util.Collection;

public interface MeasureConsolidationHandler {
  class Context {
    /**
     * @param metric must be declared in {@link org.sonar.api.batch.sensor.consolidation.MeasureConsolidation.Definition#setUses(String...)}
     */
    @CheckForNull
    Measure measure(String metric) {
      return null;
    }

    Collection<Measure> childMeasures(String metric) {
      return null;
    }

    /**
     * @param metric must be declared in {@link org.sonar.api.batch.sensor.consolidation.MeasureConsolidation.Definition#setComputes(String...)}
     */
    Context setMeasure(Measure measure) {
      return this;
    }
  }

  void handle(Context context);
}

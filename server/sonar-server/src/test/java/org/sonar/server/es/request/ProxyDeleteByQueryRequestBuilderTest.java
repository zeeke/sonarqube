/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.es.request;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProxyDeleteByQueryRequestBuilderTest {

  Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.NONE.name()));
  SearchClient searchClient = new SearchClient(new Settings(), profiling);

  @After
  public void tearDown() throws Exception {
    searchClient.stop();
  }

  @Test
  public void expect_failure() {
    try {
      searchClient.prepareDeleteByQuery(IndexDefinition.RULE.getIndexName()).setQuery(QueryBuilders.matchAllQuery()).get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Fail to execute ES delete by query request on indices 'rules'");
    }
  }

  @Test
  public void to_string() {
    assertThat(searchClient.prepareDeleteByQuery(IndexDefinition.RULE.getIndexName()).toString()).isEqualTo("ES delete by query request on indices 'rules'");
    assertThat(searchClient.prepareDeleteByQuery().toString()).isEqualTo("ES delete by query request");
  }

  @Test
  public void with_profiling_basic() {
    Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.BASIC.name()));
    SearchClient searchClient = new SearchClient(new Settings(), profiling);

    try {
      searchClient.prepareDeleteByQuery(IndexDefinition.RULE.getIndexName()).setQuery(QueryBuilders.matchAllQuery()).get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Fail to execute ES delete by query request on indices 'rules'");
    }

    // TODO assert profiling
    searchClient.stop();
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareDeleteByQuery().get("1");
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareDeleteByQuery().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    try {
      searchClient.prepareDeleteByQuery().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}

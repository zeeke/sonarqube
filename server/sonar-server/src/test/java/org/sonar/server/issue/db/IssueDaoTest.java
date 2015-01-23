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
package org.sonar.server.issue.db;

import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.BatchIssueDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.rule.RuleTesting;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueDaoTest extends AbstractDaoTestCase {

  private IssueDao sut;
  private DbSession session;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.sut = new IssueDao(getMyBatis());
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void get_by_key() {
    setupData("shared", "get_by_key");

    IssueDto issue = sut.selectByKey(session, "ABCDE");
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getId()).isEqualTo(100L);
    assertThat(issue.getComponentId()).isEqualTo(401);
    assertThat(issue.getProjectId()).isEqualTo(399);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getLanguage()).isEqualTo("java");
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getEffortToFix()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("karadoc");
    assertThat(issue.getReporter()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isEqualTo(1400000000000L);
    assertThat(issue.getUpdatedAt()).isEqualTo(1450000000000L);
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getProjectKey()).isEqualTo("struts");
  }

  @Test
  public void get_by_keys() {
    setupData("shared", "get_by_key");

    List<IssueDto> issues = sut.selectByKeys(session, Arrays.asList("ABCDE"));
    assertThat(issues).hasSize(1);
  }

  @Test
  public void find_by_action_plan() {
    setupData("shared", "find_by_action_plan");

    List<IssueDto> issues = sut.findByActionPlan(session, "AP-1");
    assertThat(issues).hasSize(1);

    IssueDto issue = issues.get(0);
    assertThat(issue.getKee()).isEqualTo("ABCDE");
    assertThat(issue.getActionPlanKey()).isEqualTo("AP-1");
    assertThat(issue.getComponentId()).isEqualTo(401);
    assertThat(issue.getProjectId()).isEqualTo(399);
    assertThat(issue.getRuleId()).isEqualTo(500);
    assertThat(issue.getLanguage()).isEqualTo("java");
    assertThat(issue.getSeverity()).isEqualTo("BLOCKER");
    assertThat(issue.isManualSeverity()).isFalse();
    assertThat(issue.getMessage()).isNull();
    assertThat(issue.getLine()).isEqualTo(200);
    assertThat(issue.getEffortToFix()).isEqualTo(4.2);
    assertThat(issue.getStatus()).isEqualTo("OPEN");
    assertThat(issue.getResolution()).isEqualTo("FIXED");
    assertThat(issue.getChecksum()).isEqualTo("XXX");
    assertThat(issue.getAuthorLogin()).isEqualTo("karadoc");
    assertThat(issue.getReporter()).isEqualTo("arthur");
    assertThat(issue.getAssignee()).isEqualTo("perceval");
    assertThat(issue.getIssueAttributes()).isEqualTo("JIRA=FOO-1234");
    assertThat(issue.getIssueCreationDate()).isNotNull();
    assertThat(issue.getIssueUpdateDate()).isNotNull();
    assertThat(issue.getIssueCloseDate()).isNotNull();
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getRuleRepo()).isEqualTo("squid");
    assertThat(issue.getRule()).isEqualTo("AvoidCycle");
    assertThat(issue.getComponentKey()).isEqualTo("Action.java");
    assertThat(issue.getProjectKey()).isEqualTo("struts");
  }

  @Test
  public void select_non_closed_issues_by_module_uuid() {
    setupData("shared", "select_non_closed_issues_by_module_uuid");

    // BCDE is a non-root module, we should find 2 issues from classes and one on itself
    DefaultResultHandler handler = new DefaultResultHandler();
    sut.selectNonClosedIssuesByModuleUuid(session, "BCDE", handler);
    assertThat(handler.getResultList()).extracting("key").containsOnly("100", "101", "103");

    // DBCA is a a simple project with a single file
    handler = new DefaultResultHandler();
    sut.selectNonClosedIssuesByModuleUuid(session, "DBCA", handler);
    assertThat(handler.getResultList()).hasSize(1);

    BatchIssueDto batchIssueDto = (BatchIssueDto) handler.getResultList().get(0);
    assertThat(batchIssueDto.getKey()).isEqualTo("1000");
    assertThat(batchIssueDto.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(batchIssueDto.getRuleRepo()).isEqualTo("squid");
    assertThat(batchIssueDto.getMessage()).isEqualTo("Avoid this");
    assertThat(batchIssueDto.getLine()).isEqualTo(200);
    assertThat(batchIssueDto.getResolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(batchIssueDto.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(batchIssueDto.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(batchIssueDto.isManualSeverity()).isTrue();
    assertThat(batchIssueDto.getComponentKey()).isEqualTo("Sample.java");
    assertThat(batchIssueDto.getChecksum()).isEqualTo("123456");
    assertThat(batchIssueDto.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void select_non_closed_issues_by_project_uuid() {
    setupData("shared", "select_non_closed_issues_by_project_uuid");

    // ABCD is the root module, we should find all 4 issues
    DefaultResultHandler handler = new DefaultResultHandler();
    sut.selectNonClosedIssuesByProjectUuid(session, "ABCD", handler);
    assertThat(handler.getResultList()).hasSize(4);

    // DBCA is a a simple project with a single file
    handler = new DefaultResultHandler();
    sut.selectNonClosedIssuesByProjectUuid(session, "DBCA", handler);
    assertThat(handler.getResultList()).hasSize(1);

    BatchIssueDto batchIssueDto = (BatchIssueDto) handler.getResultList().get(0);
    assertThat(batchIssueDto.getKey()).isEqualTo("1000");
    assertThat(batchIssueDto.getRuleKey()).isEqualTo("AvoidCycle");
    assertThat(batchIssueDto.getRuleRepo()).isEqualTo("squid");
    assertThat(batchIssueDto.getMessage()).isEqualTo("Avoid this");
    assertThat(batchIssueDto.getLine()).isEqualTo(200);
    assertThat(batchIssueDto.getResolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(batchIssueDto.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(batchIssueDto.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(batchIssueDto.isManualSeverity()).isTrue();
    assertThat(batchIssueDto.getComponentKey()).isEqualTo("Sample.java");
    assertThat(batchIssueDto.getChecksum()).isEqualTo("123456");
    assertThat(batchIssueDto.getAssigneeLogin()).isEqualTo("john");
  }

  @Test
  public void insert() throws Exception {
    IssueDto dto = new IssueDto();
    dto.setComponent(new ComponentDto().setKey("struts:Action").setId(123L).setUuid("component-uuid"));
    dto.setProject(new ComponentDto().setKey("struts").setId(100L).setUuid("project-uuid"));
    dto.setRule(RuleTesting.newDto(RuleKey.of("squid", "S001")).setId(200));
    dto.setKee("ABCDE");
    dto.setLine(500);
    dto.setEffortToFix(3.14);
    dto.setDebt(10L);
    dto.setResolution("FIXED");
    dto.setStatus("RESOLVED");
    dto.setSeverity("BLOCKER");
    dto.setReporter("emmerik");
    dto.setAuthorLogin("morgan");
    dto.setAssignee("karadoc");
    dto.setActionPlanKey("current_sprint");
    dto.setIssueAttributes("JIRA=FOO-1234");
    dto.setChecksum("123456789");
    dto.setMessage("the message");

    dto.setIssueCreationDate(DateUtils.parseDate("2013-05-18"));
    dto.setIssueUpdateDate(DateUtils.parseDate("2013-05-19"));
    dto.setIssueCloseDate(DateUtils.parseDate("2013-05-20"));
    dto.setCreatedAt(1400000000000L);
    dto.setUpdatedAt(1450000000000L);

    sut.insert(session, dto);
    session.commit();

    checkTables("insert", new String[] {"id"}, "issues");
  }
}

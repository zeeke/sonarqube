/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.event;

import com.google.common.collect.Lists;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.EventQuery;

import java.util.List;
import java.util.Properties;

import static com.sonar.it.ItUtils.inspectWithoutTests;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EventTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.javaPlugin())
    .build();

  @After
  public void clean() {
    orchestrator.resetData();
  }

  @Test
  public void testConfigurationOfEvents() {
    inspectWithoutTests(orchestrator, "shared/multi-modules-sample", null);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("events",
        "/selenium/event/events/create_event_with_special_character.html",
        "/selenium/event/events/no_events_widget_on_package.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testDeleteStandardEvent() {
    inspectWithoutTests(orchestrator, "shared/sample", null);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("delete-event",
        "/selenium/event/events/create_delete_standard_event.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testEventWidget() {

    // first build, in the past
    Properties props = new Properties();
    props.put("sonar.projectDate", "2012-01-01");
    inspectWithoutTests(orchestrator, "shared/sample", props);
    // Second build, today
    inspectWithoutTests(orchestrator, "shared/sample", null);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("event-widget",
        "/selenium/event/widget/show_events_using_filters.html"

        // SONAR-3091 - disabled because of SONAR-3490
        // "/selenium/event/widget/do_not_show_events_for_deleted_snapshots.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3308
   */
  @Test
  public void testKeepOnlyOneEventPerVersionInProjectHistory() throws Exception {

    Properties propsForReleasedVersion = new Properties();
    propsForReleasedVersion.put("sonar.projectVersion", "1.0");
    // first analyse the 1.0-SNAPSHOT version
    inspectWithoutTests(orchestrator, "shared/sample", null);
    // then analyse the 1.0 version
    inspectWithoutTests(orchestrator, "shared/sample", propsForReleasedVersion);
    // and do this all over again
    inspectWithoutTests(orchestrator, "shared/sample", null);
    inspectWithoutTests(orchestrator, "shared/sample", propsForReleasedVersion);

    // there should be only 1 "0.1-SNAPSHOT" event and only 1 "0.1" event
    List<Event> events = orchestrator.getServer().getWsClient().findAll(new EventQuery().setResourceKey("com.sonarsource.it.samples:simple-sample"));
    assertThat(events.size(), is(2));
    List<String> eventNames = Lists.newArrayList(events.get(0).getName(), events.get(1).getName());
    assertThat(eventNames, hasItems("1.0", "1.0-SNAPSHOT"));
  }
}

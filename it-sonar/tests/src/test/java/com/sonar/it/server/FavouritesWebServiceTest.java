/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Favourite;
import org.sonar.wsclient.services.FavouriteCreateQuery;
import org.sonar.wsclient.services.FavouriteDeleteQuery;
import org.sonar.wsclient.services.FavouriteQuery;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FavouritesWebServiceTest {
  @Rule
  public Orchestrator orchestrator = Orchestrator.builderEnv().build();

  @Before
  public void inspectProject() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testWebService() throws Exception {
    Sonar adminWsClient = orchestrator.getServer().getAdminWsClient();

    // GET (nothing)
    List<Favourite> favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites.size(), is(0));

    // POST (create favourites)
    Favourite favourite = adminWsClient.create(new FavouriteCreateQuery("com.sonarsource.it.samples:simple-sample"));
    assertThat(favourite, not(nullValue()));
    assertThat(favourite.getKey(), is("com.sonarsource.it.samples:simple-sample"));
    adminWsClient.create(new FavouriteCreateQuery("com.sonarsource.it.samples:simple-sample:sample.Sample"));

    // GET (created favourites)
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites.size(), is(2));
    assertThat(favourites.get(0).getKey(), is("com.sonarsource.it.samples:simple-sample"));
    assertThat(favourites.get(1).getKey(), is("com.sonarsource.it.samples:simple-sample:sample.Sample"));

    // DELETE (a favourite)
    adminWsClient.delete(new FavouriteDeleteQuery("com.sonarsource.it.samples:simple-sample"));
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites.size(), is(1));
    assertThat(favourites.get(0).getKey(), is("com.sonarsource.it.samples:simple-sample:sample.Sample"));
  }

}

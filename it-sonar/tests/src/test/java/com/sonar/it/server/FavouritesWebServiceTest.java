/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
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

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class FavouritesWebServiceTest {
  @Rule
  public Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.javaPlugin())
    .build();

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
    assertThat(favourites).isEmpty();

    // POST (create favourites)
    Favourite favourite = adminWsClient.create(new FavouriteCreateQuery("com.sonarsource.it.samples:simple-sample"));
    assertThat(favourite).isNotNull();
    assertThat(favourite.getKey()).isEqualTo("com.sonarsource.it.samples:simple-sample");
    adminWsClient.create(new FavouriteCreateQuery("com.sonarsource.it.samples:simple-sample:src/main/java/sample/Sample.java"));

    // GET (created favourites)
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).hasSize(2);
    List<String> keys = newArrayList(Iterables.transform(favourites, new Function<Favourite, String>() {
      @Override
      public String apply(Favourite input) {
        return input.getKey();
      }
    }));
    assertThat(keys).containsOnly("com.sonarsource.it.samples:simple-sample", "com.sonarsource.it.samples:simple-sample:src/main/java/sample/Sample.java");

    // DELETE (a favourite)
    adminWsClient.delete(new FavouriteDeleteQuery("com.sonarsource.it.samples:simple-sample"));
    favourites = adminWsClient.findAll(new FavouriteQuery());
    assertThat(favourites).hasSize(1);
    assertThat(favourites.get(0).getKey()).isEqualTo("com.sonarsource.it.samples:simple-sample:src/main/java/sample/Sample.java");
  }

}

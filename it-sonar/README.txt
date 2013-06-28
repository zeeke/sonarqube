# PREREQUISITES

* A connection to Internet if the Sonar version under test is not available into $USER_HOME/.sonar/installs

* If Java executable is not available in PATH, then $JAVA_HOME or -Djava.home=<path> must be set

* If Maven executable is not available in PATH, then $MAVEN_HOME or -Dmaven.home=<path> must be set

* The path to Maven local repository must be set with $MAVEN_LOCAL_REPOSITORY or -Dmaven.localRepository=<path>

* The path to Maven remote repository CAN be set with $MAVEN_REMOTE_REPOSITORY or -Dmaven.remoteRepository=<path>.
It's recommended to set the value http://sonar:tatawin27@nexus.internal.sonarsource.com/nexus/content/groups/ss-repo

* Firefox must be available in $PATH


# CONFIGURATION

Mandatory properties :
* sonar.runtimeVersion=<VERSION>. It can be a SNAPSHOT or a RELEASE version.
* sonar.jdbc.dialect=h2|derby|mssql|mysql|oracle|postgresql.

Optional properties are :
* sonar.jdbc.url
* sonar.jdbc.username. Default value is "sonar".
* sonar.jdbc.password. Default value is "sonar".
* sonar.jdbc.driverFile, usually used for Oracle
* sonar.jdbc.driverClassName, usually used for Oracle
* sonar.jdbc.rootUrl, sonar.jdbc.rootUsername and sonar.jdbc.rootPassword. By default they are correctly set according to related database.
* sonar.jdbc.schema. Default value is "sonar".
* sonar.container=<cargo container key>, for example jetty7x, tomcat5x, jboss6x...
* sonar.container.downloadUrl=<zip url> if the container is not supported yet by Orchestrator

Note: the profile '-Poracle' must be activated when using Oracle


# HOW TO RUN WITHIN IDE

* Plugins must be built before running tests from IDE : mvn clean install -f plugins/pom.xml



# NAMING CONVENTIONS

* All the test files must be located in com.sonar.it.<category>, for example com.sonar.it.duplications. The root package com.sonar.it must not be used.

* Inspected projects, Java tests and Selenium tests must be organized with the same categories. For example com.sonar.it.duplications.DuplicationTest must use projects
 located in projects/duplications and execute Selenium tests located in tests/src/test/resources/selenium/duplications.


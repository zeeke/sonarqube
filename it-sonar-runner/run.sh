#!/bin/sh
mvn clean install -Dsonar.runtimeVersion=3.1 -DsonarRunner.version=1.4-SNAPSHOT -Dsonar.jdbc.dialect=derby
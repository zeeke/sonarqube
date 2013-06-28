#!/bin/sh
mvn clean install -Dsonar.runtimeVersion=3.0 -Dsonar.jdbc.dialect=derby -DlicenseVersion=2.3-SNAPSHOT
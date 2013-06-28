This folder is the new location (since with Sonar 2.15) for HTML description files (for the Squid Java plugin: plugin key "squidjava" and repo key "squid").
See http://jira.codehaus.org/browse/SONAR-3319.

There's only 1 file here that: 
  - supersedes the one in "org.sonar.l10n.squijava_fr" when plugin executed in Sonar >= 2.15
  - is just simply ignored when plugin executed in Sonar < 2.15

SO: the fact that all HTML files have been kept in "org.sonar.l10n.squijava_fr" for the moment ensures that backward compatibility in ensured. 
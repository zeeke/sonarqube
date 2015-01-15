/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenLogs {

  /**
   * Total time: 6.015s
   * Total time: 3:14.025s
   */
  public static Long extractTotalTime(String logs) {
    Pattern pattern = Pattern.compile(".*Total time: (\\d*:)?(\\d+).(\\d+)s.*");
    Matcher matcher = pattern.matcher(logs);
    if (matcher.matches()) {
      String minutes = StringUtils.defaultIfBlank(StringUtils.removeEnd(matcher.group(1), ":"), "0");
      String seconds = StringUtils.defaultIfBlank(matcher.group(2), "0");
      String millis = StringUtils.defaultIfBlank(matcher.group(3), "0");

      return (Long.parseLong(minutes) * 60000) + (Long.parseLong(seconds) * 1000) + Long.parseLong(millis);
    }
    return null;
  }

  /**
   * Final Memory: 68M/190M
   */
  public static Long extractEndMemory(String logs) {
    return extractLong(logs, ".*Final Memory: (\\d+)M/[\\d]+M.*");
  }

  public static Long extractMaxMemory(String logs) {
    return extractLong(logs, ".*Final Memory: [\\d]+M/(\\d+)M.*");
  }

  private static Long extractLong(String logs, String format) {
    Pattern pattern = Pattern.compile(format);
    Matcher matcher = pattern.matcher(logs);
    if (matcher.matches()) {
      String s = matcher.group(1);
      return Long.parseLong(s);
    }
    return null;
  }

  /**
   * #1 - big-project - processing analysis report done: 914072 ms
   * @param logs
   */
  public static Long extractComputationTotalTime(List<String> logs) {
    Pattern pattern = Pattern.compile(".*done:\\s(\\d+)\\sms");
    for (int i = logs.size() - 1; i >= 0; i--) {
      String line = logs.get(i);
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        String duration = matcher.group(1);
        return Long.parseLong(duration);
      }
    }

    return null;
  }
}

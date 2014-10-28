/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ServerLogs {

  static Date extractDate(String line) {
    String pattern = "yyyy.MM.dd HH:mm:ss";
    SimpleDateFormat format = new SimpleDateFormat(pattern);
    if (line.length() > 19) {
      try {
        return format.parse(line.substring(0, 19));
      } catch (Exception e) {
        // ignore
      }
    }
    return null;
  }

  public static Date extractFirstDate(List<String> lines) {
    for (String line : lines) {
      Date d = ServerLogs.extractDate(line);
      if (d != null) {
        return d;
      }
    }
    return null;
  }

  public static void clear(Orchestrator orch) throws IOException {
    if (orch.getServer() != null && orch.getServer().getLogs() != null) {
      FileUtils.write(orch.getServer().getLogs(), "", false);
    }
  }

}

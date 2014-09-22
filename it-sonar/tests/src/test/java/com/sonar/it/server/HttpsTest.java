/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class HttpsTest {

  Orchestrator orchestrator;
  int httpsPort = NetworkUtils.getNextAvailablePort();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void fail_to_start_if_bad_keystore_credentials() throws Exception {
    try {
      URL jksKeystore = getClass().getResource("/com/sonar/it/server/HttpsTest/keystore.jks");
      orchestrator = Orchestrator.builderEnv()
        .setServerProperty("sonar.web.https.port", String.valueOf(httpsPort))
        .setServerProperty("sonar.web.https.keyAlias", "tests")
        .setServerProperty("sonar.web.https.keyPass", "__wrong__")
        .setServerProperty("sonar.web.https.keystoreFile", new File(jksKeystore.toURI()).getAbsolutePath())
        .setServerProperty("sonar.web.https.keystorePass", "__wrong__")
        .build();
      orchestrator.start();
      fail();
    } catch (Exception e) {
      File logFile = orchestrator.getServer().getLogs();
      assertThat(FileUtils.readFileToString(logFile)).contains("Password verification failed");
    }
  }

  @Test
  public void enable_https_port() throws Exception {
    // start server
    URL jksKeystore = getClass().getResource("/com/sonar/it/server/HttpsTest/keystore.jks");
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.web.https.port", String.valueOf(httpsPort))
      .setServerProperty("sonar.web.https.keyAlias", "tests")
      .setServerProperty("sonar.web.https.keyPass", "thetests")
      .setServerProperty("sonar.web.https.keystoreFile", new File(jksKeystore.toURI()).getAbsolutePath())
      .setServerProperty("sonar.web.https.keystorePass", "thepassword")
      .build();
    orchestrator.start();

    // check logs
    File logFile = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logFile)).contains("HTTPS connector is enabled on port " + httpsPort);

    // connect from clients
    connectTrusted();
    connectUntrusted();
  }

  private void connectTrusted() throws IOException {
    URL url = new URL("https://localhost:" + httpsPort + "/sonar");
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    try {
      connection.getInputStream();
      fail();
    } catch (SSLHandshakeException e) {
      // ok, the certificate is not trusted
    }
  }

  private void connectUntrusted() throws Exception {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }
    };

    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    SSLSocketFactory untrustedSocketFactory = sc.getSocketFactory();

    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };
    URL url = new URL("https://localhost:" + httpsPort + "/sonar/sessions/login");
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setAllowUserInteraction(true);
    connection.setSSLSocketFactory(untrustedSocketFactory);
    connection.setHostnameVerifier(allHostsValid);

    InputStream input = connection.getInputStream();
    checkCookieFlags(connection);
    try {
      String html = IOUtils.toString(input);
      assertThat(html).contains("<body");
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * SSF-13 HttpOnly flag
   * SSF-16 Secure flag
   */
  private void checkCookieFlags(HttpsURLConnection connection) {
    List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
    boolean foundSessionCookie = false;
    for (String cookie : cookies) {
      if (StringUtils.containsIgnoreCase(cookie, "JSESSIONID")) {
        foundSessionCookie = true;
        assertThat(cookie).containsIgnoringCase("Secure").containsIgnoringCase("HttpOnly");
      }
    }
    if (!foundSessionCookie) {
      fail("Session cookie not found");
    }
  }
}

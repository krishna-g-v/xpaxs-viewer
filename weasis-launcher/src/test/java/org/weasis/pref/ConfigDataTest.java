/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.pref;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigDataTest {

  /** Security: properties naming directories must not escape their parent folder. */
  @Test
  void launchPropertiesCannotInjectPathSeparators() {
    List.of(
            ConfigData.P_WEASIS_SOURCE_ID,
            ConfigData.P_WEASIS_PROFILE,
            ConfigData.P_WEASIS_USER,
            ConfigData.P_WEASIS_PATH)
        .forEach(System::clearProperty);

    ConfigData configData =
        new ConfigData(
            new String[] {
              "weasis://?$weasis:config" // NON-NLS
                  + " pro=\"weasis.source.id 45D86FE2/../../home/user/Desktop\""
                  + " pro=\"weasis.profile ../../etc\""
                  + " pro=\"weasis.user José Müller\""
                  + " pro=\"user.home /tmp/attacker\""
                  + " pro=\"java.io.tmpdir /tmp/attacker\""
                  + " pro=\"gosh.port 17181\""
                  + " pro=\"https.proxyHost 127.0.0.1\""
            });

    String sourceId = configData.getSourceID();
    assertAll(
        // A rejected property falls back to the internally computed 8-digit hexadecimal hash
        () -> assertTrue(sourceId.matches("[0-9A-F]{8}"), sourceId),
        // Namespaces owned by the JVM are never redefined
        () -> assertNull(configData.getProperty("user.home")), // NON-NLS
        () -> assertNull(configData.getProperty("java.io.tmpdir")), // NON-NLS
        () -> assertNotEquals("/tmp/attacker", System.getProperty("user.home")), // NON-NLS
        // A folder name containing a path separator falls back to the configured default
        () -> assertEquals("default", configData.getProperty(ConfigData.P_WEASIS_PROFILE)),
        // Documented launch properties are still applied, without mangling the non-ASCII letters
        () ->
            assertEquals(
                "José Müller", configData.getProperty(ConfigData.P_WEASIS_USER)), // NON-NLS
        () -> assertEquals("17181", configData.getProperty("gosh.port")), // NON-NLS
        () -> assertEquals("127.0.0.1", configData.getProperty("https.proxyHost"))); // NON-NLS
  }

  private static ConfigData launch(String... args) {
    // A launch leaves its properties in the JVM, where the next one would read them back
    List.of(ConfigData.P_WEASIS_PROFILE, ConfigData.P_WEASIS_SOURCE_ID)
        .forEach(System::clearProperty);
    return new ConfigData(args);
  }

  @Test
  void profileOption_givenAsTwoArguments_selectsTheProfile() {
    ConfigData configData =
        launch(
            "$dicom:get", "-w", "https://pacs.example.org/wado", "-u", "radiologist@example.org");

    assertAll(
        () ->
            assertEquals(
                "radiologist@example.org", configData.getProperty(ConfigData.P_WEASIS_PROFILE)),
        () ->
            assertEquals(
                List.of("dicom:get -w https://pacs.example.org/wado"), configData.getArguments()));
  }

  @Test
  void profileOption_givenAtTheEndOfTheLastArgument_selectsTheProfile() {
    ConfigData configData =
        launch("$dicom:get -w https://pacs.example.org/wado -u radiologist@example.org");

    assertAll(
        () ->
            assertEquals(
                "radiologist@example.org", configData.getProperty(ConfigData.P_WEASIS_PROFILE)),
        () ->
            assertEquals(
                List.of("dicom:get -w https://pacs.example.org/wado"), configData.getArguments()));
  }

  @Test
  void profileOption_aloneAsTheLastArgument_leavesNoEmptyArgument() {
    ConfigData configData = launch("-u radiologist@example.org");

    assertEquals("radiologist@example.org", configData.getProperty(ConfigData.P_WEASIS_PROFILE));
    assertTrue(configData.getArguments().isEmpty());
  }

  @Test
  void profileOption_withAUrl_isTheUrlOptionOfTheCommands() {
    ConfigData configData =
        launch(
            "$dicom:rs", "-u", "https://pacs.example.org/dicom-web", "-r", "StudyInstanceUID=1.2");

    assertAll(
        () -> assertEquals("default", configData.getProperty(ConfigData.P_WEASIS_PROFILE)),
        () ->
            assertEquals(
                List.of("dicom:rs -u https://pacs.example.org/dicom-web -r StudyInstanceUID=1.2"),
                configData.getArguments()));
  }

  @Test
  void profileOption_cannotBeAPath() {
    ConfigData configData = launch("-u", "../../etc");

    assertEquals("default", configData.getProperty(ConfigData.P_WEASIS_PROFILE));
  }

  @Test
  void profileOption_doesNotReplaceAProfileGivenByTheLaunchUri() {
    ConfigData configData =
        launch("weasis://?$weasis:config pro=\"weasis.profile from-uri\"", "-u", "from-option");

    assertEquals("from-uri", configData.getProperty(ConfigData.P_WEASIS_PROFILE));
  }

  @Test
  void profiles_haveTheirOwnSingleInstanceId() {
    String first = launch("-u", "first@example.org").getSourceID();
    String second = launch("-u", "second@example.org").getSourceID();

    assertNotEquals(first, second);
  }

  @Test
  void launchLinks_useTheXpaxsProtocolAndStillAcceptTheUpstreamOne() {
    ConfigData xpaxs = launch("xpaxs://?$weasis:config pro=\"weasis.profile from-xpaxs\"");
    ConfigData upstream = launch("weasis://?$weasis:config pro=\"weasis.profile from-weasis\"");

    assertAll(
        () -> assertEquals("from-xpaxs", xpaxs.getProperty(ConfigData.P_WEASIS_PROFILE)),
        () -> assertEquals("from-weasis", upstream.getProperty(ConfigData.P_WEASIS_PROFILE)));
  }
}

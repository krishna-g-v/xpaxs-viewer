/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.viewer2d.ModalityViewDefaults.DisplayMode;
import org.weasis.dicom.viewer2d.ModalityViewDefaults.ViewDefault;

class ModalityViewDefaultsTest {

  private static final String SPEC = ModalityViewDefaults.DEFAULT_SPEC;

  @Test
  void parse_readsGridAndModeOfEveryEntry() {
    Map<String, ViewDefault> defaults = ModalityViewDefaults.parse(SPEC);
    assertEquals(3, defaults.size());
    assertEquals(new ViewDefault(new Dimension(6, 2), DisplayMode.STACK), defaults.get("CT"));
    assertEquals(new ViewDefault(new Dimension(4, 3), DisplayMode.TILE), defaults.get("US"));
    assertEquals(new ViewDefault(new Dimension(2, 1), DisplayMode.TILE), defaults.get("CR+DX"));
  }

  @Test
  void parse_modeIsOptionalAndCaseInsensitive() {
    Map<String, ViewDefault> defaults = ModalityViewDefaults.parse("mr=2x2, pt=1x2:tile");
    assertEquals(DisplayMode.STACK, defaults.get("MR").mode());
    assertEquals(DisplayMode.TILE, defaults.get("PT").mode());
  }

  @Test
  void parse_skipsInvalidEntries() {
    Map<String, ViewDefault> defaults =
        ModalityViewDefaults.parse("CT=2x6:STACK,,=1x1,XA=bogus,NM=2x2:SIDEWAYS,MR=3x3:TILE:x,US");
    assertEquals(1, defaults.size());
    assertTrue(defaults.containsKey("CT"));
    assertTrue(ModalityViewDefaults.parse(null).isEmpty());
  }

  @Test
  void format_roundTripsTheEntries() {
    assertEquals(SPEC, ModalityViewDefaults.format(ModalityViewDefaults.parse(SPEC)));
  }

  @Test
  void find_matchesAnyModalityOfAnEntryIgnoringCase() {
    assertEquals(DisplayMode.TILE, ModalityViewDefaults.find(SPEC, "dx").orElseThrow().mode());
    assertEquals(DisplayMode.TILE, ModalityViewDefaults.find(SPEC, "CR").orElseThrow().mode());
    assertEquals("2x6", ModalityViewDefaults.find(SPEC, "CT").orElseThrow().gridId());
    assertTrue(ModalityViewDefaults.find(SPEC, "MR").isEmpty());
    assertTrue(ModalityViewDefaults.find(SPEC, null).isEmpty());
  }

  @Test
  void slots_areTheGridCellsForAStackAndOneForTiles() {
    assertEquals(12, ModalityViewDefaults.find(SPEC, "CT").orElseThrow().slots());
    assertEquals(1, ModalityViewDefaults.find(SPEC, "US").orElseThrow().slots());
  }

  @Test
  void forSeries_keepsTheDefaultOnlyWhenTheSeriesFitInIt() {
    assertTrue(ModalityViewDefaults.forSeries(SPEC, "CT", 12).isPresent());
    assertTrue(ModalityViewDefaults.forSeries(SPEC, "CT", 13).isEmpty());
    assertTrue(ModalityViewDefaults.forSeries(SPEC, "US", 1).isPresent());
    assertTrue(ModalityViewDefaults.forSeries(SPEC, "US", 2).isEmpty());
  }

  @Test
  void gridId_isRowsByColumns() {
    assertEquals("3x4", new ViewDefault(new Dimension(4, 3), DisplayMode.STACK).gridId());
  }
}

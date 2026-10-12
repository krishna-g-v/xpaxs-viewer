/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d.pref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.weasis.dicom.viewer2d.ModalityViewDefaults;

class ModalityViewDefaultsPanelTest {

  @Test
  void value_returnsWhatWasLoaded() {
    ModalityViewDefaultsPanel panel = new ModalityViewDefaultsPanel();
    panel.load(ModalityViewDefaults.DEFAULT_SPEC);
    assertEquals(ModalityViewDefaults.DEFAULT_SPEC, panel.value());
  }

  @Test
  void value_keepsTheEntriesThePanelDoesNotShow() {
    ModalityViewDefaultsPanel panel = new ModalityViewDefaultsPanel();
    panel.load("CT=2x6:STACK,US=3x4:TILE,CR+DX=1x2:TILE,MR=2x2:STACK");
    assertEquals("CT=2x6:STACK,US=3x4:TILE,CR+DX=1x2:TILE,MR=2x2:STACK", panel.value());
  }

  @Test
  void value_offersAGridOutsideTheListWhenItWasConfigured() {
    ModalityViewDefaultsPanel panel = new ModalityViewDefaultsPanel();
    panel.load("CT=5x5:TILE");
    assertEquals("CT=5x5:TILE,US=1x1:STACK,CR+DX=1x1:STACK", panel.value());
  }
}

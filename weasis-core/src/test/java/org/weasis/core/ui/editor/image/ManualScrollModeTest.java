/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ManualScrollModeTest {

  @Test
  void fromName_returnsTheMatchingMode() {
    assertEquals(ManualScrollMode.INDEX, ManualScrollMode.fromName("INDEX"));
    assertEquals(ManualScrollMode.LOCATION, ManualScrollMode.fromName("LOCATION"));
  }

  @Test
  void fromName_fallsBackToLocationForUnknownOrMissingNames() {
    assertEquals(ManualScrollMode.LOCATION, ManualScrollMode.fromName("bogus"));
    assertEquals(ManualScrollMode.LOCATION, ManualScrollMode.fromName(null));
  }

  @Test
  void everyModeHasATitleAndATooltip() {
    for (ManualScrollMode mode : ManualScrollMode.values()) {
      assertFalse(mode.getTitle().isBlank());
      assertFalse(mode.getTooltip().isBlank());
      assertNotEquals('!', mode.getTitle().charAt(0), "missing message key: " + mode);
    }
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void synchManager_defaultsToLocationAndKeepsTheChoice() {
    SynchManager<?> manager = new DefaultSynchManager(Mockito.mock(ImageViewerEventManager.class));
    assertEquals(ManualScrollMode.LOCATION, manager.getManualScrollMode());
    manager.setManualScrollMode(ManualScrollMode.INDEX);
    assertEquals(ManualScrollMode.INDEX, manager.getManualScrollMode());
    assertThrows(NullPointerException.class, () -> manager.setManualScrollMode(null));
  }
}

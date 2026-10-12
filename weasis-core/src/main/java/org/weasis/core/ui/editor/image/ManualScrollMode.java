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

import java.util.Arrays;
import org.weasis.core.Messages;

/** How a manually linked view follows the scrolling of the view it is paired with. */
public enum ManualScrollMode {

  /** Keeps the slice-location offset recorded when the views were linked (millimetres). */
  LOCATION("ManualScrollMode.location", "ManualScrollMode.location_tooltip"),

  /** Keeps the image-number offset recorded when the views were linked (one slice per slice). */
  INDEX("ManualScrollMode.index", "ManualScrollMode.index_tooltip");

  private final String titleKey;
  private final String tooltipKey;

  ManualScrollMode(String titleKey, String tooltipKey) {
    this.titleKey = titleKey;
    this.tooltipKey = tooltipKey;
  }

  public String getTitle() {
    return Messages.getString(titleKey);
  }

  public String getTooltip() {
    return Messages.getString(tooltipKey);
  }

  /** Returns the mode with the given name, or {@link #LOCATION} when the name is unknown. */
  public static ManualScrollMode fromName(String name) {
    return Arrays.stream(values()).filter(m -> m.name().equals(name)).findFirst().orElse(LOCATION);
  }
}

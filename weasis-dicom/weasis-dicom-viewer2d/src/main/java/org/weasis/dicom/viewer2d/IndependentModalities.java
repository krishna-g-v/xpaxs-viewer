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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.dcm4che3.data.Tag;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.dicom.codec.TagD;

/**
 * Modalities whose images are independent of each other, such as the projection radiographs: each
 * image is shown best-fit, and views of such series are never synchronized together, even when
 * their images share a Frame of Reference UID.
 *
 * <p>The modalities are named by the {@value #P_MODALITIES} preference, as DICOM Modality codes.
 */
final class IndependentModalities {

  static final String P_MODALITIES = "weasis.viewer.independent.modalities"; // NON-NLS
  static final String DEFAULT_MODALITIES = "CR,DX"; // NON-NLS

  private IndependentModalities() {}

  /** Returns the upper case modality codes of a comma separated list. */
  static Set<String> parse(String spec) {
    if (spec == null) {
      return Set.of();
    }
    return Arrays.stream(spec.split(","))
        .map(String::trim)
        .filter(code -> !code.isEmpty())
        .map(code -> code.toUpperCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }

  static boolean contains(String spec, String modality) {
    return modality != null && parse(spec).contains(modality.trim().toUpperCase(Locale.ROOT));
  }

  /** Tells whether the series belongs to one of the configured modalities. */
  static boolean isIndependent(MediaSeries<?> series) {
    return isIndependent(series, configuredModalities());
  }

  static boolean isIndependent(MediaSeries<?> series, String spec) {
    if (series == null) {
      return false;
    }
    return contains(spec, TagD.getTagValue(series, Tag.Modality, String.class));
  }

  private static String configuredModalities() {
    return GuiUtils.getUICore()
        .getSystemPreferences()
        .getProperty(P_MODALITIES, DEFAULT_MODALITIES);
  }
}

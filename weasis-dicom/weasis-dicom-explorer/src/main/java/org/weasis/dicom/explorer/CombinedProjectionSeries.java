/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer;

import java.util.List;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomSeries;

/**
 * A display-only series gathering the images of several series of a study. It shares the image
 * instances of its source series, so disposing it only drops the references and never the images.
 */
public class CombinedProjectionSeries extends DicomSeries {

  public CombinedProjectionSeries(String seriesInstanceUID) {
    super(seriesInstanceUID);
  }

  /** Tells whether any image of the other series is shown by this series. */
  public boolean sharesImagesWith(DicomSeries other) {
    List<DicomImageElement> own = copyOfMedias(null, null);
    return other.copyOfMedias(null, null).stream().anyMatch(own::contains);
  }

  @Override
  public synchronized void dispose() {
    // The images belong to the source series: forget them before the generic disposal.
    removeAllMedias();
    super.dispose();
  }
}

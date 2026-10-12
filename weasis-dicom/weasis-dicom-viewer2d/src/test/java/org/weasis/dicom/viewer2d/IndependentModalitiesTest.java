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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Test;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.TagD;

@SuppressWarnings({"unchecked", "rawtypes"})
class IndependentModalitiesTest {

  private static MediaSeries<DicomImageElement> series(String modality) {
    MediaSeries<DicomImageElement> series = mock(MediaSeries.class);
    when(series.getTagValue(TagD.get(Tag.Modality))).thenReturn(modality);
    return series;
  }

  @Test
  void parse_normalizesAndDropsBlankCodes() {
    assertEquals(Set.of("CR", "DX", "MG"), IndependentModalities.parse(" cr, DX ,, mg "));
    assertTrue(IndependentModalities.parse("").isEmpty());
    assertTrue(IndependentModalities.parse(null).isEmpty());
  }

  @Test
  void contains_ignoresCaseAndMissingModality() {
    assertTrue(IndependentModalities.contains("CR,DX", "dx"));
    assertFalse(IndependentModalities.contains("CR,DX", "CT"));
    assertFalse(IndependentModalities.contains("CR,DX", null));
    assertFalse(IndependentModalities.contains("", "CR"));
  }

  @Test
  void isIndependent_followsTheConfiguredModalities() {
    assertTrue(IndependentModalities.isIndependent(series("CR"), "CR,DX"));
    assertTrue(IndependentModalities.isIndependent(series("DX"), "CR,DX"));
    assertFalse(IndependentModalities.isIndependent(series("CT"), "CR,DX"));
    assertFalse(IndependentModalities.isIndependent(series(null), "CR,DX"));
    assertFalse(IndependentModalities.isIndependent(null, "CR,DX"));
    assertTrue(IndependentModalities.isIndependent(series("CT"), "CT"));
    assertFalse(IndependentModalities.isIndependent(series("CR"), ""));
  }

  @Test
  void defaultModalities_areTheProjectionRadiographs() {
    assertEquals(
        Set.of("CR", "DX"), IndependentModalities.parse(IndependentModalities.DEFAULT_MODALITIES));
  }
}

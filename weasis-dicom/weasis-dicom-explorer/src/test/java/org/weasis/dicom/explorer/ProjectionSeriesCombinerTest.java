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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Test;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.TagD;

class ProjectionSeriesCombinerTest {

  private static DicomSeries series(String uid, String modality, Integer number, int images) {
    DicomSeries series = new DicomSeries(uid);
    series.setTag(TagD.get(Tag.Modality), modality);
    series.setTag(TagD.get(Tag.SeriesNumber), number);
    for (int i = 0; i < images; i++) {
      series.add(mock(DicomImageElement.class));
    }
    return series;
  }

  private static DicomModel modelWith(MediaSeriesGroup study, DicomSeries... children) {
    DicomModel model = mock(DicomModel.class);
    when(model.getChildren(study)).thenReturn(List.of(children));
    return model;
  }

  @Test
  void parseModalities_normalizesTheCodes() {
    assertEquals(
        Set.of("CR", "DX", "PX"), ProjectionSeriesCombiner.parseModalities(" cr, DX ,,px"));
    assertTrue(ProjectionSeriesCombiner.parseModalities(null).isEmpty());
    assertTrue(ProjectionSeriesCombiner.parseModalities("").isEmpty());
  }

  @Test
  void combinedUid_isStablePerStudyAndIsAnOid() {
    String uid = ProjectionSeriesCombiner.combinedUid("1.2.3");
    assertEquals(uid, ProjectionSeriesCombiner.combinedUid("1.2.3"));
    assertNotEquals(uid, ProjectionSeriesCombiner.combinedUid("1.2.4"));
    assertTrue(uid.startsWith("2.25."));
    assertTrue(uid.length() <= 64);
  }

  @Test
  void candidates_keepsTheConfiguredModalitiesOrderedBySeriesNumber() {
    DicomSeries second = series("s2", "DX", 2, 1);
    DicomSeries first = series("s1", "CR", 1, 1);
    DicomSeries ct = series("s3", "CT", 3, 1);
    DicomSeries empty = series("s4", "DX", 4, 0);
    DicomSeries unnumbered = series("s5", "PX", null, 1);
    MediaSeriesGroup study = mock(MediaSeriesGroup.class);
    DicomModel model = modelWith(study, unnumbered, second, ct, empty, first);

    List<DicomSeries> result =
        ProjectionSeriesCombiner.candidates(model, study, Set.of("CR", "DX", "PX"));

    assertEquals(List.of(first, second, unnumbered), result);
  }

  @Test
  void candidates_ignoresAnExistingCombinedSeries() {
    DicomSeries a = series("a", "CR", 1, 1);
    DicomSeries b = series("b", "CR", 2, 1);
    CombinedProjectionSeries combined = new CombinedProjectionSeries("c");
    combined.setTag(TagD.get(Tag.Modality), "CR");
    combined.add(mock(DicomImageElement.class));
    MediaSeriesGroup study = mock(MediaSeriesGroup.class);
    DicomModel model = modelWith(study, a, combined, b);

    assertEquals(List.of(a, b), ProjectionSeriesCombiner.candidates(model, study, Set.of("CR")));
  }

  @Test
  void build_sharesTheImagesInTheOrderOfTheSources() {
    DicomSeries a = series("a", "CR", 1, 2);
    DicomSeries b = series("b", "CR", 2, 3);

    CombinedProjectionSeries combined =
        ProjectionSeriesCombiner.build("1.2.3", List.of(a, b)).orElseThrow();

    List<DicomImageElement> expected = new java.util.ArrayList<>(a.copyOfMedias(null, null));
    expected.addAll(b.copyOfMedias(null, null));
    List<DicomImageElement> actual = combined.copyOfMedias(null, null);
    assertEquals(5, actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertSame(expected.get(i), actual.get(i));
    }
    assertEquals(2, a.size(null));
    assertEquals(3, b.size(null));
  }

  @Test
  void build_withoutImagesGivesNothing() {
    assertTrue(ProjectionSeriesCombiner.build("1.2.3", List.of(series("a", "CR", 1, 0))).isEmpty());
  }

  @Test
  void dispose_neverDisposesTheSharedImages() {
    DicomSeries a = series("a", "CR", 1, 2);
    List<DicomImageElement> images = a.copyOfMedias(null, null);
    CombinedProjectionSeries combined =
        ProjectionSeriesCombiner.build("1.2.3", List.of(a)).orElseThrow();

    combined.dispose();

    images.forEach(image -> verify(image, never()).dispose());
    assertEquals(0, combined.size(null));
    assertEquals(2, a.size(null));
  }

  @Test
  void sharesImagesWith_tellsWhichSourcesAreShown() {
    DicomSeries a = series("a", "CR", 1, 1);
    DicomSeries b = series("b", "CR", 2, 1);
    DicomSeries other = series("c", "CR", 3, 1);
    CombinedProjectionSeries combined =
        ProjectionSeriesCombiner.build("1.2.3", List.of(a, b)).orElseThrow();

    assertTrue(combined.sharesImagesWith(a));
    assertTrue(combined.sharesImagesWith(b));
    assertFalse(combined.sharesImagesWith(other));
  }
}

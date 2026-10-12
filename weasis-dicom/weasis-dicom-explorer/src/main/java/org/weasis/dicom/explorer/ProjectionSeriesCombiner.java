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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import org.dcm4che3.data.Tag;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.TagW;
import org.weasis.dicom.codec.DcmMediaReader;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.TagD;

/**
 * Builds the non-destructive "all X-ray images of the study" series.
 *
 * <p>Projection radiographs are often stored one series per exposure. The combined series lists the
 * images of all the series of a study that have a configured modality (see {@value #P_MODALITIES}),
 * ordered by Series Number then by the order of each series. The source series are left untouched
 * and the combined series is dropped as soon as one of them is removed.
 */
public final class ProjectionSeriesCombiner {

  public static final String P_MODALITIES = "weasis.viewer.combine.modalities"; // NON-NLS
  public static final String DEFAULT_MODALITIES = "CR,DX,PX"; // NON-NLS

  private static final String UID_ROOT = "2.25."; // UUID derived OID (ITU-T X.667)

  private static final Map<DicomModel, Boolean> WATCHED_MODELS =
      Collections.synchronizedMap(new WeakHashMap<>());

  private ProjectionSeriesCombiner() {}

  /** Returns the upper case modality codes of a comma separated list. */
  public static Set<String> parseModalities(String spec) {
    if (spec == null) {
      return Set.of();
    }
    return Arrays.stream(spec.split(","))
        .map(String::trim)
        .filter(code -> !code.isEmpty())
        .map(code -> code.toUpperCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Returns the series that a combined series of the study would gather, in display order. */
  public static List<DicomSeries> candidates(
      DicomModel model, MediaSeriesGroup study, Set<String> modalities) {
    if (study == null || modalities.isEmpty()) {
      return List.of();
    }
    return model.getChildren(study).stream()
        .filter(DicomSeries.class::isInstance)
        .map(DicomSeries.class::cast)
        .filter(s -> !(s instanceof CombinedProjectionSeries))
        .filter(s -> s.size(null) > 0 && isCombinable(s, modalities))
        .sorted(Comparator.comparing(ProjectionSeriesCombiner::seriesNumber))
        .toList();
  }

  /** Tells whether the study of this series has at least two series to gather with it. */
  public static boolean canCombine(DicomModel model, DicomSeries series) {
    Set<String> modalities = configuredModalities();
    return !(series instanceof CombinedProjectionSeries)
        && isCombinable(series, modalities)
        && candidates(model, model.getParent(series, DicomModel.study), modalities).size() > 1;
  }

  /** Builds the combined series of the sources, or an empty value when they hold no image. */
  public static Optional<CombinedProjectionSeries> build(
      String studyInstanceUID, List<DicomSeries> sources) {
    List<DicomImageElement> images = new ArrayList<>();
    sources.forEach(s -> images.addAll(s.copyOfMedias(null, null)));
    if (images.isEmpty()) {
      return Optional.empty();
    }
    CombinedProjectionSeries combined = new CombinedProjectionSeries(combinedUid(studyInstanceUID));
    // Series.add keeps this order, unlike DicomSeries.addMedia which sorts by instance number.
    images.forEach(combined::add);
    if (images.getFirst().getMediaReader() instanceof DcmMediaReader reader) {
      reader.writeMetaData(combined);
    }
    combined.setTagNoNull(
        TagD.get(Tag.SeriesDescription),
        Messages.getString("ProjectionSeriesCombiner.description") + " (" + images.size() + ")");
    combined.setTag(TagD.get(Tag.SeriesNumber), null);
    return Optional.of(combined);
  }

  /** Shows all the combinable images of the study of this series, in the default viewer. */
  public static Optional<CombinedProjectionSeries> show(DicomModel model, DicomSeries series) {
    MediaSeriesGroup study = model.getParent(series, DicomModel.study);
    List<DicomSeries> sources = candidates(model, study, configuredModalities());
    if (study == null || sources.size() < 2) {
      return Optional.empty();
    }
    watch(model);
    String studyUid = (String) study.getTagValue(study.getTagID());
    Optional<CombinedProjectionSeries> existing = findCombined(model, study);
    if (existing.isPresent() && sameImages(existing.get(), sources)) {
      return existing;
    }
    existing.ifPresent(c -> drop(model, c));

    Optional<CombinedProjectionSeries> built = build(studyUid, sources);
    built.ifPresent(
        c -> {
          c.setTag(TagW.ExplorerModel, model);
          model.addHierarchyNode(study, c);
          model.firePropertyChange(
              new ObservableEvent(ObservableEvent.BasicAction.ADD, model, null, c));
        });
    return built;
  }

  static String combinedUid(String studyInstanceUID) {
    UUID uuid =
        UUID.nameUUIDFromBytes(
            ("xpaxs-combined:" + studyInstanceUID).getBytes(StandardCharsets.UTF_8));
    String value =
        new BigInteger(
                1,
                ByteBuffer.allocate(16)
                    .putLong(uuid.getMostSignificantBits())
                    .putLong(uuid.getLeastSignificantBits())
                    .array())
            .toString();
    return UID_ROOT + value;
  }

  private static boolean sameImages(CombinedProjectionSeries combined, List<DicomSeries> sources) {
    List<DicomImageElement> expected = new ArrayList<>();
    sources.forEach(s -> expected.addAll(s.copyOfMedias(null, null)));
    return expected.equals(combined.copyOfMedias(null, null));
  }

  private static Optional<CombinedProjectionSeries> findCombined(
      DicomModel model, MediaSeriesGroup study) {
    return model.getChildren(study).stream()
        .filter(CombinedProjectionSeries.class::isInstance)
        .map(CombinedProjectionSeries.class::cast)
        .findFirst();
  }

  private static void drop(DicomModel model, CombinedProjectionSeries combined) {
    model.removeSeriesWithoutDisposingMedias(combined);
    combined.dispose();
  }

  private static void watch(DicomModel model) {
    if (WATCHED_MODELS.putIfAbsent(model, Boolean.TRUE) == null) {
      model.addPropertyChangeListener(
          evt -> {
            if (evt instanceof ObservableEvent event
                && event.getActionCommand() == ObservableEvent.BasicAction.REMOVE
                && event.getNewValue() instanceof DicomSeries removed
                && !(removed instanceof CombinedProjectionSeries)) {
              dropCombinedUsing(model, removed);
            }
          });
    }
  }

  /** The combined series must not outlive the images of a removed source series. */
  private static void dropCombinedUsing(DicomModel model, DicomSeries removed) {
    MediaSeriesGroup study = model.getParent(removed, DicomModel.study);
    if (study != null) {
      findCombined(model, study)
          .filter(c -> c.sharesImagesWith(removed))
          .ifPresent(c -> drop(model, c));
    }
  }

  private static boolean isCombinable(DicomSeries series, Set<String> modalities) {
    String modality = TagD.getTagValue(series, Tag.Modality, String.class);
    return modality != null && modalities.contains(modality.trim().toUpperCase(Locale.ROOT));
  }

  private static int seriesNumber(DicomSeries series) {
    Integer number = TagD.getTagValue(series, Tag.SeriesNumber, Integer.class);
    return number == null ? Integer.MAX_VALUE : number;
  }

  private static Set<String> configuredModalities() {
    return parseModalities(
        GuiUtils.getUICore().getSystemPreferences().getProperty(P_MODALITIES, DEFAULT_MODALITIES));
  }
}

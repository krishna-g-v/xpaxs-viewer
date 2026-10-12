/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.dcm4che3.data.Tag;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.KOSpecialElement;
import org.weasis.dicom.codec.TagD;

/**
 * The selection an export starts with when the series to export have key objects: the key images,
 * the key objects and the annotations of the series, instead of whole studies.
 */
final class KeyObjectSelection {

  /** Depth of a series node in the export tree: root, patient, study, series. */
  private static final int SERIES_LEVEL = 4;

  private KeyObjectSelection() {}

  /**
   * Returns the paths to check, or none when the series of the checked paths have no key image.
   *
   * @param checkedPaths the paths checked by default, the series to export
   * @param keyObjects gives the key objects that refer to a series
   */
  static List<TreePath> pathsFor(
      TreePath[] checkedPaths,
      Function<MediaSeries<DicomImageElement>, Collection<KOSpecialElement>> keyObjects) {
    List<DefaultMutableTreeNode> checkedSeries = seriesNodes(checkedPaths);
    Set<String> keyImages = referencedInstances(checkedSeries, keyObjects);
    if (keyImages.isEmpty()) {
      return List.of();
    }
    Set<Object> exported = new HashSet<>();
    checkedSeries.forEach(node -> exported.add(node.getUserObject()));

    Set<DefaultMutableTreeNode> studies = new LinkedHashSet<>();
    checkedSeries.forEach(node -> studies.add((DefaultMutableTreeNode) node.getParent()));

    Set<TreePath> paths = new LinkedHashSet<>();
    for (DefaultMutableTreeNode study : studies) {
      for (DefaultMutableTreeNode seriesNode : children(study)) {
        if (seriesNode.getUserObject() instanceof MediaSeries<?> series) {
          addSeriesSelection(paths, seriesNode, series, exported, keyImages);
        }
      }
    }
    // Key objects whose images are not in the tree would leave nothing worth exporting
    boolean hasKeyImage =
        paths.stream()
            .anyMatch(
                path ->
                    isKeyImage(
                        ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject(),
                        keyImages));
    return hasKeyImage ? List.copyOf(paths) : List.of();
  }

  private static void addSeriesSelection(
      Set<TreePath> paths,
      DefaultMutableTreeNode seriesNode,
      MediaSeries<?> series,
      Set<Object> exported,
      Set<String> keyImages) {
    Object source = series.getTagValue(CheckTreeModel.SourceSeriesForPR);
    if (source != null) {
      // The annotations of an exported series are one series of their own in the tree
      if (exported.contains(source)) {
        paths.add(new TreePath(seriesNode.getPath()));
      }
      return;
    }
    for (DefaultMutableTreeNode node : children(seriesNode)) {
      Object element = node.getUserObject();
      if (element instanceof KOSpecialElement || isKeyImage(element, keyImages)) {
        paths.add(new TreePath(node.getPath()));
      }
    }
  }

  private static boolean isKeyImage(Object element, Set<String> keyImages) {
    return element instanceof MediaElement media
        && keyImages.contains(TagD.getTagValue(media, Tag.SOPInstanceUID, String.class));
  }

  private static List<DefaultMutableTreeNode> seriesNodes(TreePath[] paths) {
    if (paths == null) {
      return List.of();
    }
    return Arrays.stream(paths)
        .filter(path -> path.getPathCount() == SERIES_LEVEL)
        .map(path -> (DefaultMutableTreeNode) path.getLastPathComponent())
        .filter(node -> node.getUserObject() instanceof MediaSeries<?>)
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static Set<String> referencedInstances(
      List<DefaultMutableTreeNode> seriesNodes,
      Function<MediaSeries<DicomImageElement>, Collection<KOSpecialElement>> keyObjects) {
    Set<String> references = new HashSet<>();
    for (DefaultMutableTreeNode node : seriesNodes) {
      Collection<KOSpecialElement> elements =
          keyObjects.apply((MediaSeries<DicomImageElement>) node.getUserObject());
      if (elements != null) {
        elements.forEach(element -> references.addAll(element.getReferencedSOPInstanceUIDSet()));
      }
    }
    return references;
  }

  private static List<DefaultMutableTreeNode> children(DefaultMutableTreeNode node) {
    return Collections.list(node.children()).stream()
        .map(DefaultMutableTreeNode.class::cast)
        .toList();
  }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.Test;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.KOSpecialElement;
import org.weasis.dicom.codec.TagD;

@SuppressWarnings({"unchecked", "rawtypes"})
class KeyObjectSelectionTest {

  private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
  private final DefaultMutableTreeNode patient = new DefaultMutableTreeNode("patient");
  private final DefaultMutableTreeNode study = new DefaultMutableTreeNode("study");
  private final MediaSeries<DicomImageElement> series = mock(MediaSeries.class);
  private final DefaultMutableTreeNode seriesNode = new DefaultMutableTreeNode(series);
  private final KOSpecialElement keyObject = mock(KOSpecialElement.class);

  KeyObjectSelectionTest() {
    root.add(patient);
    patient.add(study);
    study.add(seriesNode);
  }

  private DefaultMutableTreeNode image(String sopInstanceUid) {
    MediaElement media = mock(MediaElement.class);
    when(media.getTagValue(TagD.get(Tag.SOPInstanceUID))).thenReturn(sopInstanceUid);
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(media, false);
    seriesNode.add(node);
    return node;
  }

  private static TreePath path(DefaultMutableTreeNode node) {
    return new TreePath(node.getPath());
  }

  private List<TreePath> select(Collection<KOSpecialElement> keyObjects, TreePath... checked) {
    return KeyObjectSelection.pathsFor(checked, s -> keyObjects);
  }

  @Test
  void pathsFor_withoutKeyObjects_selectsNothing() {
    image("1.1");
    assertTrue(select(List.of(), path(seriesNode)).isEmpty());
  }

  @Test
  void pathsFor_selectsTheKeyImagesAndTheirKeyObjectOnly() {
    DefaultMutableTreeNode key1 = image("1.1");
    DefaultMutableTreeNode other = image("1.2");
    DefaultMutableTreeNode key2 = image("1.3");
    DefaultMutableTreeNode koNode = new DefaultMutableTreeNode(keyObject, false);
    seriesNode.add(koNode);
    when(keyObject.getReferencedSOPInstanceUIDSet()).thenReturn(Set.of("1.1", "1.3"));

    List<TreePath> paths = select(List.of(keyObject), path(seriesNode));

    assertEquals(Set.of(path(key1), path(key2), path(koNode)), Set.copyOf(paths));
    assertTrue(!paths.contains(path(other)));
  }

  @Test
  void pathsFor_addsTheAnnotationsOfTheExportedSeriesOnly() {
    DefaultMutableTreeNode key = image("1.1");
    when(keyObject.getReferencedSOPInstanceUIDSet()).thenReturn(Set.of("1.1"));

    MediaSeries<DicomImageElement> annotations = mock(MediaSeries.class);
    when(annotations.getTagValue(CheckTreeModel.SourceSeriesForPR)).thenReturn(series);
    DefaultMutableTreeNode annotationsNode = new DefaultMutableTreeNode(annotations);
    study.add(annotationsNode);

    MediaSeries<DicomImageElement> unrelated = mock(MediaSeries.class);
    when(unrelated.getTagValue(CheckTreeModel.SourceSeriesForPR))
        .thenReturn(mock(MediaSeries.class));
    DefaultMutableTreeNode unrelatedNode = new DefaultMutableTreeNode(unrelated);
    study.add(unrelatedNode);

    List<TreePath> paths = select(List.of(keyObject), path(seriesNode));

    assertEquals(Set.of(path(key), path(annotationsNode)), Set.copyOf(paths));
  }

  @Test
  void pathsFor_ignoresKeyObjectsWhoseImagesAreNotInTheTree() {
    image("1.1");
    seriesNode.add(new DefaultMutableTreeNode(keyObject, false));
    when(keyObject.getReferencedSOPInstanceUIDSet()).thenReturn(Set.of("9.9"));

    assertTrue(select(List.of(keyObject), path(seriesNode)).isEmpty());
  }

  @Test
  void pathsFor_ignoresPathsThatAreNotSeries() {
    image("1.1");
    when(keyObject.getReferencedSOPInstanceUIDSet()).thenReturn(Set.of("1.1"));

    assertTrue(select(List.of(keyObject), path(study)).isEmpty());
    assertTrue(KeyObjectSelection.pathsFor(null, s -> List.of(keyObject)).isEmpty());
  }
}

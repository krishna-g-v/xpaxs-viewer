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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import org.dcm4che3.img.lut.PresetWindowLevel;
import org.junit.jupiter.api.Test;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.opencv.op.lut.LutShape;

@SuppressWarnings({"unchecked", "rawtypes"})
class PresetQuickButtonsTest {

  private static PresetWindowLevel preset(String name, String id) {
    PresetWindowLevel p = new PresetWindowLevel(name, 400.0, 40.0, LutShape.LINEAR);
    p.setId(id);
    return p;
  }

  private static ComboItemListener<Object> presetAction(Object... items) {
    return new ComboItemListener<>(ActionW.PRESET, items) {
      @Override
      public void itemStateChanged(Object object) {
        // selection is read back from the model
      }
    };
  }

  private static ImageViewerEventManager<DicomImageElement> managerWith(
      ComboItemListener<Object> action) {
    ImageViewerEventManager<DicomImageElement> manager = mock(ImageViewerEventManager.class);
    doReturn(Optional.ofNullable(action)).when(manager).getAction(ActionW.PRESET);
    return manager;
  }

  @Test
  void parseEntries_keepsDistinctNonBlankEntriesInOrder() {
    assertEquals(
        List.of("default", "weasis.ct.lung", "x"),
        PresetQuickButtons.parseEntries(" default, weasis.ct.lung ,, x,default"));
    assertTrue(PresetQuickButtons.parseEntries(null).isEmpty());
    assertTrue(PresetQuickButtons.parseEntries("  ").isEmpty());
  }

  @Test
  void findPreset_defaultDesignatesTheFirstOfferedPreset() {
    List<PresetWindowLevel> offered =
        List.of(preset("Image window", null), preset("Lung", "weasis.ct.lung"));
    assertSame(offered.get(0), PresetQuickButtons.findPreset(offered, "default").orElseThrow());
    assertTrue(PresetQuickButtons.findPreset(List.of(), "default").isEmpty());
  }

  @Test
  void findPreset_matchesAConfiguredPresetById() {
    List<PresetWindowLevel> offered =
        List.of(preset("Image window", null), preset("Lung", "weasis.ct.lung"));
    assertSame(
        offered.get(1), PresetQuickButtons.findPreset(offered, "weasis.ct.lung").orElseThrow());
    assertTrue(PresetQuickButtons.findPreset(offered, "weasis.ct.bone").isEmpty());
  }

  @Test
  void create_withoutEntries_addsNothing() {
    assertTrue(PresetQuickButtons.create(managerWith(presetAction()), "").isEmpty());
  }

  @Test
  void create_addsTheWindowLevelButtonThenOneButtonPerEntry() {
    ComboItemListener<Object> action =
        presetAction(preset("Image window", null), preset("Lung", "weasis.ct.lung"));
    ImageViewerEventManager<DicomImageElement> manager = managerWith(action);

    List<JButton> buttons = PresetQuickButtons.create(manager, "default,weasis.ct.lung");

    assertEquals(3, buttons.size());
    assertEquals("W", buttons.get(0).getText());
    assertFalse(buttons.get(1).getText().isBlank());
    assertEquals("Lung", buttons.get(2).getText());

    buttons.get(0).doClick(0);
    verify(manager).changeLeftMouseAction(ActionW.WINLEVEL.cmd());
  }

  @Test
  void create_clickSelectsThePresetAndUnavailablePresetsAreDisabled() {
    PresetWindowLevel lung = preset("Lung", "weasis.ct.lung");
    ComboItemListener<Object> action = presetAction(preset("Image window", null), lung);

    List<JButton> buttons =
        PresetQuickButtons.create(managerWith(action), "default,weasis.ct.lung,weasis.ct.bone");
    JButton lungButton = buttons.get(2);
    JButton boneButton = buttons.get(3);

    assertTrue(lungButton.isEnabled());
    assertFalse(boneButton.isEnabled());

    lungButton.doClick(0);
    assertSame(lung, action.getSelectedItem());
  }

  @Test
  void create_buttonsFollowThePresetsOfTheDisplayedImage() {
    ComboItemListener<Object> action = presetAction(preset("Image window", null));
    List<JButton> buttons = PresetQuickButtons.create(managerWith(action), "weasis.ct.lung");
    JButton lungButton = buttons.get(1);
    assertFalse(lungButton.isEnabled());

    List<Object> next = new ArrayList<>();
    next.add(preset("Image window", null));
    next.add(preset("Lung", "weasis.ct.lung"));
    action.setDataList(next.toArray());

    assertTrue(lungButton.isEnabled());
  }
}

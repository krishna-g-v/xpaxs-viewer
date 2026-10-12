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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.dcm4che3.img.lut.PresetWindowLevel;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.display.WindowPreset;
import org.weasis.dicom.codec.display.WindowPresetRegistry;

/**
 * One-click toolbar buttons selecting a window/level preset, and the one starting the window/level
 * mouse action. The presets are named by the {@value #P_PRESETS} preference, as the id of a
 * configured window preset or {@value #DEFAULT_PRESET} for the first preset of the image.
 */
final class PresetQuickButtons {

  static final String P_PRESETS = "weasis.toolbar.lut.presets"; // NON-NLS
  static final String DEFAULT_PRESETS =
      "default,weasis.ct.brain,weasis.ct.lung,weasis.ct.mediastinum,weasis.ct.abdomen,weasis.ct.bone";
  static final String DEFAULT_PRESET = "default"; // NON-NLS

  private PresetQuickButtons() {}

  /** Returns the distinct, non-blank entries of a comma separated list. */
  static List<String> parseEntries(String spec) {
    Set<String> entries = new LinkedHashSet<>();
    if (spec != null) {
      for (String token : spec.split(",")) {
        String entry = token.trim();
        if (!entry.isEmpty()) {
          entries.add(entry);
        }
      }
    }
    return new ArrayList<>(entries);
  }

  /** Finds the preset an entry designates among the presets offered for the displayed image. */
  static Optional<PresetWindowLevel> findPreset(List<PresetWindowLevel> offered, String entry) {
    if (DEFAULT_PRESET.equals(entry)) {
      return offered.stream().findFirst();
    }
    return offered.stream().filter(p -> entry.equals(p.getId())).findFirst();
  }

  /** Returns the entries configured by the user, or the default ones. */
  static String configuredEntries() {
    return GuiUtils.getUICore().getSystemPreferences().getProperty(P_PRESETS, DEFAULT_PRESETS);
  }

  static List<JButton> create(
      ImageViewerEventManager<DicomImageElement> eventManager, String spec) {
    List<String> entries = parseEntries(spec);
    List<JButton> buttons = new ArrayList<>();
    if (entries.isEmpty()) {
      return buttons;
    }

    JButton windowLevel = new JButton("W"); // NON-NLS
    windowLevel.setToolTipText(ActionW.WINLEVEL.getTitle());
    windowLevel.addActionListener(e -> eventManager.changeLeftMouseAction(ActionW.WINLEVEL.cmd()));
    buttons.add(windowLevel);

    Optional<ComboItemListener<Object>> presetAction = eventManager.getAction(ActionW.PRESET);
    for (String entry : entries) {
      JButton button = new JButton(label(entry));
      button.setToolTipText(label(entry));
      presetAction.ifPresent(
          action -> {
            button.addActionListener(e -> select(action, entry));
            action.registerActionState(button);
            DefaultComboBoxModel<?> model = action.getModel();
            model.addListDataListener(new RefreshOnChange(() -> refresh(button, model, entry)));
            refresh(button, model, entry);
          });
      buttons.add(button);
    }
    return buttons;
  }

  private static String label(String entry) {
    if (DEFAULT_PRESET.equals(entry)) {
      return Messages.getString("LutToolBar.preset_default");
    }
    return WindowPresetRegistry.getInstance().find(entry).map(WindowPreset::name).orElse(entry);
  }

  private static List<PresetWindowLevel> offered(DefaultComboBoxModel<?> model) {
    List<PresetWindowLevel> presets = new ArrayList<>();
    for (int i = 0; i < model.getSize(); i++) {
      if (model.getElementAt(i) instanceof PresetWindowLevel preset) {
        presets.add(preset);
      }
    }
    return presets;
  }

  private static void select(ComboItemListener<Object> action, String entry) {
    findPreset(offered(action.getModel()), entry).ifPresent(action::setSelectedItem);
  }

  private static void refresh(JButton button, DefaultComboBoxModel<?> model, String entry) {
    button.setEnabled(findPreset(offered(model), entry).isPresent());
  }

  private record RefreshOnChange(Runnable refresh) implements ListDataListener {
    @Override
    public void intervalAdded(ListDataEvent e) {
      refresh.run();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
      refresh.run();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
      refresh.run();
    }
  }
}

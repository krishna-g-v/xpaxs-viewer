/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d.pref;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.weasis.core.api.gui.util.GuiUtils;
import org.weasis.dicom.viewer2d.Messages;
import org.weasis.dicom.viewer2d.ModalityViewDefaults;
import org.weasis.dicom.viewer2d.ModalityViewDefaults.DisplayMode;
import org.weasis.dicom.viewer2d.ModalityViewDefaults.ViewDefault;

/** Chooses the layout and the display mode a viewer opens with for CT, US and X-ray series. */
class ModalityViewDefaultsPanel extends JPanel {

  private static final int ITEM_GAP = 10;

  /** The entries shown, by the key of the modalities of the preference. */
  private static final Map<String, String> ROWS = new LinkedHashMap<>();

  static {
    ROWS.put("CT", "CT"); // NON-NLS
    ROWS.put("US", "US"); // NON-NLS
    ROWS.put("CR+DX", Messages.getString("ModalityViewDefaults.xray")); // NON-NLS
  }

  private record Row(String key, JComboBox<String> grid, JComboBox<DisplayMode> mode) {}

  private final List<Row> rows = new ArrayList<>();
  private Map<String, ViewDefault> others = new LinkedHashMap<>();

  ModalityViewDefaultsPanel() {
    super();
    setLayout(new java.awt.GridLayout(0, 1));
    setBorder(GuiUtils.getTitledBorder(Messages.getString("ViewerPrefView.modality_defaults")));
    for (Map.Entry<String, String> row : ROWS.entrySet()) {
      JComboBox<String> grid = new JComboBox<>();
      JComboBox<DisplayMode> mode = new JComboBox<>(DisplayMode.values());
      mode.setRenderer(
          (list, value, index, selected, focus) ->
              new JLabel(Messages.getString("ModalityViewDefaults." + value.name().toLowerCase())));
      rows.add(new Row(row.getKey(), grid, mode));
      JPanel line =
          GuiUtils.getFlowLayoutPanel(
              FlowLayout.LEADING,
              ITEM_GAP,
              ITEM_GAP,
              new JLabel(row.getValue() + ":"),
              new JLabel(Messages.getString("ViewerPrefView.modality_layout")),
              grid,
              new JLabel(Messages.getString("ViewerPrefView.modality_mode")),
              mode);
      add(line);
    }
  }

  /** Shows the defaults of the preference value; the entries it does not show are kept. */
  void load(String spec) {
    Map<String, ViewDefault> defaults = ModalityViewDefaults.parse(spec);
    others = new LinkedHashMap<>(defaults);
    for (Row row : rows) {
      ViewDefault viewDefault = others.remove(row.key());
      List<String> grids = new ArrayList<>(ModalityViewDefaults.GRIDS);
      if (viewDefault != null && !grids.contains(viewDefault.gridId())) {
        grids.add(viewDefault.gridId());
      }
      row.grid().setModel(new DefaultComboBoxModel<>(grids.toArray(new String[0])));
      row.grid().setSelectedItem(viewDefault == null ? "1x1" : viewDefault.gridId());
      row.mode().setSelectedItem(viewDefault == null ? DisplayMode.STACK : viewDefault.mode());
    }
  }

  /** Returns the preference value for the choices made. */
  String value() {
    Map<String, ViewDefault> defaults = new LinkedHashMap<>();
    for (Row row : rows) {
      String grid = (String) row.grid().getSelectedItem();
      ModalityViewDefaults.parse(row.key() + "=" + grid)
          .values()
          .forEach(
              parsed ->
                  defaults.put(
                      row.key(),
                      new ViewDefault(parsed.grid(), (DisplayMode) row.mode().getSelectedItem())));
    }
    defaults.putAll(others);
    return ModalityViewDefaults.format(defaults);
  }
}

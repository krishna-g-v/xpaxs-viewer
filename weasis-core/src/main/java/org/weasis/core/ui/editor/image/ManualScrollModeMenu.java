/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import java.awt.Container;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import org.weasis.core.Messages;
import org.weasis.core.ui.util.TitleMenuItem;

/** Menu entries letting the user choose how manually linked views follow each other. */
public final class ManualScrollModeMenu {

  private ManualScrollModeMenu() {}

  /** Appends the titled mode choice to the menu; the selection is stored in the manager. */
  public static void addTo(Container target, SynchManager<?> manager) {
    target.add(new TitleMenuItem(Messages.getString("ManualScrollMode.title")));
    ButtonGroup group = new ButtonGroup();
    for (ManualScrollMode mode : ManualScrollMode.values()) {
      JRadioButtonMenuItem item =
          new JRadioButtonMenuItem(mode.getTitle(), manager.getManualScrollMode() == mode);
      item.setToolTipText(mode.getTooltip());
      item.addActionListener(e -> manager.setManualScrollMode(mode));
      group.add(item);
      target.add(item);
    }
  }

  /** Tells whether the view takes part in, or could join, a manual link. */
  public static boolean isRelevant(ViewSynchData synchData) {
    return synchData != null
        && (synchData.canBeManuallySynced() || synchData.isManualSynchActivated());
  }
}

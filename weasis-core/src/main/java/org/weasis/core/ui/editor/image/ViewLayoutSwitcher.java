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

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.weasis.core.api.gui.layout.MigLayoutModel;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.media.data.ImageElement;

/**
 * Shows the series of a viewer either as a grid of stacks, one per viewport, or as the tiles of a
 * single viewport. A grid is a dimension of columns (width) by rows (height).
 */
public final class ViewLayoutSwitcher<E extends ImageElement> {

  private final ImageViewerEventManager<E> eventManager;

  public ViewLayoutSwitcher(ImageViewerEventManager<E> eventManager) {
    this.eventManager = eventManager;
  }

  /** Displays the grid as stacks, one per viewport. */
  public void showStack(ImageViewerPlugin<E> container, Dimension grid) {
    if (container == null || !container.supportsViewportPanes()) {
      return;
    }
    for (ViewportPane<E> pane : container.getViewportPanes()) {
      pane.switchToStack();
    }
    setLayout(container, findLayout(grid));
  }

  /** Displays the grid as the tiles of a single viewport, showing the selected series. */
  public void showTiles(ImageViewerPlugin<E> container, Dimension grid) {
    ViewportPane<E> selected = container == null ? null : container.getSelectedViewportPane();
    if (selected == null || !container.supportsViewportPanes()) {
      return;
    }
    if (container.getCellManager().getLayoutModel().getCellCount() > 1) {
      moveSeriesToFirstViewport(container, selected);
      setLayout(container, ImageViewerPlugin.VIEWS_1x1);
    }
    ViewportPane<E> pane = container.getSelectedViewportPane();
    if (pane == null) {
      return;
    }
    pane.switchToTiled(grid.width, grid.height, () -> container.createDefaultView(null));
    eventManager.updateComponentsListener(pane.getSelectedCanvas());
  }

  /** Returns the grid of the layout of the container, in columns by rows. */
  public static Dimension gridOf(ImageViewerPlugin<?> container) {
    return container.getCellManager().getLayoutModel().getGridSize();
  }

  /** Builds the layout of a grid, titled and identified as {@code <rows>x<columns>}. */
  public static MigLayoutModel buildLayout(Dimension grid) {
    return ImageViewerPlugin.buildMigLayoutModel(
        grid.height, grid.width, ImageViewerPlugin.VIEWPORT_CLASS.getName());
  }

  /** The selected container follows the layout action, another one is changed directly. */
  private void setLayout(ImageViewerPlugin<E> container, MigLayoutModel model) {
    if (eventManager.getSelectedView2dContainer() == container) {
      container.changeLayoutModel(model);
    } else {
      container.setLayoutModel(model);
    }
  }

  /** A layout change keeps the first viewports, so the series to keep must be in the first. */
  private void moveSeriesToFirstViewport(ImageViewerPlugin<E> container, ViewportPane<E> selected) {
    List<ViewportPane<E>> panes = container.getViewportPanes();
    if (panes.isEmpty() || panes.getFirst() == selected) {
      return;
    }
    ViewportPane<E> first = panes.getFirst();
    if (selected.isTiled() || first.isTiled()) {
      return;
    }
    first.getSelectedCanvas().setSeries(selected.getSelectedCanvas().getSeries(), null);
    container.setSelectedImagePane(first.getSelectedCanvas());
  }

  private MigLayoutModel findLayout(Dimension grid) {
    String id = grid.height + "x" + grid.width;
    Optional<ComboItemListener<MigLayoutModel>> layoutAction =
        eventManager.getAction(ActionW.LAYOUT);
    return layoutAction
        .flatMap(
            a ->
                Arrays.stream(a.getAllItem())
                    .filter(MigLayoutModel.class::isInstance)
                    .map(MigLayoutModel.class::cast)
                    .filter(m -> id.equals(m.getId()))
                    .findFirst())
        .orElseGet(() -> buildLayout(grid));
  }
}

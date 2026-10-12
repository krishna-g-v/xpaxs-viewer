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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JButton;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;
import org.weasis.core.Messages;
import org.weasis.core.api.gui.layout.MigLayoutModel;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.service.BundlePreferences;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.api.util.ResourceUtil.ActionIcon;

/**
 * One-click toolbar buttons that shorten frequent viewer set-ups: a grid layout, the Stack or Tile
 * display of a series, and the persistence of the mouse actions.
 *
 * <p>Stack and Tile are the two modes of a {@link ViewportPane}. A layout button applies the mode
 * the selected viewport is in: a grid of stacks, or one viewport tiled with that grid.
 */
public final class QuickViewButtons<E extends ImageElement> {

  public static final String P_LAYOUTS = "weasis.toolbar.layout.quick"; // NON-NLS
  public static final String P_VIEW_MODE = "weasis.toolbar.viewmode.buttons"; // NON-NLS
  public static final String P_SAVE_MOUSE = "weasis.toolbar.mouse.save"; // NON-NLS
  public static final String DEFAULT_LAYOUTS = "1x1,1x2,1x3,1x4,2x1,2x2,2x4,2x5,3x1,3x4,3x5,4x2";

  private static final int DEFAULT_TILE_SIZE = 2;

  private final ImageViewerEventManager<E> eventManager;

  public QuickViewButtons(ImageViewerEventManager<E> eventManager) {
    this.eventManager = eventManager;
  }

  /**
   * Parses a comma separated list of {@code <rows>x<cols>} grids. Invalid and duplicated entries
   * are ignored.
   */
  public static List<Dimension> parseGrids(String spec) {
    Set<Dimension> grids = new LinkedHashSet<>();
    if (spec != null) {
      for (String token : spec.split(",")) {
        parseGrid(token.trim()).ifPresent(grids::add);
      }
    }
    return new ArrayList<>(grids);
  }

  /** Returns the grid as a dimension of columns (width) by rows (height). */
  private static Optional<Dimension> parseGrid(String token) {
    String[] parts = token.split("x");
    if (parts.length != 2) {
      return Optional.empty();
    }
    try {
      int rows = Integer.parseInt(parts[0]);
      int cols = Integer.parseInt(parts[1]);
      return rows > 0 && cols > 0 && rows < 50 && cols < 50
          ? Optional.of(new Dimension(cols, rows))
          : Optional.empty();
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  public List<JButton> createLayoutButtons(String spec) {
    List<JButton> buttons = new ArrayList<>();
    Optional<ComboItemListener<MigLayoutModel>> layoutAction =
        eventManager.getAction(ActionW.LAYOUT);
    for (Dimension grid : parseGrids(spec)) {
      MigLayoutModel model = buildLayout(grid);
      JButton button = new JButton(model.getIcon());
      button.setToolTipText(model.toString());
      button.addActionListener(e -> applyGrid(grid));
      layoutAction.ifPresent(a -> a.registerActionState(button));
      buttons.add(button);
    }
    return buttons;
  }

  public List<JButton> createViewModeButtons() {
    JButton stack = new JButton(Messages.getString("QuickViewButtons.stack"));
    stack.setToolTipText(Messages.getString("QuickViewButtons.stack_tooltip"));
    stack.addActionListener(e -> showStack());
    JButton tile = new JButton(Messages.getString("QuickViewButtons.tile"));
    tile.setToolTipText(Messages.getString("QuickViewButtons.tile_tooltip"));
    tile.addActionListener(e -> showTiles());
    eventManager
        .getAction(ActionW.LAYOUT)
        .ifPresent(
            a -> {
              a.registerActionState(stack);
              a.registerActionState(tile);
            });
    return List.of(stack, tile);
  }

  public JButton createSaveMouseActionsButton() {
    JButton button = new JButton(ResourceUtil.getToolBarIcon(ActionIcon.SAVE_MOUSE_ACTIONS));
    button.setToolTipText(Messages.getString("QuickViewButtons.save_mouse_tooltip"));
    button.addActionListener(e -> saveMouseActions());
    return button;
  }

  /** Persists the mouse buttons mapping now, and keeps it as the one saved when quitting. */
  private void saveMouseActions() {
    BundleContext context = AppProperties.getBundleContext(getClass());
    Preferences prefs = BundlePreferences.getDefaultPreferences(context);
    if (prefs != null) {
      eventManager.getMouseActions().savePreferences(prefs);
      eventManager.setMouseActionsPinned(true);
    }
  }

  private void applyGrid(Dimension grid) {
    ViewportPane<E> pane = eventManager.getSelectedViewPortPane();
    if (pane != null && pane.isTiled()) {
      showTiles(grid);
    } else {
      showStack(grid);
    }
  }

  private void showStack() {
    ViewportPane<E> pane = eventManager.getSelectedViewPortPane();
    if (pane != null && pane.isTiled()) {
      showStack(new Dimension(pane.getTiledCols(), pane.getTiledRows()));
    }
  }

  private void showTiles() {
    ImageViewerPlugin<E> container = eventManager.getSelectedView2dContainer();
    ViewportPane<E> pane = eventManager.getSelectedViewPortPane();
    if (container == null || pane == null || pane.isTiled()) {
      return;
    }
    Dimension grid = container.getCellManager().getLayoutModel().getGridSize();
    boolean singleCell = grid.width * grid.height <= 1;
    showTiles(singleCell ? new Dimension(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE) : grid);
  }

  /** Displays the grid as stacks, one per viewport. */
  private void showStack(Dimension grid) {
    ImageViewerPlugin<E> container = eventManager.getSelectedView2dContainer();
    if (container == null || !container.supportsViewportPanes()) {
      return;
    }
    for (ViewportPane<E> pane : container.getViewportPanes()) {
      pane.switchToStack();
    }
    container.changeLayoutModel(findLayout(grid));
  }

  /** Displays the grid as the tiles of a single viewport, showing the selected series. */
  private void showTiles(Dimension grid) {
    ImageViewerPlugin<E> container = eventManager.getSelectedView2dContainer();
    ViewportPane<E> selected = eventManager.getSelectedViewPortPane();
    if (container == null || selected == null || !container.supportsViewportPanes()) {
      return;
    }
    if (container.getCellManager().getLayoutModel().getCellCount() > 1) {
      moveSeriesToFirstViewport(container, selected);
      container.changeLayoutModel(ImageViewerPlugin.VIEWS_1x1);
    }
    ViewportPane<E> pane = eventManager.getSelectedViewPortPane();
    if (pane == null) {
      return;
    }
    pane.switchToTiled(grid.width, grid.height, () -> container.createDefaultView(null));
    eventManager.updateComponentsListener(pane.getSelectedCanvas());
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
    String id = layoutId(grid);
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

  private static MigLayoutModel buildLayout(Dimension grid) {
    return ImageViewerPlugin.buildMigLayoutModel(
        grid.height, grid.width, ImageViewerPlugin.VIEWPORT_CLASS.getName());
  }

  private static String layoutId(Dimension grid) {
    return grid.height + "x" + grid.width;
  }
}

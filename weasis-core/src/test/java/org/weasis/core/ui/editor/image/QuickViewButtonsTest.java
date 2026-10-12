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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.weasis.core.api.gui.layout.LayoutCellManager;
import org.weasis.core.api.gui.layout.MigLayoutModel;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.api.media.data.MediaSeries;

@SuppressWarnings({"unchecked", "rawtypes"})
class QuickViewButtonsTest {

  private static MigLayoutModel grid(int rows, int cols) {
    return ImageViewerPlugin.buildMigLayoutModel(
        rows, cols, ImageViewerPlugin.VIEWPORT_CLASS.getName());
  }

  private static class Fixture {
    final ImageViewerEventManager<ImageElement> eventManager = mock(ImageViewerEventManager.class);
    final ImageViewerPlugin<ImageElement> container = mock(ImageViewerPlugin.class);
    final LayoutCellManager<ImageElement> cellManager = mock(LayoutCellManager.class);
    final ViewportPane<ImageElement> pane = mock(ViewportPane.class);
    final ViewCanvas<ImageElement> canvas = mock(ViewCanvas.class);
    final QuickViewButtons<ImageElement> buttons = new QuickViewButtons<>(eventManager);

    Fixture(MigLayoutModel current) {
      when(eventManager.getSelectedView2dContainer()).thenReturn(container);
      when(eventManager.getSelectedViewPortPane()).thenReturn(pane);
      when(container.supportsViewportPanes()).thenReturn(true);
      when(container.getCellManager()).thenReturn(cellManager);
      when(cellManager.getLayoutModel()).thenReturn(current);
      when(container.getViewportPanes()).thenReturn(List.of(pane));
      when(pane.getSelectedCanvas()).thenReturn(canvas);
    }

    JButton layoutButton(String spec) {
      return buttons.createLayoutButtons(spec).getFirst();
    }

    JButton stackButton() {
      return buttons.createViewModeButtons().get(0);
    }

    JButton tileButton() {
      return buttons.createViewModeButtons().get(1);
    }
  }

  @Test
  void parseGrids_keepsValidGridsOnceAndInOrder() {
    List<Dimension> grids = QuickViewButtons.parseGrids(" 1x2, 3x4,bogus,0x2,2x,3x4,2x2x2,60x1");
    assertEquals(List.of(new Dimension(2, 1), new Dimension(4, 3)), grids);
  }

  @Test
  void parseGrids_acceptsMissingOrEmptySpec() {
    assertTrue(QuickViewButtons.parseGrids(null).isEmpty());
    assertTrue(QuickViewButtons.parseGrids("").isEmpty());
  }

  @Test
  void createLayoutButtons_buildsOneButtonPerGridWithItsTitle() {
    Fixture f = new Fixture(grid(1, 1));
    List<JButton> buttons = f.buttons.createLayoutButtons("1x2,3x4");
    assertEquals(2, buttons.size());
    assertEquals(grid(1, 2).toString(), buttons.get(0).getToolTipText());
    assertEquals(grid(3, 4).toString(), buttons.get(1).getToolTipText());
  }

  @Test
  void tile_fromAMultiViewLayout_collapsesToOneViewportTiledWithTheGrid() {
    Fixture f = new Fixture(grid(3, 4));
    f.tileButton().doClick(0);

    InOrder order = inOrder(f.container, f.pane, f.eventManager);
    order.verify(f.container).changeLayoutModel(ImageViewerPlugin.VIEWS_1x1);
    order.verify(f.pane).switchToTiled(eq(4), eq(3), any());
    order.verify(f.eventManager).updateComponentsListener(f.canvas);
  }

  @Test
  void tile_fromASingleView_usesTheDefaultGridWithoutChangingTheLayout() {
    Fixture f = new Fixture(ImageViewerPlugin.VIEWS_1x1);
    f.tileButton().doClick(0);

    verify(f.container, never()).changeLayoutModel(any());
    verify(f.pane).switchToTiled(eq(2), eq(2), any());
  }

  @Test
  void tile_whenAlreadyTiled_doesNothing() {
    Fixture f = new Fixture(ImageViewerPlugin.VIEWS_1x1);
    when(f.pane.isTiled()).thenReturn(true);
    f.tileButton().doClick(0);

    verify(f.pane, never()).switchToTiled(any(Integer.class), any(Integer.class), any());
    verify(f.container, never()).changeLayoutModel(any());
  }

  @Test
  void tile_bringsTheSelectedSeriesToTheFirstViewport() {
    Fixture f = new Fixture(grid(1, 2));
    ViewportPane<ImageElement> first = mock(ViewportPane.class);
    ViewCanvas<ImageElement> firstCanvas = mock(ViewCanvas.class);
    MediaSeries<ImageElement> series = mock(MediaSeries.class);
    when(first.getSelectedCanvas()).thenReturn(firstCanvas);
    when(f.canvas.getSeries()).thenReturn(series);
    when(f.container.getViewportPanes()).thenReturn(List.of(first, f.pane));

    f.tileButton().doClick(0);

    verify(firstCanvas).setSeries(series, null);
    verify(f.container).setSelectedImagePane(firstCanvas);
  }

  @Test
  void stack_fromTiles_restoresTheGridAsStacks() {
    Fixture f = new Fixture(ImageViewerPlugin.VIEWS_1x1);
    when(f.pane.isTiled()).thenReturn(true);
    when(f.pane.getTiledCols()).thenReturn(3);
    when(f.pane.getTiledRows()).thenReturn(2);
    f.stackButton().doClick(0);

    InOrder order = inOrder(f.pane, f.container);
    order.verify(f.pane).switchToStack();
    order.verify(f.container).changeLayoutModel(argThat(m -> "2x3".equals(m.getId())));
  }

  @Test
  void stack_whenAlreadyStacked_doesNothing() {
    Fixture f = new Fixture(grid(2, 2));
    f.stackButton().doClick(0);

    verify(f.pane, never()).switchToStack();
    verify(f.container, never()).changeLayoutModel(any());
  }

  @Test
  void layoutButton_inStackMode_showsTheGridAsStacks() {
    Fixture f = new Fixture(ImageViewerPlugin.VIEWS_1x1);
    f.layoutButton("2x2").doClick(0);

    verify(f.pane).switchToStack();
    verify(f.container).changeLayoutModel(argThat(m -> "2x2".equals(m.getId())));
    verify(f.pane, never()).switchToTiled(any(Integer.class), any(Integer.class), any());
  }

  @Test
  void layoutButton_inTileMode_showsTheGridAsTiles() {
    Fixture f = new Fixture(ImageViewerPlugin.VIEWS_1x1);
    when(f.pane.isTiled()).thenReturn(true);
    f.layoutButton("3x4").doClick(0);

    verify(f.pane).switchToTiled(eq(4), eq(3), any());
    verify(f.pane, never()).switchToStack();
  }

  @Test
  void viewModeButtons_ignoreContainersWithoutViewportPanes() {
    Fixture f = new Fixture(grid(2, 2));
    when(f.container.supportsViewportPanes()).thenReturn(false);
    f.layoutButton("2x2").doClick(0);
    f.tileButton().doClick(0);

    verify(f.container, never()).changeLayoutModel(any());
  }
}

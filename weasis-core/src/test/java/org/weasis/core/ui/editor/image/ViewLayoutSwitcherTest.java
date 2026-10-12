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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.weasis.core.api.gui.layout.LayoutCellManager;
import org.weasis.core.api.media.data.ImageElement;

@SuppressWarnings({"unchecked", "rawtypes"})
class ViewLayoutSwitcherTest {

  private final ImageViewerEventManager<ImageElement> eventManager =
      mock(ImageViewerEventManager.class);
  private final ImageViewerPlugin<ImageElement> container = mock(ImageViewerPlugin.class);
  private final ViewportPane<ImageElement> pane = mock(ViewportPane.class);
  private final ViewLayoutSwitcher<ImageElement> switcher = new ViewLayoutSwitcher<>(eventManager);

  private void container(boolean selected) {
    LayoutCellManager<ImageElement> cellManager = mock(LayoutCellManager.class);
    when(container.supportsViewportPanes()).thenReturn(true);
    when(container.getCellManager()).thenReturn(cellManager);
    when(cellManager.getLayoutModel()).thenReturn(ImageViewerPlugin.VIEWS_1x1);
    when(container.getViewportPanes()).thenReturn(List.of(pane));
    when(container.getSelectedViewportPane()).thenReturn(pane);
    when(eventManager.getSelectedView2dContainer()).thenReturn(selected ? container : null);
  }

  @Test
  void showStack_inTheSelectedContainer_followsTheLayoutAction() {
    container(true);
    switcher.showStack(container, new Dimension(6, 2));

    verify(pane).switchToStack();
    verify(container).changeLayoutModel(argThat(m -> "2x6".equals(m.getId())));
    verify(container, never()).setLayoutModel(any());
  }

  @Test
  void showStack_inAnotherContainer_changesItsLayoutDirectly() {
    container(false);
    switcher.showStack(container, new Dimension(6, 2));

    verify(container).setLayoutModel(argThat(m -> "2x6".equals(m.getId())));
    verify(container, never()).changeLayoutModel(any());
  }

  @Test
  void showTiles_tilesTheViewportWithTheGrid() {
    container(true);
    switcher.showTiles(container, new Dimension(4, 3));

    verify(pane).switchToTiled(eq(4), eq(3), any());
    verify(eventManager).updateComponentsListener(pane.getSelectedCanvas());
  }

  @Test
  void gridOf_isTheGridOfTheLayout() {
    container(true);
    assertEquals(new Dimension(1, 1), ViewLayoutSwitcher.gridOf(container));
  }

  @Test
  void buildLayout_isIdentifiedByRowsAndColumns() {
    assertEquals("3x4", ViewLayoutSwitcher.buildLayout(new Dimension(4, 3)).getId());
  }

  @Test
  void switching_withoutAViewportContainerDoesNothing() {
    container(true);
    when(container.supportsViewportPanes()).thenReturn(false);
    switcher.showStack(container, new Dimension(2, 2));
    switcher.showTiles(container, new Dimension(2, 2));

    verify(container, never()).changeLayoutModel(any());
    verify(pane, never()).switchToStack();
  }
}
